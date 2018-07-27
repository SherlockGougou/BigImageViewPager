package cc.shinichi.library.view;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.R;
import cc.shinichi.library.bean.ImageInfo;
import cc.shinichi.library.glide.ImageLoader;
import cc.shinichi.library.glide.engine.SimpleFileTarget;
import cc.shinichi.library.tool.ImageUtil;
import cc.shinichi.library.tool.NetworkUtil;
import cc.shinichi.library.tool.Print;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
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
            Glide.clear((SubsamplingScaleImageView) entry.getValue());
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
  public void loadOrigin(final String originUrl) {
    if (imageHashMap.get(originUrl) != null) {
      final SubsamplingScaleImageView imageView = imageHashMap.get(originUrl);

      Glide.with(activity).load(originUrl).downloadOnly(new SimpleFileTarget() {
        @Override
        public void onResourceReady(File resource, GlideAnimation<? super File> glideAnimation) {
          super.onResourceReady(resource, glideAnimation);
          String imagePath = resource.getAbsolutePath();
          boolean isLongImage = ImageUtil.isLongImage(imagePath);
          Print.d(TAG, "isLongImage = " + isLongImage);
          if (isLongImage) {
            imageView.setOrientation(ImageUtil.getOrientation(imagePath));
            imageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_START);
          }
          imageView.setImage(ImageSource.uri(Uri.fromFile(new File(resource.getAbsolutePath()))));
        }
      });
    } else {
      notifyDataSetChanged();
    }
  }

  @NonNull @Override
  public Object instantiateItem(@NonNull ViewGroup container, final int position) {
    View convertView = View.inflate(activity, R.layout.item_photoview, null);
    final ProgressBar progressBar = convertView.findViewById(R.id.progress_view);
    final SubsamplingScaleImageView imageView = convertView.findViewById(R.id.photo_view);
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

    File cacheFile = ImageLoader.getGlideCacheFile(activity, originPathUrl);
    if (cacheFile != null && cacheFile.exists()) {
      Glide.with(activity).load(cacheFile).downloadOnly(new SimpleFileTarget() {
        @Override
        public void onResourceReady(File resource, GlideAnimation<? super File> glideAnimation) {
          super.onResourceReady(resource, glideAnimation);
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

      Glide.with(activity).load(finalLoadUrl).downloadOnly(new SimpleFileTarget() {
        @Override public void onLoadStarted(Drawable placeholder) {
          super.onLoadStarted(placeholder);
          progressBar.setVisibility(View.VISIBLE);
        }

        @Override public void onLoadFailed(Exception e, Drawable errorDrawable) {
          super.onLoadFailed(e, errorDrawable);
          // glide会有时加载失败，这不是本框架的问题，具体看：https://github.com/bumptech/glide/issues/2894
          Glide.with(activity).load(finalLoadUrl).downloadOnly(new SimpleFileTarget() {
            @Override public void onLoadFailed(Exception e, Drawable errorDrawable) {
              super.onLoadFailed(e, errorDrawable);
              progressBar.setVisibility(View.GONE);
            }

            @Override public void onResourceReady(File resource,
                GlideAnimation<? super File> glideAnimation) {
              String imagePath = resource.getAbsolutePath();
              boolean isLongImage = ImageUtil.isLongImage(imagePath);
              Print.d(TAG, "isLongImage = " + isLongImage);
              if (isLongImage) {
                imageView.setOrientation(ImageUtil.getOrientation(imagePath));
                imageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_START);
              }
              imageView.setImage(
                  ImageSource.uri(Uri.fromFile(new File(resource.getAbsolutePath()))));
              progressBar.setVisibility(View.GONE);
            }
          });
        }

        @Override
        public void onResourceReady(File resource, GlideAnimation<? super File> glideAnimation) {
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