package com.bumptech.glide.integration.webp.decoder;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author liuchun
 */
class Utils {
    private static final String TAG = "Utils";

    static int getSampleSize(int srcWidth, int srcHeight, int targetWidth, int targetHeight) {
        int exactSampleSize = Math.min(srcHeight / targetHeight,
                srcWidth / targetWidth);
        int powerOfTwoSampleSize = exactSampleSize == 0 ? 0 : Integer.highestOneBit(exactSampleSize);
        // Although functionally equivalent to 0 for BitmapFactory, 1 is a safer default for our code
        // than 0.
        int sampleSize = Math.max(1, powerOfTwoSampleSize);
        if (Log.isLoggable(TAG, Log.VERBOSE) && sampleSize > 1) {
            Log.v(TAG, "Downsampling WEBP"
                    + ", sampleSize: " + sampleSize
                    + ", target dimens: [" + targetWidth + "x" + targetHeight + "]"
                    + ", actual dimens: [" + srcWidth + "x" + srcHeight + "]");
        }
        return sampleSize;
    }

    static byte[] inputStreamToBytes(InputStream is) {
        final int bufferSize = 16384;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(bufferSize);
        try {
            int nRead;
            byte[] data = new byte[bufferSize];
            while ((nRead = is.read(data)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
        } catch (IOException e) {
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Error reading data from stream", e);
            }
            return null;
        }
        return buffer.toByteArray();
    }
}
