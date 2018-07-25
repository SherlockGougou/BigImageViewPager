package cc.shinichi.library.tool;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * @author 工藤
 * @email gougou@16fan.com
 * cc.shinichi.library.tool
 * create at 2018/5/21  16:31
 * description:
 */
public class ToastUtil {

  private static final Handler HANDLER = new Handler(Looper.getMainLooper());
  private Toast toast;

  public ToastUtil() {

  }

  public static ToastUtil getInstance() {
    return InnerClass.instance;
  }

  public void _short(final Context context, final String text) {
    HANDLER.post(new Runnable() {
      @Override public void run() {
        if (toast == null) {
          toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
          toast.show();
        } else {
          toast.setText(text);
          toast.show();
        }
      }
    });
  }

  public void _long(final Context context, final String text) {
    HANDLER.post(new Runnable() {
      @Override public void run() {
        if (toast == null) {
          toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
          toast.show();
        } else {
          toast.setText(text);
          toast.show();
        }
      }
    });
  }

  private static class InnerClass {
    private static ToastUtil instance = new ToastUtil();
  }
}