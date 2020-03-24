package cc.shinichi.library.view.listener;

import android.app.Activity;
import android.view.View;

/**
 * @author 工藤
 * @email 18883840501@163.com
 * cc.shinichi.library.view.listener
 * create at 2018/12/19  16:23
 * description:
 */
public interface OnBigImageClickListener {

    /**
     * 点击事件
     */
    void onClick(Activity activity, View view, int position);
}