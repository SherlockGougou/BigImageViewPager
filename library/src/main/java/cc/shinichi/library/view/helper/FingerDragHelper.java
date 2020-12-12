package cc.shinichi.library.view.helper;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.R;
import cc.shinichi.library.view.nine.ViewHelper;
import cc.shinichi.library.view.photoview.PhotoView;

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 * com.fan16.cn.widget.ninegrid
 * create at 2018/10/19  11:22
 * description:辅助下拉关闭图片
 */
public class FingerDragHelper extends LinearLayout {

    private static final String TAG = FingerDragHelper.class.getSimpleName();
    private final static int MAX_EXIT_Y = 500;
    private final static long DURATION = 200;
    private static final int MAX_TRANSLATE_Y = 500;
    private final int fadeIn = R.anim.fade_in_150;
    private final int fadeOut = R.anim.fade_out_150;
    private SubsamplingScaleImageViewDragClose imageView;
    private PhotoView imageGif;
    private float mDownY;
    private float mTranslationY;
    private float mLastTranslationY;
    private boolean isAnimate = false;
    private boolean isFirstMoveAfterDown = true;
    private int mTouchslop;
    private onAlphaChangedListener mOnAlphaChangedListener;

    public FingerDragHelper(Context context) {
        this(context, null);
    }

    public FingerDragHelper(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FingerDragHelper(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViews();
    }

    private void initViews() {
        mTouchslop = ViewConfiguration.getTouchSlop();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        imageView = (SubsamplingScaleImageViewDragClose) getChildAt(0);
        imageGif = (PhotoView) getChildAt(1);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean isIntercept = false;
        int action = ev.getAction() & ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDownY = ev.getRawY();
                isFirstMoveAfterDown = false;
            case MotionEvent.ACTION_MOVE:
                if (ImagePreview.getInstance().isEnableDragClose()) {
                    if (imageGif != null && imageGif.getVisibility() == View.VISIBLE) {
                        isIntercept = (imageGif.getScale() <= (imageGif.getMinimumScale() + 0.001F))
                                && (imageGif.getMaxTouchCount() == 0 || imageGif.getMaxTouchCount() == 1)
                                && Math.abs(ev.getRawY() - mDownY) > 2 * mTouchslop;
                    } else if (imageView != null && imageView.getVisibility() == View.VISIBLE) {
                        // 如果设置了忽略缩放，即只要顶部或底部在边上都可拉动关闭
                        if (ImagePreview.getInstance().isEnableDragCloseIgnoreScale()) {
                            isIntercept = ((imageView.getScale() <= (imageView.getMinScale() + 0.001F)) || imageView.atYEdge)
                                    && (imageView.getMaxTouchCount() == 0 || imageView.getMaxTouchCount() == 1)
                                    && Math.abs(ev.getRawY() - mDownY) > 2 * mTouchslop;
                        } else {
                            isIntercept = (imageView.getScale() <= (imageView.getMinScale() + 0.001F))
                                    && (imageView.getMaxTouchCount() == 0 || imageView.getMaxTouchCount() == 1)
                                    && Math.abs(ev.getRawY() - mDownY) > 2 * mTouchslop
                                    && imageView.atYEdge;
                        }
                    }
                    // 我的修改: 和微信表现一样：如果在放大模式并且不在顶部，滑动到顶部的时候这里可以拦截到事件
                    // 此时mDownY应该重置为当前点，否则当前点离真正的按下点很远了直接view就被拖下来很远
                    if (isIntercept && !isFirstMoveAfterDown) {
                        mDownY = ev.getRawY();
                        isFirstMoveAfterDown = true;
                    }
                }
                break;
            default:
                break;
        }
        return isIntercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction() & event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDownY = event.getRawY();
            case MotionEvent.ACTION_MOVE:
                if (ImagePreview.getInstance().isEnableDragClose()) {
                    if (imageGif != null && imageGif.getVisibility() == View.VISIBLE) {
                        onOneFingerPanActionMove(event);
                    } else if (imageView != null && imageView.getVisibility() == View.VISIBLE) {
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

    private void onOneFingerPanActionMove(MotionEvent event) {
        float moveY = event.getRawY();
        mTranslationY = moveY - mDownY + mLastTranslationY;
        //触发回调 根据距离处理其他控件的透明度 显示或者隐藏角标，文字信息等
        if (null != mOnAlphaChangedListener) {
            mOnAlphaChangedListener.onTranslationYChanged(event, mTranslationY);
        }
        ViewHelper.setScrollY(this, -(int) mTranslationY);
    }

    private void onActionUp() {
        // 是否启用上拉关闭
        boolean enableUpDragClose = ImagePreview.getInstance().isEnableUpDragClose();
        if (enableUpDragClose) {
            if (Math.abs(mTranslationY) > MAX_EXIT_Y) {
                exitWithTranslation(mTranslationY);
            } else {
                resetCallBackAnimation();
            }
        } else {
            if (mTranslationY > MAX_EXIT_Y) {
                exitWithTranslation(mTranslationY);
            } else {
                resetCallBackAnimation();
            }
        }
    }

    public void exitWithTranslation(float currentY) {
        if (currentY > 0) {
            ValueAnimator animDown = ValueAnimator.ofFloat(mTranslationY, getHeight());
            animDown.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float fraction = (float) animation.getAnimatedValue();
                    ViewHelper.setScrollY(FingerDragHelper.this, -(int) fraction);
                }
            });
            animDown.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    reset();
                    Activity activity = ((Activity) getContext());
                    activity.finish();
                    activity.overridePendingTransition(fadeIn, fadeOut);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animDown.setDuration(DURATION);
            animDown.setInterpolator(new LinearInterpolator());
            animDown.start();
        } else {
            ValueAnimator animUp = ValueAnimator.ofFloat(mTranslationY, -getHeight());
            animUp.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float fraction = (float) animation.getAnimatedValue();
                    ViewHelper.setScrollY(FingerDragHelper.this, -(int) fraction);
                }
            });
            animUp.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    reset();
                    ((Activity) getContext()).finish();
                    ((Activity) getContext()).overridePendingTransition(fadeIn, fadeOut);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animUp.setDuration(DURATION);
            animUp.setInterpolator(new LinearInterpolator());
            animUp.start();
        }
    }

    private void resetCallBackAnimation() {
        ValueAnimator animatorY = ValueAnimator.ofFloat(mTranslationY, 0);
        animatorY.setDuration(DURATION);
        animatorY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (isAnimate) {
                    mTranslationY = (float) valueAnimator.getAnimatedValue();
                    mLastTranslationY = mTranslationY;
                    ViewHelper.setScrollY(FingerDragHelper.this, -(int) mTranslationY);
                }
            }
        });
        animatorY.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                isAnimate = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (isAnimate) {
                    mTranslationY = 0;
                    invalidate();
                    reset();
                }
                isAnimate = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animatorY.start();
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

    public interface onAlphaChangedListener {

        void onTranslationYChanged(MotionEvent event, float translationY);
    }
}