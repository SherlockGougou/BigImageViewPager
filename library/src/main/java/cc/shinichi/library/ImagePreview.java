package cc.shinichi.library;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import cc.shinichi.library.bean.ImageInfo;
import cc.shinichi.library.view.ImagePreviewActivity;
import cc.shinichi.library.view.listener.OnBigImageClickListener;
import cc.shinichi.library.view.listener.OnBigImageLongClickListener;
import cc.shinichi.library.view.listener.OnBigImagePageChangeListener;
import cc.shinichi.library.view.listener.OnOriginProgressListener;

/**
 * @author 工藤
 * @email gougou@16fan.com
 * cc.shinichi.library
 * create at 2018/5/22  09:06
 * description:
 */
public class ImagePreview {

    @LayoutRes
    public static final int PROGRESS_THEME_CIRCLE_TEXT = R.layout.sh_default_progress_layout;
    // 触发双击的最短时间，小于这个时间的直接返回
    private static final int MIN_DOUBLE_CLICK_TIME = 1500;
    private WeakReference<Context> contextWeakReference;
    private List<ImageInfo> imageInfoList;// 图片数据集合
    private int index = 0;// 默认显示第几个
    private String folderName = "Download";// 下载到的文件夹名（根目录中）
    private float minScale = 1.0f;// 最小缩放倍数
    private float mediumScale = 3.0f;// 中等缩放倍数
    private float maxScale = 5.0f;// 最大缩放倍数

    private boolean isShowIndicator = true;// 是否显示图片指示器（1/9）
    private boolean isShowCloseButton = false;// 是否显示关闭页面按钮
    private boolean isShowDownButton = true;// 是否显示下载按钮
    private int zoomTransitionDuration = 200;// 动画持续时间 单位毫秒 ms

    private boolean isEnableDragClose = false;// 是否启用下拉关闭，默认不启用
    private boolean isEnableUpDragClose = false;// 是否启用上拉关闭，默认不启用
    private boolean isEnableClickClose = true;// 是否启用点击关闭，默认启用
    private boolean isShowErrorToast = false;// 是否在加载失败时显示toast

    private LoadStrategy loadStrategy = LoadStrategy.Default;// 加载策略

    @DrawableRes
    private int closeIconResId = R.drawable.ic_action_close;
    @DrawableRes
    private int downIconResId = R.drawable.icon_download_new;

    // 加载失败时的占位图
    @DrawableRes
    private int errorPlaceHolder = R.drawable.load_failed;

    // 点击和长按事件接口
    private OnBigImageClickListener bigImageClickListener;
    private OnBigImageLongClickListener bigImageLongClickListener;
    private OnBigImagePageChangeListener bigImagePageChangeListener;
    private OnOriginProgressListener onOriginProgressListener;

    // 自定义百分比布局layout id
    @LayoutRes
    private int progressLayoutId = -1;
    // 防止多次快速点击，记录上次打开的时间戳
    private long lastClickTime = 0;

    public static ImagePreview getInstance() {
        return InnerClass.instance;
    }

    public ImagePreview setContext(@NonNull Context context) {
        this.contextWeakReference = new WeakReference<>(context);
        return this;
    }

    public List<ImageInfo> getImageInfoList() {
        return imageInfoList;
    }

    public ImagePreview setImageInfoList(@NonNull List<ImageInfo> imageInfoList) {
        this.imageInfoList = imageInfoList;
        return this;
    }

    public ImagePreview setImageList(@NonNull List<String> imageList) {
        ImageInfo imageInfo;
        this.imageInfoList = new ArrayList<>();
        for (int i = 0; i < imageList.size(); i++) {
            imageInfo = new ImageInfo();
            imageInfo.setThumbnailUrl(imageList.get(i));
            imageInfo.setOriginUrl(imageList.get(i));
            this.imageInfoList.add(imageInfo);
        }
        return this;
    }

    public ImagePreview setImage(@NonNull String image) {
        this.imageInfoList = new ArrayList<>();
        ImageInfo imageInfo;
        imageInfo = new ImageInfo();
        imageInfo.setThumbnailUrl(image);
        imageInfo.setOriginUrl(image);
        this.imageInfoList.add(imageInfo);
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

    public boolean isShowCloseButton() {
        return isShowCloseButton;
    }

    public ImagePreview setShowCloseButton(boolean showCloseButton) {
        isShowCloseButton = showCloseButton;
        return this;
    }

    public boolean isShowOriginButton(int index) {
        List<ImageInfo> imageInfoList = getImageInfoList();
        if (null == imageInfoList || imageInfoList.size() == 0) {
            return false;
        }
        // 根据不同加载策略，自行判断是否显示查看原图按钮
        String originUrl = imageInfoList.get(index).getOriginUrl();
        String thumbUrl = imageInfoList.get(index).getThumbnailUrl();
        if (originUrl.equalsIgnoreCase(thumbUrl)) {// 原图、缩略图url一样，不显示查看原图按钮
            return false;
        }
        if (loadStrategy == LoadStrategy.Default) {
            return true;// 手动模式时，根据是否有原图缓存来决定是否显示查看原图按钮
        } else if (loadStrategy == LoadStrategy.NetworkAuto) {
            return false;// 强制隐藏查看原图按钮
        } else if (loadStrategy == LoadStrategy.AlwaysThumb) {
            return false;// 强制隐藏查看原图按钮
        } else if (loadStrategy == LoadStrategy.AlwaysOrigin) {
            return false;// 强制隐藏查看原图按钮
        } else {
            return false;
        }
    }

    /**
     * 不再有效，是否显示查看原图按钮，取决于加载策略，LoadStrategy，会自行判断是否显示。
     */
    @Deprecated
    public ImagePreview setShowOriginButton(boolean showOriginButton) {
        //isShowOriginButton = showOriginButton;
        return this;
    }

    public String getFolderName() {
        if (TextUtils.isEmpty(folderName)) {
            folderName = "Download";
        }
        return folderName;
    }

    public ImagePreview setFolderName(@NonNull String folderName) {
        this.folderName = folderName;
        return this;
    }

    /**
     * 当前版本不再支持本设置，双击会在最小和中等缩放值之间进行切换，可手动放大到最大。
     */
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

    @Deprecated
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

    public LoadStrategy getLoadStrategy() {
        return loadStrategy;
    }

    public ImagePreview setLoadStrategy(LoadStrategy loadStrategy) {
        this.loadStrategy = loadStrategy;
        return this;
    }

    public boolean isEnableDragClose() {
        return isEnableDragClose;
    }

    public ImagePreview setEnableDragClose(boolean enableDragClose) {
        isEnableDragClose = enableDragClose;
        return this;
    }

    public boolean isEnableUpDragClose() {
        return isEnableUpDragClose;
    }

    public ImagePreview setEnableUpDragClose(boolean enableUpDragClose) {
        isEnableUpDragClose = enableUpDragClose;
        return this;
    }

    public boolean isEnableClickClose() {
        return isEnableClickClose;
    }

    public ImagePreview setEnableClickClose(boolean enableClickClose) {
        isEnableClickClose = enableClickClose;
        return this;
    }

    public boolean isShowErrorToast() {
        return isShowErrorToast;
    }

    public ImagePreview setShowErrorToast(boolean showErrorToast) {
        isShowErrorToast = showErrorToast;
        return this;
    }

    public int getCloseIconResId() {
        return closeIconResId;
    }

    public ImagePreview setCloseIconResId(@DrawableRes int closeIconResId) {
        this.closeIconResId = closeIconResId;
        return this;
    }

    public int getDownIconResId() {
        return downIconResId;
    }

    public ImagePreview setDownIconResId(@DrawableRes int downIconResId) {
        this.downIconResId = downIconResId;
        return this;
    }

    public boolean isShowIndicator() {
        return isShowIndicator;
    }

    public ImagePreview setShowIndicator(boolean showIndicator) {
        isShowIndicator = showIndicator;
        return this;
    }

    public int getErrorPlaceHolder() {
        return errorPlaceHolder;
    }

    public ImagePreview setErrorPlaceHolder(int errorPlaceHolderResId) {
        this.errorPlaceHolder = errorPlaceHolderResId;
        return this;
    }

    public OnBigImageClickListener getBigImageClickListener() {
        return bigImageClickListener;
    }

    public ImagePreview setBigImageClickListener(OnBigImageClickListener bigImageClickListener) {
        this.bigImageClickListener = bigImageClickListener;
        return this;
    }

    public OnBigImageLongClickListener getBigImageLongClickListener() {
        return bigImageLongClickListener;
    }

    public ImagePreview setBigImageLongClickListener(OnBigImageLongClickListener bigImageLongClickListener) {
        this.bigImageLongClickListener = bigImageLongClickListener;
        return this;
    }

    public OnBigImagePageChangeListener getBigImagePageChangeListener() {
        return bigImagePageChangeListener;
    }

    public ImagePreview setBigImagePageChangeListener(OnBigImagePageChangeListener bigImagePageChangeListener) {
        this.bigImagePageChangeListener = bigImagePageChangeListener;
        return this;
    }

    public OnOriginProgressListener getOnOriginProgressListener() {
        return onOriginProgressListener;
    }

    private ImagePreview setOnOriginProgressListener(OnOriginProgressListener onOriginProgressListener) {
        this.onOriginProgressListener = onOriginProgressListener;
        return this;
    }

    public int getProgressLayoutId() {
        return progressLayoutId;
    }

    public ImagePreview setProgressLayoutId(int progressLayoutId, OnOriginProgressListener onOriginProgressListener) {
        setOnOriginProgressListener(onOriginProgressListener);
        this.progressLayoutId = progressLayoutId;
        return this;
    }

    public void reset() {
        imageInfoList = null;
        index = 0;
        minScale = 1.0f;
        mediumScale = 3.0f;
        maxScale = 5.0f;
        zoomTransitionDuration = 200;
        isShowDownButton = true;
        isShowCloseButton = false;
        isEnableDragClose = false;
        isEnableClickClose = true;
        isShowIndicator = true;
        isShowErrorToast = false;

        closeIconResId = R.drawable.ic_action_close;
        downIconResId = R.drawable.icon_download_new;
        errorPlaceHolder = R.drawable.load_failed;

        loadStrategy = LoadStrategy.Default;
        folderName = "Download";
        if (contextWeakReference != null) {
            contextWeakReference.clear();
            contextWeakReference = null;
        }

        bigImageClickListener = null;
        bigImageLongClickListener = null;
        bigImagePageChangeListener = null;

        progressLayoutId = -1;
        lastClickTime = 0;
    }

    public void start() {
        if (System.currentTimeMillis() - lastClickTime <= MIN_DOUBLE_CLICK_TIME) {
            Log.e("ImagePreview", "---忽略多次快速点击---");
            return;
        }
        if (contextWeakReference == null) {
            throw new IllegalArgumentException("You must call 'setContext(Context context)' first!");
        }
        Context context = contextWeakReference.get();
        if (context == null) {
            throw new IllegalArgumentException("You must call 'setContext(Context context)' first!");
        }
        if (!(context instanceof Activity)) {
            throw new IllegalArgumentException("context must be a Activity!");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (((Activity) context).isFinishing() || ((Activity) context).isDestroyed()) {
                reset();
                return;
            }
        } else {
            if (((Activity) context).isFinishing()) {
                reset();
                return;
            }
        }
        if (imageInfoList == null || imageInfoList.size() == 0) {
            throw new IllegalArgumentException(
                    "Do you forget to call 'setImageInfoList(List<ImageInfo> imageInfoList)' ?");
        }
        if (this.index >= imageInfoList.size()) {
            throw new IllegalArgumentException("index out of range!");
        }
        lastClickTime = System.currentTimeMillis();
        ImagePreviewActivity.activityStart(context);
    }

    public enum LoadStrategy {
        /**
         * 仅加载原图；会强制隐藏查看原图按钮
         */
        AlwaysOrigin,

        /**
         * 仅加载普清；会强制隐藏查看原图按钮
         */
        AlwaysThumb,

        /**
         * 根据网络自适应加载，WiFi原图，流量普清；会强制隐藏查看原图按钮
         */
        NetworkAuto,

        /**
         * 手动模式：默认普清，点击按钮再加载原图；会根据原图、缩略图url是否一样来判断是否显示查看原图按钮
         */
        Default
    }

    private static class InnerClass {
        private static ImagePreview instance = new ImagePreview();
    }
}