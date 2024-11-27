package cc.shinichi.library.tool.common

import android.content.Context

/**
 * 文件名: UIUtil.java
 * 作者: kirito
 * 描述: UI工具
 * 创建时间: 2024/11/25
 */
object UIUtil {

    fun dp2px(context: Context, dp: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    fun px2dp(context: Context, px: Int): Float {
        val scale = context.resources.displayMetrics.density
        return px / scale + 0.5f
    }

    fun sp2px(context: Context, sp: Float): Int {
        val scale = context.resources.displayMetrics.scaledDensity
        return (sp * scale + 0.5f).toInt()
    }

    fun px2sp(context: Context, px: Int): Float {
        val scale = context.resources.displayMetrics.scaledDensity
        return px / scale + 0.5f
    }
}