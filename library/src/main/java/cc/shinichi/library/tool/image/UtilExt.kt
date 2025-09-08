package cc.shinichi.library.tool.image

import androidx.core.net.toUri

/**
 * 仓库: <a href="http://git.xiaozhouintel.com/root/C-UI-App-Android">...</a>
 * 文件名: UtilExt.java
 * 作者: kirito
 * 描述: 常用扩展类
 * 创建时间: 2025/8/11
 */
object UtilExt {

    fun String.isLocalFile(): Boolean {
        val uri = this.toUri()
        return when (uri.scheme?.lowercase()) {
            "file" -> true
            "content" -> true
            "android.resource" -> true
            "assets", "raw" -> true
            null -> this.startsWith("/")
            else -> false
        }
    }
}