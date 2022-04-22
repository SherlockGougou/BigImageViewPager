package cc.shinichi.library.tool.common

import android.util.Log

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 */
object Print {
    private const val LOG_MAX_LENGTH = 2000

    fun d(TAG: String, msg: String?) {
        if (msg != null && "" != msg) {
            val strLength = msg.length
            var start = 0
            var end = LOG_MAX_LENGTH
            for (i in 0..99) {
                // 剩下的文本还是大于规定长度则继续重复截取并输出
                if (strLength > end) {
                    Log.d(TAG + i, msg.substring(start, end))
                    start = end
                    end += LOG_MAX_LENGTH
                } else {
                    Log.d(TAG, msg.substring(start, strLength))
                    break
                }
            }
        } else {
            Log.e(TAG, "msg == null")
        }
    }
}