package cc.shinichi.library.view.listener

import cc.shinichi.library.bean.ImageInfo
import com.devbrackets.android.exomedia.ui.widget.VideoView

/**
 * 仓库: <a href="http://git.xiaozhouintel.com/root/C-UI-App-Android">...</a>
 * 文件名: OnVideoLoadListener.java
 * 作者: kirito
 * 描述: 视频播放回调
 * 创建时间: 2024/11/25
 */
interface OnVideoLoadListener {

    fun onVideoLoad(info: ImageInfo, videoView: VideoView)
}