package cc.shinichi.library.tool.common

import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder

/**
 * HttpURLConnection 下载图片
 */
object HttpUtil {

    /**
     * @param urlPath     下载路径
     * @param downloadDir 下载存放目录
     * @return 返回下载文件
     */
    fun downloadFile(urlPath: String?, fileFullName: String, downloadDir: String): File? {
        var file: File? = null
        try {
            // 统一资源
            val url = URL(urlPath)
            // 连接类的父类，抽象类
            val urlConnection = url.openConnection()
            // http的连接类
            val httpURLConnection = urlConnection as HttpURLConnection
            // 设定请求的方法，默认是GET
            httpURLConnection.requestMethod = "GET"
            // 设置字符编码
            httpURLConnection.setRequestProperty("Charset", "UTF-8")
            // 打开到此 URL 引用的资源的通信链接（如果尚未建立这样的连接）。
            httpURLConnection.connect()
            val bin = BufferedInputStream(httpURLConnection.inputStream)
            val path = downloadDir + File.separatorChar + fileFullName
            file = File(path)
            file.parentFile?.let {
                if (!it.exists()) {
                    it.mkdirs()
                }
            }
            val out: OutputStream = FileOutputStream(file)
            var size = 0
            var len = 0
            val buf = ByteArray(1024)
            while (bin.read(buf).also { size = it } != -1) {
                len += size
                out.write(buf, 0, size)
            }
            bin.close()
            out.close()
            return file
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun decode(text: String): String {
        return URLDecoder.decode(text)
    }
}