package cc.shinichi.library.tool.common;

import android.text.TextUtils;
import android.util.Log;

/**
 * @author SherlockHolmes
 */
public class Print {
    public static void d(String TAG, String msg) {

        int LOG_MAXLENGTH = 2000;
        if (TextUtils.isEmpty(TAG)) {
            TAG = "shinichi.cc";
        }
        if (msg != null && !"".equals(msg)) {
            int strLength = msg.length();
            int start = 0;
            int end = LOG_MAXLENGTH;
            for (int i = 0; i < 100; i++) {
                // 剩下的文本还是大于规定长度则继续重复截取并输出
                if (strLength > end) {
                    Log.d(TAG + i, msg.substring(start, end));
                    start = end;
                    end = end + LOG_MAXLENGTH;
                } else {
                    Log.d(TAG, msg.substring(start, strLength));
                    break;
                }
            }
        } else {
            Log.e(TAG, "msg == null");
        }
    }
}