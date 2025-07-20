package com.bumptech.glide.integration.webp.decoder;

import android.content.Context;
import android.graphics.Bitmap;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.util.Preconditions;

import java.security.MessageDigest;

/**
 * An {@link Transformation} that wraps a transformation for a
 * {@link Bitmap} and can apply it to every frame of any
 * {@link WebpDrawable}.
 *
 * @author liuchun
 */
public class WebpDrawableTransformation implements Transformation<WebpDrawable> {
    private final Transformation<Bitmap> wrapped;

    public WebpDrawableTransformation(Transformation<Bitmap> wrapped) {
        this.wrapped = Preconditions.checkNotNull(wrapped);
    }

    @Override
    public Resource<WebpDrawable> transform(Context context, Resource<WebpDrawable> resource, int outWidth, int outHeight) {
        WebpDrawable drawable = resource.get();

        // The drawable needs to be initialized with the correct width and height in order for a view
        // displaying it to end up with the right dimensions. Since our transformations may arbitrarily
        // modify the dimensions of our GIF, here we create a stand in for a frame and pass it to the
        // transformation to see what the final transformed dimensions will be so that our drawable can
        // report the correct intrinsic width and height.
        BitmapPool bitmapPool = Glide.get(context).getBitmapPool();
        Bitmap firstFrame = drawable.getFirstFrame();
        Resource<Bitmap> bitmapResource = new BitmapResource(firstFrame, bitmapPool);
        Resource<Bitmap> transformed = wrapped.transform(context, bitmapResource, outWidth, outHeight);
        if (!bitmapResource.equals(transformed)) {
            bitmapResource.recycle();
        }
        Bitmap transformedFrame = transformed.get();

        drawable.setFrameTransformation(wrapped, transformedFrame);
        return resource;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof WebpDrawableTransformation) {
            WebpDrawableTransformation other = (WebpDrawableTransformation) o;
            return wrapped.equals(other.wrapped);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return wrapped.hashCode();
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) {
        wrapped.updateDiskCacheKey(messageDigest);
    }
}
