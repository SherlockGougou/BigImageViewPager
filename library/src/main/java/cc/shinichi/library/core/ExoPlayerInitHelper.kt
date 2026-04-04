package cc.shinichi.library.core

import android.app.Application
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * ExoPlayer 初始化辅助类
 *
 * 封装了 ExoPlayer 相关的缓存数据源工厂创建逻辑。
 * 此类仅在确认 ExoPlayer 可用时才会被加载。
 *
 * @author kirito
 */
@UnstableApi
internal object ExoPlayerInitHelper {

    fun createCacheDataSourceFactory(application: Application): CacheDataSource.Factory {
        val downloadDirectory = File(application.cacheDir, "media_cache")
        val maxBytes = 500 * 1024 * 1024L
        val databaseProvider = StandaloneDatabaseProvider(application)
        val cache = SimpleCache(
            downloadDirectory,
            LeastRecentlyUsedCacheEvictor(maxBytes),
            databaseProvider
        )
        val dataSourceFactory = DefaultDataSource.Factory(
            application,
            DefaultHttpDataSource.Factory()
        )
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(dataSourceFactory)
    }
}
