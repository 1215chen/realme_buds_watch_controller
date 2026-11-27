# 产品规格（Spec）

## 1. 定位

一款 **Wear OS 4+ 手表端独立应用**（standalone，不依赖手机 App），通过蓝牙直连 realme / OPPO 系 TWS 耳机，在手表上完成常用的耳机控制。

非目标：不替代手机端 realme Link 的账号体系、云同步、固件 OTA、AI 助手、空间音效等高级功能。

## 2. 技术基线

| 项 | 值 |
|---|---|
| 语言 / UI | Kotlin 2.1.20 + Jetpack Compose for Wear OS Material 3 (1.6.2) |
| minSdk / targetSdk / compileSdk | 26 / 35 / 36（兼容 ColorOS Watch 与 Wear OS 3+） |
| 架构 | 单 Activity + Compose Navigation + ViewModel + Repository（StateFlow） |
| BLE | BluetoothLeScanner + GATT（私有通道自动发现）+ RFCOMM/SPP 兜底 |
| 输入 | 旋转表冠（RotaryScrollEvent / ACTION_SCROLL / DPAD 按键三种兼容路径） |
| 触觉 | Wear OS 4+ 用系统 HapticFeedback；ColorOS/旧系统用 Vibrator 兜底 |
| 主题 | Material Design 3 蓝色系 ColorScheme（深色基线，无品牌定制色） |

## 3. 页面与交互

| 页面 | 内容 |
|---|---|
| 主页 | 设备卡（设备名、连接状态、L/R/充电盒三个电量圆环）、降噪快捷分段切换、EQ/游戏模式/按键设置/设备信息、「更多」分组（双设备/断开/设置）；未连接时显示「搜索并连接」 |
| 降噪控制 | 关闭 / 通透 / 降噪；支持等级的型号自动显示等级选项 |
| 音效 | 预设单选（按耳机支持能力生效）+ 频响示意 |
| 触控手势 | 左耳 / 右耳两组，各含双击 / 三击 / 长按，弹窗选择动作 |
| 设备信息 | 型号、固件版本、蓝牙地址；按能力显示双设备/入耳检测开关；恢复出厂设置（二次确认） |
| 扫描连接 | 权限申请、扫描中、结果列表、连接中、失败提示 |
| 设置/关于 | 应用图标、应用版本、**手表型号**、**耳机型号**、演示模式开关、免责声明 |

通用交互：圆形表盘适配、表冠滚动、右滑返回、底部浮层提示（不依赖 Toast）。

## 4. 数据与接口

- `BudsState`：连接状态、设备信息、电量、ANC、EQ、左右耳手势、开关项、设备能力。
- `BudsRepository`：扫描 / 连接 / 控制命令 + StateFlow；两个实现：
  - `DemoBudsRepository`：无耳机时的演示数据；
  - `BleBudsRepository`：真实 BLE 实现，能力项由耳机协议上报动态决定（不做型号硬编码）。
- 自动回连：持久化上次成功连接的设备，应用启动自动连接；掉线后退避重连。

## 5. 兼容性策略

- 不维护「型号白名单」，按协议族通用实现；
- 运行时自动探测帧方言（Telink 5B 大端 / OPO 9B 小端）；
- UI 能力项（入耳检测/双设备/降噪等级）由设备上报的状态决定，不支持则不显示；
- 协议细节见 [PROTOCOL.md](PROTOCOL.md)。

## 6. 验收标准

1. `./gradlew :app:assembleDebug` 构建通过；
2. Wear OS 4+ 设备/模拟器可启动，圆形表盘无内容截断；
3. 扫描 → 连接 → 控制 → 断开主流程可用；
4. 演示模式可完整展示全部页面；
5. 真机验证（realme Buds T200，2026-11-13）：电量/固件读取、降噪三档、音效切换、游戏模式、双设备连接、左右耳手势实际生效。
