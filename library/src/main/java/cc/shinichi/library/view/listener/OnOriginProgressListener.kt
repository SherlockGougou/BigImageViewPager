package cc.shinichi.library.view.listener

import android.app.Activity
import android.view.View

/**
 * 原图加载百分比接口
 *
 * @author 工藤
 * @email qinglingou@gmail.com
 */
interface OnOriginProgressListener {
    /**
     * 加载中
     */
    fun progress(activity: Activity, parentView: View, progress: Int)

    /**
     * 加载完成
     */
    fun finish(activity: Activity, parentView: View)
}