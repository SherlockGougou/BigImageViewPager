package cc.shinichi.library.tool.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

import cc.shinichi.library.tool.common.Print;
import cc.shinichi.library.tool.ui.PhoneUtil;

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 * cc.shinichi.drawlongpicturedemo.util
 * create at 2018/8/28  10:46
 * description:
 */
public class ImageUtil {

    private static final String TAG = "ImageUtil";

    public static int getBitmapDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation =
                    exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
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

    public static Bitmap rotateBitmapByDegree(Bitmap bm, int degree) {
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

    public static int getOrientation(String imagePath) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(imagePath);
            int orientation =
                    exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int[] getWidthHeight(String imagePath) {
        if (imagePath.isEmpty()) {
            return new int[]{0, 0};
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
                srcHeight =
                        exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, ExifInterface.ORIENTATION_NORMAL);
                srcWidth =
                        exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, ExifInterface.ORIENTATION_NORMAL);
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
        int orient = getOrientation(imagePath);
        if (orient == 90 || orient == 270) {
            return new int[]{srcHeight, srcWidth};
        }
        return new int[]{srcWidth, srcHeight};
    }

    public static boolean isLongImage(Context context, String imagePath) {
        int[] wh = getWidthHeight(imagePath);
        float w = wh[0];
        float h = wh[1];
        float imageRatio = (h / w);
        float phoneRatio = PhoneUtil.getPhoneRatio(context.getApplicationContext()) + 0.1F;
        boolean isLongImage = (w > 0 && h > 0) && (h > w) && (imageRatio >= phoneRatio);
        Print.d(TAG, "isLongImage = " + isLongImage);
        return isLongImage;
    }

    public static boolean isWideImage(Context context, String imagePath) {
        int[] wh = getWidthHeight(imagePath);
        float w = wh[0];
        float h = wh[1];
        float imageRatio = (w / h);
        //float phoneRatio = PhoneUtil.getPhoneRatio(context.getApplicationContext()) + 0.1F;
        boolean isWideImage = (w > 0 && h > 0) && (w > h) && (imageRatio >= 2);
        Print.d(TAG, "isWideImage = " + isWideImage);
        return isWideImage;
    }

    public static boolean isSmallImage(Context context, String imagePath) {
        int[] wh = getWidthHeight(imagePath);
        boolean isSmallImage = wh[0] < PhoneUtil.getPhoneWid(context.getApplicationContext());
        Print.d(TAG, "isSmallImage = " + isSmallImage);
        return isSmallImage;
    }

    public static float getLongImageMinScale(Context context, String imagePath) {
        int[] wh = getWidthHeight(imagePath);
        float imageWid = wh[0];
        float phoneWid = PhoneUtil.getPhoneWid(context.getApplicationContext());
        return phoneWid / imageWid;
    }

    public static float getLongImageMaxScale(Context context, String imagePath) {
        return getLongImageMinScale(context, imagePath) * 2;
    }

    public static float getWideImageDoubleScale(Context context, String imagePath) {
        int[] wh = getWidthHeight(imagePath);
        float imageHei = wh[1];
        float phoneHei = PhoneUtil.getPhoneHei(context.getApplicationContext());
        return phoneHei / imageHei;
    }

    public static float getSmallImageMinScale(Context context, String imagePath) {
        int[] wh = getWidthHeight(imagePath);
        float imageWid = wh[0];
        float phoneWid = PhoneUtil.getPhoneWid(context.getApplicationContext());
        return phoneWid / imageWid;
    }

    public static float getSmallImageMaxScale(Context context, String imagePath) {
        int[] wh = getWidthHeight(imagePath);
        float imageWid = wh[0];
        float phoneWid = PhoneUtil.getPhoneWid(context.getApplicationContext());
        return phoneWid * 2 / imageWid;
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

    private static Bitmap zoomBitmap(Bitmap bitmap, int width, int height) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidth = ((float) width / w);
        float scaleHeight = ((float) height / h);
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
    }

    public static String getImageTypeWithMime(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        String type = options.outMimeType;
        Log.d(TAG, "getImageTypeWithMime: type1 = " + type);
        // ”image/png”、”image/jpeg”、”image/gif”
        if (TextUtils.isEmpty(type)) {
            type = "";
        } else {
            type = type.substring(6);
        }
        Log.d(TAG, "getImageTypeWithMime: type2 = " + type);
        return type;
    }

    public static boolean isPngImageWithMime(String url, String path) {
        return "png".equalsIgnoreCase(getImageTypeWithMime(path)) || url.toLowerCase().endsWith("png");
    }

    public static boolean isJpegImageWithMime(String url, String path) {
        return "jpeg".equalsIgnoreCase(getImageTypeWithMime(path)) || "jpg".equalsIgnoreCase(getImageTypeWithMime(path))
                || url.toLowerCase().endsWith("jpeg") || url.toLowerCase().endsWith("jpg");
    }

    public static boolean isBmpImageWithMime(String url, String path) {
        return "bmp".equalsIgnoreCase(getImageTypeWithMime(path)) || url.toLowerCase().endsWith("bmp");
    }

    public static boolean isGifImageWithMime(String url, String path) {
        return "gif".equalsIgnoreCase(getImageTypeWithMime(path)) || url.toLowerCase().endsWith("gif");
    }

    public static boolean isWebpImageWithMime(String url, String path) {
        return "webp".equalsIgnoreCase(getImageTypeWithMime(path)) || url.toLowerCase().endsWith("webp");
    }

    public static boolean isStandardImage(String url, String path) {
        return isJpegImageWithMime(url, path) || isPngImageWithMime(url, path) || isBmpImageWithMime(url, path);
    }
}