package cc.shinichi.bigimageviewpager

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.SystemClock
import android.util.Base64
import android.view.InputDevice
import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cc.shinichi.library.ImagePreview
import cc.shinichi.library.R as LibraryR
import cc.shinichi.library.ui.ImagePreviewActivity
import cc.shinichi.library.ui.subsampling.SubsamplingScaleImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class ImagePreviewLifecycleRegressionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private lateinit var server: MockWebServer
    private var activity: ImagePreviewActivity? = null
    private val fallbackGif = Base64.decode(
        "R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==",
        Base64.DEFAULT
    )

    @Before
    fun setUp() {
        ImagePreview.instance.reset()
        Glide.get(context).clearDiskCache()
        instrumentation.runOnMainSync { Glide.get(context).clearMemory() }
        server = MockWebServer()
        server.setDispatcher(object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                return if (path.startsWith("/fallback-direct.gif")) {
                    MockResponse()
                        .setHeader("Content-Type", "image/gif")
                        .setBody(Buffer().write(fallbackGif))
                } else {
                    MockResponse()
                        .setHeader("Content-Type", "image/jpeg")
                        .setBody(Buffer().write(image(32, 32)))
                }
            }
        })
        server.start()
    }

    @After
    fun tearDown() {
        closePreview()
        ImagePreview.instance.reset()
        Glide.get(context).clearDiskCache()
        instrumentation.runOnMainSync { Glide.get(context).clearMemory() }
        server.shutdown()
    }

    @Test
    fun stateListenerResetDuringTouchDoesNotCrash() {
        openPreview("/photo.jpg")
        awaitImageWidth(32)

        val changed = AtomicBoolean(false)
        val view = selectedImage() ?: error("No visible subsampling view")
        instrumentation.runOnMainSync {
            view.setOnStateChangedListener(object : SubsamplingScaleImageView.DefaultOnStateChangedListener() {
                override fun onScaleChanged(newScale: Float, origin: Int) {
                    changed.set(true)
                    view.recycle()
                }
            })
        }

        val centerX = view.width / 2f
        val centerY = view.height / 2f
        pinch(view, centerX, centerY)
        SystemClock.sleep(300)

        assertTrue("Pinch did not change scale", changed.get())
    }

    @Test
    fun fallbackDownloadCompletingAfterFinishDoesNotCrash() {
        openPreview("/photo.jpg")
        awaitRequestCount(1)

        val preview = activity ?: error("Preview activity is missing")
        val fragment = preview.supportFragmentManager.fragments.first()
        val fallbackUrl = server.url("/fallback-direct.gif").toString()
        val fallback = fallbackMethod(fragment)
        instrumentation.runOnMainSync {
            fallback.invoke(fragment, fallbackUrl, fallbackUrl, null)
            preview.finish()
        }
        assertTrue(preview.isFinishing || preview.isDestroyed)

        // 下载线程完成后由 Handler 投递，旧实现会在这里触发 Glide.with(destroyed activity)。
        SystemClock.sleep(1500)
        instrumentation.waitForIdleSync()
    }

    @Test
    fun fallbackDownloadAfterFinishIsIgnored() {
        openPreview("/photo.jpg")
        awaitRequestCount(1)

        val preview = activity ?: error("Preview activity is missing")
        val fragment = preview.supportFragmentManager.fragments.first()
        closePreview()
        assertTrue(preview.isFinishing || preview.isDestroyed)

        val fallbackUrl = server.url("/fallback-direct.gif").toString()
        invokeFallback(fragment, fallbackUrl)
        SystemClock.sleep(1000)

        assertEquals("Fallback must not start after the preview is destroyed", 1, server.requestCount)
    }

    private fun invokeFallback(fragment: androidx.fragment.app.Fragment, url: String) {
        val fallback = fallbackMethod(fragment)
        instrumentation.runOnMainSync { fallback.invoke(fragment, url, url, null) }
    }

    private fun fallbackMethod(fragment: androidx.fragment.app.Fragment): java.lang.reflect.Method {
        return fragment.javaClass.getDeclaredMethod(
            "fallbackDownload",
            String::class.java,
            String::class.java,
            GlideException::class.java
        ).apply { isAccessible = true }
    }

    private fun openPreview(path: String) {
        ImagePreview.instance
            .setImage(server.url(path).toString())
            .setEnableClickClose(false)
            .setEnableDragClose(false)
        activity = instrumentation.startActivitySync(
            Intent(context, ImagePreviewActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ) as ImagePreviewActivity
        instrumentation.waitForIdleSync()
    }

    private fun closePreview() {
        val preview = activity ?: return
        instrumentation.runOnMainSync { preview.finish() }
        activity = null
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync { Glide.get(context).clearMemory() }
    }

    private fun selectedImage(): SubsamplingScaleImageView? {
        val preview = activity ?: return null
        return preview.supportFragmentManager.fragments.mapNotNull {
            it.view?.findViewById<SubsamplingScaleImageView>(LibraryR.id.static_view)
        }.firstOrNull { view ->
            val bounds = Rect()
            view.getGlobalVisibleRect(bounds) && bounds.width() > preview.window.decorView.width * 0.8f
        }
    }

    private fun awaitImageWidth(width: Int) {
        val ready = AtomicBoolean(false)
        val deadline = SystemClock.uptimeMillis() + 15000
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.runOnMainSync {
                val view = activity?.findViewById<SubsamplingScaleImageView>(LibraryR.id.static_view)
                ready.set(view != null && view.isImageLoaded && view.sWidth == width)
            }
            if (ready.get()) return
            SystemClock.sleep(50)
        }
        fail("Preview did not display image with width $width; requests=${server.requestCount}")
    }

    private fun awaitRequestCount(count: Int) {
        val deadline = SystemClock.uptimeMillis() + 15000
        while (SystemClock.uptimeMillis() < deadline) {
            if (server.requestCount >= count) return
            SystemClock.sleep(50)
        }
        fail("Expected at least $count requests; actual=${server.requestCount}")
    }

    private fun pinch(view: SubsamplingScaleImageView, x: Float, y: Float) {
        val downTime = SystemClock.uptimeMillis()
        dispatchTouch(view, downTime, downTime, MotionEvent.ACTION_DOWN, floatArrayOf(x - 40f), floatArrayOf(y))
        dispatchTouch(
            view,
            downTime,
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            floatArrayOf(x - 40f, x + 40f),
            floatArrayOf(y, y)
        )
        for (step in 1..10) {
            SystemClock.sleep(20)
            val spread = 40f + step * 13f
            dispatchTouch(
                view,
                downTime,
                SystemClock.uptimeMillis(),
                MotionEvent.ACTION_MOVE,
                floatArrayOf(x - spread, x + spread),
                floatArrayOf(y, y)
            )
        }
        dispatchTouch(
            view,
            downTime,
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            floatArrayOf(x - 170f, x + 170f),
            floatArrayOf(y, y)
        )
        dispatchTouch(view, downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, floatArrayOf(x - 170f), floatArrayOf(y))
    }

    private fun dispatchTouch(
        view: SubsamplingScaleImageView,
        downTime: Long,
        eventTime: Long,
        action: Int,
        x: FloatArray,
        y: FloatArray
    ) {
        val pointerCount = x.size
        val properties = Array(pointerCount) { index ->
            MotionEvent.PointerProperties().apply {
                id = index
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        }
        val coords = Array(pointerCount) { index ->
            MotionEvent.PointerCoords().apply {
                this.x = x[index]
                this.y = y[index]
                pressure = 1f
                size = 1f
            }
        }
        val event = MotionEvent.obtain(
            downTime,
            eventTime,
            action,
            pointerCount,
            properties,
            coords,
            0,
            0,
            1f,
            1f,
            0,
            0,
            InputDevice.SOURCE_TOUCHSCREEN,
            0
        )
        try {
            instrumentation.runOnMainSync { view.onTouchEvent(event) }
        } finally {
            event.recycle()
        }
    }

    private fun image(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)
        return ByteArrayOutputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output))
            bitmap.recycle()
            output.toByteArray()
        }
    }
}
