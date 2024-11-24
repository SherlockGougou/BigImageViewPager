package cc.shinichi.library.tool.ui;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.view.animation.LinearInterpolator;

/**
 * 仓库: <a href="http://git.xiaozhouintel.com/root/C-UI-App-Android">...</a>
 * 文件名: AnimationUtils.java
 * 作者: kirito
 * 描述:
 * 创建时间: 2024/11/24
 */
public class AnimationUtils {

    public static ValueAnimator createAnimator(float start, float end, long duration, ValueAnimator.AnimatorUpdateListener updateListener, Animator.AnimatorListener listener) {
        ValueAnimator animator = ValueAnimator.ofFloat(start, end);
        animator.setDuration(duration);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(updateListener);
        animator.addListener(listener);
        return animator;
    }
}