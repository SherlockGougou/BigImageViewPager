package com.bumptech.glide.integration.webp.decoder;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.signature.ObjectKey;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Util;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;


/**
 * @author liuchun
 */
public class WebpFrameLoader {

    public static final Option<WebpFrameCacheStrategy> FRAME_CACHE_STRATEGY = Option.memory(
            "com.bumptech.glide.integration.webp.decoder.WebpFrameLoader.CacheStrategy", WebpFrameCacheStrategy.AUTO);

    private final WebpDecoder webpDecoder;
    private final Handler handler;
    private final List<FrameCallback> callbacks;
    final RequestManager requestManager;
    private final BitmapPool bitmapPool;

    private boolean isRunning;
    private boolean isLoadPending;
    private boolean startFromFirstFrame;
    private RequestBuilder<Bitmap> requestBuilder;
    private DelayTarget current;
    private boolean isCleared;
    private DelayTarget next;
    private Bitmap firstFrame;
    private Transformation<Bitmap> transformation;
    private DelayTarget pendingTarget;
    @Nullable
    private OnEveryFrameListener onEveryFrameListener;
    private int firstFrameSize;
    private int width;
    private int height;

    public interface FrameCallback {
        void onFrameReady();
    }

    public WebpFrameLoader(Glide glide, WebpDecoder webpDecoder,
                           int width, int height,
                           Transformation<Bitmap> transformation, Bitmap firstFrame) {
        this(glide.getBitmapPool(),
                Glide.with(glide.getContext()),
                webpDecoder,
                (Handler) null,
                getRequestBuilder(Glide.with(glide.getContext()), width, height),
                transformation,
                firstFrame);
    }

    WebpFrameLoader(BitmapPool bitmapPool, RequestManager requestManager,
                    WebpDecoder webpDecoder, Handler handler,
                    RequestBuilder<Bitmap> requestBuilder,
                    Transformation<Bitmap> transformation, Bitmap firstFrame) {
        this.callbacks = new ArrayList<>();
        this.isRunning = false;
        this.isLoadPending = false;
        this.startFromFirstFrame = false;
        this.requestManager = requestManager;
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper(), new FrameLoaderCallback());
        }

        this.bitmapPool = bitmapPool;
        this.handler = handler;
        this.requestBuilder = requestBuilder;
        this.webpDecoder = webpDecoder;
        this.setFrameTransformation(transformation, firstFrame);
    }

    void setFrameTransformation(Transformation<Bitmap> transformation, Bitmap firstFrame) {
        this.transformation = Preconditions.checkNotNull(transformation);
        this.firstFrame = (Bitmap) Preconditions.checkNotNull(firstFrame);
        requestBuilder = requestBuilder.apply((new RequestOptions()).transform(transformation));

        this.firstFrameSize = Util.getBitmapByteSize(firstFrame);
        this.width = firstFrame.getWidth();
        this.height = firstFrame.getHeight();
    }

    Transformation<Bitmap> getFrameTransformation() {
        return this.transformation;
    }

    Bitmap getFirstFrame() {
        return this.firstFrame;
    }

    void subscribe(FrameCallback frameCallback) {
        if (isCleared) {
            throw new IllegalStateException("Cannot subscribe to a cleared frame loader");
        }
        if (callbacks.contains(frameCallback)) {
            throw new IllegalStateException("Cannot subscribe twice in a row");
        }

        boolean start = callbacks.isEmpty();
        callbacks.add(frameCallback);
        if (start) {
            start();
        }
    }

    void unsubscribe(FrameCallback frameCallback) {
        callbacks.remove(frameCallback);
        if (callbacks.isEmpty()) {
            stop();
        }

    }

    int getWidth() {
        return width;
    }

    int getHeight() {
        return height;
    }

    int getSize() {
        return webpDecoder.getByteSize() + firstFrameSize;
    }

    int getCurrentIndex() {
        return current != null ? current.index : -1;
    }

    ByteBuffer getBuffer() {
        return webpDecoder.getData().asReadOnlyBuffer();
    }

    int getFrameCount() {
        return webpDecoder.getFrameCount();
    }

    int getLoopCount() {
        return webpDecoder.getTotalIterationCount();
    }

    private void start() {
        if (isRunning) {
            return;
        }
        isRunning = true;
        isCleared = false;

        loadNextFrame();
    }

    private void stop() {
        isRunning = false;
    }

    void clear() {
        callbacks.clear();
        recycleFirstFrame();
        stop();
        if (current != null) {
            requestManager.clear(current);
            current = null;
        }
        if (next != null) {
            requestManager.clear(next);
            next = null;
        }
        if (pendingTarget != null) {
            requestManager.clear(pendingTarget);
            pendingTarget = null;
        }

        webpDecoder.clear();
        isCleared = true;
    }

    Bitmap getCurrentFrame() {
        return current != null ? current.getResource() : firstFrame;
    }

    private void loadNextFrame() {
        if (!isRunning || isLoadPending) {
            return;
        }

        if (startFromFirstFrame) {
            Preconditions.checkArgument(
                    pendingTarget == null, "Pending target must be null when starting from the first frame");
            webpDecoder.resetFrameIndex();
            startFromFirstFrame = false;
        }

        if (pendingTarget != null) {
            DelayTarget temp = pendingTarget;
            pendingTarget = null;
            onFrameReady(temp);
            return;
        }

        isLoadPending = true;

        // Get the delay before incrementing the pointer because the delay indicates the amount of time
        // we want to spend on the current frame.
        int delay = webpDecoder.getNextDelay();
        long targetTime = SystemClock.uptimeMillis() + delay;

        webpDecoder.advance();
        int frameIndex = webpDecoder.getCurrentFrameIndex();
        next = new DelayTarget(handler, frameIndex, targetTime);

        WebpFrameCacheStrategy cacheStrategy = webpDecoder.getCacheStrategy();
        RequestOptions options = RequestOptions.signatureOf(getFrameSignature(frameIndex))
                .skipMemoryCache(cacheStrategy.noCache());
        requestBuilder.apply(options).load(webpDecoder).into(next);
    }

    private void recycleFirstFrame() {
        if (firstFrame != null) {
            bitmapPool.put(firstFrame);
            firstFrame = null;
        }

    }

    void setNextStartFromFirstFrame() {
        Preconditions.checkArgument(!isRunning, "Can't restart a running animation");
        startFromFirstFrame = true;
        if (pendingTarget != null) {
            requestManager.clear(pendingTarget);
            pendingTarget = null;
        }
    }

    void setOnEveryFrameReadyListener(@Nullable OnEveryFrameListener onEveryFrameListener) {
        this.onEveryFrameListener = onEveryFrameListener;
    }

    void onFrameReady(DelayTarget delayTarget) {
        if (onEveryFrameListener != null) {
            onEveryFrameListener.onFrameReady();
        }
        isLoadPending = false;
        if (isCleared) {
            handler.obtainMessage(FrameLoaderCallback.MSG_CLEAR, delayTarget).sendToTarget();
            return;
        }
        // If we're not running, notifying here will recycle the frame that we might currently be
        // showing, which breaks things (see #2526). We also can't discard this frame because we've
        // already incremented the frame pointer and can't decode the same frame again. Instead we'll
        // just hang on to this next frame until start() or clear() are called.
        if (!isRunning) {
            if (startFromFirstFrame) {
                handler.obtainMessage(FrameLoaderCallback.MSG_CLEAR, delayTarget).sendToTarget();
            } else {
                pendingTarget = delayTarget;
            }
            return;
        }

        if (delayTarget.getResource() != null) {
            recycleFirstFrame();
            DelayTarget previous = current;
            current = delayTarget;
            // The callbacks may unregister when onFrameReady is called, so iterate in reverse to avoid
            // concurrent modifications.
            for (int i = callbacks.size() - 1; i >= 0; i--) {
                try {
                    // add try-catch to fix multi-thread race bug #100, #102
                    FrameCallback cb = callbacks.get(i);
                    if (cb == null)
                        continue;
                    cb.onFrameReady();
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }
            if (previous != null) {
                handler.obtainMessage(FrameLoaderCallback.MSG_CLEAR, previous).sendToTarget();
            }
        }

        loadNextFrame();
    }

    private class FrameLoaderCallback implements Handler.Callback {
        static final int MSG_DELAY = 1;
        static final int MSG_CLEAR = 2;

        FrameLoaderCallback() {
        }

        public boolean handleMessage(Message msg) {
            DelayTarget target;
            if (msg.what == MSG_DELAY) {
                target = (DelayTarget) msg.obj;
                WebpFrameLoader.this.onFrameReady(target);
                return true;
            } else if (msg.what == MSG_CLEAR) {
                target = (DelayTarget) msg.obj;
                WebpFrameLoader.this.requestManager.clear(target);
            }
            return false;
        }
    }


    static class DelayTarget extends CustomTarget<Bitmap> {
        private final Handler handler;
        final int index;
        private final long targetTime;
        private Bitmap resource;

        DelayTarget(Handler handler, int index, long targetTime) {
            this.handler = handler;
            this.index = index;
            this.targetTime = targetTime;
        }

        Bitmap getResource() {
            return this.resource;
        }

        public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
            this.resource = resource;
            Message msg = handler.obtainMessage(FrameLoaderCallback.MSG_DELAY, this);
            handler.sendMessageAtTime(msg, targetTime);
        }

        @Override
        public void onLoadCleared(@Nullable Drawable placeholder) {
            this.resource = null;
        }
    }

    private static RequestBuilder<Bitmap> getRequestBuilder(RequestManager requestManager, int width, int height) {
        return requestManager
                .asBitmap()
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)
                        .useAnimationPool(true)
                        .skipMemoryCache(true)
                        .override(width, height));
    }

    private Key getFrameSignature(int frameIndex) {
        // Some devices seem to have crypto bugs that throw exceptions when you create a new UUID.
        // See #1510.
        //return new ObjectKey(Math.random());
        return new WebpFrameCacheKey(new ObjectKey(webpDecoder), frameIndex);
    }

    private static class WebpFrameCacheKey implements Key {

        private final Key sourceKey;
        private final int frameIndex;

        WebpFrameCacheKey(Key sourceKey, int frameIndex) {
            this.sourceKey = sourceKey;
            this.frameIndex = frameIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof WebpFrameCacheKey) {
                WebpFrameCacheKey other = (WebpFrameCacheKey) o;
                return sourceKey.equals(other.sourceKey) && frameIndex == other.frameIndex;
            }
            return false;
        }

        @Override
        public int hashCode() {
            int result = sourceKey.hashCode();
            return 31 * result + frameIndex;
        }

        @Override
        public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
            byte[] data = ByteBuffer.allocate(12).putInt(frameIndex).array();
            messageDigest.update(data);
            sourceKey.updateDiskCacheKey(messageDigest);
        }
    }

    interface OnEveryFrameListener {
        void onFrameReady();
    }
}
