package cc.shinichi.bigimageviewpager.glide;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

import cc.shinichi.library.glide.progress.ProgressManager;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;

@GlideModule
public class MyAppGlideModule extends AppGlideModule {
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        super.registerComponents(context, glide, registry);
        // 自定义client 可参考glide官方文档
        // 需要加入拦截器 ProgressManager提供的拦截器，用于监听下载进度
        Interceptor netWorkInterceptor = ProgressManager.getNetWorkInterceptor();
        new OkHttpClient.Builder()
                .addNetworkInterceptor(netWorkInterceptor)
                .build();
    }
}