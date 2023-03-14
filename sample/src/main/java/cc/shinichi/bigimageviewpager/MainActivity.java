package cc.shinichi.bigimageviewpager;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import java.util.ArrayList;
import java.util.List;

import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.bean.ImageInfo;
import cc.shinichi.library.glide.ImageLoader;
import cc.shinichi.library.tool.ui.ToastUtil;
import cc.shinichi.library.view.listener.OnBigImageClickListener;
import cc.shinichi.library.view.listener.OnBigImageLongClickListener;
import cc.shinichi.library.view.listener.OnBigImagePageChangeListener;
import cc.shinichi.library.view.listener.OnDownloadClickListener;
import cc.shinichi.library.view.listener.OnDownloadStateListener;
import cc.shinichi.library.view.listener.OnOriginProgressListener;
import cc.shinichi.library.view.listener.OnPageFinishListener;

import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    boolean enableClickClose = false;
    boolean enableDragClose = true;
    boolean enableUpDragClose = false;
    boolean enableDragIgnoreScale = true;

    boolean showIndicator = false;
    boolean showCloseButton = false;
    boolean showDownButton = false;
    boolean showErrorToast = false;

    private ImagePreview.LoadStrategy loadStrategy = ImagePreview.LoadStrategy.Default;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
    }

    private void initView() {
        SwitchCompat switchClickClose = findViewById(R.id.switchClickClose);
        SwitchCompat switchDragClose = findViewById(R.id.switchDragClose);
        SwitchCompat switchUpDragClose = findViewById(R.id.switchUpDragClose);
        SwitchCompat switchDragCloseIgnore = findViewById(R.id.switchDragCloseIgnore);
        SwitchCompat switchShowIndicator = findViewById(R.id.switchShowIndicator);
        SwitchCompat switchShowCloseButton = findViewById(R.id.switchShowCloseButton);
        SwitchCompat switchShowDownButton = findViewById(R.id.switchShowDownButton);
        SwitchCompat switchShowErrorToast = findViewById(R.id.switchShowErrorToast);
        RadioGroup radioGroupStrategy = findViewById(R.id.radioGroupStrategy);

        switchClickClose.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableClickClose = isChecked;
            }
        });
        switchClickClose.setChecked(true);

        switchDragClose.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableDragClose = isChecked;
            }
        });
        switchDragClose.setChecked(true);

        switchUpDragClose.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableUpDragClose = isChecked;
            }
        });
        switchUpDragClose.setChecked(false);

        switchDragCloseIgnore.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableDragIgnoreScale = isChecked;
            }
        });
        switchDragCloseIgnore.setChecked(true);

        switchShowIndicator.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showIndicator = isChecked;
            }
        });
        switchShowIndicator.setChecked(true);

        switchShowCloseButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showCloseButton = isChecked;
            }
        });
        switchShowCloseButton.setChecked(true);

        switchShowDownButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showDownButton = isChecked;
            }
        });
        switchShowDownButton.setChecked(true);
        switchShowErrorToast.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showErrorToast = isChecked;
            }
        });
        switchShowErrorToast.setChecked(false);

        radioGroupStrategy.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radioThumb:
                        loadStrategy = ImagePreview.LoadStrategy.AlwaysThumb;
                        break;
                    case R.id.radioOrigin:
                        loadStrategy = ImagePreview.LoadStrategy.AlwaysOrigin;
                        break;
                    case R.id.radioDefault:
                        loadStrategy = ImagePreview.LoadStrategy.Default;
                        break;
                    case R.id.radioNetAuto:
                        loadStrategy = ImagePreview.LoadStrategy.NetworkAuto;
                        break;
                    default:
                        loadStrategy = ImagePreview.LoadStrategy.Default;
                        break;
                }
            }
        });
    }

    private void initData() {
        final List<ImageInfo> imageInfoList = new ArrayList<>();
        ImageInfo i;

        // 普通图片1：
        i = new ImageInfo();
        i.setThumbnailUrl("https://star.sea.img.one/2023/03/01/63fef5545d6b6.jpg");
        i.setOriginUrl("https://star.sea.img.one/2023/03/01/63fef3fe5a07c.jpg");
        imageInfoList.add(i);

        // 普通图片2：
        i = new ImageInfo();
        i.setThumbnailUrl("https://star.sea.img.one/2023/03/01/63fef554e6408.jpg");
        i.setOriginUrl("https://star.sea.img.one/2023/03/01/63fef3ff0cdfc.jpg");
        imageInfoList.add(i);

        // 大尺寸图片：
        i = new ImageInfo();
        i.setThumbnailUrl("https://i.328888.xyz/2022/12/21/A3ZD8.md.jpeg");
        i.setOriginUrl("https://i.328888.xyz/2022/12/21/A3ZD8.jpeg");
        imageInfoList.add(i);

        // 长截图1：
        i = new ImageInfo();
        i.setThumbnailUrl("https://star.sea.img.one/2023/03/01/63fef5d3374d2.jpg");
        i.setOriginUrl("https://star.sea.img.one/2023/03/01/63fef5db1046e.jpg");
        imageInfoList.add(i);

        // 全景图片1：
        i = new ImageInfo();
        i.setThumbnailUrl("https://star.sea.img.one/2023/03/01/63fef55653a50.jpg");
        i.setOriginUrl("https://star.sea.img.one/2023/03/01/63fef40fb5cf9.jpg");
        imageInfoList.add(i);

        // 全景图片2：
        i = new ImageInfo();
        i.setThumbnailUrl("https://star.sea.img.one/2023/03/01/63fef556078ff.jpg");
        i.setOriginUrl("https://star.sea.img.one/2023/03/01/63fef40388f58.jpg");
        imageInfoList.add(i);

        // 动图：
        i = new ImageInfo();
        i.setThumbnailUrl("https://i.328888.xyz/2022/12/23/AQKsV.png");
        i.setOriginUrl("https://i0.hdslb.com/bfs/article/4421aaa8a38beeda1b195b656c883c7508f9b13d.gif");
        imageInfoList.add(i);

        // 动图：
        i = new ImageInfo();
        i.setThumbnailUrl("https://i.328888.xyz/2022/12/23/AQKsV.png");
        i.setOriginUrl("https://s3.bmp.ovh/imgs/2023/03/01/219a4fef0bae6867.jpg");
        imageInfoList.add(i);


        // ==============================================================================================================
        // 一、最简单的调用：
        findViewById(R.id.buttonEasyUse).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 一行代码即可实现大部分需求。如需定制，可参考下面【三、完全自定义调用】自定义的代码：
                ImagePreview.getInstance().setContext(MainActivity.this).setImageInfoList(imageInfoList).start();
            }
        });
        // ==============================================================================================================




        // ==============================================================================================================
        // 二、完全自定义调用：
        findViewById(R.id.buttonPreview).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 完全自定义配置
                ImagePreview.getInstance()
                        // 上下文，必须是activity，不需要担心内存泄漏，本框架已经处理好
                        .setContext(MainActivity.this)
                        // 从第几张图片开始，索引从0开始哦~
                        .setIndex(0)

                        //=================================================================================================
                        // 有三种设置数据集合的方式，根据自己的需求进行三选一：
                        // 1：第一步生成的imageInfo List
                        .setImageInfoList(imageInfoList)

                        // 2：直接传url List
                        //.setImageList(List<String> imageList)

                        // 3：只有一张图片的情况，可以直接传入这张图片的url
                        //.setImage(String image)
                        //=================================================================================================

                        // 加载策略，默认为手动模式
                        .setLoadStrategy(loadStrategy)

                        // 保存的文件夹名称，会在Picture目录进行文件夹的新建。比如："BigImageView"，会在Picture目录新建BigImageView文件夹)
                        .setFolderName("BigImageView")

                        // 缩放动画时长，单位ms
                        .setZoomTransitionDuration(300)

                        // 是否显示加载失败的Toast
                        .setShowErrorToast(showErrorToast)

                        // 是否启用点击图片关闭。默认启用
                        .setEnableClickClose(enableClickClose)
                        // 是否启用下拉关闭。默认不启用
                        .setEnableDragClose(enableDragClose)
                        // 是否启用上拉关闭。默认不启用
                        .setEnableUpDragClose(enableUpDragClose)
                        // 是否忽略缩放启用拉动关闭。默认不忽略
                        .setEnableDragCloseIgnoreScale(enableDragIgnoreScale)

                        // 是否显示关闭页面按钮，在页面左下角。默认不显示
                        .setShowCloseButton(showCloseButton)
                        // 设置关闭按钮图片资源，可不填，默认为库中自带：R.drawable.ic_action_close
                        .setCloseIconResId(R.drawable.ic_action_close)

                        // 是否显示下载按钮，在页面右下角。默认显示
                        .setShowDownButton(showDownButton)
                        // 设置下载按钮图片资源，可不填，默认为库中自带：R.drawable.icon_download_new
                        .setDownIconResId(R.drawable.icon_download_new)

                        // 设置是否显示顶部的指示器（1/9）默认显示
                        .setShowIndicator(showIndicator)
                        // 设置顶部指示器背景shape，默认自带灰色圆角shape
                        .setIndicatorShapeResId(R.drawable.shape_indicator_bg)

                        // 设置失败时的占位图，默认为库中自带R.drawable.load_failed，设置为 0 时不显示
                        .setErrorPlaceHolder(R.drawable.load_failed)

                        // 点击回调
                        .setBigImageClickListener(new OnBigImageClickListener() {
                            @Override
                            public void onClick(Activity activity, View view, int position) {
                                // ...
                                Log.d(TAG, "onClick: ");
                            }
                        })
                        // 长按回调
                        .setBigImageLongClickListener(new OnBigImageLongClickListener() {
                            @Override
                            public boolean onLongClick(Activity activity, View view, int position) {
                                // ...
                                Log.d(TAG, "onLongClick: ");
                                return false;
                            }
                        })
                        // 页面切换回调
                        .setBigImagePageChangeListener(new OnBigImagePageChangeListener() {
                            @Override
                            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                                Log.d(TAG, "onPageScrolled: ");
                            }

                            @Override
                            public void onPageSelected(int position) {
                                Log.d(TAG, "onPageSelected: ");
                            }

                            @Override
                            public void onPageScrollStateChanged(int state) {
                                Log.d(TAG, "onPageScrollStateChanged: ");
                            }
                        })
                        // 下载按钮点击回调，可以拦截下载逻辑，从而实现自己下载或埋点统计
                        .setDownloadClickListener(new OnDownloadClickListener() {
                            @Override
                            public void onClick(Activity activity, View view, int position) {
                                // 可以在此处执行您自己的下载逻辑、埋点统计等信息
                                Log.d(TAG, "onDownloadClick: position = " + position);
                            }

                            @Override
                            public boolean isInterceptDownload() {
                                // return true 时, 需要自己实现下载
                                // return false 时, 使用内置下载
                                return false;
                            }
                        })
                        // 下载过程回调，可自定义toast，如果不设置此回调会使用默认的toast内容
                        .setOnDownloadStateListener(new OnDownloadStateListener() {
                            @Override
                            public void onDownloadStart(Activity activity, int position) {
                                Toast.makeText(activity, "开始下载", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onDownloadSuccess(Activity activity, int position) {
                                Toast.makeText(activity, "下载成功", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onDownloadFailed(Activity activity, int position) {
                                Toast.makeText(activity, "下载失败", Toast.LENGTH_SHORT).show();
                            }
                        })
                        // 页面关闭回调
                        .setOnPageFinishListener(new OnPageFinishListener() {
                            @Override
                            public void onFinish(@NonNull Activity activity) {
                                // ...
                                Log.d(TAG, "onFinish: ");
                            }
                        })

                        //=================================================================================================
                        // 设置查看原图时的百分比样式：库中带有一个样式：ImagePreview.PROGRESS_THEME_CIRCLE_TEXT，使用如下：
                        .setProgressLayoutId(ImagePreview.PROGRESS_THEME_CIRCLE_TEXT, new OnOriginProgressListener() {
                            @Override
                            public void progress(View parentView, int progress) {
                                Log.d(TAG, "progress: " + progress);

                                // 需要找到进度控件并设置百分比，回调中的parentView即传入的布局的根View，可通过parentView找到控件：
                                ProgressBar progressBar = parentView.findViewById(R.id.sh_progress_view);
                                TextView textView = parentView.findViewById(R.id.sh_progress_text);
                                progressBar.setProgress(progress);
                                String progressText = progress + "%";
                                textView.setText(progressText);
                            }

                            @Override
                            public void finish(View parentView) {
                                Log.d(TAG, "finish: ");
                            }
                        })

                        // 使用自定义百分比样式，传入自己的布局，并设置回调，再根据parentView找到进度控件进行百分比的设置：
                        //.setProgressLayoutId(R.layout.image_progress_layout_theme_1, new OnOriginProgressListener() {
                        //    @Override public void progress(View parentView, int progress) {
                        //        Log.d(TAG, "progress: " + progress);
                        //
                        //        ProgressBar progressBar = parentView.findViewById(R.id.progress_horizontal);
                        //        progressBar.setProgress(progress);
                        //    }
                        //
                        //    @Override public void finish(View parentView) {
                        //        Log.d(TAG, "finish: ");
                        //    }
                        //})
                        //=================================================================================================

                        // 开启预览
                        .start();
            }
        });
        // ==============================================================================================================



        // ==============================================================================================================
        // 四、通过相册选择图片进行预览
        findViewById(R.id.buttonChoose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this.getApplicationContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        // 拒绝权限
                        ToastUtil.getInstance()._short(MainActivity.this.getApplicationContext(), "您拒绝了存储权限，无法读取图片！");
                    } else {
                        // 申请权限
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,}, 1);
                    }
                } else {
                    // 选择图片
                    chooseImage();
                }
            }
        });
        // ==============================================================================================================



        // ==============================================================================================================
        // 清除磁盘缓存
        findViewById(R.id.buttonClean).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageLoader.cleanDiskCache(MainActivity.this);
                ToastUtil.getInstance()._short(MainActivity.this, "磁盘缓存已成功清除");
            }
        });
        // ==============================================================================================================
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PERMISSION_GRANTED) {
                    chooseImage();
                } else {
                    ToastUtil.getInstance()._short(MainActivity.this.getApplicationContext(), "您拒绝了存储权限，无法读取图片！");
                }
            }
        }
    }

    // 去选择图片
    private void chooseImage() {
        Matisse.from(MainActivity.this)
                .choose(MimeType.ofImage())
                .capture(true)
                .captureStrategy(new CaptureStrategy(true, "cc.shinichi.bigimageviewpager.fileprovider", "BigImage"))
                .countable(true)
                .maxSelectable(30)
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                .thumbnailScale(0.85f)
                .imageEngine(new GlideEngine())
                .theme(com.zhihu.matisse.R.style.Matisse_Zhihu)
                .showSingleMediaType(true)
                .originalEnable(true)
                .forResult(1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK && data != null) {
                List<Uri> uriList = Matisse.obtainResult(data);
                List<String> urlList = new ArrayList<>();
                for (Uri uri : uriList) {
                    urlList.add(uri.toString());
                }
                ImagePreview.getInstance()
                        .setContext(MainActivity.this)
                        .setImageList(urlList)
                        .setShowDownButton(false)
                        .setShowCloseButton(false)
                        .setEnableDragClose(true)
                        .setEnableClickClose(false)
                        .start();
            }
        }
    }
}