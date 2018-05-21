package cc.shinichi.library.glide;

import android.graphics.drawable.Drawable;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import java.io.File;

/*
 * @author 工藤
 * @emil gougou@16fan.com
 * cc.shinichi.library.glide
 * create at 2018/5/21  14:59
 * description:
 */
public class FileTarget implements Target<File> {

	@Override public void onLoadStarted(Drawable placeholder) {

	}

	@Override public void onLoadFailed(Exception e, Drawable errorDrawable) {

	}

	@Override public void onResourceReady(File resource, GlideAnimation<? super File> glideAnimation) {

	}

	@Override public void onLoadCleared(Drawable placeholder) {

	}

	@Override public void getSize(SizeReadyCallback cb) {
		cb.onSizeReady(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
	}

	@Override public void setRequest(Request request) {

	}

	@Override public Request getRequest() {
		return null;
	}

	@Override public void onStart() {

	}

	@Override public void onStop() {

	}

	@Override public void onDestroy() {

	}
}