package com.bumptech.glide.integration.webp.decoder;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Decodes webp {@link Bitmap Bitmaps} from {@link ByteBuffer ByteBuffers}.
 * For Animated Webp Images
 *
 * @author liuchun
 */
public class ByteBufferAnimatedBitmapDecoder implements ResourceDecoder<ByteBuffer, Bitmap> {
    private final AnimatedWebpBitmapDecoder bitmapDecoder;

    public ByteBufferAnimatedBitmapDecoder(AnimatedWebpBitmapDecoder bitmapDecoder) {
        this.bitmapDecoder = bitmapDecoder;
    }

    @Override
    public boolean handles(@NonNull ByteBuffer source, @NonNull Options options) throws IOException {
        return bitmapDecoder.handles(source, options);
    }

    @Nullable
    @Override
    public Resource<Bitmap> decode(@NonNull ByteBuffer source, int width, int height, @NonNull Options options) throws IOException {
        return bitmapDecoder.decode(source, width, height, options);
    }
}
