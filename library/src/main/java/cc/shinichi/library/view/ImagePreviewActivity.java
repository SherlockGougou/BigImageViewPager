package cc.shinichi.library.view;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import cc.shinichi.library.R;
import cc.shinichi.library.bean.ImageInfo;
import cc.shinichi.library.byakugallery.GalleryViewPager;
import cc.shinichi.library.glide.ImageLoader;
import cc.shinichi.library.glide.engine.ProgressTarget;
import cc.shinichi.library.tool.DownloadPictureUtil;
import cc.shinichi.library.tool.HandlerUtils;
import cc.shinichi.library.tool.Print;
import cc.shinichi.library.tool.ToastUtil;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import java.io.File;
import java.io.Serializable;
import java.util.List;

import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class ImagePreviewActivity extends AppCompatActivity implements Handler.Callback, View.OnClickListener {

	public static final String TAG = "ImagePreview";

	private Context context;
	public static final String IMAGE_INFO = "IMAGE_INFO";// 图片集合
	public static final String CURRENT_ITEM = "CURRENT_ITEM";// 默认显示的索引
	public static final String DOWNLOAD_FOLDER_NAME = "DOWNLOAD_FOLDER_NAME";// 下载保存的文件夹名（根目录）

	private ImagePreviewAdapter imagePreviewAdapter;
	private GalleryViewPager viewPager;
	private TextView tv_indicator;
	private FrameLayout fm_image;
	private TextView tv_show_origin;

	private List<ImageInfo> imageInfoList;
    private int currentItem;// 当前显示的图片索引
    private String currentItemOriginPathUrl = "";// 当前显示的原图链接
	private String downloadFolderName = "";// 保存的文件夹名
	private HandlerUtils.HandlerHolder handlerHolder;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		setContentView(R.layout.activity_preview);

		handlerHolder = new HandlerUtils.HandlerHolder(this);

		viewPager = findViewById(R.id.viewPager);
		tv_indicator = findViewById(R.id.tv_indicator);
		fm_image = findViewById(R.id.fm_image);
		tv_show_origin = findViewById(R.id.tv_show_origin);
		// 查看与原图按钮
		tv_show_origin.setOnClickListener(this);

		Intent intent = getIntent();
		imageInfoList = (List<ImageInfo>) intent.getSerializableExtra(IMAGE_INFO);
		currentItem = intent.getIntExtra(CURRENT_ITEM, 0);
		downloadFolderName = intent.getStringExtra(DOWNLOAD_FOLDER_NAME);
		currentItemOriginPathUrl = imageInfoList.get(currentItem).getOriginUrl();

		// 检查缓存是否存在
		checkCache(currentItemOriginPathUrl);

		if (imageInfoList.size() > 1) {
			tv_indicator.setVisibility(View.VISIBLE);
		} else {
			tv_indicator.setVisibility(View.GONE);
		}

		// 更新进度指示器
		tv_indicator.setText(
			String.format(getString(R.string.indicator), currentItem + 1 + " ", " " + imageInfoList.size()));

		imagePreviewAdapter = new ImagePreviewAdapter(this, imageInfoList);
		viewPager.setAdapter(imagePreviewAdapter);
		viewPager.setCurrentItem(currentItem);
		viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override public void onPageSelected(int position) {
				currentItem = position;
				currentItemOriginPathUrl = imageInfoList.get(position).getOriginUrl();
				// 检查缓存是否存在
				checkCache(currentItemOriginPathUrl);
				// 更新进度指示器
				tv_indicator.setText(
					String.format(getString(R.string.indicator), currentItem + 1 + " ", " " + imageInfoList.size()));
			}
		});

		findViewById(R.id.img_download).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				// 检查权限
				if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
					!= PackageManager.PERMISSION_GRANTED) {
					if (ActivityCompat.shouldShowRequestPermissionRationale(ImagePreviewActivity.this,
						Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
						// 拒绝权限
                        ToastUtil.getInstance()._short(context, "您拒绝了存储权限，下载失败！");
					} else {
						//申请权限
						ActivityCompat.requestPermissions(ImagePreviewActivity.this,
							new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE, }, 1);
					}
				} else {
					// 下载当前图片
					downloadCurrentImg();
				}
			}
		});
	}

	@Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
		@NonNull int[] grantResults) {
		if (requestCode == 1) {
			for (int i = 0; i < permissions.length; i++) {
				if (grantResults[i] == PERMISSION_GRANTED) {
					downloadCurrentImg();
				} else {
					ToastUtil.getInstance()._short(context, "您拒绝了存储权限，下载失败！");
				}
			}
		}
	}

	/**
	 * 跳转到此activity需要的参数
	 *
	 * @param context 上下文
	 * @param imageInfo 图片列表
	 * @param index 点击的图片的索引
	 * @param folderName 下载图片保存的文件夹名（默认保存到存储根目录）
	 */
	public static void activityStart(Context context, List<ImageInfo> imageInfo, int index, String folderName) {
		Intent intent = new Intent(context, ImagePreviewActivity.class);
		Bundle bundle = new Bundle();
		bundle.putSerializable(ImagePreviewActivity.IMAGE_INFO, (Serializable) imageInfo);
		bundle.putInt(ImagePreviewActivity.CURRENT_ITEM, index);
		bundle.putString(ImagePreviewActivity.DOWNLOAD_FOLDER_NAME, folderName);
		intent.putExtras(bundle);
		context.startActivity(intent);
	}

	/**
	 * 下载当前图片到SD卡
	 */
	private void downloadCurrentImg() {
		String path = Environment.getExternalStorageDirectory() + "/" + downloadFolderName + "/";
		DownloadPictureUtil.downloadPicture(context, currentItemOriginPathUrl, path,
			System.currentTimeMillis() + ".jpeg");
	}

	@Override public void onBackPressed() {
		finish();
	}

	@Override public boolean handleMessage(Message msg) {
		if (msg.what == 0) {// 点击查看原图按钮，开始加载原图
			final String path = imageInfoList.get(currentItem).getOriginUrl();
			Print.d(TAG, "handler == 0 path = " + path);
			visible();
			tv_show_origin.setText("0 %");

			Glide.with(this).load(path).downloadOnly(new ProgressTarget<String, File>(path, null) {
				@Override public void onProgress(String url, long bytesRead, long expectedLength) {
					int progress = (int) ((float) bytesRead * 100 / (float) expectedLength);
					Print.d(TAG, "OnProgress--->" + progress);

					if (bytesRead == expectedLength) {// 加载完成
						Message message = handlerHolder.obtainMessage();
						Bundle bundle = new Bundle();
						bundle.putString("url", url);
						message.what = 1;
						message.obj = bundle;
						handlerHolder.sendMessage(message);
					} else {// 加载中
						Message message = handlerHolder.obtainMessage();
						Bundle bundle = new Bundle();
						bundle.putString("url", url);
						bundle.putInt("progress", progress);
						message.what = 2;
						message.obj = bundle;
						handlerHolder.sendMessage(message);
					}
				}

				@Override public void onResourceReady(File resource, GlideAnimation<? super File> animation) {
					super.onResourceReady(resource, animation);
					Message message = handlerHolder.obtainMessage();
					Bundle bundle = new Bundle();
					bundle.putString("url", path);
					message.what = 1;
					message.obj = bundle;
					handlerHolder.sendMessage(message);
				}

				@Override public void getSize(SizeReadyCallback cb) {
					cb.onSizeReady(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
				}
			});
		} else if (msg.what == 1) {// 加载完成
			Print.d(TAG, "handler == 1");
			Bundle bundle = (Bundle) msg.obj;
			String url = bundle.getString("url");
			if (currentItem == getRealIndexWithPath(url)) {
				gone();
				imagePreviewAdapter.loadOrigin(url);
			}
		} else if (msg.what == 2) {// 加载中
			Bundle bundle = (Bundle) msg.obj;
			String url = bundle.getString("url");
			int progress = bundle.getInt("progress");
			if (currentItem == getRealIndexWithPath(url)) {
				visible();
				tv_show_origin.setText(progress + " %");
				Print.d(TAG, "handler == 2 progress == " + progress);
			}
		} else if (msg.what == 3) {// gone
			tv_show_origin.setText("查看原图");
			fm_image.setVisibility(View.GONE);
		} else if (msg.what == 4) {// visible
			fm_image.setVisibility(View.VISIBLE);
		}
		return true;
	}

	private int getRealIndexWithPath(String path) {
		for (int i = 0; i < imageInfoList.size(); i++) {
			if (path.equalsIgnoreCase(imageInfoList.get(i).getOriginUrl())) {
				return i;
			}
		}
		return 0;
	}

	private void checkCache(final String url_) {
		gone();
		new Thread(new Runnable() {
			@Override public void run() {
				File cacheFile = ImageLoader.getGlideCacheFile(context, url_);
				if (cacheFile != null && cacheFile.exists()) {
					gone();
				} else {
					visible();
				}
			}
		}).start();
	}

	@Override public void onClick(View v) {
		handlerHolder.sendEmptyMessage(0);
	}

	private void gone() {
		handlerHolder.sendEmptyMessage(3);
		Print.d(TAG, "------gone------");
	}

	private void visible() {
		handlerHolder.sendEmptyMessage(4);
		Print.d(TAG, "------visible------");
	}
}