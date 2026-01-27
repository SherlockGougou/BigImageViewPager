package cc.shinichi.library.tool.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowMetrics
import cc.shinichi.library.GlobalContext


/**
 * 手机屏幕尺寸工具类
 *
 * 使用缓存优化重复调用
 *
 * @author 工藤
 * @email qinglingou@gmail.com
 */
object PhoneUtil {

    private const val TAG = "PhoneUtil"

    // 缓存屏幕尺寸，避免重复计算
    @Volatile
    private var cachedWidth: Int = 0

    @Volatile
    private var cachedHeight: Int = 0

    @Volatile
    private var lastOrientation: Int = Configuration.ORIENTATION_UNDEFINED

    /**
     * 获取屏幕宽度（像素）
     */
    @JvmStatic
    fun getPhoneWid(): Int {
        checkOrientationChanged()
        if (cachedWidth > 0) {
            return cachedWidth
        }

        cachedWidth = getScreenWidth()
        SLog.d(TAG, "getPhoneWid: $cachedWidth")
        return cachedWidth
    }

    /**
     * 获取屏幕高度（像素）
     */
    @JvmStatic
    fun getPhoneHei(): Int {
        checkOrientationChanged()
        if (cachedHeight > 0) {
            return cachedHeight
        }

        cachedHeight = getScreenHeight()
        SLog.d(TAG, "getPhoneHei: $cachedHeight")
        return cachedHeight
    }

    /**
     * 获取屏幕宽高比
     */
    @JvmStatic
    fun getPhoneRatio(): Float {
        val ratio = getPhoneHei().toFloat() / getPhoneWid().toFloat()
        SLog.d(TAG, "getPhoneRatio: $ratio")
        return ratio
    }

    /**
     * 获取导航栏高度
     */
    @JvmStatic
    @SuppressLint("InternalInsetResource")
    fun getNavBarHeight(): Int {
        val resources: Resources = GlobalContext.getContext().resources
        val resourceId: Int = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }

    /**
     * 获取状态栏高度
     */
    @JvmStatic
    @SuppressLint("InternalInsetResource")
    fun getStatusBarHeight(): Int {
        val resources: Resources = GlobalContext.getContext().resources
        val resourceId: Int = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }

    /**
     * 清除缓存（屏幕旋转时调用）
     */
    @JvmStatic
    fun clearCache() {
        cachedWidth = 0
        cachedHeight = 0
        lastOrientation = Configuration.ORIENTATION_UNDEFINED
    }

    private fun checkOrientationChanged() {
        val currentOrientation = GlobalContext.getContext().resources.configuration.orientation
        if (lastOrientation != currentOrientation) {
            lastOrientation = currentOrientation
            cachedWidth = 0
            cachedHeight = 0
        }
    }

    private fun getScreenWidth(): Int {
        val context = GlobalContext.getContext()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics: WindowMetrics = windowManager.currentWindowMetrics
            metrics.bounds.width()
        } else {
            @Suppress("DEPRECATION")
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val dm = DisplayMetrics()
            wm.defaultDisplay.getRealMetrics(dm)
            dm.widthPixels
        }
    }

    private fun getScreenHeight(): Int {
        val context = GlobalContext.getContext()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics: WindowMetrics = windowManager.currentWindowMetrics
            metrics.bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val dm = DisplayMetrics()
            wm.defaultDisplay.getRealMetrics(dm)
            dm.heightPixels
        }
    }
}