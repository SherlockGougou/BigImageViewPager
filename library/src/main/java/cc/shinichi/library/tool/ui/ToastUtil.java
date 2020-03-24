package cc.shinichi.library.tool.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * @author 工藤
 * @email 18883840501@163.com
 */
public class ToastUtil {

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    public ToastUtil() {

    }

    public static ToastUtil getInstance() {
        return InnerClass.instance;
    }

    public void _short(final Context context, final String text) {
        HANDLER.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context.getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void _long(final Context context, final String text) {
        HANDLER.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context.getApplicationContext(), text, Toast.LENGTH_LONG).show();
            }
        });
    }

    private static class InnerClass {
        private static ToastUtil instance = new ToastUtil();
    }
}