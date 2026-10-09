# RIDI Paper Launcher

This project adapts [Modificator/E-Ink-Launcher](https://github.com/Modificator/E-Ink-Launcher), branch `v0.x`, for a RIDI PAPER (Android 4.4.2). The upstream Git history and Apache-2.0 license are retained. The `upstream` Git remote points to the original repository; no GitHub fork or release has been created yet.

本仓库保存当前电纸书桌面系统的**源码**，不包含设备固件、个人壁纸、字体文件、签名私钥或历史 APK。它由三个部分组成：

| 目录 | 内容 |
| --- | --- |
| `app/` | 五列桌面、应用筛选与命名、重启和清除后台入口 |
| `companion/bottom-bar/` | 底部导航栏、背光、Wi-Fi、日期和电量显示 |
| `companion/ridi-settings/` | Wi-Fi、蓝牙、背光、导航栏、输入法及字体设置 |
| `assets/desktop-icons/` | 当前桌面使用的外部应用图标，按包名命名 |

## 构建

需要 JDK 17、Android SDK Platform 19，以及仓库 Gradle 配置对应的 Build Tools。先设置 `JAVA_HOME` 和 `ANDROID_HOME`：

```sh
./gradlew :app:assembleDebug
```

桌面 APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。两个辅助应用使用 Android SDK Build Tools `36.1.0`；可用 `RIDI_BUILD_TOOLS_VERSION` 指定其他已安装版本：

```sh
RIDI_SIGNING_KEYSTORE=/path/to/private.keystore RIDI_SIGNING_ALIAS=your_alias ./companion/bottom-bar/build.sh
RIDI_SIGNING_KEYSTORE=/path/to/private.keystore RIDI_SIGNING_ALIAS=your_alias ./companion/ridi-settings/build.sh
```

辅助应用 APK 分别输出到各自的 `build/` 目录。脚本默认查找 `~/.android/debug.keystore`，不会把签名文件放进仓库。**升级已安装的应用必须使用与设备上现有 APK 相同的签名密钥**；新密钥签出的 APK 不能直接覆盖安装。

## 图标与设备

外部桌面图标由桌面从 `/sdcard/Documents/E-Ink Launcher/icon/` 读取。设备已通过 ADB 连接后，可运行：

```sh
./scripts/deploy-desktop-icons.sh DEVICE_SERIAL
```

`DEVICE_SERIAL` 可以是 `adb devices` 显示的 USB 序列号或 `IP:5555`。项目不保存 Wi-Fi 地址、无线 ADB 凭据或设备数据。

此分支针对一台已取得 root 权限并完成系统适配的 RIDI PAPER。底部导航栏需要悬浮窗和无障碍权限；字体切换依赖设备上的指定字体与 `su`。直接安装到未改造的 Android 设备，无法保证所有功能可用。

## 上游与发布

本地分支为 `ridi-paper`，上游分支为 `v0.x`。需要发布时，可在 GitHub 上 Fork 上游仓库，把该 Fork 添加为 `origin`，再推送 `ridi-paper` 分支。发布前应确认自制图标的许可、签名策略和设备专用功能说明。
