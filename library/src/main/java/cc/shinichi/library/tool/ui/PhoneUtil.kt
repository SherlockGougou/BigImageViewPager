package cc.shinichi.library.tool.ui

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 * cc.shinichi.drawlongpicturedemo.util
 * create at 2018/8/27  17:53
 * description:
 */
object PhoneUtil {

    fun getPhoneWid(context: Context): Int {
        val windowManager = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metric = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metric)
        return metric.widthPixels
    }

    fun getPhoneHei(context: Context): Int {
        val windowManager = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metric = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metric)
        return metric.heightPixels
    }

    fun getPhoneRatio(context: Context): Float {
        return getPhoneHei(context).toFloat() / getPhoneWid(context).toFloat()
    }

    fun px2dp(context: Context, pxValue: Float): Int {
        val scale = context.applicationContext.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    fun dp2px(context: Context, dipValue: Float): Int {
        val scale = context.applicationContext.resources.displayMetrics.density
        return (dipValue * scale + 0.5f).toInt()
    }
}