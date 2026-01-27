package cc.shinichi.library.util

import cc.shinichi.library.ImagePreview
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * HTTP 下载工具类
 *
 * 使用 HttpURLConnection 下载文件
 */
object HttpUtil {

    private const val TAG = "HttpUtil"
    private const val CONNECT_TIMEOUT = 15_000 // 15秒连接超时
    private const val READ_TIMEOUT = 30_000 // 30秒读取超时
    private const val BUFFER_SIZE = 8 * 1024 // 8KB 缓冲区

    /**
     * 下载文件
     *
     * @param urlPath 下载路径
     * @param fileFullName 文件名
     * @param downloadDir 下载存放目录
     * @return 下载的文件，失败返回 null
     */
    @JvmStatic
    fun downloadFile(urlPath: String?, fileFullName: String, downloadDir: String): File? {
        if (urlPath.isNullOrBlank()) {
            SLog.e(TAG, "downloadFile: URL is null or blank")
            return null
        }

        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null

        try {
            val url: URL = URL(urlPath)
            val urlConnection = url.openConnection()
            connection = urlConnection as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.setRequestProperty("Charset", "UTF-8")

            // 添加自定义请求头
            val headers = ImagePreview.instance.headers
            if (headers != null) {
                for ((key, value) in headers) {
                    connection.setRequestProperty(key, value)
                }
            }

            connection.connect()

            // 检查响应码
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                SLog.e(TAG, "downloadFile: HTTP error code: $responseCode")
                return null
            }

            // 创建目标文件
            val file = File(downloadDir, fileFullName)
            val parentDir = file.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }

            // 写入文件
            inputStream = BufferedInputStream(connection.inputStream, BUFFER_SIZE)
            outputStream = FileOutputStream(file)

            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int = inputStream.read(buffer)
            while (bytesRead != -1) {
                outputStream.write(buffer, 0, bytesRead)
                bytesRead = inputStream.read(buffer)
            }
            outputStream.flush()

            SLog.d(TAG, "downloadFile: Success, file=${file.absolutePath}")
            return file

        } catch (e: Exception) {
            SLog.e(TAG, "downloadFile: Failed", e)
            return null
        } finally {
            try {
                inputStream?.close()
            } catch (_: Exception) {
            }
            try {
                outputStream?.close()
            } catch (_: Exception) {
            }
            connection?.disconnect()
        }
    }

    /**
     * URL 解码
     */
    @JvmStatic
    fun decode(text: String): String {
        return try {
            URLDecoder.decode(text, StandardCharsets.UTF_8.name())
        } catch (_: Exception) {
            text
        }
    }
}