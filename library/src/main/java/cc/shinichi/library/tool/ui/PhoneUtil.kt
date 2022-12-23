package cc.shinichi.library.tool.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 * cc.shinichi.drawlongpicturedemo.util
 * create at 2018/8/27  17:53
 * description:
 */
object PhoneUtil {

    fun getPhoneWid(context: Context): Int {
        val resources: Resources = context.applicationContext.resources
        val dm = resources.displayMetrics
        return dm.widthPixels
    }

    fun getPhoneHei(context: Context): Int {
        val resources: Resources = context.applicationContext.resources
        val dm = resources.displayMetrics
        return dm.heightPixels
    }

    fun getPhoneRatio(context: Context): Float {
        return getPhoneHei(context).toFloat() / getPhoneWid(context).toFloat()
    }

    @SuppressLint("InternalInsetResource")
    fun getNavBarHeight(context: Context): Int {
        val resources: Resources = context.resources
        val resourceId: Int = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
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