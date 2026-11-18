# 版本发布 Runbook

本文记录从版本准备、Maven Central 发布到 GitHub Release 和 issue 收尾的完整流程。
`doc/MANUAL_CENTRAL_UPLOAD.md` 保留 bundle 结构和 Central Portal 的详细说明；日常发版优先使用本文和 `./release.sh`。

## 一次性准备

### 1. 构建环境

- JDK 17、Android SDK、NDK `25.2.9519653`
- `gpg`、`zip`、`unzip`、`md5`、`shasum`、`jq`、`curl`、`ripgrep` (`rg`)
- GitHub CLI 已登录：`gh auth status`

### 2. 发布凭据

凭据放在仓库外的本地文件，默认路径：

```text
~/.config/BigImageViewPager/release.env
```

文件权限必须是 `600`，内容只包含变量名和值，不要提交到仓库：

```bash
SIGNING_KEY_ID=<GPG key id>
SIGNING_PASSWORD=<GPG passphrase>
SIGNING_SECRET_KEY_RING_FILE=/Users/<user>/.gnupg/secring.gpg
CENTRAL_USERNAME=<Central Portal user token username>
CENTRAL_PASSWORD=<Central Portal user token password>

# 兼容旧账号，upload-central.sh 会在 CENTRAL_* 缺失时读取这两个变量
OSSRH_USERNAME=<optional fallback>
OSSRH_PASSWORD=<optional fallback>
```

约束：

- `SIGNING_SECRET_KEY_RING_FILE` 必须指向可读的 secret key ring。
- 对应的 GPG 公钥必须已上传到 Central 支持的 keyserver。
- `release.sh`、`publish-central.sh` 会自动读取该文件；也可以用 `RELEASE_ENV_FILE` 指定其他路径。
- 不要使用 `git add -f` 添加凭据文件；仓库 `.gitignore` 已包含 `/release.env`。

## 日常发布

### 1. 更新版本和变更记录

同时更新以下文件：

| 文件 | 位置 |
| --- | --- |
| `gradle.properties` | `VERSION_NAME` |
| `README.md` | 当前版本、两条依赖示例、更新日志顶部 |
| `doc/MANUAL_CENTRAL_UPLOAD.md` | 示例中的旧版本号 |
| `gradle/libs.versions.toml` | `bigimageviewpager` 版本别名 |

版本使用 `androidx-<major>.<minor>.<patch>`，例如 `androidx-9.2.4`。release 版本不能以 `-SNAPSHOT` 结尾，发布后不能覆盖或删除 Central 上的同名版本。

先完成代码验证：

```bash
./gradlew :library:assembleRelease :library-video-media3:assembleRelease :sample:assembleDebug test lint
git diff --check
```

验证通过后提交版本变更：

```bash
git add gradle.properties README.md doc/MANUAL_CENTRAL_UPLOAD.md gradle/libs.versions.toml
git commit -m "release: androidx-9.2.5"
```

`./release.sh` 要求工作区干净、当前分支为 `master`，且版本已经提交。

### 2. 一键发布 Maven Central

```bash
./release.sh --yes
```

脚本按顺序执行：

1. 校验分支、工作区、版本号、README 版本和 tag 是否重复。
2. 构建核心库、Media3 插件和 sample，运行 `test`、`lint`。
3. 调用 `./package-central.sh` 生成并验证 Central bundle。
4. 调用 `./upload-central.sh` 上传为 `USER_MANAGED` deployment。
5. 等待 `VALIDATED`，要求 validation errors 为 0。
6. 调用 Central Publisher API 执行最终发布，等待 `PUBLISHED`。
7. 调用 `./publish-central.sh verify <version>` 校验公开仓库的 POM/AAR/`.asc` 和 AAR SHA-1。

不加 `--yes` 时，脚本会在上传前询问一次；`--skip-checks` 只用于已经单独完成构建和测试的场景。

### 3. GitHub tag 和 Release

Maven 验证通过后再创建 tag，避免 tag 指向未经发布的版本：

```bash
VERSION=androidx-9.2.5
git tag -a "$VERSION" -m "$VERSION: <release summary>"
git push origin master "$VERSION"
```

准备 release notes 后创建 GitHub Release：

```bash
gh release create "$VERSION" \
  --title "$VERSION" \
  --notes-file .agent-temp/release-notes.md \
  "build/central-publishing/$VERSION/repository/com/gouqinglin/BigImageViewPager/$VERSION/BigImageViewPager-$VERSION.aar" \
  "build/central-publishing/$VERSION/repository/com/gouqinglin/BigImageViewPager-media3/$VERSION/BigImageViewPager-media3-$VERSION.aar"
```

release notes 至少包含：

- 关联 issue 和修复内容
- 两个 Maven 坐标
- 测试设备/系统版本和测试项数量
- 构建、lint、公开仓库校验结果

### 4. 更新 issue

只有 Maven Central 验证通过、tag 和 release 都完成后才关闭 issue。先评论再关闭：

```bash
gh issue comment <number> --repo SherlockGougou/BigImageViewPager --body-file issue-reply.md
gh issue close <number> --repo SherlockGougou/BigImageViewPager --reason completed
```

评论中写明：

- 修复版本和 Maven 坐标
- 修复点
- 验证设备和测试结果
- Release 链接

## 手动分步发布

需要排查或只重跑某一步时使用以下命令。

### 生成 bundle

```bash
set -a
. ~/.config/BigImageViewPager/release.env
set +a
./package-central.sh
```

输出：

```text
build/central-publishing/BigImageViewPager-<VERSION>-central-bundle.zip
```

### 上传 deployment

```bash
./upload-central.sh --yes build/central-publishing/BigImageViewPager-<VERSION>-central-bundle.zip
```

记住输出的 `Deployment ID`。

### 查询状态

```bash
./publish-central.sh status <deployment-id>
```

状态含义：

- `VALIDATING`：Central 正在校验
- `VALIDATED`：可以最终发布
- `PUBLISHING`：正在同步到公开仓库
- `PUBLISHED`：已完成
- `FAILED` / `DROPPED`：失败，查看 `errors`

### 最终发布

```bash
./publish-central.sh publish <deployment-id>
```

脚本会等待 `VALIDATED`，确认 errors 为 0，再调用：

```text
POST https://central.sonatype.com/api/v1/publisher/deployment/<deployment-id>
```

并轮询到 `PUBLISHED`。发布不可撤销；确认版本和 bundle 后再执行。

### 校验公开仓库

```bash
./publish-central.sh verify <version>
```

脚本检查两个 artifact 的 POM、AAR、`.asc`、`.sha1` 均为 HTTP 200，比较公开 AAR 与本地 bundle 的 SHA-1，并校验 POM PGP 签名。

Central 状态接口在成功发布后可能仍显示：

```text
"errors": { "common": ["Deployment components info not found"] }
```

这是 Central 状态缓存/索引的已知表现，不等于发布失败；以公开仓库文件、SHA-1 和 PGP 校验为准。`publish-central.sh publish` 遇到 `PUBLISHED` 会直接返回成功。

## 2026-11-18 androidx-9.2.4 发布记录

| 项目 | 值 |
| --- | --- |
| Git commit | `4a1d02c` |
| Tag / GitHub Release | `androidx-9.2.4` |
| Central deployment ID | `d00c1667-80a2-470e-b9ac-20b61f544527` |
| Central 状态 | `PUBLISHED` |
| 核心库 SHA-1 | `c029fb98b24e7b09fd6ad604d68fa19169ff28f6` |
| Media3 SHA-1 | `07f1c00470a1c0da014632caa45bf51a4dd90f72` |
| 测试设备 | 华为 FIN-AL60a / Android 12，无线调试 `192.168.50.34:5555` |
| 测试结果 | 16 项 instrumentation 测试通过 |

公开坐标：

```gradle
implementation "com.gouqinglin:BigImageViewPager:androidx-9.2.4"
implementation "com.gouqinglin:BigImageViewPager-media3:androidx-9.2.4"
```

## 安全检查清单

- 发布前确认 `VERSION_NAME`、README 依赖示例、Central 文档示例一致。
- 发布前确认工作区干净、tag 不存在、当前分支为 `master`。
- 发布前确认本轮测试覆盖了本次改动和关键边界。
- 上传后等待 validation 通过再执行最终 publish。
- 发布后必须用公开仓库 URL 校验，不只依赖 Portal 状态。
- 不把 `release.env`、GPG 私钥、token、密码或签名文件提交到仓库。
- Central release 不可变；发现问题只能发布新版本，不能覆盖旧版本。
