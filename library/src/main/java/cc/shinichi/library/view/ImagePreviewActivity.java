package cc.shinichi.library.view;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.R;
import cc.shinichi.library.bean.ImageInfo;
import cc.shinichi.library.glide.ImageLoader;
import cc.shinichi.library.glide.sunfusheng.progress.OnProgressListener;
import cc.shinichi.library.glide.sunfusheng.progress.ProgressManager;
import cc.shinichi.library.tool.DownloadPictureUtil;
import cc.shinichi.sherlockutillibrary.utility.common.HandlerUtils;
import cc.shinichi.sherlockutillibrary.utility.file.FileUtil;
import cc.shinichi.sherlockutillibrary.utility.ui.ToastUtil;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
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
  private boolean isShowCloseButton;
  private boolean isShowOriginButton;

  private ImagePreviewAdapter imagePreviewAdapter;
  private HackyViewPager viewPager;
  private TextView tv_indicator;
  private FrameLayout fm_image;
  private TextView tv_show_origin;
  private ImageView img_download;
  private ImageView imgCloseButton;
  private View rootView;

  // 指示器显示状态
  private boolean indicatorStatus = false;
  // 原图按钮显示状态
  private boolean originalStatus = false;
  // 下载按钮显示状态
  private boolean downloadButtonStatus = false;
  // 关闭按钮显示状态
  private boolean closeButtonStatus = false;

  private String currentItemOriginPathUrl = "";// 当前显示的原图链接
  private HandlerUtils.HandlerHolder handlerHolder;

  public static void activityStart(Context context) {
    if (context == null) {
      return;
    }
    Intent intent = new Intent();
    intent.setClass(context, ImagePreviewActivity.class);
    context.startActivity(intent);
    ((Activity) context).overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_preview);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      Window window = getWindow();
      window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      window.getDecorView()
          .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
      window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      window.setStatusBarColor(Color.TRANSPARENT);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    }

    context = this;
    handlerHolder = new HandlerUtils.HandlerHolder(this);

    imageInfoList = ImagePreview.getInstance().getImageInfoList();
    if (null == imageInfoList || imageInfoList.size() == 0) {
      onBackPressed();
    }
    currentItem = ImagePreview.getInstance().getIndex();
    downloadFolderName = ImagePreview.getInstance().getFolderName();
    isShowDownButton = ImagePreview.getInstance().isShowDownButton();
    isShowCloseButton = ImagePreview.getInstance().isShowCloseButton();

    currentItemOriginPathUrl = imageInfoList.get(currentItem).getOriginUrl();

    isShowOriginButton = ImagePreview.getInstance().isShowOriginButton(currentItem);
    if (isShowOriginButton) {
      // 检查缓存是否存在
      checkCache(currentItemOriginPathUrl);
    }

    rootView = findViewById(R.id.rootView);
    viewPager = findViewById(R.id.viewPager);
    tv_indicator = findViewById(R.id.tv_indicator);
    fm_image = findViewById(R.id.fm_image);
    tv_show_origin = findViewById(R.id.tv_show_origin);
    img_download = findViewById(R.id.img_download);
    imgCloseButton = findViewById(R.id.imgCloseButton);

    // 关闭页面按钮
    imgCloseButton.setOnClickListener(this);
    // 查看与原图按钮
    tv_show_origin.setOnClickListener(this);
    // 下载图片按钮
    img_download.setOnClickListener(this);

    if (imageInfoList.size() > 1) {
      tv_indicator.setVisibility(View.VISIBLE);
      indicatorStatus = true;
    } else {
      tv_indicator.setVisibility(View.GONE);
      indicatorStatus = false;
    }

    if (isShowDownButton) {
      img_download.setVisibility(View.VISIBLE);
      downloadButtonStatus = true;
    } else {
      img_download.setVisibility(View.GONE);
      downloadButtonStatus = false;
    }

    if (isShowCloseButton) {
      imgCloseButton.setVisibility(View.VISIBLE);
      closeButtonStatus = true;
    } else {
      imgCloseButton.setVisibility(View.GONE);
      closeButtonStatus = false;
    }

    // 更新进度指示器
    tv_indicator.setText(
        String.format(getString(R.string.indicator), currentItem + 1 + "", "" + imageInfoList.size()));

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
            String.format(getString(R.string.indicator), currentItem + 1 + "", "" + imageInfoList.size()));
      }
    });
  }

  /**
   * 下载当前图片到SD卡
   */
  private void downloadCurrentImg() {
    String path = Environment.getExternalStorageDirectory() + "/" + downloadFolderName + "/";
    String name = currentItemOriginPathUrl.substring(currentItemOriginPathUrl.lastIndexOf("/") + 1, currentItemOriginPathUrl.length());
    FileUtil.createFileByDeleteOldFile(path + name);
    DownloadPictureUtil.downloadPicture(context, currentItemOriginPathUrl, path, name);
  }

  @Override public void onBackPressed() {
    finish();
  }

  @Override public void finish() {
    super.finish();
    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
  }


  public int convertPercentToBlackAlphaColor(float percent) {
    percent = Math.min(1, Math.max(0, percent));
    int intAlpha = (int) (percent * 255);
    String stringAlpha = Integer.toHexString(intAlpha).toLowerCase();
    String color = "#" + (stringAlpha.length() < 2 ? "0" : "") + stringAlpha + "000000";
    return Color.parseColor(color);
  }

  public void setAlpha(float alpha) {
    int colorId = convertPercentToBlackAlphaColor(alpha);
    rootView.setBackgroundColor(colorId);
    if (alpha >= 1) {
      if (indicatorStatus) {
        tv_indicator.setVisibility(View.VISIBLE);
      }
      if (originalStatus) {
        fm_image.setVisibility(View.VISIBLE);
      }
      if (downloadButtonStatus) {
        img_download.setVisibility(View.VISIBLE);
      }
      if (closeButtonStatus) {
        imgCloseButton.setVisibility(View.VISIBLE);
      }
    } else {
      tv_indicator.setVisibility(View.GONE);
      fm_image.setVisibility(View.GONE);
      img_download.setVisibility(View.GONE);
      imgCloseButton.setVisibility(View.GONE);
    }
  }

  @Override public boolean handleMessage(Message msg) {
    if (msg.what == 0) {// 点击查看原图按钮，开始加载原图
      final String path = imageInfoList.get(currentItem).getOriginUrl();
      visible();
      tv_show_origin.setText("0 %");

      if (checkCache(path)) {
        Message message = handlerHolder.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("url", path);
        message.what = 1;
        message.obj = bundle;
        handlerHolder.sendMessage(message);
        return true;
      }

      Glide.with(context).load(path).into(new SimpleTarget<Drawable>() {
        @Override public void onResourceReady(@NonNull Drawable resource,
            @Nullable Transition<? super Drawable> transition) {
        }
      });

      ProgressManager.addListener(path, new OnProgressListener() {
        @Override
        public void onProgress(String url, boolean isComplete, int percentage, long bytesRead,
            long totalBytes) {
              if (isComplete) {// 加载完成
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
                bundle.putInt("progress", percentage);
                message.what = 2;
                message.obj = bundle;
                handlerHolder.sendMessage(message);
              }
            }
        });
    } else if (msg.what == 1) {// 加载完成
      Bundle bundle = (Bundle) msg.obj;
      String url = bundle.getString("url");
      gone();
      if (currentItem == getRealIndexWithPath(url)) {
        imagePreviewAdapter.loadOrigin(imageInfoList.get(currentItem));
      }
    } else if (msg.what == 2) {// 加载中
      Bundle bundle = (Bundle) msg.obj;
      String url = bundle.getString("url");
      int progress = bundle.getInt("progress");
      if (currentItem == getRealIndexWithPath(url)) {
        visible();
        tv_show_origin.setText(progress + " %");
      }
    } else if (msg.what == 3) {
      tv_show_origin.setText("查看原图");
      fm_image.setVisibility(View.GONE);
      originalStatus = false;
    } else if (msg.what == 4) {
      fm_image.setVisibility(View.VISIBLE);
      originalStatus = true;
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

  private boolean checkCache(String url_) {
    gone();
    File cacheFile = ImageLoader.getGlideCacheFile(context, url_);
    if (cacheFile != null && cacheFile.exists()) {
      gone();
      return true;
    } else {
      visible();
      return false;
    }
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
    } else if (i == R.id.imgCloseButton) {
      onBackPressed();
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