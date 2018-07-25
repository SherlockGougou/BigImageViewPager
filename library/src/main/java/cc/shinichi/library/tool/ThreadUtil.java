package cc.shinichi.library.tool;

import android.os.Looper;

/**
 * @author 工藤
 * @email gougou@16fan.com
 * cc.shinichi.library.tool
 * create at 2018/5/21  15:28
 * description:
 */
public class ThreadUtil {
  public ThreadUtil() {
  }

  public static boolean checkMainThread() {
    return Looper.myLooper() != null && Looper.myLooper() == Looper.getMainLooper();
  }
}