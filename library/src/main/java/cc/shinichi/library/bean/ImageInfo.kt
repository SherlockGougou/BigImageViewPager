package cc.shinichi.library.bean

import java.io.Serializable

/**
 * 媒体类型枚举
 */
enum class Type {
    IMAGE,
    VIDEO
}

/**
 * 媒体信息数据类
 *
 * @author 工藤
 * @email qinglingou@gmail.com
 *
 * @property type 媒体类型，默认为图片
 * @property thumbnailUrl 缩略图URL
 * @property originUrl 原图/原视频URL
 */
data class ImageInfo(
    var type: Type = Type.IMAGE,
    var thumbnailUrl: String = "",
    var originUrl: String = ""
) : Serializable {

    companion object {
        private const val serialVersionUID = 1L

        /**
         * 创建图片类型的媒体信息
         */
        @JvmStatic
        fun createImage(thumbnailUrl: String, originUrl: String = thumbnailUrl): ImageInfo {
            return ImageInfo(
                type = Type.IMAGE,
                thumbnailUrl = thumbnailUrl,
                originUrl = originUrl
            )
        }

        /**
         * 创建视频类型的媒体信息
         */
        @JvmStatic
        fun createVideo(thumbnailUrl: String, videoUrl: String): ImageInfo {
            return ImageInfo(
                type = Type.VIDEO,
                thumbnailUrl = thumbnailUrl,
                originUrl = videoUrl
            )
        }
    }
}