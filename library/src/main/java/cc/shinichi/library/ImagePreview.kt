package cc.shinichi.library

import android.annotation.SuppressLint
import android.app.Activity
import android.text.TextUtils
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import cc.shinichi.library.bean.ImageInfo
import cc.shinichi.library.bean.Type
import cc.shinichi.library.tool.common.SLog
import cc.shinichi.library.view.ImagePreviewActivity
import cc.shinichi.library.view.listener.OnBigImageClickListener
import cc.shinichi.library.view.listener.OnBigImageLongClickListener
import cc.shinichi.library.view.listener.OnBigImagePageChangeListener
import cc.shinichi.library.view.listener.OnCustomLayoutCallback
import cc.shinichi.library.view.listener.OnDownloadClickListener
import cc.shinichi.library.view.listener.OnDownloadListener
import cc.shinichi.library.view.listener.OnFinishListener
import cc.shinichi.library.view.listener.OnOriginProgressListener
import cc.shinichi.library.view.listener.OnPageDragListener
import cc.shinichi.library.view.listener.OnPageFinishListener
import java.lang.ref.WeakReference

/**
 * 大图预览入口类
 *
 * 使用建造者模式配置预览参数，支持链式调用和 DSL 风格
 *
 * 基本用法:
 * ```kotlin
 * ImagePreview.instance
 *     .with(activity)
 *     .setImageUrlList(urls)
 *     .setIndex(0)
 *     .start()
 * ```
 *
 * DSL 用法:
 * ```kotlin
 * ImagePreview.show(activity) {
 *     imageList = urls
 *     index = 0
 *     loadStrategy = LoadStrategy.Auto
 * }
 * ```
 *
 * @author 工藤
 * @email qinglingou@gmail.com
 */
class ImagePreview private constructor() {

    private var contextWeakReference: WeakReference<Activity> = WeakReference(null)

    // 图片数据集合
    private var imageInfoList: MutableList<ImageInfo> = mutableListOf()
    private var resImageList: MutableList<Int> = mutableListOf()

    // 默认显示第几个
    var index = 0
        private set

    // 下载到的文件夹名（根目录中）
    var folderName = DEFAULT_FOLDER_NAME
        get() = field.ifEmpty { DEFAULT_FOLDER_NAME }
        private set

    // 最小缩放倍数
    var minScale = DEFAULT_MIN_SCALE
        private set

    // 中等缩放倍数
    var mediumScale = DEFAULT_MEDIUM_SCALE
        private set

    // 最大缩放倍数
    var maxScale = DEFAULT_MAX_SCALE
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
    var zoomTransitionDuration = DEFAULT_ZOOM_DURATION
        private set

    // 是否启用下拉关闭，默认启用
    var isEnableDragClose = true
        private set

    // 是否启用上拉关闭，默认启用
    var isEnableUpDragClose = true
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

    // 长图的展示模式
    var longPicDisplayMode = LongPicDisplayMode.Default
        private set

    @LayoutRes
    var previewLayoutResId = R.layout.sh_layout_preview
        private set

    var onCustomLayoutCallback: OnCustomLayoutCallback? = null
        private set

    @DrawableRes
    var indicatorShapeResId = R.drawable.shape_indicator_bg
        private set

    @DrawableRes
    var closeIconResId = R.drawable.ic_action_close
        private set

    @DrawableRes
    var closeIconBackgroundResId = INVALID_RES_ID
        private set

    @DrawableRes
    var downIconResId = R.drawable.icon_download_new
        private set

    @DrawableRes
    var downIconBackgroundResId = INVALID_RES_ID
        private set

    @DrawableRes
    var errorPlaceHolder = R.drawable.load_failed
        private set

    // 事件监听器
    var bigImageClickListener: OnBigImageClickListener? = null
        private set
    var bigImageLongClickListener: OnBigImageLongClickListener? = null
        private set
    var bigImagePageChangeListener: OnBigImagePageChangeListener? = null
        private set
    var downloadClickListener: OnDownloadClickListener? = null
        private set
    var downloadListener: OnDownloadListener? = null
        private set
    var onOriginProgressListener: OnOriginProgressListener? = null
        private set
    var onPageFinishListener: OnPageFinishListener? = null
        private set
    var onPageDragListener: OnPageDragListener? = null
        private set
    var finishListener: OnFinishListener? = null
        private set

    @LayoutRes
    var progressLayoutId = INVALID_RES_ID
        private set

    var isSkipLocalCache = false
        private set

    var headers: Map<String, String>? = null
        private set

    var hostKeywordList: List<String>? = null
        private set

    // activity实例
    var previewActivity: ImagePreviewActivity? = null
        internal set

    // 防止多次快速点击，记录上次打开的时间戳
    @Volatile
    private var lastClickTime: Long = 0

    // ==================== 配置方法 ====================

    fun with(context: Activity): ImagePreview {
        contextWeakReference = WeakReference(context)
        return this
    }

    @Deprecated("请使用 with(context) 代替", ReplaceWith("with(context)"))
    fun setContext(context: Activity): ImagePreview = with(context)

    fun getImageInfoList(): MutableList<ImageInfo> = imageInfoList

    @Deprecated("请使用 setMediaInfoList 代替", ReplaceWith("setMediaInfoList(imageInfoList)"))
    fun setImageInfoList(imageInfoList: MutableList<ImageInfo>): ImagePreview = setMediaInfoList(imageInfoList)

    /**
     * 设置媒体信息列表，支持图片视频混合
     */
    fun setMediaInfoList(mediaList: MutableList<ImageInfo>): ImagePreview {
        imageInfoList = ArrayList(mediaList) // 创建副本避免外部修改
        return this
    }

    /**
     * 设置图片 URL 列表，仅支持图片类型
     * 使用 map 优化性能
     */
    fun setImageUrlList(imageList: MutableList<String>): ImagePreview {
        imageInfoList = imageList.mapTo(ArrayList(imageList.size)) { url ->
            ImageInfo.createImage(url)
        }
        return this
    }

    /**
     * 设置单张图片
     */
    fun setImage(image: String): ImagePreview {
        imageInfoList = mutableListOf(ImageInfo.createImage(image))
        return this
    }

    fun setIndex(index: Int): ImagePreview {
        this.index = index.coerceAtLeast(0)
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

    /**
     * 判断是否显示查看原图按钮
     * 根据加载策略和 URL 差异自动判断
     */
    fun isShowOriginButton(index: Int): Boolean {
        if (imageInfoList.isEmpty() || index !in imageInfoList.indices) {
            return false
        }

        val imageInfo = imageInfoList[index]
        // 原图、缩略图url一样，不显示查看原图按钮
        if (imageInfo.originUrl.equals(imageInfo.thumbnailUrl, ignoreCase = true)) {
            return false
        }

        return when (loadStrategy) {
            LoadStrategy.Default, LoadStrategy.Auto -> true
            LoadStrategy.NetworkAuto, LoadStrategy.AlwaysThumb, LoadStrategy.AlwaysOrigin -> false
        }
    }

    @Deprecated("不再支持，是否显示查看原图按钮取决于加载策略")
    fun setShowOriginButton(showOriginButton: Boolean): ImagePreview = this

    fun setFolderName(folderName: String): ImagePreview {
        this.folderName = folderName
        return this
    }

    @Deprecated("不再支持")
    fun setScaleMode(scaleMode: Int): ImagePreview = this

    @Deprecated("不再支持，每张图片的缩放由本身的尺寸决定")
    fun setScaleLevel(min: Int, medium: Int, max: Int): ImagePreview {
        if (medium in (min + 1) until max && min > 0) {
            minScale = min.toFloat()
            mediumScale = medium.toFloat()
            maxScale = max.toFloat()
        } else {
            throw IllegalArgumentException("max must greater than medium, medium must greater than min!")
        }
        return this
    }

    fun setZoomTransitionDuration(zoomTransitionDuration: Int): ImagePreview {
        require(zoomTransitionDuration >= 0) { "zoomTransitionDuration must be >= 0" }
        this.zoomTransitionDuration = zoomTransitionDuration
        return this
    }

    fun setLoadStrategy(loadStrategy: LoadStrategy): ImagePreview {
        this.loadStrategy = loadStrategy
        return this
    }

    fun setLongPicDisplayMode(longPicDisplayMode: LongPicDisplayMode): ImagePreview {
        this.longPicDisplayMode = longPicDisplayMode
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

    fun setIndicatorShapeResId(@DrawableRes indicatorShapeResId: Int): ImagePreview {
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

    fun setCloseIconBackgroundResId(@DrawableRes closeIconBackgroundResId: Int): ImagePreview {
        this.closeIconBackgroundResId = closeIconBackgroundResId
        return this
    }

    fun setDownIconBackgroundResId(@DrawableRes downIconBackgroundResId: Int): ImagePreview {
        this.downIconBackgroundResId = downIconBackgroundResId
        return this
    }

    fun setShowIndicator(showIndicator: Boolean): ImagePreview {
        isShowIndicator = showIndicator
        return this
    }

    fun setErrorPlaceHolder(@DrawableRes errorPlaceHolderResId: Int): ImagePreview {
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

    fun setDownloadListener(downloadListener: OnDownloadListener?): ImagePreview {
        this.downloadListener = downloadListener
        return this
    }

    fun setOnPageFinishListener(onPageFinishListener: OnPageFinishListener): ImagePreview {
        this.onPageFinishListener = onPageFinishListener
        return this
    }

    fun setOnPageDragListener(onPageDragListener: OnPageDragListener): ImagePreview {
        this.onPageDragListener = onPageDragListener
        return this
    }

    internal fun setOnFinishListener(finishListener: OnFinishListener): ImagePreview {
        this.finishListener = finishListener
        return this
    }

    private fun setOnOriginProgressListener(onOriginProgressListener: OnOriginProgressListener): ImagePreview {
        this.onOriginProgressListener = onOriginProgressListener
        return this
    }

    fun setProgressLayoutId(
        @LayoutRes progressLayoutId: Int,
        onOriginProgressListener: OnOriginProgressListener
    ): ImagePreview {
        this.progressLayoutId = progressLayoutId
        setOnOriginProgressListener(onOriginProgressListener)
        return this
    }

    /**
     * 完全自定义预览界面
     * 请参考：R.layout.sh_layout_preview
     * 并保持控件类型、id 一致，否则会找不到控件而报错
     */
    fun setPreviewLayoutResId(
        @LayoutRes previewLayoutResId: Int,
        onCustomLayoutCallback: OnCustomLayoutCallback? = null
    ): ImagePreview {
        this.previewLayoutResId = previewLayoutResId
        this.onCustomLayoutCallback = onCustomLayoutCallback
        return this
    }

    fun setSkipLocalCache(skipLocalCache: Boolean): ImagePreview {
        isSkipLocalCache = skipLocalCache
        return this
    }

    fun setHeaders(headers: Map<String, String>?): ImagePreview {
        this.headers = headers?.toMap() // 创建不可变副本
        return this
    }

    fun setHostKeywordList(hostKeywordList: List<String>?): ImagePreview {
        this.hostKeywordList = hostKeywordList?.toList() // 创建不可变副本
        return this
    }

    /**
     * 重置所有配置到默认值
     */
    fun reset() {
        imageInfoList = mutableListOf()
        resImageList = mutableListOf()
        index = 0
        folderName = DEFAULT_FOLDER_NAME

        minScale = DEFAULT_MIN_SCALE
        mediumScale = DEFAULT_MEDIUM_SCALE
        maxScale = DEFAULT_MAX_SCALE

        isShowIndicator = true
        isShowCloseButton = false
        isShowDownButton = true
        zoomTransitionDuration = DEFAULT_ZOOM_DURATION

        isEnableDragClose = true
        isEnableUpDragClose = true
        isEnableDragCloseIgnoreScale = true
        isEnableClickClose = true
        isShowErrorToast = false

        loadStrategy = LoadStrategy.Default
        longPicDisplayMode = LongPicDisplayMode.Default

        previewLayoutResId = R.layout.sh_layout_preview
        onCustomLayoutCallback = null

        indicatorShapeResId = R.drawable.shape_indicator_bg
        closeIconResId = R.drawable.ic_action_close
        downIconResId = R.drawable.icon_download_new
        closeIconBackgroundResId = INVALID_RES_ID
        downIconBackgroundResId = INVALID_RES_ID
        errorPlaceHolder = R.drawable.load_failed

        // 清空所有监听器
        bigImageClickListener = null
        bigImageLongClickListener = null
        bigImagePageChangeListener = null
        downloadClickListener = null
        downloadListener = null
        onOriginProgressListener = null
        onPageFinishListener = null
        onPageDragListener = null
        finishListener = null

        progressLayoutId = INVALID_RES_ID
        isSkipLocalCache = false
        headers = null
        hostKeywordList = null
        lastClickTime = 0
        previewActivity = null

        contextWeakReference.clear()
    }

    /**
     * 启动预览
     */
    fun start() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime <= MIN_CLICK_INTERVAL) {
            SLog.d(TAG, "忽略快速点击")
            return
        }

        val context = contextWeakReference.get()
            ?: throw IllegalArgumentException("必须先调用 with(context) 设置 Activity!")

        if (context.isFinishing || context.isDestroyed) {
            SLog.w(TAG, "Activity 已销毁，取消预览")
            reset()
            return
        }

        require(imageInfoList.isNotEmpty() || resImageList.isNotEmpty()) { "没有数据源!" }
        require(index < imageInfoList.size || index < resImageList.size) { "index 越界!" }

        lastClickTime = currentTime
        ImagePreviewActivity.activityStart(context)
    }

    /**
     * 手动关闭预览页面
     */
    fun finish() {
        finishListener?.onFinish()
    }

    // ==================== 枚举定义 ====================

    /**
     * 图片加载策略
     */
    enum class LoadStrategy {
        /** 仅加载原图；会强制隐藏查看原图按钮 */
        AlwaysOrigin,

        /** 仅加载普清；会强制隐藏查看原图按钮 */
        AlwaysThumb,

        /** 根据网络自适应加载，WiFi原图，流量普清；会强制隐藏查看原图按钮 */
        NetworkAuto,

        /** 手动模式：默认普清，点击按钮再加载原图 */
        Default,

        /** 全自动模式：WiFi原图，流量下默认普清，可点击按钮查看原图 */
        Auto
    }

    /**
     * 长图展示模式
     */
    enum class LongPicDisplayMode {
        /** 缩小填充，双击拉满，可手动缩放 */
        Default,

        /** 左右拉满，双击缩小，可手动缩放（适合竖屏手机） */
        FillWidth
    }

    // ==================== 单例实现 ====================

    private object InnerClass {
        @SuppressLint("StaticFieldLeak")
        val INSTANCE = ImagePreview()
    }

    companion object {
        private const val TAG = "ImagePreview"

        // 默认值常量
        private const val DEFAULT_FOLDER_NAME = "Download"
        private const val DEFAULT_MIN_SCALE = 1.0f
        private const val DEFAULT_MEDIUM_SCALE = 3.0f
        private const val DEFAULT_MAX_SCALE = 5.0f
        private const val DEFAULT_ZOOM_DURATION = 200
        private const val INVALID_RES_ID = -1
        private const val MIN_CLICK_INTERVAL = 1500L

        @JvmField
        @LayoutRes
        val PROGRESS_THEME_CIRCLE_TEXT = R.layout.sh_default_progress_layout

        /**
         * 获取单例实例
         */
        @JvmStatic
        val instance: ImagePreview
            get() = InnerClass.INSTANCE

        /**
         * DSL 风格快速启动预览
         *
         * ```kotlin
         * ImagePreview.show(activity) {
         *     setImageUrlList(urls)
         *     setIndex(0)
         * }
         * ```
         */
        @JvmStatic
        inline fun show(context: Activity, config: ImagePreview.() -> Unit) {
            instance.reset()
            instance.with(context)
            instance.config()
            instance.start()
        }
    }
}