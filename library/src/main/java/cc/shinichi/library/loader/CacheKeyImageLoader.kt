package cc.shinichi.library.loader

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import java.io.InputStream

internal data class CacheKeyImageModel(val url: String, val cacheKey: String) {
    val sourceKey = ObjectKey(cacheKey)
}

internal class CacheKeyImageLoader(
    private val delegate: ModelLoader<GlideUrl, InputStream>
) : ModelLoader<CacheKeyImageModel, InputStream> {

    override fun handles(model: CacheKeyImageModel): Boolean = delegate.handles(GlideUrl(model.url))

    override fun buildLoadData(
        model: CacheKeyImageModel,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream>? {
        // 网络加载器只接收当前完整 URL，避免复用已经过期的鉴权链接。
        val data = delegate.buildLoadData(GlideUrl(model.url), width, height, options) ?: return null
        return ModelLoader.LoadData(model.sourceKey, data.fetcher)
    }

    class Factory : ModelLoaderFactory<CacheKeyImageModel, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<CacheKeyImageModel, InputStream> {
            return CacheKeyImageLoader(multiFactory.build(GlideUrl::class.java, InputStream::class.java))
        }

        override fun teardown() {}
    }
}
