package cc.shinichi.library.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import cc.shinichi.library.ImagePreview
import cc.shinichi.library.ImagePreview.LoadStrategy
import cc.shinichi.library.R
import cc.shinichi.library.callback.SimpleOnImageEventListener
import cc.shinichi.library.core.GlobalContext
import cc.shinichi.library.loader.FileTarget
import cc.shinichi.library.loader.GlideExt
import cc.shinichi.library.loader.ImageLoader.getGlideCacheFile
import cc.shinichi.library.model.ImageInfo
import cc.shinichi.library.model.Type
import cc.shinichi.library.ui.photoview.PhotoView
import cc.shinichi.library.ui.subsampling.ImageSource
import cc.shinichi.library.ui.subsampling.SubsamplingScaleImageView
import cc.shinichi.library.ui.widget.DragCloseView
import cc.shinichi.library.video.VideoPlayerSession
import cc.shinichi.library.video.VideoRuntimeRegistry
import cc.shinichi.library.util.FileUtil.getAvailableCacheDir
import cc.shinichi.library.util.HttpUtil.downloadFile
import cc.shinichi.library.util.ImageUtil
import cc.shinichi.library.util.NetworkUtil.isWiFi
import cc.shinichi.library.util.PhoneUtil
import cc.shinichi.library.util.PhoneUtil.getPhoneHei
import cc.shinichi.library.util.SLog
import cc.shinichi.library.util.ToastUtil
import cc.shinichi.library.util.UIUtil
import cc.shinichi.library.util.UtilExt.isLocalFile
import cc.shinichi.library.util.VideoPlayerHelper
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import java.io.File
import java.util.Locale
import kotlin.math.abs

/**
 * 文件名: ImagePreviewFragment.java
 * 作者: kirito
 * 描述: 单个页面
 * 创建时间: 2024/11/25
 */
class ImagePreviewFragment : Fragment() {

    /**
     * 当前是否加载过
     */
    private var mLoading = false

    private var imagePreviewActivity: ImagePreviewActivity? = null
    private var imageInfo: ImageInfo? = null
    private var position: Int = 0

    private var dragCloseView: DragCloseView? = null
    private var imageSubsample: SubsamplingScaleImageView? = null
    private var imagePhotoView: PhotoView? = null
    private var videoViewContainer: FrameLayout? = null
    private var playerView: View? = null
    private var progressBar: ProgressBar? = null

    private var videoSession: VideoPlayerSession? = null

    private var ivPlayButton: ImageView? = null
    private var tvPlayTime: TextView? = null
    private var seekBar: SeekBar? = null

    private var progressHandler: Handler? = null
    private var progressRunnable: Runnable? = null
    private var isDragging = false
    private var onPausePlaying = false

    // 用于保存缩略图的缩放状态，以便原图加载成功后恢复
    private var savedScale: Float? = null
    private var savedCenter: PointF? = null
    private var savedImageWidth: Int? = null
    private var savedImageHeight: Int? = null

    companion object {
        private const val TAG = "ImagePreviewFragment"
        fun newInstance(
            position: Int,
            imageInfo: ImageInfo
        ): ImagePreviewFragment {
            val fragment = ImagePreviewFragment()
            val bundle = Bundle()
            bundle.putInt("position", position)
            bundle.putSerializable("imageInfo", imageInfo)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mLoading = false
        val view = inflater.inflate(R.layout.sh_item_photoview, container, false)
        initView(view)
        return view
    }

    private fun initData() {
        initParams()
        SLog.d(TAG, "initData: position = $position")
        val type = imageInfo?.type
        if (type == Type.IMAGE) {
            initImageType()
        } else if (type == Type.VIDEO) {
            if (VideoPlayerHelper.isVideoPlaybackSupported()) {
                initVideoType()
            } else {
                initVideoUnsupported()
            }
        }
    }

    private fun initParams() {
        imagePreviewActivity = activity as ImagePreviewActivity
        arguments?.let {
            position = it.getInt("position", 0)
            imageInfo = it.getSerializable("imageInfo") as ImageInfo
        }
        SLog.d(TAG, "initParams: position = $position, imageInfo = $imageInfo")
        if ((imagePreviewActivity?.isFinishing == true) or (imagePreviewActivity?.isDestroyed == true)) {
            return
        }
        if (imageInfo == null) {
            imagePreviewActivity?.finish()
            return
        }
    }


    private fun initView(view: View) {
        progressBar = view.findViewById(R.id.progress_view)
        dragCloseView = view.findViewById(R.id.fingerDragHelper)
        imageSubsample = view.findViewById(R.id.static_view)
        imagePhotoView = view.findViewById(R.id.anim_view)
        videoViewContainer = view.findViewById(R.id.video_view_container)
        // 视频子控件将在 initVideoType() 中初始化（PlayerView 动态创建时）
        val phoneHei = getPhoneHei()
        // 手势拖拽事件
        if (ImagePreview.instance.isEnableDragClose) {
            dragCloseView?.setOnAlphaChangeListener(object : DragCloseView.OnAlphaChangedListener {
                override fun onTranslationYChanged(event: MotionEvent?, translationY: Float) {
                    imagePreviewActivity?.parentView?.apply {
                        if (translationY > 0) {
                            ImagePreview.instance.onPageDragListener?.onDrag(
                                imagePreviewActivity!!,
                                imagePreviewActivity?.parentView!!,
                                event,
                                translationY
                            )
                        } else {
                            ImagePreview.instance.onPageDragListener?.onDragEnd(
                                imagePreviewActivity!!,
                                imagePreviewActivity?.parentView!!
                            )
                        }
                    }
                    val yAbs = abs(translationY)
                    val percent = yAbs / phoneHei
                    val number = 1.0f - percent
                    imagePreviewActivity?.setAlpha(number)
                    if (imagePhotoView?.isVisible == true) {
                        imagePhotoView?.scaleY = number
                        imagePhotoView?.scaleX = number
                    }
                    if (imageSubsample?.isVisible == true) {
                        imageSubsample?.scaleY = number
                        imageSubsample?.scaleX = number
                    }
                    if (videoViewContainer?.isVisible == true) {
                        videoViewContainer?.scaleY = number
                        videoViewContainer?.scaleX = number
                    }
                }

                override fun onExit() {
                    imagePreviewActivity?.setAlpha(0f)
                }
            })
        }
        // 点击事件(视频类型不支持点击关闭)
        imageSubsample?.setOnClickListener { v ->
            if (ImagePreview.instance.isEnableClickClose) {
                imagePreviewActivity?.onBackPressed()
            }
            ImagePreview.instance.bigImageClickListener?.onClick(imagePreviewActivity!!, v, position)
        }
        // 设置状态监听器，实时保存用户的缩放和拖动状态
        imageSubsample?.setOnStateChangedListener(object : SubsamplingScaleImageView.DefaultOnStateChangedListener() {
            override fun onScaleChanged(newScale: Float, origin: Int) {
                // 保存当前缩放状态
                imageSubsample?.let { ssiv ->
                    if (ssiv.isReady) {
                        savedScale = ssiv.scale
                        savedCenter = ssiv.center
                        savedImageWidth = ssiv.sWidth
                        savedImageHeight = ssiv.sHeight
                        SLog.d(TAG, "onScaleChanged: savedScale = $savedScale, savedCenter = $savedCenter, savedImageSize = ${savedImageWidth}x${savedImageHeight}")
                    }
                }
            }

            override fun onCenterChanged(newCenter: PointF?, origin: Int) {
                // 保存当前中心点位置
                imageSubsample?.let { ssiv ->
                    if (ssiv.isReady) {
                        savedScale = ssiv.scale
                        savedCenter = ssiv.center
                        savedImageWidth = ssiv.sWidth
                        savedImageHeight = ssiv.sHeight
                        SLog.d(TAG, "onCenterChanged: savedScale = $savedScale, savedCenter = $savedCenter, savedImageSize = ${savedImageWidth}x${savedImageHeight}")
                    }
                }
            }
        })
        imagePhotoView?.setOnClickListener { v ->
            if (ImagePreview.instance.isEnableClickClose) {
                imagePreviewActivity?.onBackPressed()
            }
            ImagePreview.instance.bigImageClickListener?.onClick(imagePreviewActivity!!, v, position)
        }
        // 播放按钮的点击事件将在 initVideoType() 中设置
        // 长按事件
        ImagePreview.instance.bigImageLongClickListener?.let {
            imageSubsample?.setOnLongClickListener { v ->
                dispatchLongClick(v)
            }
            imagePhotoView?.setOnLongClickListener { v ->
                dispatchLongClick(v)
            }
            videoViewContainer?.setOnLongClickListener { v ->
                dispatchLongClick(v)
            }
        }
    }

    private fun dispatchLongClick(v: View): Boolean {
        val activity = imagePreviewActivity
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            SLog.w(TAG, "dispatchLongClick ignored: activity is not running")
            return true
        }
        ImagePreview.instance.bigImageLongClickListener?.onLongClick(activity, v, position)
        return true
    }

    private fun initImageType() {
        // 图片类型，隐藏视频
        videoViewContainer?.visibility = View.GONE

        val originPathUrl = imageInfo?.originUrl
        val thumbPathUrl = imageInfo?.thumbnailUrl

        imageSubsample?.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
        imageSubsample?.setDoubleTapZoomStyle(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER)
        imageSubsample?.setDoubleTapZoomDuration(ImagePreview.instance.zoomTransitionDuration)

        imagePhotoView?.setZoomTransitionDuration(ImagePreview.instance.zoomTransitionDuration)
        imagePhotoView?.minimumScale = ImagePreview.instance.minScale
        imagePhotoView?.maximumScale = ImagePreview.instance.maxScale
        imagePhotoView?.scaleType = ImageView.ScaleType.FIT_CENTER

        // 根据当前加载策略判断，需要加载的url是哪一个
        var finalLoadUrl: String? = ""
        when (ImagePreview.instance.loadStrategy) {
            LoadStrategy.Default -> {
                finalLoadUrl = thumbPathUrl
            }

            LoadStrategy.AlwaysOrigin -> {
                finalLoadUrl = originPathUrl
            }

            LoadStrategy.AlwaysThumb -> {
                finalLoadUrl = thumbPathUrl
            }

            LoadStrategy.NetworkAuto -> {
                finalLoadUrl = if (isWiFi(imagePreviewActivity!!)) {
                    originPathUrl
                } else {
                    thumbPathUrl
                }
            }

            LoadStrategy.Auto -> {
                finalLoadUrl = if (isWiFi(imagePreviewActivity!!)) {
                    originPathUrl
                } else {
                    thumbPathUrl
                }
            }
        }
        finalLoadUrl = finalLoadUrl?.trim()

        // 显示加载圈圈
        progressBar?.visibility = View.VISIBLE

        // 判断原图缓存是否存在，存在的话，直接显示原图缓存，优先保证清晰。
        val cacheFile = getGlideCacheFile(imagePreviewActivity!!, originPathUrl)
        if (cacheFile != null && cacheFile.exists()) {
            SLog.d(TAG, "initImageType: original exist, originPathUrl = $originPathUrl")
            loadLocalImage(originPathUrl.toString(), cacheFile)
        } else {
            SLog.d(TAG, "initImageType: original not exist, finalLoadUrl = $finalLoadUrl")
            loadImage(finalLoadUrl.toString(), originPathUrl.toString())
        }
    }

    /**
     * 视频播放不支持时的处理（ExoPlayer 未集成）
     */
    private fun initVideoUnsupported() {
        imageSubsample?.visibility = View.GONE
        imagePhotoView?.visibility = View.GONE
        videoViewContainer?.visibility = View.VISIBLE
        progressBar?.visibility = View.GONE

        // 显示提示信息
        val textView = TextView(requireContext()).apply {
            text = context.getString(R.string.video_not_supported)
            setTextColor(android.graphics.Color.WHITE)
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        videoViewContainer?.removeAllViews()
        videoViewContainer?.addView(textView)
        SLog.w(TAG, "Video playback not supported: ExoPlayer (Media3) dependency not found")
    }

    private fun initVideoType() {
        // 视频类型，隐藏图片
        imageSubsample?.visibility = View.GONE
        imagePhotoView?.visibility = View.GONE
        videoViewContainer?.visibility = View.VISIBLE
        progressBar?.visibility = View.GONE

        // 动态创建 PlayerView 并添加到容器
        if (playerView == null) {
            playerView = createPlayerView()
            if (playerView != null) {
                videoViewContainer?.removeAllViews()
                videoViewContainer?.addView(playerView)
                // 初始化视频子控件
                ivPlayButton = playerView?.findViewById(R.id.ivPlayButton)
                seekBar = playerView?.findViewById(R.id.seekbar)
                tvPlayTime = playerView?.findViewById(R.id.tvPlayTime)
                // 设置播放按钮点击事件
                setupPlayButtonListener()
            }
        }

        // 自定义控制
        refreshUIMargin()

        // 初始化播放器
        if (videoSession == null) {
            videoSession = imagePreviewActivity?.createVideoSession()
            val session = videoSession ?: return
            session.setListener(object : VideoPlayerSession.Listener {
                override fun onVideoSizeChanged(width: Int, height: Int) {
                    SLog.d(TAG, "onVideoSizeChanged: videoSize = $width * $height")
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    SLog.d(TAG, "onIsPlayingChanged: isPlaying = $isPlaying")
                    if (isPlaying) {
                        ivPlayButton?.setImageResource(R.drawable.icon_video_stop)
                    } else {
                        ivPlayButton?.setImageResource(R.drawable.icon_video_play)
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    SLog.d(TAG, "onPlaybackStateChanged: playbackState = $playbackState")
                    if (playbackState == VideoPlayerSession.STATE_READY) {
                        // 底部控制器处理
                        setProgress(videoSession)
                        VideoRuntimeRegistry.runtime.hideController(playerView)
                    } else if (playbackState == VideoPlayerSession.STATE_ENDED) {
                        // 播放结束
                        session.pause()
                        session.seekTo(0)
                    }
                    if (playbackState == VideoPlayerSession.STATE_BUFFERING) {
                        // 缓冲中
                        progressBar?.visibility = View.VISIBLE
                    } else {
                        progressBar?.visibility = View.GONE
                    }
                }
            })
        }
        val session = videoSession ?: return
        session.bindPlayerView(playerView)
        session.setMediaSource(
            imageInfo?.originUrl.toString(),
            imageInfo?.originUrl?.isLocalFile() == true
        )
        session.prepare()

        if (ImagePreview.instance.index == position) {
            // 如果是当前选中的，就播放
            session.play()
        }
    }

    /**
     * 动态创建 PlayerView（仅在 ExoPlayer 可用时调用）
     */
    private fun createPlayerView(): View? {
        return VideoRuntimeRegistry.runtime.createPlayerView(requireContext())
    }

    /**
     * 设置播放按钮的点击监听器
     */
    private fun setupPlayButtonListener() {
        ivPlayButton?.setOnClickListener {
            val session = videoSession ?: return@setOnClickListener
            if (session.isPlaying()) {
                session.pause()
                ivPlayButton?.setImageResource(R.drawable.icon_video_play)
            } else {
                session.play()
                ivPlayButton?.setImageResource(R.drawable.icon_video_stop)
            }
        }
    }

    private fun setProgress(session: VideoPlayerSession?) {
        val player = session ?: return
        // 清除之前的任务
        progressHandler?.removeCallbacksAndMessages(null)
        progressHandler = Handler(Looper.getMainLooper())

        // 定义任务
        progressRunnable = object : Runnable {
            override fun run() {
                if (!isDragging) {
                    val currentPosition = player.currentPosition()
                    val currentTime = formatTimestamp(currentPosition / 1000)
                    val totalDuration = player.duration()
                    val totalTime = formatTimestamp(totalDuration / 1000)

                    seekBar?.max = totalDuration.toInt()
                    seekBar?.progress = currentPosition.toInt()

                    tvPlayTime?.text = "$currentTime/$totalTime"
                }
                // 每秒更新一次
                progressHandler?.postDelayed(this, 1000)
            }
        }

        // 开始任务
        progressRunnable?.apply {
            progressHandler?.post(progressRunnable!!)
        }

        // 设置 SeekBar 的监听器
        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // 如果是用户拖动的，则更新播放位置。
                    player.seekTo(progress.toLong())
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

    private fun formatTimestamp(timestampInSeconds: Long): String {
        val minutes = timestampInSeconds / 60
        val seconds = timestampInSeconds % 60
        return String.format(locale = Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    fun onOriginal() {
        if (imageInfo?.type == Type.IMAGE) {
            SLog.d(TAG, "onOriginal: load image")
            loadOriginal()
        } else {
            SLog.d(TAG, "onOriginal: load video nothing to do")
        }
    }

    private fun loadOriginal() {
        val originalUrl = imageInfo?.originUrl.toString()
        val cacheFile = getGlideCacheFile(imagePreviewActivity!!, imageInfo?.originUrl) ?: return
        if (!cacheFile.exists()) return

        val imagePath = cacheFile.absolutePath
        val isLoadWithSubsample = ImageUtil.isLoadWithSubsampling(originalUrl, imagePath)

        if (isLoadWithSubsample) {
            SLog.d(TAG, "loadOriginal -> loadImageWithSubsample")
            loadOriginalWithSubsample(originalUrl, cacheFile)
        } else {
            SLog.d(TAG, "loadOriginal -> loadImageWithPhotoView")
            loadOriginalWithPhotoView(originalUrl, cacheFile)
        }
    }

    /**
     * 使用 SubsamplingScaleImageView 加载原图（适用于大图）
     */
    private fun loadOriginalWithSubsample(originalUrl: String, cacheFile: File) {
        SubsamplingScaleImageView.setPreferredBitmapConfig(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Bitmap.Config.ARGB_4444
            else Bitmap.Config.ARGB_8888
        )

        imagePhotoView?.visibility = View.GONE
        imageSubsample?.visibility = View.VISIBLE

        val imagePath = cacheFile.absolutePath

        // 尝试加载缩略图作为预览
        val thumbnailUrl = imageInfo?.thumbnailUrl.toString()
        val smallCacheFile = getGlideCacheFile(imagePreviewActivity!!, thumbnailUrl)
        val smallImageSource = smallCacheFile?.takeIf { it.exists() }?.let { file ->
            val smallImagePath = file.absolutePath
            val bitmap = ImageUtil.getImageBitmap(smallImagePath, ImageUtil.getBitmapDegree(smallImagePath))
            bitmap?.let { bmp ->
                val wh = ImageUtil.getWidthHeight(smallImagePath)
                ImageSource.bitmap(bmp).also { source ->
                    if (ImageUtil.isBmpImageWithMime(thumbnailUrl, smallImagePath) ||
                        ImageUtil.isAvifImageWithMime(thumbnailUrl, smallImagePath)
                    ) {
                        source.tilingDisabled()
                    }
                    source.dimensions(wh[0], wh[1])
                }
            }
        }

        // 加载原图
        val originSource = ImageSource.uri(imagePath)
        val whOrigin = ImageUtil.getWidthHeight(imagePath)
        val originWidth = whOrigin[0]
        val originHeight = whOrigin[1]
        if (ImageUtil.isBmpImageWithMime(originalUrl, imagePath) ||
            ImageUtil.isAvifImageWithMime(originalUrl, imagePath)
        ) {
            originSource.tilingDisabled()
        }
        originSource.dimensions(originWidth, originHeight)

        imageSubsample?.setImage(originSource, smallImageSource)
        setImageSubsample(imagePath, imageSubsample)

        // 记录当前保存的状态用于恢复（状态已经在用户操作时实时保存了）
        val restoreScale = savedScale
        val restoreCenter = savedCenter
        val restoreWidth = savedImageWidth
        val restoreHeight = savedImageHeight
        SLog.d(TAG, "loadOriginalWithSubsample: saved state - scale=$restoreScale, center=$restoreCenter, size=${restoreWidth}x${restoreHeight}")

        if (restoreScale != null && restoreCenter != null && restoreWidth != null && restoreHeight != null) {
            // 计算缩略图和原图的尺寸比例
            val ratio = originWidth.toFloat() / restoreWidth.toFloat()

            // scale 表示的是 source pixels / view pixels
            val adjustedScale = restoreScale / ratio

            // center 是源图片坐标，需要按比例换算到原图坐标系
            val adjustedCenter = PointF(
                restoreCenter.x * ratio,
                restoreCenter.y * ratio
            )

            SLog.d(TAG, "loadOriginalWithSubsample: restoring state - scale=$adjustedScale, center=$adjustedCenter, size=${originWidth}x${originHeight}")
            imageSubsample?.setScaleAndCenter(adjustedScale, adjustedCenter)
        }

        // 清除保存的状态（避免状态监听器继续更新）
        savedScale = null
        savedCenter = null
        savedImageWidth = null
        savedImageHeight = null
    }

    /**
     * 使用 PhotoView 加载原图（适用于 GIF/动图）
     */
    private fun loadOriginalWithPhotoView(originalUrl: String, cacheFile: File) {
        imageSubsample?.visibility = View.GONE
        imagePhotoView?.visibility = View.VISIBLE

        val isAnimated = ImageUtil.isAnimImageWithMime(originalUrl, cacheFile.absolutePath)

        if (isAnimated) {
            Glide.with(imagePreviewActivity!!)
                .asGif()
                .load(cacheFile)
                .skipMemoryCache(ImagePreview.instance.isSkipLocalCache)
                .diskCacheStrategy(GlideExt.getDiskCacheStrategy())
                .error(ImagePreview.instance.errorPlaceHolder)
                .into(imagePhotoView!!)
        } else {
            Glide.with(imagePreviewActivity!!)
                .load(cacheFile)
                .skipMemoryCache(ImagePreview.instance.isSkipLocalCache)
                .diskCacheStrategy(GlideExt.getDiskCacheStrategy())
                .error(ImagePreview.instance.errorPlaceHolder)
                .into(imagePhotoView!!)
        }
    }

    fun onSelected() {
        if (imageInfo?.type == Type.VIDEO && VideoPlayerHelper.isVideoPlaybackSupported()) {
            val session = videoSession ?: return
            session.seekTo(0)
            session.play()
        }
    }

    fun onUnSelected() {
        if (imageInfo?.type == Type.VIDEO && VideoPlayerHelper.isVideoPlaybackSupported()) {
            val session = videoSession ?: return
            if (session.isPlaying()) {
                session.pause()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshUIMargin()
    }

    private fun refreshUIMargin() {
        val llControllerContainer = playerView?.findViewById<LinearLayout>(R.id.llControllerContainer)
            ?: return
        val layoutParams = llControllerContainer.layoutParams as MarginLayoutParams
        // 获取当前屏幕方向
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 横屏
            layoutParams.setMargins(
                0,
                0,
                0,
                UIUtil.dp2px(70f)
            )
        } else {
            // 竖屏
            layoutParams.setMargins(
                0,
                0,
                0,
                UIUtil.dp2px(70f) + PhoneUtil.getNavBarHeight(
                )
            )
        }
        llControllerContainer.layoutParams = layoutParams
    }

    fun updateItem(imageInfo: ImageInfo) {
        this.imageInfo = imageInfo
        initData()
    }

    override fun onResume() {
        super.onResume()
        if (!mLoading) {
            mLoading = true
            // 初始化时调用一次
            initData()
        } else {
            // 已经初始化过，如果当前是视频，就执行播放
            if (imageInfo?.type == Type.VIDEO && VideoPlayerHelper.isVideoPlaybackSupported()) {
                // 后台前是播放的才恢复播放
                if (onPausePlaying) {
                    videoSession?.play()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        SLog.d(TAG, "onPause: position = $position")
        if (imageInfo?.type == Type.VIDEO && VideoPlayerHelper.isVideoPlaybackSupported()) {
            // 只特殊处理视频类型
            onPausePlaying = videoSession?.isPlaying() == true
            onUnSelected()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mLoading = false
        onRelease()
    }

    private fun loadImage(url: String, originPathUrl: String) {
        if (url.isLocalFile()) {
            // 本地图片，直接加载
            loadLocalImage(url, File(url))
            return
        }

        // 远程图片
        Glide.with(imagePreviewActivity!!)
            .downloadOnly()
            .skipMemoryCache(ImagePreview.instance.isSkipLocalCache)
            .diskCacheStrategy(GlideExt.getDiskCacheStrategy())
            .load(url)
            .addListener(object : RequestListener<File> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<File?>,
                    isFirstResource: Boolean
                ): Boolean {
                    // Glide 加载失败，使用 HTTP 下载后再次加载
                    fallbackDownload(url, originPathUrl, e)
                    return true
                }

                override fun onResourceReady(
                    resource: File,
                    model: Any,
                    target: Target<File>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    loadLocalImage(url, resource)
                    return true
                }
            })
            .into(object : FileTarget() {})
    }

    /**
     * Glide 失败时的降级下载方案
     */
    private fun fallbackDownload(url: String, originPathUrl: String, e: GlideException?) {
        Thread {
            val fileFullName = System.currentTimeMillis().toString()
            val saveDir = getAvailableCacheDir(imagePreviewActivity!!)?.absolutePath +
                    File.separator + "image/"
            val downloadFile = downloadFile(url, fileFullName, saveDir)

            Handler(Looper.getMainLooper()).post {
                if (downloadFile != null && downloadFile.exists() && downloadFile.length() > 0) {
                    loadLocalImage(originPathUrl, downloadFile)
                } else {
                    loadFailed(e)
                }
            }
        }.start()
    }

    private fun loadLocalImage(
        imageUrl: String,
        resource: File
    ) {
        val imagePath = resource.absolutePath
        val isLoadWithSubsample = ImageUtil.isLoadWithSubsampling(imageUrl, imagePath)
        if (isLoadWithSubsample) {
            SLog.d(TAG, "loadLocalImage -> loadImageWithSubsample")
            loadLocalImageWithSubsample(imagePath)
        } else {
            SLog.d(TAG, "loadLocalImage -> loadImageWithPhotoView")
            loadLocalImageWithPhotoView(imageUrl, imagePath)
        }
    }

    /**
     * 加载失败的处理
     */
    private fun loadFailed(
        e: GlideException?
    ) {
        progressBar?.visibility = View.GONE
        imagePhotoView?.visibility = View.GONE
        imageSubsample?.visibility = View.VISIBLE
        imageSubsample?.isZoomEnabled = false
        imageSubsample?.setImage(ImageSource.resource(ImagePreview.instance.errorPlaceHolder))
        if (ImagePreview.instance.isShowErrorToast) {
            var errorMsg = imagePreviewActivity?.getString(R.string.toast_load_failed)
            if (e != null) {
                errorMsg = e.localizedMessage as String
            }
            ToastUtil.showShort(GlobalContext.getContext(), errorMsg)
        }
    }

    private fun setImageSubsample(imagePath: String, imageStatic: SubsamplingScaleImageView?) {
        imageStatic?.orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            SubsamplingScaleImageView.setPreferredBitmapConfig(Bitmap.Config.ARGB_4444)
        } else {
            SubsamplingScaleImageView.setPreferredBitmapConfig(Bitmap.Config.ARGB_8888)
        }
        val tabletOrLandscape = ImageUtil.isTabletOrLandscape(imagePreviewActivity!!)
        if (tabletOrLandscape) {
            // Tablet
            imageStatic?.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
            imageStatic?.minScale = ImagePreview.instance.minScale
            imageStatic?.maxScale = ImagePreview.instance.maxScale
            imageStatic?.setDoubleTapZoomScale(ImagePreview.instance.mediumScale)
        } else {
            // Phone
            imageStatic?.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
            imageStatic?.minScale = 1f
            val isLongImage = ImageUtil.isLongImage(imagePath)
            val isWideImage = ImageUtil.isWideImage(imagePath)
            if (isLongImage) {
                // 长图，高/宽>=3
                imageStatic?.maxScale =
                    ImageUtil.getLongImageMaxZoomScale(imagePreviewActivity!!, imagePath)
                imageStatic?.setDoubleTapZoomScale(
                    ImageUtil.getLongImageDoubleZoomScale(
                        imagePreviewActivity!!,
                        imagePath
                    )
                )
                // 设置长图的默认展示模式：宽度拉满/居中显示
                when (ImagePreview.instance.longPicDisplayMode) {
                    ImagePreview.LongPicDisplayMode.Default -> {
                    }

                    ImagePreview.LongPicDisplayMode.FillWidth -> {
                        imageStatic?.setScaleAndCenter(
                            ImageUtil.getLongImageFillWidthScale(
                                this.imagePreviewActivity!!,
                                imagePath
                            ), PointF(0f, 0f)
                        )
                    }
                }
            } else if (isWideImage) {
                // 宽图，宽/高>=3
                imageStatic?.maxScale =
                    ImageUtil.getWideImageMaxZoomScale(imagePreviewActivity!!, imagePath)
                imageStatic?.setDoubleTapZoomScale(
                    ImageUtil.getWideImageDoubleScale(
                        imagePreviewActivity!!,
                        imagePath
                    )
                )
            } else {
                // 普通图片，其他
                imageStatic?.maxScale =
                    ImageUtil.getStandardImageMaxZoomScale(imagePreviewActivity!!, imagePath)
                imageStatic?.setDoubleTapZoomScale(
                    ImageUtil.getStandardImageDoubleScale(
                        imagePreviewActivity!!,
                        imagePath
                    )
                )
            }
        }
    }

    private fun loadLocalImageWithSubsample(
        imagePath: String
    ) {
        imagePhotoView?.visibility = View.GONE
        imageSubsample?.visibility = View.VISIBLE
        val imageSource = ImageSource.uri(Uri.fromFile(File(imagePath)))
        if (ImageUtil.isBmpImageWithMime(imagePath, imagePath) || ImageUtil.isAvifImageWithMime(imagePath, imagePath)) {
            imageSource.tilingDisabled()
        }
        imageSubsample?.setImage(imageSource)
        imageSubsample?.setOnImageEventListener(object : SimpleOnImageEventListener() {
            override fun onReady() {
                SLog.d(TAG, "loadLocalImageWithSubsample: onReady")
                progressBar?.visibility = View.GONE
            }
        })
        // 缩放适配
        setImageSubsample(imagePath, imageSubsample)
    }

    private fun loadLocalImageWithPhotoView(imageUrl: String, imagePath: String) {
        imageSubsample?.visibility = View.GONE
        imagePhotoView?.visibility = View.VISIBLE

        val isAnimWebpOrAvif = ImageUtil.isAnimWebp(imageUrl, imagePath) ||
                ImageUtil.isAvifImageWithMime(imageUrl, imagePath)
        val isResourceImage = ImageUtil.isResourceImage(imageUrl)

        if (isAnimWebpOrAvif || isResourceImage) {
            // WebP 动图 / AVIF / 资源图片
            loadAnimatedImage(imageUrl, imagePath, isResourceImage)
        } else {
            // GIF 动图
            loadGifImage(imagePath)
        }
    }

    /**
     * 加载动画图片（WebP/AVIF）
     */
    private fun loadAnimatedImage(imageUrl: String, imagePath: String, isResource: Boolean) {
        val fitCenter: Transformation<Bitmap> = FitCenter()
        val loadSource = if (isResource) imageUrl.toUri() else imagePath

        Glide.with(imagePreviewActivity!!)
            .load(loadSource)
            .skipMemoryCache(ImagePreview.instance.isSkipLocalCache)
            .diskCacheStrategy(GlideExt.getDiskCacheStrategy())
            .error(ImagePreview.instance.errorPlaceHolder)
            .optionalTransform(fitCenter)
            .optionalTransform(WebpDrawable::class.java, WebpDrawableTransformation(fitCenter))
            .addListener(createProgressHidingListener<Drawable>())
            .into(imagePhotoView!!)
    }

    /**
     * 加载 GIF 图片
     */
    private fun loadGifImage(imagePath: String) {
        Glide.with(imagePreviewActivity!!)
            .asGif()
            .load(imagePath)
            .skipMemoryCache(ImagePreview.instance.isSkipLocalCache)
            .diskCacheStrategy(GlideExt.getDiskCacheStrategy())
            .error(ImagePreview.instance.errorPlaceHolder)
            .listener(object : RequestListener<GifDrawable?> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<GifDrawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    progressBar?.visibility = View.GONE
                    imageSubsample?.setImage(ImageSource.resource(ImagePreview.instance.errorPlaceHolder))
                    return false
                }

                override fun onResourceReady(
                    resource: GifDrawable,
                    model: Any,
                    target: Target<GifDrawable?>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    progressBar?.visibility = View.GONE
                    return false
                }
            })
            .into(imagePhotoView!!)
    }

    /**
     * 创建隐藏进度条的通用监听器
     */
    private fun <T> createProgressHidingListener(): RequestListener<T> {
        return object : RequestListener<T> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<T?>,
                isFirstResource: Boolean
            ): Boolean {
                progressBar?.visibility = View.GONE
                return false
            }

            override fun onResourceReady(
                resource: T & Any,
                model: Any,
                target: Target<T?>?,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                progressBar?.visibility = View.GONE
                return false
            }
        }
    }

    fun onRelease() {
        // 清理 Handler 任务，防止内存泄漏
        progressHandler?.removeCallbacksAndMessages(null)
        progressHandler = null
        progressRunnable = null

        // 清理保存的缩放状态
        savedScale = null
        savedCenter = null
        savedImageWidth = null
        savedImageHeight = null

        // 释放 SubsamplingScaleImageView
        imageSubsample?.recycle()

        // 释放 PhotoView
        imagePhotoView?.setImageBitmap(null)

        // 释放 ExoPlayer
        if (VideoPlayerHelper.isVideoPlaybackSupported()) {
            releaseExoPlayer()
        }
        videoSession = null
    }

    /**
     * 释放 ExoPlayer 实例（仅在 ExoPlayer 可用时调用）
     */
    private fun releaseExoPlayer() {
        videoSession?.release()
    }
}
