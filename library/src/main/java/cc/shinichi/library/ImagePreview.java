package cc.shinichi.library;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import cc.shinichi.library.bean.ImageInfo;
import cc.shinichi.library.view.ImagePreviewActivity;
import java.util.List;

/**
 * @author 工藤
 * @email gougou@16fan.com
 * cc.shinichi.library
 * create at 2018/5/22  09:06
 * description:
 */
public class ImagePreview {

  public static final int MODE_SCALE_TO_MEDIUM_TO_MAX_TO_MIN = 1001;// 三级放大
  public static final int MODE_SCALE_TO_MAX_TO_MIN = 1002;// 二级放大，最大与最小
  public static final int MODE_SCALE_TO_MEDIUM_TO_MIN = 1003;// 二级放大，中等与最小

  private Context context;
  private List<ImageInfo> imageInfoList;// 图片数据集合
  private int index = 0;// 默认显示第几个
  private boolean isShowDownButton = true;// 是否显示下载按钮
  private boolean isShowOriginButton = true;// 是否显示查看原图按钮
  private String folderName = "ImagePreview";// 下载到的文件夹名（根目录中）
  private float minScale = 1.0f;// 最小缩放倍数
  private float mediumScale = 3.0f;// 中等缩放倍数
  private float maxScale = 5.0f;// 最大缩放倍数

  private int zoomTransitionDuration = 200;// 动画持续时间 单位毫秒 ms

  public static ImagePreview getInstance() {
    return InnerClass.instance;
  }

  public Context getContext() {
    return context;
  }

  public ImagePreview setContext(@NonNull Context context) {
    this.context = context;
    return this;
  }

  public List<ImageInfo> getImageInfoList() {
    return imageInfoList;
  }

  public ImagePreview setImageInfoList(@NonNull List<ImageInfo> imageInfoList) {
    this.imageInfoList = imageInfoList;
    return this;
  }

  public int getIndex() {
    return index;
  }

  public ImagePreview setIndex(int index) {
    this.index = index;
    return this;
  }

  public boolean isShowDownButton() {
    return isShowDownButton;
  }

  public ImagePreview setShowDownButton(boolean showDownButton) {
    isShowDownButton = showDownButton;
    return this;
  }

  public boolean isShowOriginButton() {
    return isShowOriginButton;
  }

  public ImagePreview setShowOriginButton(boolean showOriginButton) {
    isShowOriginButton = showOriginButton;
    return this;
  }

  public String getFolderName() {
    if (TextUtils.isEmpty(folderName)) {
      folderName = "BigImageViewDownload";
    }
    return folderName;
  }

  public ImagePreview setFolderName(@NonNull String folderName) {
    this.folderName = folderName;
    return this;
  }

  @Deprecated
  public ImagePreview setScaleMode(int scaleMode) {
    //if (scaleMode != MODE_SCALE_TO_MAX_TO_MIN
    //	&& scaleMode != MODE_SCALE_TO_MEDIUM_TO_MAX_TO_MIN
    //	&& scaleMode != MODE_SCALE_TO_MEDIUM_TO_MIN) {
    //	throw new IllegalArgumentException("only can use one of( MODE_SCALE_TO_MAX_TO_MIN、MODE_SCALE_TO_MEDIUM_TO_MAX_TO_MIN、MODE_SCALE_TO_MEDIUM_TO_MIN )");
    //}
    //this.scaleMode = scaleMode;
    return this;
  }

  public ImagePreview setScaleLevel(int min, int medium, int max) {
    if (max > medium && medium > min && min > 0) {
      this.minScale = min;
      this.mediumScale = medium;
      this.maxScale = max;
    } else {
      throw new IllegalArgumentException("max must greater to medium, medium must greater to min!");
    }
    return this;
  }

  public float getMinScale() {
    return minScale;
  }

  public float getMediumScale() {
    return mediumScale;
  }

  public float getMaxScale() {
    return maxScale;
  }

  public int getZoomTransitionDuration() {
    return zoomTransitionDuration;
  }

  public ImagePreview setZoomTransitionDuration(int zoomTransitionDuration) {
    if (zoomTransitionDuration < 0) {
      throw new IllegalArgumentException("zoomTransitionDuration must greater 0");
    }
    this.zoomTransitionDuration = zoomTransitionDuration;
    return this;
  }

  public void reset() {
    imageInfoList = null;
    index = 0;
    isShowDownButton = true;
    isShowOriginButton = true;
    folderName = "ImagePreview";
  }

  public void start() {
    if (context == null) {
      throw new IllegalArgumentException("You must call 'setContext(Context context)' first!");
    }
    if (imageInfoList == null || imageInfoList.size() == 0) {
      throw new IllegalArgumentException(
          "Do you forget to call 'setImageInfoList(List<ImageInfo> imageInfoList)' ?");
    }
    if (this.index >= imageInfoList.size()) {
      throw new IllegalArgumentException("index out of range!");
    }
    Intent intent = new Intent();
    intent.setClass(context, ImagePreviewActivity.class);
    context.startActivity(intent);
  }

  public static class InnerClass {
    private static ImagePreview instance = new ImagePreview();
  }
}