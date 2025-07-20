package com.bumptech.glide.integration.webp;


import static com.bumptech.glide.integration.webp.WebpFrame.FRAME_DURATION_MS_FOR_MIN;
import static com.bumptech.glide.integration.webp.WebpFrame.MIN_FRAME_DURATION_MS;

import androidx.annotation.Keep;

import com.bumptech.glide.util.Preconditions;

import java.nio.ByteBuffer;

/**
 *  A WebpImage container whose encoded data held by native ptr
 *
 *  @author liuchun
 */
@Keep
public class WebpImage {
    // Access by native
    @Keep
    private long mNativePtr;

    private int mWidth;
    private int mHeigth;

    private int mFrameCount;
    private int mDurationMs;
    private int[] mFrameDurations;
    private int mLoopCount;

    private int mBackgroundColor;

    static {
        System.loadLibrary("glide-webp");
    }

    public static WebpImage create(byte[] source) {
        Preconditions.checkNotNull(source);

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(source.length);
        byteBuffer.put(source);
        byteBuffer.rewind();

        return nativeCreateFromDirectByteBuffer(byteBuffer);
    }

    /**
     * Private constructor that must received an already allocated native bitmap
     * int (pointer).
     */
    // called from JNI
    @Keep
    WebpImage(long nativePtr, int width, int height, int frameCount,
              int durationMs, int[] frameDurations, int loopCount,
              int backgroundColor) {

        if (nativePtr == 0) {
            throw new RuntimeException("internal error: native WebpImage is 0");
        }

        mWidth = width;
        mHeigth = height;

        mFrameCount = frameCount;
        mDurationMs = durationMs;
        mFrameDurations = frameDurations;
        mLoopCount = loopCount;
        fixFrameDurations(mFrameDurations);

        mBackgroundColor = backgroundColor;

        mNativePtr = nativePtr;
    }

    /**
     * Adjust the frame duration to respect logic for minimum frame duration times
     */
    private void fixFrameDurations(int[] frameDurationMs) {
        for (int i = 0; i < frameDurationMs.length; i++) {
            if (frameDurationMs[i] < MIN_FRAME_DURATION_MS) {
                frameDurationMs[i] = FRAME_DURATION_MS_FOR_MIN;
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        nativeFinalize();
    }

    public void dispose(){
        nativeDispose();
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeigth;
    }

    public int getFrameCount() {
        return mFrameCount;
    }

    public int getDuration() {
        return mDurationMs;
    }

    public int[] getFrameDurations() {
        return mFrameDurations;
    }

    public int getLoopCount() {
        return mLoopCount;
    }

    public int getBackgroundColor() {
        return mBackgroundColor;
    }

    public WebpFrame getFrame(int frameNubmer) {
        return nativeGetFrame(frameNubmer);
    }

    public WebpFrameInfo getFrameInfo(int frameNumber) {

        WebpFrame frame = getFrame(frameNumber);
        try {
            return new WebpFrameInfo(frameNumber, frame);
        } finally {
            frame.dispose();
        }
    }

    public int getSizeInBytes() {
        return nativeGetSizeInBytes();
    }

    private static native WebpImage nativeCreateFromDirectByteBuffer(ByteBuffer buffer);
    private native WebpFrame nativeGetFrame(int frameNumber);
    private native int nativeGetSizeInBytes();
    private native void nativeDispose();
    private native void nativeFinalize();
}
