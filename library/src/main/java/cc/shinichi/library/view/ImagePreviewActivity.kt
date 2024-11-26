package cc.shinichi.library.view

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import cc.shinichi.library.ImagePreview
import cc.shinichi.library.R
import cc.shinichi.library.bean.ImageInfo
import cc.shinichi.library.glide.FileTarget
import cc.shinichi.library.glide.ImageLoader
import cc.shinichi.library.glide.progress.OnProgressListener
import cc.shinichi.library.glide.progress.ProgressManager.addListener
import cc.shinichi.library.tool.common.DeviceUtil
import cc.shinichi.library.tool.common.HandlerHolder
import cc.shinichi.library.tool.common.HttpUtil
import cc.shinichi.library.tool.common.NetworkUtil
import cc.shinichi.library.tool.common.SLog
import cc.shinichi.library.tool.image.DownloadPictureUtil.downloadPicture
import cc.shinichi.library.tool.ui.PhoneUtil
import cc.shinichi.library.tool.ui.ToastUtil
import cc.shinichi.library.tool.ui.UIUtil
import com.bumptech.glide.Glide

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 */
class ImagePreviewActivity : AppCompatActivity(), Handler.Callback, View.OnClickListener {

    private lateinit var context: Activity
    private lateinit var handlerHolder: HandlerHolder
    private val imageInfoList: MutableList<ImageInfo> = mutableListOf()
    private val fragmentList: MutableList<ImagePreviewFragment> = mutableListOf()

    lateinit var viewPager2: ViewPager2
    private lateinit var tvIndicator: TextView

    private lateinit var fmImageShowOriginContainer: FrameLayout
    private lateinit var fmCenterProgressContainer: FrameLayout
    private lateinit var btnShowOrigin: Button
    private lateinit var imgDownload: ImageView
    private lateinit var imgCloseButton: ImageView
    private lateinit var rootView: View
    private lateinit var progressParentLayout: View

    private lateinit var imagePreviewAdapter: ImagePreviewAdapter2

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
        val parentView = View.inflate(this, ImagePreview.instance.previewLayoutResId, null)
        setContentView(parentView)
        ImagePreview.instance.onCustomLayoutCallback?.onLayout(parentView)

        transparentStatusBar(this)
        transparentNavBar(this)

        context = this
        handlerHolder = HandlerHolder(this)
        imageInfoList.clear()
        imageInfoList.addAll(ImagePreview.instance.getImageInfoList())
        if (imageInfoList.isEmpty()) {
            onBackPressed()
            return
        }
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
        viewPager2 = findViewById(R.id.viewPager)
        tvIndicator = findViewById(R.id.tv_indicator)
        val consBottomController = findViewById<ConstraintLayout>(R.id.consBottomController)
        fmImageShowOriginContainer = findViewById(R.id.fm_image_show_origin_container)
        fmCenterProgressContainer = findViewById(R.id.fm_center_progress_container)

        // tvIndicator top margin
        val topMargin = PhoneUtil.getStatusBarHeight(context) + UIUtil.dp2px(context, 20f)
        val layoutParamsIndicator = tvIndicator.layoutParams as ConstraintLayout.LayoutParams
        layoutParamsIndicator.setMargins(0, topMargin, 0, 0)
        tvIndicator.layoutParams = layoutParamsIndicator

        // consBottomController bottom margin
        val bottomMargin = PhoneUtil.getNavBarHeight(context) + UIUtil.dp2px(context, 20f)
        val layoutParams = consBottomController.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.setMargins(0, 0, 0, bottomMargin)
        consBottomController.layoutParams = layoutParams

        fmImageShowOriginContainer.visibility = View.GONE
        fmCenterProgressContainer.visibility = View.GONE
        val progressLayoutId = ImagePreview.instance.progressLayoutId
        // != -1 即用户自定义了view
        if (progressLayoutId != -1) {
            // add用户自定义的view到frameLayout中，回调进度和view
            progressParentLayout =
                View.inflate(context, ImagePreview.instance.progressLayoutId, null)
            fmCenterProgressContainer.removeAllViews()
            fmCenterProgressContainer.addView(progressParentLayout)
            isUserCustomProgressView = true
        } else {
            // 使用默认的textView进行百分比的显示
            isUserCustomProgressView = false
        }
        btnShowOrigin = findViewById(R.id.btn_show_origin)
        imgDownload = findViewById(R.id.img_download)
        imgCloseButton = findViewById(R.id.imgCloseButton)
        imgDownload.setImageResource(ImagePreview.instance.downIconResId)
        imgCloseButton.setImageResource(ImagePreview.instance.closeIconResId)

        // 关闭页面按钮
        imgCloseButton.setOnClickListener(this)
        // 查看与原图按钮
        btnShowOrigin.setOnClickListener(this)
        // 下载图片按钮
        imgDownload.setOnClickListener(this)
        indicatorStatus = if (!isShowIndicator) {
            tvIndicator.visibility = View.GONE
            false
        } else {
            if (imageInfoList.size > 1) {
                tvIndicator.visibility = View.VISIBLE
                true
            } else {
                tvIndicator.visibility = View.GONE
                false
            }
        }
        // 设置顶部指示器背景shape
        if (ImagePreview.instance.indicatorShapeResId > 0) {
            tvIndicator.setBackgroundResource(ImagePreview.instance.indicatorShapeResId)
        }
        downloadButtonStatus = if (isShowDownButton) {
            imgDownload.visibility = View.VISIBLE
            true
        } else {
            imgDownload.visibility = View.GONE
            false
        }
        closeButtonStatus = if (isShowCloseButton) {
            imgCloseButton.visibility = View.VISIBLE
            true
        } else {
            imgCloseButton.visibility = View.GONE
            false
        }

        // 更新进度指示器
        tvIndicator.text = String.format(
            getString(R.string.indicator),
            (currentItem + 1).toString(),
            (imageInfoList.size).toString()
        )

        // 适配器
        fragmentList.clear()
        for ((index, info) in imageInfoList.withIndex()) {
            fragmentList.add(
                ImagePreviewFragment.newInstance(
                    this@ImagePreviewActivity,
                    index,
                    info
                )
            )
        }
        imagePreviewAdapter = ImagePreviewAdapter2(this, fragmentList)
        viewPager2.adapter = imagePreviewAdapter
        viewPager2.setCurrentItem(currentItem, false)

        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentItem = position
                // select回调
                ImagePreview.instance.bigImagePageChangeListener?.onPageSelected(currentItem)

                // 判断是否展示查看原图按钮
                currentItemOriginPathUrl = imageInfoList[currentItem].originUrl
                isShowOriginButton = ImagePreview.instance.isShowOriginButton(currentItem)
                if (isShowOriginButton) {
                    // 检查缓存是否存在
                    checkCache(currentItemOriginPathUrl)
                } else {
                    gone()
                }
                // 更新进度指示器
                tvIndicator.text = String.format(
                    getString(R.string.indicator),
                    (currentItem + 1).toString(),
                    (imageInfoList.size).toString()
                )
                // 如果是自定义百分比进度view，每次切换都先隐藏，并重置百分比
                if (isUserCustomProgressView) {
                    fmCenterProgressContainer.visibility = View.GONE
                    lastProgress = 0
                }
                for ((index, fragment) in fragmentList.withIndex()) {
                    if (index == currentItem) {
                        fragment.onSelected()
                    } else {
                        fragment.onUnSelected()
                    }
                }
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                ImagePreview.instance.bigImagePageChangeListener?.onPageScrolled(
                    position,
                    positionOffset,
                    positionOffsetPixels
                )
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                ImagePreview.instance.bigImagePageChangeListener?.onPageScrollStateChanged(state)
            }
        })
    }

    /**
     * 下载当前图片到SD卡
     */
    private fun downloadCurrentImg() {
        downloadPicture(context, currentItem, currentItemOriginPathUrl)
    }

    override fun finish() {
        super.finish()
        for (fragment in fragmentList) {
            fragment.onRelease()
        }
        ImagePreview.instance.onPageFinishListener?.onFinish(this)
        ImagePreview.instance.reset()
    }

    override fun onDestroy() {
        super.onDestroy()
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
        rootView.setBackgroundColor(colorId)
        if (alpha >= 1) {
            if (indicatorStatus) {
                tvIndicator.visibility = View.VISIBLE
            }
            if (originalStatus) {
                fmImageShowOriginContainer.visibility = View.VISIBLE
            }
            if (downloadButtonStatus) {
                imgDownload.visibility = View.VISIBLE
            }
            if (closeButtonStatus) {
                imgCloseButton.visibility = View.VISIBLE
            }
        } else {
            tvIndicator.visibility = View.GONE
            fmImageShowOriginContainer.visibility = View.GONE
            imgDownload.visibility = View.GONE
            imgCloseButton.visibility = View.GONE
        }
    }

    override fun handleMessage(msg: Message): Boolean {
        if (msg.what == 0) {
            // 点击查看原图按钮，开始加载原图
            val path = imageInfoList[currentItem].originUrl
            visible()
            if (isUserCustomProgressView) {
                gone()
            } else {
                btnShowOrigin.text = "0 %"
            }
            if (checkCache(path)) {
                val message = handlerHolder.obtainMessage()
                val bundle = Bundle()
                bundle.putString("url", path)
                message.what = 1
                message.obj = bundle
                handlerHolder.sendMessage(message)
                return true
            }
            loadOriginImage(path)
        } else if (msg.what == 1) {
            // 加载完成
            val bundle = msg.obj as Bundle
            val url = HttpUtil.decode(bundle.getString("url", ""))
            gone()
            if (currentItem == getRealIndexWithPath(url)) {
                if (isUserCustomProgressView) {
                    fmCenterProgressContainer.visibility = View.GONE
                    progressParentLayout.visibility = View.GONE
                    ImagePreview.instance.onOriginProgressListener?.finish(progressParentLayout)
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
                    fmCenterProgressContainer.visibility = View.VISIBLE
                    progressParentLayout.visibility = View.VISIBLE
                    ImagePreview.instance.onOriginProgressListener?.progress(
                        progressParentLayout,
                        progress
                    )
                } else {
                    visible()
                    btnShowOrigin.text = String.format("%s %%", progress)
                }
            }
        } else if (msg.what == 3) {
            // 隐藏查看原图按钮
            btnShowOrigin.setText(R.string.btn_original)
            fmImageShowOriginContainer.visibility = View.GONE
            originalStatus = false
        } else if (msg.what == 4) {
            // 显示查看原图按钮
            fmImageShowOriginContainer.visibility = View.VISIBLE
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
        val cacheFile = ImageLoader.getGlideCacheFile(context, url)
        return if (cacheFile != null && cacheFile.exists()) {
            gone()
            true
        } else {
            // 缓存不存在
            // 如果是全自动模式且当前是WiFi，就不显示查看原图按钮
            if (ImagePreview.instance.loadStrategy == ImagePreview.LoadStrategy.Auto && NetworkUtil.isWiFi(
                    context
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
                ImagePreview.instance.downloadClickListener?.onClick(context, v, currentItem)
            } else {
                checkAndDownload()
            }
        } else if (i == R.id.btn_show_origin) {
            handlerHolder.sendEmptyMessage(0)
        } else if (i == R.id.imgCloseButton) {
            onBackPressed()
        }
    }

    private fun checkAndDownload() {
        if (DeviceUtil.isHarmonyOs()) {
            val harmonyVersion = DeviceUtil.getHarmonyVersionCode()
            SLog.d("checkAndDownload", "是鸿蒙系统, harmonyVersion:$harmonyVersion")
            if (harmonyVersion < 6) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        context,
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
            SLog.d("checkAndDownload", "不是鸿蒙系统")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // Android 10 以下，需要动态申请权限
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        context,
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
                    context,
                    getString(R.string.toast_deny_permission_save_failed)
                )
            }
        }
    }

    private fun gone() {
        handlerHolder.sendEmptyMessage(3)
    }

    private fun visible() {
        handlerHolder.sendEmptyMessage(4)
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
                    val message = handlerHolder.obtainMessage()
                    val bundle = Bundle()
                    bundle.putString("url", url)
                    message.what = 1
                    message.obj = bundle
                    handlerHolder.sendMessage(message)
                } else { // 加载中，为减少回调次数，此处做判断，如果和上次的百分比一致就跳过
                    if (percentage == lastProgress) {
                        return
                    }
                    lastProgress = percentage
                    val message = handlerHolder.obtainMessage()
                    val bundle = Bundle()
                    bundle.putString("url", url)
                    bundle.putInt("progress", percentage)
                    message.what = 2
                    message.obj = bundle
                    handlerHolder.sendMessage(message)
                }
            }
        })
        Glide.with(context).downloadOnly().load(path).into(object : FileTarget() {
        })
    }

    private fun transparentStatusBar(activity: Activity) {
        transparentStatusBar(activity.window)
    }

    private fun transparentStatusBar(window: Window) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        val option = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        val vis = window.decorView.systemUiVisibility
        window.decorView.systemUiVisibility = option or vis
        window.statusBarColor = Color.TRANSPARENT
    }

    private fun transparentNavBar(activity: Activity) {
        transparentNavBar(activity.window)
    }

    private fun transparentNavBar(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        window.navigationBarColor = Color.TRANSPARENT
        val decorView = window.decorView
        val vis = decorView.systemUiVisibility
        val option =
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        decorView.systemUiVisibility = vis or option
    }

    companion object {
        fun activityStart(context: Context?) {
            if (context == null) {
                return
            }
            val intent = Intent()
            intent.setClass(context, ImagePreviewActivity::class.java)
            // 默认动画
            context.startActivity(intent)
            if (context is Activity) {
                context.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
        }
    }
}