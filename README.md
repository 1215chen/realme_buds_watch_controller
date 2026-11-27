# 耳机控制器（Wear OS）· Buds Wear Controller

> 在 **Wear OS / ColorOS Watch 手表**上直接控制 realme / OPPO 系 TWS 耳机的开源应用：降噪、音效、触控手势、游戏模式、双设备连接、电量与设备信息。
>
> **本项目是非官方开源项目，与 realme、OPPO、OnePlus 及其关联公司没有任何关系**；「realme」「OPPO」等商标归其各自所有者。协议信息来自对客户端行为的互操作研究，仅供个人学习与设备互操作使用。

![平台](https://img.shields.io/badge/Wear%20OS-4%2B-3DDC84) ![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-7F52FF) ![Compose](https://img.shields.io/badge/Compose%20for%20Wear%20OS-Material%203-4285F4) ![minSdk](https://img.shields.io/badge/minSdk-33-blue) ![Version](https://img.shields.io/badge/version-1.1.1-4285F4) ![License](https://img.shields.io/badge/License-MIT-green)

| 主页（已连接） | 设备卡 · 电量环 | 降噪切换 |
|---|---|---|
| ![主页](docs/screenshots/ui-home.png) | ![设备卡](docs/screenshots/ui-home-card.png) | ![降噪切换](docs/screenshots/ui-anc.png) |

| 功能行 | 设置 / 关于 | 启动图标 |
|---|---|---|
| ![功能行](docs/screenshots/ui-home-rows.png) | ![设置](docs/screenshots/ui-about.png) | ![图标](docs/screenshots/app-icon-512.png) |

> 以上为真机截图（Wear OS 手表 + realme Buds T200），界面配色为 **Material Design 3 蓝色系**。

## 功能

- **主页设备卡**：设备名、连接状态、L / R / 充电盒三个电量圆环（未上报显示 `-`）
- **降噪控制**：主页快捷分段切换（关闭 / 通透 / 降噪），选中块平滑滑动；支持等级的型号自动提供智能/深度/中等/轻度
- **音效**：纯净人声 / 纯粹原音 / 纯净低音 / 浑厚低音 / 明亮 / 动态（按耳机实际上报的能力生效）
- **触控手势**：左耳、右耳可分别配置双击 / 三击 / 长按
- **游戏模式**：低延迟开关
- **双设备连接**：耳机支持时自动显示开关
- **设备信息**：型号、固件版本、蓝牙地址
- **自动回连**：记住上次设备，打开应用自动连接；掉线后按退避策略重连
- **演示模式**：没有耳机也能完整体验界面
- **平台适配**：Wear OS 4/5 已验证；OPPO Watch（ColorOS Watch）自 1.1.1 起支持，OPPO Watch X2 已真机实测可用（表冠滚动 + 震动兜底）

## 支持的耳机

本项目不是按型号白名单硬编码，而是按 **realme/OPPO 系私有协议**通用适配：

- ✅ **已真机验证**：`realme Buds T200`（降噪/音效/游戏模式/双设备/左右耳手势全部实际生效）
- ✅ **已真机验证（OPPO 组合）**：**OPPO Watch X2 + OPPO Enco Air 5 Pro** 实测可用（安装、连接与基础控制正常）
- ✅ **平台兼容**：Wear OS 4/5 真机验证；OPPO Watch（ColorOS Watch）自 1.1.1 起可安装（minSdk 26），OPPO Watch X2 已实测，表冠/震动做了兼容适配
- 🟡 **理论支持**：采用同一协议族的 realme TWS（Buds T 系列、Buds Air 系列、Buds Q / Air Neo 等）与部分 OPPO/OnePlus Enco 系列；是否全部可用取决于该型号的命令支持子集，欢迎反馈实测结果
- ❌ **不支持**：非 realme/OPPO 品牌耳机（只能做标准 BLE 电量/信息读取），以及协议完全不同的头戴/颈挂型号

协议细节与实测命令表见 [docs/PROTOCOL.md](docs/PROTOCOL.md)。

## 工作原理（简述）

1. BLE 扫描并识别耳机 → 建立 GATT 连接（部分型号走经典蓝牙 RFCOMM 控制通道）；
2. 运行时自动探测帧方言：Telink 5 字节大端帧 / OPO 9 字节小端帧；
3. 通过统一命令集完成状态查询与控制，耳机上报的能力决定 UI 显示哪些选项；
4. 应用启动时自动回连上次成功连接的设备。

## 安装

1. 从 [Releases](../../releases) 下载签名 APK（当前版本 `buds-wear-controller-1.1.1.apk`）；
2. 手表打开开发者选项与无线调试（Wear OS：设置 → 系统 → 开发者选项 → 无线调试）；
3. 安装：

```bash
adb install -r buds-wear-controller-1.1.1.apk
adb shell am start -n com.cyhao.realmebuds/com.cyhao.realmebuds.MainActivity
```

> 首次打开默认为**真实设备模式**：在主页点击「搜索并连接」扫描耳机；没有耳机时可在「设置」页手动开启「演示模式」体验全部界面。

## 构建

**环境要求**：JDK 17、Android SDK 36（compileSdk 36 / targetSdk 35 / minSdk 26）、Gradle 8.14.x（已在仓库内提供 `gradlew`）。

```bash
# macOS / Linux
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
./gradlew :app:assembleDebug

# Windows
set JAVA_HOME=C:\path\to\jdk17
set ANDROID_HOME=C:\path\to\android-sdk
gradlew.bat :app:assembleDebug
```

也可以直接用 Android Studio 打开项目（首次同步需联网下载依赖）。

> 注意：Wear Compose Material3 固定使用 **1.6.2**（1.7.x 需要 compileSdk 37 + AGP 9.1+，本仓库矩阵不兼容）；release 构建产物未签名，自行签名后分发。

## 项目结构

```
app/                      # 应用模块（Kotlin + Jetpack Compose for Wear OS）
  src/main/java/com/cyhao/realmebuds/
    data/                 # 状态模型、Repository、ViewModel（演示/真实实现）
    ble/                  # BLE/RFCOMM 传输、realme 协议编解码、命令表
    ui/screens/           # 7 个 Wear M3 页面
    ui/components/        # 通用组件（电量圆环、列表行、开关行）
    ui/theme/             # 主题（Material Design 3 蓝色系）
docs/                     # 设计、安装、协议、校准、AI 使用说明
gradlew / gradlew.bat     # Gradle Wrapper
```

## 关于 AI 生成（重要说明）

- 本项目**大量代码、文档与设计稿由 AI 编码智能体（DeepSeek Harness）在人工需求指导下生成**；
- 人工负责：需求定义、真机联调、协议抓包校准、功能验证、视觉设计取舍与最终审查；
- 因此代码风格/结构可能带有 AI 生成痕迹，欢迎通过 Issue / PR 改进；
- 逆向分析得到的协议实现**不保证适用于所有型号**，仅供学习与互操作研究，请遵守当地法律与耳机厂商的条款；
- 详见 [docs/AI_USAGE.md](docs/AI_USAGE.md)。

## 已知限制

- 部分型号的降噪等级子档、手势「切换降噪」动作、恢复出厂设置尚未下发（命令未确认）；
- 「查找耳机」仅硬件支持该功能的型号有效（例如 T200 会接受命令但不会响）；
- 应用被杀掉后无后台常驻服务，重新打开会自动回连但无法在关闭时保持连接；
- 固件 OTA、空间音效、高清音质（LDAC）等高级功能尚未实现；
- Wear OS 低电量省电模式可能冻结后台，影响回连稳定性。

## 贡献

欢迎提交 Issue / PR：型号兼容性反馈（型号 + 可用的功能）、协议抓包、UI 改进、文档修正都很有价值。提交前请先看 [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md)。

## License

[MIT](LICENSE)（仅覆盖本项目自有代码与文档；realme/OPPO 商标及耳机固件/协议归其所有者，本项目与官方无关。）
