package com.bumptech.glide.integration.webp.decoder;

import com.bumptech.glide.load.engine.Initializable;
import com.bumptech.glide.load.resource.drawable.DrawableResource;

/**
 * @author liuchun
 */
public class WebpDrawableResource extends DrawableResource<WebpDrawable> implements Initializable {

    public WebpDrawableResource(WebpDrawable drawable) {
        super(drawable);
    }

    public Class<WebpDrawable> getResourceClass() {
        return WebpDrawable.class;
    }

    public int getSize() {
        return drawable.getSize();
    }

    public void recycle() {
        drawable.stop();
        drawable.recycle();
    }

    public void initialize() {
        drawable.getFirstFrame().prepareToDraw();
    }
}
