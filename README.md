### BigImage + ImageView + ViewPager = BigImageViewPager

一个图片浏览器，支持超大图、超长图、支持手势放大、支持查看原图、下载、加载百分比进度显示。采用区块复用加载，优化内存占用，有效避免OOM。支持手势下拉退出。
#### 注意：本框架支持网络图片、本地图片、支持gif动图。

# 框架特性
- 支持网络图片、本地图片；
- 支持https图片；
- 支持缩放比例、缩放动画时间的设置；
- 支持手势下拉关闭；
- 支持多种加载策略：（仅普清、仅原图、手动模式、网络自适应）
- 支持多种类型图片（超大图、超长图、超宽图、小图、gif动图）
- 支持查看原图，支持查看原图时百分比进度的展示；
- 看图体验优化：长图拉到屏幕宽度显示、宽图双击放大到屏幕高度显示、gif图放大到屏幕宽度显示；
- 自动根据Exif信息旋转图片进行显示；
- 支持图片的下载，可设置保存到的路径（默认在存储根目录新建文件夹）
- 支持多种界面的自定义（具体可查看Demo）
- 支持加载失败时占位图的设置；
- 针对保存图片进行优化，文件扩展名使用文件头部Mime信息进行设置，不用担心gif保存成jpeg；

# 截图

# 推荐扫描二维码进行安装体验：

![扫码下载demo](https://upload-images.jianshu.io/upload_images/1710902-0073c2f34a714fe2.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

![v1.2.6新增可下拉/上拉关闭](https://upload-images.jianshu.io/upload_images/1710902-08b5d2e3e9696f9f.gif?imageMogr2/auto-orient/strip)

![QQ20181108-0.png](https://upload-images.jianshu.io/upload_images/1710902-9c9f417905c52934.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

![主要功能概览](https://upload-images.jianshu.io/upload_images/1710902-2c4cae8d0ddaef1f.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

# 用法
#### 添加依赖
##### Step 1. 在你project层级的build.gradle中，添加仓库地址:
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
##### Step 2. 在你主module的build.gradle中添加依赖：

##### 此处显示的是本框架的最新版本号：
```
对于glide4.x : 使用 v4_3.2.2
对于glide3.x : 使用 v3_3.2.2
```

```
dependencies {

  // 针对glide v4 版本：如果您的app中没有使用glide任何版本，或者使用了glide，且glide版本号为4.x，请依赖以下库：

  // 主库，必须添加！
  implementation 'com.github.SherlockGougou:BigImageViewPager:v4_3.2.2'
  // v7支持库，必须添加！
  implementation 'com.android.support:appcompat-v7:27.1.1'
  // 由于本框架使用了glide和okhttp3，所以还请增加依赖以下框架，必须添加！
  // 如果您app中已经依赖某一个的话，可以略过那一个，但要保证以下这些库的版本号一致：
  implementation 'com.github.bumptech.glide:glide:4.8.0'
  annotationProcessor 'com.github.bumptech.glide:compiler:4.8.0'
  implementation 'com.github.bumptech.glide:okhttp3-integration:4.8.0'

================================v4/v3分割线==================================

  // 针对glide v3 版本：如果您的app中已经使用了glide，且glide版本号为3.x，仅需要依赖以下库：
  implementation 'com.github.SherlockGougou:BigImageViewPager:v3_3.2.2'
  implementation 'com.android.support:appcompat-v7:27.1.1'
}
```

##### Step 3. 在您的主module里，添加自定义AppGlideModule
######（注意！！！如果您用的是glide 3.x版本，不需要做这一步的操作，上一步的依赖后就完事儿了；
###### 但如果您用的是glide 4.x版本，并且您的app中已经存在了自定义的GlideModule，您只需要把下面的那一行代码，添加到对应的重载方法中即可。）例如：
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

##### Step 4. 以上操作完成后，请点击顶部按钮：Build->Rebuild Project，等待重建完成，至此，框架添加完成。如遇到任何问题，请附带截图提issues，我会及时回复，或添加底部QQ群，进行交流。

## 调用方式

#### 1：生成图片源：（如果你有缩略图和原图两种路径，请使用下面的方式，进行图片List的生成；如果你是本地图片或者只有一张图片，可以跳过这一步）
```
		ImageInfo imageInfo;
		final List<ImageInfo> imageInfoList = new ArrayList<>();
		for (String image : images) {
			imageInfo = new ImageInfo();
			imageInfo.setOriginUrl(url);// 原图url
			imageInfo.setThumbnailUrl(thumbUrl);// 缩略图url
			imageInfoList.add(imageInfo);
		}
```

#### 2：最简单的调用方式：
```
        // 最简单的调用，即可实现大部分需求，如需定制，可参考下一步的自定义代码
        
        ImagePreview
            .getInstance()
            // 上下文，必须是activity，不需要担心内存泄漏，本框架已经处理好；
            .setContext(MainActivity.this)
            
            // 有三种设置数据集合的方式，根据自己的需求进行选择：
            
            // 第一步生成的imageInfo List
            .setImageInfoList(imageInfoList)
            
            // 直接传url List
            //.setImageList(List<String> imageList)
            
            // 只有一张图片的情况，可以直接传入这张图片的url
            //.setImage(String image)
            
            // 开启预览
            .start();
            
        // 默认的配置为：
        //  显示顶部进度指示器、
        //  显示右侧下载按钮、
        //  隐藏左侧关闭按钮、
        //  开启点击图片关闭、
        //  关闭下拉图片关闭、
        //  加载方式为手动模式 
```

##### 3：自定义多种配置：
```
        ImagePreview
            .getInstance()
            // 上下文，必须是activity，不需要担心内存泄漏，本框架已经处理好
            .setContext(MainActivity.this)
            // 从第几张图片开始，索引从0开始哦~
            .setIndex(0)
            
            // 有三种设置数据集合的方式，根据自己的需求进行选择：
            
            // 第一步生成的imageInfo List
            .setImageInfoList(imageInfoList)
            
            // 直接传url List
            //.setImageList(List<String> imageList)
            
            // 只有一张图片的情况，可以直接传入这张图片的url
            //.setImage(String image)
            
            // 加载策略，详细说明见下面“加载策略介绍”。默认为手动模式
            .setLoadStrategy(ImagePreview.LoadStrategy.AlwaysThumb)
            
            // 保存的文件夹名称，会在SD卡根目录进行文件夹的新建。
            // (你也可设置嵌套模式，比如："BigImageView/Download"，会在SD卡根目录新建BigImageView文件夹，并在BigImageView文件夹中新建Download文件夹)
            .setFolderName("BigImageViewDownload")
            
            // 缩放动画时长，单位ms
            .setZoomTransitionDuration(300)

            // 是否启用点击图片关闭。默认启用
            .setEnableClickClose(enableClickClose)
            // 是否启用上拉/下拉关闭。默认不启用
            .setEnableDragClose(enableDragClose)

            // 是否显示关闭页面按钮，在页面左下角。默认不显示
            .setShowCloseButton(showCloseButton)
            // 设置关闭按钮图片资源，可不填，默认为：R.drawable.ic_action_close
            .setCloseIconResId(R.drawable.ic_action_close)

            // 是否显示下载按钮，在页面右下角。默认显示
            .setShowDownButton(showDownButton)
            // 设置下载按钮图片资源，可不填，默认为：R.drawable.icon_download_new
            .setDownIconResId(R.drawable.icon_download_new)

            // 设置是否显示顶部的指示器（1/9）默认显示
            .setShowIndicator(showIndicator)
            
            // 设置失败时的占位图，默认为R.drawable.load_failed，设置为 0 时不显示
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
            
            // 开启预览
            .start();

```

##### 4：加载策略介绍
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
  
  注：以上所有方式，如果原图缓存存在的情况，会默认加载原图缓存保证清晰度；且原图缓存只要存在，就不会显示查看原图按钮。
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

# LICENSE
Copyright (C) 2018 SherlockGougou 18883840501@163.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.