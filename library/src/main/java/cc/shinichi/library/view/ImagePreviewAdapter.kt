package cc.shinichi.library.view

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
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
import cc.shinichi.library.tool.image.ImageUtil
import cc.shinichi.library.tool.image.ImageUtil.getBitmapDegree
import cc.shinichi.library.tool.image.ImageUtil.getImageBitmap
import cc.shinichi.library.tool.image.ImageUtil.getImageDoubleScale
import cc.shinichi.library.tool.image.ImageUtil.getLongImageDoubleZoomScale
import cc.shinichi.library.tool.image.ImageUtil.getWideImageDoubleScale
import cc.shinichi.library.tool.image.ImageUtil.getWidthHeight
import cc.shinichi.library.tool.image.ImageUtil.isAnimWebp
import cc.shinichi.library.tool.image.ImageUtil.isBmpImageWithMime
import cc.shinichi.library.tool.image.ImageUtil.isHeifImageWithMime
import cc.shinichi.library.tool.image.ImageUtil.isLongImage
import cc.shinichi.library.tool.image.ImageUtil.isStaticImage
import cc.shinichi.library.tool.image.ImageUtil.isTabletOrLandscape
import cc.shinichi.library.tool.image.ImageUtil.isWideImage
import cc.shinichi.library.tool.ui.PhoneUtil.getPhoneHei
import cc.shinichi.library.tool.ui.ToastUtil
import cc.shinichi.library.view.helper.FingerDragHelper
import cc.shinichi.library.view.listener.SimpleOnImageEventListener
import cc.shinichi.library.view.photoview.PhotoView
import cc.shinichi.library.view.subsampling.ImageSource
import cc.shinichi.library.view.subsampling.SubsamplingScaleImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.FitCenter
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
class ImagePreviewAdapter(private val activity: AppCompatActivity, imageList: MutableList<ImageInfo>) :
    PagerAdapter() {

    private val imageMyList: MutableList<ImageInfo> = mutableListOf()
    private val imageStaticHashMap: HashMap<String, SubsamplingScaleImageView?> = HashMap()
    private val imageAnimHashMap: HashMap<String, PhotoView?> = HashMap()
    private var finalLoadUrl: String = ""

    init {
        imageMyList.addAll(imageList)
    }

    fun closePage() {
        try {
            if (imageStaticHashMap.size > 0) {
                for (o in imageStaticHashMap.entries) {
                    val entry = o as Map.Entry<*, *>
                    if (entry.value != null) {
                        (entry.value as SubsamplingScaleImageView).destroyDrawingCache()
                        (entry.value as SubsamplingScaleImageView).recycle()
                    }
                }
                imageStaticHashMap.clear()
            }
            if (imageAnimHashMap.size > 0) {
                for (o in imageAnimHashMap.entries) {
                    val entry = o as Map.Entry<*, *>
                    if (entry.value != null) {
                        (entry.value as PhotoView).destroyDrawingCache()
                        (entry.value as PhotoView).setImageBitmap(null)
                    }
                }
                imageAnimHashMap.clear()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getCount(): Int {
        return imageMyList.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val convertView = View.inflate(activity, R.layout.sh_item_photoview, null)
        val progressBar = convertView.findViewById<ProgressBar>(R.id.progress_view)
        val fingerDragHelper: FingerDragHelper = convertView.findViewById(R.id.fingerDragHelper)
        val imageStatic: SubsamplingScaleImageView = convertView.findViewById(R.id.static_view)
        val imageAnim: PhotoView = convertView.findViewById(R.id.anim_view)

        val info = imageMyList[position]
        val originPathUrl = info.originUrl
        val thumbPathUrl = info.thumbnailUrl

        imageStatic.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
        imageStatic.setDoubleTapZoomStyle(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER)
        imageStatic.setDoubleTapZoomDuration(ImagePreview.instance.zoomTransitionDuration)

        imageAnim.setZoomTransitionDuration(ImagePreview.instance.zoomTransitionDuration)
        imageAnim.minimumScale = ImagePreview.instance.minScale
        imageAnim.maximumScale = ImagePreview.instance.maxScale
        imageAnim.scaleType = ImageView.ScaleType.FIT_CENTER

        imageStatic.setOnClickListener { v ->
            if (ImagePreview.instance.isEnableClickClose) {
                activity.onBackPressed()
            }
            ImagePreview.instance.bigImageClickListener?.onClick(activity, v, position)
        }
        imageAnim.setOnClickListener { v ->
            if (ImagePreview.instance.isEnableClickClose) {
                activity.onBackPressed()
            }
            ImagePreview.instance.bigImageClickListener?.onClick(activity, v, position)
        }
        imageStatic.setOnLongClickListener { v ->
            ImagePreview.instance.bigImageLongClickListener?.onLongClick(activity, v, position)
            true
        }
        imageAnim.setOnLongClickListener { v ->
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
                if (imageAnim.visibility == View.VISIBLE) {
                    imageAnim.scaleY = number
                    imageAnim.scaleX = number
                }
                if (imageStatic.visibility == View.VISIBLE) {
                    imageStatic.scaleY = number
                    imageStatic.scaleX = number
                }
            }
        }

        imageAnimHashMap.remove(originPathUrl)
        imageAnimHashMap[originPathUrl + "_" + position] = imageAnim
        imageStaticHashMap.remove(originPathUrl)
        imageStaticHashMap[originPathUrl + "_" + position] = imageStatic

        // 根据当前加载策略判断，需要加载的url是哪一个
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
        finalLoadUrl = finalLoadUrl.trim()
        val url: String = finalLoadUrl

        // 显示加载圈圈
        progressBar.visibility = View.VISIBLE

        // 判断原图缓存是否存在，存在的话，直接显示原图缓存，优先保证清晰。
        val cacheFile = getGlideCacheFile(activity, originPathUrl)
        if (cacheFile != null && cacheFile.exists()) {
            Log.d("instantiateItem", "原图缓存存在，直接显示 originPathUrl = $originPathUrl")
            val imagePath = cacheFile.absolutePath
            val isStatic = isStaticImage(originPathUrl, imagePath)
            if (isStatic) {
                Log.d("instantiateItem", "动静判断: 静态图")
                loadImageStatic(imagePath, imageStatic, imageAnim, progressBar)
            } else {
                Log.d("instantiateItem", "动静判断: 动态图")
                loadImageAnim(originPathUrl, imagePath, imageStatic, imageAnim, progressBar)
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
                                loadSuccess(originPathUrl, downloadFile, imageStatic, imageAnim, progressBar)
                            } else {
                                loadFailed(imageStatic, imageAnim, progressBar, e)
                            }
                        }
                    }.start()
                    return true
                }

                override fun onResourceReady(
                    resource: File, model: Any, target: Target<File>, dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    loadSuccess(url, resource, imageStatic, imageAnim, progressBar)
                    return true
                }
            }).into(object : FileTarget() {
            })
        }
        container.addView(convertView)
        return convertView
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

    /**
     * 加载原图
     */
    fun loadOrigin(imageInfo: ImageInfo) {
        val originalUrl = imageInfo.originUrl
        if (imageStaticHashMap[originalUrl] != null && imageAnimHashMap[originalUrl] != null) {
            val imageStatic = imageStaticHashMap[imageInfo.originUrl]
            val imageAnim = imageAnimHashMap[imageInfo.originUrl]
            val cacheFile = getGlideCacheFile(activity, imageInfo.originUrl)
            if (cacheFile != null && cacheFile.exists()) {
                val isStatic = isStaticImage(originalUrl, cacheFile.absolutePath)
                if (isStatic) {
                    Log.d("loadOrigin", "动静判断: 静态图")
                    val isHeifImageWithMime = isHeifImageWithMime(imageInfo.originUrl, cacheFile.absolutePath)
                    if (isHeifImageWithMime) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            SubsamplingScaleImageView.setPreferredBitmapConfig(Bitmap.Config.ALPHA_8)
                        } else {
                            SubsamplingScaleImageView.setPreferredBitmapConfig(Bitmap.Config.ARGB_8888)
                        }
                    }
                    imageAnim?.visibility = View.GONE
                    imageStatic?.visibility = View.VISIBLE
                    imageStatic?.let {
                        val thumbnailUrl = imageInfo.thumbnailUrl
                        val smallCacheFile = getGlideCacheFile(activity, thumbnailUrl)
                        var small: ImageSource? = null
                        if (smallCacheFile != null && smallCacheFile.exists()) {
                            val smallImagePath = smallCacheFile.absolutePath
                            small = getImageBitmap(smallImagePath, getBitmapDegree(smallImagePath))?.let {
                                ImageSource.bitmap(it)
                            }
                            val widSmall = getWidthHeight(smallImagePath)[0]
                            val heiSmall = getWidthHeight(smallImagePath)[1]
                            if (isBmpImageWithMime(originalUrl, cacheFile.absolutePath)) {
                                small?.tilingDisabled()
                            }
                            small?.dimensions(widSmall, heiSmall)
                        }
                        val imagePath = cacheFile.absolutePath
                        val origin = ImageSource.uri(imagePath)
                        val widOrigin = getWidthHeight(imagePath)[0]
                        val heiOrigin = getWidthHeight(imagePath)[1]
                        if (isBmpImageWithMime(originalUrl, cacheFile.absolutePath)) {
                            origin.tilingDisabled()
                        }
                        origin.dimensions(widOrigin, heiOrigin)
                        setImageStatic(imagePath, imageStatic)
                        imageStatic.orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF
                        imageStatic.setImage(origin, small)
                    }
                } else {
                    Log.d("loadOrigin", "动静判断: 动态图")
                    imageStatic?.visibility = View.GONE
                    imageAnim?.visibility = View.VISIBLE
                    imageAnim?.let {
                        Glide.with(activity)
                            .asGif()
                            .load(cacheFile)
                            .apply(
                                RequestOptions()
                                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                    .error(ImagePreview.instance.errorPlaceHolder)
                            )
                            .into(imageAnim)
                    }
                }
            } else {
                notifyDataSetChanged()
            }
        } else {
            notifyDataSetChanged()
        }
    }

    private fun loadSuccess(
        imageUrl: String, resource: File, imageStatic: SubsamplingScaleImageView, imageAnim: ImageView,
        progressBar: ProgressBar
    ) {
        val imagePath = resource.absolutePath
        val isStatic = isStaticImage(imageUrl, imagePath)
        if (isStatic) {
            Log.d("loadSuccess", "动静判断: 静态图")
            loadImageStatic(imagePath, imageStatic, imageAnim, progressBar)
        } else {
            Log.d("loadSuccess", "动静判断: 动态图")
            loadImageAnim(imageUrl, imagePath, imageStatic, imageAnim, progressBar)
        }
    }

    /**
     * 加载失败的处理
     */
    private fun loadFailed(
        imageStatic: SubsamplingScaleImageView, imageAnim: ImageView, progressBar: ProgressBar,
        e: GlideException?
    ) {
        progressBar.visibility = View.GONE
        imageAnim.visibility = View.GONE
        imageStatic.visibility = View.VISIBLE
        imageStatic.isZoomEnabled = false
        imageStatic.setImage(ImageSource.resource(ImagePreview.instance.errorPlaceHolder))
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

    private fun setImageStatic(imagePath: String, imageStatic: SubsamplingScaleImageView) {
        val isHeifImageWithMime = isHeifImageWithMime("", imagePath)
        if (isHeifImageWithMime) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                SubsamplingScaleImageView.setPreferredBitmapConfig(Bitmap.Config.ALPHA_8)
            } else {
                SubsamplingScaleImageView.setPreferredBitmapConfig(Bitmap.Config.ARGB_8888)
            }
        }
        val tabletOrLandscape = isTabletOrLandscape(activity)
        if (tabletOrLandscape) {
            // Tablet
            imageStatic.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
            imageStatic.minScale = ImagePreview.instance.minScale
            imageStatic.maxScale = ImagePreview.instance.maxScale
            imageStatic.setDoubleTapZoomScale(ImagePreview.instance.mediumScale)
        } else {
            // Phone
            imageStatic.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
            imageStatic.minScale = 1f
            val isLongImage = isLongImage(activity, imagePath)
            if (isLongImage) {
                // 长图
                imageStatic.maxScale = ImageUtil.getLongImageMaxZoomScale(activity, imagePath)
                imageStatic.setDoubleTapZoomScale(getLongImageDoubleZoomScale(activity, imagePath))
            } else {
                val isWideImage = isWideImage(imagePath)
                if (isWideImage) {
                    // 宽图
                    imageStatic.maxScale = ImageUtil.getWideImageMaxZoomScale(activity, imagePath)
                    imageStatic.setDoubleTapZoomScale(getWideImageDoubleScale(activity, imagePath))
                } else {
                    // 普通图片
                    imageStatic.maxScale = ImageUtil.getImageMaxZoomScale(activity, imagePath)
                    imageStatic.setDoubleTapZoomScale(getImageDoubleScale(activity, imagePath))
                }
            }
        }
    }

    /**
     * 加载静态图片
     */
    private fun loadImageStatic(
        imagePath: String, imageStatic: SubsamplingScaleImageView, imageAnim: ImageView,
        progressBar: ProgressBar
    ) {
        imageAnim.visibility = View.GONE
        imageStatic.visibility = View.VISIBLE
        setImageStatic(imagePath, imageStatic)
        imageStatic.orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF
        val imageSource = ImageSource.uri(Uri.fromFile(File(imagePath)))
        if (isBmpImageWithMime(imagePath, imagePath)) {
            imageSource.tilingDisabled()
        }
        imageStatic.setImage(imageSource)
        imageStatic.setOnImageEventListener(object : SimpleOnImageEventListener() {
            override fun onReady() {
                progressBar.visibility = View.GONE
            }
        })
    }

    /**
     * 加载动图
     */
    private fun loadImageAnim(
        imageUrl: String, imagePath: String, imageStatic: SubsamplingScaleImageView,
        imageAnim: ImageView, progressBar: ProgressBar
    ) {
        imageAnim.visibility = View.VISIBLE
        imageStatic.visibility = View.GONE
        if (isAnimWebp(imageUrl, imagePath)) {
            val fitCenter: Transformation<Bitmap> = FitCenter()
            Glide.with(activity)
                .load(imagePath)
                .apply(
                    RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .error(ImagePreview.instance.errorPlaceHolder)
                )
                .optionalTransform(fitCenter)
                .optionalTransform(WebpDrawable::class.java, WebpDrawableTransformation(fitCenter))
                .addListener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.visibility = View.GONE
                        return false
                    }
                })
                .into(imageAnim)
        } else {
            Glide.with(activity)
                .asGif()
                .load(imagePath)
                .apply(
                    RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .error(ImagePreview.instance.errorPlaceHolder)
                )
                .listener(object : RequestListener<GifDrawable?> {
                    override fun onLoadFailed(
                        e: GlideException?, model: Any, target: Target<GifDrawable?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.visibility = View.GONE
                        imageStatic.setImage(ImageSource.resource(ImagePreview.instance.errorPlaceHolder))
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
                .into(imageAnim)
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val originUrl = imageMyList[position].originUrl + "_" + position
        try {
            val imageViewDragClose = imageStaticHashMap[originUrl]
            imageViewDragClose?.resetScaleAndCenter()
            imageViewDragClose?.destroyDrawingCache()
            imageViewDragClose?.recycle()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            val photoView = imageAnimHashMap[originUrl]
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