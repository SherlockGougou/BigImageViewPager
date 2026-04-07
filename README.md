<p style="text-align: center;">
  <img src="image/cover.png" alt="BigImageViewPager" style="max-width: 80%; width: 100%; height: auto;" />
</p>

# BigImageViewPager

📷 一个支持 **超大图 / 超长图 / GIF / 视频** 的轻量级预览库

[![Maven](https://img.shields.io/maven-central/v/com.gouqinglin/BigImageViewPager.svg?label=Maven%20Central&color=blue)](https://maven.org/)
[![Stars](https://img.shields.io/github/stars/SherlockGougou/BigImageViewPager.svg?style=flat&color=gold)](https://github.com/SherlockGougou/BigImageViewPager/stargazers)
[![License](https://img.shields.io/github/license/SherlockGougou/BigImageViewPager?color=green)](https://github.com/SherlockGougou/BigImageViewPager/blob/master/LICENSE)

---

## 目录

- [特性](#特性)
- [安装](#安装)
- [快速开始](#快速开始)
- [模块拆分与迁移](#模块拆分与迁移)
- [运行时行为](#运行时行为)
- [更新日志](#更新日志)
- [界面与视频展示](#界面与视频展示)
- [社区与支持](#社区与支持)
- [License](#license)

---

## 特性

- 支持 **超大图 / 超长图 / GIF / 视频**
- 支持 **双指缩放 / 平移 / 左右滑动切换**
- 支持 **原图查看 / 下载 / 加载进度监听**
- 支持 **数据源动态更新与删除**
- 采用 **区块复用加载**，有效降低内存占用并减少 OOM 风险

---

## 安装

> 当前版本：`androidx-9.2.0`

### 1) 添加仓库

在你的项目仓库配置中确保包含 `mavenCentral()`。

### 2) 添加依赖

`BigImageViewPager` 已拆分为核心库与可选视频插件：

- 核心库（必选）：`com.gouqinglin:BigImageViewPager`
- 视频插件（可选）：`com.gouqinglin:BigImageViewPager-media3`

```gradle
dependencies {
    // 必选：核心库（图片能力）
    implementation "com.gouqinglin:BigImageViewPager:androidx-9.2.0"

    // 可选：视频插件（需要视频预览时添加）
    implementation "com.gouqinglin:BigImageViewPager-media3:androidx-9.2.0"

    // 必选：Glide
    def glideVersion = "4.16.0"
    implementation "com.github.bumptech.glide:glide:$glideVersion"
    annotationProcessor "com.github.bumptech.glide:compiler:$glideVersion"
    implementation "com.github.bumptech.glide:okhttp3-integration:$glideVersion"
}
```

源码方式接入（本仓库本地调试）：

```gradle
// implementation(project(":library"))
// implementation(project(":library-video-media3"))
```

---

## 快速开始

### 1) 配置 GlideModule

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

> 注意：如果不配置，原图加载进度可能长期停在 1%。

### 2) 发起预览

```
ImagePreview.getInstance().setContext(MainActivity.this).setMediaInfoList(imageInfoList).start();
```

更多参数配置见：[`doc/DETAIL.md`](doc/DETAIL.md)

---

## 模块拆分与迁移

`androidx-9.2.0` 起，视频能力从核心库中解耦为可选插件：

- `BigImageViewPager`：核心图片能力
- `BigImageViewPager-media3`：基于 Media3 的视频能力

迁移建议：

- 仅图片预览：保留 `BigImageViewPager` 即可
- 需要视频预览：额外添加 `BigImageViewPager-media3`

发布建议（源码仓库维护者）：

- 核心库 artifactId：`BigImageViewPager`
- 视频插件 artifactId：`BigImageViewPager-media3`
- 两个 artifact 保持相同版本号发布

---

## 运行时行为

- 未引入 `BigImageViewPager-media3`：视频条目自动降级为“视频能力不可用”提示，不影响图片预览。
- 引入 `BigImageViewPager-media3`：视频能力自动启用，无需额外初始化。
- 关闭预览页时会释放视频运行时资源；再次进入会自动重建缓存和播放器会话。

---

## 更新日志

- `androidx-9.2.0`：ExoPlayer (Media3) 改为可选依赖，仅图片场景可减少 APK 体积
- `androidx-8.4.7`：支持 32 位 16KB page size 特性
- `androidx-8.4.6`：新增支持 `res` 资源
- `androidx-8.4.5`：新增自定义请求头功能
- `androidx-8.4.4`：修复本地视频加载问题

---

## 界面与视频展示

### 图片展示

<table>
  <tr>
    <td align="center"><img src="image/1.jpg" alt="preview-1" /></td>
    <td align="center"><img src="image/2.jpg" alt="preview-2" /></td>
    <td align="center"><img src="image/3.jpg" alt="preview-3" /></td>
  </tr>
  <tr>
    <td align="center"><img src="image/4-video-hor.jpg" alt="video-preview" /></td>
    <td align="center"><img src="image/5-long-ver.jpg" alt="long-vertical" /></td>
    <td align="center"><img src="image/6-long-hor.jpg" alt="long-horizontal" /></td>
  </tr>
</table>

### 视频展示

<table>
  <tr>
    <td align="center">
      <video controls preload="metadata" width="100%" src="https://github.com/user-attachments/assets/101d706c-d8d0-455b-b38a-6c42282c03e2"></video>
    </td>
    <td align="center">
      <video controls preload="metadata" width="100%" src="https://github.com/user-attachments/assets/b7becb07-e6bd-459b-9795-d048d02fb21b"></video>
    </td>
    <td align="center">
      <video controls preload="metadata" width="100%" src="https://github.com/user-attachments/assets/070caa63-8e9f-4b11-9240-4cb56440d3c2"></video>
    </td>
  </tr>
</table>

### Star 曲线

[![Star History Chart](https://api.star-history.com/svg?repos=SherlockGougou/BigImageViewPager&type=Date)](https://star-history.com/#SherlockGougou/BigImageViewPager&Date)

---

## 社区与支持

### 社区交流

<p style="text-align: center;">
  <a title="qq">
    <img src="image/qq.png" alt="qq" style="width: 30%; height: auto;" />
  </a>
</p>

### 支持项目

<table>
  <tr>
    <td align="center"><img src="image/pay-wepay.jpg" alt="wechat-pay" style="width: 40%; height: auto;" /></td>
    <td align="center"><img src="image/pay-alipay.jpg" alt="alipay" style="width: 40%; height: auto;" /></td>
  </tr>
</table>

### 致谢

- Powered by DartNode - Free VPS for Open Source

<p style="text-align: center;">
  <a href="https://dartnode.com" title="Powered by DartNode - Free VPS for Open Source">
    <img src="image/black_color_full.png" alt="dartnode" style="max-width: 60%; width: 100%; height: auto;" />
  </a>
</p>

---

## License

```text
Copyright (C) 2018 SherlockGougou
Licensed under the Apache License, Version 2.0
http://www.apache.org/licenses/LICENSE-2.0
```

