package cc.shinichi.library.view.listener;

/**
 * @author 绿旋
 * Created by 绿旋 on 2020/6/3 10:10.
 */
public interface OnBigImageDeleteListener {
    /**
     * 触发删除, 传递给外部调用者
     * @param position 被删除的图片在list中的position
     */
    void onDelete(int position);
}
