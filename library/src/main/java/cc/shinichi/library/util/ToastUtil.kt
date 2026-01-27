package cc.shinichi.library.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.lang.ref.WeakReference

/**
 * Toast 工具类
 *
 * @author 工藤
 * @email qinglingou@gmail.com
 */
object ToastUtil {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastToast: WeakReference<Toast>? = null

    /**
     * 显示短时 Toast
     */
    @JvmStatic
    fun showShort(context: Context, text: String?) {
        show(context, text, Toast.LENGTH_SHORT)
    }

    /**
     * 显示长时 Toast
     */
    @JvmStatic
    fun showLong(context: Context, text: String?) {
        show(context, text, Toast.LENGTH_LONG)
    }

    private fun show(context: Context, text: String?, duration: Int) {
        if (text.isNullOrEmpty()) return

        mainHandler.post {
            // 取消上一个 Toast，避免队列堆积
            lastToast?.get()?.cancel()

            val toast = Toast.makeText(context.applicationContext, text, duration)
            lastToast = WeakReference(toast)
            toast.show()
        }
    }

    /**
     * 兼容旧代码的 instance 属性
     */
    @JvmStatic
    val instance: ToastUtil
        get() = this
}