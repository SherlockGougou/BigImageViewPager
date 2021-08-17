package cc.shinichi.bigimageviewpager.glide;

import com.bumptech.glide.load.model.GlideUrl;

public class CustomGlideUrl extends GlideUrl {

    private String url;

    public CustomGlideUrl(String url) {
        super(url);
        this.url = url;
    }

    @Override
    public String getCacheKey() {
        return generateUrl();
    }

    /**
     * 此处是例子，意为去除动态变化的部分，只保留固定不变的，作为唯一的key
     * @return
     */
    private String generateUrl() {
        if (url.contains("id=")) {
            return url.substring(0, url.indexOf("id="));
        }
        return url;
    }
}