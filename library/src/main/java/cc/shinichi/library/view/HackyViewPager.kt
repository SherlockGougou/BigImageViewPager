package cc.shinichi.library.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager
import cc.shinichi.library.tool.common.SLog

/**
 * 修复 PhotoView 与 ViewPager 手势冲突的 ViewPager
 *
 * ScaleGestureDetector 可能会导致触摸事件异常，
 * 引发 IllegalArgumentException: pointerIndex out of range
 *
 * 此类通过捕获异常来避免崩溃
 *
 * @author 工藤
 * @email qinglingou@gmail.com
 */
class HackyViewPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewPager(context, attrs) {

    companion object {
        private const val TAG = "HackyViewPager"
    }

    /**
     * 是否启用滑动
     */
    var isScrollEnabled = true

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!isScrollEnabled) {
            return false
        }
        return try {
            super.onInterceptTouchEvent(ev)
        } catch (e: IllegalArgumentException) {
            SLog.w(TAG, "onInterceptTouchEvent: IllegalArgumentException caught")
            false
        } catch (e: ArrayIndexOutOfBoundsException) {
            SLog.w(TAG, "onInterceptTouchEvent: ArrayIndexOutOfBoundsException caught")
            false
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!isScrollEnabled) {
            return false
        }
        return try {
            super.onTouchEvent(ev)
        } catch (e: IllegalArgumentException) {
            SLog.w(TAG, "onTouchEvent: IllegalArgumentException caught")
            false
        } catch (e: ArrayIndexOutOfBoundsException) {
            SLog.w(TAG, "onTouchEvent: ArrayIndexOutOfBoundsException caught")
            false
        }
    }
}