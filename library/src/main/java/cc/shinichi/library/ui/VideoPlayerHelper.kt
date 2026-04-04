package cc.shinichi.library.ui

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import cc.shinichi.library.ImagePreview
import cc.shinichi.library.R
import cc.shinichi.library.model.ImageInfo
import cc.shinichi.library.util.SLog
import cc.shinichi.library.util.UtilExt.isLocalFile
import java.io.File
import java.util.Locale

/**
 * 视频播放辅助类
 *
 * 封装了所有 ExoPlayer 相关逻辑。此类仅在确认 ExoPlayer 可用时才会被加载，
 * 从而避免在未引入 ExoPlayer 依赖时产生 ClassNotFoundException。
 *
 * @author kirito
 */
@UnstableApi
internal class VideoPlayerHelper(
    private val container: FrameLayout,
    private val progressBar: ProgressBar?,
    private val activity: ImagePreviewActivity
) {

    companion object {
        private const val TAG = "VideoPlayerHelper"
    }

    private var exoPlayer: ExoPlayer? = null
    private var videoView: PlayerView? = null
    private var ivPlayButton: ImageView? = null
    private var tvPlayTime: TextView? = null
    private var seekBar: SeekBar? = null
    private var progressHandler: Handler? = null
    private var progressRunnable: Runnable? = null
    private var isDragging = false
    var onPausePlaying = false

    /**
     * 初始化视频播放器和 PlayerView
     */
    fun initVideoType(imageInfo: ImageInfo, position: Int) {
        // 创建 PlayerView 并添加到容器
        if (videoView == null) {
            videoView = PlayerView(container.context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setControllerLayoutId(R.layout.sh_media_controller)
                controllerShowTimeoutMs = 1500
            }
            container.addView(videoView)

            ivPlayButton = videoView?.findViewById(R.id.ivPlayButton)
            seekBar = videoView?.findViewById(R.id.seekbar)
            tvPlayTime = videoView?.findViewById(R.id.tvPlayTime)

            ivPlayButton?.setOnClickListener {
                videoView?.player?.let {
                    if (it.isPlaying) {
                        it.pause()
                        ivPlayButton?.setImageResource(R.drawable.icon_video_play)
                    } else {
                        it.play()
                        ivPlayButton?.setImageResource(R.drawable.icon_video_stop)
                    }
                }
            }

            videoView?.setOnLongClickListener { v ->
                ImagePreview.instance.bigImageLongClickListener?.onLongClick(
                    activity, v, position
                )
                true
            }
        }

        progressBar?.visibility = android.view.View.GONE

        // 初始化播放器
        if (exoPlayer == null) {
            exoPlayer = activity.getExoPlayer(activity) as ExoPlayer
            exoPlayer?.addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    super.onVideoSizeChanged(videoSize)
                    SLog.d(TAG, "onVideoSizeChanged: videoSize = ${videoSize.width} * ${videoSize.height}")
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    SLog.d(TAG, "onIsPlayingChanged: isPlaying = $isPlaying")
                    if (isPlaying) {
                        ivPlayButton?.setImageResource(R.drawable.icon_video_stop)
                    } else {
                        ivPlayButton?.setImageResource(R.drawable.icon_video_play)
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    SLog.d(TAG, "onPlaybackStateChanged: playbackState = $playbackState")
                    if (playbackState == Player.STATE_READY) {
                        setProgress(exoPlayer)
                        videoView?.hideController()
                    } else if (playbackState == Player.STATE_ENDED) {
                        exoPlayer?.pause()
                        exoPlayer?.seekTo(0)
                    }
                    if (playbackState == Player.STATE_BUFFERING) {
                        progressBar?.visibility = android.view.View.VISIBLE
                    } else {
                        progressBar?.visibility = android.view.View.GONE
                    }
                }
            })
        }
        videoView?.player = exoPlayer

        if (imageInfo.originUrl.isLocalFile()) {
            val mediaItem = MediaItem.fromUri(Uri.fromFile(File(imageInfo.originUrl)))
            val dataSourceFactory = DefaultDataSource.Factory(activity)
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            exoPlayer?.setMediaSource(mediaSource)
        } else {
            val mediaItem = MediaItem.fromUri(imageInfo.originUrl)
            exoPlayer?.setMediaItem(mediaItem)
        }

        exoPlayer?.prepare()
        exoPlayer?.playWhenReady = false

        if (ImagePreview.instance.index == position) {
            exoPlayer?.play()
        }
    }

    private fun setProgress(exoPlayer: ExoPlayer?) {
        exoPlayer?.apply {
            progressHandler?.removeCallbacksAndMessages(null)
            progressHandler = Handler(Looper.getMainLooper())

            progressRunnable = object : Runnable {
                override fun run() {
                    if (!isDragging) {
                        val currentPosition = exoPlayer.currentPosition
                        val currentTime = formatTimestamp(currentPosition / 1000)
                        val totalDuration = exoPlayer.duration
                        val totalTime = formatTimestamp(totalDuration / 1000)

                        seekBar?.max = totalDuration.toInt()
                        seekBar?.progress = currentPosition.toInt()

                        tvPlayTime?.text = "$currentTime/$totalTime"
                    }
                    progressHandler?.postDelayed(this, 1000)
                }
            }

            progressRunnable?.apply {
                progressHandler?.post(progressRunnable!!)
            }

            seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        exoPlayer.seekTo(progress.toLong())
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isDragging = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    isDragging = false
                }
            })
        }
    }

    private fun formatTimestamp(timestampInSeconds: Long): String {
        val minutes = timestampInSeconds / 60
        val seconds = timestampInSeconds % 60
        return String.format(locale = Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    fun getVideoView(): android.view.View? = videoView

    fun play() {
        exoPlayer?.seekTo(0)
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.isPlaying?.let {
            if (it) {
                exoPlayer?.pause()
            }
        }
    }

    val isPlaying: Boolean
        get() = exoPlayer?.isPlaying == true

    fun refreshUIMargin(context: Context) {
        val llControllerContainer = videoView?.findViewById<LinearLayout>(R.id.llControllerContainer)
        val layoutParams = llControllerContainer?.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        val orientation = context.resources.configuration.orientation
        if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            layoutParams.setMargins(
                0, 0, 0,
                cc.shinichi.library.util.UIUtil.dp2px(70f)
            )
        } else {
            layoutParams.setMargins(
                0, 0, 0,
                cc.shinichi.library.util.UIUtil.dp2px(70f) + cc.shinichi.library.util.PhoneUtil.getNavBarHeight()
            )
        }
        llControllerContainer.layoutParams = layoutParams
    }

    /**
     * 释放播放器资源
     */
    fun release() {
        progressHandler?.removeCallbacksAndMessages(null)
        progressHandler = null
        progressRunnable = null
        exoPlayer?.release()
        exoPlayer = null
    }
}
