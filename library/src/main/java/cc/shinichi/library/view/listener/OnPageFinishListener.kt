package cc.shinichi.library.view.listener

import android.app.Activity

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 * cc.shinichi.library.view.listener
 * create at 2018/12/19  16:23
 * description: 页面关闭回调
 */
abstract class OnPageFinishListener {
    abstract fun onFinish(activity: Activity)
}