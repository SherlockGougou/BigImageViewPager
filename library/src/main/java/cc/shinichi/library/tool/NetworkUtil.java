package cc.shinichi.library.tool;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import cc.shinichi.library.App;

/**
 * @author 工藤一号
 */
public class NetworkUtil {

  public static boolean isWiFi() {
    NetworkInfo info = getActiveNetworkInfo();
    if (info != null && info.isAvailable()) {
      return info.getType() == ConnectivityManager.TYPE_WIFI;
    }
    return false;
  }

  private static NetworkInfo getActiveNetworkInfo() {
    return ((ConnectivityManager) App.getAppContext()
        .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
  }
}