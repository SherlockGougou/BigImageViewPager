package cc.shinichi.library.tool.common;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * Handler相关工具类
 * <p> 实现 os.handler的callback接口
 * <p> 在需要处直接调用handler.sendmessage...即可
 * implements Callback
 * private HandlerUtils.HandlerHolder handlerHolder;
 * handlerHolder = new HandlerHolder(this);
 */
public final class HandlerUtils {

    private HandlerUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    public static class HandlerHolder extends Handler {
        WeakReference<Callback> mListenerWeakReference;

        /**
         * 使用必读：推荐在Activity或者Activity内部持有类中实现该接口，不要使用匿名类，可能会被GC
         *
         * @param listener 收到消息回调接口
         */
        public HandlerHolder(Callback listener) {
            mListenerWeakReference = new WeakReference<>(listener);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mListenerWeakReference != null && mListenerWeakReference.get() != null) {
                mListenerWeakReference.get().handleMessage(msg);
            }
        }
    }
}