package cc.shinichi.library.view.helper

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.ExoDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * 文件名: MediaCacheHelper.java
 * 作者: kirito
 * 描述: 静态内部类模式创建缓存帮助类
 * 创建时间: 2024/11/27
 */
@UnstableApi
object MediaCacheHelper {

    @Volatile
    private var simpleCache: SimpleCache? = null

    fun getCache(context: Context): SimpleCache {
        return simpleCache ?: synchronized(this) {
            simpleCache ?: initializeCache(context).also { simpleCache = it }
        }
    }

    private fun initializeCache(context: Context): SimpleCache {
        val cacheDir = File(context.cacheDir, "media_cache")
        val cacheEvictor = LeastRecentlyUsedCacheEvictor(300L * 1024 * 1024) // 最大缓存
        val databaseProvider = ExoDatabaseProvider(context)
        return SimpleCache(cacheDir, cacheEvictor, databaseProvider)
    }
}