package cc.shinichi.library.glide.progress

import android.text.TextUtils
import cc.shinichi.library.glide.SSLSocketClient
import cc.shinichi.library.glide.progress.ProgressResponseBody.InternalProgressListener
import okhttp3.OkHttpClient
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 */
object ProgressManager {

    private val listenersMap = Collections.synchronizedMap(HashMap<String, OnProgressListener>())

    private val LISTENER = object : InternalProgressListener {
        override fun onProgress(url: String?, bytesRead: Long, totalBytes: Long) {
            val percentage = (bytesRead * 1f / totalBytes * 100f).toInt()
            val isComplete = percentage >= 100
            listenersMap.let {
                for (listener in it.values) {
                    listener.onProgress(url, isComplete, percentage, bytesRead, totalBytes)
                }
            }
            if (isComplete) {
                removeListener(url)
            }
        }
    }

    @JvmStatic
    val okHttpClient: OkHttpClient
        get() {
            val builder = OkHttpClient.Builder()
            builder.addNetworkInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                response.newBuilder()
                    .body(response.body()?.let { ProgressResponseBody(request.url().toString(), LISTENER, it) })
                    .build()
            }
                .sslSocketFactory(SSLSocketClient.sSLSocketFactory, SSLSocketClient.geX509tTrustManager())
                .hostnameVerifier(SSLSocketClient.hostnameVerifier)
            builder.connectTimeout(30, TimeUnit.SECONDS)
            builder.writeTimeout(30, TimeUnit.SECONDS)
            builder.readTimeout(30, TimeUnit.SECONDS)
            return builder.build()
        }

    @JvmStatic
    fun addListener(url: String?, listener: OnProgressListener?) {
        if (!TextUtils.isEmpty(url)) {
            listenersMap[url] = listener
            listener?.onProgress(url, false, 1, 0, 0)
        }
    }

    private fun removeListener(url: String?) {
        if (!TextUtils.isEmpty(url)) {
            listenersMap.remove(url)
        }
    }
}