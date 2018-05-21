package cc.shinichi.bigimageviewpager;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import cc.shinichi.library.bean.ImageInfo;
import cc.shinichi.library.glide.ImageLoader;
import cc.shinichi.library.view.ImagePreviewActivity;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

	String[] images = {
		"http://img3.16fan.com/live/origin/201805/21/E421b24c08446.jpg",
		"http://img3.16fan.com/live/origin/201805/21/4D7B35fdf082e.jpg",
		"http://img3.16fan.com/live/origin/201805/21/2D02ebc5838e6.jpg",
		"http://img3.16fan.com/live/origin/201805/21/14C5e483e7583.jpg",
		"http://img3.16fan.com/live/origin/201805/21/A1B17c5f59b78.jpg",
		"http://img3.16fan.com/live/origin/201805/21/94699b2be3cfa.jpg",
		"http://img3.16fan.com/live/origin/201805/21/14C5e483e7583.jpg",
		"http://img3.16fan.com/live/origin/201805/21/EB298ce595dd2.jpg",
		"http://img3.16fan.com/live/origin/201805/21/264Ba4860d469.jpg"
	};

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ImageInfo imageInfo;
		final List<ImageInfo> imageInfoList = new ArrayList<>();
		for (int i = 0; i < images.length; i++) {
			imageInfo = new ImageInfo();
			imageInfo.setOriginUrl(images[i]);// 原图
			imageInfo.setThumbnailUrl(images[i].concat("-600"));// 缩略图
			imageInfoList.add(imageInfo);
			imageInfo = null;
		}

		findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				ImagePreviewActivity.activityStart(MainActivity.this, imageInfoList, 0);
			}
		});

		findViewById(R.id.button_clean).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				ImageLoader.cleanDiskCache(MainActivity.this);
			}
		});
	}
}