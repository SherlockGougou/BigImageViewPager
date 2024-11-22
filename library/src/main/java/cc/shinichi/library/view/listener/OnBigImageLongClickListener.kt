package cc.shinichi.library.view.listener

import android.app.Activity
import android.view.View

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 * cc.shinichi.library.view.listener
 * create at 2018/12/19  16:24
 * description:
 */
interface OnBigImageLongClickListener {
    /**
     * 长按事件
     */
    fun onLongClick(activity: Activity?, view: View?, position: Int): Boolean
}