<div align="center">

# **SPW Collage Wall**

#### 为SPW曲库提供拼贴封面墙的插件

</div>

---

## 用法:

1. 在 SPW 设置 → 创意工坊 → 模组管理 → 右上角导入模组 → 选择 `SPW-CollageWall.spmod` 发布包（也可按下方步骤自行构建）
2. 设置 → 创意工坊 → 模组管理 启用 "SPW Collage Wall"
3. 修改配置（可选）
4. 在页面标题栏点击打开

##

## 可配置项:

- `scroll.vertical_only`（仅纵向滑动，默认关闭）：关闭时墙面可上下左右自由平移；开启后只保留纵向滚动，并改为单列墙面
- `card.corner_radius`（卡片圆角，默认 0）：0 为直角，最大 32；数值越大封面四角越圆

配置在 设置 → 创意工坊 → 模组管理 → SPW Collage Wall → 右键修改，保存后立即生效，无需重启。

注意：插件通过解析宿主内部结构替换页面，目前仅在 SPW 1.19.3 上验证，宿主升级后可能失效。首次安装若按钮无效或墙面没有出现，重启一次 SPW。曲库曲目较少时，墙面会重复同一批封面；插件数据目录下的 `spw-collage.log` 可用于排查问题。

## 构建:

需要 JDK 25。在 `spw-collage-wall/` 下执行 `./gradlew build plugin`（首次构建需联网拉取依赖，之后可加 `--offline`），产物为 `spw-collage-wall/build/libs/plugin-dev.spw.collage-1.0.0.spmod`。

## 其他:

- [SPW Workshop API 开发文档](https://saltplayer.com/workshop/)
- 模组图标：[SVG 源文件](assets/CollageWall.svg) / [透明 PNG](assets/CollageWall.png)（可用于创意工坊封面）
