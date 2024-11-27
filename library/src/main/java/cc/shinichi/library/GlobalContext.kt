package cc.shinichi.library

import android.app.Application
import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource

/**
 * 文件名: GlobalContext.java
 * 作者: kirito
 * 描述: 全局
 * 创建时间: 2024/11/27
 */
@UnstableApi
object GlobalContext {
    private var application: Application? = null
    private var cacheDataSourceFactory: CacheDataSource.Factory? = null

    fun init(app: Application, cacheDataSourceFactory: CacheDataSource.Factory) {
        if (application == null) {
            application = app
        }
        if (this.cacheDataSourceFactory == null) {
            this.cacheDataSourceFactory = cacheDataSourceFactory
        }
    }

    fun getApplication(): Application {
        return application ?: throw IllegalStateException("Application is not initialized")
    }

    fun getCacheDataSourceFactory(): CacheDataSource.Factory {
        return cacheDataSourceFactory ?: throw IllegalStateException("CacheDataSourceFactory is not initialized")
    }

    fun getContext(): Context {
        return getApplication().applicationContext
    }
}