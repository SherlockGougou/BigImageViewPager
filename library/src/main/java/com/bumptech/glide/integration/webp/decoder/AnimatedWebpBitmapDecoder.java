package com.bumptech.glide.integration.webp.decoder;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.bumptech.glide.integration.webp.WebpHeaderParser;
import com.bumptech.glide.integration.webp.WebpImage;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.load.resource.gif.GifBitmapProvider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Decode the animated webp image and obtain the first frame bitmap
 *
 * @author liuchun
 */
public class AnimatedWebpBitmapDecoder {
    public static final Option<Boolean> DISABLE_BITMAP = Option.memory(
            "com.bumptech.glide.integration.webp.decoder.AnimatedWebpBitmapDecoder.DisableBitmap", false);

    private final ArrayPool mArrayPool;
    private final BitmapPool mBitmapPool;
    private final GifBitmapProvider mProvider;

    public AnimatedWebpBitmapDecoder(ArrayPool byteArrayPool, BitmapPool bitmapPool) {
        mArrayPool = byteArrayPool;
        mBitmapPool = bitmapPool;
        mProvider = new GifBitmapProvider(bitmapPool, byteArrayPool);
    }

    public boolean handles(InputStream source, @NonNull Options options) throws IOException {
        if (options.get(DISABLE_BITMAP)) {
            return false;
        }
        WebpHeaderParser.WebpImageType webpType = WebpHeaderParser.getType(source, mArrayPool);
        return WebpHeaderParser.isAnimatedWebpType(webpType);
    }

    public boolean handles(ByteBuffer source, @NonNull Options options) throws IOException {
        if (options.get(DISABLE_BITMAP)) {
            return false;
        }
        WebpHeaderParser.WebpImageType webpType = WebpHeaderParser.getType(source);
        return WebpHeaderParser.isAnimatedWebpType(webpType);
    }

    public Resource<Bitmap> decode(InputStream source, int width, int height,
                                   Options options) throws IOException {
        byte[] data = Utils.inputStreamToBytes(source);
        if (data == null) {
            return null;
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        return decode(byteBuffer, width, height, options);
    }

    public Resource<Bitmap> decode(ByteBuffer source, int width, int height,
                                   Options options) throws IOException {
        int length = source.remaining();
        byte[] data = new byte[length];
        source.get(data, 0, length);

        WebpImage webp = WebpImage.create(data);

        int sampleSize = Utils.getSampleSize(webp.getWidth(), webp.getHeight(), width, height);
        WebpDecoder webpDecoder = new WebpDecoder(mProvider, webp, source, sampleSize);
        try {
            webpDecoder.advance();
            Bitmap firstFrame = webpDecoder.getNextFrame();
            return BitmapResource.obtain(firstFrame, mBitmapPool);
        } finally {
            // release the resources
            webpDecoder.clear();
        }
    }
}
