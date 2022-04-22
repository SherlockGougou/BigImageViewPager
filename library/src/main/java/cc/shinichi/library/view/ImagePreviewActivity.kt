package cc.shinichi.library.view

import android.Manifest
import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import cc.shinichi.library.ImagePreview
import cc.shinichi.library.R
import cc.shinichi.library.bean.ImageInfo
import cc.shinichi.library.glide.FileTarget
import cc.shinichi.library.glide.ImageLoader.getGlideCacheFile
import cc.shinichi.library.glide.progress.OnProgressListener
import cc.shinichi.library.glide.progress.ProgressManager.addListener
import cc.shinichi.library.tool.common.HandlerHolder
import cc.shinichi.library.tool.image.DownloadPictureUtil.downloadPicture
import cc.shinichi.library.tool.ui.ToastUtil
import com.bumptech.glide.Glide
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import java.util.*

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 */
class ImagePreviewActivity : AppCompatActivity(), Handler.Callback, View.OnClickListener {

    private lateinit var context: Activity

    private lateinit var handlerHolder: HandlerHolder
    private lateinit var imageInfoList: MutableList<ImageInfo>
    private var currentItem = 0

    private var isShowDownButton = false
    private var isShowCloseButton = false
    private var isShowOriginButton = false
    private var isShowIndicator = false

    private lateinit var imagePreviewAdapter: ImagePreviewAdapter
    private lateinit var viewPager: HackyViewPager
    private lateinit var tv_indicator: TextView
    private lateinit var fm_image_show_origin_container: FrameLayout
    private lateinit var fm_center_progress_container: FrameLayout
    private lateinit var btn_show_origin: Button
    private lateinit var img_download: ImageView
    private lateinit var imgCloseButton: ImageView
    private lateinit var rootView: View
    private lateinit var progressParentLayout: View

    private var isUserCustomProgressView = false

    // 指示器显示状态
    private var indicatorStatus = false

    // 原图按钮显示状态
    private var originalStatus = false

    // 下载按钮显示状态
    private var downloadButtonStatus = false

    // 关闭按钮显示状态
    private var closeButtonStatus = false
    private var currentItemOriginPathUrl: String? = ""
    private var lastProgress = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        // 只有安卓版本大于 5.0 才可使用过度动画
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !TextUtils.isEmpty(ImagePreview.instance.transitionShareElementName)) {
            window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            findViewById<View>(android.R.id.content).transitionName = "shared_element_container"
            setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())
            window.sharedElementEnterTransition =
                MaterialContainerTransform().addTarget(android.R.id.content).setDuration(300L)
            window.sharedElementReturnTransition =
                MaterialContainerTransform().addTarget(android.R.id.content).setDuration(250L)
        }
        super.onCreate(savedInstanceState)

        // R.layout.sh_layout_preview
        setContentView(ImagePreview.instance.previewLayoutResId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
        context = this
        handlerHolder = HandlerHolder(this)
        imageInfoList = ImagePreview.instance.getImageInfoList()
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
        viewPager = findViewById(R.id.viewPager)
        tv_indicator = findViewById(R.id.tv_indicator)
        fm_image_show_origin_container = findViewById(R.id.fm_image_show_origin_container)
        fm_center_progress_container = findViewById(R.id.fm_center_progress_container)
        fm_image_show_origin_container.visibility = View.GONE
        fm_center_progress_container.visibility = View.GONE
        val progressLayoutId = ImagePreview.instance.progressLayoutId
        // != -1 即用户自定义了view
        if (progressLayoutId != -1) {
            // add用户自定义的view到frameLayout中，回调进度和view
            progressParentLayout = View.inflate(context, ImagePreview.instance.progressLayoutId, null)
            isUserCustomProgressView = run {
                fm_center_progress_container.removeAllViews()
                fm_center_progress_container.addView(progressParentLayout)
                true
            }
        } else {
            // 使用默认的textView进行百分比的显示
            isUserCustomProgressView = false
        }
        btn_show_origin = findViewById(R.id.btn_show_origin)
        img_download = findViewById(R.id.img_download)
        imgCloseButton = findViewById(R.id.imgCloseButton)
        img_download.setImageResource(ImagePreview.instance.downIconResId)
        imgCloseButton.setImageResource(ImagePreview.instance.closeIconResId)

        // 关闭页面按钮
        imgCloseButton.setOnClickListener(this)
        // 查看与原图按钮
        btn_show_origin.setOnClickListener(this)
        // 下载图片按钮
        img_download.setOnClickListener(this)
        indicatorStatus = if (!isShowIndicator) {
            tv_indicator.visibility = View.GONE
            false
        } else {
            if (imageInfoList.size > 1) {
                tv_indicator.visibility = View.VISIBLE
                true
            } else {
                tv_indicator.visibility = View.GONE
                false
            }
        }
        // 设置顶部指示器背景shape
        if (ImagePreview.instance.indicatorShapeResId > 0) {
            tv_indicator.setBackgroundResource(ImagePreview.instance.indicatorShapeResId)
        }
        downloadButtonStatus = if (isShowDownButton) {
            img_download.visibility = View.VISIBLE
            true
        } else {
            img_download.visibility = View.GONE
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
        tv_indicator.text = String.format(
            getString(R.string.indicator),
            (currentItem + 1).toString(),
            (imageInfoList.size).toString()
        )
        imagePreviewAdapter = ImagePreviewAdapter(this, imageInfoList)
        viewPager.adapter = imagePreviewAdapter
        viewPager.currentItem = currentItem

        viewPager.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                ImagePreview.instance.bigImagePageChangeListener?.onPageSelected(position)
                currentItem = position
                currentItemOriginPathUrl = imageInfoList[position].originUrl
                isShowOriginButton = ImagePreview.instance.isShowOriginButton(currentItem)
                if (isShowOriginButton) {
                    // 检查缓存是否存在
                    checkCache(currentItemOriginPathUrl)
                } else {
                    gone()
                }
                // 更新进度指示器
                tv_indicator.text = String.format(
                    getString(R.string.indicator),
                    (currentItem + 1).toString(),
                    (imageInfoList.size).toString()
                )
                // 如果是自定义百分比进度view，每次切换都先隐藏，并重置百分比
                if (isUserCustomProgressView) {
                    fm_center_progress_container.visibility = View.GONE
                    lastProgress = 0
                }
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
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
        downloadPicture(context.applicationContext, currentItemOriginPathUrl)
    }

    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            supportFinishAfterTransition()
        } else {
            finish()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        ImagePreview.instance.reset()
        imagePreviewAdapter.closePage()
    }

    private fun convertPercentToBlackAlphaColor(percent: Float): Int {
        var percent = percent
        percent = Math.min(1f, Math.max(0f, percent))
        val intAlpha = (percent * 255).toInt()
        val stringAlpha = Integer.toHexString(intAlpha).lowercase(Locale.getDefault())
        val color = "#" + (if (stringAlpha.length < 2) "0" else "") + stringAlpha + "000000"
        return Color.parseColor(color)
    }

    fun setAlpha(alpha: Float) {
        val colorId = convertPercentToBlackAlphaColor(alpha)
        rootView.setBackgroundColor(colorId)
        if (alpha >= 1) {
            if (indicatorStatus) {
                tv_indicator.visibility = View.VISIBLE
            }
            if (originalStatus) {
                fm_image_show_origin_container.visibility = View.VISIBLE
            }
            if (downloadButtonStatus) {
                img_download.visibility = View.VISIBLE
            }
            if (closeButtonStatus) {
                imgCloseButton.visibility = View.VISIBLE
            }
        } else {
            tv_indicator.visibility = View.GONE
            fm_image_show_origin_container.visibility = View.GONE
            img_download.visibility = View.GONE
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
                btn_show_origin.text = "0 %"
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
            val url = bundle.getString("url")
            gone()
            if (currentItem == getRealIndexWithPath(url)) {
                if (isUserCustomProgressView) {
                    fm_center_progress_container.visibility = View.GONE
                    progressParentLayout.visibility = View.GONE
                    ImagePreview.instance.onOriginProgressListener?.finish(progressParentLayout)
                    imagePreviewAdapter.loadOrigin(imageInfoList[currentItem])
                } else {
                    imagePreviewAdapter.loadOrigin(imageInfoList[currentItem])
                }
            }
        } else if (msg.what == 2) {
            // 加载中
            val bundle = msg.obj as Bundle
            val url = bundle.getString("url")
            val progress = bundle.getInt("progress")
            if (currentItem == getRealIndexWithPath(url)) {
                if (isUserCustomProgressView) {
                    gone()
                    fm_center_progress_container.visibility = View.VISIBLE
                    progressParentLayout.visibility = View.VISIBLE
                    ImagePreview.instance.onOriginProgressListener?.progress(progressParentLayout, progress)
                } else {
                    visible()
                    btn_show_origin.text = String.format("%s %%", progress)
                }
            }
        } else if (msg.what == 3) {
            // 隐藏查看原图按钮
            btn_show_origin.setText(R.string.btn_original)
            fm_image_show_origin_container.visibility = View.GONE
            originalStatus = false
        } else if (msg.what == 4) {
            // 显示查看原图按钮
            fm_image_show_origin_container.visibility = View.VISIBLE
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
        val cacheFile = getGlideCacheFile(context, url)
        return if (cacheFile != null && cacheFile.exists()) {
            gone()
            true
        } else {
            visible()
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
        // 检查权限
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@ImagePreviewActivity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                // 拒绝权限
                ToastUtil.instance.showShort(context, getString(R.string.toast_deny_permission_save_failed))
            } else {
                //申请权限
                ActivityCompat.requestPermissions(
                    this@ImagePreviewActivity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
            }
        } else {
            // 下载当前图片
            downloadCurrentImg()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            for (i in permissions.indices) {
                if (grantResults[i] == PermissionChecker.PERMISSION_GRANTED) {
                    downloadCurrentImg()
                } else {
                    ToastUtil.instance.showShort(context, getString(R.string.toast_deny_permission_save_failed))
                }
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
        Glide.with(context).downloadOnly().load(path).into(object : FileTarget() {
        })
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
    }

    companion object {
        fun activityStart(context: Context?) {
            if (context == null) {
                return
            }
            val intent = Intent()
            intent.setClass(context, ImagePreviewActivity::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // 过度动画效果只对安卓 5.0 以上有效
                val transitionView = ImagePreview.instance.transitionView
                val transitionShareElementName = ImagePreview.instance.transitionShareElementName
                // 如果未设置则使用默认动画
                if (transitionView != null && transitionShareElementName != null) {
                    val options = ActivityOptions.makeSceneTransitionAnimation(
                        context as Activity?,
                        transitionView,
                        transitionShareElementName
                    )
                    context.startActivity(intent, options.toBundle())
                } else {
                    context.startActivity(intent)
                    if (context is Activity) {
                        context.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    }
                }
            } else {
                // 低于 5.0 使用默认动画
                context.startActivity(intent)
                if (context is Activity) {
                    context.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                }
            }
        }
    }
}