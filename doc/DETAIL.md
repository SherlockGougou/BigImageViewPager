# 详细用法

## 一、添加依赖

#### Step 1. 在你project层级的build.gradle中，添加仓库地址:

```
    allprojects {
        repositories {
            ...
            mavenCentral()
        }
    }
```

#### Step 2. 在你app的build.gradle中添加依赖：

当前最新版本为：<a href="https://maven.org/"><img src="https://img.shields.io/maven-central/v/com.gouqinglin/BigImageViewPager.svg?label=Maven%20Central&color=blue" alt="Maven"></a>

```
    dependencies {
        // 必选：核心框架（仅图片能力）
        implementation 'com.gouqinglin:BigImageViewPager:版本号'

        // 可选：视频插件（需要视频预览时再添加）
        implementation 'com.gouqinglin:BigImageViewPager-media3:版本号'
    
        // 必选：Glide
        def glideVersion = "4.16.0"
        implementation "com.github.bumptech.glide:glide:$glideVersion"
        annotationProcessor "com.github.bumptech.glide:compiler:$glideVersion"
        implementation "com.github.bumptech.glide:okhttp3-integration:$glideVersion"

        // 源码集成（本仓库本地调试）
        // implementation(project(":library"))
        // implementation(project(":library-video-media3")) // 可选视频插件
    }
```

#### Step 2.1. 模块拆分后的依赖迁移说明

- `BigImageViewPager`：核心库（图片能力）
- `BigImageViewPager-media3`：可选视频插件（Media3 实现）

按需接入：

- 仅图片场景：仅添加 `BigImageViewPager`
- 图片 + 视频场景：同时添加 `BigImageViewPager` 与 `BigImageViewPager-media3`

发布者（仓库维护者）建议：

- 两个 artifact 使用同一版本号发布
- 保持 `BigImageViewPager` 兼容历史依赖
- 将新增能力放在 `BigImageViewPager-media3`，避免核心库体积膨胀

#### Step 2.2. 运行时行为

- 未添加 `BigImageViewPager-media3`：视频自动降级为不支持提示，图片能力不受影响。
- 已添加 `BigImageViewPager-media3`：视频能力自动启用，无需额外初始化。
- 预览 Activity 结束时会释放视频运行时资源；下次进入会自动重建缓存与播放会话。

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

#### Step 5. 针对Android 16KB对齐问题，本库已完成支持，如果您的AGP(Android gradle plugin)版本低于8.5.1，还需要在app模块的build.gradle中额外增加以下配置，否则会显示支持16KB，但NON 16KB STORED：

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

#### 3：自定义多种配置，请查看Demo相关代码

<a href="https://github.com/SherlockGougou/BigImageViewPager/blob/master/sample/src/main/java/cc/shinichi/bigimageviewpager/MainActivity.java">
MainActivity.java</a>

#### 4：加载策略介绍

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

### 鉴权图片的稳定缓存键

当同一图片的 URL 仅鉴权参数变化时，可用 `setImageCacheKeyProvider` 提供稳定缓存键。
该配置统一作用于图片预览、查看原图、保存图片和原始数据缓存查询。
网络请求、失败后的 HTTP 下载及进度回调仍使用完整 URL；视频播放缓存不受影响。

推荐直接使用后端资源标识、内容版本和图片规格。下面的 `photos` 表示调用方的业务数据：

```kotlin
val cacheKeysByUrl = buildMap<String, String> {
    photos.forEach { photo ->
        put(photo.originUrl, "$accountId:${photo.id}:${photo.version}:original")
        put(photo.thumbnailUrl, "$accountId:${photo.id}:${photo.version}:thumb-200")
    }
}

ImagePreview.instance
    .with(activity)
    .setMediaInfoList(imageInfoList)
    .setImageCacheKeyProvider { url -> cacheKeysByUrl[url] }
    .start()
```

Java 调用同样支持 lambda：

```java
ImagePreview.getInstance()
    .with(activity)
    .setMediaInfoList(imageInfoList)
    .setImageCacheKeyProvider(url -> cacheKeysByUrl.get(url))
    .start();
```

- 未设置提供器或返回 `null` 时，保持完整 URL 缓存键；返回空白字符串会抛出异常。
- 提供器只接收 HTTP/HTTPS URL。本地文件、`content://` 和资源 URI 保持原有加载方式。
- 缩略图与原图内容不同、尺寸或版本不同、账号权限范围不同，必须使用不同键。
- 同一个完整 URL 如果同时作为原图和缩略图，映射中的缓存键必须一致。
- 若按 URL 删除鉴权参数，应使用 URI 解析器，仅删除业务明确确认不影响内容的参数。
- 该回调应快速、线程安全、结果稳定。不要发起网络请求，也不要捕获 Activity。
- 预览关闭和 `reset()` 会清除此配置，下次打开需要重新设置。
- 缓存命中会直接读取本地文件，不重新向服务器验证签名是否过期。业务若禁止复用，应禁用预览缓存或使用不同的授权范围键。
- 本库的文件下载使用 `DATA` 保存原始数据。`downloadOnly()` 不应改为 `RESOURCE` 或 `ALL`，否则会尝试编码 File 资源而失败。
- 宿主仍需按上文配置 `AppGlideModule` 和 Glide 注解处理器，以自动注册库的缓存 model。

回归测试位于 `sample/src/androidTest/`，使用本地 MockWebServer 验证真实 Glide 磁盘缓存和网络请求。
在指定设备运行：`ANDROID_SERIAL=<设备序列号> ./gradlew :sample:connectedDebugAndroidTest`。

#### 4.5：视频播放策略

`VideoPlayPolicy` 控制视频类型的自动播放行为，不影响手动点击播放按钮：

```
enum class VideoPlayPolicy {
    /** 仅 WiFi 下自动播放 */
    WiFiOnly,

    /** WiFi 和移动数据下都自动播放（默认） */
    WiFiAndMobile,

    /** 不自动播放，需要手动点击播放按钮 */
    Manual
}
```

用法：

```
// 链式调用
ImagePreview
    .getInstance()
    .setContext(MainActivity.this)
    .setMediaInfoList(imageInfoList)
    .setVideoPlayPolicy(ImagePreview.VideoPlayPolicy.WiFiOnly) // 仅 WiFi 自动播放
    .start();

// DSL 风格
ImagePreview.show(this) {
    setMediaInfoList(imageInfoList)
    setIndex(0)
    setVideoPlayPolicy(ImagePreview.VideoPlayPolicy.WiFiOnly)
}
```

生效时机：
- 首次加载视频页（initData）
- 滑动切换到视频页（onSelected）
- 从后台恢复时（onResume）

> 注：手动点击播放按钮不受此策略限制，用户始终可以主动播放。

#### 5：完全自定义预览界面布局：

详细操作请参考Demo：<https://github.com/SherlockGougou/BigImageViewPager/blob/master/sample/src/main/java/cc/shinichi/bigimageviewpager/MainActivity.java>

#### 6：Q\&A

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
