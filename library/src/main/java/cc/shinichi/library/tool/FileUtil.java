package cc.shinichi.library.tool;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * @author 工藤
 * @email gougou@16fan.com
 * cc.shinichi.library.tool
 * create at 2018/5/21  15:24
 * description:文件工具类
 */
public class FileUtil {
  /**
   * 根据文件路径拷贝文件
   *
   * @param resourceFile 源文件
   * @param targetPath 目标路径（包含文件名和文件格式）
   * @return boolean 成功true、失败false
   */
  public static boolean copyFile(File resourceFile, String targetPath, String fileName) {
    boolean result = false;
    if (resourceFile == null || TextUtils.isEmpty(targetPath)) {
      return result;
    }
    File target = new File(targetPath);
    if (target.exists()) {
      target.delete(); // 已存在的话先删除
    } else {
      try {
        target.mkdirs();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    File targetFile = new File(targetPath.concat(fileName));
    if (targetFile.exists()) {
      targetFile.delete();
    } else {
      try {
        targetFile.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    FileChannel resourceChannel = null;
    FileChannel targetChannel = null;
    try {
      resourceChannel = new FileInputStream(resourceFile).getChannel();
      targetChannel = new FileOutputStream(targetFile).getChannel();
      resourceChannel.transferTo(0, resourceChannel.size(), targetChannel);
      result = true;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return result;
    } catch (IOException e) {
      e.printStackTrace();
      return result;
    }
    try {
      resourceChannel.close();
      targetChannel.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  /**
   * 获取缓存路径 不存在自动创建
   *
   * @param uniqueName 独一无二的文件夹名(如：img_cache_glide)
   */
  public static File getDiskCacheDir(Context context, String uniqueName) {
    String cachePath;
    File file;
    try {
      if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
          || !Environment.isExternalStorageRemovable()) {
        if (context.getExternalCacheDir() != null) {
          cachePath = context.getExternalCacheDir().toString();
        } else {
          cachePath = context.getCacheDir().toString();
        }
      } else {
        cachePath = context.getCacheDir().toString();
      }
      file = new File(cachePath + File.separator + uniqueName);
      if (!file.exists()) {
        file.mkdirs();
      }
      return file;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}