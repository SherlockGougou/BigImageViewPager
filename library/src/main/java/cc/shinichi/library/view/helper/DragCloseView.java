package cc.shinichi.library.view.helper;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.devbrackets.android.exomedia.ui.widget.VideoView;

import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.view.nine.ViewHelper;
import cc.shinichi.library.view.photoview.PhotoView;
import cc.shinichi.library.view.subsampling.SubsamplingScaleImageView;

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 * create at 2018/10/19  11:22
 * description:辅助下拉关闭图片
 */
public class DragCloseView extends RelativeLayout {

    private static final String TAG = DragCloseView.class.getSimpleName();

    private final static int MAX_EXIT_Y = 500;
    private final static long DURATION = 200;

    private SubsamplingScaleImageView imageStatic;
    private PhotoView imageAnime;
    private VideoView videoView;

    private float mDownX;
    private float mDownY;
    private float mTranslationY;

    private onAlphaChangedListener mOnAlphaChangedListener;

    public DragCloseView(Context context) {
        this(context, null);
    }

    public DragCloseView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragCloseView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        imageStatic = (SubsamplingScaleImageView) getChildAt(0);
        imageAnime = (PhotoView) getChildAt(1);
        videoView = (VideoView) getChildAt(2);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ImagePreview.Companion.getInstance().isEnableDragClose()) {
            int action = ev.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                mDownX = ev.getRawX();
                mDownY = ev.getRawY();
            } else if (action == MotionEvent.ACTION_MOVE) {
                return canInterceptDrag(ev);
            }
        }
        return false;
    }

    private boolean canInterceptDrag(MotionEvent ev) {
        float diffX = Math.abs(ev.getX() - mDownX);
        float diffY = Math.abs(ev.getY() - mDownY);
        if (diffY <= diffX) {
            // 下拉手势，Y 轴位移小于 X 轴位移，是横向滑动，不拦截
            return false;
        }
        getParent().requestDisallowInterceptTouchEvent(true);
        if (imageStatic != null && imageStatic.getVisibility() == View.VISIBLE) {
            // 静图
            boolean isAtEdge = ImagePreview.Companion.getInstance().isEnableDragCloseIgnoreScale()
                    ? imageStatic.getScale() <= (imageStatic.getMinScale() + 0.001F) || imageStatic.isAtYEdge()
                    : imageStatic.getScale() <= (imageStatic.getMinScale() + 0.001F) && imageStatic.isAtYEdge();
            return isAtEdge && (imageStatic.getMaxTouchCount() == 0 || imageStatic.getMaxTouchCount() == 1);
        } else if (imageAnime != null && imageAnime.getVisibility() == View.VISIBLE) {
            // 动图
            return imageAnime.getScale() <= (imageAnime.getMinimumScale() + 0.001F) &&
                    (imageAnime.getMaxTouchCount() == 0 || imageAnime.getMaxTouchCount() == 1);
        } else if (videoView != null && videoView.getVisibility() == View.VISIBLE) {
            // 视频
            return true;
        }
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction() & event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getRawX();
                mDownY = event.getRawY();
            case MotionEvent.ACTION_MOVE:
                if (ImagePreview.Companion.getInstance().isEnableDragClose()) {
                    if ((imageAnime != null && imageAnime.getVisibility() == View.VISIBLE)
                            || (imageStatic != null && imageStatic.getVisibility() == View.VISIBLE)
                            || (videoView != null && videoView.getVisibility() == View.VISIBLE)
                    ) {
                        onOneFingerPanActionMove(event);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                onActionUp();
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 处理拖拽事件
     *
     * @param event
     */
    private void onOneFingerPanActionMove(MotionEvent event) {
        float moveY = event.getRawY();
        mTranslationY = moveY - mDownY;
        if (mOnAlphaChangedListener != null) {
            mOnAlphaChangedListener.onTranslationYChanged(event, mTranslationY);
        }
        setTranslationY(mTranslationY);
    }

    private void onActionUp() {
        // 是否启用上拉关闭
        boolean enableUpDragClose = ImagePreview.Companion.getInstance().isEnableUpDragClose();
        if (enableUpDragClose) {
            if (Math.abs(mTranslationY) > MAX_EXIT_Y) {
                exit(mTranslationY);
            } else {
                resetCallBackAnimation();
            }
        } else {
            if (mTranslationY > MAX_EXIT_Y) {
                exit(mTranslationY);
            } else {
                resetCallBackAnimation();
            }
        }
    }

    private void resetCallBackAnimation() {
        ValueAnimator animatorY = ValueAnimator.ofFloat(mTranslationY, 0);
        animatorY.setDuration(DURATION);
        animatorY.addUpdateListener(animation -> {
            mTranslationY = (float) animation.getAnimatedValue();
            setTranslationY(mTranslationY);
        });
        animatorY.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                mTranslationY = 0;
                reset();
            }
        });
        animatorY.start();
    }

    private void exit(float currentY) {
        // 默认的退出动画
        ValueAnimator animatorExit;
        if (currentY > 0) {
            animatorExit = ValueAnimator.ofFloat(mTranslationY, getHeight());
        } else {
            animatorExit = ValueAnimator.ofFloat(mTranslationY, -getHeight());
        }
        animatorExit.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator animation) {
                float fraction = (float) animation.getAnimatedValue();
                ViewHelper.setScrollY(DragCloseView.this, -(int) fraction);
            }
        });
        animatorExit.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animation) {
            }

            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                reset();
                ((Activity) getContext()).finish();
            }

            @Override
            public void onAnimationCancel(@NonNull Animator animation) {
            }

            @Override
            public void onAnimationRepeat(@NonNull Animator animation) {
            }
        });
        animatorExit.setDuration(DURATION);
        animatorExit.setInterpolator(new LinearInterpolator());
        animatorExit.start();
    }

    /**
     * 暴露的回调方法（可根据位移距离或者alpha来改变主UI控件的透明度等
     */
    public void setOnAlphaChangeListener(onAlphaChangedListener alphaChangeListener) {
        mOnAlphaChangedListener = alphaChangeListener;
    }

    private void reset() {
        if (null != mOnAlphaChangedListener) {
            mOnAlphaChangedListener.onTranslationYChanged(null, mTranslationY);
        }
    }

    public abstract static class SimpleAnimatorListener implements Animator.AnimatorListener {
        @Override
        public void onAnimationStart(@NonNull Animator animation) {
        }

        @Override
        public void onAnimationCancel(@NonNull Animator animation) {
        }

        @Override
        public void onAnimationRepeat(@NonNull Animator animation) {
        }
    }

    public interface onAlphaChangedListener {

        void onTranslationYChanged(MotionEvent event, float translationY);
    }
}