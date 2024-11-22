package cc.shinichi.library.tool.common

import android.os.Handler
import android.os.Looper
import android.os.Message
import java.lang.ref.WeakReference

/**
 * Handler相关工具类
 *
 *  实现 os.handler的callback接口
 *
 *  在需要处直接调用handler.sendmessage...即可
 * implements Callback
 * private HandlerUtils.HandlerHolder handlerHolder;
 * handlerHolder = new HandlerHolder(this);
 */
class HandlerHolder(listener: Callback?) : Handler(Looper.getMainLooper()) {

    private var mListenerWeakReference: WeakReference<Callback?>? = WeakReference(listener)

    override fun handleMessage(msg: Message) {
        mListenerWeakReference?.get()?.handleMessage(msg)
    }
}