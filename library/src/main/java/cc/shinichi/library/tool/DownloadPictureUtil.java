package cc.shinichi.library.tool;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import cc.shinichi.sherlockutillibrary.utility.file.FileUtil;
import cc.shinichi.sherlockutillibrary.utility.file.SingleMediaScanner;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import java.io.File;

/**
 * @author 工藤
 * @email gougou@16fan.com
 * com.fan16.cn.util.picture
 * create at 2018/5/4  16:34
 * description:图片下载工具类
 */
public class DownloadPictureUtil {

  public static void downloadPicture(final Context context, final String url, final String path,
      final String name) {
    MyToast.getInstance()._short(context, "开始下载...");

    SimpleTarget<File> target = new SimpleTarget<File>() {

      @Override public void onLoadFailed(@Nullable Drawable errorDrawable) {
        super.onLoadFailed(errorDrawable);
        MyToast.getInstance()._short(context, "保存失败");
      }

      @Override public void onResourceReady(@NonNull File resource,
          @Nullable Transition<? super File> transition) {
            boolean result = FileUtil.copyFile(resource, path, name);
            if (result) {
              MyToast.getInstance()._short(context, "成功保存到 ".concat(path).concat(name));
              new SingleMediaScanner(context, path.concat(name), new SingleMediaScanner.ScanListener() {
                @Override public void onScanFinish() {
                  // scanning...
                }
              });
            } else {
              MyToast.getInstance()._short(context, "保存失败");
            }
      }
    };
    Glide.with(context).downloadOnly().load(url).into(target);
  }
}