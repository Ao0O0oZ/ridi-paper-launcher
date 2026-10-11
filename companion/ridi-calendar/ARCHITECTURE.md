# 架构

- `CalendarEngine`：以设备时区获取公历日期，调用 `lunar-java` 转换农历、识别传统节日与节气。
- `HolidaySchedule`：解析按年度维护的官方休假表；不从周末推断法定安排。
- `Weather`：优先 Open-Meteo 城市解析与预报。Android 4.4 的旧 TLS 套件无法连接时，使用内置中国城市坐标和 MET Norway 的实时预报；统一数据模型、天气代码映射和三小时 SharedPreferences 缓存。请求在工作线程执行。Open-Meteo HTTPS 使用 TLS 1.2 和随包附带的 ISRG Root X1 证书，保留主机名校验。
- `TodoStore`：SQLiteOpenHelper 本地持久化，按日期查询，支持新增、更新、删除和完成状态。
- `CalendarScreen`：单个 Canvas View 按横竖屏分别排版，使用设备自带 serif 字体、内置 QWeather 图标字体和黑白灰线条复现预览图。每次月份或待办变化时预计算日期格信息；天气和复选框变化只重绘对应区域。无动画与后台轮询。
- `MainActivity`：页面状态、交互弹窗、生命周期、跨午夜刷新和设置。屏幕方向提供自动、竖屏、横屏三档，通过 Activity 方向请求生效并保存在 SharedPreferences；配置变更时保留所选日期和月份。

Android 4.4.2 不适合当前 Jetpack Compose 依赖链，因此使用 Java 7、API 19 原生 View。没有绑定厂商墨水屏私有接口；设备会使用标准 Android 局部无效化回退。
