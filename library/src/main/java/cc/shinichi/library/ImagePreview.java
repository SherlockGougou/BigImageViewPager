package cc.shinichi.library;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import cc.shinichi.library.bean.ImageInfo;
import cc.shinichi.library.view.ImagePreviewActivity;
import java.util.List;

/*
 * @author 工藤
 * @emil gougou@16fan.com
 * cc.shinichi.library
 * create at 2018/5/22  09:06
 * description:
 */
public class ImagePreview {

	private Context context;
	private List<ImageInfo> imageInfoList;// 图片数据集合
	private int index = 0;// 默认显示第几个
	private boolean isShowDownButton = true;// 是否显示下载按钮
	private boolean isShowOriginButton = true;// 是否显示查看原图按钮
	private String folderName = "ImagePreview";// 下载到的文件夹名（根目录中）

	public static ImagePreview getInstance() {
		return InnerClass.instance;
	}

	public static class InnerClass {
		private static ImagePreview instance = new ImagePreview();
	}

	public Context getContext() {
		return context;
	}

	public ImagePreview setContext(@NonNull Context context) {
		this.context = context;
		return this;
	}

	public List<ImageInfo> getImageInfoList() {
		return imageInfoList;
	}

	public ImagePreview setImageInfoList(@NonNull List<ImageInfo> imageInfoList) {
		this.imageInfoList = imageInfoList;
		return this;
	}

	public int getIndex() {
		return index;
	}

	public ImagePreview setIndex(int index) {
		this.index = index;
		return this;
	}

	public boolean isShowDownButton() {
		return isShowDownButton;
	}

	public ImagePreview setShowDownButton(boolean showDownButton) {
		isShowDownButton = showDownButton;
		return this;
	}

	public boolean isShowOriginButton() {
		return isShowOriginButton;
	}

	public ImagePreview setShowOriginButton(boolean showOriginButton) {
		isShowOriginButton = showOriginButton;
		return this;
	}

	public String getFolderName() {
		if (TextUtils.isEmpty(folderName)) {
			folderName = "BigImageViewDownload";
		}
		return folderName;
	}

	public ImagePreview setFolderName(@NonNull String folderName) {
		this.folderName = folderName;
		return this;
	}

	public void start() {
		if (context == null) {
			throw new IllegalArgumentException("You must call 'setContext(Context context)' first!");
		}
		if (imageInfoList == null || imageInfoList.size() == 0) {
			throw new IllegalArgumentException("Do you forget to call 'setImageInfoList(List<ImageInfo> imageInfoList)' ?");
		}
		if (this.index >= imageInfoList.size()) {
			throw new IllegalArgumentException("index out of range!");
		}
		Intent intent = new Intent();
		intent.setClass(context, ImagePreviewActivity.class);
		context.startActivity(intent);
	}
}