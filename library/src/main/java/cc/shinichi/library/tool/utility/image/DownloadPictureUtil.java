package cc.shinichi.library.tool.utility.image;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.tool.utility.file.FileUtil;
import cc.shinichi.library.tool.utility.file.SingleMediaScanner;
import cc.shinichi.library.tool.utility.text.MD5Util;
import cc.shinichi.library.tool.utility.ui.ToastUtil;
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

  public static void downloadPicture(final Context context, final String url) {
    SimpleTarget<File> target = new SimpleTarget<File>() {
      @Override public void onLoadStarted(@Nullable Drawable placeholder) {
        ToastUtil.getInstance()._short(context, "开始下载...");
        super.onLoadStarted(placeholder);
      }

      @Override public void onLoadFailed(@Nullable Drawable errorDrawable) {
        super.onLoadFailed(errorDrawable);
        ToastUtil.getInstance()._short(context, "保存失败");
      }

      @Override public void onResourceReady(@NonNull File resource,
          @Nullable Transition<? super File> transition) {
          final String downloadFolderName = ImagePreview.getInstance().getFolderName();
          final String path = Environment.getExternalStorageDirectory() + "/" + downloadFolderName + "/";
          String name = "";
          try {
              name = url.substring(url.lastIndexOf("/") + 1, url.length());
              if (name.contains(".")) {
                  name = name.substring(0, name.lastIndexOf("."));
              }
              name = MD5Util.md5Encode(name);
          } catch (Exception e) {
              e.printStackTrace();
              name = System.currentTimeMillis() + "";
          }
          String mimeType = ImageUtil.getImageTypeWithMime(resource.getAbsolutePath());
          name = name + "." + mimeType;
          FileUtil.createFileByDeleteOldFile(path + name);
          boolean result = FileUtil.copyFile(resource, path, name);
          if (result) {
              ToastUtil.getInstance()._short(context, "成功保存到 ".concat(path).concat(name));
              new SingleMediaScanner(context, path.concat(name), new SingleMediaScanner.ScanListener() {
                  @Override public void onScanFinish() {
                      // scanning...
                  }
              });
          } else {
              ToastUtil.getInstance()._short(context, "保存失败");
          }
      }
    };
    Glide.with(context).downloadOnly().load(url).into(target);
  }
}