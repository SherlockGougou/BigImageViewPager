package cc.shinichi.library.video

import android.app.Application
import android.content.Context
import android.view.View
import cc.shinichi.library.core.GlobalContext

/**
 * 默认空实现：不提供视频播放能力。
 */
object NoopVideoRuntime : VideoRuntime {
    override val id: String = "noop"

    override fun isPlaybackSupported(): Boolean = false

    override fun initialize(application: Application) {
        GlobalContext.init(application, null)
    }

    override fun createPlayerView(context: Context): View? = null

    override fun createPlayerSession(
        context: Context,
        headers: Map<String, String>
    ): VideoPlayerSession? = null

    override fun hideController(playerView: View?) = Unit

    override fun releaseRuntimeResources() = Unit
}

