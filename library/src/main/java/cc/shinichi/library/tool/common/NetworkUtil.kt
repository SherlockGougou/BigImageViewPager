package cc.shinichi.library.tool.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 */
object NetworkUtil {

    private val TAG = "NetworkUtil"

    fun isWiFi(context: Context): Boolean {
        val info = getActiveNetworkInfo(context)
        val isWifi = info != null && info.isAvailable && info.type == ConnectivityManager.TYPE_WIFI
        SLog.d(TAG, "isWiFi: $isWifi")
        return isWifi
    }

    private fun getActiveNetworkInfo(context: Context): NetworkInfo? {
        return (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
    }
}