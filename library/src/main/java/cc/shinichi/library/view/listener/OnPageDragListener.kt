package cc.shinichi.library.view.listener

import android.app.Activity
import android.view.MotionEvent
import android.view.View

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 * cc.shinichi.library.view.listener
 * create at 2018/12/19  16:23
 * description: 页面拖拽回调
 */
abstract class OnPageDragListener {
    abstract fun onDrag(
        activity: Activity,
        parentView: View,
        event: MotionEvent?,
        translationY: Float
    )

    abstract fun onDragEnd(activity: Activity, parentView: View)
}