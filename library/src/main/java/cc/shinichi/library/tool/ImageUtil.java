package cc.shinichi.library.tool;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
}