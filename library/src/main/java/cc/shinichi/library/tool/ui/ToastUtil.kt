package cc.shinichi.library.tool.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 */
class ToastUtil {

    fun showShort(context: Context, text: String?) {
        HANDLER.post { Toast.makeText(context.applicationContext, text, Toast.LENGTH_SHORT).show() }
    }

    fun showLong(context: Context, text: String?) {
        HANDLER.post { Toast.makeText(context.applicationContext, text, Toast.LENGTH_LONG).show() }
    }

    private object InnerClass {
        val instance = ToastUtil()
    }

    companion object {
        private val HANDLER = Handler(Looper.getMainLooper())

        @JvmStatic
        val instance: ToastUtil
            get() = InnerClass.instance
    }
}