package cc.shinichi.library.glide.progress;

/**
 * @author 工藤
 * @email gougou@16fan.com
 */
public interface OnProgressListener {
    void onProgress(String url, boolean isComplete, int percentage, long bytesRead, long totalBytes);
}