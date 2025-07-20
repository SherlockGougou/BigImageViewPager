package com.bumptech.glide.integration.webp;


import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.Keep;

import java.io.BufferedInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The Same API with android framework {@link BitmapFactory} that can decode
 * static webp images to bitmap
 *
 * @author liuchun
 */
@Keep
public class WebpBitmapFactory {
    private static final int IN_TEMP_BUFFER_SIZE = 8 * 1024;

    private static final int MAX_WEBP_HEADER_SIZE = 21;

    public static boolean sUseSystemDecoder = true;

    static {
        System.loadLibrary("glide-webp");
    }

    private static void setDensityFromOptions(Bitmap outputBitmap, BitmapFactory.Options opts) {
        if (outputBitmap == null || opts == null) {
            return;
        }

        final int density = opts.inDensity;
        if (density != 0) {
            outputBitmap.setDensity(density);
            final int targetDensity = opts.inTargetDensity;
            if (targetDensity == 0 || density == targetDensity || density == opts.inScreenDensity) {
                return;
            }

            if (opts.inScaled) {
                outputBitmap.setDensity(targetDensity);
            }
        } else if (opts.inBitmap != null) {
            // bitmap was reused, ensure density is reset
            outputBitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        }
    }

    /**
     * 设置webp的BitmapOptions
     * @param bitmap
     * @param opts
     */
    private static void setWebpBitmapOptions(Bitmap bitmap, BitmapFactory.Options opts) {
        setDensityFromOptions(bitmap, opts);
        if (opts != null) {
            opts.outMimeType = "image/webp";
        }
    }

    /**
     * 设置BitmapOptions的宽高
     * @param options
     * @param imageWidth
     * @param imageHeight
     * @return
     */
    // Called from JNI
    @Keep
    private static boolean setOutDimensions(
            BitmapFactory.Options options,
            int imageWidth,
            int imageHeight) {
        if (options != null) {
            options.outWidth = imageWidth;
            options.outHeight = imageHeight;
            return options.inJustDecodeBounds;
        }
        return false;
    }

    /**
     * 创建Bitmap对象
     * @param width
     * @param height
     * @param options
     * @return
     */
    // Called from JNI
    @Keep
    private static Bitmap createBitmap(int width, int height, BitmapFactory.Options options) {
        if (options != null &&
                options.inBitmap != null &&
                options.inBitmap.isMutable()) {
            Bitmap bm = options.inBitmap;
            if (bm.getWidth() == width &&
                    bm.getHeight() == height &&
                    bm.getConfig() == options.inPreferredConfig) {
                bm.setHasAlpha(true);
                bm.eraseColor(Color.TRANSPARENT);
                return bm;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setHasAlpha(true);
        bitmap.eraseColor(Color.TRANSPARENT);
        return bitmap;
    }


    /**
     * 设置默认的padding
     * @param padding
     */
    private static void setDefaultPadding(Rect padding) {
        if (padding != null) {
            padding.top = -1;
            padding.left = -1;
            padding.bottom = -1;
            padding.right = -1;
        }
    }

    /**
     * 是否需要使用libwebp进行webp解码
     * 1. sUseSystemDecoder=true, 4.2以下系统针对无损和透明webp图片
     * 2. sUseSystemDecoder=false, Android所有版本都使用libwebp解码
     * @param headers
     * @param offset
     * @param length
     * @return
     */
    public static boolean webpSupportRequired(byte[] headers, int offset, int length) {

        WebpHeaderParser.WebpImageType imageType;
        try {
            imageType = WebpHeaderParser.getType(headers, offset, length);
        } catch (IOException e) {
            imageType = WebpHeaderParser.WebpImageType.NONE_WEBP;
        }

        if (sUseSystemDecoder) {
            return Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1 &&
                    WebpHeaderParser.isNonSimpleWebpType(imageType);
        } else {
            return WebpHeaderParser.isStaticWebpType(imageType);
        }
    }


    /**
     * 读取图片Header字节
     * @param is
     * @return
     */
    private static byte[] getImageHeader(InputStream is) {

        if (!is.markSupported()) {
            is = new BufferedInputStream(is, MAX_WEBP_HEADER_SIZE);
        }

        is.mark(MAX_WEBP_HEADER_SIZE);
        byte[] header = new byte[MAX_WEBP_HEADER_SIZE];
        try {
            is.read(header, 0, MAX_WEBP_HEADER_SIZE);
            is.reset();
        } catch (IOException exp) {
            return null;
        }
        return header;
    }


    /**
     * 包装流
     * @param inputStream
     * @return
     */
    private static InputStream wrapToMarkSupportedStream(InputStream inputStream) {
        if (!inputStream.markSupported()) {
            inputStream = new BufferedInputStream(inputStream, IN_TEMP_BUFFER_SIZE);
        }
        return inputStream;
    }


    /**
     * Decode an immutable bitmap from the specified byte array.
     *
     * @param data byte array of compressed image data
     * @param offset offset into imageData for where the decoder should begin
     *               parsing.
     * @param length the number of bytes, beginning at offset, to parse
     * @return The decoded bitmap, or null if the image could not be decoded.
     */
    public static Bitmap decodeByteArray(byte[] data, int offset, int length) {
        return decodeByteArray(data, offset, length, null);
    }


    /**
     * Decode an immutable bitmap from the specified byte array.
     *
     * @param data byte array of compressed image data
     * @param offset offset into imageData for where the decoder should begin
     *               parsing.
     * @param length the number of bytes, beginning at offset, to parse
     * @param opts null-ok; Options that control downsampling and whether the
     *             image should be completely decoded, or just is size returned.
     * @return The decoded bitmap, or null if the image data could not be
     *         decoded, or, if opts is non-null, if opts requested only the
     *         size be returned (in opts.outWidth and opts.outHeight)
     * @throws IllegalArgumentException if {@link BitmapFactory.Options#inPreferredConfig}
     *         is {@link Bitmap.Config#HARDWARE}
     *         and {@link BitmapFactory.Options#inMutable} is set, if the specified color space
     *         is not {@link ColorSpace.Model#RGB RGB}, or if the specified color space's transfer
     *         function is not an {@link ColorSpace.Rgb.TransferParameters ICC parametric curve}
     */
    public static Bitmap decodeByteArray(byte[] data, int offset, int length, BitmapFactory.Options opts) {
        if ((offset | length) < 0 || data.length < offset + length) {
            throw new ArrayIndexOutOfBoundsException();
        }

        Bitmap bm;
        if (webpSupportRequired(data, offset, length)) {
            bm = nativeDecodeByteArray(data, offset, length, opts,
                    getScaleFromOptions(opts), getInTempStorageFromOptions(opts));

            setWebpBitmapOptions(bm, opts);
        } else {
            // system decode method used
            bm = BitmapFactory.decodeByteArray(data, offset, length, opts);
        }

        return bm;
    }


    /**
     * Decode a file path into a bitmap. If the specified file name is null,
     * or cannot be decoded into a bitmap, the function returns null.
     *
     * @param pathName complete path name for the file to be decoded.
     * @return the resulting decoded bitmap, or null if it could not be decoded.
     */
    public static Bitmap decodeFile(String pathName) {
        return decodeFile(pathName, null);
    }

    /**
     * Decode a file path into a bitmap. If the specified file name is null,
     * or cannot be decoded into a bitmap, the function returns null.
     *
     * @param pathName complete path name for the file to be decoded.
     * @param opts null-ok; Options that control downsampling and whether the
     *             image should be completely decoded, or just is size returned.
     * @return The decoded bitmap, or null if the image data could not be
     *         decoded, or, if opts is non-null, if opts requested only the
     *         size be returned (in opts.outWidth and opts.outHeight)
     * @throws IllegalArgumentException if {@link BitmapFactory.Options#inPreferredConfig}
     *         is {@link Bitmap.Config#HARDWARE}
     *         and {@link BitmapFactory.Options#inMutable} is set, if the specified color space
     *         is not {@link ColorSpace.Model#RGB RGB}, or if the specified color space's transfer
     *         function is not an {@link ColorSpace.Rgb.TransferParameters ICC parametric curve}
     */
    public static Bitmap decodeFile(String pathName, BitmapFactory.Options opts) {

        Bitmap bm = null;
        InputStream stream = null;
        try {
            stream = new FileInputStream(pathName);
            bm = decodeStream(stream, null, opts);
        } catch (Exception e) {
            /*  do nothing.
                If the exception happened on open, bm will be null.
            */
            Log.e("WebpBitmapFactory", "Unable to decode stream: " + e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // do nothing here
                }
            }
        }
        return bm;
    }


    /**
     * Synonym for {@link #decodeResource(Resources, int, BitmapFactory.Options)}
     * with null Options.
     *
     * @param res The resources object containing the image data
     * @param id The resource id of the image data
     * @return The decoded bitmap, or null if the image could not be decoded.
     */
    public static Bitmap decodeResource(Resources res, int id) {
        return decodeResource(res, id, null);
    }


    /**
     * Synonym for opening the given resource and calling
     * {@link #decodeResourceStream}.
     *
     * @param res   The resources object containing the image data
     * @param id The resource id of the image data
     * @param opts null-ok; Options that control downsampling and whether the
     *             image should be completely decoded, or just is size returned.
     * @return The decoded bitmap, or null if the image data could not be
     *         decoded, or, if opts is non-null, if opts requested only the
     *         size be returned (in opts.outWidth and opts.outHeight)
     * @throws IllegalArgumentException if {@link BitmapFactory.Options#inPreferredConfig}
     *         is {@link Bitmap.Config#HARDWARE}
     *         and {@link BitmapFactory.Options#inMutable} is set, if the specified color space
     *         is not {@link ColorSpace.Model#RGB RGB}, or if the specified color space's transfer
     *         function is not an {@link ColorSpace.Rgb.TransferParameters ICC parametric curve}
     */
    public static Bitmap decodeResource(Resources res, int id, BitmapFactory.Options opts) {

        Bitmap bm = null;
        InputStream is = null;
        try {
            final TypedValue value = new TypedValue();
            is = res.openRawResource(id, value);

            bm = decodeResourceStream(res, value, is, null, opts);
        } catch (Exception e) {
            /*  do nothing.
                If the exception happened on open, bm will be null.
                If it happened on close, bm is still valid.
            */
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                // Ignore
            }
        }

        if (bm == null && opts != null && opts.inBitmap != null) {
            throw new IllegalArgumentException("Problem decoding into existing bitmap");
        }

        return bm;
    }


    /**
     * Decode a new Bitmap from an InputStream. This InputStream was obtained from
     * resources, which we pass to be able to scale the bitmap accordingly.
     * @throws IllegalArgumentException if {@link BitmapFactory.Options#inPreferredConfig}
     *         is {@link Bitmap.Config#HARDWARE}
     *         and {@link BitmapFactory.Options#inMutable} is set, if the specified color space
     *         is not {@link ColorSpace.Model#RGB RGB}, or if the specified color space's transfer
     *         function is not an {@link ColorSpace.Rgb.TransferParameters ICC parametric curve}
     */
    public static Bitmap decodeResourceStream(Resources res, TypedValue value,
                                              InputStream is, Rect pad, BitmapFactory.Options opts) {
        if (opts == null) {
            opts = new BitmapFactory.Options();
        }

        if (opts.inDensity == 0 && value != null) {
            final int density = value.density;
            if (density == TypedValue.DENSITY_DEFAULT) {
                opts.inDensity = DisplayMetrics.DENSITY_DEFAULT;
            } else if (density != TypedValue.DENSITY_NONE) {
                opts.inDensity = density;
            }
        }

        if (opts.inTargetDensity == 0 && res != null) {
            opts.inTargetDensity = res.getDisplayMetrics().densityDpi;
        }

        return decodeStream(is, pad, opts);
    }



    /**
     * Decode a bitmap from the file descriptor. If the bitmap cannot be decoded
     * return null. The position within the descriptor will not be changed when
     * this returns, so the descriptor can be used again as is.
     *
     * @param fd The file descriptor containing the bitmap data to decode
     * @return the decoded bitmap, or null
     */
    public static Bitmap decodeFileDescriptor(FileDescriptor fd) {
        return decodeFileDescriptor(fd, null, null);
    }


    /**
     * Decode a bitmap from the file descriptor. If the bitmap cannot be decoded
     * return null. The position within the descriptor will not be changed when
     * this returns, so the descriptor can be used again as-is.
     *
     * @param fd The file descriptor containing the bitmap data to decode
     * @param outPadding If not null, return the padding rect for the bitmap if
     *                   it exists, otherwise set padding to [-1,-1,-1,-1]. If
     *                   no bitmap is returned (null) then padding is
     *                   unchanged.
     * @param opts null-ok; Options that control downsampling and whether the
     *             image should be completely decoded, or just its size returned.
     * @return the decoded bitmap, or null
     * @throws IllegalArgumentException if {@link BitmapFactory.Options#inPreferredConfig}
     *         is {@link Bitmap.Config#HARDWARE}
     *         and {@link BitmapFactory.Options#inMutable} is set, if the specified color space
     *         is not {@link ColorSpace.Model#RGB RGB}, or if the specified color space's transfer
     *         function is not an {@link ColorSpace.Rgb.TransferParameters ICC parametric curve}
     */
    public static Bitmap decodeFileDescriptor(FileDescriptor fd, Rect outPadding, BitmapFactory.Options opts) {

        Bitmap bm;
        InputStream is = wrapToMarkSupportedStream(new FileInputStream(fd));
        try {
            byte[] header = getImageHeader(is);
            if (webpSupportRequired(header, 0, MAX_WEBP_HEADER_SIZE)) {
                bm = nativeDecodeStream(is, opts, getScaleFromOptions(opts), getInTempStorageFromOptions(opts));

                setWebpBitmapOptions(bm, opts);
                setDefaultPadding(outPadding);
            } else {
                bm = BitmapFactory.decodeFileDescriptor(fd, outPadding, opts);
            }
        } finally {
            try {
                is.close();
            } catch (Throwable t) {
                /* ignore */
            }
        }

        return bm;
    }


    /**
     * Decode an input stream into a bitmap. If the input stream is null, or
     * cannot be used to decode a bitmap, the function returns null.
     * The stream's position will be where ever it was after the encoded data
     * was read.
     *
     * @param is The input stream that holds the raw data to be decoded into a
     *           bitmap.
     * @return The decoded bitmap, or null if the image data could not be decoded.
     */
    public static Bitmap decodeStream(InputStream is) {
        return decodeStream(is, null, null);
    }

    /**
     * Decode an input stream into a bitmap. If the input stream is null, or
     * cannot be used to decode a bitmap, the function returns null.
     * The stream's position will be where ever it was after the encoded data
     * was read.
     *
     * @param is The input stream that holds the raw data to be decoded into a
     *           bitmap.
     * @param outPadding If not null, return the padding rect for the bitmap if
     *                   it exists, otherwise set padding to [-1,-1,-1,-1]. If
     *                   no bitmap is returned (null) then padding is
     *                   unchanged.
     * @param opts null-ok; Options that control downsampling and whether the
     *             image should be completely decoded, or just is size returned.
     * @return The decoded bitmap, or null if the image data could not be
     *         decoded, or, if opts is non-null, if opts requested only the
     *         size be returned (in opts.outWidth and opts.outHeight)
     * @throws IllegalArgumentException if {@link BitmapFactory.Options#inPreferredConfig}
     *         is {@link Bitmap.Config#HARDWARE}
     *         and {@link BitmapFactory.Options#inMutable} is set, if the specified color space
     *         is not {@link ColorSpace.Model#RGB RGB}, or if the specified color space's transfer
     *         function is not an {@link ColorSpace.Rgb.TransferParameters ICC parametric curve}
     *
     * <p class="note">Prior to {@link Build.VERSION_CODES#KITKAT},
     * if {@link InputStream#markSupported is.markSupported()} returns true,
     * <code>is.mark(1024)</code> would be called. As of
     * {@link Build.VERSION_CODES#KITKAT}, this is no longer the case.</p>
     */
    public static Bitmap decodeStream(InputStream is, Rect outPadding, BitmapFactory.Options opts) {
        // we don't throw in this case, thus allowing the caller to only check
        // the cache, and not force the image to be decoded.
        if (is == null) {
            return null;
        }

        is = wrapToMarkSupportedStream(is);
        Bitmap bm;
        byte[] header = getImageHeader(is);
        if (webpSupportRequired(header, 0, MAX_WEBP_HEADER_SIZE)) {
            bm = nativeDecodeStream(
                    is,
                    opts,
                    getScaleFromOptions(opts),
                    getInTempStorageFromOptions(opts));
            setWebpBitmapOptions(bm, opts);
            setDefaultPadding(outPadding);
        } else {
            // system decode method used
            bm = BitmapFactory.decodeStream(is, outPadding, opts);
        }

        return bm;
    }


    private static byte[] getInTempStorageFromOptions(final BitmapFactory.Options options) {
        if (options != null && options.inTempStorage != null) {
            return options.inTempStorage;
        } else {
            return new byte[IN_TEMP_BUFFER_SIZE];
        }
    }


    private static float getScaleFromOptions(BitmapFactory.Options options) {
        float scale = 1.0f;
        if (options != null) {
            int sampleSize = options.inSampleSize;
            if (sampleSize > 1) {
                scale = 1.0f / (float) sampleSize;
            }
            if (options.inScaled) {
                int density = options.inDensity;
                int targetDensity = options.inTargetDensity;
                int screenDensity = options.inScreenDensity;
                if (density != 0 && targetDensity != 0 && density != screenDensity) {
                    float factor = targetDensity / (float) density;
                    scale = scale * factor;
                }
            }
        }
        return scale;
    }


    private static native Bitmap nativeDecodeStream(InputStream is, BitmapFactory.Options opts,
                                                    float scale, byte[] inTempStorage);
    private static native Bitmap nativeDecodeByteArray(byte[] data, int offset,
                                                       int length, BitmapFactory.Options opts,
                                                       float scale, byte[] inTempStorage);
}
