# Maven Central Bundle 打包上传指南

## 结论与适用场景

可以绕过 `publish.sh` 使用的 OSSRH Staging API 和 `initializeSonatypeStagingRepository`：先通过仓库已有的 `maven-publish`、`signing` 配置把两个 publication 发布到一个重定向后的 Maven Local 目录，再补齐 checksum、压缩为 Maven Repository Layout bundle，最后在 Central Publisher Portal 网页上传。

这种方式只绕过 Nexus staging profile 查询，不绕过 Central 的 namespace 权限和组件校验。Portal 账号仍须拥有已验证的 `com.gouqinglin` namespace；上传后仍会检查坐标、POM、sources、Javadoc、PGP 签名和 checksum。[Sonatype：上传 bundle](https://central.sonatype.org/publish/publish-portal-upload/)；[Sonatype：发布要求](https://central.sonatype.org/publish/requirements/)

本仓库当前 publication 为：

- `com.gouqinglin:BigImageViewPager:androidx-9.2.3`
- `com.gouqinglin:BigImageViewPager-media3:androidx-9.2.3`

实际值均从 `gradle.properties` 读取。发布前只需修改 `VERSION_NAME`，两个 artifact 会继续使用相同版本。本流程面向 release；Central 的 release 版本不能以 `-SNAPSHOT` 结尾。[Sonatype：坐标和版本要求](https://central.sonatype.org/publish/requirements/#correct-coordinates)

## 前置条件

1. 当前 Portal 账号已验证并有权发布 `com.gouqinglin` namespace。[Sonatype：Central Portal 入门](https://central.sonatype.org/publish/publish-portal-guide/)
2. 以下签名环境变量已设置，且 `SIGNING_SECRET_KEY_RING_FILE` 指向可读的 secret key ring：

   ```bash
   SIGNING_KEY_ID
   SIGNING_PASSWORD
   SIGNING_SECRET_KEY_RING_FILE
   ```

   这是仓库两个 module 的 `signing` 配置所读取的变量。Gradle 官方说明，对 publication 使用 `sign(...)` 会生成 detached signature，并在发布时把 signature artifact 一同发布。[Gradle 8.5：Signing Plugin](https://docs.gradle.org/8.5/userguide/signing_plugin.html#sec:signing_publications)；[Gradle 8.5：发布签名](https://docs.gradle.org/8.5/userguide/signing_plugin.html#sec:publishing_the_signatures)
3. 对应公钥已上传到 Central 支持的 keyserver，例如 `keyserver.ubuntu.com`、`keys.openpgp.org` 或 `pgp.mit.edu`。[Sonatype：分发 PGP 公钥](https://central.sonatype.org/publish/requirements/gpg/#distributing-your-public-key)
4. 本机有 JDK 17、Android SDK/NDK、`gpg`、`zip`、`unzip`、`md5` 和 `shasum`。通过 API 上传时还需要 `curl` 和 `base64`。脚本按本仓库当前 macOS 环境编写。

`OSSRH_USERNAME` 和 `OSSRH_PASSWORD` 不参与本地 bundle 生成；网页上传使用已登录的 Central Portal 会话。通过 Publisher API 上传时，优先读取 `CENTRAL_USERNAME`、`CENTRAL_PASSWORD`，未设置时兼容读取现有的 `OSSRH_USERNAME`、`OSSRH_PASSWORD`。

## 日常发版流程

### 1. 更新版本

在 `gradle.properties` 中修改 `VERSION_NAME`。两个 artifact 必须使用同一个新版本，且 release 版本不能以 `-SNAPSHOT` 结尾。

### 2. 生成并校验 bundle

在仓库根目录执行：

```bash
./package-central.sh
```

脚本会自动执行以下工作：

1. 检查签名环境变量和本机命令。
2. 执行 `clean`，将两个 `MavenPublication` 发布到项目 `build/` 下的隔离 Maven Local 目录。
3. 检查 AAR、POM、sources 和 Javadoc 是否齐全。
4. 为所有主文件生成 `.md5`、`.sha1`。
5. 验证每个 `.asc` 签名以及两种 checksum。
6. 将两个 component 打进同一个 Maven Repository Layout ZIP。
7. 检查 ZIP 完整性并拒绝包含 `maven-metadata-local.xml` 的 bundle。

输出位置固定为：

```text
build/central-publishing/BigImageViewPager-<VERSION_NAME>-central-bundle.zip
```

生成失败时不要上传；脚本只有在全部本地检查通过后才会输出成功信息。

### 3. 上传

推荐先使用 Portal 网页上传，参见下方“Portal 上传与发布”。也可以调用 Publisher API：

```bash
export CENTRAL_USERNAME='<Portal User Token username>'
export CENTRAL_PASSWORD='<Portal User Token password>'

./upload-central.sh
```

上传脚本默认查找当前 `VERSION_NAME` 对应的 bundle，也可以传入明确路径：

```bash
./upload-central.sh build/central-publishing/BigImageViewPager-<version>-central-bundle.zip
```

脚本会在产生远端写入前询问确认。只有在 CI 等无人值守环境中，才应显式跳过确认：

```bash
./upload-central.sh --yes
```

上传固定使用 `USER_MANAGED`，只创建 deployment 并启动 validation，绝不会自动执行最终发布。Bearer 凭证通过权限为 `600` 的临时 curl 配置传递，脚本退出时自动删除，不会打印凭证。成功后会输出 deployment ID 和 Portal 地址。

### 4. 验证并最终发布

无论网页还是 API 上传，都应在 Portal 查看 Validation Results。全部通过并确认坐标、版本、依赖正确后再点击 `Publish`；发现问题则点击 `Drop`。

这里使用 `publishReleasePublicationToMavenLocal` 是因为 Gradle 会为每个 `MavenPublication` 自动创建此任务，并把 publication 的 POM 与其他 artifact 按 Maven 布局复制到 Maven Local。[Gradle 8.5：Publishing to Maven Local](https://docs.gradle.org/8.5/userguide/publishing_maven.html#publishing_maven:install) Android 官方也推荐用 Maven Publish Plugin 发布 Android library，并说明本地仓库可用于生成 repository/zip。[Android Developers：Upload your library](https://developer.android.com/build/publish-library/upload-library#local-repo)

`-Dmaven.repo.local` 把 Maven Local 重定向到本次任务的构建目录，避免污染 `~/.m2/repository`。Gradle 官方明确指出 `publishToMavenLocal` 不生成 checksum，因此打包脚本为所有主文件补充 `.md5` 与 `.sha1`。[Gradle 8.5：Maven Publish Plugin](https://docs.gradle.org/8.5/userguide/publishing_maven.html#publishing_maven:complete_example)

脚本只压缩两个精确的版本目录，不压缩 artifact 根目录，因此 Maven Local 生成的 `maven-metadata-local.xml` 不会进入 bundle；Portal 解压后看到的首层路径直接是 `com/`，没有额外的 `repository/` 或 bundle 名称目录。[Sonatype：Maven Repository Layout bundle 示例](https://central.sonatype.org/publish/publish-portal-upload/)

## bundle 结构

以当前 `androidx-9.2.3` 为例，ZIP 解压后应类似：

```text
com/
└── gouqinglin/
    ├── BigImageViewPager/
    │   └── androidx-9.2.3/
    │       ├── BigImageViewPager-androidx-9.2.3.aar
    │       ├── BigImageViewPager-androidx-9.2.3-sources.jar
    │       ├── BigImageViewPager-androidx-9.2.3-javadoc.jar
    │       ├── BigImageViewPager-androidx-9.2.3.pom
    │       └── BigImageViewPager-androidx-9.2.3.module
    └── BigImageViewPager-media3/
        └── androidx-9.2.3/
            ├── BigImageViewPager-media3-androidx-9.2.3.aar
            ├── BigImageViewPager-media3-androidx-9.2.3-sources.jar
            ├── BigImageViewPager-media3-androidx-9.2.3-javadoc.jar
            ├── BigImageViewPager-media3-androidx-9.2.3.pom
            └── BigImageViewPager-media3-androidx-9.2.3.module
```

上面每个主文件旁边还必须有：

```text
<主文件>.asc
<主文件>.md5
<主文件>.sha1
```

`.module` 是当前 Gradle publication 生成的 Gradle Module Metadata；既然它包含在 deployment 中，就和 AAR、POM、sources/Javadoc JAR 一样保留签名与 checksum。Central 要求所有部署文件提供有效的 hex checksum：`.md5`、`.sha1` 必需，`.sha256`、`.sha512` 可选；所有主文件必须有 ASCII-armored detached PGP signature。`.asc` 不需要 checksum，checksum 文件也不需要 `.asc`。[Sonatype：checksum 与签名要求](https://central.sonatype.org/publish/requirements/#provide-file-checksums)；[Sonatype：手动签名示例](https://central.sonatype.org/publish/requirements/gpg/#signing-a-file)

对非 `pom` packaging，Central 还要求 sources 和 Javadoc JAR。当前两个 Android publication 的主 packaging 是 `aar`：`library` 显式配置了 `withSourcesJar()`，`library-video-media3` 当前会生成 `releaseSourcesJar`；两个 module 都通过 Dokka `javadocJar` 生成文档包。本流程已验证两套 publication 均包含 sources 与 Javadoc JAR。[Sonatype：Sources/Javadoc 要求](https://central.sonatype.org/publish/requirements/#supply-javadoc-and-sources)

## Portal 上传与发布

1. 登录 [Central Publisher Portal](https://central.sonatype.com/)。
2. 在 `Namespaces` 中找到已验证的 `com.gouqinglin`，点击 `Publish Component`；也可从右上角 `Publish`，或 `Publishing Settings` → `Deployments` 进入。[Sonatype：网页上传入口](https://central.sonatype.org/publish/publish-portal-upload/)
3. 填写便于识别的 Deployment Name，例如 `com.gouqinglin:BigImageViewPager:androidx-9.2.3`，可选填 Description。
4. 点击 `Upload File`，选择上述 `BigImageViewPager-<version>-central-bundle.zip`，再点击 `Publish Component` 开始上传。同一个 ZIP 可以包含这里的两个 component；每次 publishing request 只能上传一个 archive，最大 1 GB。[Sonatype：bundle 数量和大小限制](https://central.sonatype.org/publish/publish-portal-upload/)
5. 等待 Portal validation，使用 `Refresh` 查看结果。失败项会出现在 deployment 卡片的 `Validation Results`；修复本地 bundle 后需要重新创建并上传新的 deployment。[Sonatype：Component Validation](https://central.sonatype.org/publish/publish-portal-guide/#component-validation)
6. Validation 全部通过后，再人工点击 `Publish` 同步到 Maven Central；如果发现版本或内容不对，点击 `Drop`，不要发布。

## 其他可选发布方式

`upload-central.sh` 将同一个 bundle 上传到 Central Publisher API 的
`POST /api/v1/publisher/upload`，不经过 OSSRH Staging API 或 staging profile 查询。脚本固定
使用 `publishingType=USER_MANAGED`：API 只负责上传和验证，验证通过后仍由用户在 Portal
点击 `Publish`。API 使用 Portal User Token 组成的 Bearer 凭证，并返回后续查询状态所需的 deployment ID。
[Sonatype：Publisher API 上传 bundle](https://central.sonatype.org/publish/publish-portal-api/#uploading-a-deployment-bundle)

若希望继续由 Gradle 一条命令完成，也可以评估 JReleaser、GradleUp `nmcp` 或 Vanniktech
等社区插件。但 Sonatype 当前明确说明尚无官方 Central Portal Gradle 插件，列出的社区插件
不由 Sonatype 支持；对于本仓库，优先建议保留现有 publication/signing 配置，只把上述
bundle 生成与 Publisher API 上传封装成独立任务或脚本，迁移范围更小、失败边界也更清晰。
[Sonatype：Gradle Portal 发布与社区插件](https://central.sonatype.org/publish/publish-portal-gradle/)

## 限制与注意事项

- 手动上传解决的是 OSSRH Staging API 的 profile discovery 问题，不会修复 Portal 账号与 `com.gouqinglin` namespace 的归属问题。如果 Portal 中看不到已验证 namespace，仍需先解决账号/namespace 权限。
- 不要手工把整个 Maven Local 目录打包；其中的 `maven-metadata-local.xml` 不属于本次两个 GAV 版本目录。优先使用 `package-central.sh`。
- 两个 artifact 应在同一个 ZIP 中以同一个 `VERSION_NAME` 上传，避免 `BigImageViewPager-media3` 的 POM 指向尚未发布的核心版本。
- 发布前检查 ZIP 中没有 secret key、Gradle 配置、环境变量或其他仓库文件。bundle 只应包含 `com/gouqinglin/.../<version>/` 下的 publication 文件和 sidecar。
- Portal validation 通过不等于已经公开发布；最后点击 `Publish` 才会同步到 Central。
- `upload-central.sh` 会产生远端 deployment，但不会执行最终发布；不要把 `--yes` 用在未经审核的本地 bundle 上。
- 一旦发布，Central 上的组件不能修改、覆盖或删除。发现问题时必须使用新版本号重新发布。[Sonatype：发布后不可变](https://central.sonatype.org/publish/publish-portal-guide/#component-validation)
- `fcntl(): Bad file descriptor` 和 sources JAR duplicate-path warning 与 staging profile 查询无关；若本地 publication 任务最终为 `BUILD SUCCESSFUL`，它们不会阻止本流程生成 bundle，但仍应单独处理重复 sources 的构建告警。

## 官方资料

- [Sonatype Central：Publishing By Uploading a Bundle](https://central.sonatype.org/publish/publish-portal-upload/)
- [Sonatype Central：Publishing Requirements](https://central.sonatype.org/publish/requirements/)
- [Sonatype Central：Working with PGP Signatures](https://central.sonatype.org/publish/requirements/gpg/)
- [Sonatype Central：Central Publisher Portal Guide](https://central.sonatype.org/publish/publish-portal-guide/)
- [Sonatype Central：Publisher API](https://central.sonatype.org/publish/publish-portal-api/)
- [Sonatype Central：Gradle Portal publishing](https://central.sonatype.org/publish/publish-portal-gradle/)
- [Gradle 8.5：Maven Publish Plugin](https://docs.gradle.org/8.5/userguide/publishing_maven.html)
- [Gradle 8.5：Signing Plugin](https://docs.gradle.org/8.5/userguide/signing_plugin.html)
- [Android Developers：Upload your library](https://developer.android.com/build/publish-library/upload-library)
