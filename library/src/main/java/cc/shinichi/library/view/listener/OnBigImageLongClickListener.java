package cc.shinichi.library.view.listener;

import android.view.View;

/**
 * @author 工藤
 * @email gougou@16fan.com
 * cc.shinichi.library.view.listener
 * create at 2018/12/19  16:24
 * description:
 */
public interface OnBigImageLongClickListener {

    /**
     * 长按事件
     */
    boolean onLongClick(View view, int position);
}