package cc.shinichi.library.tool.utility.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * @author SherlockHolmes
 */
public class MyToast {

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private Toast toast;

    public MyToast() {

    }

    public static MyToast getInstance() {
        return InnerClass.instance;
    }

    public void _short(final Context context, final String text) {
        HANDLER.post(new Runnable() {
            @Override public void run() {
                if (toast == null) {
                    toast = Toast.makeText(context.getApplicationContext(), text, Toast.LENGTH_SHORT);
                }
                toast.setText(text);
                toast.show();
            }
        });
    }

    public void _long(final Context context, final String text) {
        HANDLER.post(new Runnable() {
            @Override public void run() {
                if (toast == null) {
                    toast = Toast.makeText(context.getApplicationContext(), text, Toast.LENGTH_LONG);
                }
                toast.setText(text);
                toast.show();
            }
        });
    }

    private static class InnerClass {
        private static MyToast instance = new MyToast();
    }
}