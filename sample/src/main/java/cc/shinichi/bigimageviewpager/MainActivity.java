package cc.shinichi.bigimageviewpager;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback;
import com.luck.picture.lib.basic.PictureSelector;
import com.luck.picture.lib.config.SelectMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.interfaces.OnResultCallbackListener;

import java.util.ArrayList;
import java.util.List;

import cc.shinichi.bigimageviewpager.glide.GlideEngine;
import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.bean.ImageInfo;
import cc.shinichi.library.glide.ImageLoader;
import cc.shinichi.library.tool.ui.ToastUtil;
import cc.shinichi.library.view.listener.OnBigImageClickListener;
import cc.shinichi.library.view.listener.OnBigImageLongClickListener;
import cc.shinichi.library.view.listener.OnBigImagePageChangeListener;
import cc.shinichi.library.view.listener.OnCustomLayoutCallback;
import cc.shinichi.library.view.listener.OnDownloadClickListener;
import cc.shinichi.library.view.listener.OnDownloadListener;
import cc.shinichi.library.view.listener.OnOriginProgressListener;
import cc.shinichi.library.view.listener.OnPageFinishListener;

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
            setExitSharedElementCallback(new MaterialContainerTransformSharedElementCallback());
            getWindow().setSharedElementsUseOverlay(false);
        }

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
                    case R.id.radioAuto:
                        loadStrategy = ImagePreview.LoadStrategy.Auto;
                        break;
                    default:
                        loadStrategy = ImagePreview.LoadStrategy.Auto;
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
        i.setThumbnailUrl("http://image.coolapk.com/picture/2023/0331/15/804026_4012f28c_8643_0867_309@1440x2560.jpeg.m.jpg");
        i.setOriginUrl("http://image.coolapk.com/picture/2023/0331/15/804026_4012f28c_8643_0867_309@1440x2560.jpeg");
        imageInfoList.add(i);

        // 普通图片2：
        i = new ImageInfo();
        i.setThumbnailUrl("http://image.coolapk.com/picture/2023/0406/08/736620_39842c8b_0392_4907_446@2160x3840.jpeg.m.jpg");
        i.setOriginUrl("http://image.coolapk.com/picture/2023/0406/08/736620_39842c8b_0392_4907_446@2160x3840.jpeg");
        imageInfoList.add(i);

        // 大尺寸图片：
        i = new ImageInfo();
        i.setThumbnailUrl("https://yitaoyitao.oss-cn-qingdao.aliyuncs.com/app/img/temp/test/A3ZD8.md.jpg");
        i.setOriginUrl("https://yitaoyitao.oss-cn-qingdao.aliyuncs.com/app/img/temp/test/A3ZD8.jpg");
        imageInfoList.add(i);

        // 长截图1：
        i = new ImageInfo();
        i.setThumbnailUrl("https://yitaoyitao.oss-cn-qingdao.aliyuncs.com/app/img/temp/test/llong1thubm.jpg");
        i.setOriginUrl("https://yitaoyitao.oss-cn-qingdao.aliyuncs.com/app/img/temp/test/llong1.jpg");
        imageInfoList.add(i);

        // 全景图片1：
        i = new ImageInfo();
        i.setThumbnailUrl("https://yitaoyitao.oss-cn-qingdao.aliyuncs.com/app/img/temp/test/heng1thumb.jpg");
        i.setOriginUrl("https://yitaoyitao.oss-cn-qingdao.aliyuncs.com/app/img/temp/test/heng1.jpg");
        imageInfoList.add(i);

        // 全景图片2：
        i = new ImageInfo();
        i.setThumbnailUrl("https://yitaoyitao.oss-cn-qingdao.aliyuncs.com/app/img/temp/test/heng2thumb.jpg");
        i.setOriginUrl("https://yitaoyitao.oss-cn-qingdao.aliyuncs.com/app/img/temp/test/heng2.jpg");
        imageInfoList.add(i);

        // 动图：
        i = new ImageInfo();
        i.setThumbnailUrl("https://yitaoyitao.oss-cn-qingdao.aliyuncs.com/app/img/temp/test/gif1thumb.png");
        i.setOriginUrl("https://yitaoyitao.oss-cn-qingdao.aliyuncs.com/app/img/temp/test/gif1.gif");
        imageInfoList.add(i);

        // 测试：
        i = new ImageInfo();
        i.setThumbnailUrl("https://switch-cdn.vgjump.com/Android_1681181317625_d2959134-f14b-4b7b-9f41-5c08fb72c4cb?imageView2/2/w/1080/h/0/format/webp/q/75|imageslim");
        i.setOriginUrl("https://switch-cdn.vgjump.com/Android_1681181317625_d2959134-f14b-4b7b-9f41-5c08fb72c4cb");
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
        // 二、共享元素动画
        ImageView image1 = findViewById(R.id.image1);
        ImageView image2 = findViewById(R.id.image2);
        ImageView image3 = findViewById(R.id.image3);

        List<String> list2 = new ArrayList<>();
        list2.add("http://img6.16fan.com/201510/11/005258wdngg6rv0tpn8z9z.jpg-400");
        list2.add("http://img6.16fan.com/201510/11/013553aj3kp9u6iuz6k9uj.jpg-400");
        list2.add("http://img6.16fan.com/201510/11/011753fnanichdca0wbhxc.jpg-400");

        Glide.with(this).load("http://img6.16fan.com/201510/11/005258wdngg6rv0tpn8z9z.jpg-400").into(image1);
        Glide.with(this).load("http://img6.16fan.com/201510/11/013553aj3kp9u6iuz6k9uj.jpg-400").into(image2);
        Glide.with(this).load("http://img6.16fan.com/201510/11/011753fnanichdca0wbhxc.jpg-400").into(image3);

        image1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImagePreview.getInstance()
                        .setContext(MainActivity.this)
                        .setImageList(list2)
                        .setIndex(0)
                        .setTransitionView(view)
                        .setTransitionShareElementName("shared_element_container")
                        .start();
            }
        });
        image2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImagePreview.getInstance()
                        .setContext(MainActivity.this)
                        .setImageList(list2)
                        .setIndex(1)
                        .setTransitionView(view)
                        .setTransitionShareElementName("shared_element_container")
                        .start();
            }
        });
        image3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImagePreview.getInstance()
                        .setContext(MainActivity.this)
                        .setImageList(list2)
                        .setIndex(2)
                        .setEnableDragClose(true)
                        .setEnableDragCloseIgnoreScale(true)
                        .setTransitionView(view)
                        .setTransitionShareElementName("shared_element_container")
                        .start();
            }
        });
        // ==============================================================================================================


        // ==============================================================================================================
        // 三、完全自定义调用：
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

                        // 长图的展示模式，默认是SCALE_TYPE_CENTER_INSIDE，缩小到内部居中：
                        .setLongPicDisplayMode(ImagePreview.LongPicDisplayMode.FillWidth)

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

                        // 点击图片回调
                        .setBigImageClickListener(new OnBigImageClickListener() {
                            @Override
                            public void onClick(Activity activity, View view, int position) {
                                // ...
                                Log.d(TAG, "onClick: ");
                            }
                        })
                        // 长按图片回调
                        .setBigImageLongClickListener(new OnBigImageLongClickListener() {
                            @Override
                            public boolean onLongClick(Activity activity, View view, int position) {
                                // ...请使用该方法提供的activity，否则弹窗会被覆盖
                                Log.d(TAG, "onLongClick: ");
                                AlertDialog dialog = new AlertDialog.Builder(activity)
                                        .setTitle("提示")
                                        .setMessage("这里是提示")
                                        .setPositiveButton("确定", null)
                                        .setNegativeButton("取消", null)
                                        .create();
                                dialog.show();
                                return true;
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
                        .setDownloadListener(new OnDownloadListener() {
                            @Override
                            public void onDownloadStart(Activity activity, int position) {
                                // 此处可以设置自己的开始下载的toast，仅仅在使用内置下载时失效
                                Toast.makeText(activity, "开始下载", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onDownloadSuccess(Activity activity, int position) {
                                // 此处可以设置自己的下载成功的toast，仅仅在使用内置下载时失效
                                Toast.makeText(activity, "下载成功", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onDownloadFailed(Activity activity, int position) {
                                // 此处可以设置自己的下载失败的toast，仅仅在使用内置下载时失效
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
                        // 完全自定义预览界面，请参考这个布局（R.layout.sh_layout_preview），需要保持控件类型、id和其中的一致，否则会找不到控件而报错
                        .setPreviewLayoutResId(R.layout.custom_layout_preview, new OnCustomLayoutCallback() {
                            @Override
                            public void onLayout(@NonNull View parentView) {
                                // 自定义控件事件处理
                            }
                        })
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
                // 选择图片
                chooseImage();
            }
        });
        // ==============================================================================================================


        // ==============================================================================================================
        // 清除磁盘缓存
        findViewById(R.id.buttonClean).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageLoader.cleanDiskCache(MainActivity.this);
                ToastUtil.getInstance().showShort(MainActivity.this, "磁盘缓存已成功清除");
            }
        });
        // ==============================================================================================================
    }

    private void chooseImage() {
        PictureSelector.create(this)
                .openGallery(SelectMimeType.ofImage())
                .isDisplayCamera(false)
                .setImageEngine(GlideEngine.createGlideEngine())
                .forResult(new OnResultCallbackListener<LocalMedia>() {
                    @Override
                    public void onResult(ArrayList<LocalMedia> result) {
                        List<String> urlList = new ArrayList<>();
                        for (LocalMedia localMedia : result) {
                            urlList.add(localMedia.getPath());
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

                    @Override
                    public void onCancel() {
                    }
                });
    }
}