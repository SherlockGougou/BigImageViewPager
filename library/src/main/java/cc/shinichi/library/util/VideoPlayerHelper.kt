package cc.shinichi.library.util

import cc.shinichi.library.video.VideoRuntimeRegistry

/**
 * 视频播放能力检测工具
 *
 * 用于运行时检测 ExoPlayer (Media3) 是否可用
 * 当用户未添加 Media3 依赖时，视频播放功能将被自动禁用
 *
 * @author kirito
 */
object VideoPlayerHelper {

    private const val TAG = "VideoPlayerHelper"

    /**
     * 检查是否支持视频播放
     *
     * @return true 如果 ExoPlayer 可用，false 否则
     */
    @JvmStatic
    fun isVideoPlaybackSupported(): Boolean {
        val runtime = VideoRuntimeRegistry.runtime
        val supported = runtime.isPlaybackSupported()
        SLog.d(TAG, "Video runtime=${runtime.id}, supported=$supported")
        return supported
    }
}
