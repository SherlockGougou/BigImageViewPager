### BigImage + ImageView + ViewPager = BigImageViewPager
一个图片浏览器，支持超大图、超长图、支持手势放大、支持查看原图、下载、加载百分比进度显示。采用区块复用加载，优化内存占用，有效避免OOM。

# 截图
![截图.jpg](https://upload-images.jianshu.io/upload_images/1710902-55e84221177f0ddd.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


# 功能
- 支持多张图片（网络图片）滑动浏览，支持手势放大、双击放大。
- 支持下载，支持自定义下载目录文件夹名称。
- 查看原图支持加载进度条显示
- 支持超大图、超长图，sample中测试的大图尺寸分别是：2280 * 22116、5760 * 3840。
- 采用区块加载，不用担心OOM的风险。
- v0.0.5新增：可设置缩放比例、缩放动画时间。

# 用法
#### 添加依赖
Step 1. Add it in your root build.gradle at the end of repositories:
```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
}
```
Step 2. Add the dependency
```
dependencies {
	  implementation 'com.github.SherlockGougou:BigImageViewPager:v0.0.5'
}
```
#### 调用方式
生成图片源：
```
ImageInfo imageInfo;
		final List<ImageInfo> imageInfoList = new ArrayList<>();
		for (int i = 0; i < images.length; i++) {
			imageInfo = new ImageInfo();
			imageInfo.setOriginUrl(images[i]);// 原图
			imageInfo.setThumbnailUrl(images[i].concat("-1200"));// 缩略图，实际使用中，根据需求传入缩略图路径。如果没有缩略图url，可以将两项设置为一样，并隐藏查看原图按钮即可。
			imageInfoList.add(imageInfo);
			imageInfo = null;
		}
```
链式调用，多种配置
```
				ImagePreview
					.getInstance()
					.setContext(MainActivity.this)
					.setIndex(5)// 默认显示第几个
					.setImageInfoList(imageInfoList)// 图片集合
					.setShowDownButton(true)// 是否显示下载按钮
					.setShowOriginButton(true)// 是否显示查看原图按钮
					.setFolderName("BigImageViewDownload")// 设置下载到的文件夹名（保存到根目录）
					.setScaleLevel(1, 3, 8)// 设置三级放大倍数，分别是最小、中等、最大倍数。
					.setScaleMode(ImagePreview.MODE_SCALE_TO_MEDIUM_TO_MAX_TO_MIN)// 设置缩放模式，详细描述见下方。
					.setZoomTransitionDuration(500)// 设置缩放的动画时长
					.start();// 开始跳转
```

# 缩放模式详解
```
MODE_SCALE_TO_MEDIUM_TO_MAX_TO_MIN = 1001;// 三级放大：第一次双击放大到中等，再次双击，放大到最大，再次双击，缩小到最小。
MODE_SCALE_TO_MAX_TO_MIN = 1002;// 二级放大，最大与最小：第一次双击放大到最大，再次双击缩小到最小。
MODE_SCALE_TO_MEDIUM_TO_MIN = 1003;// 二级放大，中等与最小：第一次双击放大到中等，用户可铜过双指放大到最大。再次双击，缩小到最小。
```
# DEMO体验
![DEMO 扫码体验](https://upload-images.jianshu.io/upload_images/1710902-47d9a4367e092924.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


# TODO
- 增加本地图片浏览功能
- 自定义主题

# GitHub源码
https://github.com/SherlockGougou/BigImageViewPager

# 致谢
[diegocarloslima/ByakuGallery](https://github.com/diegocarloslima/ByakuGallery)

# Bug反馈、增加需求，加 QQ 交流群：
### 欢迎加入“大话安卓”技术交流群，一起分享，共同进步##

![欢迎加入“大话安卓”技术交流群，互相学习提升](http://upload-images.jianshu.io/upload_images/1956769-326c166b86ed8e94.JPG?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
