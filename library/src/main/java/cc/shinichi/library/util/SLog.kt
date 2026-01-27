package cc.shinichi.library.util

import android.util.Log

/**
 * 文件名: SLog.kt
 * 作者: kirito
 * 描述: 内部日志打印工具，根据 DEBUG 模式决定是否打印
 * 创建时间: 2024/11/22
 */
object SLog {

    const val DEFAULT_TAG = "SLog"

    /**
     * 是否启用调试日志，默认 false
     * 可在应用初始化时设置：SLog.isDebug = BuildConfig.DEBUG
     */
    @JvmStatic
    var isDebug: Boolean = false

    // region 无自定义 TAG 的日志方法（Kotlin 推荐用法，延迟计算消息）
    fun d(message: () -> String) {
        if (isDebug) Log.d(DEFAULT_TAG, message())
    }

    fun e(message: () -> String) {
        if (isDebug) Log.e(DEFAULT_TAG, message())
    }

    fun i(message: () -> String) {
        if (isDebug) Log.i(DEFAULT_TAG, message())
    }

    fun w(message: () -> String) {
        if (isDebug) Log.w(DEFAULT_TAG, message())
    }

    fun v(message: () -> String) {
        if (isDebug) Log.v(DEFAULT_TAG, message())
    }
    // endregion

    // region 带自定义 TAG 的日志方法（兼容 Java 调用）
    @JvmStatic
    fun d(msg: String) {
        if (isDebug) Log.d(DEFAULT_TAG, msg)
    }

    @JvmStatic
    fun e(msg: String) {
        if (isDebug) Log.e(DEFAULT_TAG, msg)
    }

    @JvmStatic
    fun i(msg: String) {
        if (isDebug) Log.i(DEFAULT_TAG, msg)
    }

    @JvmStatic
    fun w(msg: String) {
        if (isDebug) Log.w(DEFAULT_TAG, msg)
    }

    @JvmStatic
    fun v(msg: String) {
        if (isDebug) Log.v(DEFAULT_TAG, msg)
    }

    @JvmStatic
    fun d(tag: String, msg: String) {
        if (isDebug) Log.d(tag, msg)
    }

    @JvmStatic
    fun e(tag: String, msg: String) {
        if (isDebug) Log.e(tag, msg)
    }

    @JvmStatic
    fun i(tag: String, msg: String) {
        if (isDebug) Log.i(tag, msg)
    }

    @JvmStatic
    fun w(tag: String, msg: String) {
        if (isDebug) Log.w(tag, msg)
    }

    @JvmStatic
    fun v(tag: String, msg: String) {
        if (isDebug) Log.v(tag, msg)
    }
    // endregion

    // region 带异常的日志方法
    @JvmStatic
    fun d(tag: String, msg: String, tr: Throwable) {
        if (isDebug) Log.d(tag, msg, tr)
    }

    @JvmStatic
    fun e(tag: String, msg: String, tr: Throwable) {
        if (isDebug) Log.e(tag, msg, tr)
    }

    @JvmStatic
    fun i(tag: String, msg: String, tr: Throwable) {
        if (isDebug) Log.i(tag, msg, tr)
    }

    @JvmStatic
    fun w(tag: String, msg: String, tr: Throwable?) {
        if (isDebug) Log.w(tag, msg, tr)
    }
    // endregion
}