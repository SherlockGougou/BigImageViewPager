package com.bumptech.glide.integration.webp;


import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Base64;

import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.resource.bitmap.RecyclableBufferedInputStream;
import com.bumptech.glide.util.Preconditions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 解析webp数据头，区分不同的Webp图片类型
 *
 * @author liuchun
 */
public class WebpHeaderParser {
    public static final int MAX_WEBP_HEADER_SIZE = 21;
    // WebP-related
    // "RIFF"
    private static final int RIFF_HEADER = 0x52494646;
    // "WEBP"
    private static final int WEBP_HEADER = 0x57454250;
    // "VP8" null.
    private static final int WEBP_VP8_HEADER = 0x56503820;
    private static final int WEBP_VP8L_HEADER = 0x5650384C;
    private static final int WEBP_VP8X_HEADER = 0x56503858;
    // "VP8X" Flag
    private static final int WEBP_EXTENDED_ALPHA_FLAG = 1 << 4;
    private static final int WEBP_EXTENDED_ANIM_FLAG = 1 << 1;
    private static final int WEBP_LOSSLESS_ALPHA_FLAG = 1 << 3;

    /**
     * BASE64 encoded extended WebP image.
     */
    private static final String VP8X_WEBP_BASE64 = "UklGRkoAAABXRUJQVlA4WAoAAAAQAAAAAAAAAAAAQUxQSAw" +
            "AAAARBxAR/Q9ERP8DAABWUDggGAAAABQBAJ0BKgEAAQAAAP4AAA3AAP7mtQAAAA==";

    public static final boolean sIsExtendedWebpSupported = isExtendedWebpSupported();


    public static boolean isExtendedWebpSupported() {

        // Lossless and extended formats are supported on Android 4.2.1+
        // Unfortunately SDK_INT is not enough to distinguish 4.2 and 4.2.1
        // (both are API level 17 (JELLY_BEAN_MR1))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return false;
        }

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Let's test if extended webp is supported
            // To this end we will try to decode bounds of vp8x webp with alpha channel
            byte[] decodedBytes = Base64.decode(VP8X_WEBP_BASE64, Base64.DEFAULT);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length, opts);

            // If Android managed to find appropriate decoder then opts.outHeight and opts.outWidth
            // should be set. We can not assume that outMimeType is set.
            // Android guys forgot to update logic for mime types when they introduced support for webp.
            // For example, on 4.2.2 this field is not set for webp images.
            if (opts.outHeight != 1 || opts.outWidth != 1) {
                return false;
            }
        }

        return true;
    }


    public static boolean isStaticWebpType(WebpImageType imageType) {
        return imageType == WebpImageType.WEBP_SIMPLE ||
                imageType == WebpImageType.WEBP_LOSSLESS ||
                imageType == WebpImageType.WEBP_LOSSLESS_WITH_ALPHA ||
                imageType == WebpImageType.WEBP_EXTENDED ||
                imageType == WebpImageType.WEBP_EXTENDED_WITH_ALPHA;
    }


    public static boolean isNonSimpleWebpType(WebpImageType imageType) {
        return imageType != WebpImageType.NONE_WEBP && imageType != WebpImageType.WEBP_SIMPLE;
    }

    public static boolean isAnimatedWebpType(WebpImageType imageType) {
        return imageType == WebpImageType.WEBP_EXTENDED_ANIMATED;
    }

    public static WebpImageType getType(InputStream is, ArrayPool byteArrayPool) throws IOException {

        if (is == null) {
            return WebpImageType.NONE_WEBP;
        }

        if (!is.markSupported()) {
            is = new RecyclableBufferedInputStream(is, byteArrayPool);
        }

        is.mark(MAX_WEBP_HEADER_SIZE);
        try {
            return getType(new StreamReader(Preconditions.checkNotNull(is)));
        } finally {
            is.reset();
        }
    }

    public static WebpImageType getType(ByteBuffer byteBuffer) throws IOException {

        if (byteBuffer == null) {
            return WebpImageType.NONE_WEBP;
        }

        return getType(new ByteBufferReader(Preconditions.checkNotNull(byteBuffer)));
    }

    public static WebpImageType getType(byte[] headers) throws IOException {
        return getType(headers, 0, headers.length);
    }

    public static WebpImageType getType(byte[] headers, int offset, int headerSize) throws IOException {
        return getType(new ByteArrayReader(headers, offset, headerSize));
    }

    private static WebpImageType getType(Reader reader) throws IOException {

        final int firstFourBytes = reader.getUInt16() << 16 & 0xFFFF0000 | reader.getUInt16() & 0xFFFF;
        // WebP (reads up to 21 bytes). See https://developers.google.com/speed/webp/docs/riff_container
        // for details.
        if (firstFourBytes != RIFF_HEADER) {
            return WebpImageType.NONE_WEBP;
        }

        // Bytes 4 - 7 contain length information. Skip these.
        reader.skip(4);
        final int thirdFourBytes = reader.getUInt16() << 16 & 0xFFFF0000 | reader.getUInt16() & 0xFFFF;
        if (thirdFourBytes != WEBP_HEADER) {
            return WebpImageType.NONE_WEBP;
        }

        final int fourthFourBytes = reader.getUInt16() << 16 & 0xFFFF0000 | reader.getUInt16() & 0xFFFF;
        // "VP8 " Lossy
        if (fourthFourBytes == WEBP_VP8_HEADER) {
            return WebpImageType.WEBP_SIMPLE;
        }
        // "VP8L" Lossless
        if (fourthFourBytes == WEBP_VP8L_HEADER) {
            // See chromium.googlesource.com/webm/libwebp/+/master/doc/webp-lossless-bitstream-spec.txt
            // for more info.
            reader.skip(4);
            return (reader.getByte() & WEBP_LOSSLESS_ALPHA_FLAG) != 0 ? WebpImageType.WEBP_LOSSLESS_WITH_ALPHA :
                    WebpImageType.WEBP_LOSSLESS;
        }

        // "VP8X" Extended
        if (fourthFourBytes == WEBP_VP8X_HEADER) {
            // Skip some more length bytes and check for transparency/alpha flag.
            reader.skip(4);
            int meta = reader.getByte();
            if ((meta & WEBP_EXTENDED_ANIM_FLAG) != 0) {
                return WebpImageType.WEBP_EXTENDED_ANIMATED;
            } else if ((meta & WEBP_EXTENDED_ALPHA_FLAG) != 0) {
                return WebpImageType.WEBP_EXTENDED_WITH_ALPHA;
            }

            return WebpImageType.WEBP_EXTENDED;
        }

        return WebpImageType.NONE_WEBP;
    }


    public enum WebpImageType {
        /**
         * Simple Webp type (Lossy) without alpha and animation
         */
        WEBP_SIMPLE(false, false),
        /**
         * Simple Webp type (Lossless) without alpha and animation
         */
        WEBP_LOSSLESS(false, false),
        /**
         * Simple Webp type (Lossless) with alpha and without animation
         */
        WEBP_LOSSLESS_WITH_ALPHA(true, false),
        /**
         * Extended Webp type without alpha and animation
         */
        WEBP_EXTENDED(false, false),
        /**
         * Extended Webp type with alpha and no animation
         */
        WEBP_EXTENDED_WITH_ALPHA(true, false),
        /**
         * Extened Webp type without alpha and has animation
         */
        WEBP_EXTENDED_ANIMATED(false, true),
        /**
         * Unrecognized type
         */
        NONE_WEBP(false, false);

        private final boolean hasAlpha;

        private final boolean hasAnimation;

        WebpImageType(boolean hasAlpha, boolean hasAnimation) {
            this.hasAlpha = hasAlpha;
            this.hasAnimation = hasAnimation;
        }

        public boolean hasAlpha() {
            return hasAlpha;
        }

        public boolean hasAnimation() {
            return hasAnimation;
        }
    }


    private interface Reader {
        int getUInt16() throws IOException;

        short getUInt8() throws IOException;

        long skip(long total) throws IOException;

        int read(byte[] buffer, int byteCount) throws IOException;

        int getByte() throws IOException;
    }

    private static final class ByteArrayReader implements Reader {
        private final byte[] data;
        private final int offset;
        private final int size;
        private int pos;

        ByteArrayReader(byte[] data, int offset, int size) {
            this.data = data;
            this.offset = offset;
            this.size = size;
            pos = offset;
        }

        @Override
        public int getUInt16() throws IOException {
            return (getByte() << 8 & 0xFF00) | (getByte() & 0xFF);
        }

        @Override
        public short getUInt8() throws IOException {
            return (short) (getByte() & 0xFF);
        }

        @Override
        public long skip(long total) throws IOException {
            int toSkip = (int) Math.min(offset + size - pos, total);
            pos += toSkip;
            return toSkip;
        }

        @Override
        public int read(byte[] buffer, int byteCount) throws IOException {
            int toRead = (int) Math.min(offset + size - pos, byteCount);
            if (toRead == 0) {
                return -1;
            }
            System.arraycopy(data, pos, buffer, 0, toRead);
            return toRead;
        }

        @Override
        public int getByte() throws IOException {
            if (pos >= offset + size) {
                return -1;
            }
            return data[pos++];
        }
    }

    private static final class ByteBufferReader implements Reader {

        private final ByteBuffer byteBuffer;

        ByteBufferReader(ByteBuffer byteBuffer) {
            this.byteBuffer = byteBuffer;
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
        }

        @Override
        public int getUInt16() throws IOException {
            return (getByte() << 8 & 0xFF00) | (getByte() & 0xFF);
        }

        @Override
        public short getUInt8() throws IOException {
            return (short) (getByte() & 0xFF);
        }

        @Override
        public long skip(long total) throws IOException {
            int toSkip = (int) Math.min(byteBuffer.remaining(), total);
            byteBuffer.position(byteBuffer.position() + toSkip);
            return toSkip;
        }

        @Override
        public int read(byte[] buffer, int byteCount) throws IOException {
            int toRead = Math.min(byteCount, byteBuffer.remaining());
            if (toRead == 0) {
                return -1;
            }
            byteBuffer.get(buffer, 0 /*dstOffset*/, toRead);
            return toRead;
        }

        @Override
        public int getByte() throws IOException {
            if (byteBuffer.remaining() < 1) {
                return -1;
            }
            return byteBuffer.get();
        }
    }

    private static final class StreamReader implements Reader {
        private final InputStream is;

        // Motorola / big endian byte order.
        StreamReader(InputStream is) {
            this.is = is;
        }

        @Override
        public int getUInt16() throws IOException {
            return (is.read() << 8 & 0xFF00) | (is.read() & 0xFF);
        }

        @Override
        public short getUInt8() throws IOException {
            return (short) (is.read() & 0xFF);
        }

        @Override
        public long skip(long total) throws IOException {
            if (total < 0) {
                return 0;
            }

            long toSkip = total;
            while (toSkip > 0) {
                long skipped = is.skip(toSkip);
                if (skipped > 0) {
                    toSkip -= skipped;
                } else {
                    // Skip has no specific contract as to what happens when you reach the end of
                    // the stream. To differentiate between temporarily not having more data and
                    // having finished the stream, we read a single byte when we fail to skip any
                    // amount of data.
                    int testEofByte = is.read();
                    if (testEofByte == -1) {
                        break;
                    } else {
                        toSkip--;
                    }
                }
            }
            return total - toSkip;
        }

        @Override
        public int read(byte[] buffer, int byteCount) throws IOException {
            int toRead = byteCount;
            int read;
            while (toRead > 0 && ((read = is.read(buffer, byteCount - toRead, toRead)) != -1)) {
                toRead -= read;
            }
            return byteCount - toRead;
        }

        @Override
        public int getByte() throws IOException {
            return is.read();
        }
    }
}
