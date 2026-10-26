package cc.shinichi.library.loader

/**
 * 为 HTTP/HTTPS 图片提供稳定缓存键，不改变实际请求 URL。
 *
 * 返回 null 时保留完整 URL 作为缓存键；非 null 的键不能为空白。
 * 键必须区分不同内容、缩略图规格、内容版本和必要的账号范围。
 * 此回调可能在不同线程调用，必须快速、线程安全且对同一 URL 返回相同结果。
 * 不应捕获 Activity；预览关闭或 reset() 后会清除此配置。
 */
fun interface ImageCacheKeyProvider {
    fun getCacheKey(url: String): String?
}
