package cc.shinichi.library.loader;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.LibraryGlideModule;

import java.io.InputStream;

// 使用 Java 以便现有 annotationProcessor 配置生成 Glide 模块索引。
@GlideModule
public final class ImageCacheGlideModule extends LibraryGlideModule {
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide,
                                   @NonNull Registry registry) {
        registry.append(CacheKeyImageModel.class, InputStream.class, new CacheKeyImageLoader.Factory());
    }
}
