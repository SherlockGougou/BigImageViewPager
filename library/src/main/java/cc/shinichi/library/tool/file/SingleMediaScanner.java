package cc.shinichi.library.tool.file;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;

/**
 * @author 工藤
 * @email 18883840501@163.com
 * com.fan16.cn.util
 * create at 2018/5/4  16:50
 * description:媒体扫描
 */
public class SingleMediaScanner implements MediaScannerConnection.MediaScannerConnectionClient {

    private MediaScannerConnection mMs;
    private String path;
    private ScanListener listener;

    public SingleMediaScanner(Context context, String path, ScanListener l) {
        this.path = path;
        this.listener = l;
        this.mMs = new MediaScannerConnection(context, this);
        this.mMs.connect();
    }

    @Override
    public void onMediaScannerConnected() {
        mMs.scanFile(path, null);
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        mMs.disconnect();
        if (listener != null) {
            listener.onScanFinish();
        }
    }

    public interface ScanListener {
        void onScanFinish();
    }
}