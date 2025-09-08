package cc.shinichi.bigimageviewpager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.media3.common.util.UnstableApi;

import com.bumptech.glide.Glide;
import com.luck.picture.lib.basic.PictureSelector;
import com.luck.picture.lib.config.SelectMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.interfaces.OnResultCallbackListener;

import java.util.ArrayList;
import java.util.List;

import cc.shinichi.bigimageviewpager.glide.GlideEngine;
import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.bean.ImageInfo;
import cc.shinichi.library.bean.Type;
import cc.shinichi.library.glide.ImageLoader;
import cc.shinichi.library.tool.common.PhoneUtil;
import cc.shinichi.library.tool.common.SLog;
import cc.shinichi.library.tool.common.ToastUtil;
import cc.shinichi.library.view.listener.OnBigImageClickListener;
import cc.shinichi.library.view.listener.OnBigImageLongClickListener;
import cc.shinichi.library.view.listener.OnBigImagePageChangeListener;
import cc.shinichi.library.view.listener.OnCustomLayoutCallback;
import cc.shinichi.library.view.listener.OnDownloadClickListener;
import cc.shinichi.library.view.listener.OnDownloadListener;
import cc.shinichi.library.view.listener.OnOriginProgressListener;
import cc.shinichi.library.view.listener.OnPageDragListener;
import cc.shinichi.library.view.listener.OnPageFinishListener;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    boolean enableClickClose = true;
    boolean enableDragClose = true;
    boolean enableUpDragClose = true;
    boolean enableDragIgnoreScale = true;

    boolean showIndicator = false;
    boolean showCloseButton = false;
    boolean showDownButton = true;
    boolean showErrorToast = false;
    boolean skipCache = false;

    // 自定义展示UI，业务控件
    ConstraintLayout customViewContainer = null;
    ImageView imgClose = null;
    ImageView imgShare = null;
    TextView tvIndicatorCustom = null;
    int currentPosition = 0;

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
        SwitchCompat switchSkipCache = findViewById(R.id.switchSkipCache);
        RadioGroup radioGroupStrategy = findViewById(R.id.radioGroupStrategy);

        switchClickClose.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableClickClose = isChecked;
            }
        });
        switchClickClose.setChecked(enableClickClose);

        switchDragClose.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableDragClose = isChecked;
            }
        });
        switchDragClose.setChecked(enableDragClose);

        switchUpDragClose.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableUpDragClose = isChecked;
            }
        });
        switchUpDragClose.setChecked(enableUpDragClose);

        switchDragCloseIgnore.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableDragIgnoreScale = isChecked;
            }
        });
        switchDragCloseIgnore.setChecked(enableDragIgnoreScale);

        switchShowIndicator.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showIndicator = isChecked;
            }
        });
        switchShowIndicator.setChecked(showIndicator);

        switchShowCloseButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showCloseButton = isChecked;
            }
        });
        switchShowCloseButton.setChecked(showCloseButton);

        switchShowDownButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showDownButton = isChecked;
            }
        });
        switchShowDownButton.setChecked(showDownButton);

        switchShowErrorToast.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showErrorToast = isChecked;
            }
        });
        switchShowErrorToast.setChecked(showErrorToast);
        switchSkipCache.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                skipCache = isChecked;
            }
        });
        switchSkipCache.setChecked(skipCache);

        radioGroupStrategy.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioThumb) {
                    loadStrategy = ImagePreview.LoadStrategy.AlwaysThumb;
                } else if (checkedId == R.id.radioOrigin) {
                    loadStrategy = ImagePreview.LoadStrategy.AlwaysOrigin;
                } else if (checkedId == R.id.radioDefault) {
                    loadStrategy = ImagePreview.LoadStrategy.Default;
                } else if (checkedId == R.id.radioNetAuto) {
                    loadStrategy = ImagePreview.LoadStrategy.NetworkAuto;
                } else if (checkedId == R.id.radioAuto) {
                    loadStrategy = ImagePreview.LoadStrategy.Auto;
                } else {
                    loadStrategy = ImagePreview.LoadStrategy.Auto;
                }
            }
        });
    }

    private void initData() {
        final List<ImageInfo> mediaList = new ArrayList<>();
        ImageInfo i;

        // avif 图片：
        i = new ImageInfo();
        i.setThumbnailUrl("https://shinichi-common.oss-cn-hangzhou.aliyuncs.com/upload/hato.profile0.10bpc.yuv420.avif");
        i.setOriginUrl("https://shinichi-common.oss-cn-hangzhou.aliyuncs.com/upload/hato.profile0.10bpc.yuv420.avif");
        mediaList.add(i);

        // webp图片：
        i = new ImageInfo();
        i.setThumbnailUrl("https://www.gstatic.com/webp/gallery/1.webp");
        i.setOriginUrl("https://www.gstatic.com/webp/gallery/1.webp");
        mediaList.add(i);

        // 普通图片1：
        i = new ImageInfo();
        i.setThumbnailUrl("http://img3.16fan.com/static/live/origin/202104/20/9a7d0915c91b.jpg-600");
        i.setOriginUrl("http://img3.16fan.com/static/live/origin/202104/20/9a7d0915c91b.jpg");
        mediaList.add(i);

        // 普通图片2：
        i = new ImageInfo();
        i.setThumbnailUrl("http://img3.16fan.com/static/live/origin/202104/20/96247e9c3757.jpg-600");
        i.setOriginUrl("http://img3.16fan.com/static/live/origin/202104/20/96247e9c3757.jpg");
        mediaList.add(i);

        // 视频
        i = new ImageInfo();
        i.setType(Type.VIDEO);
        i.setThumbnailUrl("https://static.smartisanos.cn/common/video/production/delta/smartisan-os-8-0.mp4");
        i.setOriginUrl("https://static.smartisanos.cn/common/video/production/delta/smartisan-os-8-0.mp4");
        mediaList.add(i);

        // 视频
        i = new ImageInfo();
        i.setType(Type.VIDEO);
        i.setThumbnailUrl("https://static.smartisanos.cn/common/video/t1-ui.mp4");
        i.setOriginUrl("https://static.smartisanos.cn/common/video/t1-ui.mp4");
        mediaList.add(i);

        // 大尺寸图片：
        i = new ImageInfo();
        i.setThumbnailUrl("https://yitaoyitao.oss-cn-qingdao.aliyuncs.com/app/img/temp/test/A3ZD8.md.jpg");
        i.setOriginUrl("https://yitaoyitao.oss-cn-qingdao.aliyuncs.com/app/img/temp/test/A3ZD8.jpg");
        mediaList.add(i);

        // gif 动图：
        i = new ImageInfo();
        i.setThumbnailUrl("https://yitaoyitao.oss-cn-qingdao.aliyuncs.com/app/img/temp/test/gif1thumb.png");
        i.setOriginUrl("https://yitaoyitao.oss-cn-qingdao.aliyuncs.com/app/img/temp/test/gif1.gif");
        mediaList.add(i);

        // webp 动图：
        i = new ImageInfo();
        i.setThumbnailUrl("https://shinichi-common.oss-cn-hangzhou.aliyuncs.com/upload/1.webp");
        i.setOriginUrl("https://shinichi-common.oss-cn-hangzhou.aliyuncs.com/upload/1.webp");
        mediaList.add(i);

        // 全景图片1：(风景-横向)
        i = new ImageInfo();
        i.setThumbnailUrl("https://yitaoyitao.oss-cn-qingdao.aliyuncs.com/app/img/temp/test/heng1thumb.jpg");
        i.setOriginUrl("https://yitaoyitao.oss-cn-qingdao.aliyuncs.com/app/img/temp/test/heng1.jpg");
        mediaList.add(i);

        // 全景图片2：(清明上河图-横向)
        i = new ImageInfo();
        i.setThumbnailUrl("https://cdn.jeff1992.com/av/ai/image/2024/upload/am_c4d05c716a1df7eefba1760909111912.jpg");
        i.setOriginUrl("https://cdn.jeff1992.com/av/ai/image/2024/upload/am_f8c3fc818e61d5b878537cf8b2e2a3c4.jpg");
        mediaList.add(i);

        // 全景图片3：(清明上河图-竖向)
        i = new ImageInfo();
        i.setThumbnailUrl("https://cdn.jeff1992.com/av/app/image/2024/upload/am_b4b46dab0e4599fa793ab79cf061e061.jpg");
        i.setOriginUrl("https://cdn.jeff1992.com/av/app/image/2024/upload/am_af2caea4ce80cb515e9279e6d58ef78a.jpg");
        mediaList.add(i);

        // ==============================================================================================================
        // 一、最简单的调用：
        findViewById(R.id.buttonEasyUse).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 一行代码即可实现大部分需求。
                // 如需定制，可参考下面【三、完全自定义调用】自定义的代码：
                ImagePreview.getInstance().with(MainActivity.this).setMediaInfoList(mediaList).start();
            }
        });
        // ==============================================================================================================


        // ==============================================================================================================
        // 二、图片展示调用
        ImageView image1 = findViewById(R.id.image1);
        ImageView image2 = findViewById(R.id.image2);
        ImageView image3 = findViewById(R.id.image3);

        Glide.with(this).load(mediaList.get(0).getThumbnailUrl()).into(image1);
        Glide.with(this).load(mediaList.get(1).getThumbnailUrl()).into(image2);
        Glide.with(this).load(mediaList.get(2).getThumbnailUrl()).into(image3);

        image1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentPosition = 0;
                startPreview(0, mediaList);
            }
        });
        image2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentPosition = 1;
                startPreview(1, mediaList);
            }
        });
        image3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentPosition = 2;
                startPreview(2, mediaList);
            }
        });
        // ==============================================================================================================


        // ==============================================================================================================
        // 三、完全自定义调用：
        findViewById(R.id.buttonPreview).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 完全自定义配置
                currentPosition = 0;
                startPreview(0, mediaList);
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
                .openGallery(SelectMimeType.ofAll())
                .isWithSelectVideoImage(true)
                .isDisplayCamera(false)
                .setImageEngine(GlideEngine.createGlideEngine())
                .forResult(new OnResultCallbackListener<>() {
                    @Override
                    public void onResult(ArrayList<LocalMedia> result) {
                        List<ImageInfo> mediaList = new ArrayList<>();
                        for (LocalMedia localMedia : result) {
                            ImageInfo imageInfo = getImageInfo(localMedia);
                            mediaList.add(imageInfo);
                        }
                        currentPosition = 0;
                        startPreview(0, mediaList);
                    }

                    @NonNull
                    private ImageInfo getImageInfo(LocalMedia localMedia) {
                        ImageInfo imageInfo = new ImageInfo();
                        if (localMedia.getMimeType().startsWith("video")) {
                            imageInfo.setType(Type.VIDEO);
                        } else if (localMedia.getMimeType().startsWith("image")) {
                            imageInfo.setType(Type.IMAGE);
                        }
                        imageInfo.setThumbnailUrl(localMedia.getRealPath());
                        imageInfo.setOriginUrl(localMedia.getRealPath());
                        return imageInfo;
                    }

                    @Override
                    public void onCancel() {
                    }
                });
    }

    private void startPreview(int index, List<ImageInfo> mediaList) {
        ImagePreview.getInstance()
                // 上下文，必须是activity，不需要担心内存泄漏，本框架已经处理好
                .with(MainActivity.this)

                // 从第几张图片开始，索引从0开始
                .setIndex(index)

                //=================================================================================================
                // 有三种设置数据集合的方式，根据自己的需求进行三选一：
                // 1：第一步生成的imageInfo List
                .setMediaInfoList(mediaList)

                // 2：直接传url List
                //.setImageList(List<String> imageList)

                // 3：只有一张图片的情况，可以直接传入这张图片的url
                //.setImage(String image)
                //=================================================================================================

                // 加载策略，默认为手动模式：默认普清，点击按钮再加载原图；会根据原图、缩略图url是否一样来判断是否显示查看原图按钮
                .setLoadStrategy(loadStrategy)

                // 长图的展示模式，默认是缩小到内部居中，可选撑满屏幕：
                .setLongPicDisplayMode(ImagePreview.LongPicDisplayMode.Default)

                // 保存的文件夹名称，会在(Pictures/Movies)目录进行文件夹的新建。
                .setFolderName("BigImageView")

                // 缩放动画时长，单位ms
                .setZoomTransitionDuration(300)

                // 是否显示加载失败的Toast
                .setShowErrorToast(showErrorToast)

                // 是否跳过缓存，强制只从网络获取图片
                .setSkipLocalCache(skipCache)

                // 是否启用点击图片关闭。默认启用
                .setEnableClickClose(enableClickClose)
                // 是否启用下拉关闭。默认启用
                .setEnableDragClose(enableDragClose)
                // 是否启用上拉关闭。默认启用
                .setEnableUpDragClose(enableUpDragClose)
                // 是否忽略缩放启用拉动关闭。默认忽略
                .setEnableDragCloseIgnoreScale(enableDragIgnoreScale)

                // 是否显示关闭页面按钮，在页面左下角。默认不显示
                .setShowCloseButton(showCloseButton)
                // 设置关闭按钮图片资源，可不填，默认为库中自带：R.drawable.ic_action_close
                .setCloseIconResId(R.drawable.ic_action_close)
                // 设置关闭按钮背景shape
                .setCloseIconBackgroundResId(R.drawable.shape_indicator_bg)

                // 是否显示下载按钮，在页面右下角。默认显示
                .setShowDownButton(showDownButton)
                // 设置下载按钮图片资源，可不填，默认为库中自带：R.drawable.icon_download_new
                .setDownIconResId(R.drawable.icon_download_new)
                // 设置下载按钮背景shape
                .setDownIconBackgroundResId(R.drawable.shape_indicator_bg)
                // 下载按钮点击回调：重写此方法，isInterceptDownload返回true时，即代表您需要自己实现下载逻辑
                .setDownloadClickListener(new OnDownloadClickListener() {
                    @Override
                    public void onClick(@NonNull Activity activity, @NonNull View view, int position) {
                        // 可以在此处执行您自己的下载逻辑、埋点统计等信息
                        SLog.INSTANCE.d(TAG, "onDownloadClick: position = " + position);
                    }

                    @Override
                    public boolean isInterceptDownload() {
                        // return true 时, 需要自己实现下载
                        // return false 时, 使用内置下载
                        return false;
                    }
                })
                // 内置下载过程回调，可自定义toast。如果不设置此回调会使用默认的toast内容，反之，设置了此回调时不会展示默认toast
                .setDownloadListener(new OnDownloadListener() {
                    @Override
                    public void onDownloadStart(@NonNull Activity activity, int position) {
                        // 此处可以设置自己的开始下载的toast，仅仅在使用内置下载时生效
                        Toast.makeText(activity, "开始下载", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onDownloadSuccess(@NonNull Activity activity, int position) {
                        // 此处可以设置自己的下载成功的toast，仅仅在使用内置下载时生效
                        Toast.makeText(activity, "下载成功", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onDownloadFailed(@NonNull Activity activity, int position) {
                        // 此处可以设置自己的下载失败的toast，仅仅在使用内置下载时生效
                        Toast.makeText(activity, "下载失败", Toast.LENGTH_SHORT).show();
                    }
                })

                // 设置是否显示顶部的指示器（1/9）默认显示
                .setShowIndicator(showIndicator)
                // 设置顶部指示器背景shape，默认自带灰色圆角shape
                .setIndicatorShapeResId(R.drawable.shape_indicator_bg)

                // 设置失败时的占位图，默认为库中自带R.drawable.load_failed，设置为 0 时不显示
                .setErrorPlaceHolder(R.drawable.load_failed)

                // 点击图片回调：不受点击关闭开关的影响，都会回调此方法
                .setBigImageClickListener(new OnBigImageClickListener() {
                    @Override
                    public void onClick(@NonNull Activity activity, @NonNull View view, int position) {
                        // ...
                        SLog.INSTANCE.d(TAG, "点击了: position = " + position);
                    }
                })
                // 长按图片回调
                .setBigImageLongClickListener(new OnBigImageLongClickListener() {
                    @Override
                    public boolean onLongClick(@NonNull Activity activity, @NonNull View view, int position) {
                        // ...请使用该方法提供的activity，否则弹窗会被覆盖
                        SLog.INSTANCE.d(TAG, "onLongClick: ");
                        AlertDialog dialog = new AlertDialog.Builder(activity)
                                .setTitle("这里是模拟长按的弹窗")
                                .setMessage("是否删除当前图片？")
                                .setPositiveButton("确定", new AlertDialog.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // 删除图片
                                        ImagePreview.getInstance().getPreviewActivity().deleteItem(position);
                                    }
                                })
                                .setNegativeButton("更新当前", new DialogInterface.OnClickListener() {
                                    @OptIn(markerClass = UnstableApi.class)
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // 更新图片
                                        ImageInfo imageInfo = mediaList.get(position);
                                        imageInfo.setOriginUrl("https://cdn.jeff1992.com/av/ai/video/2024/upload/am_f34ca072f2e0812204233934085111dd.mp4");
                                        imageInfo.setThumbnailUrl("https://cdn.jeff1992.com/av/ai/video/2024/upload/am_f34ca072f2e0812204233934085111dd.mp4");
                                        imageInfo.setType(Type.VIDEO);
                                        ImagePreview.getInstance().getPreviewActivity().updateItem(position, imageInfo);
                                    }
                                })
                                .create();
                        dialog.show();
                        // 返回true
                        return true;
                    }
                })

                // 页面左右切换的回调
                .setBigImagePageChangeListener(new OnBigImagePageChangeListener() {
                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    }

                    @Override
                    public void onPageSelected(int position, @NonNull List<ImageInfo> imageInfoList) {
                        SLog.INSTANCE.d(TAG, "onPageSelected: position = " + position);
                        currentPosition = position;
                        tvIndicatorCustom.setText((currentPosition + 1) + " / " + imageInfoList.size());
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {
                        SLog.INSTANCE.d(TAG, "onPageScrollStateChanged: state = " + state);
                    }
                })

                // 页面关闭回调
                .setOnPageFinishListener(new OnPageFinishListener() {
                    @Override
                    public void onFinish(@NonNull Activity activity) {
                        // ...
                        SLog.INSTANCE.d(TAG, "onFinish: ");
                    }
                })

                // 页面手势上下拖动的回调：自定义布局可以根据是否拖动进行隐藏或者展示
                .setOnPageDragListener(new OnPageDragListener() {
                    @Override
                    public void onDrag(@NonNull Activity activity, @NonNull View parentView, MotionEvent event, float translationY) {
                        SLog.INSTANCE.d(TAG, "onDrag: translationY = " + translationY);
                        // 此处可以根据是否拖拽设置自定义的View的逻辑
                        if (customViewContainer != null) {
                            customViewContainer.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onDragEnd(@NonNull Activity activity, @NonNull View parentView) {
                        SLog.INSTANCE.d(TAG, "onDragEnd: ");
                        if (customViewContainer != null) {
                            customViewContainer.setVisibility(View.VISIBLE);
                        }
                    }
                })

                // 设置查看原图时的百分比样式：默认为库中自带样式：ImagePreview.PROGRESS_THEME_CIRCLE_TEXT。
                // 可手动更改为自定义样式，需要在回调中手动更新进度
                .setProgressLayoutId(ImagePreview.PROGRESS_THEME_CIRCLE_TEXT, new OnOriginProgressListener() {
                    @Override
                    public void progress(@NonNull Activity activity, @NonNull View parentView, int progress) {
                        SLog.INSTANCE.d(TAG, "原图progress: " + progress);
                        // 需要找到进度控件并设置百分比，回调中的parentView即传入的布局的根View，可通过parentView找到控件：
                        ProgressBar progressBar = parentView.findViewById(R.id.sh_progress_view);
                        TextView textView = parentView.findViewById(R.id.sh_progress_text);
                        progressBar.setProgress(progress);
                        String progressText = progress + "%";
                        textView.setText(progressText);
                    }

                    @Override
                    public void finish(@NonNull Activity activity, @NonNull View parentView) {
                        SLog.INSTANCE.d(TAG, "finish: ");
                    }
                })

                // 完全自定义预览界面，请参考这个布局（R.layout.sh_layout_preview），需要保持控件类型、id和其中的一致，否则会找不到控件而报错
                .setPreviewLayoutResId(R.layout.custom_layout_preview, new OnCustomLayoutCallback() {
                    @Override
                    public void onLayout(@NonNull Activity activity, @NonNull View parentView) {
                        // 除了默认的控件之外，你可以在此处处理你的其他控件，比如分享按钮、业务数据展示等
                        customViewContainer = parentView.findViewById(R.id.custom_view_container);
                        imgClose = parentView.findViewById(R.id.img_close_button_custom);
                        imgShare = parentView.findViewById(R.id.img_share_button_custom);
                        tvIndicatorCustom = parentView.findViewById(R.id.tv_indicator_custom);
                        // 业务逻辑处理
                        int statusBarHeight = PhoneUtil.INSTANCE.getStatusBarHeight(MainActivity.this);
                        customViewContainer.setPadding(0, statusBarHeight, 0, 0);
                        tvIndicatorCustom.setText((currentPosition + 1) + " / " + mediaList.size());
                        // 点击事件
                        imgClose.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ImagePreview.getInstance().finish();
                            }
                        });
                        imgShare.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ToastUtil.getInstance().showShort(MainActivity.this, "点击了分享, 当前 position = " + currentPosition);
                            }
                        });
                    }
                })

                // 开启预览
                .start();
    }
}