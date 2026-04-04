package cc.shinichi.library.ui

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import cc.shinichi.library.ImagePreview
import cc.shinichi.library.util.ExoCacheManager

/**
 * ExoPlayer Activity 级别辅助类
 *
 * 封装了 Activity 中与 ExoPlayer 相关的缓存管理和播放器创建逻辑。
 * 此类仅在确认 ExoPlayer 可用时才会被加载。
 *
 * @author kirito
 */
@OptIn(UnstableApi::class)
internal class ExoPlayerActivityHelper(context: Context) {

    private var simpleCache: SimpleCache? = ExoCacheManager.getSimpleCache(context)

    /**
     * 创建 ExoPlayer 实例
     */
    fun createExoPlayer(context: Context): ExoPlayer {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(10_000)
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(ImagePreview.instance.headers?.toMap() ?: mapOf())

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(simpleCache!!)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setCacheWriteDataSinkFactory(CacheDataSink.Factory().setCache(simpleCache!!))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    /**
     * 释放缓存资源
     */
    fun release() {
        ExoCacheManager.release()
    }
}
