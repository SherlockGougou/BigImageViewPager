package cc.shinichi.library.view.listener

import android.app.Activity
import android.view.View

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 * cc.shinichi.library.view.listener
 * create at 2018/12/19  16:23
 * description:
 */
interface OnBigImageClickListener {
    /**
     * 点击事件
     */
    fun onClick(activity: Activity?, view: View?, position: Int)
}