# RIDI Paper Launcher

适用于 RIDI PAPER 电纸书（Android 4.4.2）的桌面系统，基于 [E-Ink Launcher](https://github.com/Modificator/E-Ink-Launcher) 改造。

项目包含四个应用：

- **桌面**：五列应用列表和常用快捷操作。
- **底部栏**：返回、主页、刷新、背光、Wi-Fi 和电量显示。
- **设置**：背光、字体、锁屏背景、左侧实体按键等设备设置。
- **日历**：农历、节假日、天气和待办事项，支持横竖屏。

## 快速构建

使用 Codex、Copilot 等 Agent 时，让它使用 [build-ridi-paper SKILL](.github/skills/build-ridi-paper/SKILL.md) 构建项目即可。

手动构建需要 **JDK 17** 和 **Android SDK**（Platform 19、Platform 36、Build Tools 36.1.0）。设置 `JAVA_HOME` 和 `ANDROID_HOME` 后，在仓库根目录运行：

```sh
./gradlew :app:assembleDebug
sh companion/bottom-bar/build.sh
sh companion/ridi-settings/build.sh
sh companion/ridi-calendar/build.sh
```

生成的 APK 位于 `app/build/outputs/apk/debug/` 和各辅助应用的 `build/` 目录。辅助应用默认使用本机的 `~/.android/debug.keystore` 签名；升级已安装的应用时，须使用原来的签名密钥。

日历使用方法与测试命令见 [日历说明](companion/ridi-calendar/README.md)。设备上的桌面图标可用 `./scripts/deploy-desktop-icons.sh DEVICE_SERIAL` 同步。

## 使用提示

本项目针对已适配的 RIDI PAPER。底部栏需要悬浮窗和无障碍权限；部分设置需要 root 权限。其他 Android 设备可能需要调整。

左侧实体按键可在“设置 → 左侧实体按键设置”中选择返回、返回主页、背光开关、Wi-Fi 开关或刷新屏幕。

原项目采用 Apache-2.0 许可证；本项目保留了上游历史。新增图标的使用权请以图标权利人的授权为准。
