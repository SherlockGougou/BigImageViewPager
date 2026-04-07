package cc.shinichi.library.video

import cc.shinichi.library.util.SLog

/**
 * 视频运行时注册中心。
 *
 * 默认尝试加载可选插件模块中的实现，失败则回退到 Noop。
 */
object VideoRuntimeRegistry {

    private const val TAG = "VideoRuntimeRegistry"
    private const val MEDIA3_RUNTIME_CLASS = "cc.shinichi.library.video.media3.Media3VideoRuntime"

    @Volatile
    private var injectedRuntime: VideoRuntime? = null

    @Volatile
    private var discoveredRuntime: VideoRuntime? = null

    @JvmStatic
    val runtime: VideoRuntime
        get() = injectedRuntime ?: discoveredRuntime ?: synchronized(this) {
            injectedRuntime ?: discoveredRuntime ?: discoverRuntime().also { discoveredRuntime = it }
        }

    @JvmStatic
    fun register(runtime: VideoRuntime) {
        injectedRuntime = runtime
        discoveredRuntime = null
        SLog.d(TAG, "Video runtime registered: ${runtime.id}")
    }

    private fun discoverRuntime(): VideoRuntime {
        return try {
            val clazz = Class.forName(MEDIA3_RUNTIME_CLASS)
            val instance = clazz.getField("INSTANCE").get(null)
            val runtime = instance as? VideoRuntime ?: NoopVideoRuntime
            SLog.d(TAG, "Video runtime discovered: ${runtime.id}")
            runtime
        } catch (e: Throwable) {
            SLog.d(TAG, "No external video runtime found, fallback to noop")
            NoopVideoRuntime
        }
    }
}


