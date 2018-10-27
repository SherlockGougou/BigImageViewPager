### BigImage + ImageView + ViewPager = BigImageViewPager

一个图片浏览器，支持超大图、超长图、支持手势放大、支持查看原图、下载、加载百分比进度显示。采用区块复用加载，优化内存占用，有效避免OOM，
#### 注意：支持网络图片、本地图片。

# 更新日志
- v0.0.5新增：可设置缩放比例、缩放动画时间。
- v1.2.0新增：可设置多种加载策略（仅普清、仅原图、手动模式、网络自适应）
- v1.2.1修复可能与app冲突的部分
- v1.2.2新增：长图（宽高比大于等于3的图）默认宽度放大到手机屏幕的宽度
- v1.2.3优化内存泄漏问题，优化查看原图时更新闪烁问题，简单优化进场退场动画
- v1.2.5新增：是否显示关闭页面按钮，21以上透明化状态栏
- v1.2.6新增：可选择 点击关闭、上拉/下拉关闭
- v1.2.7更新glide版本为4.8.0，更新demo
- v1.2.8修复某些手机toast不显示的问题，更改保存图片的名称为原始名称
- v1.2.9修复下载后相册看不到的问题，修复某些情况下的进度显示问题

# 截图

# gif查看不流畅，可扫描底部二维码进行安装体验
![v1.2.6新增可下拉/上拉关闭](https://upload-images.jianshu.io/upload_images/1710902-08b5d2e3e9696f9f.gif?imageMogr2/auto-orient/strip)

![Screenshot_2018-10-25-11-50-59-477.png](https://upload-images.jianshu.io/upload_images/1710902-152b7d3068c6454f.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

![Screenshot_2018-10-19-18-01-52-162副本.jpg](https://upload-images.jianshu.io/upload_images/1710902-84fb4fb928a5001d.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


# 功能
- 支持多张图片（网络图片、本地图片均支持）滑动浏览，支持手势放大、双击放大、下拉关闭。
- 支持下载，支持自定义下载目录文件夹名称。
- 查看原图支持加载进度条显示
- 支持超大图、超长图，sample中测试的大图尺寸分别是：2280 * 22116、5760 * 3840。
- 采用区块加载，不用担心OOM的风险。


# 用法
#### 添加依赖
Step 1. 在你project层级的build.gradle中，添加仓库地址:
```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
}
```
Step 2. 在你主module的build.gradle中添加依赖：

# 此处显示的是本框架的最新版本号：
[![](https://jitpack.io/v/SherlockGougou/BigImageViewPager.svg)](https://jitpack.io/#SherlockGougou/BigImageViewPager)
#### 对于glide4.x : 使用v4_1.2.9
#### 对于glide3.x : 使用v3_1.2.9

```
dependencies {

  // 针对glide v4 版本：如果您的app中没有使用glide任何版本，或者使用了glide，且glide版本号为4.x，请依赖以下库：

  // 主库，必须添加！
  implementation 'com.github.SherlockGougou:BigImageViewPager:v4_1.2.9'
  // v7支持库，必须添加！
  implementation 'com.android.support:appcompat-v7:27.1.1'
  // 由于本框架使用了glide和okhttp3，所以还请增加依赖以下框架，必须添加！
  // 如果您app中已经依赖某一个的话，可以略过那一个，但要保证以下这些库的版本号一致：
  implementation 'com.github.bumptech.glide:glide:4.8.0'
  annotationProcessor 'com.github.bumptech.glide:compiler:4.8.0'
  implementation 'com.github.bumptech.glide:okhttp3-integration:4.8.0'

============================分割线==================================

  // 针对glide v3 版本：如果您的app中已经使用了glide，且glide版本号为3.x，仅需要依赖以下库：
  implementation 'com.github.SherlockGougou:BigImageViewPager:v3_1.2.9'
  implementation 'com.android.support:appcompat-v7:27.1.1'
}
```
Step 3. 在您的主module里，添加自定义AppGlideModule（如果是glide 3.x版本，不需要做这一步），例如：

```
@GlideModule
public class MyAppGlideModule extends AppGlideModule {
  @Override
  public void registerComponents(@NonNull Context context, @NonNull Glide glide,
      @NonNull Registry registry) {
    super.registerComponents(context, glide, registry);

    // 替换底层网络框架为okhttp3，这步很重要！
    // 如果您的app中已经存在了自定义的GlideModule，您只需要把这一行代码，添加到对应的重载方法中即可。
    registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(ProgressManager.getOkHttpClient()));
  }

}
```

Step 4. 以上操作完成后，请点击顶部按钮：Build->Rebuild Project，等待重建完成，至此，框架添加完成。如遇到任何问题，请附带截图提issues，我会及时回复，或添加底部QQ群，进行交流。


#### 调用方式

根据需求生成图片源：
```
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
			imageInfo = null;
		}

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

```

链式调用，多种配置
```
		// 仅加载普清
		findViewById(R.id.buttonThumb).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				ImagePreview
					.getInstance()
					.setContext(MainActivity.this)// 上下文
					.setIndex(0)// 从第一张图片开始，索引从0开始哦
					.setImageInfoList(imageInfoList)// 图片源
					.setShowDownButton(true)// 是否显示下载按钮
					.setLoadStrategy(ImagePreview.LoadStrategy.AlwaysThumb)// 加载策略，见下面介绍
					.setFolderName("BigImageViewDownload")// 保存的文件夹名称，SD卡根目录
					.setScaleLevel(1, 3, 8)// 设置三级缩放级别
					.setZoomTransitionDuration(300)// 缩放动画时长
					.setShowCloseButton(false)// 是否显示关闭页面按钮，在页面左下角
					.setEnableDragClose(enableDragClose)// 是否启用上拉/下拉关闭，默认不启用
					.setEnableClickClose(true)// 是否启用点击图片关闭，默认启用
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
					.setShowCloseButton(true)
					.setEnableDragClose(enableDragClose)// 是否启用上拉/下拉关闭，默认不启用
					.setEnableClickClose(true)// 是否启用点击图片关闭，默认启用
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
					.setShowCloseButton(true)
					.setEnableDragClose(enableDragClose)// 是否启用上拉/下拉关闭，默认不启用
					.setEnableClickClose(true)// 是否启用点击图片关闭，默认启用
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
					.setFolderName("BigImageViewDownload")
					.setScaleLevel(1, 3, 5)
					.setZoomTransitionDuration(300)
					.setShowCloseButton(true)
					.setEnableDragClose(enableDragClose)// 是否启用上拉/下拉关闭，默认不启用
					.setEnableClickClose(true)// 是否启用点击图片关闭，默认启用
					.start();
			}
		});
```

# 加载策略介绍

```
  public enum LoadStrategy {
    /**
     * 仅加载原图；会强制隐藏查看原图按钮
     */
    AlwaysOrigin,

    /**
     * 仅加载普清；会强制隐藏查看原图按钮
     */
    AlwaysThumb,

    /**
     * 根据网络自适应加载，WiFi原图，流量普清；会强制隐藏查看原图按钮
     */
    NetworkAuto,

    /**
     * 手动模式：默认普清，点击按钮再加载原图；会根据原图、缩略图url是否一样来判断是否显示查看原图按钮
     */
    Default
  }

  以上所有方式，如果原图缓存存在的情况，会默认加载原图缓存保证清晰度，且原图缓存只要存在，就不会显示查看原图按钮
```


# DEMO体验
![扫码下载demo](https://upload-images.jianshu.io/upload_images/1710902-0073c2f34a714fe2.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)




# GitHub源码
https://github.com/SherlockGougou/BigImageViewPager

# 致谢
- 本框架核心是开源作者 [davemorrissey](https://github.com/davemorrissey) 的 [subsampling-scale-image-view](https://github.com/davemorrissey/subsampling-scale-image-view)，在此感谢他的付出！
对原作感兴趣的，可以去研究学习 ---> [传送门点我](https://github.com/davemorrissey/subsampling-scale-image-view)
- okhttp 进度监听部分代码，借鉴使用了[GlideImageView](https://github.com/sunfusheng/GlideImageView)，在此对其表示感谢，喜欢其作品的可以移步去查看学习


# Bug反馈、增加需求，加 QQ 交流群：271127803（大话安卓）
### 欢迎加入“大话安卓”技术交流群，一起分享，共同进步##

![欢迎加入“大话安卓”技术交流群，互相学习提升](https://upload-images.jianshu.io/upload_images/1710902-5cdeb8c1f58dd425.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

# License
```
Copyright 2018 SherlockGougou

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
