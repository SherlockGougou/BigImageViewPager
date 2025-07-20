package com.bumptech.glide.integration.webp.decoder;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.util.ByteBufferUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Decodes webp {@link Bitmap Bitmaps} from {@link ByteBuffer ByteBuffers}.
 * For static lossless and transparent webp
 *
 * @author liuchun
 */
public class ByteBufferBitmapWebpDecoder implements ResourceDecoder<ByteBuffer, Bitmap> {
    private final WebpDownsampler downsampler;

    public ByteBufferBitmapWebpDecoder(WebpDownsampler downsampler) {
        this.downsampler = downsampler;
    }

    @Override
    public boolean handles(@NonNull ByteBuffer source, @NonNull Options options) throws IOException {
        return downsampler.handles(source, options);
    }

    @Override
    public Resource<Bitmap> decode(@NonNull ByteBuffer source, int width, int height, @NonNull Options options) throws IOException {
        InputStream is = ByteBufferUtil.toStream(source);
        return downsampler.decode(is, width, height, options);
    }
}
