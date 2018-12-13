package cc.shinichi.bigimageviewpager;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.CompoundButton;
import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.bean.ImageInfo;
import cc.shinichi.library.glide.ImageLoader;
import cc.shinichi.library.tool.utility.ui.MyToast;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

	String[] images = {
		"http://img3.16fan.com/live/origin/201805/21/E421b24c08446.jpg",
		"http://img3.16fan.com/live/origin/201805/21/4D7B35fdf082e.jpg",
		"http://img6.16fan.com/attachments/wenzhang/201805/18/152660818127263ge.jpeg", //  5760 * 3840
		"http://img3.16fan.com/live/origin/201805/21/2D02ebc5838e6.jpg",
		"http://img3.16fan.com/live/origin/201812/01/qz2x9e6l98b5h.jpg",
		"http://img6.16fan.com/attachments/wenzhang/201805/18/152660818716180ge.jpeg", //  2280 * 22116
		"http://img3.16fan.com/live/origin/201805/21/14C5e483e7583.jpg",
		"http://img3.16fan.com/live/origin/201805/21/A1B17c5f59b78.jpg",
		"http://img3.16fan.com/live/origin/201805/21/94699b2be3cfa.jpg",
		"http://img3.16fan.com/live/origin/201812/01/qz2x9e6l98b5h.jpg",
		"http://img6.16fan.com/attachments/wenzhang/201805/18/152660818716180ge.jpeg", //  2280 * 22116
		"http://img3.16fan.com/live/origin/201805/21/14C5e483e7583.jpg",
		"http://img3.16fan.com/live/origin/201805/21/EB298ce595dd2.jpg",
		"http://img6.16fan.com/attachments/wenzhang/201805/18/152660818127263ge.jpeg", //  5760 * 3840
		"http://img3.16fan.com/live/origin/201805/21/264Ba4860d469.jpg",
		"http://img3.16fan.com/live/origin/201812/01/qz2x9e6l98b5h.jpg",
		"http://img6.16fan.com/attachments/wenzhang/201805/18/152660818716180ge.jpeg", //  2280 * 22116
		"http://img6.16fan.com/attachments/wenzhang/201805/18/152660818127263ge.jpeg" //  5760 * 3840
	};

	boolean enableClickClose = false;
	boolean enableDragClose = false;
	boolean showIndicator = false;
	boolean showCloseButton = false;
	boolean showDownButton = false;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		SwitchCompat switchClickClose = findViewById(R.id.switchClickClose);
		SwitchCompat switchDragClose = findViewById(R.id.switchDragClose);
		SwitchCompat switchShowIndicator = findViewById(R.id.switchShowIndicator);
		SwitchCompat switchShowCloseButton = findViewById(R.id.switchShowCloseButton);
		SwitchCompat switchShowDownButton = findViewById(R.id.switchShowDownButton);

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

		// 网络图片：
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

		// 此图为超宽图
		imageInfo = new ImageInfo();
		imageInfo.setOriginUrl("http://cache.house.sina.com.cn/citylifehouse/citylife/de/26/20090508_7339__.jpg");// 这张是https图片
		imageInfo.setThumbnailUrl("http://cache.house.sina.com.cn/citylifehouse/citylife/de/26/20090508_7339__.jpg");// 这张是https图片
		imageInfoList.add(0, imageInfo);

		// 此图为https图
		imageInfo = new ImageInfo();
		imageInfo.setOriginUrl("https://ws1.sinaimg.cn/large/610dc034ly1fgepc1lpvfj20u011i0wv.jpg");// 这张是https图片
		imageInfo.setThumbnailUrl("https://ws1.sinaimg.cn/large/610dc034ly1fgepc1lpvfj20u011i0wv.jpg");// 这张是https图片
		imageInfoList.add(0, imageInfo);

		// 此图为gif图
		imageInfo = new ImageInfo();
		imageInfo.setOriginUrl("http://s1.dwstatic.com/group1/M00/82/DB/c9d0b4c9fdba07709071784bce709c26.gif");
		imageInfo.setThumbnailUrl("http://s1.dwstatic.com/group1/M00/82/DB/c9d0b4c9fdba07709071784bce709c26.gif");
		imageInfoList.add(0, imageInfo);

		// 此图为gif图
		imageInfo = new ImageInfo();
		imageInfo.setOriginUrl("http://s1.dwstatic.com/group1/M00/EE/9C/701cab3f6f04b8e7f8f5562ed65f8639.gif");
		imageInfo.setThumbnailUrl("http://s1.dwstatic.com/group1/M00/EE/9C/701cab3f6f04b8e7f8f5562ed65f8639.gif");
		imageInfoList.add(0, imageInfo);

		imageInfo = new ImageInfo();
		imageInfo.setOriginUrl("/storage/emulated/0/Pictures/screenshots/Screenshot_2018-12-13-10-41-27-342.png");
		imageInfo.setThumbnailUrl("/storage/emulated/0/Pictures/screenshots/Screenshot_2018-12-13-10-41-27-342.png");
		imageInfoList.add(0, imageInfo);

		imageInfo = new ImageInfo();
		imageInfo.setOriginUrl("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1544679916732&di=3bc3e11ccd7185a4ab9932b93de2077a&imgtype=0&src=http%3A%2F%2Fb-ssl.duitang.com%2Fuploads%2Fitem%2F201602%2F05%2F20160205113750_UsRPe.thumb.700_0.jpeg");
		imageInfo.setThumbnailUrl("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1544679916732&di=3bc3e11ccd7185a4ab9932b93de2077a&imgtype=0&src=http%3A%2F%2Fb-ssl.duitang.com%2Fuploads%2Fitem%2F201602%2F05%2F20160205113750_UsRPe.thumb.700_0.jpeg");
		imageInfoList.add(0, imageInfo);

		imageInfo = new ImageInfo();
		imageInfo.setOriginUrl("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1544679916731&di=89e3775c25c5f21254cd0a5aa3b0b1b1&imgtype=0&src=http%3A%2F%2Fimage.biaobaiju.com%2Fuploads%2F20180210%2F20%2F1518266167-ActRaEkWDS.jpg");
		imageInfo.setThumbnailUrl("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1544679916731&di=89e3775c25c5f21254cd0a5aa3b0b1b1&imgtype=0&src=http%3A%2F%2Fimage.biaobaiju.com%2Fuploads%2F20180210%2F20%2F1518266167-ActRaEkWDS.jpg");
		imageInfoList.add(0, imageInfo);

		imageInfo = new ImageInfo();
		imageInfo.setOriginUrl("https://sacasnap.neusoft.com/snap-engine-file/image/obtain/1264c125-387e-4af0-a361-f26c3f7fd12e?tenantId=neusoft");
		imageInfo.setThumbnailUrl("https://sacasnap.neusoft.com/snap-engine-file/image/obtain/1264c125-387e-4af0-a361-f26c3f7fd12e?tenantId=neusoft");
		imageInfoList.add(0, imageInfo);

		// 本地图片：将原图和缩略图地址传一样的即可。
		//ImageInfo imageInfo;
		//final List<ImageInfo> imageInfoList = new ArrayList<>();
		//for (String image : images) {
		//	imageInfo = new ImageInfo();
		//	imageInfo.setOriginUrl(image);
		//	imageInfo.setThumbnailUrl(image);
		//	imageInfoList.add(imageInfo);
		//	imageInfo = null;
		//}

		// 最简单的调用
		findViewById(R.id.buttonEasyUse).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				// 仅需一行代码
				// 默认配置为：显示顶部进度指示器、显示右侧下载按钮、隐藏左侧关闭按钮、开启点击图片关闭、关闭下拉图片关闭、加载方式为手动模式
				// 一行代码即可实现大部分需求，如需定制，可参考下面代码
				ImagePreview.getInstance().setContext(MainActivity.this).setImageInfoList(imageInfoList).start();
			}
		});

		// 仅加载普清
		findViewById(R.id.buttonThumb).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				ImagePreview
					.getInstance()
					.setContext(MainActivity.this)// 上下文
					.setIndex(0)// 从第一张图片开始，索引从0开始哦
					.setImageInfoList(imageInfoList)// 图片源
					.setLoadStrategy(ImagePreview.LoadStrategy.AlwaysThumb)// 加载策略，见下面介绍
					.setFolderName("BigImageViewDownload")// 保存的文件夹名称，SD卡根目录
					.setScaleLevel(1, 3, 8)// 设置三级缩放级别
					.setZoomTransitionDuration(300)// 缩放动画时长

					.setEnableClickClose(enableClickClose)// 是否启用点击图片关闭。默认启用
					.setEnableDragClose(enableDragClose)// 是否启用上拉/下拉关闭。默认不启用

					.setShowCloseButton(showCloseButton)// 是否显示关闭页面按钮，在页面左下角。默认显示
					.setCloseIconResId(R.drawable.ic_action_close)// 设置关闭按钮图片资源，可不填，默认为：R.drawable.ic_action_close

					.setShowDownButton(showDownButton)// 是否显示下载按钮，在页面右下角。默认显示
					.setDownIconResId(R.drawable.icon_download_new)// 设置下载按钮图片资源，可不填，默认为：R.drawable.icon_download_new

					.setShowIndicator(showIndicator)// 设置是否显示顶部的指示器（1/9）。默认显示
					.setErrorPlaceHolder(R.drawable.load_failed)// 设置失败时的占位图，默认为R.drawable.load_failed，设置为0时不显示
					.start();
			}
		});

		// 仅加载原图
		findViewById(R.id.buttonOrigin).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				ImagePreview
					.getInstance()
					.setContext(MainActivity.this)
					.setIndex(0)
					.setImageInfoList(imageInfoList)
					.setShowDownButton(true)
					.setLoadStrategy(ImagePreview.LoadStrategy.AlwaysOrigin)
					.setFolderName("BigImageViewDownload")
					.setScaleLevel(1, 3, 8)
					.setZoomTransitionDuration(300)

					.setEnableClickClose(enableClickClose)// 是否启用点击图片关闭。默认启用
					.setEnableDragClose(enableDragClose)// 是否启用上拉/下拉关闭。默认不启用

					.setShowCloseButton(showCloseButton)// 是否显示关闭页面按钮，在页面左下角。默认显示
					.setCloseIconResId(R.drawable.ic_action_close)// 设置关闭按钮图片资源，可不填，默认为：R.drawable.ic_action_close

					.setShowDownButton(showDownButton)// 是否显示下载按钮，在页面右下角。默认显示
					.setDownIconResId(R.drawable.icon_download_new)// 设置下载按钮图片资源，可不填，默认为：R.drawable.icon_download_new

					.setShowIndicator(showIndicator)// 设置是否显示顶部的指示器（1/9）。默认显示
					.setErrorPlaceHolder(R.drawable.load_failed)// 设置失败时的占位图，默认为R.drawable.load_failed，设置为0时不显示
					.start();
			}
		});

		// 手动模式：默认普清，手动高清
		findViewById(R.id.buttonDefault).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				ImagePreview
					.getInstance()
					.setContext(MainActivity.this)
					.setIndex(0)
					.setImageInfoList(imageInfoList)
					.setShowDownButton(true)
					.setLoadStrategy(ImagePreview.LoadStrategy.Default)
					.setFolderName("BigImageViewDownload")
					.setScaleLevel(1, 3, 8)
					.setZoomTransitionDuration(500)

					.setEnableClickClose(enableClickClose)// 是否启用点击图片关闭。默认启用
					.setEnableDragClose(enableDragClose)// 是否启用上拉/下拉关闭。默认不启用

					.setShowCloseButton(showCloseButton)// 是否显示关闭页面按钮，在页面左下角。默认显示
					.setCloseIconResId(R.drawable.ic_action_close)// 设置关闭按钮图片资源，可不填，默认为：R.drawable.ic_action_close

					.setShowDownButton(showDownButton)// 是否显示下载按钮，在页面右下角。默认显示
					.setDownIconResId(R.drawable.icon_download_new)// 设置下载按钮图片资源，可不填，默认为：R.drawable.icon_download_new

					.setShowIndicator(showIndicator)// 设置是否显示顶部的指示器（1/9）。默认显示
					.setErrorPlaceHolder(R.drawable.load_failed)// 设置失败时的占位图，默认为R.drawable.load_failed，设置为0时不显示
					.start();
			}
		});

		// 网络自适应（WiFi原图，流量普清）
		findViewById(R.id.buttonAuto).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				ImagePreview
					.getInstance()
					.setContext(MainActivity.this)
					.setIndex(0)
					.setImageInfoList(imageInfoList)
					.setShowDownButton(true)
					.setLoadStrategy(ImagePreview.LoadStrategy.NetworkAuto)
					.setFolderName("BigImageView/Download")
					.setScaleLevel(1, 3, 5)
					.setZoomTransitionDuration(300)

					.setEnableClickClose(enableClickClose)// 是否启用点击图片关闭。默认启用
					.setEnableDragClose(enableDragClose)// 是否启用上拉/下拉关闭。默认不启用

					.setShowCloseButton(showCloseButton)// 是否显示关闭页面按钮，在页面左下角。默认显示
					.setCloseIconResId(R.drawable.ic_action_close)// 设置关闭按钮图片资源，可不填，默认为：R.drawable.ic_action_close

					.setShowDownButton(showDownButton)// 是否显示下载按钮，在页面右下角。默认显示
					.setDownIconResId(R.drawable.icon_download_new)// 设置下载按钮图片资源，可不填，默认为：R.drawable.icon_download_new

					.setShowIndicator(showIndicator)// 设置是否显示顶部的指示器（1/9）。默认显示
					.setErrorPlaceHolder(R.drawable.load_failed)// 设置失败时的占位图，默认为R.drawable.load_failed，设置为0时不显示
					.start();
			}
		});

		findViewById(R.id.buttonClean).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				ImageLoader.cleanDiskCache(MainActivity.this);
				MyToast.getInstance()._short(MainActivity.this, "磁盘缓存已成功清除");
			}
		});
	}
}