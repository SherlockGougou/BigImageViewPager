package cc.shinichi.bigimageviewpager.glide;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.transition.Transition;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.zhihu.matisse.engine.ImageEngine;

import java.io.File;

import cc.shinichi.library.glide.FileTarget;

public class GlideV4Engine implements ImageEngine {

    @Override
    public void loadThumbnail(Context context, int i, Drawable drawable, ImageView imageView, Uri uri) {
        GlideApp.with(context).load(uri).placeholder(drawable).apply(new RequestOptions().centerCrop()).into(imageView);
    }

    @Override
    public void loadGifThumbnail(Context context, int i, Drawable drawable, ImageView imageView, Uri uri) {
        GlideApp.with(context).asGif().placeholder(drawable).load(uri).into(imageView);
    }

    @Override
    public void loadImage(Context context, int i, int i1, final SubsamplingScaleImageView subsamplingScaleImageView,
                          Uri uri) {
        GlideApp.with(context).downloadOnly().load(uri).into(new FileTarget() {
            @Override
            public void onResourceReady(@NonNull File resource, @Nullable Transition<? super File> transition) {
                super.onResourceReady(resource, transition);
                subsamplingScaleImageView.setImage(ImageSource.uri(resource.getAbsolutePath()));
            }
        });
    }

    @Override
    public void loadGifImage(Context context, int i, int i1, ImageView imageView, Uri uri) {
        GlideApp.with(context).asGif().load(uri).into(imageView);
    }

    @Override
    public boolean supportAnimatedGif() {
        return true;
    }
}