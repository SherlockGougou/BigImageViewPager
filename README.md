<p align="center">
  <img src="image/cover.png" alt="BigImageViewPager" width="80%">
</p>

<h1 align="center">BigImageViewPager</h1>

<p align="center">
  📷 一个支持 <b>超大图 / 超长图 / 动图 / 视频</b> 的轻量级浏览器库  
</p>

<p align="center">
  <a href="https://maven.org/"><img src="https://img.shields.io/maven-central/v/com.gouqinglin/BigImageViewPager.svg?label=Maven%20Central&color=blue" alt="Maven"></a>
  <a href="https://github.com/SherlockGougou/BigImageViewPager/stargazers"><img src="https://img.shields.io/github/stars/SherlockGougou/BigImageViewPager.svg?style=flat&color=gold" alt="Stars"></a>
  <a href="https://github.com/SherlockGougou/BigImageViewPager/blob/master/LICENSE"><img src="https://img.shields.io/github/license/SherlockGougou/BigImageViewPager?color=green" alt="License"></a>
</p>

---

## ✨ 特性

- 支持 **超大图 / 超长图 / GIF / 视频**
- 支持 **手势缩放 / 滑动切换**
- 支持 **原图查看 / 下载 / 加载进度**
- 支持 **动态更新 / 删除数据源**
- **区块复用加载** → 优化内存占用，有效避免 OOM

---

## 📦 安装体验

<p align="center">
  <img src="image/qrcode.png" width="250" alt="安装二维码">
</p>

---

## 🆕 更新日志

- **androidx-9.2.0**：ExoPlayer (Media3) 依赖改为可选，仅需图片预览的用户无需引入视频相关依赖，减小APK体积
- **androidx-8.4.7**：支持32位的16KB page size特性
- **androidx-8.4.6**：新增支持res资源
- **androidx-8.4.5**：新增自定义请求头功能
- **androidx-8.4.4**：修复本地视频加载存在的问题

---

## 🎨 界面展示

<p align="center">
  <img src="image/1.jpg" width="200">
  <img src="image/2.jpg" width="200">
  <img src="image/3.jpg" width="200">
</p>

<p align="center">
  <img src="image/4-video-hor.jpg" width="600">
</p>

<p align="center">
  <img src="image/5-long-ver.jpg" width="300">
  <img src="image/6-long-hor.jpg" width="300">
</p>

---

## 🎬 视频展示

https://github.com/user-attachments/assets/101d706c-d8d0-455b-b38a-6c42282c03e2

https://github.com/user-attachments/assets/b7becb07-e6bd-459b-9795-d048d02fb21b

https://github.com/user-attachments/assets/070caa63-8e9f-4b11-9240-4cb56440d3c2

---

## ⭐️ Star 曲线

[![Star History Chart](https://api.star-history.com/svg?repos=SherlockGougou/BigImageViewPager&type=Date)](https://star-history.com/#SherlockGougou/BigImageViewPager&Date)

---

## 🚀 快速开始

### 1️⃣ 添加依赖

在 `project/build.gradle` 中加入：

```gradle
allprojects {
    repositories {
        mavenCentral()
    }
}
```

在 `app/build.gradle`
中添加：最新版本 <a href="https://maven.org/"><img src="https://img.shields.io/maven-central/v/com.gouqinglin/BigImageViewPager.svg?label=Maven%20Central&color=blue" alt="Maven"></a>

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

### 1.1️⃣ 版本迁移说明（模块拆分后）

- `BigImageViewPager`：核心库（图片能力）
- `BigImageViewPager-media3`：可选视频插件（Media3 实现）

升级建议：

- 仅图片预览：只保留 `BigImageViewPager`
- 需要视频预览：额外添加 `BigImageViewPager-media3`

发布者说明（源码仓库）：

- 核心库 artifactId：`BigImageViewPager`
- 视频插件 artifactId：`BigImageViewPager-media3`
- 两个 artifact 建议使用同一版本号发布

### 1.2️⃣ 运行时行为说明

- 未引入 `BigImageViewPager-media3`：视频条目会自动降级为“视频能力不可用”提示，不影响图片预览。
- 引入 `BigImageViewPager-media3`：视频能力自动启用，无需额外初始化代码。
- 预览页关闭时会释放视频运行时资源；再次进入预览会自动重建缓存与播放器会话。

### 2️⃣ 配置 GlideModule

```
@GlideModule
public class MyAppGlideModule extends AppGlideModule {
  @Override
  public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
    super.registerComponents(context, glide, registry);
    registry.replace(
      GlideUrl.class,
      InputStream.class,
      new OkHttpUrlLoader.Factory(ProgressManager.getOkHttpClient())
    );
  }
}
```

⚠️ 必须配置，否则原图加载进度可能卡在 1%！

### 3️⃣ 调用示例

```
ImagePreview
    .getInstance()
    .setContext(MainActivity.this)
    .setMediaInfoList(imageInfoList)
    .start();
```

### 详细配置文档：<a href="https://github.com/SherlockGougou/BigImageViewPager/blob/master/doc/DETAIL.md">详细文档</a>。

### 🙏 致谢

- <a href="https://github.com/davemorrissey/subsampling-scale-image-view">subsampling-scale-image-view</a> —— 提供大图加载核心
- <a href="https://github.com/sunfusheng/GlideImageView">GlideImageView</a> —— 进度监听实现参考

### 💬 社区交流

<p align="center">
  <img src="image/qq.png" width="260" alt="QQ群">
</p>

### ☕ 支持我

<p align="center">
  <img src="image/pay-wepay.jpg" width="280">
  <img src="image/pay-alipay.jpg" width="280">
</p>

### 📄 License

```
Copyright (C) 2018 SherlockGougou
Licensed under the Apache License, Version 2.0
http://www.apache.org/licenses/LICENSE-2.0
```

<p align="center">
  <a href="https://dartnode.com" title="Powered by DartNode - Free VPS for Open Source">
    <img src="https://dartnode.com/branding/DN-Open-Source-sm.png">
  </a>
</p>
