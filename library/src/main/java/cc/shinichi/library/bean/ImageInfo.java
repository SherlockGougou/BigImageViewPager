package cc.shinichi.library.bean;

import java.io.Serializable;

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 * 图片信息
 */
public class ImageInfo implements Serializable {

    /**
     * 缩略图
     */
    private String thumbnailUrl;

    /**
     * 原图
     */
    private String originUrl;

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public void setOriginUrl(String originUrl) {
        this.originUrl = originUrl;
    }
}