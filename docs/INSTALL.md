# 安装与配对指南 · 耳机控制器

> 适用：**Wear OS 4 及以上**（Android 13 / API 33+）的圆形/方形表盘手表
> 应用名：**耳机控制器**；正式版包名 `com.cyhao.realmebuds`，调试版 `com.cyhao.realmebuds.debug`（debug 构建带 `.debug` 后缀，可与正式版共存）
> 前置：直接使用已签名交付包 `dist/耳机控制器-1.1.1.apk`（文件名以交付说明为准）；或按根目录 `README.md`「构建」章节自行构建
> 免责声明：本项目为非官方开源项目，与 realme 官方无关；耳机控制协议来自对 realme Link 客户端的学习，**尚未在真机耳机上完整验证**。

---

## 1. 环境要求

| 项 | 要求 |
|---|---|
| 手表系统 | Wear OS 4+（API 33/Android 13）、Wear OS 5、Wear OS 6 |
| 硬件 | 支持 Bluetooth LE 的 Wear OS 手表；应用声明 `android.hardware.type.watch`，仅面向手表 |
| 耳机 | realme 系 TWS（如 realme Buds Air 6 Pro / Buds T300）；其他品牌可体验界面但无法控制 |
| 电脑 | Windows / macOS / Linux + Android SDK Platform-Tools（`adb`） |
| 网络 | 手表与电脑处于**同一 Wi-Fi**（无线调试必需） |

---

## 2. 手表开启开发者模式与 ADB 调试

Wear OS 4/5/6 的入口与手机类似：

1. 手表 → **设置** → **系统** → **关于**（部分版本为「关于手表」）→ 连续点击 **版本号 / 内部版本号** 7 次，提示「您已处于开发者模式」。
2. 返回 **设置** → **系统** → **开发者选项**：
   - 打开 **ADB 调试**；
   - 打开 **无线调试**（Wireless debugging，Wear OS 4+ 首选）；
   - 部分机型额外提供 **通过蓝牙调试**（Debug over Bluetooth），无 Wi-Fi 时可用，但速度慢且依赖手机端配合。
3. 保持手表**亮屏、靠近电脑、与电脑同一 Wi-Fi**。

> 若手表支持 USB-C 数据线直连（部分 Wear OS 机型），也可直接插线调试，跳过无线配对，见 §3.2。

---

## 3. 连接 adb

### 3.1 无线调试（推荐）

1. 手表：**开发者选项 → 无线调试 → 配对新的设备**，屏幕显示：
   - IP 地址与**配对端口**（如 `192.168.1.23:37419`）
   - 6 位**配对码**
2. 电脑执行配对（配对端口与调试端口不同，注意区分）：

   ```powershell
   adb pair 192.168.1.23:37419
   # 按提示输入手表上的 6 位配对码
   ```

3. 回到手表无线调试主页面，记下**调试端口**（如 `192.168.1.23:42157`），连接：

   ```powershell
   adb connect 192.168.1.23:42157
   adb devices
   ```

   `adb devices` 应出现 `192.168.1.23:42157  device`（若为 `unauthorized`，见 §6.4）。

4. 验证设备确为手表且系统版本达标：

   ```powershell
   adb shell getprop ro.build.characteristics   # 应包含 watch
   adb shell getprop ro.build.version.sdk       # 应 >= 33
   ```

### 3.2 USB 数据线（如手表支持）

```powershell
adb devices
# 手表上出现「允许 USB 调试吗？」→ 勾选「一律允许」→ 允许
```

若 `adb devices` 无设备：换数据线（需数据线而非纯充电线）、安装手表厂商 USB 驱动、在设备管理器确认无未知设备。

---

## 4. 安装应用

APK 产物与交付包：

| 类型 | 路径 | 包名 |
|---|---|---|
| **已签名交付 APK（推荐）** | `dist/耳机控制器-1.1.1.apk`（最终文件名以交付说明为准） | `com.cyhao.realmebuds` |
| debug（开箱即装） | `app/build/outputs/apk/debug/app-debug.apk` | `com.cyhao.realmebuds.debug` |
| release（自行构建） | `app/build/outputs/apk/release/app-release-unsigned.apk`（未配置签名，**不可直接安装**） | `com.cyhao.realmebuds` |

安装已签名交付包（推荐）：

```powershell
adb install -r "dist\耳机控制器-1.1.1.apk"
```

或安装 debug 包（包名带 `.debug`，可与正式版共存）：

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

- `-r`：覆盖安装（保留数据）。
- 若提示 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`（签名不一致），先卸载旧包再装：

  ```powershell
  adb uninstall com.cyhao.realmebuds
  # 若装的是 debug 包：
  adb uninstall com.cyhao.realmebuds.debug
  ```

- 若提示 `INSTALL_FAILED_OLDER_SDK`：手表系统低于 Android 13（Wear OS 3.x 及更早），本应用不支持。

安装后启动：

```powershell
# 正式版（已签名交付包）
adb shell am start -n com.cyhao.realmebuds/com.cyhao.realmebuds.MainActivity
# 调试版
adb shell am start -n com.cyhao.realmebuds.debug/com.cyhao.realmebuds.MainActivity
```

也可以直接在手表应用列表中找到 **「耳机控制器」** 图标启动。

卸载：

```powershell
adb uninstall com.cyhao.realmebuds
# 或调试版
adb uninstall com.cyhao.realmebuds.debug
```

---

## 5. 权限说明

应用在扫描页首次使用时**运行时申请**以下权限（Android 12+ / API 31+ 蓝牙权限模型）：

| 权限 | 用途 | 说明 |
|---|---|---|
| `BLUETOOTH_SCAN` | 扫描附近 BLE 耳机 | 声明了 `usesPermissionFlags="neverForLocation"`，**不需要定位权限**、不采集位置 |
| `BLUETOOTH_CONNECT` | 连接耳机、读写 GATT、获取设备名 | 连接与控制的必需权限 |
| `VIBRATE` | 查找耳机/确认操作的触觉反馈 | 普通权限，安装即授予 |
| `POST_NOTIFICATIONS` | 预留通知能力 | Android 13+ 运行时权限，本版本核心流程不依赖 |
| `WAKE_LOCK` | 扫描/连接期间保持唤醒 | 普通权限 |

说明：
- `minSdk = 33`，因此 `BLUETOOTH` / `BLUETOOTH_ADMIN` / `ACCESS_FINE_LOCATION` 仅在清单中以 `maxSdkVersion=30` 兼容保留，**Wear OS 4+ 不会请求定位权限**。
- 权限被拒绝时，扫描页会显示「需要蓝牙权限才能扫描设备」与「授予蓝牙权限」按钮；也可到 **设置 → 应用 → 耳机控制器 → 权限** 手动开启。
- 可用 adb 预授权（调试方便，注意包名后缀）：

  ```powershell
  adb shell pm grant com.cyhao.realmebuds.debug android.permission.BLUETOOTH_SCAN
  adb shell pm grant com.cyhao.realmebuds.debug android.permission.BLUETOOTH_CONNECT
  ```

---

## 6. 常见问题（FAQ）

### 6.1 打开应用是「演示数据」，不是我的耳机？

**演示模式默认开启**（`AppSettings.demoMode` 默认 `true`），首次启动展示模拟设备 *realme Buds Air 6 Pro*（电量 82%/78%/91%）。

**关闭方法**：主页 →「设置」→ **关于** → 关闭 **「演示模式」** 开关 → 返回主页 →「扫描并连接」。
关闭后若未连接，主页设备卡显示「未连接」，首行变为「扫描并连接」。

> 演示模式用于无耳机/无权限时完整体验 UI；切换后无需重启应用。

### 6.2 扫描不到耳机

按顺序排查：

1. **耳机进入配对态**：耳机放入充电盒、开盖，长按充电盒功能键约 3 秒，直到指示灯闪烁（不同型号以说明书为准）。
2. **手机抢连**：耳机可能已连接手机。先在手机蓝牙中「断开」或关闭手机蓝牙，再在手表扫描。
3. **距离与遮挡**：耳机与手表保持 1 米内，避开金属桌面/微波炉等干扰源。
4. **权限**：确认 `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT` 已授予（§5）。
5. **保持前台**：应用需在前台且屏幕常亮；系统可能限制后台 BLE 扫描。
6. **重新扫描**：一次扫描约 10–12 秒，结束后点「重新扫描」；或重启手表蓝牙后再试。
7. 若列表始终为空但演示模式正常，说明是 BLE 扫描/协议问题而非界面问题（见 §6.3、§7）。

### 6.3 扫描到但连不上 / 连接后控制无效

- **协议未真机验证**：realme 私有控制协议仍在真机验证阶段（见 `docs/BLE_IMPL.md` 与 `docs/PROTOCOL.md`）。主要命令已按 earFrame/Gadgetbridge 等开源实现绑定（电量/ANC/EQ/手势/游戏模式/查找/固件），但仍可能出现「能连上但个别指令无响应」「ANC 等级/EQ 高档位枚举不符」等情况，属于已知限制，不代表界面 Bug。
- **传输通道是双通道**：realme Link 官方默认走 **SPP（经典蓝牙 RFCOMM）**，BLE GATT 为并存通道；本应用实现为「BLE 扫描/连接 + RFCOMM 控制通道兜底」。若真机提示连接成功但数据不更新：先确认耳机已在**系统蓝牙设置中完成配对（BR/EDR 绑定）**，再重试；RFCOMM 首次连接可能需要系统配对弹窗。
- 先解除耳机与手机的配对，并关闭手机上的 realme Link（它可能占用同一耳机连接）。
- 让耳机远离其他已配对设备，手表重启蓝牙后重试。
- 若连接后立即断开：确认耳机未处于低电量/固件异常状态，放回充电盒再取出。
- 仅 realme 系耳机受支持；其他品牌可能播放提示音等通用操作有效，降噪/EQ 无效。

### 6.4 adb 相关

| 现象 | 处理 |
|---|---|
| `adb devices` 显示 `unauthorized` | 手表上点「允许调试」并勾选「一律允许」；若弹窗错过：开发者选项 → 撤销 USB 调试授权，重新连接 |
| `adb: device offline` | `adb kill-server` → `adb start-server` → 重新 `adb connect`；无线调试断开后需重新配对 |
| `adb pair` 提示配对码错误/过期 | 手表上「配对新的设备」重新生成（配对码一次性，约 1 分钟有效） |
| 只能看到手机设备 | `adb devices` 有多台时用 `adb -s <序列号> install ...` 指定手表 |
| 无线调试找不到手表 | 确认同一 Wi-Fi、关闭 PC 防火墙/代理、手表未休眠；或改用 USB / 蓝牙调试 |
| `adb: failed to install ... INSTALL_FAILED_INSUFFICIENT_STORAGE` | 手表存储不足，清理后重装 |
| release APK 装不上 | 自行构建的 release 未签名（`app-release-unsigned.apk`），请改用已签名交付包 `dist/耳机控制器-1.1.1.apk`，或安装 debug 包 |
| OPPO Watch 提示「解析软件包出现问题」 | 旧版 minSdk 33 导致；1.1.1 起 minSdk 26，可正常安装。仍失败请提供手表系统版本（设置 → 关于 → 版本 / db shell getprop ro.build.version.sdk） |

### 6.5 权限弹窗不再出现

若曾点「拒绝」且勾选不再询问：**设置 → 应用 → 耳机控制器 → 权限 → 附近设备** 手动开启；或按 §5 用 adb `pm grant` 授予。

### 6.6 手表上没有应用图标

- 确认安装到了手表而不是手机：`adb shell pm list packages | findstr realmebuds` 在手表设备上执行应能看到包名。
- 部分表盘启动器把应用收在「应用列表」第二页/下滑菜单。
- 确认 APK 为 `minSdk 33` 且手表 API ≥ 33。

---

## 7. 已知限制（与安装相关）

1. **realme 私有协议未在真机完整验证**：连接/控制可能不稳定，部分指令枚举（ANC 等级、EQ 高档位、手势「切换降噪」）仍属推断；恢复出厂设置未下发。详见 `docs/BLE_IMPL.md` §9。
2. **需先系统配对（RFCOMM）**：realme Air 系列控制走经典蓝牙 RFCOMM，需先在手表「设置 → 蓝牙」中完成耳机配对（BR/EDR 绑定）；仅 BLE 扫描到设备不代表控制通道可用。
3. **固件升级未实现**：手表端不提供固件更新（realme Link 的 OTA 能力未移植）。
4. **release 未配置签名**：仓库默认输出未签名 release APK，正式分发需自行签名（交付版见 `dist/`）。
5. **单耳机型号适配有限**：功能开关是否可用取决于耳机型号上报的能力位；34 个型号的延时/UUID 配置未完整下发。
6. **无手机伴侣 App**：本应用为 standalone（`com.google.android.wearable.standalone=true`），不依赖手机 realme Link，也无法读取 realme Link 的云端数据。
7. **图片说明**：README/docs 中的界面图为真机截图（Wear OS 手表），启动图标 PNG 为矢量图标导出的预览图。
