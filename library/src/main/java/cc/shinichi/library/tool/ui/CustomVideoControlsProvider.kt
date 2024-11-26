package cc.shinichi.library.tool.ui

import android.content.Context
import androidx.annotation.Keep
import com.devbrackets.android.exomedia.ui.widget.controls.VideoControls
import com.devbrackets.android.exomedia.ui.widget.controls.VideoControlsProvider

/**
 * 仓库: <a href="http://git.xiaozhouintel.com/root/C-UI-App-Android">...</a>
 * 文件名: CustomVideoControlsProvider.java
 * 作者: kirito
 * 描述: 视频控制器
 * 创建时间: 2024/11/26
 */
@Keep
open class CustomVideoControlsProvider : VideoControlsProvider() {

    @Keep
    override fun getControls(context: Context): VideoControls? {
        return CustomVideoControls(context)
    }
}