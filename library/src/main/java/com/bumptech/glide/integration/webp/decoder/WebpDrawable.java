package com.bumptech.glide.integration.webp.decoder;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.Preconditions;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * A Drawable that can display animated webp with multiple frame bitmaps
 *
 * @author liuchun
 */
public class WebpDrawable extends Drawable implements WebpFrameLoader.FrameCallback,
        Animatable, Animatable2Compat {
    /**
     * A constant indicating that an animated drawable should loop continuously.
     */
    public static final int LOOP_FOREVER = -1;
    /**
     * A constant indicating that an animated drawable should loop for its default number of times.
     * For animated GIFs, this constant indicates the GIF should use the netscape loop count if
     * present.
     */
    public static final int LOOP_INTRINSIC = 0;

    private static final int GRAVITY = Gravity.FILL;

    private final WebpState state;
    /**
     * True if the drawable is currently animating.
     */
    private boolean isRunning;
    /**
     * True if the drawable should animate while visible.
     */
    private boolean isStarted;
    /**
     * True if the drawable's resources have been recycled.
     */
    private boolean isRecycled;
    /**
     * True if the drawable is currently visible. Default to true because on certain platforms (at
     * least 4.1.1), setVisible is not called on {@link Drawable Drawables}
     * during {@link android.widget.ImageView#setImageDrawable(Drawable)}.
     * See issue #130.
     */
    private boolean isVisible = true;
    /**
     * The number of times we've looped over all the frames in the GIF.
     */
    private int loopCount;
    /**
     * The number of times to loop through the GIF animation.
     */
    private int maxLoopCount = LOOP_FOREVER;

    private boolean applyGravity;
    private Paint paint;
    private Rect destRect;

    /**
     * Callbacks to notify loop completion of a webp animation, where the loop count is explicitly specified.
     */
    private List<AnimationCallback> animationCallbacks;

    public WebpDrawable(Context context, WebpDecoder webDecoder, BitmapPool bitmapPool,
           Transformation<Bitmap> frameTransformation, int targetFrameWidth, int targetFrameHeight,
                        Bitmap firstFrame) {
        this(
            new WebpState(bitmapPool,
                new WebpFrameLoader(
                        Glide.get(context),
                        webDecoder,
                        targetFrameWidth,
                        targetFrameHeight,
                        frameTransformation,
                        firstFrame)));
    }

    WebpDrawable(WebpState state) {
        this.isVisible = true;
        this.state = Preconditions.checkNotNull(state);
        setLoopCount(LOOP_INTRINSIC);
    }

    @VisibleForTesting
    WebpDrawable(WebpFrameLoader frameLoader, BitmapPool bitmapPool, Paint paint) {
        this(new WebpState(bitmapPool, frameLoader));
        this.paint = paint;
    }

    public int getSize() {
        return state.frameLoader.getSize();
    }

    public Bitmap getFirstFrame() {
        return state.frameLoader.getFirstFrame();
    }

    public void setFrameTransformation(Transformation<Bitmap> frameTransformation, Bitmap firstFrame) {
        state.frameLoader.setFrameTransformation(frameTransformation, firstFrame);
    }

    public Transformation<Bitmap> getFrameTransformation() {
        return state.frameLoader.getFrameTransformation();
    }

    public ByteBuffer getBuffer() {
        return state.frameLoader.getBuffer();
    }

    public int getFrameCount() {
        return state.frameLoader.getFrameCount();
    }

    public int getFrameIndex() {
        return state.frameLoader.getCurrentIndex();
    }

    private void resetLoopCount() {
        loopCount = 0;
    }

    public void startFromFirstFrame() {
        Preconditions.checkArgument(!isRunning, "You cannot restart a currently running animation.");
        state.frameLoader.setNextStartFromFirstFrame();
        start();
    }

    public void start() {
        isStarted = true;
        resetLoopCount();
        if(isVisible) {
            startRunning();
        }

    }

    public void stop() {
        isStarted = false;
        stopRunning();
    }

    private void startRunning() {
        Preconditions.checkArgument(!isRecycled, "You cannot start a recycled Drawable. Ensure thatyou clear any references to the Drawable when clearing the corresponding request.");
        if(state.frameLoader.getFrameCount() == 1) {
            invalidateSelf();
        } else if(!isRunning) {
            isRunning = true;
            state.frameLoader.subscribe(this);
            invalidateSelf();
        }

    }

    private void stopRunning() {
        isRunning = false;
        state.frameLoader.unsubscribe(this);
    }

    public boolean setVisible(boolean visible, boolean restart) {
        Preconditions.checkArgument(!isRecycled, "Cannot change the visibility of a recycled resource. Ensure that you unset the Drawable from your View before changing the View\'s visibility.");
        isVisible = visible;
        if(!visible) {
            stopRunning();
        } else if(isStarted) {
            startRunning();
        }

        return super.setVisible(visible, restart);
    }

    public int getIntrinsicWidth() {
        return state.frameLoader.getWidth();
    }

    public int getIntrinsicHeight() {
        return state.frameLoader.getHeight();
    }

    public boolean isRunning() {
        return isRunning;
    }

    void setIsRunning(boolean isRunning) {
        isRunning = isRunning;
    }

    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        applyGravity = true;
    }

    public void draw(Canvas canvas) {
        if (isRecycled()) {
            return;
        }

        if(applyGravity) {
            Gravity.apply(GRAVITY, getIntrinsicWidth(), getIntrinsicHeight(), getBounds(), getDestRect());
            applyGravity = false;
        }

        Bitmap currentFrame = state.frameLoader.getCurrentFrame();
        canvas.drawBitmap(currentFrame, (Rect)null, getDestRect(), getPaint());
    }

    public void setAlpha(int i) {
        getPaint().setAlpha(i);
    }

    public void setColorFilter(ColorFilter colorFilter) {
        getPaint().setColorFilter(colorFilter);
    }

    private Rect getDestRect() {
        if(destRect == null) {
            destRect = new Rect();
        }

        return destRect;
    }

    private Paint getPaint() {
        if(paint == null) {
            paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        }

        return paint;
    }

    @Deprecated
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    // See #1087.
    private Callback findCallback() {
        Callback callback = getCallback();
        while (callback instanceof Drawable) {
            callback = ((Drawable) callback).getCallback();
        }
        return callback;
    }

    public void onFrameReady() {
        if(findCallback() == null) {
            stop();
            invalidateSelf();
            return;
        }
        invalidateSelf();
        if(getFrameIndex() == getFrameCount() - 1) {
            loopCount++;
        }

        if(maxLoopCount != LOOP_FOREVER && loopCount >= maxLoopCount) {
            stop();
            notifyAnimationEndToListeners();
        }
    }

    private void notifyAnimationEndToListeners() {
        if (animationCallbacks != null) {
            for (int i = 0, size = animationCallbacks.size(); i < size; i++) {
                animationCallbacks.get(i).onAnimationEnd(this);
            }
        }
    }

    public ConstantState getConstantState() {
        return state;
    }

    public void recycle() {
        isRecycled = true;
        state.frameLoader.clear();
    }

    boolean isRecycled() {
        return isRecycled;
    }

    public void setLoopCount(int loopCount) {
        if(loopCount <= 0 && loopCount != LOOP_FOREVER && loopCount != LOOP_INTRINSIC) {
            throw new IllegalArgumentException("Loop count must be greater than 0, or equal to LOOP_FOREVER, or equal to LOOP_INTRINSIC");
        } else {
            if(loopCount == LOOP_INTRINSIC) {
                int intrinsicCount = state.frameLoader.getLoopCount();
                maxLoopCount = intrinsicCount == LOOP_INTRINSIC ? LOOP_FOREVER : intrinsicCount;
            } else {
                maxLoopCount = loopCount;
            }
        }
    }

    public int getLoopCount() {
        return maxLoopCount;
    }

    public int getIntrinsicLoopCount() {
        return state.frameLoader.getLoopCount();
    }

    /**
     * Register callback to listen to WebpDrawable animation end event after specific loop count
     * set by {@link WebpDrawable#setLoopCount(int)}.
     *
     * Note: This will only be called if the Gif stop because it reaches the loop count. Unregister
     * this in onLoadCleared to avoid potential memory leak.
     * @see WebpDrawable#unregisterAnimationCallback(AnimationCallback).
     *
     * @param animationCallback Animation callback {@link AnimationCallback}.
     */
    @Override
    public void registerAnimationCallback(@NonNull AnimationCallback animationCallback) {
        if (animationCallback == null) {
            return;
        }
        if (animationCallbacks == null) {
            animationCallbacks = new ArrayList<>();
        }
        animationCallbacks.add(animationCallback);
    }

    @Override
    public boolean unregisterAnimationCallback(@NonNull AnimationCallback animationCallback) {
        if (animationCallbacks == null || animationCallback == null) {
            return false;
        }
        return animationCallbacks.remove(animationCallback);
    }

    @Override
    public void clearAnimationCallbacks() {
        if (animationCallbacks != null) {
            animationCallbacks.clear();
        }
    }

    static class WebpState extends ConstantState {
        final BitmapPool bitmapPool;
        final WebpFrameLoader frameLoader;

        public WebpState(BitmapPool bitmapPool, WebpFrameLoader frameLoader) {
            this.bitmapPool = bitmapPool;
            this.frameLoader = frameLoader;
        }

        public Drawable newDrawable(Resources res) {
            return this.newDrawable();
        }

        public Drawable newDrawable() {
            return new WebpDrawable(this);
        }

        public int getChangingConfigurations() {
            return 0;
        }
    }
}
