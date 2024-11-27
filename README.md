<p align="center">
    <img src="https://cdn.jeff1992.com/av/app/image/2024/upload/am_0c8dab2391b904d0bcf78a427bb741af.jpeg" alt="头图">
</p>

### BigImageViewPager = BigImage + ImageView + ViewPager

BigImageViewPager是一个图片/视频浏览器库，支持超大图、超长图、动图、视频，支持手势，支持查看原图、下载、加载百分比进度显示。采用区块复用加载，优化内存占用，有效避免OOM。

# 推荐扫描二维码进行安装体验：

<p align="start">
  <img src="https://www.pgyer.com/manager/dashboard/app/288e41d4f0f1251c68559c58205a70d9#" width="300" alt="蒲公英">
</p>

# 界面展示：
<p align="center">
    <video controls width="300">
      <source src="https://cdn.jeff1992.com/av/app/image/2024/upload/am_b90b7ba16a6735c34c10d4d1c3e4bba9.mp4" type="video/mp4" />
    </video>
    <video controls width="300">
      <source src="https://cdn.jeff1992.com/av/app/image/2024/upload/am_3198cad3c32246c330b2b271038210b5.mp4" type="video/mp4" />
    </video>
    <video controls width="300">
      <source src="https://cdn.jeff1992.com/av/app/image/2024/upload/am_f9bf8ccffd9f043de8aa467bfc0d0ad7.mp4" type="video/mp4" />
    </video>
</p>
<p align="center">
  <img src="https://cdn.jeff1992.com/av/app/image/2024/upload/am_c6bdee2c702b81739566986fefd2d0f3.jpg" width="200" alt="界面">
  <img src="https://cdn.jeff1992.com/av/app/image/2024/upload/am_117eeaa3a76690cc87a92b94c7c3220f.jpg" width="200" alt="界面">
  <img src="https://cdn.jeff1992.com/av/app/image/2024/upload/am_b53404299b60f1debe0c37736dd9dc86.jpg" width="200" alt="界面">
</p>
<p align="center">
  <img src="https://cdn.jeff1992.com/av/app/image/2024/upload/am_417e153c7b2327fc99ba02ffd5c4492b.jpg" width="600" alt="界面">
</p>
<p align="center">
  <img src="https://cdn.jeff1992.com/av/app/image/2024/upload/am_b3f8f59107f3a92234776791e6411da1.jpg" width="200" alt="界面">
  <img src="https://cdn.jeff1992.com/av/app/image/2024/upload/am_f02c24c3e0e94d7cb897b77ae5c0a115.jpg" width="200" alt="界面">
</p>

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
当前最新版本为：```androidx-8.0.0```
```
    dependencies {
        // 本框架
        implementation 'com.github.SherlockGougou:BigImageViewPager:androidx-8.0.0'

        // glide
        implementation 'com.github.bumptech.glide:glide:4.16.0'
        annotationProcessor 'com.github.bumptech.glide:compiler:4.16.0'// kotlin请使用kapt
        implementation 'com.github.bumptech.glide:okhttp3-integration:4.16.0'
        implementation "com.github.zjupure:webpdecoder:2.3.4.14.2"

        // ExoPlayer https://developer.android.com/media/media3/exoplayer/hello-world?hl=zh-cn#groovy
        implementation "androidx.media3:media3-exoplayer:1.4.1"
        implementation "androidx.media3:media3-exoplayer-dash:1.4.1"
        implementation "androidx.media3:media3-ui:1.4.1"
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
# GitHub源码
<https://github.com/SherlockGougou/BigImageViewPager>
# 致谢
*   本框架核心是开源作者 [davemorrissey](https://github.com/davemorrissey) 的 [subsampling-scale-image-view](https://github.com/davemorrissey/subsampling-scale-image-view)，在此感谢他的付出！
    对原作感兴趣的，可以去研究学习 ---> [传送门点我](https://github.com/davemorrissey/subsampling-scale-image-view)
*   okhttp 进度监听部分代码，借鉴使用了[GlideImageView](https://github.com/sunfusheng/GlideImageView)，在此对其表示感谢，喜欢其作品的可以移步去查看学习
# Bug反馈、增加需求，加 QQ 交流群
<p align="center">
  <img src="" width="300" alt="QQ群">
</p>

# 如果有帮助到你，欢迎请我喝杯☕️：
<p align="center">
  <img src="https://github.com/SherlockGougou/BigImageViewPager/assets/17920617/ad358723-6c5c-4878-81b9-ec77b17cda09" width="400" alt="">
  <img src="https://github.com/SherlockGougou/BigImageViewPager/assets/17920617/507f28d2-c5d0-449c-bbe6-a5e022484130" width="400" alt="">
</p>

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