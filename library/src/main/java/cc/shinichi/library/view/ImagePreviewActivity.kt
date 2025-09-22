package cc.shinichi.library.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import cc.shinichi.library.ImagePreview
import cc.shinichi.library.R
import cc.shinichi.library.bean.ImageInfo
import cc.shinichi.library.bean.Type
import cc.shinichi.library.glide.FileTarget
import cc.shinichi.library.glide.ImageLoader
import cc.shinichi.library.glide.progress.OnProgressListener
import cc.shinichi.library.glide.progress.ProgressManager.addListener
import cc.shinichi.library.tool.common.DeviceUtil
import cc.shinichi.library.tool.common.HandlerHolder
import cc.shinichi.library.tool.common.HttpUtil
import cc.shinichi.library.tool.common.NetworkUtil
import cc.shinichi.library.tool.common.PhoneUtil
import cc.shinichi.library.tool.common.SLog
import cc.shinichi.library.tool.common.ToastUtil
import cc.shinichi.library.tool.common.UIUtil
import cc.shinichi.library.tool.image.DownloadUtil
import cc.shinichi.library.view.listener.OnFinishListener
import com.bumptech.glide.Glide

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 */
class ImagePreviewActivity : AppCompatActivity(), Handler.Callback, View.OnClickListener,
    OnFinishListener {

    private var handlerHolder: HandlerHolder? = null
    private val imageInfoList: MutableList<ImageInfo> = mutableListOf()
    private val fragmentList: MutableList<ImagePreviewFragment> = mutableListOf()

    var parentView: View? = null

    private var viewPager: HackyViewPager? = null
    private var consControllerOverlay: ConstraintLayout? = null
    private var tvIndicator: TextView? = null
    private var consBottomController: ConstraintLayout? = null

    private var fmImageShowOriginContainer: FrameLayout? = null
    private var fmCenterProgressContainer: FrameLayout? = null
    private var btnShowOrigin: Button? = null
    private var imgDownload: ImageView? = null
    private var imgCloseButton: ImageView? = null
    private var rootView: View? = null
    private var progressParentLayout: View? = null

    private var imagePreviewAdapter: ImagePreviewAdapter? = null
    private var pageChangeListener: OnPageChangeListener? = null

    private var isShowDownButton = false
    private var isShowCloseButton = false
    private var isShowOriginButton = false
    private var isShowIndicator = false
    private var isUserCustomProgressView = false
    private var indicatorStatus = false
    private var originalStatus = false
    private var downloadButtonStatus = false
    private var closeButtonStatus = false

    private var currentItem = 0
    private var currentItemOriginPathUrl: String? = ""
    private var lastProgress = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parentView = View.inflate(this, ImagePreview.instance.previewLayoutResId, null)
        setContentView(parentView)
        ImagePreview.instance.onCustomLayoutCallback?.onLayout(this, parentView!!)

        transparentStatusBar()
        transparentNavBar()

        ImagePreview.instance.previewActivity = this
        handlerHolder = HandlerHolder(this)
        imageInfoList.clear()
        imageInfoList.addAll(ImagePreview.instance.getImageInfoList())
        if (imageInfoList.isEmpty()) {
            finish()
            return
        }

        // 回调
        ImagePreview.instance.setOnFinishListener(this)

        currentItem = ImagePreview.instance.index
        isShowDownButton = ImagePreview.instance.isShowDownButton
        isShowCloseButton = ImagePreview.instance.isShowCloseButton
        isShowIndicator = ImagePreview.instance.isShowIndicator
        currentItemOriginPathUrl = imageInfoList[currentItem].originUrl
        isShowOriginButton = ImagePreview.instance.isShowOriginButton(currentItem)
        if (isShowOriginButton) {
            // 检查缓存是否存在
            checkCache(currentItemOriginPathUrl)
        }

        rootView = findViewById(R.id.rootView)
        viewPager = findViewById(R.id.viewPager)
        consControllerOverlay = findViewById(R.id.consControllerOverlay)
        tvIndicator = findViewById(R.id.tv_indicator)
        consBottomController = findViewById(R.id.consBottomController)
        fmImageShowOriginContainer = findViewById(R.id.fm_image_show_origin_container)
        fmCenterProgressContainer = findViewById(R.id.fm_center_progress_container)

        // 顶部和底部margin
        refreshUIMargin()

        fmImageShowOriginContainer?.visibility = View.GONE
        fmCenterProgressContainer?.visibility = View.GONE
        val progressLayoutId = ImagePreview.instance.progressLayoutId
        // != -1 即用户自定义了view
        if (progressLayoutId != -1) {
            // add用户自定义的view到frameLayout中，回调进度和view
            progressParentLayout =
                View.inflate(this@ImagePreviewActivity, ImagePreview.instance.progressLayoutId, null)
            fmCenterProgressContainer?.removeAllViews()
            fmCenterProgressContainer?.addView(progressParentLayout)
            isUserCustomProgressView = true
        } else {
            // 使用默认的textView进行百分比的显示
            isUserCustomProgressView = false
        }
        btnShowOrigin = findViewById(R.id.btn_show_origin)
        imgDownload = findViewById(R.id.img_download)
        imgCloseButton = findViewById(R.id.imgCloseButton)
        imgDownload?.setImageResource(ImagePreview.instance.downIconResId)
        imgCloseButton?.setImageResource(ImagePreview.instance.closeIconResId)

        // 关闭页面按钮
        imgCloseButton?.setOnClickListener(this)
        // 查看与原图按钮
        btnShowOrigin?.setOnClickListener(this)
        // 下载图片按钮
        imgDownload?.setOnClickListener(this)
        indicatorStatus = if (!isShowIndicator) {
            tvIndicator?.visibility = View.GONE
            false
        } else {
            if (imageInfoList.size > 1) {
                tvIndicator?.visibility = View.VISIBLE
                true
            } else {
                tvIndicator?.visibility = View.GONE
                false
            }
        }
        // 设置顶部指示器背景shape
        if (ImagePreview.instance.indicatorShapeResId > 0) {
            tvIndicator?.setBackgroundResource(ImagePreview.instance.indicatorShapeResId)
        }
        // 设置关闭和下载的shape
        if (ImagePreview.instance.closeIconBackgroundResId > 0) {
            imgCloseButton?.setBackgroundResource(ImagePreview.instance.closeIconBackgroundResId)
        }
        if (ImagePreview.instance.downIconBackgroundResId > 0) {
            imgDownload?.setBackgroundResource(ImagePreview.instance.downIconBackgroundResId)
        }
        downloadButtonStatus = if (isShowDownButton) {
            imgDownload?.visibility = View.VISIBLE
            true
        } else {
            imgDownload?.visibility = View.GONE
            false
        }
        closeButtonStatus = if (isShowCloseButton) {
            imgCloseButton?.visibility = View.VISIBLE
            true
        } else {
            imgCloseButton?.visibility = View.GONE
            false
        }

        // 更新进度指示器
        tvIndicator?.text = String.format(
            getString(R.string.indicator),
            (currentItem + 1).toString(),
            (imageInfoList.size).toString()
        )

        // 适配器
        fragmentList.clear()
        for ((index, info) in imageInfoList.withIndex()) {
            fragmentList.add(
                ImagePreviewFragment.newInstance(
                    index,
                    info
                )
            )
        }
        imagePreviewAdapter = ImagePreviewAdapter(supportFragmentManager, fragmentList)
        viewPager?.adapter = imagePreviewAdapter
        viewPager?.offscreenPageLimit = 1
        viewPager?.setCurrentItem(currentItem, false)

        pageChangeListener = object : OnPageChangeListener {
            override fun onPageSelected(position: Int) {
                currentItem = position
                // select回调
                ImagePreview.instance.bigImagePageChangeListener?.onPageSelected(currentItem, imageInfoList)
                ImagePreview.instance.setIndex(currentItem)

                // 判断是否展示查看原图按钮
                if (imageInfoList[currentItem].type == Type.IMAGE) {
                    currentItemOriginPathUrl = imageInfoList[currentItem].originUrl
                    isShowOriginButton = ImagePreview.instance.isShowOriginButton(currentItem)
                    if (isShowOriginButton) {
                        // 检查缓存是否存在
                        checkCache(currentItemOriginPathUrl)
                    } else {
                        gone()
                    }
                } else {
                    gone()
                }

                // 更新进度指示器
                tvIndicator?.text = String.format(
                    getString(R.string.indicator),
                    (currentItem + 1).toString(),
                    (imageInfoList.size).toString()
                )

                // 如果是自定义百分比进度view，每次切换都先隐藏，并重置百分比
                if (isUserCustomProgressView) {
                    fmCenterProgressContainer?.visibility = View.GONE
                    lastProgress = 0
                }

                // 调用非当前页的Fragment的方法
                for (i in fragmentList.indices) {
                    if (i != currentItem) {
                        fragmentList[i].onUnSelected()
                    }
                }
                // 调用当前页的Fragment的方法
                fragmentList[currentItem].onSelected()
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                ImagePreview.instance.bigImagePageChangeListener?.onPageScrolled(
                    position,
                    positionOffset,
                    positionOffsetPixels
                )
            }

            override fun onPageScrollStateChanged(state: Int) {
                ImagePreview.instance.bigImagePageChangeListener?.onPageScrollStateChanged(state)
            }
        }

        pageChangeListener?.apply {
            viewPager?.addOnPageChangeListener(this)
        }
    }

    private fun refreshUIMargin() {
        val layoutParamsIndicator = tvIndicator?.layoutParams as ConstraintLayout.LayoutParams
        val layoutParams = consBottomController?.layoutParams as ConstraintLayout.LayoutParams
        // 获取当前屏幕方向
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 横屏
            // tvIndicator top margin
            val topMargin = UIUtil.dp2px(20f)
            layoutParamsIndicator.setMargins(0, topMargin, 0, 0)

            // consBottomController bottom margin
            val bottomMargin = UIUtil.dp2px(20f)
            layoutParams.setMargins(0, 0, 0, bottomMargin)
        } else {
            // 竖屏
            // tvIndicator top margin
            val topMargin = PhoneUtil.getStatusBarHeight() + UIUtil.dp2px(20f)
            layoutParamsIndicator.setMargins(0, topMargin, 0, 0)

            // consBottomController bottom margin
            val bottomMargin = PhoneUtil.getNavBarHeight() + UIUtil.dp2px(20f)
            layoutParams.setMargins(0, 0, 0, bottomMargin)
        }
        tvIndicator?.layoutParams = layoutParamsIndicator
        consBottomController?.layoutParams = layoutParams
    }

    @OptIn(UnstableApi::class)
    fun getExoPlayer(): ExoPlayer {
        // 创建带自定义 Header 的 HttpDataSource.Factory
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(10_000)
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(ImagePreview.instance.headers?.toMap() ?: mapOf())
        // 创建 ExoPlayer
        val exoPlayer = ExoPlayer.Builder(this@ImagePreviewActivity)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
        return exoPlayer
    }

    /**
     * 下载当前图片/视频到SD卡
     */
    private fun downloadCurrentImg() {
        if (imageInfoList[currentItem].type == Type.IMAGE) {
            DownloadUtil.downloadPicture(this@ImagePreviewActivity, currentItem, currentItemOriginPathUrl)
        } else if (imageInfoList[currentItem].type == Type.VIDEO) {
            DownloadUtil.downloadVideo(this@ImagePreviewActivity, currentItem, currentItemOriginPathUrl)
        }
    }

    override fun onFinish() {
        onBackPressed()
    }

    override fun onBackPressed() {
        finish()
    }

    override fun finish() {
        for (fragment in fragmentList) {
            fragment.onRelease()
        }
        ImagePreview.instance.onPageFinishListener?.onFinish(this)
        ImagePreview.instance.reset()
        super.finish()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 判断屏幕方向变化
        // 顶部和底部margin
        refreshUIMargin()
    }

    private fun convertPercentToBlackAlphaColor(percent: Float): Int {
        val realPercent = 1f.coerceAtMost(0f.coerceAtLeast(percent))
        val intAlpha = (realPercent * 255).toInt()
        val stringAlpha = Integer.toHexString(intAlpha).lowercase()
        val color = "#" + (if (stringAlpha.length < 2) "0" else "") + stringAlpha + "000000"
        return Color.parseColor(color)
    }

    fun setAlpha(alpha: Float) {
        val colorId = convertPercentToBlackAlphaColor(alpha)
        rootView?.setBackgroundColor(colorId)
        if (alpha >= 1) {
            if (indicatorStatus) {
                tvIndicator?.visibility = View.VISIBLE
            }
            consBottomController?.visibility = View.VISIBLE
        } else {
            tvIndicator?.visibility = View.GONE
            consBottomController?.visibility = View.GONE
        }
    }

    @UnstableApi
    override fun handleMessage(msg: Message): Boolean {
        if (msg.what == 0) {
            val path = imageInfoList[currentItem].originUrl
            if (ImagePreview.instance.isSkipLocalCache) {
                // 设置了跳过缓存，这里点击查看原图，直接更换当前item的url
                updateItem(currentItem, path, path)
            } else {
                // 点击查看原图按钮，开始加载原图
                visible()
                if (isUserCustomProgressView) {
                    gone()
                } else {
                    btnShowOrigin?.text = "0 %"
                }
                if (checkCache(path)) {
                    val message = handlerHolder?.obtainMessage()
                    val bundle = Bundle()
                    bundle.putString("url", path)
                    message?.what = 1
                    message?.obj = bundle
                    message?.apply {
                        handlerHolder?.sendMessage(message)
                    }
                    return true
                }
                loadOriginImage(path)
            }
        } else if (msg.what == 1) {
            // 加载完成
            val bundle = msg.obj as Bundle
            val url = HttpUtil.decode(bundle.getString("url", ""))
            gone()
            if (currentItem == getRealIndexWithPath(url)) {
                if (isUserCustomProgressView) {
                    fmCenterProgressContainer?.visibility = View.GONE
                    progressParentLayout?.visibility = View.GONE
                    progressParentLayout?.apply {
                        ImagePreview.instance.onOriginProgressListener?.finish(
                            this@ImagePreviewActivity,
                            progressParentLayout!!
                        )
                    }
                }
                fragmentList[currentItem].onOriginal()
            }
        } else if (msg.what == 2) {
            // 加载中
            val bundle = msg.obj as Bundle
            val url = HttpUtil.decode(bundle.getString("url", ""))
            val progress = bundle.getInt("progress")
            if (currentItem == getRealIndexWithPath(url)) {
                if (isUserCustomProgressView) {
                    gone()
                    fmCenterProgressContainer?.visibility = View.VISIBLE
                    progressParentLayout?.visibility = View.VISIBLE
                    progressParentLayout?.apply {
                        ImagePreview.instance.onOriginProgressListener?.progress(
                            this@ImagePreviewActivity,
                            progressParentLayout!!,
                            progress
                        )
                    }
                } else {
                    visible()
                    btnShowOrigin?.text = String.format("%s %%", progress)
                }
            }
        } else if (msg.what == 3) {
            // 隐藏查看原图按钮
            btnShowOrigin?.setText(R.string.btn_original)
            fmImageShowOriginContainer?.visibility = View.GONE
            originalStatus = false
        } else if (msg.what == 4) {
            // 显示查看原图按钮
            fmImageShowOriginContainer?.visibility = View.VISIBLE
            originalStatus = true
        }
        return true
    }

    private fun getRealIndexWithPath(path: String?): Int {
        for (i in imageInfoList.indices) {
            if (path.equals(imageInfoList[i].originUrl, ignoreCase = true)) {
                return i
            }
        }
        return 0
    }

    private fun checkCache(url: String?): Boolean {
        val cacheFile = ImageLoader.getGlideCacheFile(this@ImagePreviewActivity, url)
        return if (cacheFile != null && cacheFile.exists()) {
            gone()
            true
        } else {
            // 缓存不存在
            // 如果是全自动模式且当前是WiFi，就不显示查看原图按钮
            if (ImagePreview.instance.loadStrategy == ImagePreview.LoadStrategy.Auto && NetworkUtil.isWiFi(
                    this@ImagePreviewActivity
                )
            ) {
                gone()
            } else {
                visible()
            }
            false
        }
    }

    override fun onClick(v: View) {
        val i = v.id
        if (i == R.id.img_download) {
            val downloadClickListener = ImagePreview.instance.downloadClickListener
            if (downloadClickListener != null) {
                val interceptDownload = downloadClickListener.isInterceptDownload
                if (interceptDownload) {
                    // 拦截了下载，不执行下载
                } else {
                    // 没有拦截下载
                    checkAndDownload()
                }
                ImagePreview.instance.downloadClickListener?.onClick(this@ImagePreviewActivity, v, currentItem)
            } else {
                checkAndDownload()
            }
        } else if (i == R.id.btn_show_origin) {
            handlerHolder?.sendEmptyMessage(0)
        } else if (i == R.id.imgCloseButton) {
            finish()
        }
    }

    private fun checkAndDownload() {
        if (DeviceUtil.isHarmonyOs()) {
            val harmonyVersion = DeviceUtil.getHarmonyVersionCode()
            SLog.d("checkAndDownload", "is HarmonyOS, HarmonyOS version:$harmonyVersion")
            if (harmonyVersion < 6) {
                if (ContextCompat.checkSelfPermission(
                        this@ImagePreviewActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this@ImagePreviewActivity,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        1
                    )
                } else {
                    // 下载当前图片
                    downloadCurrentImg()
                }
            } else {
                // 下载当前图片
                downloadCurrentImg()
            }
        } else {
            SLog.d("checkAndDownload", "not HarmonyOS")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // Android 10 以下，需要动态申请权限
                if (ContextCompat.checkSelfPermission(
                        this@ImagePreviewActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this@ImagePreviewActivity,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        1
                    )
                } else {
                    // 下载当前图片
                    downloadCurrentImg()
                }
            } else {
                // Android 10 及以上，保存图片不需要权限
                // 下载当前图片
                downloadCurrentImg()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 下载当前图片
                downloadCurrentImg()
            } else {
                ToastUtil.instance.showShort(
                    this@ImagePreviewActivity,
                    getString(R.string.toast_deny_permission_save_failed)
                )
            }
        }
    }

    private fun gone() {
        handlerHolder?.sendEmptyMessage(3)
    }

    private fun visible() {
        handlerHolder?.sendEmptyMessage(4)
    }

    private fun loadOriginImage(path: String?) {
        addListener(path, object : OnProgressListener {
            override fun onProgress(
                url: String?,
                isComplete: Boolean,
                percentage: Int,
                bytesRead: Long,
                totalBytes: Long
            ) {
                if (isComplete) { // 加载完成
                    val message = handlerHolder?.obtainMessage()
                    val bundle = Bundle()
                    bundle.putString("url", url)
                    message?.what = 1
                    message?.obj = bundle
                    message?.apply {
                        handlerHolder?.sendMessage(message)
                    }
                } else { // 加载中，为减少回调次数，此处做判断，如果和上次的百分比一致就跳过
                    if (percentage == lastProgress) {
                        return
                    }
                    lastProgress = percentage
                    val message = handlerHolder?.obtainMessage()
                    val bundle = Bundle()
                    bundle.putString("url", url)
                    bundle.putInt("progress", percentage)
                    message?.what = 2
                    message?.obj = bundle
                    message?.apply {
                        handlerHolder?.sendMessage(message)
                    }
                }
            }
        })
        Glide.with(this@ImagePreviewActivity).downloadOnly().load(path).into(object : FileTarget() {
        })
    }

    private fun transparentStatusBar() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        val option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.decorView.systemUiVisibility = option
        window.statusBarColor = Color.TRANSPARENT
    }

    private fun transparentNavBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        window.navigationBarColor = Color.TRANSPARENT
        val decorView = window.decorView
        val option =
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        decorView.systemUiVisibility = option
    }

    @UnstableApi
    fun updateItem(index: Int, image: String) {
        updateItem(index, image, image)
    }

    @UnstableApi
    fun updateItem(index: Int, thumbnail: String, origin: String) {
        updateItem(index, thumbnail, origin, Type.IMAGE)
    }

    @UnstableApi
    fun updateItem(index: Int, thumbnail: String, origin: String, type: Type) {
        val imageInfo = ImageInfo()
        imageInfo.originUrl = origin
        imageInfo.thumbnailUrl = thumbnail
        imageInfo.type = type
        updateItem(index, imageInfo)
    }

    // 更新指定的数据源
    @UnstableApi
    fun updateItem(index: Int, imageInfo: ImageInfo) {
        imageInfoList[index] = imageInfo
        fragmentList[index].updateItem(imageInfo)
    }

    // 删除指定的数据源
    fun deleteItem(index: Int) {
        imageInfoList.removeAt(index)
        if (imageInfoList.isEmpty()) {
            onFinish()
            return
        }
        // 清空重建
        fragmentList.clear()
        for ((i, info) in imageInfoList.withIndex()) {
            fragmentList.add(
                ImagePreviewFragment.newInstance(
                    i,
                    info
                )
            )
        }
        var newIndex = currentItem - 1
        if (newIndex < 0) {
            newIndex = 0
        }
        imagePreviewAdapter = ImagePreviewAdapter(supportFragmentManager, fragmentList)
        viewPager?.adapter = imagePreviewAdapter
        viewPager?.offscreenPageLimit = 1
        viewPager?.setCurrentItem(newIndex, false)
        // 触发页面选中回调
        pageChangeListener?.onPageSelected(newIndex)
    }

    companion object {
        fun activityStart(context: Activity) {
            val intent = Intent()
            intent.setClass(context, ImagePreviewActivity::class.java)
            context.startActivity(intent)
        }
    }
}