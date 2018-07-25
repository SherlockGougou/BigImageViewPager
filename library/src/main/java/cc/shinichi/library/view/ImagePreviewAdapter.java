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
import cc.shinichi.library.glide.sunfusheng.progress.GlideApp;
import cc.shinichi.library.tool.Print;
import cc.shinichi.library.tool.ToastUtil;
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
  public void loadOrigin(final String originUrl) {
    if (imageHashMap.get(originUrl) != null) {
      final SubsamplingScaleImageView imageView = imageHashMap.get(originUrl);
      File cacheFile = ImageLoader.getGlideCacheFile(activity, originUrl);
      if (cacheFile != null && cacheFile.exists()) {
        imageView.setImage(ImageSource.uri(Uri.fromFile(new File(cacheFile.getAbsolutePath()))));
      }
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

    if (imageHashMap.containsKey(originPathUrl)) {
      imageHashMap.remove(originPathUrl);
    }
    imageHashMap.put(originPathUrl, imageView);

    File cacheFile = ImageLoader.getGlideCacheFile(activity, originPathUrl);
    if (cacheFile != null && cacheFile.exists()) {
      imageView.setImage(ImageSource.uri(Uri.fromFile(new File(cacheFile.getAbsolutePath()))));
      progressBar.setVisibility(View.GONE);
    } else {
      // 加载缩略图
      Print.d(TAG, "thumbPathUrl == " + thumbPathUrl);

      SimpleTarget<File> target = new SimpleTarget<File>() {
        @Override public void onLoadStarted(@Nullable Drawable placeholder) {
          super.onLoadStarted(placeholder);
          progressBar.setVisibility(View.VISIBLE);
        }

        @Override public void onLoadFailed(@Nullable Drawable errorDrawable) {
          super.onLoadFailed(errorDrawable);
          progressBar.setVisibility(View.GONE);
          notifyDataSetChanged();
        }

        @Override public void onResourceReady(@NonNull File resource,
            @Nullable Transition<? super File> transition) {
          imageView.setImage(ImageSource.uri(Uri.fromFile(new File(resource.getAbsolutePath()))));
          progressBar.setVisibility(View.GONE);
        }
      };
      GlideApp.with(activity).downloadOnly().load(thumbPathUrl).into(target);
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