package cc.shinichi.bigimageviewpager

import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.SystemClock
import android.provider.MediaStore
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.viewpager.widget.ViewPager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import cc.shinichi.library.ImagePreview
import cc.shinichi.library.R as LibraryR
import cc.shinichi.library.callback.OnDownloadListener
import cc.shinichi.library.model.ImageInfo
import cc.shinichi.library.ui.ImagePreviewActivity
import cc.shinichi.library.ui.subsampling.SubsamplingScaleImageView
import cc.shinichi.library.util.DownloadUtil
import com.bumptech.glide.Glide
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class ImagePreviewCacheFlowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private lateinit var server: MockWebServer
    private var activity: ImagePreviewActivity? = null
    private val thumbnail = image(16)
    private val original = image(32)
    private val photo by lazy { context.resources.openRawResource(R.raw.blackhole).use { it.readBytes() } }
    private val longImage by lazy { image(256, 4096) }

    @Before
    fun setUp() {
        ImagePreview.instance.reset()
        Glide.get(context).clearDiskCache()
        instrumentation.runOnMainSync { Glide.get(context).clearMemory() }
        server = MockWebServer()
        server.setDispatcher(object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val bytes = when {
                    request.path!!.startsWith("/photo") -> photo
                    request.path!!.startsWith("/long") -> longImage
                    request.path!!.startsWith("/thumb") -> thumbnail
                    else -> original
                }
                return MockResponse().setHeader("Content-Type", "image/jpeg")
                    .setBody(Buffer().write(bytes))
            }
        })
        server.start()
    }

    @After
    fun tearDown() {
        closePreview()
        ImagePreview.instance.reset()
        Glide.get(context).clearDiskCache()
        server.shutdown()
    }

    @Test
    fun reopenPreviewWithRotatedSignatureDisplaysCachedOriginal() {
        openPreview("/original.jpg?sign=first", "/original.jpg?sign=first")
        awaitImageWidth(32)
        closePreview()
        assertNull(ImagePreview.instance.imageCacheKeyProvider)
        openPreview("/original.jpg?sign=second", "/original.jpg?sign=second")
        awaitImageWidth(32)
        assertEquals(1, server.requestCount)
        assertEquals("/original.jpg?sign=first", server.takeRequest(5, TimeUnit.SECONDS)!!.path)
    }

    @Test
    fun realPhotoSupportsZoomPagingRotationAndCachedReopen() {
        configureGallery("first")
        launchPreview()
        awaitUi("Photo did not load") { selectedImage()?.let { it.isImageLoaded && it.sWidth == 1024 } == true }
        screenshot("01-photo-portrait")
        var scale = 0f
        val bounds = Rect()
        instrumentation.runOnMainSync {
            scale = selectedImage()!!.scale
            check(selectedImage()!!.getGlobalVisibleRect(bounds))
        }
        doubleTap(bounds.exactCenterX(), bounds.exactCenterY())
        awaitUi("Double tap did not zoom") { selectedImage()!!.scale > scale * 1.2f }
        screenshot("02-photo-zoomed")
        doubleTap(bounds.exactCenterX(), bounds.exactCenterY())
        awaitUi("Double tap did not restore scale") { selectedImage()!!.scale <= scale * 1.05f }
        swipe(bounds.left + bounds.width() * 0.8f, bounds.exactCenterY(),
            bounds.left + bounds.width() * 0.2f, bounds.exactCenterY())
        awaitUi("Swipe did not select long image") {
            activity!!.findViewById<ViewPager>(LibraryR.id.viewPager).currentItem == 1 &&
                selectedImage()?.let { it.isImageLoaded && it.sHeight == 4096 } == true
        }
        screenshot("03-long-image")
        instrumentation.runOnMainSync { activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        awaitUi("Landscape layout did not load") {
            activity!!.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                selectedImage()?.isReady == true
        }
        screenshot("04-landscape")
        instrumentation.runOnMainSync { activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        awaitUi("Portrait layout did not restore") {
            activity!!.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT &&
                selectedImage()?.isReady == true
        }
        closePreview()
        configureGallery("second")
        launchPreview()
        awaitUi("Cached photo did not load") { selectedImage()?.let { it.isImageLoaded && it.sWidth == 1024 } == true }
        screenshot("05-cached-reopen")
        assertEquals("Rotated signatures must not request either image again", 2, server.requestCount)
    }

    private fun configureGallery(signature: String) {
        ImagePreview.instance
            .setMediaInfoList(mutableListOf(
                ImageInfo.createImage(server.url("/photo.jpg?sign=$signature").toString()),
                ImageInfo.createImage(server.url("/long.jpg?sign=$signature").toString())))
            .setLoadStrategy(ImagePreview.LoadStrategy.AlwaysOrigin)
            .setEnableClickClose(false)
            .setEnableDragCloseIgnoreScale(false)
            .setShowIndicator(true)
            .setImageCacheKeyProvider { android.net.Uri.parse(it).buildUpon().clearQuery().build().toString() }
    }

    private fun selectedImage(): SubsamplingScaleImageView? {
        return activity!!.supportFragmentManager.fragments.mapNotNull {
            it.view?.findViewById<SubsamplingScaleImageView>(LibraryR.id.static_view)
        }.firstOrNull { view ->
            val bounds = Rect()
            view.getGlobalVisibleRect(bounds) && bounds.width() > activity!!.window.decorView.width * 0.8f
        }
    }

    private fun awaitUi(message: String, condition: () -> Boolean) {
        val ready = AtomicBoolean(false)
        val deadline = SystemClock.uptimeMillis() + 15000
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.runOnMainSync { ready.set(condition()) }
            if (ready.get()) return
            SystemClock.sleep(50)
        }
        fail(message)
    }

    private fun doubleTap(x: Float, y: Float) {
        repeat(2) {
            val down = SystemClock.uptimeMillis()
            touch(down, MotionEvent.ACTION_DOWN, x, y)
            SystemClock.sleep(40)
            touch(down, MotionEvent.ACTION_UP, x, y)
            SystemClock.sleep(80)
        }
    }

    private fun swipe(startX: Float, startY: Float, endX: Float, endY: Float) {
        val down = SystemClock.uptimeMillis()
        touch(down, MotionEvent.ACTION_DOWN, startX, startY)
        for (step in 1..20) {
            SystemClock.sleep(15)
            val fraction = step / 20f
            touch(down, MotionEvent.ACTION_MOVE, startX + (endX - startX) * fraction,
                startY + (endY - startY) * fraction)
        }
        touch(down, MotionEvent.ACTION_UP, endX, endY)
    }

    private fun touch(downTime: Long, action: Int, x: Float, y: Float) {
        val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
        try {
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            instrumentation.sendPointerSync(event)
        } finally {
            event.recycle()
        }
    }

    private fun screenshot(name: String) {
        // 布局状态先于系统旋转和缩放动画完成，等待合成器稳定后再留存画面。
        instrumentation.waitForIdleSync()
        SystemClock.sleep(600)
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        val directory = File(context.getExternalFilesDir(null), "pixel-cache-qa").apply { mkdirs() }
        try {
            File(directory, "$name.png").outputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun localPreviewBypassesCacheKeyProvider() {
        val file = File(context.cacheDir, "cache-key-local-test.jpg")
        try {
            file.writeBytes(original)
            ImagePreview.instance.setImage(file.absolutePath)
                .setImageCacheKeyProvider { error("Local file must bypass provider") }
            launchPreview()
            awaitImageWidth(32)
            assertEquals(0, server.requestCount)
        } finally {
            closePreview()
            file.delete()
        }
    }

    @Test
    fun skipCacheReopenRequestsLatestSignedUrl() {
        openPreview("/original.jpg?sign=first", "/original.jpg?sign=first")
        awaitImageWidth(32)
        closePreview()
        ImagePreview.instance.setSkipLocalCache(true)
        openPreview("/original.jpg?sign=second", "/original.jpg?sign=second")
        awaitImageWidth(32)
        assertEquals(2, server.requestCount)
        assertEquals("/original.jpg?sign=first", server.takeRequest(5, TimeUnit.SECONDS)!!.path)
        assertEquals("/original.jpg?sign=second", server.takeRequest(5, TimeUnit.SECONDS)!!.path)
    }

    @Test
    fun viewOriginalDownloadsFullUrlAndReopenUsesOriginalCache() {
        openPreview("/thumb.jpg?sign=first", "/original.jpg?sign=first")
        awaitImageWidth(16)
        instrumentation.runOnMainSync {
            activity!!.findViewById<View>(LibraryR.id.btn_show_origin).performClick()
        }
        awaitImageWidth(32)
        assertEquals(2, server.requestCount)
        assertEquals("/thumb.jpg?sign=first", server.takeRequest(5, TimeUnit.SECONDS)!!.path)
        assertEquals("/original.jpg?sign=first", server.takeRequest(5, TimeUnit.SECONDS)!!.path)
        closePreview()
        openPreview("/thumb.jpg?sign=second", "/original.jpg?sign=second")
        awaitImageWidth(32)
        assertEquals(2, server.requestCount)
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun savingRotatedUrlUsesOriginalCacheAndWritesCorrectBytes() {
        openPreview("/original.jpg?sign=first", "/original.jpg?sign=first")
        awaitImageWidth(32)
        val folder = "BigImageCacheTest-${UUID.randomUUID()}"
        val saved = CountDownLatch(1)
        val success = AtomicBoolean(false)
        val relativePath = "Pictures/$folder/"
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        try {
            ImagePreview.instance.setFolderName(folder).setDownloadListener(object : OnDownloadListener() {
                override fun onDownloadStart(activity: Activity, position: Int) {}
                override fun onDownloadSuccess(activity: Activity, position: Int) {
                    success.set(true)
                    saved.countDown()
                }
                override fun onDownloadFailed(activity: Activity, position: Int) {
                    saved.countDown()
                }
            })
            instrumentation.runOnMainSync {
                DownloadUtil.downloadPicture(activity!!, 0, server.url("/original.jpg?sign=second").toString())
            }
            assertTrue("Save callback timed out", saved.await(15, TimeUnit.SECONDS))
            assertTrue("Image save failed", success.get())
            assertEquals(1, server.requestCount)
            context.contentResolver.query(collection, arrayOf(MediaStore.Images.Media._ID),
                "${MediaStore.Images.Media.RELATIVE_PATH} = ?", arrayOf(relativePath), null)!!.use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                val uri = ContentUris.withAppendedId(collection, cursor.getLong(0))
                context.contentResolver.openInputStream(uri)!!.use {
                    assertArrayEquals(original, it.readBytes())
                }
            }
        } finally {
            // 仅删除本测试在唯一目录下创建的媒体记录。
            context.contentResolver.delete(collection,
                "${MediaStore.Images.Media.RELATIVE_PATH} = ?", arrayOf(relativePath))
        }
    }

    private fun openPreview(thumb: String, origin: String) {
        ImagePreview.instance
            .setMediaInfoList(mutableListOf(ImageInfo.createImage(
                server.url(thumb).toString(), server.url(origin).toString())))
            .setLoadStrategy(ImagePreview.LoadStrategy.Default)
            .setImageCacheKeyProvider { android.net.Uri.parse(it).buildUpon().clearQuery().build().toString() }
        launchPreview()
    }

    private fun launchPreview() {
        activity = instrumentation.startActivitySync(
            Intent(context, ImagePreviewActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ) as ImagePreviewActivity
        instrumentation.waitForIdleSync()
    }

    private fun closePreview() {
        activity?.let { preview -> instrumentation.runOnMainSync { preview.finish() } }
        activity = null
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync { Glide.get(context).clearMemory() }
    }

    private fun awaitImageWidth(width: Int) {
        val ready = AtomicBoolean(false)
        val deadline = SystemClock.uptimeMillis() + 15000
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.runOnMainSync {
                val view = activity!!.findViewById<SubsamplingScaleImageView>(LibraryR.id.static_view)
                ready.set(view != null && view.isImageLoaded && view.sWidth == width)
            }
            if (ready.get()) return
            SystemClock.sleep(50)
        }
        fail("Preview did not display image with width $width; requests=${server.requestCount}")
    }

    private fun image(width: Int, height: Int = 16): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)
        return ByteArrayOutputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output))
            bitmap.recycle()
            output.toByteArray()
        }
    }
}
