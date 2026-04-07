package cc.shinichi.library.video

/**
 * 跨播放器实现的统一播放会话。
 */
interface VideoPlayerSession {

    interface Listener {
        fun onVideoSizeChanged(width: Int, height: Int)
        fun onIsPlayingChanged(isPlaying: Boolean)
        fun onPlaybackStateChanged(playbackState: Int)
    }

    fun bindPlayerView(playerView: android.view.View?)

    fun setMediaSource(url: String, isLocalFile: Boolean)

    fun setListener(listener: Listener)

    fun prepare()

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    fun isPlaying(): Boolean

    fun currentPosition(): Long

    fun duration(): Long

    fun release()

    companion object {
        const val STATE_IDLE = 1
        const val STATE_BUFFERING = 2
        const val STATE_READY = 3
        const val STATE_ENDED = 4
    }
}

