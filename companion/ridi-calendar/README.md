# RIDI 日历

面向 Android 4.4.2 / API 19 电纸书的原生日历。竖屏显示今日、天气、月历和待办；横屏改为左侧今日与待办、右侧天气与月历。相邻月份日期显示为灰色。

## 构建和安装

需要 Android SDK Platform 19、Build Tools 36.0.0、JDK 17，以及 Android 调试签名文件。仓库已包含 `lunar-java 1.7.7` jar。

```sh
ANDROID_HOME=/path/to/android-sdk JAVA_HOME=/path/to/jdk17 sh companion/ridi-calendar/build.sh
adb install -r companion/ridi-calendar/build/RidiCalendar.apk
```

本机产物：`companion/ridi-calendar/build/RidiCalendar.apk`。

## 使用

- 点击月历左右箭头切换月份；点击标题或今日大数字回到今天。
- 点击日期查看公历、农历、节日及当天待办，并可新增任务。
- 待办标题右侧的 `＋` 新增任务；点击方框切换完成；点击文字编辑或删除。日期输入格式为 `yyyy-MM-dd`，可选时间为 `HH:mm`。
- 点击天气区域的定位图标进入设置。点按“屏幕方向”可在“自动旋转 → 竖屏 → 横屏”之间切换，点击“保存”生效；所选方向会在重新打开应用后保留。
- 输入城市（如“北京”或 `Beijing`）后点击“保存”，城市改变时会刷新天气；也可点“刷新天气”。默认城市为北京，但没有网络数据时会明确显示“天气暂无数据”。
- 可在设置中切换华氏度或启用基于设备近期网络定位的自动定位。定位无结果时关闭自动定位并手动输入城市。
- 天气成功请求后缓存三小时。离线时日历和待办仍可使用，天气保留最后一次有效数据、来源及更新时间。

## 数据来源与范围

农历与二十四节气使用 [lunar-java 1.7.7](https://github.com/6tail/lunar-java)（MIT，许可证随 `libs/` 提供）。天气优先使用 [Open-Meteo](https://open-meteo.com/en/docs)；Android 4.4 无法与其服务器协商加密套件时，自动回退到 [MET Norway Locationforecast](https://api.met.no/weatherapi/locationforecast/2.0/documentation) 的真实预报。旧系统在城市搜索不可用时支持 `ChineseCities.java` 中列出的主要城市。联网只用于天气。

天气图标来自 [QWeather Icons](https://github.com/qwd/Icons)，按 CC BY 4.0 授权使用；图标字体已打包，离线也能显示。

`assets/holidays.csv` 当前包含 2026 年国务院办公厅公布的休假与调休安排。月历用“休”标记放假日、“工”标记调休工作日（设备当前字体缺少“班”字）；没有权威年度配置的日期不显示标记。后续年度可按同一格式添加数据并重新构建。

## 测试

```sh
JAVA_HOME=/path/to/jdk17 sh companion/ridi-calendar/tests/run.sh
```

本地测试覆盖闰年、跨月跨年、农历初一、闰月、春节、中秋、清明、冬至。
