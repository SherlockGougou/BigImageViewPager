package cc.shinichi.bigimageviewpager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
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
import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.bean.ImageInfo;
import cc.shinichi.library.glide.ImageLoader;
import cc.shinichi.library.tool.utility.ui.ToastUtil;
import cc.shinichi.library.view.listener.OnBigImageClickListener;
import cc.shinichi.library.view.listener.OnBigImageLongClickListener;
import cc.shinichi.library.view.listener.OnBigImagePageChangeListener;
import cc.shinichi.library.view.listener.OnOriginProgressListener;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;
import com.zhihu.matisse.internal.entity.CaptureStrategy;
import java.util.ArrayList;
import java.util.List;

import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    String[] images = {
        "http://img3.16fan.com/live/origin/201805/21/E421b24c08446.jpg",
        "http://img3.16fan.com/live/origin/201805/21/4D7B35fdf082e.jpg",
        "http://img3.16fan.com/live/origin/201805/21/2D02ebc5838e6.jpg",
        "http://img3.16fan.com/live/origin/201805/21/A1B17c5f59b78.jpg",
        "http://img3.16fan.com/live/origin/201805/21/94699b2be3cfa.jpg",
        "http://img6.16fan.com/attachments/wenzhang/201805/18/152660818127263ge.jpeg", //  5760 * 3840
        "http://img3.16fan.com/live/origin/201812/01/qz2x9e6l98b5h.jpg",
        "http://img3.16fan.com/live/origin/201805/21/14C5e483e7583.jpg",
        "http://img3.16fan.com/live/origin/201805/21/EB298ce595dd2.jpg",
        "http://img6.16fan.com/attachments/wenzhang/201805/18/152660818716180ge.jpeg", //  2280 * 22116
        "http://img3.16fan.com/live/origin/201805/21/264Ba4860d469.jpg"
    };

    boolean enableClickClose = false;
    boolean enableDragClose = false;
    boolean enableUpDragClose = false;
    boolean showIndicator = false;
    boolean showCloseButton = false;
    boolean showDownButton = false;

    ImagePreview.LoadStrategy loadStrategy = ImagePreview.LoadStrategy.Default;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SwitchCompat switchClickClose = findViewById(R.id.switchClickClose);
        SwitchCompat switchDragClose = findViewById(R.id.switchDragClose);
        SwitchCompat switchUpDragClose = findViewById(R.id.switchUpDragClose);
        SwitchCompat switchShowIndicator = findViewById(R.id.switchShowIndicator);
        SwitchCompat switchShowCloseButton = findViewById(R.id.switchShowCloseButton);
        SwitchCompat switchShowDownButton = findViewById(R.id.switchShowDownButton);

        RadioGroup radioGroupStrategy = findViewById(R.id.radioGroupStrategy);

        switchClickClose.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableClickClose = isChecked;
            }
        });
        switchClickClose.setChecked(true);

        switchDragClose.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableDragClose = isChecked;
            }
        });
        switchDragClose.setChecked(true);

        switchUpDragClose.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableUpDragClose = isChecked;
            }
        });
        switchUpDragClose.setChecked(true);

        switchShowIndicator.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showIndicator = isChecked;
            }
        });
        switchShowIndicator.setChecked(true);

        switchShowCloseButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showCloseButton = isChecked;
            }
        });
        switchShowCloseButton.setChecked(false);

        switchShowDownButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showDownButton = isChecked;
            }
        });
        switchShowDownButton.setChecked(true);

        loadStrategy = ImagePreview.LoadStrategy.Default;
        radioGroupStrategy.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(RadioGroup group, int checkedId) {
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

        ImageInfo imageInfo;
        final List<ImageInfo> imageInfoList = new ArrayList<>();

        for (String image : images) {
            imageInfo = new ImageInfo();
            // 原图地址
            imageInfo.setOriginUrl(image);
            // 缩略图，实际使用中，根据需求传入缩略图路径。如果没有缩略图url，可以将两项设置为一样。
            imageInfo.setThumbnailUrl(image.concat("-1200"));
            imageInfoList.add(imageInfo);
        }

        // 测试超宽图
        imageInfo = new ImageInfo();
        imageInfo.setOriginUrl(
            "http://cache.house.sina.com.cn/citylifehouse/citylife/de/26/20090508_7339__.jpg");// 这张是https图片
        imageInfo.setThumbnailUrl(
            "http://cache.house.sina.com.cn/citylifehouse/citylife/de/26/20090508_7339__.jpg");// 这张是https图片
        imageInfoList.add(0, imageInfo);

        // 测试https图
        imageInfo = new ImageInfo();
        imageInfo.setOriginUrl("https://ws1.sinaimg.cn/large/610dc034ly1fgepc1lpvfj20u011i0wv.jpg");// 这张是https图片
        imageInfo.setThumbnailUrl("https://ws1.sinaimg.cn/large/610dc034ly1fgepc1lpvfj20u011i0wv.jpg");// 这张是https图片
        imageInfoList.add(0, imageInfo);

        // 测试gif图
        imageInfo = new ImageInfo();
        imageInfo.setOriginUrl("http://s1.dwstatic.com/group1/M00/82/DB/c9d0b4c9fdba07709071784bce709c26.gif");
        imageInfo.setThumbnailUrl("http://s1.dwstatic.com/group1/M00/82/DB/c9d0b4c9fdba07709071784bce709c26.gif");
        imageInfoList.add(0, imageInfo);

        // 测试gif图
        imageInfo = new ImageInfo();
        imageInfo.setOriginUrl("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1551350691824&di=888eada749b09bb0a1db18c3cb36a077&imgtype=0&src=http%3A%2F%2Fs9.rr.itc.cn%2Fr%2FwapChange%2F20173_7_18%2Fa1h59g3470867073619.gif");
        imageInfo.setThumbnailUrl("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1551350691824&di=888eada749b09bb0a1db18c3cb36a077&imgtype=0&src=http%3A%2F%2Fs9.rr.itc.cn%2Fr%2FwapChange%2F20173_7_18%2Fa1h59g3470867073619.gif");
        imageInfoList.add(0, imageInfo);

        // 测试gif图
        imageInfo = new ImageInfo();
        imageInfo.setOriginUrl("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1550234159438&di=345e38a6d82e79a3c48abd85b57d5e89&imgtype=0&src=http%3A%2F%2Fimg.mp.itc.cn%2Fq_70%2Cc_zoom%2Cw_640%2Fupload%2F20170331%2F99098fa2ae0e48ac8ee8d813c1620900_th.gif");
        imageInfo.setThumbnailUrl("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1550234159438&di=345e38a6d82e79a3c48abd85b57d5e89&imgtype=0&src=http%3A%2F%2Fimg.mp.itc.cn%2Fq_70%2Cc_zoom%2Cw_640%2Fupload%2F20170331%2F99098fa2ae0e48ac8ee8d813c1620900_th.gif");
        imageInfoList.add(0, imageInfo);

        imageInfo = new ImageInfo();
        imageInfo.setOriginUrl("http://hongdan24.oss-cn-qingdao.aliyuncs.com/nba/9071185e93c349518d43a39b3281d99f.png");
        imageInfo.setThumbnailUrl("http://hongdan24.oss-cn-qingdao.aliyuncs.com/nba/9071185e93c349518d43a39b3281d99f.png");
        imageInfoList.add(0, imageInfo);

        // 测试小尺寸图
        imageInfo = new ImageInfo();
        imageInfo.setOriginUrl(
            "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1544679916732&di=3bc3e11ccd7185a4ab9932b93de2077a&imgtype=0&src=http%3A%2F%2Fb-ssl.duitang.com%2Fuploads%2Fitem%2F201602%2F05%2F20160205113750_UsRPe.thumb.700_0.jpeg");
        imageInfo.setThumbnailUrl(
            "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1544679916732&di=3bc3e11ccd7185a4ab9932b93de2077a&imgtype=0&src=http%3A%2F%2Fb-ssl.duitang.com%2Fuploads%2Fitem%2F201602%2F05%2F20160205113750_UsRPe.thumb.700_0.jpeg");
        imageInfoList.add(0, imageInfo);

        // 测试没有后缀的链接
        imageInfo = new ImageInfo();
        imageInfo.setOriginUrl(
            "https://sacasnap.neusoft.com/snap-engine-file/image/obtain/1264c125-387e-4af0-a361-f26c3f7fd12e?tenantId=neusoft");
        imageInfo.setThumbnailUrl(
            "https://sacasnap.neusoft.com/snap-engine-file/image/obtain/1264c125-387e-4af0-a361-f26c3f7fd12e?tenantId=neusoft");
        imageInfoList.add(0, imageInfo);

        // 测试超长文件名、没有后缀的链接
        imageInfo = new ImageInfo();
        imageInfo.setOriginUrl(
            "http://img2.weishoot.com/?4583B5ECE38DC1B27FD1269F4E70B7670C426883D16355F21B46193F75071C599A3E6AED54A235489F4C38835361546D7648065651316476F7E30592DAF1CD6C50358835D5DD10D029DA4A9C59B56C3EA48055CE4E0A7627AF8C90303A4D1FBC2D88218AB0F699E74425A61E5D9E2B378E199A2906934C52DAC2D3920C74DBAF34BD945EEA7E86AC72AF12CD00F1179846E1DA2926B782C1D2215DB087EFDE04A1C0F46B1352527F");
        imageInfo.setThumbnailUrl(
            "http://img2.weishoot.com/?4583B5ECE38DC1B27FD1269F4E70B7670C426883D16355F21B46193F75071C599A3E6AED54A235489F4C38835361546D7648065651316476F7E30592DAF1CD6C50358835D5DD10D029DA4A9C59B56C3EA48055CE4E0A7627AF8C90303A4D1FBC2D88218AB0F699E74425A61E5D9E2B378E199A2906934C52DAC2D3920C74DBAF34BD945EEA7E86AC72AF12CD00F1179846E1DA2926B782C1D2215DB087EFDE04A1C0F46B1352527F");
        imageInfoList.add(0, imageInfo);

        // 也可直接传入url List
        final List<String> imageList = new ArrayList<>();
        imageList.add(
            "http://img2.weishoot.com/?4583B5ECE38DC1B27FD1269F4E70B7670C426883D16355F21B46193F75071C599A3E6AED54A235489F4C38835361546D7648065651316476F7E30592DAF1CD6C50358835D5DD10D029DA4A9C59B56C3EA48055CE4E0A7627AF8C90303A4D1FBC2D88218AB0F699E74425A61E5D9E2B378E199A2906934C52DAC2D3920C74DBAF34BD945EEA7E86AC72AF12CD00F1179846E1DA2926B782C1D2215DB087EFDE04A1C0F46B1352527F");
        imageList.add(
            "https://sacasnap.neusoft.com/snap-engine-file/image/obtain/1264c125-387e-4af0-a361-f26c3f7fd12e?tenantId=neusoft");
        imageList.add(
            "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1544679916731&di=89e3775c25c5f21254cd0a5aa3b0b1b1&imgtype=0&src=http%3A%2F%2Fimage.biaobaiju.com%2Fuploads%2F20180210%2F20%2F1518266167-ActRaEkWDS.jpg");
        imageList.add("http://s1.dwstatic.com/group1/M00/82/DB/c9d0b4c9fdba07709071784bce709c26.gif");
        imageList.add("https://ws1.sinaimg.cn/large/610dc034ly1fgepc1lpvfj20u011i0wv.jpg");
        imageList.add("http://cache.house.sina.com.cn/citylifehouse/citylife/de/26/20090508_7339__.jpg");
        imageList.add("http://s1.dwstatic.com/group1/M00/EE/9C/701cab3f6f04b8e7f8f5562ed65f8639.gif");
        imageList.add("http://img3.16fan.com/live/origin/201903/12/3EB3b3070c803.jpg?imageView2/0/h/1600/interlace/1/q/50/format/bmp");

        // 最简单的调用：
        findViewById(R.id.buttonEasyUse).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {

                // 仅需一行代码,默认配置为：
                //      显示顶部进度指示器、
                //      显示右侧下载按钮、
                //      隐藏左侧关闭按钮、
                //      开启点击图片关闭、
                //      关闭下拉图片关闭、
                //      加载方式为手动模式
                //      加载原图的百分比在底部

                // 一行代码即可实现大部分需求，如需定制，可参考下面自定义的代码：
                ImagePreview.getInstance().setContext(MainActivity.this).setImageList(imageList).start();
            }
        });

        // 完全自定义调用：
        findViewById(R.id.buttonPreview).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
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

                    // 保存的文件夹名称，会在SD卡根目录进行文件夹的新建。
                    // (你也可设置嵌套模式，比如："BigImageView/Download"，会在SD卡根目录新建BigImageView文件夹，并在BigImageView文件夹中新建Download文件夹)
                    .setFolderName("BigImageView/Download")

                    // 缩放动画时长，单位ms
                    .setZoomTransitionDuration(300)

                    // 是否启用点击图片关闭。默认启用
                    .setEnableClickClose(enableClickClose)
                    // 是否启用下拉关闭。默认不启用
                    .setEnableDragClose(enableDragClose)
                    // 是否启用上拉关闭。默认不启用
                    .setEnableUpDragClose(enableUpDragClose)

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

                    // 设置失败时的占位图，默认为库中自带R.drawable.load_failed，设置为 0 时不显示
                    .setErrorPlaceHolder(R.drawable.load_failed)

                    // 点击回调
                    .setBigImageClickListener(new OnBigImageClickListener() {
                        @Override public void onClick(View view, int position) {
                            // ...
                            Log.d(TAG, "onClick: ");
                        }
                    })
                    // 长按回调
                    .setBigImageLongClickListener(new OnBigImageLongClickListener() {
                        @Override public boolean onLongClick(View view, int position) {
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

                        @Override public void onPageSelected(int position) {
                            Log.d(TAG, "onPageSelected: ");
                        }

                        @Override public void onPageScrollStateChanged(int state) {
                            Log.d(TAG, "onPageScrollStateChanged: ");
                        }
                    })

                    //=================================================================================================
                    // 设置查看原图时的百分比样式：库中带有一个样式：ImagePreview.PROGRESS_THEME_CIRCLE_TEXT，使用如下：
                    .setProgressLayoutId(ImagePreview.PROGRESS_THEME_CIRCLE_TEXT, new OnOriginProgressListener() {
                        @Override public void progress(View parentView, int progress) {
                            Log.d(TAG, "progress: " + progress);

                            // 需要找到进度控件并设置百分比，回调中的parentView即传入的布局的根View，可通过parentView找到控件：
                            ProgressBar progressBar = parentView.findViewById(R.id.sh_progress_view);
                            TextView textView = parentView.findViewById(R.id.sh_progress_text);
                            progressBar.setProgress(progress);
                            String progressText = progress + "%";
                            textView.setText(progressText);
                        }

                        @Override public void finish(View parentView) {
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

        // 通过相册选择图片，进行预览
        findViewById(R.id.buttonChoose).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this.getApplicationContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        // 拒绝权限
                        ToastUtil.getInstance()._short(MainActivity.this.getApplicationContext(), "您拒绝了存储权限，无法读取图片！");
                    } else {
                        // 申请权限
                        ActivityCompat.requestPermissions(MainActivity.this,
                            new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE, }, 1);
                    }
                } else {
                    // 选择图片
                    chooseImage();
                }
            }
        });

        // 清除磁盘缓存
        findViewById(R.id.buttonClean).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                ImageLoader.cleanDiskCache(MainActivity.this);
                ToastUtil.getInstance()._short(MainActivity.this, "磁盘缓存已成功清除");
            }
        });
    }

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
        @NonNull int[] grantResults) {
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
                ArrayList<String> mCurrentSelectedPath = (ArrayList<String>) Matisse.obtainPathResult(data);
                ImagePreview
                    .getInstance()
                    .setContext(MainActivity.this)
                    .setImageList(mCurrentSelectedPath)
                    .setShowDownButton(false)
                    .setShowCloseButton(false)
                    .setEnableDragClose(true)
                    .setEnableClickClose(false)
                    .start();
            }
        }
    }
}