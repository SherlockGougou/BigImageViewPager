package cc.shinichi.library.view;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.R;
import cc.shinichi.library.bean.ImageInfo;
import cc.shinichi.library.glide.FileTarget;
import cc.shinichi.library.glide.ImageLoader;
import cc.shinichi.library.tool.common.HttpUtil;
import cc.shinichi.library.tool.common.NetworkUtil;
import cc.shinichi.library.tool.file.FileUtil;
import cc.shinichi.library.tool.image.ImageUtil;
import cc.shinichi.library.tool.ui.PhoneUtil;
import cc.shinichi.library.tool.ui.ToastUtil;
import cc.shinichi.library.view.helper.FingerDragHelper;
import cc.shinichi.library.view.helper.ImageSource;
import cc.shinichi.library.view.helper.SubsamplingScaleImageViewDragClose;
import cc.shinichi.library.view.listener.SimpleOnImageEventListener;
import cc.shinichi.library.view.photoview.PhotoView;

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 */
public class ImagePreviewAdapter extends PagerAdapter {

    private static final String TAG = "ImagePreview";
    private final AppCompatActivity activity;
    private final List<ImageInfo> imageInfo;
    private final HashMap<String, SubsamplingScaleImageViewDragClose> imageHashMap = new HashMap<>();
    private final HashMap<String, PhotoView> imageGifHashMap = new HashMap<>();
    private String finalLoadUrl = "";

    public ImagePreviewAdapter(AppCompatActivity activity, @NonNull List<ImageInfo> imageInfo) {
        super();
        this.imageInfo = imageInfo;
        this.activity = activity;
    }

    public void closePage() {
        try {
            if (imageHashMap != null && imageHashMap.size() > 0) {
                for (Object o : imageHashMap.entrySet()) {
                    Map.Entry entry = (Map.Entry) o;
                    if (entry != null && entry.getValue() != null) {
                        ((SubsamplingScaleImageViewDragClose) entry.getValue()).destroyDrawingCache();
                        ((SubsamplingScaleImageViewDragClose) entry.getValue()).recycle();
                    }
                }
                imageHashMap.clear();
            }
            if (imageGifHashMap != null && imageGifHashMap.size() > 0) {
                for (Object o : imageGifHashMap.entrySet()) {
                    Map.Entry entry = (Map.Entry) o;
                    if (entry != null && entry.getValue() != null) {
                        ((PhotoView) entry.getValue()).destroyDrawingCache();
                        ((PhotoView) entry.getValue()).setImageBitmap(null);
                    }
                }
                imageGifHashMap.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getCount() {
        return imageInfo.size();
    }

    /**
     * 加载原图
     */
    public void loadOrigin(final ImageInfo imageInfo) {
        String originalUrl = imageInfo.getOriginUrl();
        if (imageHashMap == null || imageGifHashMap == null) {
            notifyDataSetChanged();
            return;
        }
        if (imageHashMap.get(originalUrl) != null && imageGifHashMap.get(originalUrl) != null) {
            final SubsamplingScaleImageViewDragClose imageView = imageHashMap.get(imageInfo.getOriginUrl());
            final PhotoView imageGif = imageGifHashMap.get(imageInfo.getOriginUrl());

            File cacheFile = ImageLoader.getGlideCacheFile(activity, imageInfo.getOriginUrl());
            if (cacheFile != null && cacheFile.exists()) {
                boolean isCacheIsGif = ImageUtil.isGifImageWithMime(originalUrl, cacheFile.getAbsolutePath());
                if (isCacheIsGif) {
                    if (imageView != null) {
                        imageView.setVisibility(View.GONE);
                    }
                    if (imageGif != null) {
                        imageGif.setVisibility(View.VISIBLE);
                        Glide.with(activity)
                                .asGif()
                                .load(cacheFile)
                                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                        .error(ImagePreview.getInstance().getErrorPlaceHolder()))
                                .into(imageGif);
                    }
                } else {
                    if (imageGif != null) {
                        imageGif.setVisibility(View.GONE);
                    }
                    if (imageView != null) {
                        imageView.setVisibility(View.VISIBLE);
                        String thumbnailUrl = imageInfo.getThumbnailUrl();
                        File smallCacheFile = ImageLoader.getGlideCacheFile(activity, thumbnailUrl);

                        ImageSource small = null;
                        if (smallCacheFile != null && smallCacheFile.exists()) {
                            String smallImagePath = smallCacheFile.getAbsolutePath();
                            small = ImageSource.bitmap(
                                    ImageUtil.getImageBitmap(smallImagePath, ImageUtil.getBitmapDegree(smallImagePath)));
                            int widSmall = ImageUtil.getWidthHeight(smallImagePath)[0];
                            int heiSmall = ImageUtil.getWidthHeight(smallImagePath)[1];
                            if (ImageUtil.isBmpImageWithMime(originalUrl, cacheFile.getAbsolutePath())) {
                                small.tilingDisabled();
                            }
                            small.dimensions(widSmall, heiSmall);
                        }

                        String imagePath = cacheFile.getAbsolutePath();
                        ImageSource origin = ImageSource.uri(imagePath);
                        int widOrigin = ImageUtil.getWidthHeight(imagePath)[0];
                        int heiOrigin = ImageUtil.getWidthHeight(imagePath)[1];
                        if (ImageUtil.isBmpImageWithMime(originalUrl, cacheFile.getAbsolutePath())) {
                            origin.tilingDisabled();
                        }
                        origin.dimensions(widOrigin, heiOrigin);

                        setImageSpec(imagePath, imageView);

                        imageView.setOrientation(SubsamplingScaleImageViewDragClose.ORIENTATION_USE_EXIF);
                        imageView.setImage(origin, small);
                    }
                }
            } else {
                notifyDataSetChanged();
            }
        } else {
            notifyDataSetChanged();
        }
    }

    @SuppressLint("CheckResult")
    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, final int position) {
        if (activity == null) {
            return container;
        }
        View convertView = View.inflate(activity, R.layout.sh_item_photoview, null);
        final ProgressBar progressBar = convertView.findViewById(R.id.progress_view);
        final FingerDragHelper fingerDragHelper = convertView.findViewById(R.id.fingerDragHelper);
        final SubsamplingScaleImageViewDragClose imageView = convertView.findViewById(R.id.photo_view);
        final PhotoView imageGif = convertView.findViewById(R.id.gif_view);

        final ImageInfo info = this.imageInfo.get(position);
        final String originPathUrl = info.getOriginUrl();
        final String thumbPathUrl = info.getThumbnailUrl();

        imageView.setMinimumScaleType(SubsamplingScaleImageViewDragClose.SCALE_TYPE_CENTER_INSIDE);
        imageView.setDoubleTapZoomStyle(SubsamplingScaleImageViewDragClose.ZOOM_FOCUS_CENTER);
        imageView.setDoubleTapZoomDuration(ImagePreview.getInstance().getZoomTransitionDuration());
        imageView.setMinScale(ImagePreview.getInstance().getMinScale());
        imageView.setMaxScale(ImagePreview.getInstance().getMaxScale());
        imageView.setDoubleTapZoomScale(ImagePreview.getInstance().getMediumScale());

        imageGif.setZoomTransitionDuration(ImagePreview.getInstance().getZoomTransitionDuration());
        imageGif.setMinimumScale(ImagePreview.getInstance().getMinScale());
        imageGif.setMaximumScale(ImagePreview.getInstance().getMaxScale());
        imageGif.setScaleType(ImageView.ScaleType.FIT_CENTER);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ImagePreview.getInstance().isEnableClickClose()) {
                    activity.onBackPressed();
                }
                if (ImagePreview.getInstance().getBigImageClickListener() != null) {
                    ImagePreview.getInstance().getBigImageClickListener().onClick(activity, v, position);
                }
            }
        });
        imageGif.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ImagePreview.getInstance().isEnableClickClose()) {
                    activity.onBackPressed();
                }
                if (ImagePreview.getInstance().getBigImageClickListener() != null) {
                    ImagePreview.getInstance().getBigImageClickListener().onClick(activity, v, position);
                }
            }
        });

        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (ImagePreview.getInstance().getBigImageLongClickListener() != null) {
                    return ImagePreview.getInstance().getBigImageLongClickListener().onLongClick(activity, v, position);
                }
                return false;
            }
        });
        imageGif.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (ImagePreview.getInstance().getBigImageLongClickListener() != null) {
                    return ImagePreview.getInstance().getBigImageLongClickListener().onLongClick(activity, v, position);
                }
                return false;
            }
        });

        if (activity instanceof ImagePreviewActivity) {
            ((ImagePreviewActivity) activity).setAlpha(1);
        }

        if (ImagePreview.getInstance().isEnableDragClose()) {
            fingerDragHelper.setOnAlphaChangeListener(new FingerDragHelper.onAlphaChangedListener() {
                @Override
                public void onTranslationYChanged(MotionEvent event, float translationY) {
                    float yAbs = Math.abs(translationY);
                    float percent = yAbs / PhoneUtil.getPhoneHei(activity.getApplicationContext());
                    float number = 1.0F - percent;

                    if (activity instanceof ImagePreviewActivity) {
                        ((ImagePreviewActivity) activity).setAlpha(number);
                    }

                    if (imageGif.getVisibility() == View.VISIBLE) {
                        imageGif.setScaleY(number);
                        imageGif.setScaleX(number);
                    }
                    if (imageView.getVisibility() == View.VISIBLE) {
                        imageView.setScaleY(number);
                        imageView.setScaleX(number);
                    }
                }
            });
        }

        imageGifHashMap.remove(originPathUrl);
        imageGifHashMap.put(originPathUrl + "_" + position, imageGif);

        imageHashMap.remove(originPathUrl);
        imageHashMap.put(originPathUrl + "_" + position, imageView);

        ImagePreview.LoadStrategy loadStrategy = ImagePreview.getInstance().getLoadStrategy();
        // 根据当前加载策略判断，需要加载的url是哪一个
        if (loadStrategy == ImagePreview.LoadStrategy.Default) {
            finalLoadUrl = thumbPathUrl;
        } else if (loadStrategy == ImagePreview.LoadStrategy.AlwaysOrigin) {
            finalLoadUrl = originPathUrl;
        } else if (loadStrategy == ImagePreview.LoadStrategy.AlwaysThumb) {
            finalLoadUrl = thumbPathUrl;
        } else if (loadStrategy == ImagePreview.LoadStrategy.NetworkAuto) {
            if (NetworkUtil.isWiFi(activity)) {
                finalLoadUrl = originPathUrl;
            } else {
                finalLoadUrl = thumbPathUrl;
            }
        }
        finalLoadUrl = finalLoadUrl.trim();
        final String url = finalLoadUrl;

        // 显示加载圈圈
        progressBar.setVisibility(View.VISIBLE);

        // 判断原图缓存是否存在，存在的话，直接显示原图缓存，优先保证清晰。
        File cacheFile = ImageLoader.getGlideCacheFile(activity, originPathUrl);
        if (cacheFile != null && cacheFile.exists()) {
            String imagePath = cacheFile.getAbsolutePath();
            boolean isStandardImage = ImageUtil.isStandardImage(originPathUrl, imagePath);
            if (isStandardImage) {
                loadImageStandard(imagePath, imageView, imageGif, progressBar);
            } else {
                loadImageSpec(url, imagePath, imageView, imageGif, progressBar);
            }
        } else {
            Glide.with(activity).downloadOnly().load(url).addListener(new RequestListener<File>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<File> target,
                                            boolean isFirstResource) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String fileFullName = String.valueOf(System.currentTimeMillis());
                            String saveDir = FileUtil.getAvailableCacheDir(activity).getAbsolutePath() + File.separator + "image/";
                            File downloadFile = HttpUtil.downloadFile(url, fileFullName, saveDir);
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    if (downloadFile != null && downloadFile.exists() && downloadFile.length() > 0) {
                                        // 通过urlConn下载完成
                                        loadSuccess(originPathUrl, downloadFile, imageView, imageGif, progressBar);
                                    } else {
                                        loadFailed(imageView, imageGif, progressBar, e);
                                    }
                                }
                            });
                        }
                    }).start();
                    return true;
                }

                @Override
                public boolean onResourceReady(File resource, Object model, Target<File> target, DataSource dataSource,
                                               boolean isFirstResource) {
                    loadSuccess(url, resource, imageView, imageGif, progressBar);
                    return true;
                }
            }).into(new FileTarget() {
                @Override
                public void onLoadStarted(@Nullable Drawable placeholder) {
                    super.onLoadStarted(placeholder);
                }
            });
        }
        container.addView(convertView);
        return convertView;
    }

    @Override
    public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull final Object object) {
        super.setPrimaryItem(container, position, object);
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return POSITION_NONE;
    }

    private void loadFailed(SubsamplingScaleImageViewDragClose imageView, ImageView imageGif, ProgressBar progressBar,
                            GlideException e) {
        progressBar.setVisibility(View.GONE);
        imageGif.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);

        imageView.setZoomEnabled(false);
        imageView.setImage(ImageSource.resource(ImagePreview.getInstance().getErrorPlaceHolder()));

        if (ImagePreview.getInstance().isShowErrorToast()) {
            String errorMsg = activity.getString(R.string.toast_load_failed);
            if (e != null) {
                errorMsg = errorMsg.concat(":\n").concat(e.getMessage());
            }
            if (errorMsg.length() > 200) {
                errorMsg = errorMsg.substring(0, 199);
            }
            ToastUtil.getInstance()._short(activity.getApplicationContext(), errorMsg);
        }
    }

    private void loadSuccess(String imageUrl, File resource, SubsamplingScaleImageViewDragClose imageView, ImageView imageGif,
                             ProgressBar progressBar) {
        String imagePath = resource.getAbsolutePath();
        boolean isStandardImage = ImageUtil.isStandardImage(imageUrl, imagePath);
        if (isStandardImage) {
            loadImageStandard(imagePath, imageView, imageGif, progressBar);
        } else {
            loadImageSpec(imageUrl, imagePath, imageView, imageGif, progressBar);
        }
    }

    private void setImageSpec(final String imagePath, final SubsamplingScaleImageViewDragClose imageView) {
        boolean isLongImage = ImageUtil.isLongImage(activity, imagePath);
        if (isLongImage) {
            imageView.setMinimumScaleType(SubsamplingScaleImageViewDragClose.SCALE_TYPE_START);
            imageView.setMinScale(ImageUtil.getLongImageMinScale(activity, imagePath));
            imageView.setMaxScale(ImageUtil.getLongImageMaxScale(activity, imagePath));
            imageView.setDoubleTapZoomScale(ImageUtil.getLongImageMaxScale(activity, imagePath));
        } else {
            boolean isWideImage = ImageUtil.isWideImage(activity, imagePath);
            boolean isSmallImage = ImageUtil.isSmallImage(activity, imagePath);
            if (isWideImage) {
                imageView.setMinimumScaleType(SubsamplingScaleImageViewDragClose.SCALE_TYPE_CENTER_INSIDE);
                imageView.setMinScale(ImagePreview.getInstance().getMinScale());
                imageView.setMaxScale(ImagePreview.getInstance().getMaxScale());
                imageView.setDoubleTapZoomScale(ImageUtil.getWideImageDoubleScale(activity, imagePath));
            } else if (isSmallImage) {
                imageView.setMinimumScaleType(SubsamplingScaleImageViewDragClose.SCALE_TYPE_CUSTOM);
                imageView.setMinScale(ImageUtil.getSmallImageMinScale(activity, imagePath));
                imageView.setMaxScale(ImageUtil.getSmallImageMaxScale(activity, imagePath));
                imageView.setDoubleTapZoomScale(ImageUtil.getSmallImageMaxScale(activity, imagePath));
            } else {
                imageView.setMinimumScaleType(SubsamplingScaleImageViewDragClose.SCALE_TYPE_CENTER_INSIDE);
                imageView.setMinScale(ImagePreview.getInstance().getMinScale());
                imageView.setMaxScale(ImagePreview.getInstance().getMaxScale());
                imageView.setDoubleTapZoomScale(ImagePreview.getInstance().getMediumScale());
            }
        }
    }

    private void loadImageStandard(final String imagePath, final SubsamplingScaleImageViewDragClose imageView,
                                   final ImageView imageGif, final ProgressBar progressBar) {

        imageGif.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);

        setImageSpec(imagePath, imageView);

        imageView.setOrientation(SubsamplingScaleImageViewDragClose.ORIENTATION_USE_EXIF);
        ImageSource imageSource = ImageSource.uri(Uri.fromFile(new File(imagePath)));
        if (ImageUtil.isBmpImageWithMime(imagePath, imagePath)) {
            imageSource.tilingDisabled();
        }
        imageView.setImage(imageSource);

        imageView.setOnImageEventListener(new SimpleOnImageEventListener() {
            @Override
            public void onReady() {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void loadImageSpec(final String imageUrl, final String imagePath, final SubsamplingScaleImageViewDragClose imageView,
                               final ImageView imageSpec, final ProgressBar progressBar) {

        imageSpec.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.GONE);

        boolean isGifFile = ImageUtil.isGifImageWithMime(imageUrl, imagePath);
        if (isGifFile) {
            Glide.with(activity)
                    .asGif()
                    .load(imagePath)
                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE).error(ImagePreview.getInstance().getErrorPlaceHolder()))
                    .listener(new RequestListener<GifDrawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<GifDrawable> target,
                                                    boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            imageSpec.setVisibility(View.GONE);
                            imageView.setVisibility(View.VISIBLE);
                            imageView.setImage(ImageSource.resource(ImagePreview.getInstance().getErrorPlaceHolder()));
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target,
                                                       DataSource dataSource, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(imageSpec);
        } else {
            Glide.with(activity)
                    .load(imageUrl)
                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE).error(ImagePreview.getInstance().getErrorPlaceHolder()))
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            imageSpec.setVisibility(View.GONE);
                            imageView.setVisibility(View.VISIBLE);
                            imageView.setImage(ImageSource.resource(ImagePreview.getInstance().getErrorPlaceHolder()));
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(imageSpec);
        }
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        String originUrl = imageInfo.get(position).getOriginUrl() + "_" + position;
        try {
            if (imageHashMap != null) {
                SubsamplingScaleImageViewDragClose imageViewDragClose = imageHashMap.get(originUrl);
                if (imageViewDragClose != null) {
                    imageViewDragClose.resetScaleAndCenter();
                    imageViewDragClose.destroyDrawingCache();
                    imageViewDragClose.recycle();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (imageGifHashMap != null) {
                PhotoView photoView = imageGifHashMap.get(originUrl);
                if (photoView != null) {
                    photoView.destroyDrawingCache();
                    photoView.setImageBitmap(null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            ImageLoader.clearMemory(activity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}