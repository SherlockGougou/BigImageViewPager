package com.bumptech.glide.integration.webp;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.integration.webp.decoder.AnimatedWebpBitmapDecoder;
import com.bumptech.glide.integration.webp.decoder.ByteBufferAnimatedBitmapDecoder;
import com.bumptech.glide.integration.webp.decoder.ByteBufferBitmapWebpDecoder;
import com.bumptech.glide.integration.webp.decoder.ByteBufferWebpDecoder;
import com.bumptech.glide.integration.webp.decoder.StreamAnimatedBitmapDecoder;
import com.bumptech.glide.integration.webp.decoder.StreamBitmapWebpDecoder;
import com.bumptech.glide.integration.webp.decoder.StreamWebpDecoder;
import com.bumptech.glide.integration.webp.decoder.WebpDownsampler;
import com.bumptech.glide.integration.webp.decoder.WebpDrawable;
import com.bumptech.glide.integration.webp.decoder.WebpDrawableEncoder;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapDrawableDecoder;

import java.io.InputStream;
import java.nio.ByteBuffer;

@Deprecated
public class WebpGlideModule implements com.bumptech.glide.module.GlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {

    }

    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {

        // We should put our decoder before the build-in decoders,
        // because the Downsampler will consume arbitrary data and make the inputstream corrupt
        // on some devices
        final Resources resources = context.getResources();
        final BitmapPool bitmapPool = glide.getBitmapPool();
        final ArrayPool arrayPool = glide.getArrayPool();
        /* static webp decoders */
        WebpDownsampler webpDownsampler = new WebpDownsampler(registry.getImageHeaderParsers(),
                resources.getDisplayMetrics(), bitmapPool, arrayPool);
        AnimatedWebpBitmapDecoder bitmapDecoder = new AnimatedWebpBitmapDecoder(arrayPool, bitmapPool);
        ByteBufferBitmapWebpDecoder byteBufferBitmapDecoder = new ByteBufferBitmapWebpDecoder(webpDownsampler);
        StreamBitmapWebpDecoder streamBitmapDecoder = new StreamBitmapWebpDecoder(webpDownsampler, arrayPool);
        /* animate webp decoders */
        ByteBufferWebpDecoder byteBufferWebpDecoder =
                new ByteBufferWebpDecoder(context, arrayPool, bitmapPool);
        registry
                /* Bitmaps for static webp images */
                .prepend(Registry.BUCKET_BITMAP, ByteBuffer.class, Bitmap.class, byteBufferBitmapDecoder)
                .prepend(Registry.BUCKET_BITMAP, InputStream.class, Bitmap.class, streamBitmapDecoder)
                /* BitmapDrawables for static webp images */
                .prepend(
                        Registry.BUCKET_BITMAP_DRAWABLE,
                        ByteBuffer.class,
                        BitmapDrawable.class,
                        new BitmapDrawableDecoder<>(resources, byteBufferBitmapDecoder))
                .prepend(
                        Registry.BUCKET_BITMAP_DRAWABLE,
                        InputStream.class,
                        BitmapDrawable.class,
                        new BitmapDrawableDecoder<>(resources, streamBitmapDecoder))
                /* Bitmaps for animated webp images*/
                .prepend(Registry.BUCKET_BITMAP, ByteBuffer.class, Bitmap.class, new ByteBufferAnimatedBitmapDecoder(bitmapDecoder))
                .prepend(Registry.BUCKET_BITMAP, InputStream.class, Bitmap.class, new StreamAnimatedBitmapDecoder(bitmapDecoder))
                /* Animated webp images */
                .prepend(ByteBuffer.class, WebpDrawable.class, byteBufferWebpDecoder)
                .prepend(InputStream.class, WebpDrawable.class, new StreamWebpDecoder(byteBufferWebpDecoder, arrayPool))
                .prepend(WebpDrawable.class, new WebpDrawableEncoder());
    }
}
