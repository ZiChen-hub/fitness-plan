# MyGymPlane · 个人健身增肌计划

> 专为 183cm / 63kg 偏瘦新手定制的科学增肌系统。
> **原生 Android App（Kotlin + Jetpack Compose）+ PWA 网页版** 双端实现，苹果风 UI，数据全部本地存储。

当前版本：**v6.2.2**（versionCode 11） · APK 下载见 [Releases](../../releases)

---

## ✨ 功能总览

### 🏋️ 训练系统（核心）

- **推拉分化 4 练/周**：推 A / 拉 A / 推 B / 拉 B，附全年三阶段进阶规划
- **23 个动作库**：每个动作含要领、常见错误、目标肌群与 B 站教学视频
- **组级训练流程**：
  - 每天「开始训练」→ 训练子页含动作要领 + 视频 + 每组独立「开始」按钮
  - 组计时**每秒跳动**，记录重量 / 次数 / 用时
  - 组进行中锁定视频与退出（防止分心），组间休息可看视频
  - 支持减重重做本组、临时调整重量
  - **全部组数完成后才能保存退出**，放弃则不保存
- **智能重量推荐**：基于上次表现自动递增
- **练前热身清单**（通用 + 部位专项）与 **练后拉伸清单**（可勾选）
- **屏幕常亮**：训练中自动保持
- **中断恢复**：中途退出 App，下次进入询问「继续上次 / 重新开始」
- **今日总计时**：实际训练时长 vs 休息浪费时长，生成针对性建议

### 🧰 工具箱（8 个工具）

| 工具 | 说明 |
|---|---|
| 🏋️ 杠铃片计算器 | 输入目标重量，算出每侧挂片组合（含 20kg 奥杆） |
| 💯 1RM 计算器 | Epley 公式 + 各强度区间对照 |
| 🔥 TDEE 计算器 | Mifflin-St Jeor 公式，按体重（自动取最近晨重）与年龄计算 |
| ⏱️ 间隔计时器 | Tabata / HIIT / 自定义，环形倒计时 + 暂停/停止 + 提示音震动 |
| 📈 动作进步曲线 | 自动选中最近动作出图，SVG 渐变折线，支持切换动作 |
| 📅 训练日历 | 月视图打卡（绿色 = 已训练） |
| 📤 导出数据 | 全部训练记录导出 CSV，分享到微信/QQ |
| ⚙️ 设置 | 组间休息（复合/孤立分开）、震动/提示音、外观三态、年龄 |

### 📊 统计与数据

- 体重趋势折线图（带增肌目标区间带）
- 训练打卡图、肌肉热力图（近 7 天）
- 部位训练量分布、各动作最大重量
- 基于 `logs.json` 的数据分析：组用时效率、动作进展、训练频率、**减载提醒**、体重趋势

### 💧 科学饮水

目标 = 体重(kg) × 35ml + 训练日 500ml，向上取整到 250ml。
按 63kg 体重：非训练日约 2250ml，训练日约 2750ml。

### 🌗 主题与体验

- 苹果风设计系统：iOS 官方色值、毛玻璃质感、SF 字体栈
- **深色模式三态**：跟随系统 / 浅色 / 深色，切换即时生效
- 自绘线性 Tab 图标（Canvas）
- 提示音（ToneGenerator）与震动（Vibrator）反馈

---

## 📱 安装

1. 到 [Releases](../../releases) 下载 `MyGymPlane_v6.2.2.apk`
2. 手机安装（需允许未知来源）
3. 同签名可直接覆盖升级，数据自动保留

- 最低支持：Android 8.0（minSdk 26）
- 目标版本：Android 15（targetSdk 35）

---

## 🗂️ 项目结构

```
├── index.html / style.css / app.js / data.js   # PWA 网页版（v6.2.2 同步版）
├── manifest.json / sw.js                        # PWA 配置与离线缓存
├── icon-192.png / icon-512.png                  # 应用图标
└── android/                                     # 原生 Android 工程
    ├── build.gradle.kts / settings.gradle.kts / gradle.properties
    └── app/src/main/
        ├── AndroidManifest.xml
        ├── res/                                 # 图标、主题
        └── java/com/zichen/fitnessplan/
            ├── MainActivity.kt   # 入口 + iOS 设计系统（Palette 双主题）+ 各页面 + 训练会话引擎
            ├── Data.kt           # 静态数据：周计划 / 23 动作 / 营养 / 阶段规划
            ├── Store.kt          # 数据层：Gson 持久化、记录、推荐、建议引擎、会话恢复
            └── Tools.kt          # 工具箱 8 件套 + 图表组件 + 设置页 + 提示音/震动
```

---

## 🛠️ 本地构建（Android）

```bash
cd android
# Android Studio 打开，或命令行：
./gradlew assembleDebug          # 调试包
./gradlew assembleRelease        # 需自行配置签名
```

要求：JDK 17、AGP 8.7.3、Kotlin 2.0.21、compileSdk 35。

网页版无需构建，任意静态服务器托管即可（如 `python3 -m http.server`）。

---

## 💾 数据与隐私

- **所有数据仅存本地**：Android 端 `logs.json`（Gson），网页端 `localStorage`
- 不上传、不联网收集任何个人数据（仅视频嵌入 B 站播放器）
- 训练记录可随时通过「工具箱 → 导出数据」备份为 CSV

## 📜 技术栈

- **Android**：Kotlin 2.0.21 · Jetpack Compose（BOM 2024.10）· Material 3 · Gson
- **Web**：原生 HTML/CSS/JS（零依赖）· Service Worker 离线缓存 · SVG 图表
- **设计**：iOS HIG 风格自建设计系统（Palette 浅/深双主题）
