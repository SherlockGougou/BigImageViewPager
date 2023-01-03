package cc.shinichi.library.tool.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val dm = DisplayMetrics()
            display.getRealMetrics(dm)
            screenWidth = dm.widthPixels
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            screenWidth = try {
                Display::class.java.getMethod("getRawWidth").invoke(display) as Int
            } catch (e: Exception) {
                val dm = DisplayMetrics()
                display.getMetrics(dm)
                dm.widthPixels
            }
        }
        return screenWidth.apply {
            Log.d(TAG, "getPhoneWid: $this")
        }
    }

    fun getPhoneHei(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        var screenHeight = 0

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val dm = DisplayMetrics()
            display.getRealMetrics(dm)
            screenHeight = dm.heightPixels
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            screenHeight = try {
                Display::class.java.getMethod("getRawHeight").invoke(display) as Int
            } catch (e: java.lang.Exception) {
                val dm = DisplayMetrics()
                display.getMetrics(dm)
                dm.heightPixels
            }
        }
        return screenHeight.apply {
            Log.d(TAG, "getPhoneHei: $this")
        }
    }

    fun getPhoneRatio(context: Context): Float {
        return (getPhoneHei(context).toFloat() / getPhoneWid(context).toFloat()).apply {
            Log.d(TAG, "getPhoneRatio: $this")
        }
    }

    @SuppressLint("InternalInsetResource")
    fun getNavBarHeight(context: Context): Int {
        val resources: Resources = context.resources
        val resourceId: Int = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }
}