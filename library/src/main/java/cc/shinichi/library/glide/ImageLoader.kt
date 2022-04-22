package cc.shinichi.library.glide

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import cc.shinichi.library.glide.cache.DataCacheKey
import cc.shinichi.library.glide.cache.SafeKeyGenerator
import com.bumptech.glide.Glide
import com.bumptech.glide.disklrucache.DiskLruCache
import com.bumptech.glide.load.engine.cache.DiskCache
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.signature.EmptySignature
import java.io.File

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 * cc.shinichi.library.glide
 * create at 2018/5/21  15:22
 * description:
 */
object ImageLoader {

    private const val TAG = "ImageLoader"

    /**
     * 获取是否有某张原图的缓存
     * 缓存模式必须是：DiskCacheStrategy.SOURCE 才能获取到缓存文件
     */
    @JvmStatic
    fun getGlideCacheFile(context: Context, url: String?): File? {
        try {
            val dataCacheKey = DataCacheKey(GlideUrl(url), EmptySignature.obtain())
            val safeKeyGenerator = SafeKeyGenerator()
            val safeKey = safeKeyGenerator.getSafeKey(dataCacheKey)
            Log.d(TAG, "safeKey = $safeKey")
            val file = File(context.cacheDir, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR)
            val diskLruCache = DiskLruCache.open(file, 1, 1, DiskCache.Factory.DEFAULT_DISK_CACHE_SIZE.toLong())
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