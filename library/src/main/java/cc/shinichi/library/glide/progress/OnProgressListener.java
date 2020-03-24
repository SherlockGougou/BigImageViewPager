package cc.shinichi.library.glide.progress;

/**
 * @author 工藤
 * @email 18883840501@163.com
 */
public interface OnProgressListener {
    void onProgress(String url, boolean isComplete, int percentage, long bytesRead, long totalBytes);
}