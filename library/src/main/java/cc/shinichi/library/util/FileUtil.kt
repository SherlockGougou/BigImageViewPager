package cc.shinichi.library.util

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * 文件工具类
 *
 * @author 工藤
 * @email qinglingou@gmail.com
 */
object FileUtil {

    private const val TAG = "FileUtil"

    /**
     * 获取可用的缓存目录
     * 优先使用外部存储，不可用时使用内部存储
     */
    @JvmStatic
    fun getAvailableCacheDir(context: Context): File? {
        return if (isExternalStorageWritable()) {
            context.externalCacheDir
        } else {
            context.cacheDir
        }
    }

    /**
     * 检查外部存储是否可写
     */
    @JvmStatic
    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    /**
     * 创建文件，如果已存在则先删除
     *
     * @param filePath 文件路径
     * @return true 成功，false 失败
     */
    @JvmStatic
    fun createFileByDeleteOldFile(filePath: String?): Boolean {
        if (filePath.isNullOrBlank()) return false

        val file = File(filePath)

        // 如果文件存在，尝试删除
        if (file.exists() && !file.delete()) {
            SLog.e(TAG, "Failed to delete existing file: $filePath")
            return false
        }

        // 确保父目录存在
        file.parentFile?.let { parent ->
            if (!parent.exists() && !parent.mkdirs()) {
                SLog.e(TAG, "Failed to create parent directory: ${parent.absolutePath}")
                return false
            }
        }

        return try {
            file.createNewFile()
        } catch (e: IOException) {
            SLog.e(TAG, "Failed to create file: $filePath", e)
            false
        }
    }

    /**
     * 复制文件
     * 使用 NIO FileChannel 提高效率
     *
     * @param sourceFile 源文件
     * @param targetPath 目标路径
     * @param fileName 目标文件名
     * @return true 成功，false 失败
     */
    @JvmStatic
    fun copyFile(sourceFile: File?, targetPath: String?, fileName: String?): Boolean {
        if (sourceFile == null || !sourceFile.exists()) {
            SLog.e(TAG, "Source file is null or does not exist")
            return false
        }
        if (targetPath.isNullOrBlank() || fileName.isNullOrBlank()) {
            SLog.e(TAG, "Target path or file name is invalid")
            return false
        }

        // 确保目标目录存在
        val targetDir = File(targetPath)
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            SLog.e(TAG, "Failed to create target directory: $targetPath")
            return false
        }

        val targetFile = File(targetDir, fileName)

        // 如果目标文件存在，删除它
        if (targetFile.exists()) {
            targetFile.delete()
        }

        return try {
            FileInputStream(sourceFile).channel.use { sourceChannel ->
                FileOutputStream(targetFile).channel.use { targetChannel ->
                    sourceChannel.transferTo(0, sourceChannel.size(), targetChannel)
                }
            }
            SLog.d(TAG, "File copied successfully: ${targetFile.absolutePath}")
            true
        } catch (e: Exception) {
            SLog.e(TAG, "Failed to copy file", e)
            false
        }
    }

    /**
     * 删除文件或目录
     *
     * @param file 要删除的文件或目录
     * @return true 成功，false 失败
     */
    @JvmStatic
    fun deleteFile(file: File?): Boolean {
        if (file == null || !file.exists()) return true

        return if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                deleteFile(child)
            }
            file.delete()
        } else {
            file.delete()
        }
    }

    /**
     * 获取文件大小（格式化字符串）
     */
    @JvmStatic
    fun getFileSizeFormatted(file: File?): String {
        if (file == null || !file.exists()) return "0 B"

        val size = file.length()
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format("%.2f KB", size / 1024.0)
            size < 1024 * 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024))
            else -> String.format("%.2f GB", size / (1024.0 * 1024 * 1024))
        }
    }
}