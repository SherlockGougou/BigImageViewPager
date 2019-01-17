package cc.shinichi.library.glide.engine;

import android.graphics.drawable.Drawable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.Target;

public abstract class ProgressTarget<T, Z> extends WrappingTarget<Z>
    implements OkHttpProgressGlideModule.UIProgressListener {
    private T model;
    private boolean ignoreProgress = true;

    public ProgressTarget(T model, Target<Z> target) {
        super(target);
        this.model = model;
    }

    public final T getModel() {
        return model;
    }

    public final void setModel(T model) {
        Glide.clear(this); // indirectly calls cleanup
        this.model = model;
    }

    protected String toUrlString(T model) {
        return String.valueOf(model);
    }

    @Override public float getGranualityPercentage() {
        return 1.0f;
    }

    private void start() {
        OkHttpProgressGlideModule.expect(toUrlString(model), this);
        ignoreProgress = false;
    }

    private void cleanup() {
        ignoreProgress = true;
        T model = this.model; // save in case it gets modified
        OkHttpProgressGlideModule.forget(toUrlString(model));
        this.model = null;
    }

    @Override public void onLoadStarted(Drawable placeholder) {
        super.onLoadStarted(placeholder);
        start();
    }

    @Override public void onResourceReady(Z resource, GlideAnimation<? super Z> animation) {
        cleanup();
        super.onResourceReady(resource, animation);
    }

    @Override public void onLoadFailed(Exception e, Drawable errorDrawable) {
        cleanup();
        super.onLoadFailed(e, errorDrawable);
    }

    @Override public void onLoadCleared(Drawable placeholder) {
        cleanup();
        super.onLoadCleared(placeholder);
    }
}