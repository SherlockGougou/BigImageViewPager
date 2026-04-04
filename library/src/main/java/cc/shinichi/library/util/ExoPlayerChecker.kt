package cc.shinichi.library.util

/**
 * ExoPlayer 可用性检查工具
 *
 * 用于运行时检测用户是否引入了 ExoPlayer（Media3）依赖。
 * 如果用户未引入 ExoPlayer 依赖，视频播放功能将自动禁用，仅保留图片浏览功能。
 *
 * @author kirito
 */
object ExoPlayerChecker {

    private const val TAG = "ExoPlayerChecker"

    /**
     * 检查 ExoPlayer 是否可用
     */
    val isAvailable: Boolean by lazy {
        try {
            Class.forName("androidx.media3.exoplayer.ExoPlayer")
            SLog.d(TAG, "ExoPlayer is available")
            true
        } catch (e: ClassNotFoundException) {
            SLog.d(TAG, "ExoPlayer is not available, video playback will be disabled")
            false
        }
    }
}
