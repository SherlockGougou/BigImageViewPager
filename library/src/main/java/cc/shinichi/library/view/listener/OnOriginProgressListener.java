package cc.shinichi.library.view.listener;

import android.view.View;

/**
 * 原图加载百分比接口
 *
 * @author SherlockHolmes
 */
public interface OnOriginProgressListener {

    /**
     * 加载中
     */
    void progress(View parentView, int progress);

    /**
     * 加载完成
     */
    void finish(View parentView);
}