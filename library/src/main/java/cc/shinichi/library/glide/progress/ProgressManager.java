package cc.shinichi.library.glide.progress;

import android.text.TextUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import cc.shinichi.library.glide.SSLSocketClient;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author by sunfusheng on 2017/6/14.
 */
public class ProgressManager {

    private static Map<String, OnProgressListener> listenersMap =
            Collections.synchronizedMap(new HashMap<String, OnProgressListener>());

    private static final ProgressResponseBody.InternalProgressListener LISTENER =
            new ProgressResponseBody.InternalProgressListener() {
                @Override
                public void onProgress(String url, long bytesRead, long totalBytes) {
                    OnProgressListener onProgressListener = getProgressListener(url);
                    if (onProgressListener != null) {
                        int percentage = (int) ((bytesRead * 1f / totalBytes) * 100f);
                        boolean isComplete = percentage >= 100;
                        onProgressListener.onProgress(url, isComplete, percentage, bytesRead, totalBytes);
                        if (isComplete) {
                            removeListener(url);
                        }
                    }
                }
            };

    private ProgressManager() {

    }

    public static OkHttpClient getOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.addNetworkInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                Response response = chain.proceed(request);
                return response.newBuilder()
                        .body(new ProgressResponseBody(request.url().toString(), LISTENER, response.body()))
                        .build();
            }
        })
                .sslSocketFactory(SSLSocketClient.getSSLSocketFactory(), SSLSocketClient.geX509tTrustManager())
                .hostnameVerifier(SSLSocketClient.getHostnameVerifier());
        builder.connectTimeout(30, TimeUnit.SECONDS);
        builder.writeTimeout(30, TimeUnit.SECONDS);
        builder.readTimeout(30, TimeUnit.SECONDS);
        return builder.build();
    }

    public static void addListener(String url, OnProgressListener listener) {
        if (!TextUtils.isEmpty(url) && listener != null) {
            listenersMap.put(url, listener);
            listener.onProgress(url, false, 1, 0, 0);
        }
    }

    public static void removeListener(String url) {
        if (!TextUtils.isEmpty(url)) {
            listenersMap.remove(url);
        }
    }

    public static OnProgressListener getProgressListener(String url) {
        if (TextUtils.isEmpty(url) || listenersMap == null || listenersMap.size() == 0) {
            return null;
        }

        OnProgressListener listenerWeakReference = listenersMap.get(url);
        return listenerWeakReference;
    }
}