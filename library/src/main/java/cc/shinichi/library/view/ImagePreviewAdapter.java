package cc.shinichi.library.view;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.R;
import cc.shinichi.library.bean.ImageInfo;
import cc.shinichi.library.glide.ImageLoader;
import cc.shinichi.library.glide.engine.SimpleFileTarget;
import cc.shinichi.library.tool.utility.common.NetworkUtil;
import cc.shinichi.library.tool.utility.image.ImageUtil;
import cc.shinichi.library.tool.utility.ui.PhoneUtil;
import cc.shinichi.library.tool.utility.ui.ToastUtil;
import cc.shinichi.library.view.helper.FingerDragHelper;
import cc.shinichi.library.view.helper.ImageSource;
import cc.shinichi.library.view.helper.SubsamplingScaleImageViewDragClose;
import cc.shinichi.library.view.photoview.PhotoView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.Target;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author SherlockHolmes
 */
public class ImagePreviewAdapter extends PagerAdapter {

    private static final String TAG = "ImagePreview";
    private Activity activity;
    private List<ImageInfo> imageInfo;
    private HashMap<String, SubsamplingScaleImageViewDragClose> imageHashMap = new HashMap<>();
    private HashMap<String, PhotoView> imageGifHashMap = new HashMap<>();
    private String finalLoadUrl = "";

    public ImagePreviewAdapter(Activity activity, @NonNull List<ImageInfo> imageInfo) {
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
                imageHashMap = null;
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
                imageGifHashMap = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override public int getCount() {
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
                boolean isCacheIsGif = ImageUtil.isGifImageWithMime(cacheFile.getAbsolutePath());
                if (isCacheIsGif) {
                    imageGif.setVisibility(View.VISIBLE);
                    imageView.setVisibility(View.GONE);

                    Glide.with(activity)
                        .load(cacheFile)
                        .asGif()
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .error(ImagePreview.getInstance().getErrorPlaceHolder())
                        .into(imageGif);
                } else {
                    imageGif.setVisibility(View.GONE);
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
                        if (ImageUtil.isBmpImageWithMime(cacheFile.getAbsolutePath())) {
                            small.tilingDisabled();
                        }
                        small.dimensions(widSmall, heiSmall);
                    }

                    String imagePath = cacheFile.getAbsolutePath();
                    ImageSource origin = ImageSource.uri(imagePath);
                    int widOrigin = ImageUtil.getWidthHeight(imagePath)[0];
                    int heiOrigin = ImageUtil.getWidthHeight(imagePath)[1];
                    if (ImageUtil.isBmpImageWithMime(cacheFile.getAbsolutePath())) {
                        origin.tilingDisabled();
                    }
                    origin.dimensions(widOrigin, heiOrigin);

                    setImageSpec(imagePath, imageView);

                    imageView.setOrientation(SubsamplingScaleImageViewDragClose.ORIENTATION_USE_EXIF);
                    imageView.setImage(origin, small);
                }
            } else {
                notifyDataSetChanged();
            }
        } else {
            notifyDataSetChanged();
        }
    }

    @NonNull @Override public Object instantiateItem(@NonNull ViewGroup container, final int position) {
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
            @Override public void onClick(View v) {
                if (ImagePreview.getInstance().isEnableClickClose()) {
                    activity.finish();
                }
                if (ImagePreview.getInstance().getBigImageClickListener() != null) {
                    ImagePreview.getInstance().getBigImageClickListener().onClick(v, position);
                }
            }
        });
        imageGif.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (ImagePreview.getInstance().isEnableClickClose()) {
                    activity.finish();
                }
                if (ImagePreview.getInstance().getBigImageClickListener() != null) {
                    ImagePreview.getInstance().getBigImageClickListener().onClick(v, position);
                }
            }
        });

        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                if (ImagePreview.getInstance().getBigImageLongClickListener() != null) {
                    return ImagePreview.getInstance().getBigImageLongClickListener().onLongClick(v, position);
                }
                return false;
            }
        });
        imageGif.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                if (ImagePreview.getInstance().getBigImageLongClickListener() != null) {
                    return ImagePreview.getInstance().getBigImageLongClickListener().onLongClick(v, position);
                }
                return false;
            }
        });

        if (ImagePreview.getInstance().isEnableDragClose()) {
            fingerDragHelper.setOnAlphaChangeListener(new FingerDragHelper.onAlphaChangedListener() {
                @Override public void onTranslationYChanged(MotionEvent event, float translationY) {
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
        imageGifHashMap.put(originPathUrl, imageGif);

        imageHashMap.remove(originPathUrl);
        imageHashMap.put(originPathUrl, imageView);

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
            boolean isCacheIsGif = ImageUtil.isGifImageWithMime(cacheFile.getAbsolutePath());
            if (isCacheIsGif) {
                imageGif.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.GONE);

                String imagePath = cacheFile.getAbsolutePath();
                loadGifImageSpec(imagePath, imageView, imageGif, progressBar);
            } else {
                imageGif.setVisibility(View.GONE);
                imageView.setVisibility(View.VISIBLE);

                String imagePath = cacheFile.getAbsolutePath();
                loadImageSpec(imagePath, imageView, imageGif, progressBar);
            }
        } else {
            Glide.with(activity).load(url).downloadOnly(new SimpleFileTarget() {
                @Override public void onLoadStarted(Drawable placeholder) {
                    super.onLoadStarted(placeholder);
                }

                @Override public void onLoadFailed(Exception e, Drawable errorDrawable) {
                    super.onLoadFailed(e, errorDrawable);

                    Glide.with(activity).load(url).downloadOnly(new SimpleFileTarget() {
                        @Override public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            super.onLoadFailed(e, errorDrawable);

                            Glide.with(activity).load(url).downloadOnly(new SimpleFileTarget() {
                                @Override public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                    super.onLoadFailed(e, errorDrawable);
                                    loadFailed(imageView, imageGif, progressBar, e);
                                }

                                @Override public void onResourceReady(File resource,
                                    GlideAnimation<? super File> glideAnimation) {
                                    loadSuccess(resource, imageView, imageGif, progressBar);
                                }
                            });
                        }

                        @Override
                        public void onResourceReady(File resource, GlideAnimation<? super File> glideAnimation) {
                            loadSuccess(resource, imageView, imageGif, progressBar);
                        }
                    });
                }

                @Override public void onResourceReady(File resource, GlideAnimation<? super File> glideAnimation) {
                    loadSuccess(resource, imageView, imageGif, progressBar);
                }
            });
        }
        container.addView(convertView);
        return convertView;
    }

    @Override public void destroyItem(ViewGroup container, int position, Object object) {
        try {
            container.removeView((View) object);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            ImageLoader.clearMemory(activity);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (imageHashMap != null && imageHashMap.get(imageInfo.get(position).getOriginUrl()) != null) {
                imageHashMap.get(imageInfo.get(position).getOriginUrl()).destroyDrawingCache();
                imageHashMap.get(imageInfo.get(position).getOriginUrl()).recycle();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (imageGifHashMap != null && imageGifHashMap.get(imageInfo.get(position).getOriginUrl()) != null) {
                imageGifHashMap.get(imageInfo.get(position).getOriginUrl()).destroyDrawingCache();
                imageGifHashMap.get(imageInfo.get(position).getOriginUrl()).setImageBitmap(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override public void setPrimaryItem(ViewGroup container, int position, final Object object) {
        super.setPrimaryItem(container, position, object);
    }

    @Override public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    private void loadFailed(SubsamplingScaleImageViewDragClose imageView, ImageView imageGif, ProgressBar progressBar,
        Exception e) {
        progressBar.setVisibility(View.GONE);
        imageGif.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);

        imageView.setZoomEnabled(false);
        imageView.setImage(ImageSource.resource(ImagePreview.getInstance().getErrorPlaceHolder()));

        String errorMsg = "加载失败";
        if (e != null) {
            errorMsg = errorMsg.concat(":\n").concat(e.getMessage());
        }
        if (errorMsg.length() > 200) {
            errorMsg = errorMsg.substring(0, 199);
        }
        ToastUtil.getInstance()._short(activity.getApplicationContext(), errorMsg);
    }

    private void loadSuccess(File resource, SubsamplingScaleImageViewDragClose imageView, ImageView imageGif,
        ProgressBar progressBar) {
        String imagePath = resource.getAbsolutePath();
        boolean isCacheIsGif = ImageUtil.isGifImageWithMime(imagePath);
        if (isCacheIsGif) {
            loadGifImageSpec(imagePath, imageView, imageGif, progressBar);
        } else {
            loadImageSpec(imagePath, imageView, imageGif, progressBar);
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

    private void loadImageSpec(final String imagePath, final SubsamplingScaleImageViewDragClose imageView,
        final ImageView imageGif, final ProgressBar progressBar) {

        imageGif.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);

        setImageSpec(imagePath, imageView);

        imageView.setOrientation(SubsamplingScaleImageViewDragClose.ORIENTATION_USE_EXIF);
        ImageSource imageSource = ImageSource.uri(Uri.fromFile(new File(imagePath)));
        if (ImageUtil.isBmpImageWithMime(imagePath)) {
            imageSource.tilingDisabled();
        }
        imageView.setImage(imageSource);

        imageView.setOnImageEventListener(new SubsamplingScaleImageViewDragClose.OnImageEventListener() {
            @Override public void onReady() {
                progressBar.setVisibility(View.GONE);
            }

            @Override public void onImageLoaded() {

            }

            @Override public void onPreviewLoadError(Exception e) {

            }

            @Override public void onImageLoadError(Exception e) {

            }

            @Override public void onTileLoadError(Exception e) {

            }

            @Override public void onPreviewReleased() {

            }
        });
    }

    private void loadGifImageSpec(final String imagePath, final SubsamplingScaleImageViewDragClose imageView,
        final ImageView imageGif, final ProgressBar progressBar) {

        imageGif.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.GONE);

        Glide.with(activity)
            .load(new File(imagePath))
            .asGif()
            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
            .error(ImagePreview.getInstance().getErrorPlaceHolder())
            .listener(new RequestListener<File, GifDrawable>() {
                @Override public boolean onException(Exception e, File model, Target<GifDrawable> target,
                    boolean isFirstResource) {
                    imageGif.setVisibility(View.GONE);
                    imageView.setVisibility(View.VISIBLE);
                    imageView.setImage(ImageSource.resource(ImagePreview.getInstance().getErrorPlaceHolder()));
                    return false;
                }

                @Override public boolean onResourceReady(GifDrawable resource, File model, Target<GifDrawable> target,
                    boolean isFromMemoryCache, boolean isFirstResource) {
                    progressBar.setVisibility(View.GONE);
                    return false;
                }
            })
            .into(imageGif);
    }
}