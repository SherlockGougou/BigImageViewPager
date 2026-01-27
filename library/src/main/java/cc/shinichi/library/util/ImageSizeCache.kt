package cc.shinichi.library.util

import android.util.LruCache

/**
 * 图片尺寸缓存管理器
 *
 * 避免对同一张图片重复读取尺寸信息，提高性能
 * 使用 LruCache 自动管理内存
 */
object ImageSizeCache {

    /**
     * 图片尺寸信息数据类
     */
    data class ImageSize(
        val width: Int,
        val height: Int,
        val isLongImage: Boolean,
        val isWideImage: Boolean
    ) {
        val isValid: Boolean
            get() = width > 0 && height > 0

        val ratio: Float
            get() = if (width > 0) height.toFloat() / width else 0f

        val widthHeightRatio: Float
            get() = if (height > 0) width.toFloat() / height else 0f
    }

    // 缓存最多 50 张图片的尺寸信息
    private val cache = LruCache<String, ImageSize>(50)

    /**
     * 获取图片尺寸，优先从缓存读取
     */
    @JvmStatic
    fun getImageSize(imagePath: String): ImageSize {
        // 检查缓存
        cache.get(imagePath)?.let { return it }

        // 计算尺寸
        val wh = ImageUtil.getWidthHeight(imagePath)
        val width = wh[0]
        val height = wh[1]

        val isLongImage = height > width && (height.toFloat() / width) >= 3
        val isWideImage = width > height && (width.toFloat() / height) >= 3

        val size = ImageSize(
            width = width,
            height = height,
            isLongImage = isLongImage,
            isWideImage = isWideImage
        )

        // 存入缓存
        if (size.isValid) {
            cache.put(imagePath, size)
        }

        return size
    }

    /**
     * 清除指定图片的缓存
     */
    @JvmStatic
    fun remove(imagePath: String) {
        cache.remove(imagePath)
    }

    /**
     * 清除所有缓存
     */
    @JvmStatic
    fun clear() {
        cache.evictAll()
    }

    /**
     * 获取当前缓存大小
     */
    @JvmStatic
    fun size(): Int = cache.size()

    /**
     * 预加载图片尺寸（可在后台线程调用）
     */
    @JvmStatic
    fun preload(imagePaths: List<String>) {
        imagePaths.forEach { path ->
            if (cache.get(path) == null) {
                getImageSize(path)
            }
        }
    }
}
