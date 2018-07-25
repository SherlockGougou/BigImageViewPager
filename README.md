### BigImage + ImageView + ViewPager = BigImageViewPager
一个图片浏览器，支持超大图、超长图、支持手势放大、支持查看原图、下载、加载百分比进度显示。采用区块复用加载，优化内存占用，有效避免OOM。

# 截图
![截图1.jpg](https://upload-images.jianshu.io/upload_images/1710902-11827e4c9c08fc86.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

![截图2.jpg](https://upload-images.jianshu.io/upload_images/1710902-213bed170b37f027.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)



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
// 此处显示的均是最新版本号：
	implementation 'com.github.SherlockGougou:BigImageViewPager:v1.1.1'// glide 4.x 请依赖这个
	implementation 'com.github.SherlockGougou:BigImageViewPager:v3_1.0.0'// glide 3.x 请依赖这个
}
```

# 注意：
由于glide4.x的api变更较大，无法与glide3.x共存，故，本框架后期将维护两个版本的不同分支。


#### 调用方式
根据需求生成图片源：
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
					.setZoomTransitionDuration(500)// 设置缩放的动画时长
					.start();// 开始跳转
```
# DEMO体验
![DEMO 扫码体验](https://upload-images.jianshu.io/upload_images/1710902-b4e2ea2bb1425fa1.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)



# GitHub源码
https://github.com/SherlockGougou/BigImageViewPager

# 致谢
- 本框架核心是开源作者 [davemorrissey](https://github.com/davemorrissey) 的 [subsampling-scale-image-view](https://github.com/davemorrissey/subsampling-scale-image-view)，在此感谢他的付出！
对原作感兴趣的，可以去研究学习 ---> [传送门点我](https://github.com/davemorrissey/subsampling-scale-image-view)
- okhttp 进度监听部分代码，借鉴使用了[GlideImageView](https://github.com/sunfusheng/GlideImageView)，在此对其表示感谢，喜欢其作品的可以移步去查看学习


# Bug反馈、增加需求，加 QQ 交流群：
### 欢迎加入“大话安卓”技术交流群，一起分享，共同进步##

![欢迎加入“大话安卓”技术交流群，互相学习提升](http://upload-images.jianshu.io/upload_images/1956769-326c166b86ed8e94.JPG?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
