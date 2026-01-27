package cc.shinichi.library.util

import android.content.Context
import androidx.annotation.IntRange
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * ExoPlayer 视频缓存管理器
 *
 * 使用 LRU 策略管理视频缓存，支持配置缓存大小
 * 线程安全的单例实现
 *
 * @author kirito
 * @since 2025/11/13
 */
object ExoCacheManager {

    private const val TAG = "ExoCacheManager"
    private const val DEFAULT_CACHE_DIR = "exo_cache"
    private const val DEFAULT_MAX_CACHE_SIZE = 2L * 1024 * 1024 * 1024 // 2GB

    @Volatile
    private var simpleCache: SimpleCache? = null

    @Volatile
    private var maxCacheSize: Long = DEFAULT_MAX_CACHE_SIZE

    /**
     * 配置最大缓存大小
     *
     * @param sizeInMB 缓存大小（MB）
     */
    @JvmStatic
    fun setMaxCacheSize(@IntRange(from = 100) sizeInMB: Long) {
        maxCacheSize = sizeInMB * 1024 * 1024
    }

    /**
     * 获取 SimpleCache 实例（线程安全）
     */
    @JvmStatic
    fun getSimpleCache(context: Context): SimpleCache {
        return simpleCache ?: synchronized(this) {
            simpleCache ?: createSimpleCache(context).also {
                simpleCache = it
                SLog.d(TAG, "SimpleCache created, maxSize=${maxCacheSize / 1024 / 1024}MB")
            }
        }
    }

    private fun createSimpleCache(context: Context): SimpleCache {
        val cacheDir = File(context.cacheDir, DEFAULT_CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
        val cacheEvictor = LeastRecentlyUsedCacheEvictor(maxCacheSize)
        val databaseProvider = StandaloneDatabaseProvider(context)
        return SimpleCache(cacheDir, cacheEvictor, databaseProvider)
    }

    /**
     * 释放缓存资源
     * 应在 Activity 销毁时调用
     */
    @JvmStatic
    fun release() {
        synchronized(this) {
            try {
                simpleCache?.release()
                SLog.d(TAG, "SimpleCache released")
            } catch (e: Exception) {
                SLog.e(TAG, "Error releasing SimpleCache", e)
            } finally {
                simpleCache = null
            }
        }
    }

    /**
     * 检查缓存是否已初始化
     */
    @JvmStatic
    fun isInitialized(): Boolean = simpleCache != null

    /**
     * 获取当前缓存使用量（字节）
     */
    @JvmStatic
    fun getCacheUsage(): Long {
        return simpleCache?.cacheSpace ?: 0L
    }

    /**
     * 获取当前缓存使用量（MB）
     */
    @JvmStatic
    fun getCacheUsageMB(): Long {
        return getCacheUsage() / 1024 / 1024
    }
}