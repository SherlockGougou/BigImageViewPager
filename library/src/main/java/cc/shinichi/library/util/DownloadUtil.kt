package cc.shinichi.library.util

import android.app.Activity
import android.content.ContentValues
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.net.toUri
import cc.shinichi.library.ImagePreview
import cc.shinichi.library.R
import cc.shinichi.library.loader.FileTarget
import cc.shinichi.library.util.HttpUtil.downloadFile
import cc.shinichi.library.util.ImageUtil.refresh
import cc.shinichi.library.util.UtilExt.isLocalFile
import com.bumptech.glide.Glide
import com.bumptech.glide.request.transition.Transition
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors

/**
 * 媒体文件下载工具类
 *
 * 支持图片和视频的下载保存
 *
 * @author 工藤
 * @email qinglingou@gmail.com
 */
object DownloadUtil {

    private const val TAG = "DownloadUtil"
    private const val BUFFER_SIZE = 4 * 1024 // 4KB

    // 使用单线程池处理下载任务
    private val downloadExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    // ==================== 公共方法 ====================

    /**
     * 下载图片
     */
    @JvmStatic
    fun downloadPicture(context: Activity, currentItem: Int, url: String?) {
        notifyDownloadStart(context, currentItem)

        Glide.with(context.applicationContext)
            .downloadOnly()
            .load(url)
            .into(object : FileTarget() {
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    notifyDownloadFailed(context, currentItem)
                }

                override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                    super.onResourceReady(resource, transition)
                    saveImageToGallery(context, resource, currentItem)
                }
            })
    }

    /**
     * 下载视频
     */
    @JvmStatic
    fun downloadVideo(context: Activity, currentItem: Int, url: String?) {
        notifyDownloadStart(context, currentItem)

        downloadExecutor.execute {
            try {
                if (url?.isLocalFile() == true) {
                    // 本地视频，直接保存
                    runOnMainThread {
                        saveVideoToGallery(context, url, currentItem)
                    }
                } else {
                    // 网络视频，先下载
                    val saveDir = FileUtil.getAvailableCacheDir(context)?.absolutePath +
                            File.separator + "video/"
                    val fileFullName = "${System.currentTimeMillis()}.mp4"
                    val downloadFile = downloadFile(url, fileFullName, saveDir)

                    runOnMainThread {
                        if (downloadFile != null && downloadFile.exists() && downloadFile.length() > 0) {
                            saveVideoToGallery(context, downloadFile.absolutePath, currentItem)
                        } else {
                            notifyDownloadFailed(context, currentItem)
                        }
                    }
                }
            } catch (e: Exception) {
                SLog.e(TAG, "downloadVideo failed", e)
                runOnMainThread {
                    notifyDownloadFailed(context, currentItem)
                }
            }
        }
    }

    // ==================== 图片保存 ====================

    private fun saveImageToGallery(context: Activity, resource: File, currentItem: Int) {
        val folderName = ImagePreview.instance.folderName
        val mimeType = ImageUtil.getImageTypeWithMime(resource.absolutePath)
        val fileName = "${System.currentTimeMillis()}.$mimeType"

        val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveMediaQ(
                context = context,
                sourceFile = resource,
                fileName = fileName,
                mimeType = "image/$mimeType",
                folderName = folderName,
                isVideo = false
            )
        } else {
            saveMediaLegacy(
                context = context,
                sourceFile = resource,
                fileName = fileName,
                folderName = folderName,
                isVideo = false
            )
        }

        if (success) {
            val savePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${Environment.DIRECTORY_PICTURES}/$folderName/$fileName"
            } else {
                "${Environment.getExternalStorageDirectory()}/$folderName/$fileName"
            }
            notifyDownloadSuccess(context, currentItem, savePath)
        } else {
            notifyDownloadFailed(context, currentItem)
        }
    }

    // ==================== 视频保存 ====================

    private fun saveVideoToGallery(context: Activity, resourcePath: String, currentItem: Int) {
        val folderName = ImagePreview.instance.folderName
        val fileName = "${System.currentTimeMillis()}.mp4"

        val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveMediaQ(
                context = context,
                sourcePath = resourcePath,
                fileName = fileName,
                mimeType = "video/mp4",
                folderName = folderName,
                isVideo = true
            )
        } else {
            saveMediaLegacy(
                context = context,
                sourceFile = File(resourcePath),
                fileName = fileName,
                folderName = folderName,
                isVideo = true
            )
        }

        if (success) {
            val savePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${Environment.DIRECTORY_MOVIES}/$folderName/$fileName"
            } else {
                "${Environment.getExternalStorageDirectory()}/$folderName/$fileName"
            }
            notifyDownloadSuccess(context, currentItem, savePath)
        } else {
            notifyDownloadFailed(context, currentItem)
        }
    }

    // ==================== Android Q+ 保存方法 ====================

    private fun saveMediaQ(
        context: Activity,
        sourceFile: File? = null,
        sourcePath: String? = null,
        fileName: String,
        mimeType: String,
        folderName: String,
        isVideo: Boolean
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false

        val resolver = context.contentResolver
        val contentUri = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val relativePath = if (isVideo) {
            "${Environment.DIRECTORY_MOVIES}/$folderName/"
        } else {
            "${Environment.DIRECTORY_PICTURES}/$folderName/"
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }

        val insertUri = resolver.insert(contentUri, values) ?: return false

        return try {
            val inputStream: InputStream = when {
                sourceFile != null -> BufferedInputStream(FileInputStream(sourceFile))
                sourcePath != null -> {
                    val uri = sourcePath.toUri()
                    if ("content" == uri.scheme) {
                        resolver.openInputStream(uri) ?: return false
                    } else {
                        BufferedInputStream(FileInputStream(sourcePath))
                    }
                }

                else -> return false
            }

            inputStream.use { input ->
                resolver.openOutputStream(insertUri)?.use { output ->
                    copyStream(input, output)
                }
            }

            insertUri.refresh(resolver)
            true
        } catch (e: Exception) {
            SLog.e(TAG, "saveMediaQ failed", e)
            false
        }
    }

    // ==================== 旧版保存方法 ====================

    @Suppress("DEPRECATION")
    private fun saveMediaLegacy(
        context: Activity,
        sourceFile: File,
        fileName: String,
        folderName: String,
        isVideo: Boolean
    ): Boolean {
        val path = "${Environment.getExternalStorageDirectory()}/$folderName/"

        if (!FileUtil.createFileByDeleteOldFile(path + fileName)) {
            return false
        }

        val result = FileUtil.copyFile(sourceFile, path, fileName)

        if (result) {
            // 通知媒体库扫描
            SingleMediaScanner(context, path + fileName, object : SingleMediaScanner.ScanListener {
                override fun onScanFinish() {
                    SLog.d(TAG, "Media scan finished")
                }
            })
        }

        return result
    }

    // ==================== 回调通知 ====================

    private fun notifyDownloadStart(context: Activity, currentItem: Int) {
        val listener = ImagePreview.instance.downloadListener
        if (listener != null) {
            listener.onDownloadStart(context, currentItem)
        } else {
            ToastUtil.showShort(context, context.getString(R.string.toast_start_download))
        }
    }

    private fun notifyDownloadSuccess(context: Activity, currentItem: Int, savePath: String) {
        val listener = ImagePreview.instance.downloadListener
        if (listener != null) {
            listener.onDownloadSuccess(context, currentItem)
        } else {
            ToastUtil.showShort(context, context.getString(R.string.toast_save_success, savePath))
        }
    }

    private fun notifyDownloadFailed(context: Activity, currentItem: Int) {
        val listener = ImagePreview.instance.downloadListener
        if (listener != null) {
            listener.onDownloadFailed(context, currentItem)
        } else {
            ToastUtil.showShort(context, context.getString(R.string.toast_save_failed))
        }
    }

    // ==================== 工具方法 ====================

    private fun copyStream(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
        }
        output.flush()
    }

    private fun runOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }
}