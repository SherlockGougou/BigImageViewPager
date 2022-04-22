package cc.shinichi.library.tool.file

import android.content.Context
import android.os.Environment
import android.text.TextUtils
import java.io.*
import java.nio.channels.FileChannel

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 */
class FileUtil {

    companion object {
        /**
         * 获取可用的cache路径
         */
        fun getAvailableCacheDir(context: Context): File? {
            val file: File? = if (isExternalStorageWritable) {
                context.externalCacheDir
            } else {
                context.cacheDir
            }
            return file
        }

        private val isExternalStorageWritable: Boolean
            get() {
                val state = Environment.getExternalStorageState()
                return Environment.MEDIA_MOUNTED == state
            }

        /**
         * Return the file by path.
         *
         * @param filePath The path of file.
         * @return the file
         */
        private fun getFileByPath(filePath: String?): File? {
            return if (isSpace(filePath)) null else filePath?.let { File(it) }
        }

        /**
         * Create a directory if it doesn't exist, otherwise do nothing.
         *
         * @param file The file.
         * @return `true`: exists or creates successfully<br></br>`false`: otherwise
         */
        private fun createOrExistsDir(file: File?): Boolean {
            return file != null && if (file.exists()) file.isDirectory else file.mkdirs()
        }

        /**
         * Create a file if it doesn't exist, otherwise delete old file before creating.
         *
         * @param filePath The path of file.
         * @return `true`: success<br></br>`false`: fail
         */
        fun createFileByDeleteOldFile(filePath: String?): Boolean {
            return createFileByDeleteOldFile(getFileByPath(filePath))
        }

        /**
         * Create a file if it doesn't exist, otherwise delete old file before creating.
         *
         * @param file The file.
         * @return `true`: success<br></br>`false`: fail
         */
        private fun createFileByDeleteOldFile(file: File?): Boolean {
            if (file == null) {
                return false
            }
            // file exists and unsuccessfully delete then return false
            if (file.exists() && !file.delete()) {
                return false
            }
            return if (!createOrExistsDir(file.parentFile)) {
                false
            } else try {
                file.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }

        private fun isSpace(s: String?): Boolean {
            if (s == null) {
                return true
            }
            var i = 0
            val len = s.length
            while (i < len) {
                if (!Character.isWhitespace(s[i])) {
                    return false
                }
                ++i
            }
            return true
        }

        /**
         * 根据文件路径拷贝文件
         *
         * @param resourceFile 源文件
         * @param targetPath   目标路径（包含文件名和文件格式）
         * @return boolean 成功true、失败false
         */
        fun copyFile(resourceFile: File?, targetPath: String, fileName: String): Boolean {
            var result = false
            if (resourceFile == null || TextUtils.isEmpty(targetPath)) {
                return result
            }
            val target = File(targetPath)
            if (target.exists()) {
                target.delete() // 已存在的话先删除
            } else {
                try {
                    target.mkdirs()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val targetFile = File(targetPath + fileName)
            if (targetFile.exists()) {
                targetFile.delete()
            } else {
                try {
                    targetFile.createNewFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            var resourceChannel: FileChannel? = null
            var targetChannel: FileChannel? = null
            try {
                resourceChannel = FileInputStream(resourceFile).channel
                targetChannel = FileOutputStream(targetFile).channel
                resourceChannel.transferTo(0, resourceChannel.size(), targetChannel)
                result = true
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                return result
            } catch (e: IOException) {
                e.printStackTrace()
                return result
            }
            try {
                resourceChannel.close()
                targetChannel.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return result
        }
    }

    init {
        throw UnsupportedOperationException("u can't instantiate me...")
    }
}