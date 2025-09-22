package cc.shinichi.library

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * 文件名: InitProvider.java
 * 作者: kirito
 * 描述: 初始化
 * 创建时间: 2024/11/27
 */
@UnstableApi
class InitProvider : ContentProvider() {

    private var application: Application? = null

    override fun onCreate(): Boolean {
        // 获取 Application 实例
        val application = context?.applicationContext as? Application
        if (application != null) {
            // 在这里进行初始化操作
            this.application = application
            initializeLibrary(application)
        }
        return true // 返回 true 表示成功初始化
    }

    fun getApplication(): Application? {
        return application
    }

    private fun initializeLibrary(application: Application) {
        // downloadDirectory
        val downloadDirectory = File(application.cacheDir, "media_cache")
        // maxBytes 500MB
        val maxBytes = 500 * 1024 * 1024L
        // Note: This should be a singleton in your app.
        val databaseProvider = StandaloneDatabaseProvider(application)
        // An on-the-fly cache should evict media when reaching a maximum disk space limit.
        val cache = SimpleCache(
            downloadDirectory,
            LeastRecentlyUsedCacheEvictor(maxBytes),
            databaseProvider
        )
        val dataSourceFactory = DefaultDataSource.Factory(
            application,
            DefaultHttpDataSource.Factory()
        )
        // Configure the DataSource.Factory with the cache and factory for the desired HTTP stack.
        val cacheDataSourceFactory =
            CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(dataSourceFactory)
        GlobalContext.init(application, cacheDataSourceFactory)
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?
    ): Int = 0

    override fun getType(uri: Uri): String? = null
}