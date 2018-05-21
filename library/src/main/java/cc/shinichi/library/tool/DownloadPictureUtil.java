package cc.shinichi.library.tool;

import android.content.Context;
import cc.shinichi.library.glide.FileTarget;
import cc.shinichi.library.glide.ImageLoader;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import java.io.File;

/*
 * @author 工藤
 * @emil gougou@16fan.com
 * com.fan16.cn.util.picture
 * create at 2018/5/4  16:34
 * description:图片下载工具类
 */
public class DownloadPictureUtil {

	private static final String TAG = "DownloadPictureUtil";

	public static void downloadPicture(final Context context, final String url, final String path, final String name) {
		// 如果图片缓存存在，就直接复制缓存到指定目录即可
		File cache = ImageLoader.getGlideCacheFile(context, url);
		if (cache != null && cache.exists()) {
			boolean result = FileUtil.copyFile(cache, path, name);
			if (result) {
				ToastUtil.getInstance()._short(context, "成功保存到 ".concat(path).concat(name));
				new SingleMediaScanner(context, path, new SingleMediaScanner.ScanListener() {
					@Override public void onScanFinish() {
						// scanning...
					}
				});
			} else {
				ToastUtil.getInstance()._short(context, "保存失败");
			}
			return;
		}
		ToastUtil.getInstance()._short(context, "开始下载...");
		Glide.with(context.getApplicationContext()).load(url).downloadOnly(new FileTarget() {
			@Override public void onResourceReady(File resource, GlideAnimation<? super File> glideAnimation) {
				boolean result = FileUtil.copyFile(resource, path, name);
				if (result) {
					ToastUtil.getInstance()._short(context, "成功保存到 ".concat(path).concat(name));
					new SingleMediaScanner(context, path, new SingleMediaScanner.ScanListener() {
						@Override public void onScanFinish() {
							// scanning...
						}
					});
				} else {
					ToastUtil.getInstance()._short(context, "保存失败");
				}
			}
		});
	}
}