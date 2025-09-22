package cc.shinichi.library.tool.common

import android.content.Context
import androidx.media3.common.util.UnstableApi
import cc.shinichi.library.GlobalContext

/**
 * 文件名: UIUtil.java
 * 作者: kirito
 * 描述: UI工具
 * 创建时间: 2024/11/25
 */
object UIUtil {

    fun dp2px(dp: Float): Int {
        val scale = GlobalContext.getContext().resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    fun px2dp(px: Int): Float {
        val scale = GlobalContext.getContext().resources.displayMetrics.density
        return px / scale + 0.5f
    }

    fun sp2px(sp: Float): Int {
        val scale = GlobalContext.getContext().resources.displayMetrics.scaledDensity
        return (sp * scale + 0.5f).toInt()
    }

    fun px2sp(px: Int): Float {
        val scale = GlobalContext.getContext().resources.displayMetrics.scaledDensity
        return px / scale + 0.5f
    }
}