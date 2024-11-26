package cc.shinichi.library.bean

import java.io.Serializable

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 * 图片信息
 */
class ImageInfo : Serializable {
    /**
     * 类型
     */
    var type: Type = Type.IMAGE // image / video
    /**
     * 缩略图
     */
    var thumbnailUrl: String = ""
    /**
     * 原图
     */
    var originUrl: String = ""
}

enum class Type {
    IMAGE, VIDEO
}