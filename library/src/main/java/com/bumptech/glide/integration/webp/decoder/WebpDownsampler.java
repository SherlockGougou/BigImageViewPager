package com.bumptech.glide.integration.webp.decoder;

import static com.bumptech.glide.load.resource.bitmap.Downsampler.ALLOW_HARDWARE_CONFIG;
import static com.bumptech.glide.load.resource.bitmap.Downsampler.DECODE_FORMAT;
import static com.bumptech.glide.load.resource.bitmap.Downsampler.DOWNSAMPLE_STRATEGY;
import static com.bumptech.glide.load.resource.bitmap.Downsampler.FIX_BITMAP_SIZE_TO_REQUESTED_DIMENSIONS;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.Nullable;

import com.bumptech.glide.integration.webp.WebpBitmapFactory;
import com.bumptech.glide.integration.webp.WebpHeaderParser;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParserUtils;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.load.resource.bitmap.TransformationUtils;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Queue;

/**
 * code copy from {@link Downsampler} due to it is final that cannot be inherited
 *
 * @author liuchun
 */
public final class WebpDownsampler {
    private static final String TAG = "WebpDownsampler";

    public static final Option<Boolean> DISABLE_DECODER = Option.memory(
            "com.bumptech.glide.integration.webp.decoder.WebpDownsampler.DisableDecoder", false);

    public static final Option<Boolean> USE_SYSTEM_DECODER = Option.memory(
            "com.bumptech.glide.integration.webp.decoder.WebpDownsampler.SystemDecoder", true);

    private static final Downsampler.DecodeCallbacks EMPTY_CALLBACKS = new Downsampler.DecodeCallbacks() {
        @Override
        public void onObtainBounds() {
            // Do nothing.
        }

        @Override
        public void onDecodeComplete(BitmapPool bitmapPool, Bitmap downsampled) throws IOException {
            // Do nothing.
        }
    };

    private static final Queue<BitmapFactory.Options> OPTIONS_QUEUE = Util.createQueue(0);
    // 10MB. This is the max image header size we can handle, we preallocate a much smaller buffer
    // but will resize up to this amount if necessary.
    private static final int MARK_POSITION = 10 * 1024 * 1024;
    // Defines the level of precision we get when using inDensity/inTargetDensity to calculate an
    // arbitrary float scale factor.
    private static final int DENSITY_PRECISION_MULTIPLIER = 1000000000;

    private final BitmapPool bitmapPool;
    private final DisplayMetrics displayMetrics;
    private final ArrayPool byteArrayPool;
    private final List<ImageHeaderParser> parsers;

    //private final HardwareConfigState hardwareConfigState = HardwareConfigState.getInstance();

    public WebpDownsampler(List<ImageHeaderParser> parsers, DisplayMetrics displayMetrics,
                       BitmapPool bitmapPool, ArrayPool byteArrayPool) {
        this.parsers = parsers;
        this.displayMetrics = Preconditions.checkNotNull(displayMetrics);
        this.bitmapPool = Preconditions.checkNotNull(bitmapPool);
        this.byteArrayPool = Preconditions.checkNotNull(byteArrayPool);
    }

    public boolean handles(InputStream is, Options options) throws IOException{
        if (options.get(DISABLE_DECODER)) {
            // disable by user
            return false;
        }
        // user can disable webp decoder from android framework by options for CVE-2023-4863 (https://github.com/advisories/GHSA-j7hp-h8jx-5ppr),
        if (options.get(USE_SYSTEM_DECODER)) {
            if (WebpHeaderParser.sIsExtendedWebpSupported) {
                // Android framework support webp decoder, just to next decoder
                return false;
            }
            WebpHeaderParser.WebpImageType webpType = WebpHeaderParser.getType(is, byteArrayPool);
            // handle lossless and transparent webp below Android 4.2
            return WebpHeaderParser.isStaticWebpType(webpType)
                    && webpType != WebpHeaderParser.WebpImageType.WEBP_SIMPLE;
        }
        // force use libwebp in this library for all Android versions
        WebpHeaderParser.WebpImageType webpType = WebpHeaderParser.getType(is, byteArrayPool);
        return WebpHeaderParser.isStaticWebpType(webpType);
    }

    public boolean handles(ByteBuffer byteBuffer, Options options) throws IOException{
        if (options.get(DISABLE_DECODER)) {
            // disable by user
            return false;
        }
        // user can disable webp decoder from android framework by options for CVE-2023-4863 (https://github.com/advisories/GHSA-j7hp-h8jx-5ppr),
        if (options.get(USE_SYSTEM_DECODER)) {
            if (WebpHeaderParser.sIsExtendedWebpSupported) {
                // Android framework support webp decoder, just to next decoder
                return false;
            }
            WebpHeaderParser.WebpImageType webpType = WebpHeaderParser.getType(byteBuffer);
            // handle lossless and transparent webp below Android 4.2
            return WebpHeaderParser.isStaticWebpType(webpType)
                    && webpType != WebpHeaderParser.WebpImageType.WEBP_SIMPLE;
        }
        // force use libwebp in this library for all Android versions
        WebpHeaderParser.WebpImageType webpType = WebpHeaderParser.getType(byteBuffer);
        return WebpHeaderParser.isStaticWebpType(webpType);
    }


    /**
     * Returns a Bitmap decoded from the given {@link InputStream} that is rotated to match any EXIF
     * data present in the stream and that is downsampled according to the given dimensions and any
     * provided  {@link DownsampleStrategy} option.
     *
     * @see #decode(InputStream, int, int, Options, Downsampler.DecodeCallbacks)
     */
    public Resource<Bitmap> decode(InputStream is, int outWidth, int outHeight,
                                   Options options) throws IOException {
        return decode(is, outWidth, outHeight, options, EMPTY_CALLBACKS);
    }

    /**
     * Returns a Bitmap decoded from the given {@link InputStream} that is rotated to match any EXIF
     * data present in the stream and that is downsampled according to the given dimensions and any
     * provided  {@link DownsampleStrategy} option.
     *
     * <p> If a Bitmap is present in the
     * {@link BitmapPool} whose dimensions exactly match
     * those of the image for the given InputStream is available, the operation is much less expensive
     * in terms of memory. </p>
     *
     * <p> The provided {@link InputStream} must return <code>true</code> from
     * {@link InputStream#markSupported()} and is expected to support a reasonably large
     * mark limit to accommodate reading large image headers (~5MB). </p>
     *
     * @param is        An {@link InputStream} to the data for the image.
     * @param requestedWidth  The width the final image should be close to.
     * @param requestedHeight The height the final image should be close to.
     * @param options   A set of options that may contain one or more supported options that influence
     *                  how a Bitmap will be decoded from the given stream.
     * @param callbacks A set of callbacks allowing callers to optionally respond to various
     *                  significant events during the decode process.
     * @return A new bitmap containing the image from the given InputStream, or recycle if recycle is
     * not null.
     */
    @SuppressWarnings({"resource", "deprecation"})
    public Resource<Bitmap> decode(InputStream is, int requestedWidth, int requestedHeight,
                                   Options options, Downsampler.DecodeCallbacks callbacks) throws IOException {
        Preconditions.checkArgument(is.markSupported(), "You must provide an InputStream that supports"
                + " mark()");

        byte[] bytesForOptions = byteArrayPool.get(ArrayPool.STANDARD_BUFFER_SIZE_BYTES, byte[].class);
        BitmapFactory.Options bitmapFactoryOptions = getDefaultOptions();
        bitmapFactoryOptions.inTempStorage = bytesForOptions;

        DecodeFormat decodeFormat = options.get(DECODE_FORMAT);
        DownsampleStrategy downsampleStrategy = options.get(DOWNSAMPLE_STRATEGY);
        boolean fixBitmapToRequestedDimensions = options.get(FIX_BITMAP_SIZE_TO_REQUESTED_DIMENSIONS);
        boolean isHardwareConfigAllowed =
                options.get(ALLOW_HARDWARE_CONFIG) != null && options.get(ALLOW_HARDWARE_CONFIG);

        try {
            Bitmap result = decodeFromWrappedStreams(is, bitmapFactoryOptions,
                    downsampleStrategy, decodeFormat, isHardwareConfigAllowed, requestedWidth,
                    requestedHeight, fixBitmapToRequestedDimensions, callbacks);
            return BitmapResource.obtain(result, bitmapPool);
        } finally {
            releaseOptions(bitmapFactoryOptions);
            byteArrayPool.put(bytesForOptions, byte[].class);
        }
    }

    private Bitmap decodeFromWrappedStreams(InputStream is,
                                            BitmapFactory.Options options, DownsampleStrategy downsampleStrategy,
                                            DecodeFormat decodeFormat, boolean isHardwareConfigAllowed, int requestedWidth,
                                            int requestedHeight, boolean fixBitmapToRequestedDimensions,
                                            Downsampler.DecodeCallbacks callbacks) throws IOException {
        long startTime = LogTime.getLogTime();

        int[] sourceDimensions = getDimensions(is, options, callbacks, bitmapPool);
        int sourceWidth = sourceDimensions[0];
        int sourceHeight = sourceDimensions[1];
        String sourceMimeType = options.outMimeType;

        // If we failed to obtain the image dimensions, we may end up with an incorrectly sized Bitmap,
        // so we want to use a mutable Bitmap type. One way this can happen is if the image header is so
        // large (10mb+) that our attempt to use inJustDecodeBounds fails and we're forced to decode the
        // full size image.
        if (sourceWidth == -1 || sourceHeight == -1) {
            isHardwareConfigAllowed = false;
        }

        int orientation = ImageHeaderParserUtils.getOrientation(parsers, is, byteArrayPool);
        int degreesToRotate = TransformationUtils.getExifOrientationDegrees(orientation);
        boolean isExifOrientationRequired = TransformationUtils.isExifOrientationRequired(orientation);

        int targetWidth = requestedWidth == Target.SIZE_ORIGINAL ? sourceWidth : requestedWidth;
        int targetHeight = requestedHeight == Target.SIZE_ORIGINAL ? sourceHeight : requestedHeight;

        ImageHeaderParser.ImageType imageType = ImageHeaderParserUtils.getType(parsers, is, byteArrayPool);

        calculateScaling(
                imageType,
                is,
                callbacks,
                bitmapPool,
                downsampleStrategy,
                degreesToRotate,
                sourceWidth,
                sourceHeight,
                targetWidth,
                targetHeight,
                options);
        calculateConfig(
                is,
                decodeFormat,
                isHardwareConfigAllowed,
                isExifOrientationRequired,
                options,
                targetWidth,
                targetHeight);

        boolean isKitKatOrGreater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // Prior to KitKat, the inBitmap size must exactly match the size of the bitmap we're decoding.
        if ((options.inSampleSize == 1 || isKitKatOrGreater) && shouldUsePool(imageType)) {
            int expectedWidth;
            int expectedHeight;
            if (fixBitmapToRequestedDimensions && isKitKatOrGreater) {
                expectedWidth = targetWidth;
                expectedHeight = targetHeight;
            } else {
                float densityMultiplier = isScaling(options)
                        ? (float) options.inTargetDensity / options.inDensity : 1f;
                int sampleSize = options.inSampleSize;
                int downsampledWidth = (int) Math.ceil(sourceWidth / (float) sampleSize);
                int downsampledHeight = (int) Math.ceil(sourceHeight / (float) sampleSize);
                expectedWidth = Math.round(downsampledWidth * densityMultiplier);
                expectedHeight = Math.round(downsampledHeight * densityMultiplier);

                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Calculated target [" + expectedWidth + "x" + expectedHeight + "] for source"
                            + " [" + sourceWidth + "x" + sourceHeight + "]"
                            + ", sampleSize: " + sampleSize
                            + ", targetDensity: " + options.inTargetDensity
                            + ", density: " + options.inDensity
                            + ", density multiplier: " + densityMultiplier);
                }
            }
            // If this isn't an image, or BitmapFactory was unable to parse the size, width and height
            // will be -1 here.
            if (expectedWidth > 0 && expectedHeight > 0) {
                setInBitmap(options, bitmapPool, expectedWidth, expectedHeight);
            }
        }
        Bitmap downsampled = decodeStream(is, options, callbacks, bitmapPool);
        callbacks.onDecodeComplete(bitmapPool, downsampled);

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            logDecode(sourceWidth, sourceHeight, sourceMimeType, options, downsampled,
                    requestedWidth, requestedHeight, startTime);
        }

        Bitmap rotated = null;
        if (downsampled != null) {
            // If we scaled, the Bitmap density will be our inTargetDensity. Here we correct it back to
            // the expected density dpi.
            downsampled.setDensity(displayMetrics.densityDpi);

            rotated = TransformationUtils.rotateImageExif(bitmapPool, downsampled, orientation);
            if (!downsampled.equals(rotated)) {
                bitmapPool.put(downsampled);
            }
        }

        return rotated;
    }

    // Visible for testing.
    static void calculateScaling(
            ImageHeaderParser.ImageType imageType,
            InputStream is,
            Downsampler.DecodeCallbacks decodeCallbacks,
            BitmapPool bitmapPool,
            DownsampleStrategy downsampleStrategy,
            int degreesToRotate,
            int sourceWidth,
            int sourceHeight,
            int targetWidth,
            int targetHeight,
            BitmapFactory.Options options) throws IOException {
        // We can't downsample source content if we can't determine its dimensions.
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return;
        }

        final float exactScaleFactor;
        if (degreesToRotate == 90 || degreesToRotate == 270) {
            // If we're rotating the image +-90 degrees, we need to downsample accordingly so the image
            // width is decreased to near our target's height and the image height is decreased to near
            // our target width.
            //noinspection SuspiciousNameCombination
            exactScaleFactor = downsampleStrategy.getScaleFactor(sourceHeight, sourceWidth,
                    targetWidth, targetHeight);
        } else {
            exactScaleFactor =
                    downsampleStrategy.getScaleFactor(sourceWidth, sourceHeight, targetWidth, targetHeight);
        }

        if (exactScaleFactor <= 0f) {
            throw new IllegalArgumentException("Cannot scale with factor: " + exactScaleFactor
                    + " from: " + downsampleStrategy
                    + ", source: [" + sourceWidth + "x" + sourceHeight + "]"
                    + ", target: [" + targetWidth + "x" + targetHeight + "]");
        }
        DownsampleStrategy.SampleSizeRounding rounding = downsampleStrategy.getSampleSizeRounding(sourceWidth,
                sourceHeight, targetWidth, targetHeight);
        if (rounding == null) {
            throw new IllegalArgumentException("Cannot round with null rounding");
        }

        int outWidth = round(exactScaleFactor * sourceWidth);
        int outHeight = round(exactScaleFactor * sourceHeight);

        int widthScaleFactor = sourceWidth / outWidth;
        int heightScaleFactor = sourceHeight / outHeight;

        int scaleFactor = rounding == DownsampleStrategy.SampleSizeRounding.MEMORY
                ? Math.max(widthScaleFactor, heightScaleFactor)
                : Math.min(widthScaleFactor, heightScaleFactor);

        int powerOfTwoSampleSize;
        // BitmapFactory does not support downsampling wbmp files on platforms <= M. See b/27305903.
//        if (Build.VERSION.SDK_INT <= 23
//                && NO_DOWNSAMPLE_PRE_N_MIME_TYPES.contains(options.outMimeType)) {
//            powerOfTwoSampleSize = 1;
//        } else {
            powerOfTwoSampleSize = Math.max(1, Integer.highestOneBit(scaleFactor));
            if (rounding == DownsampleStrategy.SampleSizeRounding.MEMORY
                    && powerOfTwoSampleSize < (1.f / exactScaleFactor)) {
                powerOfTwoSampleSize = powerOfTwoSampleSize << 1;
            }
//        }

        // Here we mimic framework logic for determining how inSampleSize division is rounded on various
        // versions of Android. The logic here has been tested on emulators for Android versions 15-26.
        // PNG - Always uses floor
        // JPEG - Always uses ceiling
        // Webp - Prior to N, always uses floor. At and after N, always uses round.
        options.inSampleSize = powerOfTwoSampleSize;
        int powerOfTwoWidth;
        int powerOfTwoHeight;
        if (imageType == ImageHeaderParser.ImageType.JPEG) {
            // libjpegturbo can downsample up to a sample size of 8. libjpegturbo uses ceiling to round.
            // After libjpegturbo's native rounding, skia does a secondary scale using floor
            // (integer division). Here we replicate that logic.
            int nativeScaling = Math.min(powerOfTwoSampleSize, 8);
            powerOfTwoWidth = (int) Math.ceil(sourceWidth / (float) nativeScaling);
            powerOfTwoHeight = (int) Math.ceil(sourceHeight / (float) nativeScaling);
            int secondaryScaling = powerOfTwoSampleSize / 8;
            if (secondaryScaling > 0) {
                powerOfTwoWidth = powerOfTwoWidth / secondaryScaling;
                powerOfTwoHeight = powerOfTwoHeight / secondaryScaling;
            }
        } else if (imageType == ImageHeaderParser.ImageType.PNG || imageType == ImageHeaderParser.ImageType.PNG_A) {
            powerOfTwoWidth = (int) Math.floor(sourceWidth / (float) powerOfTwoSampleSize);
            powerOfTwoHeight = (int) Math.floor(sourceHeight / (float) powerOfTwoSampleSize);
        } else if (imageType == ImageHeaderParser.ImageType.WEBP || imageType == ImageHeaderParser.ImageType.WEBP_A) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                powerOfTwoWidth = Math.round(sourceWidth / (float) powerOfTwoSampleSize);
                powerOfTwoHeight = Math.round(sourceHeight / (float) powerOfTwoSampleSize);
            } else {
                powerOfTwoWidth = (int) Math.floor(sourceWidth / (float) powerOfTwoSampleSize);
                powerOfTwoHeight = (int) Math.floor(sourceHeight / (float) powerOfTwoSampleSize);
            }
        } else if (
                sourceWidth % powerOfTwoSampleSize != 0 || sourceHeight % powerOfTwoSampleSize != 0) {
            // If we're not confident the image is in one of our types, fall back to checking the
            // dimensions again. inJustDecodeBounds decodes do obey inSampleSize.
            int[] dimensions = getDimensions(is, options, decodeCallbacks, bitmapPool);
            // Power of two downsampling in BitmapFactory uses a variety of random factors to determine
            // rounding that we can't reliably replicate for all image formats. Use ceiling here to make
            // sure that we at least provide a Bitmap that's large enough to fit the content we're going
            // to load.
            powerOfTwoWidth = dimensions[0];
            powerOfTwoHeight = dimensions[1];
        } else {
            powerOfTwoWidth = sourceWidth / powerOfTwoSampleSize;
            powerOfTwoHeight = sourceHeight / powerOfTwoSampleSize;
        }

        double adjustedScaleFactor = downsampleStrategy.getScaleFactor(
                powerOfTwoWidth, powerOfTwoHeight, targetWidth, targetHeight);

        // Density scaling is only supported if inBitmap is null prior to KitKat. Avoid setting
        // densities here so we calculate the final Bitmap size correctly.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            options.inTargetDensity = adjustTargetDensityForError(adjustedScaleFactor);
            options.inDensity = DENSITY_PRECISION_MULTIPLIER;
        }
        if (isScaling(options)) {
            options.inScaled = true;
        } else {
            options.inDensity = options.inTargetDensity = 0;
        }

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Calculate scaling"
                    + ", source: [" + sourceWidth + "x" + sourceHeight + "]"
                    + ", target: [" + targetWidth + "x" + targetHeight + "]"
                    + ", power of two scaled: [" + powerOfTwoWidth + "x" + powerOfTwoHeight + "]"
                    + ", exact scale factor: " + exactScaleFactor
                    + ", power of 2 sample size: " + powerOfTwoSampleSize
                    + ", adjusted scale factor: " + adjustedScaleFactor
                    + ", target density: " + options.inTargetDensity
                    + ", density: " + options.inDensity);
        }
    }

    /**
     * BitmapFactory calculates the density scale factor as a float. This introduces some non-trivial
     * error. This method attempts to account for that error by adjusting the inTargetDensity so that
     * the final scale factor is as close to our target as possible.
     */
    private static int adjustTargetDensityForError(double adjustedScaleFactor) {
        int targetDensity = round(DENSITY_PRECISION_MULTIPLIER * adjustedScaleFactor);
        float scaleFactorWithError = targetDensity / (float) DENSITY_PRECISION_MULTIPLIER;
        double difference = adjustedScaleFactor / scaleFactorWithError;
        return round(difference * targetDensity);
    }

    // This is weird, but it matches the logic in a bunch of Android views/framework classes for
    // rounding.
    private static int round(double value) {
        return (int) (value + 0.5d);
    }

    private boolean shouldUsePool(ImageHeaderParser.ImageType imageType) throws IOException {
        // On KitKat+, any bitmap (of a given config) can be used to decode any other bitmap
        // (with the same config).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return true;
        }

        // We cannot reuse bitmaps when decoding images that are not PNG or JPG prior to KitKat.
        // See: https://groups.google.com/forum/#!msg/android-developers/Mp0MFVFi1Fo/e8ZQ9FGdWdEJ
        return false;  //TYPES_THAT_USE_POOL_PRE_KITKAT.contains(imageType);
    }

    @SuppressWarnings("deprecation")
    private void calculateConfig(
            InputStream is,
            DecodeFormat format,
            boolean isHardwareConfigAllowed,
            boolean isExifOrientationRequired,
            BitmapFactory.Options optionsWithScaling,
            int targetWidth,
            int targetHeight)
            throws IOException {

//        if (hardwareConfigState.setHardwareConfigIfAllowed(
//                targetWidth,
//                targetHeight,
//                optionsWithScaling,
//                format,
//                isHardwareConfigAllowed,
//                isExifOrientationRequired)) {
//            return;
//        }

        // Changing configs can cause skewing on 4.1, see issue #128.
        if (format == DecodeFormat.PREFER_ARGB_8888
                || Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
            optionsWithScaling.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return;
        }

        boolean hasAlpha = false;
        try {
            hasAlpha = ImageHeaderParserUtils.getType(parsers, is, byteArrayPool).hasAlpha();
        } catch (IOException e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Cannot determine whether the image has alpha or not from header"
                        + ", format " + format, e);
            }
        }

        optionsWithScaling.inPreferredConfig =
                hasAlpha ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        if (optionsWithScaling.inPreferredConfig == Bitmap.Config.RGB_565
                || optionsWithScaling.inPreferredConfig == Bitmap.Config.ARGB_4444
                || optionsWithScaling.inPreferredConfig == Bitmap.Config.ALPHA_8) {
            optionsWithScaling.inDither = true;
        }
    }

    /**
     * A method for getting the dimensions of an image from the given InputStream.
     *
     * @param is      The InputStream representing the image.
     * @param options The options to pass to {@link BitmapFactory#decodeStream(InputStream,
     *                android.graphics.Rect, BitmapFactory.Options)}.
     * @return an array containing the dimensions of the image in the form {width, height}.
     */
    private static int[] getDimensions(InputStream is, BitmapFactory.Options options,
                                       Downsampler.DecodeCallbacks decodeCallbacks, BitmapPool bitmapPool) throws IOException {
        options.inJustDecodeBounds = true;
        decodeStream(is, options, decodeCallbacks, bitmapPool);
        options.inJustDecodeBounds = false;
        return new int[] { options.outWidth, options.outHeight };
    }

    private static Bitmap decodeStream(InputStream is, BitmapFactory.Options options,
                                       Downsampler.DecodeCallbacks callbacks, BitmapPool bitmapPool) throws IOException {
        if (options.inJustDecodeBounds) {
            is.mark(MARK_POSITION);
        } else {
            // Once we've read the image header, we no longer need to allow the buffer to expand in
            // size. To avoid unnecessary allocations reading image data, we fix the mark limit so that it
            // is no larger than our current buffer size here. We need to do so immediately before
            // decoding the full image to avoid having our mark limit overridden by other calls to
            // markand reset. See issue #225.
            callbacks.onObtainBounds();
        }
        // BitmapFactory.Options out* variables are reset by most calls to decodeStream, successful or
        // otherwise, so capture here in case we log below.
        int sourceWidth = options.outWidth;
        int sourceHeight = options.outHeight;
        String outMimeType = options.outMimeType;
        final Bitmap result;
        TransformationUtils.getBitmapDrawableLock().lock();
        try {
            //result = BitmapFactory.decodeStream(is, null, options);
            result = WebpBitmapFactory.decodeStream(is, null, options);
        } catch (IllegalArgumentException e) {
            IOException bitmapAssertionException =
                    newIoExceptionForInBitmapAssertion(e, sourceWidth, sourceHeight, outMimeType, options);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Failed to decode with inBitmap, trying again without Bitmap re-use",
                        bitmapAssertionException);
            }
            if (options.inBitmap != null) {
                try {
                    is.reset();
                    bitmapPool.put(options.inBitmap);
                    options.inBitmap = null;
                    return decodeStream(is, options, callbacks, bitmapPool);
                } catch (IOException resetException) {
                    throw bitmapAssertionException;
                }
            }
            throw bitmapAssertionException;
        } finally {
            TransformationUtils.getBitmapDrawableLock().unlock();
        }

        if (options.inJustDecodeBounds) {
            is.reset();

        }
        return result;
    }

    private static boolean isScaling(BitmapFactory.Options options) {
        return options.inTargetDensity > 0 && options.inDensity > 0
                && options.inTargetDensity != options.inDensity;
    }

    private static void logDecode(int sourceWidth, int sourceHeight, String outMimeType,
                                  BitmapFactory.Options options, Bitmap result, int requestedWidth, int requestedHeight,
                                  long startTime) {
        Log.v(TAG, "Decoded " + getBitmapString(result)
                + " from [" + sourceWidth + "x" + sourceHeight + "] " + outMimeType
                + " with inBitmap " + getInBitmapString(options)
                + " for [" + requestedWidth + "x" + requestedHeight + "]"
                + ", sample size: " + options.inSampleSize
                + ", density: " + options.inDensity
                + ", target density: " + options.inTargetDensity
                + ", thread: " + Thread.currentThread().getName()
                + ", duration: " + LogTime.getElapsedMillis(startTime));
    }

    private static String getInBitmapString(BitmapFactory.Options options) {
        return getBitmapString(options.inBitmap);
    }

    @Nullable
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static String getBitmapString(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        String sizeString = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                ? " (" + bitmap.getAllocationByteCount() + ")" : "";
        return  "[" + bitmap.getWidth() + "x" + bitmap.getHeight() + "] " + bitmap.getConfig()
                + sizeString;
    }

    // BitmapFactory throws an IllegalArgumentException if any error occurs attempting to decode a
    // file when inBitmap is non-null, including those caused by partial or corrupt data. We still log
    // the error because the IllegalArgumentException is supposed to catch errors reusing Bitmaps, so
    // want some useful log output. In most cases this can be safely treated as a normal IOException.
    private static IOException newIoExceptionForInBitmapAssertion(IllegalArgumentException e,
                                                                  int outWidth, int outHeight, String outMimeType, BitmapFactory.Options options) {
        return new IOException("Exception decoding bitmap"
                + ", outWidth: " + outWidth
                + ", outHeight: " + outHeight
                + ", outMimeType: " + outMimeType
                + ", inBitmap: " + getInBitmapString(options), e);
    }

    @SuppressWarnings("PMD.CollapsibleIfStatements")
    @TargetApi(Build.VERSION_CODES.O)
    private static void setInBitmap(BitmapFactory.Options options, BitmapPool bitmapPool, int width,
                                    int height) {
        // Avoid short circuiting, it appears to break on some devices.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (options.inPreferredConfig == Bitmap.Config.HARDWARE) {
                return;
            }
        }
        // BitmapFactory will clear out the Bitmap before writing to it, so getDirty is safe.
        options.inBitmap = bitmapPool.getDirty(width, height, options.inPreferredConfig);
    }

    private static synchronized BitmapFactory.Options getDefaultOptions() {
        BitmapFactory.Options decodeBitmapOptions;
        synchronized (OPTIONS_QUEUE) {
            decodeBitmapOptions = OPTIONS_QUEUE.poll();
        }
        if (decodeBitmapOptions == null) {
            decodeBitmapOptions = new BitmapFactory.Options();
            resetOptions(decodeBitmapOptions);
        }

        return decodeBitmapOptions;
    }

    private static void releaseOptions(BitmapFactory.Options decodeBitmapOptions) {
        resetOptions(decodeBitmapOptions);
        synchronized (OPTIONS_QUEUE) {
            OPTIONS_QUEUE.offer(decodeBitmapOptions);
        }
    }

    @SuppressWarnings("deprecation")
    private static void resetOptions(BitmapFactory.Options decodeBitmapOptions) {
        decodeBitmapOptions.inTempStorage = null;
        decodeBitmapOptions.inDither = false;
        decodeBitmapOptions.inScaled = false;
        decodeBitmapOptions.inSampleSize = 1;
        decodeBitmapOptions.inPreferredConfig = null;
        decodeBitmapOptions.inJustDecodeBounds = false;
        decodeBitmapOptions.inDensity = 0;
        decodeBitmapOptions.inTargetDensity = 0;
        decodeBitmapOptions.outWidth = 0;
        decodeBitmapOptions.outHeight = 0;
        decodeBitmapOptions.outMimeType = null;
        decodeBitmapOptions.inBitmap = null;
        decodeBitmapOptions.inMutable = true;
    }
}
