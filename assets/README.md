# 图标资源

`icon-masters/` 保留当前系统所用图标的设计母版：`01`–`15` 为底部导航栏，`16`–`19`、`22`–`23` 为桌面应用，`24`–`25` 为桌面的重启与清理入口。未使用的日历和时钟图标已从项目中排除。

`desktop-icons/` 为桌面实际读取的文件，文件名必须与应用包名一致。它们由 `icon-masters/` 中对应图片复制并重命名而来。导航栏图标打包在 `companion/bottom-bar/res/drawable-nodpi/`，重启和清理图标打包在 `app/src/main/res/drawable-nodpi/`。

这些图标来自本机项目设计资料，不属于上游 E-Ink Launcher。公开发布前，应由图标权利人明确其使用许可。
