package cc.shinichi.library

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import cc.shinichi.library.bean.ImageInfo
import cc.shinichi.library.view.ImagePreviewActivity
import cc.shinichi.library.view.listener.*
import java.lang.ref.WeakReference

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 * cc.shinichi.library
 * create at 2018/5/22  09:06
 * description:
 */
class ImagePreview {
    private var contextWeakReference: WeakReference<Context> = WeakReference(null)

    // 图片数据集合
    private var imageInfoList: MutableList<ImageInfo> = mutableListOf()

    // 用于过度动画的 VIEW
    var transitionView: View? = null
        private set

    // 过度动画的共享元素名
    var transitionShareElementName: String? = null
        private set

    // 默认显示第几个
    var index = 0
        private set

    // 下载到的文件夹名（根目录中）
    var folderName = ""
        get() {
            if (TextUtils.isEmpty(field)) {
                field = "Download"
            }
            return field
        }
        private set

    // 最小缩放倍数
    var minScale = 1.0f
        private set

    // 中等缩放倍数
    var mediumScale = 3.0f
        private set

    // 最大缩放倍数
    var maxScale = 5.0f
        private set

    // 是否显示图片指示器（1/9）
    var isShowIndicator = true
        private set

    // 是否显示关闭页面按钮
    var isShowCloseButton = false
        private set

    // 是否显示下载按钮
    var isShowDownButton = true
        private set

    // 动画持续时间 单位毫秒 ms
    var zoomTransitionDuration = 200
        private set

    // 是否启用下拉关闭，默认不启用
    var isEnableDragClose = true
        private set

    // 是否启用上拉关闭，默认不启用
    var isEnableUpDragClose = false
        private set

    // 是否忽略缩放启用拉动关闭，默认false，true即忽略
    var isEnableDragCloseIgnoreScale = true
        private set

    // 是否启用点击关闭，默认启用
    var isEnableClickClose = true
        private set

    // 是否在加载失败时显示toast
    var isShowErrorToast = false
        private set

    // 加载策略
    var loadStrategy = LoadStrategy.Auto
        private set

    @LayoutRes
    var previewLayoutResId = R.layout.sh_layout_preview
        private set

    @DrawableRes
    var indicatorShapeResId = R.drawable.shape_indicator_bg
        private set

    @DrawableRes
    var closeIconResId = R.drawable.ic_action_close
        private set

    @DrawableRes
    var downIconResId = R.drawable.icon_download_new
        private set

    // 加载失败时的占位图
    @DrawableRes
    var errorPlaceHolder = R.drawable.load_failed
        private set

    // 点击和长按事件接口
    var bigImageClickListener: OnBigImageClickListener? = null
        private set
    var bigImageLongClickListener: OnBigImageLongClickListener? = null
        private set
    var bigImagePageChangeListener: OnBigImagePageChangeListener? = null
        private set
    var downloadClickListener: OnDownloadClickListener? = null
        private set
    var onOriginProgressListener: OnOriginProgressListener? = null
        private set

    // 自定义百分比布局layout id
    @LayoutRes
    var progressLayoutId = -1
        private set

    // 防止多次快速点击，记录上次打开的时间戳
    private var lastClickTime: Long = 0
    fun setContext(context: Context): ImagePreview {
        contextWeakReference = WeakReference(context)
        return this
    }

    /**
     * 设置用于过度动画的共享元素view<br></br>
     * 需要同时设置 [ImagePreview.setTransitionShareElementName]
     */
    fun setTransitionView(transitionView: View?): ImagePreview {
        this.transitionView = transitionView
        return this
    }

    /**
     * 设置用于过度动画的共享元素name <br></br>
     * 需要同时设置 [ImagePreview.setTransitionView]
     */
    fun setTransitionShareElementName(transitionShareElementName: String?): ImagePreview {
        this.transitionShareElementName = transitionShareElementName
        return this
    }

    fun getImageInfoList(): MutableList<ImageInfo> {
        return imageInfoList
    }

    fun setImageInfoList(imageInfoList: MutableList<ImageInfo>): ImagePreview {
        this.imageInfoList.clear()
        this.imageInfoList.addAll(imageInfoList)
        return this
    }

    fun setImageList(imageList: MutableList<String>): ImagePreview {
        var imageInfo: ImageInfo
        imageInfoList.clear()
        for (i in imageList.indices) {
            imageInfo = ImageInfo()
            imageInfo.thumbnailUrl = imageList[i]
            imageInfo.originUrl = imageList[i]
            imageInfoList.add(imageInfo)
        }
        return this
    }

    fun setImage(image: String): ImagePreview {
        imageInfoList.clear()
        val imageInfo = ImageInfo()
        imageInfo.thumbnailUrl = image
        imageInfo.originUrl = image
        imageInfoList.add(imageInfo)
        return this
    }

    fun setIndex(index: Int): ImagePreview {
        this.index = index
        return this
    }

    fun setShowDownButton(showDownButton: Boolean): ImagePreview {
        isShowDownButton = showDownButton
        return this
    }

    fun setShowCloseButton(showCloseButton: Boolean): ImagePreview {
        isShowCloseButton = showCloseButton
        return this
    }

    fun isShowOriginButton(index: Int): Boolean {
        if (getImageInfoList().isEmpty()) {
            return false
        }
        // 根据不同加载策略，自行判断是否显示查看原图按钮
        val originUrl = imageInfoList[index].originUrl
        val thumbUrl = imageInfoList[index].thumbnailUrl
        // 原图、缩略图url一样，不显示查看原图按钮
        if (originUrl.equals(thumbUrl, ignoreCase = true)) {
            return false
        }
        return when (loadStrategy) {
            LoadStrategy.Default -> {
                true // 手动模式时，根据是否有原图缓存来决定是否显示查看原图按钮
            }
            LoadStrategy.NetworkAuto -> {
                false // 强制隐藏查看原图按钮
            }
            LoadStrategy.AlwaysThumb -> {
                false // 强制隐藏查看原图按钮
            }
            LoadStrategy.AlwaysOrigin -> {
                false // 强制隐藏查看原图按钮
            }
            LoadStrategy.Auto -> {
                true // 显示查看原图按钮
            }
        }
    }

    /**
     * 不再有效，是否显示查看原图按钮，取决于加载策略，LoadStrategy，会自行判断是否显示。
     */
    @Deprecated("不再支持")
    fun setShowOriginButton(showOriginButton: Boolean): ImagePreview {
        //isShowOriginButton = showOriginButton;
        return this
    }

    fun setFolderName(folderName: String): ImagePreview {
        this.folderName = folderName
        return this
    }

    /**
     * 当前版本不再支持本设置，双击会在最小和中等缩放值之间进行切换，可手动放大到最大。
     */
    @Deprecated("不再支持")
    fun setScaleMode(scaleMode: Int): ImagePreview {
        //if (scaleMode != MODE_SCALE_TO_MAX_TO_MIN
        //	&& scaleMode != MODE_SCALE_TO_MEDIUM_TO_MAX_TO_MIN
        //	&& scaleMode != MODE_SCALE_TO_MEDIUM_TO_MIN) {
        //	throw new IllegalArgumentException("only can use one of( MODE_SCALE_TO_MAX_TO_MIN、MODE_SCALE_TO_MEDIUM_TO_MAX_TO_MIN、MODE_SCALE_TO_MEDIUM_TO_MIN )");
        //}
        //this.scaleMode = scaleMode;
        return this
    }

    @Deprecated("")
    fun setScaleLevel(min: Int, medium: Int, max: Int): ImagePreview {
        if (medium in (min + 1) until max && min > 0) {
            minScale = min.toFloat()
            mediumScale = medium.toFloat()
            maxScale = max.toFloat()
        } else {
            throw IllegalArgumentException("max must greater to medium, medium must greater to min!")
        }
        return this
    }

    fun setZoomTransitionDuration(zoomTransitionDuration: Int): ImagePreview {
        require(zoomTransitionDuration >= 0) { "zoomTransitionDuration must greater 0" }
        this.zoomTransitionDuration = zoomTransitionDuration
        return this
    }

    fun setLoadStrategy(loadStrategy: LoadStrategy): ImagePreview {
        this.loadStrategy = loadStrategy
        return this
    }

    fun setEnableDragClose(enableDragClose: Boolean): ImagePreview {
        isEnableDragClose = enableDragClose
        return this
    }

    fun setEnableUpDragClose(enableUpDragClose: Boolean): ImagePreview {
        isEnableUpDragClose = enableUpDragClose
        return this
    }

    fun setEnableDragCloseIgnoreScale(enableDragCloseIgnoreScale: Boolean): ImagePreview {
        isEnableDragCloseIgnoreScale = enableDragCloseIgnoreScale
        return this
    }

    fun setEnableClickClose(enableClickClose: Boolean): ImagePreview {
        isEnableClickClose = enableClickClose
        return this
    }

    fun setShowErrorToast(showErrorToast: Boolean): ImagePreview {
        isShowErrorToast = showErrorToast
        return this
    }

    fun setIndicatorShapeResId(indicatorShapeResId: Int): ImagePreview {
        this.indicatorShapeResId = indicatorShapeResId
        return this
    }

    fun setCloseIconResId(@DrawableRes closeIconResId: Int): ImagePreview {
        this.closeIconResId = closeIconResId
        return this
    }

    fun setDownIconResId(@DrawableRes downIconResId: Int): ImagePreview {
        this.downIconResId = downIconResId
        return this
    }

    fun setShowIndicator(showIndicator: Boolean): ImagePreview {
        isShowIndicator = showIndicator
        return this
    }

    fun setErrorPlaceHolder(errorPlaceHolderResId: Int): ImagePreview {
        errorPlaceHolder = errorPlaceHolderResId
        return this
    }

    fun setBigImageClickListener(bigImageClickListener: OnBigImageClickListener?): ImagePreview {
        this.bigImageClickListener = bigImageClickListener
        return this
    }

    fun setBigImageLongClickListener(bigImageLongClickListener: OnBigImageLongClickListener?): ImagePreview {
        this.bigImageLongClickListener = bigImageLongClickListener
        return this
    }

    fun setBigImagePageChangeListener(bigImagePageChangeListener: OnBigImagePageChangeListener?): ImagePreview {
        this.bigImagePageChangeListener = bigImagePageChangeListener
        return this
    }

    fun setDownloadClickListener(downloadClickListener: OnDownloadClickListener?): ImagePreview {
        this.downloadClickListener = downloadClickListener
        return this
    }

    private fun setOnOriginProgressListener(onOriginProgressListener: OnOriginProgressListener): ImagePreview {
        this.onOriginProgressListener = onOriginProgressListener
        return this
    }

    fun setProgressLayoutId(progressLayoutId: Int, onOriginProgressListener: OnOriginProgressListener): ImagePreview {
        setOnOriginProgressListener(onOriginProgressListener)
        this.progressLayoutId = progressLayoutId
        return this
    }

    /**
     * 完全自定义预览界面，请参考：R.layout.sh_layout_preview
     * 并保持控件类型、id和其中一致，否则会找不到控件而报错
     */
    fun setPreviewLayoutResId(previewLayoutResId: Int): ImagePreview {
        this.previewLayoutResId = previewLayoutResId
        return this
    }

    fun reset() {
        imageInfoList.clear()
        transitionView = null
        transitionShareElementName = null
        index = 0
        minScale = 1.0f
        mediumScale = 3.0f
        maxScale = 5.0f
        zoomTransitionDuration = 200
        isShowDownButton = true
        isShowCloseButton = false
        isEnableDragClose = false
        isEnableClickClose = true
        isShowIndicator = true
        isShowErrorToast = false
        closeIconResId = R.drawable.ic_action_close
        downIconResId = R.drawable.icon_download_new
        errorPlaceHolder = R.drawable.load_failed
        loadStrategy = LoadStrategy.Default
        folderName = "Download"
        contextWeakReference.clear()
        bigImageClickListener = null
        bigImageLongClickListener = null
        bigImagePageChangeListener = null
        progressLayoutId = -1
        lastClickTime = 0
    }

    fun start() {
        if (System.currentTimeMillis() - lastClickTime <= MIN_DOUBLE_CLICK_TIME) {
            Log.e("ImagePreview", "---忽略多次快速点击---")
            return
        }
        val context = contextWeakReference.get()
            ?: throw IllegalArgumentException("You must call 'setContext(Context context)' first!")
        require(context is Activity) { "context must be a Activity!" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (context.isFinishing || context.isDestroyed) {
                reset()
                return
            }
        } else {
            if (context.isFinishing) {
                reset()
                return
            }
        }
        require(imageInfoList.size != 0) { "Do you forget to call 'setImageInfoList(List<ImageInfo> imageInfoList)' ?" }
        require(index < imageInfoList.size) { "index out of range!" }
        lastClickTime = System.currentTimeMillis()
        ImagePreviewActivity.activityStart(context)
    }

    enum class LoadStrategy {
        /**
         * 仅加载原图；会强制隐藏查看原图按钮
         */
        AlwaysOrigin,

        /**
         * 仅加载普清；会强制隐藏查看原图按钮
         */
        AlwaysThumb,

        /**
         * 根据网络自适应加载，WiFi原图，流量普清；会强制隐藏查看原图按钮
         */
        NetworkAuto,

        /**
         * 手动模式：默认普清，点击按钮再加载原图；会根据原图、缩略图url是否一样来判断是否显示查看原图按钮
         */
        Default,

        /**
         * 全自动模式：WiFi原图，流量下默认普清，可点击按钮查看原图
         */
        Auto
    }

    private object InnerClass {
        @SuppressLint("StaticFieldLeak")
        val instance = ImagePreview()
    }

    companion object {
        @JvmField
        @LayoutRes
        val PROGRESS_THEME_CIRCLE_TEXT = R.layout.sh_default_progress_layout

        // 触发双击的最短时间，小于这个时间的直接返回
        private const val MIN_DOUBLE_CLICK_TIME = 1500

        @JvmStatic
        val instance: ImagePreview
            get() = InnerClass.instance
    }
}