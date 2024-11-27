package cc.shinichi.library.tool.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager


/**
 * @author 工藤
 * @email qinglingou@gmail.com
 * cc.shinichi.drawlongpicturedemo.util
 * create at 2018/8/27  17:53
 * description:
 */
object PhoneUtil {

    private val TAG = PhoneUtil::class.java.simpleName

    fun getPhoneWid(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display: Display = wm.defaultDisplay
        var screenWidth = 0
        val dm = DisplayMetrics()
        display.getRealMetrics(dm)
        screenWidth = dm.widthPixels
        return screenWidth.apply {
            SLog.d(TAG, "getPhoneWid: $this")
        }
    }

    fun getPhoneHei(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        var screenHeight = 0

        val dm = DisplayMetrics()
        display.getRealMetrics(dm)
        screenHeight = dm.heightPixels
        return screenHeight.apply {
            SLog.d(TAG, "getPhoneHei: $this")
        }
    }

    fun getPhoneRatio(context: Context): Float {
        return (getPhoneHei(context).toFloat() / getPhoneWid(context).toFloat()).apply {
            SLog.d(TAG, "getPhoneRatio: $this")
        }
    }

    @SuppressLint("InternalInsetResource")
    fun getNavBarHeight(context: Context): Int {
        val resources: Resources = context.resources
        val resourceId: Int = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }

    @SuppressLint("InternalInsetResource")
    fun getStatusBarHeight(context: Context): Int {
        val resources: Resources = context.resources
        val resourceId: Int = resources.getIdentifier("status_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }
}