package cc.shinichi.library.tool.common

import android.util.Log
import cc.shinichi.library.BuildConfig

/**
 * 文件名: SLog.java
 * 作者: kirito
 * 描述: 内部日志打印，可以根据是否debug来决定是否打印
 * 创建时间: 2024/11/22
 */
object SLog {

    private const val TAG = "SLog"
    private val isDebug = BuildConfig.DEBUG

    fun d(msg: String) {
        if (isDebug) {
            Log.d(TAG, msg)
        }
    }

    fun e(msg: String) {
        if (isDebug) {
            Log.e(TAG, msg)
        }
    }

    fun i(msg: String) {
        if (isDebug) {
            Log.i(TAG, msg)
        }
    }

    fun w(msg: String) {
        if (isDebug) {
            Log.w(TAG, msg)
        }
    }

    fun v(msg: String) {
        if (isDebug) {
            Log.v(TAG, msg)
        }
    }

    fun d(tag: String, msg: String) {
        if (isDebug) {
            Log.d(tag, msg)
        }
    }

    fun e(tag: String, msg: String) {
        if (isDebug) {
            Log.e(tag, msg)
        }
    }

    fun i(tag: String, msg: String) {
        if (isDebug) {
            Log.i(tag, msg)
        }
    }

    fun w(tag: String, msg: String) {
        if (isDebug) {
            Log.w(tag, msg)
        }
    }

    fun v(tag: String, msg: String) {
        if (isDebug) {
            Log.v(tag, msg)
        }
    }

    fun d(tag: String, msg: String, tr: Throwable) {
        if (isDebug) {
            Log.d(tag, msg, tr)
        }
    }

    fun e(tag: String, msg: String, tr: Throwable) {
        if (isDebug) {
            Log.e(tag, msg, tr)
        }
    }

    fun i(tag: String, msg: String, tr: Throwable) {
        if (isDebug) {
            Log.i(tag, msg, tr)
        }
    }

    fun w(tag: String, msg: String, tr: Throwable?) {
        if (isDebug) {
            Log.w(tag, msg, tr)
        }
    }
}