package cc.shinichi.library.tool.image

import android.app.Activity
import android.content.ContentValues
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import cc.shinichi.library.ImagePreview
import cc.shinichi.library.R
import cc.shinichi.library.glide.FileTarget
import cc.shinichi.library.tool.common.HttpUtil.downloadFile
import cc.shinichi.library.tool.file.FileUtil.Companion.copyFile
import cc.shinichi.library.tool.file.FileUtil.Companion.createFileByDeleteOldFile
import cc.shinichi.library.tool.file.FileUtil.Companion.getAvailableCacheDir
import cc.shinichi.library.tool.file.SingleMediaScanner
import cc.shinichi.library.tool.image.ImageUtil.refresh
import cc.shinichi.library.tool.common.ToastUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.request.transition.Transition
import java.io.*

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 * create at 2018/5/4  16:34
 * description:图片下载工具类
 */
object DownloadUtil {

    fun downloadPicture(context: Activity, currentItem: Int, url: String?) {
        Glide.with(context).downloadOnly().load(url).into(object : FileTarget() {
            override fun onLoadStarted(placeholder: Drawable?) {
                super.onLoadStarted(placeholder)
                if (ImagePreview.instance.downloadListener != null) {
                    ImagePreview.instance.downloadListener?.onDownloadStart(context, currentItem)
                } else {
                    ToastUtil.instance.showShort(
                        context,
                        context.getString(R.string.toast_start_download)
                    )
                }
                super.onLoadStarted(placeholder)
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                super.onLoadFailed(errorDrawable)
                if (ImagePreview.instance.downloadListener != null) {
                    ImagePreview.instance.downloadListener?.onDownloadFailed(context, currentItem)
                } else {
                    ToastUtil.instance.showShort(
                        context,
                        context.getString(R.string.toast_save_failed)
                    )
                }
            }

            override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                super.onResourceReady(resource, transition)
                saveImage(context, resource, currentItem)
            }
        })
    }

    private fun saveImage(context: Activity, resource: File, currentItem: Int) {
        // 传入的保存文件夹名
        val downloadFolderName = ImagePreview.instance.folderName
        // 保存的图片名称
        var name = System.currentTimeMillis().toString()
        val mimeType = ImageUtil.getImageTypeWithMime(resource.absolutePath)
        name = "$name.$mimeType"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 大于等于29版本的保存方法
            val resolver = context.contentResolver
            // 设置文件参数到ContentValues中
            val values = ContentValues()
            values.put(MediaStore.Images.Media.DISPLAY_NAME, name)
            values.put(MediaStore.Images.Media.DESCRIPTION, name)
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/$mimeType")
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + downloadFolderName + "/")
            val insertUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            var inputStream: BufferedInputStream? = null
            var os: OutputStream? = null
            try {
                inputStream = BufferedInputStream(FileInputStream(resource.absolutePath))
                os = insertUri?.let { resolver.openOutputStream(it) }
                os?.let {
                    val buffer = ByteArray(1024 * 4)
                    var len: Int
                    while (inputStream.read(buffer).also { len = it } != -1) {
                        os.write(buffer, 0, len)
                    }
                    os.flush()
                }
                if (ImagePreview.instance.downloadListener != null) {
                    ImagePreview.instance.downloadListener?.onDownloadSuccess(context, currentItem)
                } else {
                    ToastUtil.instance.showShort(
                        context,
                        context.getString(
                            R.string.toast_save_success,
                            "$downloadFolderName/$name"
                        )
                    )
                }
                insertUri?.refresh(resolver)
            } catch (e: IOException) {
                e.printStackTrace()
                if (ImagePreview.instance.downloadListener != null) {
                    ImagePreview.instance.downloadListener?.onDownloadFailed(context, currentItem)
                } else {
                    ToastUtil.instance.showShort(
                        context,
                        context.getString(R.string.toast_save_failed)
                    )
                }
            } finally {
                try {
                    os?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                try {
                    inputStream?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } else {
            // 低于29版本的保存方法
            val path = Environment.getExternalStorageDirectory().toString() + "/" + downloadFolderName + "/"
            createFileByDeleteOldFile(path + name)
            val result = copyFile(resource, path, name)
            if (result) {
                if (ImagePreview.instance.downloadListener != null) {
                    ImagePreview.instance.downloadListener?.onDownloadSuccess(context, currentItem)
                } else {
                    ToastUtil.instance.showShort(
                        context,
                        context.getString(R.string.toast_save_success, path)
                    )
                }
                SingleMediaScanner(context, path + name, object : SingleMediaScanner.ScanListener {
                    override fun onScanFinish() {
                        // scanning...
                    }
                })
            } else {
                if (ImagePreview.instance.downloadListener != null) {
                    ImagePreview.instance.downloadListener?.onDownloadFailed(context, currentItem)
                } else {
                    ToastUtil.instance.showShort(
                        context,
                        context.getString(R.string.toast_save_failed)
                    )
                }
            }
        }
    }

    fun downloadVideo(context: Activity, currentItem: Int, url: String?) {
        if (ImagePreview.instance.downloadListener != null) {
            ImagePreview.instance.downloadListener?.onDownloadStart(context, currentItem)
        } else {
            ToastUtil.instance.showShort(
                context,
                context.getString(R.string.toast_start_download)
            )
        }
        Thread {
            val saveDir = getAvailableCacheDir(context)?.absolutePath + File.separator + "video/"
            val fileFullName = System.currentTimeMillis().toString() + ".mp4"
            val downloadFile = downloadFile(url, fileFullName, saveDir)
            Handler(Looper.getMainLooper()).post {
                if (downloadFile != null && downloadFile.exists() && downloadFile.length() > 0) {
                    // 通过urlConn下载完成
                    saveVideo(context, downloadFile, currentItem)
                } else {
                    if (ImagePreview.instance.downloadListener != null) {
                        ImagePreview.instance.downloadListener?.onDownloadFailed(context, currentItem)
                    } else {
                        ToastUtil.instance.showShort(
                            context,
                            context.getString(R.string.toast_save_failed)
                        )
                    }
                }
            }
        }.start()
    }

    private fun saveVideo(context: Activity, resource: File, currentItem: Int) {
        // 传入的保存文件夹名
        val downloadFolderName = ImagePreview.instance.folderName
        // 保存的名称
        var name = System.currentTimeMillis().toString() + ".mp4"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 大于等于29版本的保存方法
            val resolver = context.contentResolver
            // 设置文件参数到ContentValues中
            val values = ContentValues()
            values.put(MediaStore.Video.Media.DISPLAY_NAME, name)
            values.put(MediaStore.Video.Media.DESCRIPTION, name)
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/" + downloadFolderName + "/")
            val insertUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            var inputStream: BufferedInputStream? = null
            var os: OutputStream? = null
            try {
                inputStream = BufferedInputStream(FileInputStream(resource.absolutePath))
                os = insertUri?.let { resolver.openOutputStream(it) }
                os?.let {
                    val buffer = ByteArray(1024 * 4)
                    var len: Int
                    while (inputStream.read(buffer).also { len = it } != -1) {
                        os.write(buffer, 0, len)
                    }
                    os.flush()
                }
                if (ImagePreview.instance.downloadListener != null) {
                    ImagePreview.instance.downloadListener?.onDownloadSuccess(context, currentItem)
                } else {
                    ToastUtil.instance.showShort(
                        context,
                        context.getString(
                            R.string.toast_save_success,
                            "$downloadFolderName/$name"
                        )
                    )
                }
                insertUri?.refresh(resolver)
            } catch (e: IOException) {
                e.printStackTrace()
                if (ImagePreview.instance.downloadListener != null) {
                    ImagePreview.instance.downloadListener?.onDownloadFailed(context, currentItem)
                } else {
                    ToastUtil.instance.showShort(
                        context,
                        context.getString(R.string.toast_save_failed)
                    )
                }
            } finally {
                try {
                    os?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                try {
                    inputStream?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } else {
            // 低于29版本的保存方法
            val path = Environment.getExternalStorageDirectory().toString() + "/" + downloadFolderName + "/"
            createFileByDeleteOldFile(path + name)
            val result = copyFile(resource, path, name)
            if (result) {
                if (ImagePreview.instance.downloadListener != null) {
                    ImagePreview.instance.downloadListener?.onDownloadSuccess(context, currentItem)
                } else {
                    ToastUtil.instance.showShort(
                        context,
                        context.getString(R.string.toast_save_success, path)
                    )
                }
                SingleMediaScanner(context, path + name, object : SingleMediaScanner.ScanListener {
                    override fun onScanFinish() {
                        // scanning...
                    }
                })
            } else {
                if (ImagePreview.instance.downloadListener != null) {
                    ImagePreview.instance.downloadListener?.onDownloadFailed(context, currentItem)
                } else {
                    ToastUtil.instance.showShort(
                        context,
                        context.getString(R.string.toast_save_failed)
                    )
                }
            }
        }
    }
}