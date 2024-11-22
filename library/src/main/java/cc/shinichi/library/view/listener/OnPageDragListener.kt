package cc.shinichi.library.view.listener

import android.view.MotionEvent

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 * cc.shinichi.library.view.listener
 * create at 2018/12/19  16:23
 * description: 页面拖拽回调
 */
abstract class OnPageDragListener {
    abstract fun onDrag(event: MotionEvent?, translationY: Float)
}