<p align="center">
    <img src="image/cover.png" alt="头图">
</p>

### BigImageViewPager = BigImage + ImageView + ViewPager

BigImageViewPager是一个图片/视频浏览器库，支持超大图、超长图、动图、视频，支持手势，支持查看原图、下载、加载百分比进度显示，支持动态更新/删除数据源。采用区块复用加载，优化内存占用，有效避免OOM。

# 推荐扫描二维码进行安装体验：

<p align="start">
  <img src="image/qrcode.png" width="300" alt="蒲公英">
</p>

# 界面展示：
<p align="center">
  <img src="image/1.jpg" width="200" alt="界面">
  <img src="image/2.jpg" width="200" alt="界面">
  <img src="image/3.jpg" width="200" alt="界面">
</p>
<p align="center">
  <img src="image/4-video-hor.jpg" width="600" alt="界面">
</p>
<p align="center">
  <img src="image/5-long-ver.jpg" width="300" alt="界面">
  <img src="image/6-long-hor.jpg" width="300" alt="界面">
</p>

# 视频展示：
https://github.com/user-attachments/assets/101d706c-d8d0-455b-b38a-6c42282c03e2

https://github.com/user-attachments/assets/b7becb07-e6bd-459b-9795-d048d02fb21b

https://github.com/user-attachments/assets/070caa63-8e9f-4b11-9240-4cb56440d3c2

## ⭐️⭐️Star数量曲线⭐️⭐️
[![Star History Chart](https://api.star-history.com/svg?repos=SherlockGougou/BigImageViewPager&type=Date)](https://star-history.com/#SherlockGougou/BigImageViewPager&Date)

# 用法
### 一、添加依赖
#### Step 1. 在你project层级的build.gradle中，添加仓库地址:
```
    allprojects {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }
```
#### Step 2. 在你app的build.gradle中添加依赖：
当前最新版本为：```androidx-8.3.4```
```
    dependencies {
        // 本框架
        implementation 'com.github.SherlockGougou:BigImageViewPager:androidx-8.3.4'

        // glide
        def glideVersion = "4.16.0"
        implementation "com.github.bumptech.glide:glide:${glideVersion}"
        annotationProcessor "com.github.bumptech.glide:compiler:${glideVersion}"
        implementation "com.github.bumptech.glide:okhttp3-integration:${glideVersion}"
    
        // ExoPlayer https://developer.android.com/media/media3/exoplayer/hello-world?hl=zh-cn#groovy
        def media3Version = "1.4.1"
        implementation "androidx.media3:media3-exoplayer:${media3Version}"
        implementation "androidx.media3:media3-exoplayer-dash:${media3Version}"
        implementation "androidx.media3:media3-ui:${media3Version}"
    }
```
#### Step 3. 在你app中添加AppGlideModule。需要继承AppGlideModule并添加以下代码到对应的重载方法中，例如：
```
    @GlideModule
    public class MyAppGlideModule extends AppGlideModule {
      @Override
      public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        super.registerComponents(context, glide, registry);

        // 替换底层网络框架为okhttp3，这步很重要！如果不添加会无法正常显示原图的加载百分比，或者卡在1%
        // 如果你的app中已经存在了自定义的GlideModule，你只需要把这一行代码，添加到对应的重载方法中即可。
        registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(ProgressManager.getOkHttpClient()));
      }
    }
```
#### Step 4. 以上操作完成后，请点击顶部按钮：Build->Rebuild Project，等待重建完成，至此，框架添加完成。如遇到任何问题，请附带截图提issues，我会及时回复，或添加底部QQ群，进行交流。

#### Step 5. 针对Android 16KB对齐问题，本库已完成支持，如果您的APG版本低于8.5.1，需要在app模块的build.gradle中额外增加以下配置，否则会显示支持16KB，但NON 16KB STORED：
```
    # 参见：https://developer.android.com/guide/practices/page-sizes?hl=zh-cn
    packagingOptions {
        jniLibs {
            useLegacyPackaging true
        }
    }
```

## 二、调用方式

#### 1：生成图片源：
```
    ImageInfo imageInfo;
    final List<ImageInfo> imageInfoList = new ArrayList<>();
    for (String image : images) {
            imageInfo = new ImageInfo();
            imageInfo.setType(Type.VIDEO);// 指定媒体类型：VIDEO/IMAGE
            imageInfo.setOriginUrl(url);// 原图url
            imageInfo.setThumbnailUrl(thumbUrl);// 缩略图url
            imageInfoList.add(imageInfo);
    }
```
#### 2：最简单的调用方式：
```
    // 最简单的调用，即可实现大部分需求，如需定制，可参考Demo相关代码：
    ImagePreview
        .getInstance()
        .setContext(MainActivity.this)
        .setMediaInfoList(imageInfoList)
        .start();
```
##### 3：自定义多种配置，请查看Demo相关代码
<a href="https://github.com/SherlockGougou/BigImageViewPager/blob/master/sample/src/main/java/cc/shinichi/bigimageviewpager/MainActivity.java">MainActivity.java</a>
##### 4：加载策略介绍
```
    enum class LoadStrategy {
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
        Default,

        /**
         * 全自动模式：WiFi原图，流量下默认普清，可点击按钮查看原图
         */
        Auto
    }
```
注：以上所有方式，如果原图缓存存在，会默认加载原图缓存保证清晰度；且原图缓存只要存在，就不会显示查看原图按钮。
##### 5：完全自定义预览界面布局：
详细操作请参考Demo：<https://github.com/SherlockGougou/BigImageViewPager/blob/master/sample/src/main/java/cc/shinichi/bigimageviewpager/MainActivity.java>
##### 6：Q\&A
1.查看原图卡在1%？
答：请仔细查看以上第三步的操作。
2.我想展示后，动态更新某些item/删除某些item，比如，更新图片为视频、更新图片或视频的地址，该如何做？
答：框架提供了更新数据源的方法，使用示例如下：
```
// 删除，执行删除后，会自动选中前一张，并触发onPageSelected回调；如果全部都被删除，会自动关闭预览界面
ImagePreview.getInstance().getPreviewActivity().deleteItem(position);
// 更新
ImagePreview.getInstance().getPreviewActivity().updateItem(position, "new url");
ImagePreview.getInstance().getPreviewActivity().updateItem(position, "new thumbnail url", "new origin url");
ImagePreview.getInstance().getPreviewActivity().updateItem(position, "new thumbnail url", "new origin url", Type.IMAGE / Type.VIDEO);
ImagePreview.getInstance().getPreviewActivity().updateItem(position, newImageInfo);
例如：
ImageInfo imageInfo = new ImageInfo();
imageInfo.setOriginUrl("https://cdn.jeff1992.com/av/ai/video/2024/upload/am_f34ca072f2e0812204233934085111dd.mp4");
imageInfo.setThumbnailUrl("https://cdn.jeff1992.com/av/ai/video/2024/upload/am_f34ca072f2e0812204233934085111dd.mp4");
imageInfo.setType(Type.VIDEO);
ImagePreview.getInstance().getPreviewActivity().updateItem(position, imageInfo);
```
3.如果你们服务器的图片url不会变动但图片内容可能变动，需要每次都走网络加载（即跳过缓存），可以设置setSkipLocalCache来跳过本地的缓存。
注意，跳过缓存可能导致缩略图和原图加载功能出现异常；但如果你没有缩略图和原图需求，就没有影响。
除了使用setSkipLocalCache来跳过本地的缓存外，你还可以使用url拼接一个随机的参数来实现跳过缓存的效果；例如，在末尾拼接时间戳等。
# GitHub源码
<https://github.com/SherlockGougou/BigImageViewPager>
# 致谢
*   本框架核心是开源作者 [davemorrissey](https://github.com/davemorrissey) 的 [subsampling-scale-image-view](https://github.com/davemorrissey/subsampling-scale-image-view)，在此感谢他的付出！
    对原作感兴趣的，可以去研究学习 ---> [传送门点我](https://github.com/davemorrissey/subsampling-scale-image-view)
*   okhttp 进度监听部分代码，借鉴使用了[GlideImageView](https://github.com/sunfusheng/GlideImageView)，在此对其表示感谢，喜欢其作品的可以移步去查看学习
# Bug反馈、增加需求，加 QQ 交流群
<p align="start">
  <img src="image/qq.png" width="300" alt="QQ群">
</p>

# 如果有帮助到你，欢迎请我喝杯☕️：
<p align="center">
  <img src="image/pay-wepay.jpg" width="300" alt="">
  <img src="image/pay-alipay.jpg" width="300" alt="">
</p>

[![Powered by DartNode](https://dartnode.com/branding/DN-Open-Source-sm.png)](https://dartnode.com "Powered by DartNode - Free VPS for Open Source")

# LICENSE
```
Copyright (C) 2018 SherlockGougou qinglingou@gmail.com

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
