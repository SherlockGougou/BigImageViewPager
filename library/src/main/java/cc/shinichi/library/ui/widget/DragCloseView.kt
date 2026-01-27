package cc.shinichi.library.ui.widget

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.RelativeLayout
import cc.shinichi.library.ImagePreview
import cc.shinichi.library.ui.photoview.PhotoView
import cc.shinichi.library.ui.subsampling.SubsamplingScaleImageView
import cc.shinichi.library.ui.widget.nine.ViewHelper
import kotlin.math.abs

/**
 * 拖拽关闭辅助 View
 *
 * 支持上下拖拽关闭图片预览
 *
 * @author 工藤
 * @email qinglingou@gmail.com
 */
class DragCloseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "DragCloseView"
        private const val MAX_EXIT_Y = 500
        private const val DURATION = 200L
    }

    private var imageStatic: SubsamplingScaleImageView? = null
    private var imageAnime: PhotoView? = null
    private var videoView: View? = null

    private var downX = 0f
    private var downY = 0f
    private var translationYValue = 0f

    private var alphaChangedListener: OnAlphaChangedListener? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        imageStatic = getChildAt(0) as? SubsamplingScaleImageView
        imageAnime = getChildAt(1) as? PhotoView
        videoView = getChildAt(2)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!ImagePreview.instance.isEnableDragClose) {
            return false
        }

        return when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX
                downY = ev.rawY
                false
            }

            MotionEvent.ACTION_MOVE -> canInterceptDrag(ev)
            else -> false
        }
    }

    private fun canInterceptDrag(ev: MotionEvent): Boolean {
        val diffX = abs(ev.x - downX)
        val diffY = abs(ev.y - downY)

        // Y 轴位移小于 X 轴位移，是横向滑动，不拦截
        if (diffY <= diffX) {
            return false
        }

        parent.requestDisallowInterceptTouchEvent(true)

        return when {
            // 静图
            imageStatic?.visibility == View.VISIBLE -> {
                val isAtEdge = if (ImagePreview.instance.isEnableDragCloseIgnoreScale) {
                    imageStatic!!.scale <= imageStatic!!.minScale + 0.001f || imageStatic!!.isAtYEdge
                } else {
                    imageStatic!!.scale <= imageStatic!!.minScale + 0.001f && imageStatic!!.isAtYEdge
                }
                isAtEdge && (imageStatic!!.maxTouchCount == 0 || imageStatic!!.maxTouchCount == 1)
            }
            // 动图
            imageAnime?.visibility == View.VISIBLE -> {
                imageAnime!!.scale <= imageAnime!!.minimumScale + 0.001f &&
                        (imageAnime!!.maxTouchCount == 0 || imageAnime!!.maxTouchCount == 1)
            }
            // 视频
            videoView?.visibility == View.VISIBLE -> true
            else -> false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
            }

            MotionEvent.ACTION_MOVE -> {
                if (ImagePreview.instance.isEnableDragClose) {
                    val hasVisibleContent = listOf(imageAnime, imageStatic, videoView)
                        .any { it?.visibility == View.VISIBLE }
                    if (hasVisibleContent) {
                        onOneFingerPanActionMove(event)
                    }
                }
            }

            MotionEvent.ACTION_UP -> onActionUp()
        }
        return true
    }

    /**
     * 处理拖拽事件
     */
    private fun onOneFingerPanActionMove(event: MotionEvent) {
        translationYValue = event.rawY - downY
        alphaChangedListener?.onTranslationYChanged(event, translationYValue)
        translationY = translationYValue
    }

    private fun onActionUp() {
        val enableUpDragClose = ImagePreview.instance.isEnableUpDragClose

        val shouldExit = if (enableUpDragClose) {
            abs(translationYValue) > MAX_EXIT_Y
        } else {
            translationYValue > MAX_EXIT_Y
        }

        if (shouldExit) {
            alphaChangedListener?.onExit()
            exit()
        } else {
            resetCallBackAnimation()
        }
    }

    private fun resetCallBackAnimation() {
        ValueAnimator.ofFloat(translationYValue, 0f).apply {
            duration = DURATION
            addUpdateListener { animation ->
                translationYValue = animation.animatedValue as Float
                translationY = translationYValue
            }
            addListener(object : SimpleAnimatorListener() {
                override fun onAnimationEnd(animation: Animator) {
                    translationYValue = 0f
                    reset()
                }
            })
            start()
        }
    }

    private fun exit() {
        val targetY = if (translationYValue > 0) height.toFloat() else -height.toFloat()

        ValueAnimator.ofFloat(translationYValue, targetY).apply {
            addUpdateListener { animation ->
                val fraction = animation.animatedValue as Float
                ViewHelper.setScrollY(this@DragCloseView, -fraction.toInt())
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    (context as? Activity)?.finish()
                }

                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            duration = DURATION
            interpolator = LinearInterpolator()
            start()
        }
    }

    /**
     * 设置透明度变化监听器
     */
    fun setOnAlphaChangeListener(listener: OnAlphaChangedListener?) {
        alphaChangedListener = listener
    }

    private fun reset() {
        alphaChangedListener?.onTranslationYChanged(null, translationYValue)
    }

    /**
     * 透明度变化监听接口
     */
    interface OnAlphaChangedListener {
        /**
         * Y 轴位移变化回调
         */
        fun onTranslationYChanged(event: MotionEvent?, translationY: Float)

        /**
         * 退出回调
         */
        fun onExit()
    }

    /**
     * 简化的动画监听器
     */
    abstract class SimpleAnimatorListener : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator) {}
        override fun onAnimationCancel(animation: Animator) {}
        override fun onAnimationRepeat(animation: Animator) {}
    }
}
