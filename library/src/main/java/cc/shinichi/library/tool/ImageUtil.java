package cc.shinichi.library.tool;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.support.media.ExifInterface;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import java.io.IOException;

/**
 * @author 工藤一号
 */
public class ImageUtil {

  private final static int LONG_IMAGE_RATIO = 3;

  public static int getOrientation(String imagePath) {
    int degree = 0;
    try {
      ExifInterface exifInterface = new ExifInterface(imagePath);
      int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
          ExifInterface.ORIENTATION_NORMAL);
      switch (orientation) {
        case ExifInterface.ORIENTATION_ROTATE_90:
          return SubsamplingScaleImageView.ORIENTATION_90;
        case ExifInterface.ORIENTATION_ROTATE_180:
          return SubsamplingScaleImageView.ORIENTATION_180;
        case ExifInterface.ORIENTATION_ROTATE_270:
          return SubsamplingScaleImageView.ORIENTATION_270;
        default:
          return SubsamplingScaleImageView.ORIENTATION_0;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return SubsamplingScaleImageView.ORIENTATION_0;
  }

  public static int[] getWidthHeight(String imagePath) {
    if (imagePath.isEmpty()) {
      return new int[] { 0, 0 };
    }
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    try {
      Bitmap originBitmap = BitmapFactory.decodeFile(imagePath, options);
    } catch (Exception e) {
      e.printStackTrace();
    }

    // 使用第一种方式获取原始图片的宽高
    int srcWidth = options.outWidth;
    int srcHeight = options.outHeight;

    // 使用第二种方式获取原始图片的宽高
    if (srcHeight == -1 || srcWidth == -1) {
      try {
        ExifInterface exifInterface = new ExifInterface(imagePath);
        srcHeight = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH,
            ExifInterface.ORIENTATION_NORMAL);
        srcWidth = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH,
            ExifInterface.ORIENTATION_NORMAL);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // 使用第三种方式获取原始图片的宽高
    if (srcWidth <= 0 || srcHeight <= 0) {
      Bitmap bitmap2 = BitmapFactory.decodeFile(imagePath);
      if (bitmap2 != null) {
        srcWidth = bitmap2.getWidth();
        srcHeight = bitmap2.getHeight();
        try {
          if (!bitmap2.isRecycled()) {
            bitmap2.recycle();
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return new int[] { srcWidth, srcHeight };
  }

  public static boolean isLongImage(String imagePath) {
    int[] wh = getWidthHeight(imagePath);
    return wh[0] > 0
        && wh[1] > 0
        && Math.max(wh[0], wh[1]) / Math.min(wh[0], wh[1]) >= LONG_IMAGE_RATIO;
  }

  public static Bitmap getImageBitmap(String srcPath, int degree) {
    boolean isOOM = false;
    BitmapFactory.Options newOpts = new BitmapFactory.Options();
    newOpts.inJustDecodeBounds = true;
    Bitmap bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
    newOpts.inJustDecodeBounds = false;
    float be = 1;
    newOpts.inSampleSize = (int) be;
    newOpts.inPreferredConfig = Bitmap.Config.RGB_565;
    newOpts.inDither = false;
    newOpts.inPurgeable = true;
    newOpts.inInputShareable = true;
    if (bitmap != null && !bitmap.isRecycled()) {
      bitmap.recycle();
    }
    try {
      bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
    } catch (OutOfMemoryError e) {
      isOOM = true;
      if (bitmap != null && !bitmap.isRecycled()) {
        bitmap.recycle();
      }
      Runtime.getRuntime().gc();
    } catch (Exception e) {
      isOOM = true;
      Runtime.getRuntime().gc();
    }
    if (isOOM) {
      try {
        bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
      } catch (Exception e) {
        newOpts.inPreferredConfig = Bitmap.Config.RGB_565;
        bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
      }
    }
    if (bitmap != null) {
      if (degree == 90) {
        degree += 180;
      }
      bitmap = rotateBitmapByDegree(bitmap, degree);
      int ttHeight = (1080 * bitmap.getHeight() / bitmap.getWidth());
      if (bitmap.getWidth() >= 1080) {
        bitmap = zoomBitmap(bitmap, 1080, ttHeight);
      }
    }
    return bitmap;
  }

  private static Bitmap rotateBitmapByDegree(Bitmap bm, int degree) {
    Bitmap returnBm = null;
    Matrix matrix = new Matrix();
    matrix.postRotate(degree);
    try {
      returnBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
    } catch (OutOfMemoryError e) {
      e.printStackTrace();
    }
    if (returnBm == null) {
      returnBm = bm;
    }
    if (bm != returnBm) {
      bm.recycle();
    }
    return returnBm;
  }

  private static Bitmap zoomBitmap(Bitmap bitmap, int width, int height) {
    int w = bitmap.getWidth();
    int h = bitmap.getHeight();
    Matrix matrix = new Matrix();
    float scaleWidth = ((float) width / w);
    float scaleHeight = ((float) height / h);
    matrix.postScale(scaleWidth, scaleHeight);
    return Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
  }

  public static int getBitmapDegree(String path) {
    int degree = 0;
    try {
      // 从指定路径下读取图片，并获取其EXIF信息
      ExifInterface exifInterface = new ExifInterface(path);
      // 获取图片的旋转信息
      int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
          ExifInterface.ORIENTATION_NORMAL);
      switch (orientation) {
        case ExifInterface.ORIENTATION_ROTATE_90:
          degree = 90;
          break;
        case ExifInterface.ORIENTATION_ROTATE_180:
          degree = 180;
          break;
        case ExifInterface.ORIENTATION_ROTATE_270:
          degree = 270;
          break;
        default:
          degree = 0;
          break;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return degree;
  }
}