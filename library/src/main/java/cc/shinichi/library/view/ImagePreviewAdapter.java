package cc.shinichi.library.view;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.R;
import cc.shinichi.library.bean.ImageInfo;
import cc.shinichi.library.glide.ImageLoader;
import cc.shinichi.sherlockutillibrary.utility.common.NetworkUtil;
import cc.shinichi.sherlockutillibrary.utility.common.Print;
import cc.shinichi.sherlockutillibrary.utility.image.ImageUtil;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImagePreviewAdapter extends PagerAdapter {

  private static final String TAG = "ImagePreview";
  private Activity activity;
  private List<ImageInfo> imageInfo;
  private HashMap<String, SubsamplingScaleImageView> imageHashMap = new HashMap<>();
  private String finalLoadUrl = "";// 最终加载的图片url

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
            ((SubsamplingScaleImageView) entry.getValue()).recycle();
          }
        }
        imageHashMap.clear();
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
    if (imageHashMap.get(imageInfo.getOriginUrl()) != null) {
      final SubsamplingScaleImageView imageView = imageHashMap.get(imageInfo.getOriginUrl());
      File cacheFile = ImageLoader.getGlideCacheFile(activity, imageInfo.getOriginUrl());
      if (cacheFile != null && cacheFile.exists()) {
        String thumbnailUrl = imageInfo.getThumbnailUrl();
        File smallCacheFile = ImageLoader.getGlideCacheFile(activity, thumbnailUrl);
        ImageSource small = null;
        if (smallCacheFile != null && smallCacheFile.exists()) {
          String smallImagePath = smallCacheFile.getAbsolutePath();
          small = ImageSource.bitmap(ImageUtil.getImageBitmap(smallImagePath, ImageUtil.getBitmapDegree(smallImagePath)));
          int widSmall = ImageUtil.getWidthHeight(smallImagePath)[0];
          int heiSmall = ImageUtil.getWidthHeight(smallImagePath)[1];
          small.dimensions(widSmall, heiSmall);
        }

        String imagePath = cacheFile.getAbsolutePath();
        ImageSource origin = ImageSource.uri(imagePath);
        int widOrigin = ImageUtil.getWidthHeight(imagePath)[0];
        int heiOrigin = ImageUtil.getWidthHeight(imagePath)[1];
        origin.dimensions(widOrigin, heiOrigin);

        boolean isLongImage = ImageUtil.isLongImage(imagePath);
        Print.d(TAG, "isLongImage = " + isLongImage);
        if (isLongImage) {
          imageView.setOrientation(ImageUtil.getOrientation(imagePath));
          imageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_START);
        }
        imageView.setImage(origin, small);
      }
    } else {
      notifyDataSetChanged();
    }
  }

  @NonNull @Override
  public Object instantiateItem(@NonNull ViewGroup container, final int position) {
    if (activity == null) {
      return container;
    }
    View convertView = View.inflate(activity, R.layout.item_photoview, null);
    final ProgressBar progressBar = convertView.findViewById(R.id.progress_view);
    final SubsamplingScaleImageView imageView = convertView.findViewById(R.id.photo_view);

    imageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE);
    imageView.setDoubleTapZoomStyle(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER);
    imageView.setDoubleTapZoomDuration(ImagePreview.getInstance().getZoomTransitionDuration());
    imageView.setMinScale(ImagePreview.getInstance().getMinScale());
    imageView.setMaxScale(ImagePreview.getInstance().getMaxScale());
    imageView.setDoubleTapZoomScale(ImagePreview.getInstance().getMediumScale());

    imageView.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        activity.finish();
      }
    });

    final ImageInfo info = this.imageInfo.get(position);
    final String originPathUrl = info.getOriginUrl();
    final String thumbPathUrl = info.getThumbnailUrl();

    finalLoadUrl = thumbPathUrl;
    ImagePreview.LoadStrategy loadStrategy = ImagePreview.getInstance().getLoadStrategy();

    if (imageHashMap.containsKey(originPathUrl)) {
      imageHashMap.remove(originPathUrl);
    }
    imageHashMap.put(originPathUrl, imageView);

    // 判断原图缓存是否存在，存在的话，直接显示原图缓存，优先保证清晰。
    File cacheFile = ImageLoader.getGlideCacheFile(activity, originPathUrl);
    if (cacheFile != null && cacheFile.exists()) {
      String imagePath = cacheFile.getAbsolutePath();
      boolean isLongImage = ImageUtil.isLongImage(imagePath);
      Print.d(TAG, "isLongImage = " + isLongImage);
      if (isLongImage) {
        imageView.setOrientation(ImageUtil.getOrientation(imagePath));
        imageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_START);
      }
      imageView.setImage(ImageSource.uri(Uri.fromFile(new File(cacheFile.getAbsolutePath()))));
      progressBar.setVisibility(View.GONE);
    } else {
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
      Print.d(TAG, "finalLoadUrl == " + finalLoadUrl);
      final String url = finalLoadUrl;

      Glide.with(activity).downloadOnly().load(url).into(new SimpleTarget<File>() {
        @Override public void onLoadStarted(@Nullable Drawable placeholder) {
          super.onLoadStarted(placeholder);
          progressBar.setVisibility(View.VISIBLE);
        }

        @Override public void onLoadFailed(@Nullable Drawable errorDrawable) {
          super.onLoadFailed(errorDrawable);
          // glide会有时加载失败，这不是本框架的问题，具体看：https://github.com/bumptech/glide/issues/2894
          Glide.with(activity).downloadOnly().load(url).into(new SimpleTarget<File>() {
            @Override public void onLoadStarted(@Nullable Drawable placeholder) {
              super.onLoadStarted(placeholder);
              progressBar.setVisibility(View.VISIBLE);
            }

            @Override public void onLoadFailed(@Nullable Drawable errorDrawable) {
              super.onLoadFailed(errorDrawable);
              // glide会有时加载失败，这不是本框架的问题，具体看：https://github.com/bumptech/glide/issues/2894

            }

            @Override public void onResourceReady(@NonNull File resource,
                @Nullable Transition<? super File> transition) {
              String imagePath = resource.getAbsolutePath();
              boolean isLongImage = ImageUtil.isLongImage(imagePath);
              Print.d(TAG, "isLongImage = " + isLongImage);
              if (isLongImage) {
                imageView.setOrientation(ImageUtil.getOrientation(imagePath));
                imageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_START);
              }
              imageView.setImage(ImageSource.uri(Uri.fromFile(new File(resource.getAbsolutePath()))));
              progressBar.setVisibility(View.GONE);
            }
          });
        }

        @Override public void onResourceReady(@NonNull File resource,
            @Nullable Transition<? super File> transition) {
          String imagePath = resource.getAbsolutePath();
          boolean isLongImage = ImageUtil.isLongImage(imagePath);
          Print.d(TAG, "isLongImage = " + isLongImage);
          if (isLongImage) {
            imageView.setOrientation(ImageUtil.getOrientation(imagePath));
            imageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_START);
          }
          imageView.setImage(ImageSource.uri(Uri.fromFile(new File(resource.getAbsolutePath()))));
          progressBar.setVisibility(View.GONE);
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
}