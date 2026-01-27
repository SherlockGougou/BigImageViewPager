package cc.shinichi.library.loader.progress

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 */
interface OnProgressListener {
    fun onProgress(
        url: String?,
        isComplete: Boolean,
        percentage: Int,
        bytesRead: Long,
        totalBytes: Long
    )
}