package cc.shinichi.library.tool.common

import android.text.TextUtils

object DeviceUtil {

    /**
     * 是否为鸿蒙系统
     *
     * @return true为鸿蒙系统
     */
    fun isHarmonyOs(): Boolean {
        return try {
            val buildExClass = Class.forName("com.huawei.system.BuildEx")
            val osBrand = buildExClass.getMethod("getOsBrand").invoke(buildExClass)
            osBrand.toString().contains("harmony", ignoreCase = true)
        } catch (x: Throwable) {
            false
        }
    }

    /**
     * 获取鸿蒙系统版本号
     *
     * @return 版本号
     */
    fun getHarmonyVersion(): String? {
        return getProp("hw_sc.build.os.version", "")
    }

    /**
     * 获取鸿蒙系统版本号
     * 鸿蒙2.0版本号为6
     * 鸿蒙3.0版本号为8
     * @return 版本号
     */
    fun getHarmonyVersionCode(): Int {
        return getProp("hw_sc.build.os.apiversion", "0")?.toInt() ?: 0
    }

    private fun getProp(property: String, defaultValue: String): String? {
        try {
            val spClz = Class.forName("android.os.SystemProperties")
            val method = spClz.getDeclaredMethod("get", String::class.java)
            val value = method.invoke(spClz, property) as String
            return if (TextUtils.isEmpty(value)) {
                defaultValue
            } else value
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return defaultValue
    }
}