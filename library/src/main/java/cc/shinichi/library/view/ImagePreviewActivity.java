package cc.shinichi.library.view;

import android.Manifest;
import android.content.Context;
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
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.R;
import cc.shinichi.library.bean.ImageInfo;
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
import java.util.List;

import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class ImagePreviewActivity extends AppCompatActivity
    implements Handler.Callback, View.OnClickListener {

  public static final String TAG = "ImagePreview";

  private Context context;

  private List<ImageInfo> imageInfoList;
  private int currentItem;// 当前显示的图片索引
  private String downloadFolderName = "";// 保存的文件夹名
  private boolean isShowDownButton;
  private boolean isShowOriginButton;

  private ImagePreviewAdapter imagePreviewAdapter;
  private HackyViewPager viewPager;
  private TextView tv_indicator;
  private FrameLayout fm_image;
  private TextView tv_show_origin;
  private ImageView img_download;

  private String currentItemOriginPathUrl = "";// 当前显示的原图链接
  private HandlerUtils.HandlerHolder handlerHolder;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_preview);
    context = this;
    handlerHolder = new HandlerUtils.HandlerHolder(this);

    imageInfoList = ImagePreview.getInstance().getImageInfoList();
    currentItem = ImagePreview.getInstance().getIndex();
    downloadFolderName = ImagePreview.getInstance().getFolderName();
    isShowDownButton = ImagePreview.getInstance().isShowDownButton();

    currentItemOriginPathUrl = imageInfoList.get(currentItem).getOriginUrl();

    isShowOriginButton = ImagePreview.getInstance().isShowOriginButton(currentItem);
    if (isShowOriginButton) {
      // 检查缓存是否存在
      checkCache(currentItemOriginPathUrl);
    }

    viewPager = findViewById(R.id.viewPager);
    tv_indicator = findViewById(R.id.tv_indicator);
    fm_image = findViewById(R.id.fm_image);
    tv_show_origin = findViewById(R.id.tv_show_origin);
    img_download = findViewById(R.id.img_download);
    // 查看与原图按钮
    tv_show_origin.setOnClickListener(this);
    // 下载图片按钮
    img_download.setOnClickListener(this);

    if (imageInfoList.size() > 1) {
      tv_indicator.setVisibility(View.VISIBLE);
    } else {
      tv_indicator.setVisibility(View.GONE);
    }

    if (isShowDownButton) {
      img_download.setVisibility(View.VISIBLE);
    } else {
      img_download.setVisibility(View.GONE);
    }

    // 更新进度指示器
    tv_indicator.setText(
        String.format(getString(R.string.indicator), currentItem + 1 + " ",
            " " + imageInfoList.size()));

    imagePreviewAdapter = new ImagePreviewAdapter(this, imageInfoList);
    viewPager.setAdapter(imagePreviewAdapter);
    viewPager.setCurrentItem(currentItem);
    viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
      @Override public void onPageSelected(int position) {
        currentItem = position;
        currentItemOriginPathUrl = imageInfoList.get(position).getOriginUrl();

        isShowOriginButton = ImagePreview.getInstance().isShowOriginButton(currentItem);
        if (isShowOriginButton) {
          // 检查缓存是否存在
          checkCache(currentItemOriginPathUrl);
        }
        // 更新进度指示器
        tv_indicator.setText(
            String.format(getString(R.string.indicator), currentItem + 1 + " ",
                " " + imageInfoList.size()));
      }
    });
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

        @Override
        public void onResourceReady(File resource, GlideAnimation<? super File> animation) {
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
    } else if (msg.what == 3) {
      tv_show_origin.setText("查看原图");
      fm_image.setVisibility(View.GONE);
    } else if (msg.what == 4) {
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
    int i = v.getId();
    if (i == R.id.img_download) {// 检查权限
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
    } else if (i == R.id.tv_show_origin) {
      handlerHolder.sendEmptyMessage(0);
    }
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

  @Override protected void onDestroy() {
    super.onDestroy();
    if (imagePreviewAdapter != null) {
      imagePreviewAdapter.closePage();
    }
    ImagePreview.getInstance().reset();
  }

  private void gone() {
    handlerHolder.sendEmptyMessage(3);
  }

  private void visible() {
    handlerHolder.sendEmptyMessage(4);
  }
}