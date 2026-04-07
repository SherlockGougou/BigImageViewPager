package cc.shinichi.library.video.media3

import android.app.Application
import android.content.Context
import android.view.View
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import cc.shinichi.library.core.GlobalContext
import cc.shinichi.library.video.VideoPlayerSession
import cc.shinichi.library.video.VideoRuntime
import java.io.File

/**
 * Media3 视频运行时实现。
 */
@OptIn(UnstableApi::class)
object Media3VideoRuntime : VideoRuntime {

    override val id: String = "media3"

    @Volatile
    internal var simpleCache: androidx.media3.datasource.cache.SimpleCache? = null

    override fun isPlaybackSupported(): Boolean {
        return try {
            Class.forName("androidx.media3.exoplayer.ExoPlayer")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    override fun initialize(application: Application) {
        ensureCache(application)
    }

    private fun ensureCache(context: Context) {
        if (simpleCache != null) return
        synchronized(this) {
            if (simpleCache != null) return
            try {
                val downloadDirectory = File(context.cacheDir, "media_cache")
                val maxBytes = 500 * 1024 * 1024L
                val databaseProvider = androidx.media3.database.StandaloneDatabaseProvider(context)
                val cache = androidx.media3.datasource.cache.SimpleCache(
                    downloadDirectory,
                    androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor(maxBytes),
                    databaseProvider
                )
                simpleCache = cache
                val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(
                    context,
                    androidx.media3.datasource.DefaultHttpDataSource.Factory()
                )
                val cacheDataSourceFactory =
                    androidx.media3.datasource.cache.CacheDataSource.Factory()
                        .setCache(cache)
                        .setUpstreamDataSourceFactory(dataSourceFactory)
                (context.applicationContext as? Application)?.let {
                    GlobalContext.init(it, cacheDataSourceFactory)
                }
            } catch (_: Exception) {
                simpleCache = null
                (context.applicationContext as? Application)?.let {
                    GlobalContext.init(it, null)
                }
            }
        }
    }

    override fun createPlayerView(context: Context): View? {
        return try {
            androidx.media3.ui.PlayerView(context).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
                controllerShowTimeoutMs = 1500
                runCatching {
                    val method = javaClass.getMethod("setControllerLayoutId", Int::class.javaPrimitiveType)
                    method.invoke(this, cc.shinichi.library.R.layout.sh_media_controller)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun createPlayerSession(
        context: Context,
        headers: Map<String, String>
    ): VideoPlayerSession? {
        return try {
            ensureCache(context.applicationContext)
            Media3VideoPlayerSession(context, headers)
        } catch (_: Exception) {
            null
        }
    }

    override fun hideController(playerView: View?) {
        (playerView as? androidx.media3.ui.PlayerView)?.hideController()
    }

    override fun releaseRuntimeResources() {
        synchronized(this) {
            runCatching {
                simpleCache?.release()
            }
            simpleCache = null
        }
    }
}

