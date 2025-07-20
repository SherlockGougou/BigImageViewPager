package com.bumptech.glide.integration.webp.decoder;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Decodes Webp {@link Bitmap Bitmaps} from {@link InputStream InputStreams}.
 * For Animated Webp Images
 *
 * @author liuchun
 */
public class StreamAnimatedBitmapDecoder implements ResourceDecoder<InputStream, Bitmap> {
    private final AnimatedWebpBitmapDecoder bitmapDecoder;

    public StreamAnimatedBitmapDecoder(AnimatedWebpBitmapDecoder bitmapDecoder) {
        this.bitmapDecoder = bitmapDecoder;
    }

    @Override
    public boolean handles(@NonNull InputStream source, @NonNull Options options) throws IOException {
        return bitmapDecoder.handles(source, options);
    }

    @Nullable
    @Override
    public Resource<Bitmap> decode(@NonNull InputStream source, int width, int height, @NonNull Options options) throws IOException {
        return bitmapDecoder.decode(source, width, height, options);
    }
}
