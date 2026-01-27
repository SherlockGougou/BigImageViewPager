package cc.shinichi.library.loader

import android.content.Context
import cc.shinichi.library.ImagePreview
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

/**
 * Glide 请求构建器扩展
 * 封装常用的 Glide 配置，减少重复代码
 */
object GlideExt {

    /**
     * 获取统一的磁盘缓存策略
     */
    @JvmStatic
    fun getDiskCacheStrategy(): DiskCacheStrategy {
        return if (ImagePreview.instance.isSkipLocalCache) {
            DiskCacheStrategy.NONE
        } else {
            DiskCacheStrategy.ALL
        }
    }

    /**
     * 获取是否跳过内存缓存
     */
    @JvmStatic
    fun isSkipMemoryCache(): Boolean {
        return ImagePreview.instance.isSkipLocalCache
    }

    /**
     * 获取错误占位图
     */
    @JvmStatic
    fun getErrorPlaceholder(): Int {
        return ImagePreview.instance.errorPlaceHolder
    }

    /**
     * 创建标准请求选项
     */
    @JvmStatic
    fun standardOptions(): RequestOptions {
        return RequestOptions()
            .skipMemoryCache(isSkipMemoryCache())
            .diskCacheStrategy(getDiskCacheStrategy())
            .error(getErrorPlaceholder())
    }

    /**
     * 扩展函数：应用标准缓存配置
     */
    fun <T> RequestBuilder<T>.applyStandardCache(): RequestBuilder<T> {
        return this
            .skipMemoryCache(isSkipMemoryCache())
            .diskCacheStrategy(getDiskCacheStrategy())
            .error(getErrorPlaceholder())
    }

    /**
     * 下载文件到 Glide 缓存
     */
    @JvmStatic
    fun downloadToCache(
        context: Context,
        url: String,
        target: FileTarget
    ) {
        Glide.with(context)
            .downloadOnly()
            .skipMemoryCache(isSkipMemoryCache())
            .diskCacheStrategy(getDiskCacheStrategy())
            .load(url)
            .into(target)
    }
}
