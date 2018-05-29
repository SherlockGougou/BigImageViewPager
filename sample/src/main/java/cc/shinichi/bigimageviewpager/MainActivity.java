package cc.shinichi.bigimageviewpager;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.bean.ImageInfo;
import cc.shinichi.library.glide.ImageLoader;
import cc.shinichi.library.tool.ToastUtil;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

	String[] images = {
		"http://img3.16fan.com/live/origin/201805/21/E421b24c08446.jpg",
		"http://img3.16fan.com/live/origin/201805/21/4D7B35fdf082e.jpg",
		"http://img6.16fan.com/attachments/wenzhang/201805/18/152660818127263ge.jpeg", //  5760 * 3840
		"http://img3.16fan.com/live/origin/201805/21/2D02ebc5838e6.jpg",
		"http://img6.16fan.com/attachments/wenzhang/201805/18/152660818716180ge.jpeg", //  2280 * 22116
		"http://img3.16fan.com/live/origin/201805/21/14C5e483e7583.jpg",
		"http://img3.16fan.com/live/origin/201805/21/A1B17c5f59b78.jpg",
		"http://img3.16fan.com/live/origin/201805/21/94699b2be3cfa.jpg",
		"http://img6.16fan.com/attachments/wenzhang/201805/18/152660818716180ge.jpeg", //  2280 * 22116
		"http://img3.16fan.com/live/origin/201805/21/14C5e483e7583.jpg",
		"http://img3.16fan.com/live/origin/201805/21/EB298ce595dd2.jpg",
		"http://img6.16fan.com/attachments/wenzhang/201805/18/152660818127263ge.jpeg", //  5760 * 3840
		"http://img3.16fan.com/live/origin/201805/21/264Ba4860d469.jpg",
		"http://img6.16fan.com/attachments/wenzhang/201805/18/152660818716180ge.jpeg", //  2280 * 22116
		"http://img6.16fan.com/attachments/wenzhang/201805/18/152660818127263ge.jpeg" //  5760 * 3840
	};

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ImageInfo imageInfo;
		final List<ImageInfo> imageInfoList = new ArrayList<>();
		for (int i = 0; i < images.length; i++) {
			imageInfo = new ImageInfo();
			imageInfo.setOriginUrl(images[i]);// 原图
			imageInfo.setThumbnailUrl(images[i].concat("-1200"));// 缩略图，实际使用中，根据需求传入缩略图路径。如果没有缩略图url，可以将两项设置为一样，并隐藏查看原图按钮即可。
			imageInfoList.add(imageInfo);
			imageInfo = null;
		}

		findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				ImagePreview
					.getInstance()
					.setContext(MainActivity.this)
					.setIndex(5)
					.setImageInfoList(imageInfoList)
					.setShowDownButton(true)
					.setShowOriginButton(true)
					.setFolderName("BigImageViewDownload")
					.setScaleLevel(1, 3, 8)
					.setScaleMode(ImagePreview.MODE_SCALE_TO_MEDIUM_TO_MAX_TO_MIN)
					.setZoomTransitionDuration(500)
					.start();
			}
		});

		findViewById(R.id.button_clean).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				ImageLoader.cleanDiskCache(MainActivity.this);
				ToastUtil.getInstance()._short(MainActivity.this, "clean cache complete!");
			}
		});
	}
}