package cc.shinichi.library.tool.image;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;

import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.glide.FileTarget;
import cc.shinichi.library.tool.file.FileUtil;
import cc.shinichi.library.tool.file.SingleMediaScanner;
import cc.shinichi.library.tool.text.MD5Util;
import cc.shinichi.library.tool.ui.ToastUtil;

/**
 * @author 工藤
 * @email gougou@16fan.com
 * com.fan16.cn.util.picture
 * create at 2018/5/4  16:34
 * description:图片下载工具类
 */
public class DownloadPictureUtil {

    public static void downloadPicture(final Context context, final String url) {
        Glide.with(context).downloadOnly().load(url).into(new FileTarget() {
            @Override
            public void onLoadStarted(@Nullable Drawable placeholder) {
                super.onLoadStarted(placeholder);
                ToastUtil.getInstance()._short(context, "开始下载...");
                super.onLoadStarted(placeholder);
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                super.onLoadFailed(errorDrawable);
                ToastUtil.getInstance()._short(context, "保存失败");
            }

            @Override
            public void onResourceReady(@NonNull File resource, @Nullable Transition<? super File> transition) {
                super.onResourceReady(resource, transition);
                final String downloadFolderName = ImagePreview.getInstance().getFolderName();
                final String path = Environment.getExternalStorageDirectory() + "/" + downloadFolderName + "/";
                String name = "";
                try {
                    name = url.substring(url.lastIndexOf("/") + 1);
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
                        @Override
                        public void onScanFinish() {
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