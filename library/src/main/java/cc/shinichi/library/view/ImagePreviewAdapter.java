package cc.shinichi.library.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.R;
import cc.shinichi.library.bean.ImageInfo;
import cc.shinichi.library.byakugallery.TileBitmapDrawable;
import cc.shinichi.library.glide.ImageLoader;
import cc.shinichi.library.glide.engine.SimpleFileTarget;
import cc.shinichi.library.tool.Print;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.github.chrisbanes.photoview.PhotoView;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImagePreviewAdapter extends PagerAdapter {

	private static final String TAG = "ImagePreview";
	private ImagePreviewActivity activity;
	private List<ImageInfo> imageInfo;
	private HashMap<String, PhotoView> detachImageViewHashMap = new HashMap<>();
	private int phoneWid = 0;

	public ImagePreviewAdapter(ImagePreviewActivity activity, @NonNull List<ImageInfo> imageInfo) {
		super();
		this.imageInfo = imageInfo;
		this.activity = activity;

		WindowManager windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics metric = new DisplayMetrics();
		windowManager.getDefaultDisplay().getMetrics(metric);
		this.phoneWid = metric.widthPixels;
	}

	private void closePage() {
		try {
			if (detachImageViewHashMap != null && detachImageViewHashMap.size() > 0) {
				for (Object o : detachImageViewHashMap.entrySet()) {
					Map.Entry entry = (Map.Entry) o;
					if (entry != null && entry.getValue() != null) {
						((PhotoView) entry.getValue()).setImageDrawable(null);
						Glide.clear((PhotoView) entry.getValue());
					}
				}
				detachImageViewHashMap.clear();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		activity.finish();
	}

	@Override public int getCount() {
		return imageInfo.size();
	}

	/**
	 * 加载原图
	 */
	public void loadOrigin(final String originUrl) {
		if (detachImageViewHashMap.get(originUrl) != null) {
			final PhotoView imageView = detachImageViewHashMap.get(originUrl);

			Glide.with(activity).load(originUrl).downloadOnly(new SimpleFileTarget() {
				@Override public void onResourceReady(File resource, GlideAnimation<? super File> glideAnimation) {
					super.onResourceReady(resource, glideAnimation);
					TileBitmapDrawable.attachTileBitmapDrawable(imageView, resource.getAbsolutePath(), null, null);
				}
			});
		} else {
			notifyDataSetChanged();
		}
	}

	@NonNull @Override public Object instantiateItem(@NonNull ViewGroup container, final int position) {
		View convertView = View.inflate(activity, R.layout.item_photoview, null);
		final ProgressBar progressBar = convertView.findViewById(R.id.progress_view);
		final PhotoView touchImageView = convertView.findViewById(R.id.photo_view);

		touchImageView.setZoomTransitionDuration(ImagePreview.getInstance().getZoomTransitionDuration());
		touchImageView.setScaleLevels(ImagePreview.getInstance().getMinScale(),
			ImagePreview.getInstance().getMediumScale(),
			ImagePreview.getInstance().getMaxScale());
		touchImageView.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {
			@Override public boolean onSingleTapConfirmed(MotionEvent e) {
				closePage();
				return false;
			}

			@Override public boolean onDoubleTap(MotionEvent e) {
				int mode = ImagePreview.getInstance().getScaleMode();
				switch (mode) {
					case ImagePreview.MODE_SCALE_TO_MAX_TO_MIN:
						if (touchImageView.getScale() < touchImageView.getMaximumScale()) {
							touchImageView.setScale(touchImageView.getMaximumScale(), e.getX(), e.getY(), true);
						} else {
							touchImageView.setScale(touchImageView.getMinimumScale(), e.getX(), e.getY(), true);
						}
						break;
					case ImagePreview.MODE_SCALE_TO_MEDIUM_TO_MAX_TO_MIN:
						if (touchImageView.getScale() < touchImageView.getMediumScale()) {
							touchImageView.setScale(touchImageView.getMediumScale(), e.getX(), e.getY(), true);
						} else if (touchImageView.getScale() >= touchImageView.getMediumScale()
							&& touchImageView.getScale() < touchImageView.getMaximumScale()) {
							touchImageView.setScale(touchImageView.getMaximumScale(), e.getX(), e.getY(), true);
						} else {
							touchImageView.setScale(touchImageView.getMinimumScale(), e.getX(), e.getY(), true);
						}
						break;
					case ImagePreview.MODE_SCALE_TO_MEDIUM_TO_MIN:
						if (touchImageView.getScale() < touchImageView.getMediumScale()) {
							touchImageView.setScale(touchImageView.getMediumScale(), e.getX(), e.getY(), true);
						} else {
							touchImageView.setScale(touchImageView.getMinimumScale(), e.getX(), e.getY(), true);
						}
						break;
					default:
						if (touchImageView.getScale() < touchImageView.getMediumScale()) {
							touchImageView.setScale(touchImageView.getMediumScale(), e.getX(), e.getY(), true);
						} else if (touchImageView.getScale() >= touchImageView.getMediumScale()
							&& touchImageView.getScale() < touchImageView.getMaximumScale()) {
							touchImageView.setScale(touchImageView.getMaximumScale(), e.getX(), e.getY(), true);
						} else {
							touchImageView.setScale(touchImageView.getMinimumScale(), e.getX(), e.getY(), true);
						}
						break;
				}
				return true;
			}

			@Override public boolean onDoubleTapEvent(MotionEvent e) {
				return false;
			}
		});

		final ImageInfo info = this.imageInfo.get(position);
		final String originPathUrl = info.getOriginUrl();
		final String thumbPathUrl = info.getThumbnailUrl();

		if (detachImageViewHashMap.containsKey(originPathUrl)) {
			detachImageViewHashMap.remove(originPathUrl);
		}
		detachImageViewHashMap.put(originPathUrl, touchImageView);

		File cacheFile = ImageLoader.getGlideCacheFile(activity, originPathUrl);
		if (cacheFile != null && cacheFile.exists()) {
			Glide.with(activity).load(cacheFile).downloadOnly(new SimpleFileTarget() {
				@Override public void onResourceReady(File resource, GlideAnimation<? super File> glideAnimation) {
					super.onResourceReady(resource, glideAnimation);
					TileBitmapDrawable.attachTileBitmapDrawable(touchImageView, resource.getAbsolutePath(), null, null);
					progressBar.setVisibility(View.GONE);
				}
			});
		} else {
			// 加载缩略图
			Print.d(TAG, "thumbPathUrl == " + thumbPathUrl);
			Glide.with(activity).load(thumbPathUrl).downloadOnly(new SimpleFileTarget() {
				@Override public void onLoadStarted(Drawable placeholder) {
					super.onLoadStarted(placeholder);
					progressBar.setVisibility(View.VISIBLE);
				}

				@Override public void onLoadFailed(Exception e, Drawable errorDrawable) {
					super.onLoadFailed(e, errorDrawable);
					// 不止为何会有时候加载失败，几率挺高，在此处重新加载一次。
					Glide.with(activity).load(thumbPathUrl).downloadOnly(new SimpleFileTarget() {
						@Override public void onLoadFailed(Exception e, Drawable errorDrawable) {
							super.onLoadFailed(e, errorDrawable);
							progressBar.setVisibility(View.GONE);
						}

						@Override public void onResourceReady(File resource, GlideAnimation<? super File> glideAnimation) {
							TileBitmapDrawable.attachTileBitmapDrawable(touchImageView, resource.getAbsolutePath(), null, null);
							progressBar.setVisibility(View.GONE);
						}
					});
				}

				@Override public void onResourceReady(File resource, GlideAnimation<? super File> glideAnimation) {
					TileBitmapDrawable.attachTileBitmapDrawable(touchImageView, resource.getAbsolutePath(), null, null);
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