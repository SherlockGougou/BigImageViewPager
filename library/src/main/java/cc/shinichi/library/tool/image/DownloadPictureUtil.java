package cc.shinichi.library.tool.image;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.transition.Transition;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.R;
import cc.shinichi.library.glide.FileTarget;
import cc.shinichi.library.tool.file.FileUtil;
import cc.shinichi.library.tool.file.SingleMediaScanner;
import cc.shinichi.library.tool.ui.ToastUtil;

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 * com.fan16.cn.util.picture
 * create at 2018/5/4  16:34
 * description:图片下载工具类
 */
public class DownloadPictureUtil {

    public static void downloadPicture(final Activity context, final int position, final String url) {
        Glide.with(context).downloadOnly().load(url).into(new FileTarget() {
            @Override
            public void onLoadStarted(@Nullable Drawable placeholder) {
                super.onLoadStarted(placeholder);
                if (ImagePreview.getInstance().getOnDownloadStateListener() != null) {
                    ImagePreview.getInstance().getOnDownloadStateListener().onDownloadStart(context, position);
                } else {
                    ToastUtil.getInstance()._short(context, context.getString(R.string.toast_start_download));
                }
                super.onLoadStarted(placeholder);
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                super.onLoadFailed(errorDrawable);
                if (ImagePreview.getInstance().getOnDownloadStateListener() != null) {
                    ImagePreview.getInstance().getOnDownloadStateListener().onDownloadFailed(context, position);
                } else {
                    ToastUtil.getInstance()._short(context, context.getString(R.string.toast_save_failed));
                }
            }

            @Override
            public void onResourceReady(@NonNull File resource, @Nullable Transition<? super File> transition) {
                super.onResourceReady(resource, transition);

                // 传入的保存文件夹名
                final String downloadFolderName = ImagePreview.getInstance().getFolderName();
                // 保存的图片名称
                String name = System.currentTimeMillis() + "";
                String mimeType = ImageUtil.getImageTypeWithMime(resource.getAbsolutePath());
                name = name + "." + mimeType;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // 大于等于29版本的保存方法
                    ContentResolver resolver = context.getContentResolver();
                    // 设置文件参数到ContentValues中
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
                    values.put(MediaStore.Images.Media.DESCRIPTION, name);
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/" + mimeType);
                    values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + downloadFolderName + "/");

                    Uri insertUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                    BufferedInputStream inputStream = null;
                    OutputStream os = null;
                    try {
                        inputStream = new BufferedInputStream(new FileInputStream(resource.getAbsolutePath()));
                        if (insertUri != null) {
                            os = resolver.openOutputStream(insertUri);
                        }
                        if (os != null) {
                            byte[] buffer = new byte[1024 * 4];
                            int len;
                            while ((len = inputStream.read(buffer)) != -1) {
                                os.write(buffer, 0, len);
                            }
                            os.flush();
                        }
                        if (ImagePreview.getInstance().getOnDownloadStateListener() != null) {
                            ImagePreview.getInstance().getOnDownloadStateListener().onDownloadSuccess(context, position);
                        } else {
                            ToastUtil.getInstance()._short(context, context.getString(R.string.toast_save_success, Environment.DIRECTORY_PICTURES + "/" + downloadFolderName));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        if (ImagePreview.getInstance().getOnDownloadStateListener() != null) {
                            ImagePreview.getInstance().getOnDownloadStateListener().onDownloadFailed(context, position);
                        } else {
                            ToastUtil.getInstance()._short(context, context.getString(R.string.toast_save_failed));
                        }
                    } finally {
                        try {
                            if (os != null) {
                                os.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            if (inputStream != null) {
                                inputStream.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    // 低于29版本的保存方法
                    final String path = Environment.getExternalStorageDirectory() + "/" + downloadFolderName + "/";

                    FileUtil.createFileByDeleteOldFile(path + name);
                    boolean result = FileUtil.copyFile(resource, path, name);
                    if (result) {
                        if (ImagePreview.getInstance().getOnDownloadStateListener() != null) {
                            ImagePreview.getInstance().getOnDownloadStateListener().onDownloadSuccess(context, position);
                        } else {
                            ToastUtil.getInstance()._short(context, context.getString(R.string.toast_save_success, path));
                        }
                        new SingleMediaScanner(context, path.concat(name), new SingleMediaScanner.ScanListener() {
                            @Override
                            public void onScanFinish() {
                                // scanning...
                            }
                        });
                    } else {
                        if (ImagePreview.getInstance().getOnDownloadStateListener() != null) {
                            ImagePreview.getInstance().getOnDownloadStateListener().onDownloadFailed(context, position);
                        } else {
                            ToastUtil.getInstance()._short(context, context.getString(R.string.toast_save_failed));
                        }
                    }
                }
            }
        });
    }
}