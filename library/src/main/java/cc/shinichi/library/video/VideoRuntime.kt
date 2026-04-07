package cc.shinichi.library.video

import android.app.Application
import android.content.Context
import android.view.View

/**
 * 可选视频运行时能力抽象。
 *
 * 核心库只依赖该接口，不直接依赖具体播放器实现。
 */
interface VideoRuntime {

    /**
     * 运行时标识，用于日志排查。
     */
    val id: String

    /**
     * 当前运行环境是否支持视频播放。
     */
    fun isPlaybackSupported(): Boolean

    /**
     * 库初始化入口。
     */
    fun initialize(application: Application)

    /**
     * 创建播放器容器视图。
     */
    fun createPlayerView(context: Context): View?

    /**
     * 创建播放器会话。
     */
    fun createPlayerSession(context: Context, headers: Map<String, String>): VideoPlayerSession?

    /**
     * 隐藏播放器内置控制器。
     */
    fun hideController(playerView: View?)

    /**
     * 释放运行时资源。
     */
    fun releaseRuntimeResources()
}

