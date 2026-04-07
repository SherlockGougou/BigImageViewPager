package cc.shinichi.library.core

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.ApplicationInfo
import android.database.Cursor
import android.net.Uri
import cc.shinichi.library.util.SLog
import cc.shinichi.library.video.VideoRuntimeRegistry

/**
 * 文件名: InitProvider.kt
 * 作者: kirito
 * 描述: 自动初始化 Provider，通过 ContentProvider 机制在应用启动时自动初始化库
 * 创建时间: 2024/11/27
 */
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
        // 初始化日志开关，根据应用的调试标志决定
        SLog.isDebug = (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        VideoRuntimeRegistry.runtime.initialize(application)
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