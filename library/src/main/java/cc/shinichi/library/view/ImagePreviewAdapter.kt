package cc.shinichi.library.view

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.PagerAdapter
import cc.shinichi.library.ImagePreview
import cc.shinichi.library.ImagePreview.LoadStrategy
import cc.shinichi.library.R
import cc.shinichi.library.bean.ImageInfo
import cc.shinichi.library.glide.FileTarget
import cc.shinichi.library.glide.ImageLoader.clearMemory
import cc.shinichi.library.glide.ImageLoader.getGlideCacheFile
import cc.shinichi.library.tool.common.HttpUtil.downloadFile
import cc.shinichi.library.tool.common.NetworkUtil.isWiFi
import cc.shinichi.library.tool.file.FileUtil.Companion.getAvailableCacheDir
import cc.shinichi.library.tool.image.ImageUtil.getBitmapDegree
import cc.shinichi.library.tool.image.ImageUtil.getImageBitmap
import cc.shinichi.library.tool.image.ImageUtil.getLongImageMaxScale
import cc.shinichi.library.tool.image.ImageUtil.getLongImageMinScale
import cc.shinichi.library.tool.image.ImageUtil.getSmallImageMaxScale
import cc.shinichi.library.tool.image.ImageUtil.getSmallImageMinScale
import cc.shinichi.library.tool.image.ImageUtil.getWideImageDoubleScale
import cc.shinichi.library.tool.image.ImageUtil.getWidthHeight
import cc.shinichi.library.tool.image.ImageUtil.isBmpImageWithMime
import cc.shinichi.library.tool.image.ImageUtil.isGifImageWithMime
import cc.shinichi.library.tool.image.ImageUtil.isLongImage
import cc.shinichi.library.tool.image.ImageUtil.isSmallImage
import cc.shinichi.library.tool.image.ImageUtil.isStandardImage
import cc.shinichi.library.tool.image.ImageUtil.isWideImage
import cc.shinichi.library.tool.ui.PhoneUtil.getPhoneHei
import cc.shinichi.library.tool.ui.ToastUtil
import cc.shinichi.library.view.helper.FingerDragHelper
import cc.shinichi.library.view.helper.ImageSource
import cc.shinichi.library.view.helper.SubsamplingScaleImageViewDragClose
import cc.shinichi.library.view.listener.SimpleOnImageEventListener
import cc.shinichi.library.view.photoview.PhotoView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import java.io.File
import kotlin.math.abs

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 */
class ImagePreviewAdapter(private val activity: AppCompatActivity, private val imageList: MutableList<ImageInfo>) :
    PagerAdapter() {

    private val imageMyList: MutableList<ImageInfo> = mutableListOf()
    private val imageHashMap: HashMap<String?, SubsamplingScaleImageViewDragClose?> = HashMap()
    private val imageGifHashMap: HashMap<String?, PhotoView?> = HashMap()
    private var finalLoadUrl: String? = ""

    init {
        imageMyList.addAll(imageList)
    }

    fun closePage() {
        try {
            if (imageHashMap.size > 0) {
                for (o in imageHashMap.entries) {
                    val entry = o as Map.Entry<*, *>
                    if (entry.value != null) {
                        (entry.value as SubsamplingScaleImageViewDragClose).destroyDrawingCache()
                        (entry.value as SubsamplingScaleImageViewDragClose).recycle()
                    }
                }
                imageHashMap.clear()
            }
            if (imageGifHashMap.size > 0) {
                for (o in imageGifHashMap.entries) {
                    val entry = o as Map.Entry<*, *>
                    if (entry.value != null) {
                        (entry.value as PhotoView).destroyDrawingCache()
                        (entry.value as PhotoView).setImageBitmap(null)
                    }
                }
                imageGifHashMap.clear()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getCount(): Int {
        return imageMyList.size
    }

    /**
     * 加载原图
     */
    fun loadOrigin(imageInfo: ImageInfo) {
        val originalUrl = imageInfo.originUrl
        if (imageHashMap[originalUrl] != null && imageGifHashMap[originalUrl] != null) {
            val imageView = imageHashMap[imageInfo.originUrl]
            val imageGif = imageGifHashMap[imageInfo.originUrl]
            val cacheFile = getGlideCacheFile(activity, imageInfo.originUrl)
            if (cacheFile != null && cacheFile.exists()) {
                val isCacheIsGif = originalUrl?.let { isGifImageWithMime(it, cacheFile.absolutePath) }
                if (isCacheIsGif == true) {
                    imageView?.visibility = View.GONE
                    imageGif?.let {
                        imageGif.visibility = View.VISIBLE
                        Glide.with(activity)
                            .asGif()
                            .load(cacheFile)
                            .apply(
                                RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                    .error(ImagePreview.instance.errorPlaceHolder)
                            )
                            .into(imageGif)
                    }
                } else {
                    imageGif?.visibility = View.GONE
                    imageView?.let {
                        imageView.visibility = View.VISIBLE
                        val thumbnailUrl = imageInfo.thumbnailUrl
                        val smallCacheFile = getGlideCacheFile(activity, thumbnailUrl)
                        var small: ImageSource? = null
                        if (smallCacheFile != null && smallCacheFile.exists()) {
                            val smallImagePath = smallCacheFile.absolutePath
                            small = getImageBitmap(smallImagePath, getBitmapDegree(smallImagePath))?.let {
                                ImageSource.bitmap(
                                    it
                                )
                            }
                            val widSmall = getWidthHeight(smallImagePath)[0]
                            val heiSmall = getWidthHeight(smallImagePath)[1]
                            if (originalUrl?.let { isBmpImageWithMime(it, cacheFile.absolutePath) } == true) {
                                small?.tilingDisabled()
                            }
                            small?.dimensions(widSmall, heiSmall)
                        }
                        val imagePath = cacheFile.absolutePath
                        val origin = ImageSource.uri(imagePath)
                        val widOrigin = getWidthHeight(imagePath)[0]
                        val heiOrigin = getWidthHeight(imagePath)[1]
                        if (originalUrl?.let { isBmpImageWithMime(it, cacheFile.absolutePath) } == true) {
                            origin.tilingDisabled()
                        }
                        origin.dimensions(widOrigin, heiOrigin)
                        setImageSpec(imagePath, imageView)
                        imageView.orientation = SubsamplingScaleImageViewDragClose.ORIENTATION_USE_EXIF
                        imageView.setImage(origin, small)
                    }
                }
            } else {
                notifyDataSetChanged()
            }
        } else {
            notifyDataSetChanged()
        }
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val convertView = View.inflate(activity, R.layout.sh_item_photoview, null)
        val progressBar = convertView.findViewById<ProgressBar>(R.id.progress_view)
        val fingerDragHelper: FingerDragHelper = convertView.findViewById(R.id.fingerDragHelper)
        val imageView: SubsamplingScaleImageViewDragClose = convertView.findViewById(R.id.photo_view)
        val imageGif: PhotoView = convertView.findViewById(R.id.gif_view)

        val info = imageMyList[position]
        val originPathUrl = info.originUrl
        val thumbPathUrl = info.thumbnailUrl

        imageView.setMinimumScaleType(SubsamplingScaleImageViewDragClose.SCALE_TYPE_CENTER_INSIDE)
        imageView.setDoubleTapZoomStyle(SubsamplingScaleImageViewDragClose.ZOOM_FOCUS_CENTER)
        imageView.setDoubleTapZoomDuration(ImagePreview.instance.zoomTransitionDuration)
        imageView.minScale = ImagePreview.instance.minScale
        imageView.maxScale = ImagePreview.instance.maxScale
        imageView.setDoubleTapZoomScale(ImagePreview.instance.mediumScale)

        imageGif.setZoomTransitionDuration(ImagePreview.instance.zoomTransitionDuration)
        imageGif.minimumScale = ImagePreview.instance.minScale
        imageGif.maximumScale = ImagePreview.instance.maxScale
        imageGif.scaleType = ImageView.ScaleType.FIT_CENTER

        imageView.setOnClickListener { v ->
            if (ImagePreview.instance.isEnableClickClose) {
                activity.onBackPressed()
            }
            ImagePreview.instance.bigImageClickListener?.onClick(activity, v, position)
        }
        imageGif.setOnClickListener { v ->
            if (ImagePreview.instance.isEnableClickClose) {
                activity.onBackPressed()
            }
            ImagePreview.instance.bigImageClickListener?.onClick(activity, v, position)
        }
        imageView.setOnLongClickListener { v ->
            ImagePreview.instance.bigImageLongClickListener?.onLongClick(activity, v, position)
            true
        }
        imageGif.setOnLongClickListener { v ->
            ImagePreview.instance.bigImageLongClickListener?.onLongClick(activity, v, position)
            true
        }

        if (activity is ImagePreviewActivity) {
            activity.setAlpha(1f)
        }

        if (ImagePreview.instance.isEnableDragClose) {
            fingerDragHelper.setOnAlphaChangeListener { _, translationY ->
                val yAbs = abs(translationY)
                val percent = yAbs / getPhoneHei(activity.applicationContext)
                val number = 1.0f - percent
                if (activity is ImagePreviewActivity) {
                    activity.setAlpha(number)
                }
                if (imageGif.visibility == View.VISIBLE) {
                    imageGif.scaleY = number
                    imageGif.scaleX = number
                }
                if (imageView.visibility == View.VISIBLE) {
                    imageView.scaleY = number
                    imageView.scaleX = number
                }
            }
        }

        imageGifHashMap.remove(originPathUrl)
        imageGifHashMap[originPathUrl + "_" + position] = imageGif
        imageHashMap.remove(originPathUrl)
        imageHashMap[originPathUrl + "_" + position] = imageView

        val loadStrategy = ImagePreview.instance.loadStrategy
        // 根据当前加载策略判断，需要加载的url是哪一个
        when (loadStrategy) {
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
                finalLoadUrl = if (isWiFi(activity)) {
                    originPathUrl
                } else {
                    thumbPathUrl
                }
            }
            LoadStrategy.Auto -> {
                finalLoadUrl = if (isWiFi(activity)) {
                    originPathUrl
                } else {
                    thumbPathUrl
                }
            }
        }
        finalLoadUrl = finalLoadUrl?.trim()
        val url: String? = finalLoadUrl

        // 显示加载圈圈
        progressBar.visibility = View.VISIBLE

        // 判断原图缓存是否存在，存在的话，直接显示原图缓存，优先保证清晰。
        val cacheFile = getGlideCacheFile(activity, originPathUrl)
        if (cacheFile != null && cacheFile.exists()) {
            Log.d("instantiateItem", "原图缓存存在，直接显示")
            val imagePath = cacheFile.absolutePath
            val isStandardImage = originPathUrl?.let { isStandardImage(it, imagePath) }
            if (isStandardImage == true) {
                loadImageStandard(imagePath, imageView, imageGif, progressBar)
            } else {
                loadImageSpec(url, imagePath, imageView, imageGif, progressBar)
            }
        } else {
            Log.d("instantiateItem", "原图缓存不存在，开始加载 url = $url")
            Glide.with(activity).downloadOnly().load(url).addListener(object : RequestListener<File> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any, target: Target<File>,
                    isFirstResource: Boolean
                ): Boolean {
                    Thread {
                        val fileFullName = System.currentTimeMillis().toString()
                        val saveDir = getAvailableCacheDir(activity)?.absolutePath + File.separator + "image/"
                        val downloadFile = downloadFile(url, fileFullName, saveDir)
                        Handler(Looper.getMainLooper()).post {
                            if (downloadFile != null && downloadFile.exists() && downloadFile.length() > 0) {
                                // 通过urlConn下载完成
                                loadSuccess(originPathUrl, downloadFile, imageView, imageGif, progressBar)
                            } else {
                                loadFailed(imageView, imageGif, progressBar, e)
                            }
                        }
                    }.start()
                    return true
                }

                override fun onResourceReady(
                    resource: File, model: Any, target: Target<File>, dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    loadSuccess(url, resource, imageView, imageGif, progressBar)
                    return true
                }
            }).into(object : FileTarget() {
            })
        }
        container.addView(convertView)
        return convertView
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
        super.setPrimaryItem(container, position, `object`)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

    private fun loadFailed(
        imageView: SubsamplingScaleImageViewDragClose, imageGif: ImageView, progressBar: ProgressBar,
        e: GlideException?
    ) {
        progressBar.visibility = View.GONE
        imageGif.visibility = View.GONE
        imageView.visibility = View.VISIBLE
        imageView.isZoomEnabled = false
        imageView.setImage(ImageSource.resource(ImagePreview.instance.errorPlaceHolder))
        if (ImagePreview.instance.isShowErrorToast) {
            var errorMsg = activity.getString(R.string.toast_load_failed)
            if (e != null) {
                errorMsg = e.localizedMessage as String
            }
            if (errorMsg.length > 200) {
                errorMsg = errorMsg.substring(0, 199)
            }
            ToastUtil.instance.showShort(activity.applicationContext, errorMsg)
        }
    }

    private fun loadSuccess(
        imageUrl: String?, resource: File, imageView: SubsamplingScaleImageViewDragClose, imageGif: ImageView,
        progressBar: ProgressBar
    ) {
        val imagePath = resource.absolutePath
        val isStandardImage = imageUrl?.let { isStandardImage(it, imagePath) }
        if (isStandardImage == true) {
            loadImageStandard(imagePath, imageView, imageGif, progressBar)
        } else {
            loadImageSpec(imageUrl, imagePath, imageView, imageGif, progressBar)
        }
    }

    private fun setImageSpec(imagePath: String, imageView: SubsamplingScaleImageViewDragClose) {
        val isLongImage = isLongImage(activity, imagePath)
        if (isLongImage) {
            imageView.setMinimumScaleType(SubsamplingScaleImageViewDragClose.SCALE_TYPE_START)
            imageView.minScale = getLongImageMinScale(activity, imagePath)
            imageView.maxScale = getLongImageMaxScale(activity, imagePath)
            imageView.setDoubleTapZoomScale(getLongImageMaxScale(activity, imagePath))
        } else {
            val isWideImage = isWideImage(activity, imagePath)
            val isSmallImage = isSmallImage(activity, imagePath)
            when {
                isWideImage -> {
                    imageView.setMinimumScaleType(SubsamplingScaleImageViewDragClose.SCALE_TYPE_CENTER_INSIDE)
                    imageView.minScale = ImagePreview.instance.minScale
                    imageView.maxScale = ImagePreview.instance.maxScale
                    imageView.setDoubleTapZoomScale(
                        getWideImageDoubleScale(
                            activity, imagePath
                        )
                    )
                }
                isSmallImage -> {
                    imageView.setMinimumScaleType(SubsamplingScaleImageViewDragClose.SCALE_TYPE_CUSTOM)
                    imageView.minScale = getSmallImageMinScale(activity, imagePath)
                    imageView.maxScale = getSmallImageMaxScale(activity, imagePath)
                    imageView.setDoubleTapZoomScale(getSmallImageMaxScale(activity, imagePath))
                }
                else -> {
                    imageView.setMinimumScaleType(SubsamplingScaleImageViewDragClose.SCALE_TYPE_CENTER_INSIDE)
                    imageView.minScale = ImagePreview.instance.minScale
                    imageView.maxScale = ImagePreview.instance.maxScale
                    imageView.setDoubleTapZoomScale(ImagePreview.instance.mediumScale)
                }
            }
        }
    }

    private fun loadImageStandard(
        imagePath: String, imageView: SubsamplingScaleImageViewDragClose,
        imageGif: ImageView, progressBar: ProgressBar
    ) {
        imageGif.visibility = View.GONE
        imageView.visibility = View.VISIBLE
        setImageSpec(imagePath, imageView)
        imageView.orientation = SubsamplingScaleImageViewDragClose.ORIENTATION_USE_EXIF
        val imageSource = ImageSource.uri(Uri.fromFile(File(imagePath)))
        if (isBmpImageWithMime(imagePath, imagePath)) {
            imageSource.tilingDisabled()
        }
        imageView.setImage(imageSource)
        imageView.setOnImageEventListener(object : SimpleOnImageEventListener() {
            override fun onReady() {
                progressBar.visibility = View.GONE
            }
        })
    }

    private fun loadImageSpec(
        imageUrl: String?, imagePath: String, imageView: SubsamplingScaleImageViewDragClose,
        imageSpec: ImageView, progressBar: ProgressBar
    ) {
        imageSpec.visibility = View.VISIBLE
        imageView.visibility = View.GONE
        val isGifFile = imageUrl?.let { isGifImageWithMime(it, imagePath) }
        if (isGifFile == true) {
            Glide.with(activity)
                .asGif()
                .load(imagePath)
                .apply(
                    RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE).error(ImagePreview.instance.errorPlaceHolder)
                )
                .listener(object : RequestListener<GifDrawable?> {
                    override fun onLoadFailed(
                        e: GlideException?, model: Any, target: Target<GifDrawable?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.visibility = View.GONE
                        imageSpec.visibility = View.GONE
                        imageView.visibility = View.VISIBLE
                        imageView.setImage(ImageSource.resource(ImagePreview.instance.errorPlaceHolder))
                        return false
                    }

                    override fun onResourceReady(
                        resource: GifDrawable?, model: Any, target: Target<GifDrawable?>,
                        dataSource: DataSource, isFirstResource: Boolean
                    ): Boolean {
                        progressBar.visibility = View.GONE
                        return false
                    }
                })
                .into(imageSpec)
        } else {
            Glide.with(activity)
                .load(imageUrl)
                .apply(
                    RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE).error(ImagePreview.instance.errorPlaceHolder)
                )
                .listener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any,
                        target: Target<Drawable?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.visibility = View.GONE
                        imageSpec.visibility = View.GONE
                        imageView.visibility = View.VISIBLE
                        imageView.setImage(ImageSource.resource(ImagePreview.instance.errorPlaceHolder))
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any,
                        target: Target<Drawable?>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.visibility = View.GONE
                        return false
                    }
                })
                .into(imageSpec)
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val originUrl = imageMyList[position].originUrl + "_" + position
        try {
            val imageViewDragClose = imageHashMap[originUrl]
            imageViewDragClose?.resetScaleAndCenter()
            imageViewDragClose?.destroyDrawingCache()
            imageViewDragClose?.recycle()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            val photoView = imageGifHashMap[originUrl]
            photoView?.destroyDrawingCache()
            photoView?.setImageBitmap(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            clearMemory(activity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}