package com.bumptech.glide.integration.webp.decoder;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;

import java.io.IOException;
import java.io.InputStream;

/**
 * Decodes Webp {@link Bitmap Bitmaps} from {@link InputStream InputStreams}.
 * For static lossless and transparent webp
 *
 * @author liuchun
 */
public class StreamBitmapWebpDecoder implements ResourceDecoder<InputStream, Bitmap> {
    private final WebpDownsampler downsampler;
    private final ArrayPool byteArrayPool;

    public StreamBitmapWebpDecoder(WebpDownsampler downsampler, ArrayPool byteArrayPool) {
        this.downsampler = downsampler;
        this.byteArrayPool = byteArrayPool;
    }

    @Override
    public boolean handles(@NonNull InputStream source, @NonNull Options options) throws IOException {
        return downsampler.handles(source, options);
    }

    @Override
    public Resource<Bitmap> decode(@NonNull InputStream source, int width, int height, @NonNull Options options) throws IOException {
        return downsampler.decode(source, width, height, options);
    }
}
