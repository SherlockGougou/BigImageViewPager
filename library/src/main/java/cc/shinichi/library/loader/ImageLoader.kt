package cc.shinichi.library.loader

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import cc.shinichi.library.ImagePreview
import cc.shinichi.library.loader.cache.DataCacheKey
import cc.shinichi.library.loader.cache.SafeKeyGenerator
import cc.shinichi.library.util.SLog
import com.bumptech.glide.Glide
import com.bumptech.glide.disklrucache.DiskLruCache
import com.bumptech.glide.load.engine.cache.DiskCache
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.signature.EmptySignature
import java.io.File

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 * cc.shinichi.library.loader
 * create at 2018/5/21  15:22
 * description:
 */
object ImageLoader {

    private const val TAG = "ImageLoader"

    internal fun getModel(url: String?): Any? {
        if (url == null) return null
        val scheme = Uri.parse(url).scheme
        if (!scheme.equals("http", ignoreCase = true) && !scheme.equals("https", ignoreCase = true)) {
            return url
        }
        val cacheKey = ImagePreview.instance.imageCacheKeyProvider?.getCacheKey(url) ?: return url
        require(cacheKey.isNotBlank()) { "ImageCacheKeyProvider must return null or a non-blank key" }
        return CacheKeyImageModel(url, cacheKey)
    }

    /**
     * 获取是否有某张原图的缓存
     * 必须缓存原始数据（DiskCacheStrategy.DATA 或 ALL），且与下载使用相同缓存键。
     */
    fun getGlideCacheFile(context: Context, url: String?): File? {
        if (ImagePreview.instance.isSkipLocalCache || url.isNullOrBlank()) return null
        // 提供器配置错误应直接暴露，不能被当作普通缓存未命中。
        val model = getModel(url)
        val sourceKey = if (model is CacheKeyImageModel) model.sourceKey else GlideUrl(url)
        try {
            val dataCacheKey = DataCacheKey(sourceKey, EmptySignature.obtain())
            val safeKeyGenerator = SafeKeyGenerator()
            val safeKey = safeKeyGenerator.getSafeKey(dataCacheKey)
            SLog.d(TAG, "safeKey = $safeKey")
            val file = File(context.cacheDir, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR)
            val diskLruCache =
                DiskLruCache.open(file, 1, 1, DiskCache.Factory.DEFAULT_DISK_CACHE_SIZE.toLong())
            val value = diskLruCache[safeKey]
            return value?.getFile(0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    @JvmStatic
    fun clearMemory(activity: AppCompatActivity) {
        Glide.get(activity.applicationContext).clearMemory()
    }

    @JvmStatic
    fun cleanDiskCache(context: Context) {
        Thread { Glide.get(context.applicationContext).clearDiskCache() }.start()
    }
}
