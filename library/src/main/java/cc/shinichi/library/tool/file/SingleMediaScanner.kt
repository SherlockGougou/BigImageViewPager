package cc.shinichi.library.tool.file

import android.content.Context
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.MediaScannerConnectionClient
import android.net.Uri

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 * create at 2018/5/4  16:50
 * description:媒体扫描
 */
class SingleMediaScanner(context: Context?, private val path: String, private val listener: ScanListener?) :
    MediaScannerConnectionClient {

    private val mMs: MediaScannerConnection = MediaScannerConnection(context, this)

    override fun onMediaScannerConnected() {
        mMs.scanFile(path, null)
    }

    override fun onScanCompleted(path: String, uri: Uri) {
        mMs.disconnect()
        listener?.onScanFinish()
    }

    interface ScanListener {
        fun onScanFinish()
    }

    init {
        mMs.connect()
    }
}