package cc.shinichi.bigimageviewpager

import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cc.shinichi.library.ImagePreview
import cc.shinichi.library.loader.FileTarget
import cc.shinichi.library.loader.GlideExt
import cc.shinichi.library.loader.ImageLoader
import com.bumptech.glide.Glide
import com.bumptech.glide.request.transition.Transition
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class ImageCacheIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext.applicationContext
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        ImagePreview.instance.reset()
        Glide.get(context).clearDiskCache()
        clearMemory()
        server = MockWebServer()
        server.setDispatcher(object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return MockResponse().setBody(request.path ?: "missing-path")
            }
        })
        server.start()
    }

    @After
    fun tearDown() {
        ImagePreview.instance.reset()
        clearMemory()
        Glide.get(context).clearDiskCache()
        server.shutdown()
    }

    @Test
    fun defaultKeepsFullUrlAndExistingDataCacheLookup() {
        val first = url("/photo.png?sign=first&t=1")
        val second = url("/photo.png?sign=second&t=2")
        assertEquals("/photo.png?sign=first&t=1", download(first).readText())
        assertEquals("/photo.png?sign=second&t=2", download(second).readText())
        assertEquals(2, server.requestCount)
        assertEquals("/photo.png?sign=first&t=1", ImageLoader.getGlideCacheFile(context, first)!!.readText())
    }

    @Test
    fun rotatedSignatureReusesRawDataAndManualLookup() {
        ImagePreview.instance.setImageCacheKeyProvider(::stableKey)
        val first = url("/photo.png?width=100&sign=first&t=1")
        val second = url("/photo.png?width=100&sign=second&t=2")
        val file = download(first)
        assertEquals("/photo.png?width=100&sign=first&t=1", file.readText())
        assertEquals("/photo.png?width=100&sign=first&t=1", server.takeRequest(5, TimeUnit.SECONDS)!!.path)
        assertEquals(file.absolutePath, ImageLoader.getGlideCacheFile(context, second)!!.absolutePath)
        assertEquals(file.absolutePath, download(second).absolutePath)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun cacheMissAfterClearingDiskUsesLatestSignedUrl() {
        ImagePreview.instance.setImageCacheKeyProvider(::stableKey)
        download(url("/photo.png?sign=old&t=1"))
        Glide.get(context).clearDiskCache()
        val latest = url("/photo.png?sign=current&t=2")
        assertEquals("/photo.png?sign=current&t=2", download(latest).readText())
        assertEquals(2, server.requestCount)
        assertEquals("/photo.png?sign=old&t=1", server.takeRequest(5, TimeUnit.SECONDS)!!.path)
        assertEquals("/photo.png?sign=current&t=2", server.takeRequest(5, TimeUnit.SECONDS)!!.path)
    }

    @Test
    fun contentParametersAndAccountNamespacesStaySeparate() {
        ImagePreview.instance.setImageCacheKeyProvider { "account-a:${stableKey(it)}" }
        val thumb = url("/photo.png?width=100&version=1&sign=a")
        val original = url("/photo.png?width=1000&version=1&sign=a")
        val revised = url("/photo.png?width=1000&version=2&sign=a")
        assertEquals("/photo.png?width=100&version=1&sign=a", download(thumb).readText())
        assertEquals("/photo.png?width=1000&version=1&sign=a", download(original).readText())
        assertEquals("/photo.png?width=1000&version=2&sign=a", download(revised).readText())
        ImagePreview.instance.setImageCacheKeyProvider { "account-b:${stableKey(it)}" }
        assertNull(ImageLoader.getGlideCacheFile(context, revised))
        download(revised)
        assertEquals(4, server.requestCount)
    }

    @Test
    fun nullProviderResultAndResetRestoreDefaultBehavior() {
        ImagePreview.instance.setImageCacheKeyProvider { null }
        download(url("/photo.png?sign=a"))
        download(url("/photo.png?sign=b"))
        ImagePreview.instance.setImageCacheKeyProvider(::stableKey)
        ImagePreview.instance.reset()
        assertNull(ImagePreview.instance.imageCacheKeyProvider)
        download(url("/photo.png?sign=c"))
        assertEquals(3, server.requestCount)
    }

    @Test
    fun skipLocalCacheDoesNotReturnExistingRawFile() {
        ImagePreview.instance.setImageCacheKeyProvider(::stableKey)
        val path = url("/photo.png?sign=a")
        download(path)
        assertNotNull(ImageLoader.getGlideCacheFile(context, path))
        ImagePreview.instance.setSkipLocalCache(true)
        assertNull(ImageLoader.getGlideCacheFile(context, path))
        assertEquals(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE, GlideExt.getDiskCacheStrategy())
        assertEquals(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE, GlideExt.getDownloadDiskCacheStrategy())
    }

    @Test
    fun blankCacheKeyFailsBeforeRequestOrCacheLookup() {
        ImagePreview.instance.setImageCacheKeyProvider { " " }
        val path = url("/photo.png?sign=a")
        assertThrows(IllegalArgumentException::class.java) { download(path) }
        assertThrows(IllegalArgumentException::class.java) { ImageLoader.getGlideCacheFile(context, path) }
        assertEquals(0, server.requestCount)
    }

    private fun url(path: String): String = server.url(path).toString()

    private fun stableKey(url: String): String {
        val uri = Uri.parse(url)
        val builder = uri.buildUpon().clearQuery()
        uri.queryParameterNames.filterNot { it == "sign" || it == "t" }.forEach { name ->
            uri.getQueryParameters(name).forEach { value -> builder.appendQueryParameter(name, value) }
        }
        return builder.build().toString()
    }

    private fun clearMemory() {
        instrumentation.runOnMainSync { Glide.get(context).clearMemory() }
    }

    private fun download(url: String): File {
        // 每次清理内存，确保复用来自磁盘缓存，而不是仍存活的 File 资源。
        clearMemory()
        val done = CountDownLatch(1)
        val result = AtomicReference<File>()
        GlideExt.downloadToCache(context, url, object : FileTarget() {
            override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                result.set(resource)
                done.countDown()
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                done.countDown()
            }
        })
        assertTrue("Timed out downloading image", done.await(15, TimeUnit.SECONDS))
        return requireNotNull(result.get()) { "Glide file download failed" }
    }
}
