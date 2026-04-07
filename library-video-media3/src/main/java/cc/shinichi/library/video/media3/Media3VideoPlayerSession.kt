package cc.shinichi.library.video.media3

import android.content.Context
import android.net.Uri
import android.view.View
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import cc.shinichi.library.video.VideoPlayerSession
import java.io.File

@OptIn(UnstableApi::class)
internal class Media3VideoPlayerSession(
    private val context: Context,
    private val headers: Map<String, String>
) : VideoPlayerSession {

    private val player: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(buildMediaSourceFactory())
            .build()
    }

    private var listener: VideoPlayerSession.Listener? = null

    override fun bindPlayerView(playerView: View?) {
        (playerView as? PlayerView)?.player = player
    }

    override fun setMediaSource(url: String, isLocalFile: Boolean) {
        if (isLocalFile) {
            val mediaItem = MediaItem.fromUri(Uri.fromFile(File(url)))
            val dataSourceFactory = DefaultDataSource.Factory(context)
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            player.setMediaSource(mediaSource)
        } else {
            player.setMediaItem(MediaItem.fromUri(url))
        }
    }

    override fun setListener(listener: VideoPlayerSession.Listener) {
        this.listener = listener
        player.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                listener.onVideoSizeChanged(videoSize.width, videoSize.height)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                listener.onIsPlayingChanged(isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                listener.onPlaybackStateChanged(playbackState)
            }
        })
    }

    override fun prepare() {
        player.prepare()
        player.playWhenReady = false
    }

    override fun play() = player.play()

    override fun pause() = player.pause()

    override fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    override fun isPlaying(): Boolean = player.isPlaying

    override fun currentPosition(): Long = player.currentPosition

    override fun duration(): Long = if (player.duration < 0) 0 else player.duration

    override fun release() {
        player.release()
    }

    private fun buildMediaSourceFactory(): DefaultMediaSourceFactory {
        val cache = Media3VideoRuntime.simpleCache
        if (cache == null) {
            val httpFactory = DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(10_000)
                .setReadTimeoutMs(10_000)
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(headers)
            return DefaultMediaSourceFactory(httpFactory)
        }

        val httpFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(10_000)
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(headers)

        val cacheFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpFactory)
            .setCacheWriteDataSinkFactory(CacheDataSink.Factory().setCache(cache))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        return DefaultMediaSourceFactory(cacheFactory)
    }
}


