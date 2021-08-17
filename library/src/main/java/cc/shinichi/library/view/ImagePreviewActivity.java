package cc.shinichi.library.view;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.transition.platform.MaterialContainerTransform;
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback;

import java.io.File;
import java.util.List;

import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.R;
import cc.shinichi.library.bean.ImageInfo;
import cc.shinichi.library.glide.FileTarget;
import cc.shinichi.library.glide.ImageLoader;
import cc.shinichi.library.glide.progress.OnProgressListener;
import cc.shinichi.library.glide.progress.ProgressManager;
import cc.shinichi.library.tool.common.HandlerUtils;
import cc.shinichi.library.tool.image.DownloadPictureUtil;
import cc.shinichi.library.tool.ui.ToastUtil;
import cc.shinichi.library.view.listener.OnDownloadClickListener;

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 */
public class ImagePreviewActivity extends AppCompatActivity implements Handler.Callback, View.OnClickListener {

    public static final String TAG = "ImagePreview";

    private Activity context;
    private HandlerUtils.HandlerHolder handlerHolder;

    private List<ImageInfo> imageInfoList;
    private int currentItem;
    private boolean isShowDownButton;
    private boolean isShowCloseButton;
    private boolean isShowOriginButton;
    private boolean isShowIndicator;

    private ImagePreviewAdapter imagePreviewAdapter;
    private HackyViewPager viewPager;

    private TextView tv_indicator;
    private FrameLayout fm_image_show_origin_container;
    private FrameLayout fm_center_progress_container;
    private Button btn_show_origin;
    private ImageView img_download;
    private ImageView imgCloseButton;
    private View rootView;
    private View progressParentLayout;

    private boolean isUserCustomProgressView = false;

    // 指示器显示状态
    private boolean indicatorStatus = false;
    // 原图按钮显示状态
    private boolean originalStatus = false;
    // 下载按钮显示状态
    private boolean downloadButtonStatus = false;
    // 关闭按钮显示状态
    private boolean closeButtonStatus = false;

    private String currentItemOriginPathUrl = "";
    private int lastProgress = 0;

    public static void activityStart(Context context) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent();
        intent.setClass(context, ImagePreviewActivity.class);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            // 过度动画效果只对安卓 5.0 以上有效
            View transitionView = ImagePreview.getInstance().getTransitionView();
            String transitionShareElementName = ImagePreview.getInstance().getTransitionShareElementName();
            // 如果未设置则使用默认动画
            if (transitionView != null && transitionShareElementName != null) {
                ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                        (Activity) context,
                        transitionView,
                        transitionShareElementName
                );
                context.startActivity(intent, options.toBundle());
            } else {
                context.startActivity(intent);
                if (context instanceof Activity) {
                    ((Activity) context).overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                }
            }
        } else {
            // 低于 5.0 使用默认动画
            context.startActivity(intent);
            if (context instanceof Activity) {
                ((Activity) context).overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 只有安卓版本大于 5.0 才可使用过度动画
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !TextUtils.isEmpty(ImagePreview.getInstance().getTransitionShareElementName())) {
            getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
            findViewById(android.R.id.content).setTransitionName("shared_element_container");
            setEnterSharedElementCallback(new MaterialContainerTransformSharedElementCallback());
            getWindow().setSharedElementEnterTransition(new MaterialContainerTransform().addTarget(android.R.id.content).setDuration(300L));
            getWindow().setSharedElementReturnTransition(new MaterialContainerTransform().addTarget(android.R.id.content).setDuration(250L));
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.sh_layout_preview);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
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
            return;
        }
        currentItem = ImagePreview.getInstance().getIndex();
        isShowDownButton = ImagePreview.getInstance().isShowDownButton();
        isShowCloseButton = ImagePreview.getInstance().isShowCloseButton();
        isShowIndicator = ImagePreview.getInstance().isShowIndicator();

        currentItemOriginPathUrl = imageInfoList.get(currentItem).getOriginUrl();

        isShowOriginButton = ImagePreview.getInstance().isShowOriginButton(currentItem);
        if (isShowOriginButton) {
            // 检查缓存是否存在
            checkCache(currentItemOriginPathUrl);
        }

        rootView = findViewById(R.id.rootView);
        viewPager = findViewById(R.id.viewPager);
        tv_indicator = findViewById(R.id.tv_indicator);

        fm_image_show_origin_container = findViewById(R.id.fm_image_show_origin_container);
        fm_center_progress_container = findViewById(R.id.fm_center_progress_container);

        fm_image_show_origin_container.setVisibility(View.GONE);
        fm_center_progress_container.setVisibility(View.GONE);

        int progressLayoutId = ImagePreview.getInstance().getProgressLayoutId();
        // != -1 即用户自定义了view
        if (progressLayoutId != -1) {
            // add用户自定义的view到frameLayout中，回调进度和view
            progressParentLayout = View.inflate(context, ImagePreview.getInstance().getProgressLayoutId(), null);
            if (progressParentLayout != null) {
                fm_center_progress_container.removeAllViews();
                fm_center_progress_container.addView(progressParentLayout);
                isUserCustomProgressView = true;
            } else {
                // 使用默认的textView进行百分比的显示
                isUserCustomProgressView = false;
            }
        } else {
            // 使用默认的textView进行百分比的显示
            isUserCustomProgressView = false;
        }

        btn_show_origin = findViewById(R.id.btn_show_origin);
        img_download = findViewById(R.id.img_download);
        imgCloseButton = findViewById(R.id.imgCloseButton);

        img_download.setImageResource(ImagePreview.getInstance().getDownIconResId());
        imgCloseButton.setImageResource(ImagePreview.getInstance().getCloseIconResId());

        // 关闭页面按钮
        imgCloseButton.setOnClickListener(this);
        // 查看与原图按钮
        btn_show_origin.setOnClickListener(this);
        // 下载图片按钮
        img_download.setOnClickListener(this);

        if (!isShowIndicator) {
            tv_indicator.setVisibility(View.GONE);
            indicatorStatus = false;
        } else {
            if (imageInfoList.size() > 1) {
                tv_indicator.setVisibility(View.VISIBLE);
                indicatorStatus = true;
            } else {
                tv_indicator.setVisibility(View.GONE);
                indicatorStatus = false;
            }
        }
        // 设置顶部指示器背景shape
        if (ImagePreview.getInstance().getIndicatorShapeResId() > 0) {
            tv_indicator.setBackgroundResource(ImagePreview.getInstance().getIndicatorShapeResId());
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
        tv_indicator.setText(String.format(getString(R.string.indicator), currentItem + 1 + "", "" + imageInfoList.size()));

        imagePreviewAdapter = new ImagePreviewAdapter(this, imageInfoList);
        viewPager.setAdapter(imagePreviewAdapter);
        viewPager.setCurrentItem(currentItem);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (ImagePreview.getInstance().getBigImagePageChangeListener() != null) {
                    ImagePreview.getInstance().getBigImagePageChangeListener().onPageSelected(position);
                }
                currentItem = position;
                currentItemOriginPathUrl = imageInfoList.get(position).getOriginUrl();

                isShowOriginButton = ImagePreview.getInstance().isShowOriginButton(currentItem);
                if (isShowOriginButton) {
                    // 检查缓存是否存在
                    checkCache(currentItemOriginPathUrl);
                } else {
                    gone();
                }
                // 更新进度指示器
                tv_indicator.setText(String.format(getString(R.string.indicator), currentItem + 1 + "", "" + imageInfoList.size()));
                // 如果是自定义百分比进度view，每次切换都先隐藏，并重置百分比
                if (isUserCustomProgressView) {
                    fm_center_progress_container.setVisibility(View.GONE);
                    lastProgress = 0;
                }
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);

                if (ImagePreview.getInstance().getBigImagePageChangeListener() != null) {
                    ImagePreview.getInstance()
                            .getBigImagePageChangeListener()
                            .onPageScrolled(position, positionOffset, positionOffsetPixels);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);

                if (ImagePreview.getInstance().getBigImagePageChangeListener() != null) {
                    ImagePreview.getInstance().getBigImagePageChangeListener().onPageScrollStateChanged(state);
                }
            }
        });
    }

    /**
     * 下载当前图片到SD卡
     */
    private void downloadCurrentImg() {
        DownloadPictureUtil.downloadPicture(context.getApplicationContext(), currentItemOriginPathUrl);
    }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            supportFinishAfterTransition();
        } else {
            finish();
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        ImagePreview.getInstance().reset();
        if (imagePreviewAdapter != null) {
            imagePreviewAdapter.closePage();
        }
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
                fm_image_show_origin_container.setVisibility(View.VISIBLE);
            }
            if (downloadButtonStatus) {
                img_download.setVisibility(View.VISIBLE);
            }
            if (closeButtonStatus) {
                imgCloseButton.setVisibility(View.VISIBLE);
            }
        } else {
            tv_indicator.setVisibility(View.GONE);
            fm_image_show_origin_container.setVisibility(View.GONE);
            img_download.setVisibility(View.GONE);
            imgCloseButton.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == 0) {
            // 点击查看原图按钮，开始加载原图
            final String path = imageInfoList.get(currentItem).getOriginUrl();
            visible();
            if (isUserCustomProgressView) {
                gone();
            } else {
                btn_show_origin.setText("0 %");
            }

            if (checkCache(path)) {
                Message message = handlerHolder.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putString("url", path);
                message.what = 1;
                message.obj = bundle;
                handlerHolder.sendMessage(message);
                return true;
            }
            loadOriginImage(path);
        } else if (msg.what == 1) {
            // 加载完成
            Bundle bundle = (Bundle) msg.obj;
            String url = bundle.getString("url");
            gone();
            if (currentItem == getRealIndexWithPath(url)) {
                if (isUserCustomProgressView) {
                    fm_center_progress_container.setVisibility(View.GONE);
                    if (ImagePreview.getInstance().getOnOriginProgressListener() != null) {
                        progressParentLayout.setVisibility(View.GONE);
                        ImagePreview.getInstance().getOnOriginProgressListener().finish(progressParentLayout);
                    }
                    imagePreviewAdapter.loadOrigin(imageInfoList.get(currentItem));
                } else {
                    imagePreviewAdapter.loadOrigin(imageInfoList.get(currentItem));
                }
            }
        } else if (msg.what == 2) {
            // 加载中
            Bundle bundle = (Bundle) msg.obj;
            String url = bundle.getString("url");
            int progress = bundle.getInt("progress");
            if (currentItem == getRealIndexWithPath(url)) {
                if (isUserCustomProgressView) {
                    gone();
                    fm_center_progress_container.setVisibility(View.VISIBLE);
                    if (ImagePreview.getInstance().getOnOriginProgressListener() != null) {
                        progressParentLayout.setVisibility(View.VISIBLE);
                        ImagePreview.getInstance()
                                .getOnOriginProgressListener()
                                .progress(progressParentLayout, progress);
                    }
                } else {
                    visible();
                    btn_show_origin.setText(String.format("%s %%", progress));
                }
            }
        } else if (msg.what == 3) {
            // 隐藏查看原图按钮
            btn_show_origin.setText(R.string.btn_original);
            fm_image_show_origin_container.setVisibility(View.GONE);
            originalStatus = false;
        } else if (msg.what == 4) {
            // 显示查看原图按钮
            fm_image_show_origin_container.setVisibility(View.VISIBLE);
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

    private boolean checkCache(String url) {
        File cacheFile = ImageLoader.getGlideCacheFile(context, url);
        if (cacheFile != null && cacheFile.exists()) {
            gone();
            return true;
        } else {
            visible();
            return false;
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.img_download) {
            OnDownloadClickListener downloadClickListener = ImagePreview.getInstance().getDownloadClickListener();
            if (downloadClickListener != null) {
                boolean interceptDownload = downloadClickListener.isInterceptDownload();
                if (interceptDownload) {
                    // 拦截了下载，不执行下载
                } else {
                    // 没有拦截下载
                    checkAndDownload();
                }
                ImagePreview.getInstance().getDownloadClickListener().onClick(context, v, currentItem);
            } else {
                checkAndDownload();
            }
        } else if (i == R.id.btn_show_origin) {
            handlerHolder.sendEmptyMessage(0);
        } else if (i == R.id.imgCloseButton) {
            onBackPressed();
        }
    }

    private void checkAndDownload() {
        // 检查权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(ImagePreviewActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // 拒绝权限
                ToastUtil.getInstance()._short(context, getString(R.string.toast_deny_permission_save_failed));
            } else {
                //申请权限
                ActivityCompat.requestPermissions(ImagePreviewActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,}, 1);
            }
        } else {
            // 下载当前图片
            downloadCurrentImg();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 1) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PERMISSION_GRANTED) {
                    downloadCurrentImg();
                } else {
                    ToastUtil.getInstance()._short(context, getString(R.string.toast_deny_permission_save_failed));
                }
            }
        }
    }

    private void gone() {
        handlerHolder.sendEmptyMessage(3);
    }

    private void visible() {
        handlerHolder.sendEmptyMessage(4);
    }

    private void loadOriginImage(final String path) {
        Glide.with(context).downloadOnly().load(path).into(new FileTarget() {
            @Override
            public void onResourceReady(@NonNull File resource, @Nullable Transition<? super File> transition) {
                super.onResourceReady(resource, transition);
            }
        });

        ProgressManager.addListener(path, new OnProgressListener() {
            @Override
            public void onProgress(String url, boolean isComplete, int percentage, long bytesRead, long totalBytes) {
                if (isComplete) {// 加载完成
                    Message message = handlerHolder.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putString("url", url);
                    message.what = 1;
                    message.obj = bundle;
                    handlerHolder.sendMessage(message);
                } else {// 加载中，为减少回调次数，此处做判断，如果和上次的百分比一致就跳过
                    if (percentage == lastProgress) {
                        return;
                    }
                    lastProgress = percentage;
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
    }
}