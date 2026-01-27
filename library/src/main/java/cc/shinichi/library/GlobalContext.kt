package cc.shinichi.library

import android.app.Application
import android.content.Context
import androidx.annotation.MainThread
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 文件名: GlobalContext.kt
 * 作者: kirito
 * 描述: 全局上下文管理器，线程安全实现
 * 创建时间: 2024/11/27
 */
object GlobalContext {

    @Volatile
    private var application: Application? = null

    @Volatile
    @UnstableApi
    private var cacheDataSourceFactory: CacheDataSource.Factory? = null

    private val initialized = AtomicBoolean(false)

    /**
     * 初始化全局上下文
     * 此方法应在应用启动时调用一次
     *
     * @param app Application 实例
     * @param cacheDataSourceFactory 视频缓存数据源工厂
     */
    @UnstableApi
    @MainThread
    @Synchronized
    fun init(app: Application, cacheDataSourceFactory: CacheDataSource.Factory) {
        if (initialized.compareAndSet(false, true)) {
            application = app
            this.cacheDataSourceFactory = cacheDataSourceFactory
        }
    }

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = initialized.get()

    /**
     * 获取 Application 实例
     * @throws IllegalStateException 如果未初始化
     */
    fun getApplication(): Application {
        return application
            ?: throw IllegalStateException("GlobalContext is not initialized. Make sure InitProvider is registered in AndroidManifest.xml")
    }

    /**
     * 获取视频缓存数据源工厂
     * @throws IllegalStateException 如果未初始化
     */
    @UnstableApi
    fun getCacheDataSourceFactory(): CacheDataSource.Factory {
        return cacheDataSourceFactory
            ?: throw IllegalStateException("CacheDataSourceFactory is not initialized. Make sure InitProvider is registered in AndroidManifest.xml")
    }

    /**
     * 获取应用上下文
     */
    fun getContext(): Context = getApplication().applicationContext
}