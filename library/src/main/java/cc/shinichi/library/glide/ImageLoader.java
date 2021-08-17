package cc.shinichi.library.glide;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.disklrucache.DiskLruCache;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.signature.EmptySignature;

import java.io.File;
import java.lang.reflect.Constructor;

import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.glide.cache.DataCacheKey;
import cc.shinichi.library.glide.cache.SafeKeyGenerator;

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 * cc.shinichi.library.glide
 * create at 2018/5/21  15:22
 * description:
 */
public class ImageLoader {

    private final static String TAG = "ImageLoader";

    /**
     * 获取是否有某张原图的缓存
     * 缓存模式必须是：DiskCacheStrategy.SOURCE 才能获取到缓存文件
     */
    public static File getGlideCacheFile(Context context, String url) {
        try {
            DataCacheKey dataCacheKey;

            String customGlideUrlClzPath = ImagePreview.getInstance().getCustomGlideUrlClzPath();
            if (TextUtils.isEmpty(customGlideUrlClzPath)) {
                dataCacheKey = new DataCacheKey(new GlideUrl(url), EmptySignature.obtain());
            } else {
                try {
                    Class customGlideClz = Class.forName(customGlideUrlClzPath);
                    Constructor constructor = customGlideClz.getConstructor(String.class);
                    Object newInstance = constructor.newInstance(url);
                    dataCacheKey = new DataCacheKey((Key) newInstance, EmptySignature.obtain());
                } catch (Exception e) {
                    e.printStackTrace();
                    dataCacheKey = new DataCacheKey(new GlideUrl(url), EmptySignature.obtain());
                }
            }

            SafeKeyGenerator safeKeyGenerator = new SafeKeyGenerator();
            String safeKey = safeKeyGenerator.getSafeKey(dataCacheKey);
            Log.d(TAG, "safeKey = " + safeKey);
            File file = new File(context.getCacheDir(), DiskCache.Factory.DEFAULT_DISK_CACHE_DIR);
            DiskLruCache diskLruCache = DiskLruCache.open(file, 1, 1, DiskCache.Factory.DEFAULT_DISK_CACHE_SIZE);
            DiskLruCache.Value value = diskLruCache.get(safeKey);
            if (value != null) {
                return value.getFile(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void clearMemory(AppCompatActivity activity) {
        Glide.get(activity.getApplicationContext()).clearMemory();
    }

    public static void cleanDiskCache(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Glide.get(context.getApplicationContext()).clearDiskCache();
            }
        }).start();
    }
}