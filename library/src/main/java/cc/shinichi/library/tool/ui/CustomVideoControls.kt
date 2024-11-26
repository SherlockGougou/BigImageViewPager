package cc.shinichi.library.tool.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import cc.shinichi.library.R
import com.devbrackets.android.exomedia.ui.widget.controls.DefaultVideoControls
import com.devbrackets.android.exomedia.ui.widget.controls.DefaultVideoControls.LoadState
import java.util.*

/**
 * 仓库: <a href="http://git.xiaozhouintel.com/root/C-UI-App-Android">...</a>
 * 文件名: CustomVideoControlsMobile.java
 * 作者: kirito
 * 描述: 自定义视频控制器
 * 创建时间: 2024/11/26
 */
class CustomVideoControls : DefaultVideoControls {
    private lateinit var extraViewsContainer: LinearLayout
    private lateinit var container: ViewGroup

    override val layoutResource: Int
        get() = R.layout.sh_exomedia_controls

    override val extraViews: List<View>
        get() {
            val childCount = extraViewsContainer.childCount
            if (childCount <= 0) {
                return super.extraViews
            }
            val children = LinkedList<View>()
            for (i in 0 until childCount) {
                children.add(extraViewsContainer.getChildAt(i))
            }

            return children
        }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    )

    override fun setup(context: Context) {
        super.setup(context)
        setOnTouchListener(TouchListener(context))
    }

    override fun registerListeners() {
        super.registerListeners()
        seekBar.setOnSeekBarChangeListener(SeekBarChanged())
    }

    override fun retrieveViews() {
        super.retrieveViews()

        extraViewsContainer = findViewById(R.id.exomedia_controls_extra_container)
        container = findViewById(R.id.exomedia_controls_container)
        val bottomMargin = PhoneUtil.getNavBarHeight(context) + UIUtil.dp2px(context, 20f) + UIUtil.dp2px(context, 60f)
        val seekBarParams = seekBar.layoutParams as MarginLayoutParams
        seekBarParams.bottomMargin = bottomMargin
        seekBar.layoutParams = seekBarParams

        // 自定义隐藏一些UI
        titleTextView.visibility = GONE
        subTitleTextView.visibility = GONE
        setPreviousButtonRemoved(true)
        setNextButtonRemoved(true)
        previousButton.visibility = GONE
        nextButton.visibility = GONE
    }

    override fun addExtraView(view: View) {
        extraViewsContainer.addView(view)
    }

    override fun removeExtraView(view: View) {
        extraViewsContainer.removeView(view)
    }

    override fun show() {
        super.show()

        if (videoView?.isPlaying == true) {
            hide(true)
        }
    }

    override fun hideDelayed(delay: Long) {
        if (delay < 0 || currentLoadState != null) {
            return
        }

        //If the user is interacting with controls we don't want to start the delayed hide yet
        if (!userInteracting) {
            visibilityHandler.postDelayed({ animateVisibility(false) }, delay)
        }
    }

    override fun animateVisibility(toVisible: Boolean) {
        if (isVisible == toVisible) {
            return
        }

        val endAlpha = if (toVisible) 1F else 0F
        container.animate().alpha(endAlpha).start()

        isVisible = toVisible
        onVisibilityChanged()
    }

    override fun onLoadStarted(state: LoadState) {
        loadingProgressBar.visibility = View.VISIBLE
        playPauseButton.visibility = View.INVISIBLE

        if (state == LoadState.PREPARING) {
            seekBar.visibility = View.INVISIBLE

            currentTimeTextView.visibility = View.INVISIBLE
            timeSeparatorView.visibility = View.INVISIBLE
            endTimeTextView.visibility = View.INVISIBLE

//            previousButton.visibility = View.INVISIBLE
//            nextButton.visibility = View.INVISIBLE

            extraViewsContainer.visibility = View.INVISIBLE
        }

        show()
    }

    override fun onLoadEnded(state: LoadState?) {
        currentLoadState = null
        loadingProgressBar.visibility = View.GONE
        seekBar.visibility = View.VISIBLE
        container.visibility = View.VISIBLE

        currentTimeTextView.visibility = View.VISIBLE
        timeSeparatorView.visibility = View.VISIBLE
        endTimeTextView.visibility = View.VISIBLE

        playPauseButton.visibility = View.VISIBLE
        playPauseButton.isEnabled = true

//        previousButton.visibility = View.VISIBLE
//        previousButton.isEnabled = enabledViews.get(R.id.exomedia_controls_previous_btn, true)

//        nextButton.visibility = View.VISIBLE
//        nextButton.isEnabled = enabledViews.get(R.id.exomedia_controls_next_btn, true)

        extraViewsContainer.visibility = View.VISIBLE

        updatePlaybackState(videoView?.isPlaying == true)
    }

    /**
     * Listens to the seek bar change events and correctly handles the changes
     */
    private inner class SeekBarChanged : SeekBar.OnSeekBarChangeListener {
        private var seekToTime: Long = 0

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (!fromUser) {
                return
            }

            seekToTime = progress.toLong()
            updatePositionText(seekToTime)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            userInteracting = true

            if (seekListener?.onSeekStarted() != true) {
                internalListener.onSeekStarted()
            }
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            userInteracting = false
            if (seekListener?.onSeekEnded(seekToTime) != true) {
                internalListener.onSeekEnded(seekToTime)
            }
        }
    }

    /**
     * Monitors the view click events to show and hide the video controls if they have been specified.
     */
    private inner class TouchListener(context: Context) : GestureDetector.SimpleOnGestureListener(),
        OnTouchListener {
        private val gestureDetector by lazy {
            GestureDetector(context, this)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            gestureDetector.onTouchEvent(event)
            return true
        }

        override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
            // Toggles between hiding and showing the controls
            if (isVisible) {
                hide(false)
            } else {
                show()
            }

            return true
        }
    }
}