package cc.shinichi.library.tool.image

/**
 * 仓库: <a href="http://git.xiaozhouintel.com/root/C-UI-App-Android">...</a>
 * 文件名: UtilExt.java
 * 作者: kirito
 * 描述: 常用扩展类
 * 创建时间: 2025/8/11
 */
object UtilExt {

    fun String.isLocalImage(): Boolean {
        return this.lowercase().startsWith("file://")
                || this.lowercase().startsWith("/storage")
                || this.lowercase().startsWith("content://")
                || this.lowercase().startsWith("android.resource://")
                || this.lowercase().startsWith("assets://")
                || this.lowercase().startsWith("raw://")
    }
}