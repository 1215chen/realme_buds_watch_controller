# 安装与配对指南 · 耳机控制器

> 适用：**Wear OS 4/5/6** 手表，以及 **OPPO Watch（ColorOS Watch，Android 8.0+ / API 26+）**
> 应用名：**耳机控制器**；正式版包名 `com.cyhao.realmebuds`，调试版 `com.cyhao.realmebuds.debug`（debug 构建带 `.debug` 后缀，可与正式版共存）
> 前置：直接使用已签名交付包 `dist/耳机控制器-1.1.1.apk`（文件名以交付说明为准）；或按根目录 `README.md`「构建」章节自行构建
> 免责声明：本项目为非官方开源项目，与 realme / OPPO 官方无关；耳机控制协议来自互操作研究，已在 realme Buds T200 与 OPPO Enco Air 5 Pro 上真机验证，其他型号可能存在差异。

---

## 1. 环境要求

| 项 | 要求 |
|---|---|
| 手表系统 | Wear OS 4/5/6（Android 13+ 推荐）；ColorOS Watch（OPPO Watch 系列，Android 8.0+ / API 26+） |
| 硬件 | 支持 Bluetooth LE 的手表；`android.hardware.type.watch` 为非强制 feature，Wear OS 与 OPPO Watch 均可安装 |
| 耳机 | realme / OPPO 系 TWS：`realme Buds T200`、`OPPO Enco Air 5 Pro` 已真机验证；同协议族其他型号可能部分可用 |
| 电脑 | Windows / macOS / Linux + Android SDK Platform-Tools（`adb`） |
| 网络 | 手表与电脑处于**同一 Wi-Fi**（无线调试必需） |

---

## 2. 手表开启开发者模式与 ADB 调试

### Wear OS 4/5/6

1. 手表 → **设置** → **系统** → **关于**（部分版本为「关于手表」）→ 连续点击 **版本号 / 内部版本号** 7 次，提示「您已处于开发者模式」。
2. 返回 **设置** → **系统** → **开发者选项**：
   - 打开 **ADB 调试**；
   - 打开 **无线调试**（Wireless debugging，推荐）；
   - 部分机型额外提供 **通过蓝牙调试**（Debug over Bluetooth），无 Wi-Fi 时可用，但速度慢且依赖手机端配合。
3. 保持手表**亮屏、靠近电脑、与电脑同一 Wi-Fi**。

### OPPO Watch（ColorOS Watch）

1. 手表 → **设置** → **其他设置** → **关于手表** → 连续点击 **版本号** 7 次。
2. 返回 **设置** → **其他设置** → **开发者选项**：
   - 打开 **USB 调试**；
   - 若系统提供 **无线调试**，可直接使用无线安装；否则用 USB 数据线连接电脑。
3. 保持手表亮屏；如有「允许 USB 调试」弹窗，勾选「一律允许」。

> 若手表支持 USB-C 数据线直连，也可直接插线调试，跳过无线配对，见 §3.2。

---

## 3. 连接 adb

### 3.1 无线调试（推荐，Wear OS / 部分 ColorOS Watch）

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

4. 验证设备与系统版本（最低 Android 8.0 / API 26）：

   ```powershell
   adb shell getprop ro.build.version.sdk       # Wear OS 一般 >= 33；OPPO Watch 一般 26~31
   adb shell getprop ro.build.characteristics   # Wear OS 通常包含 watch；ColorOS 可能为空，可忽略
   ```

### 3.2 USB 数据线（Wear OS / OPPO Watch 均可）

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

- 若提示 `INSTALL_FAILED_OLDER_SDK`：手表系统低于 Android 8.0（API 26），本应用不支持。
- OPPO Watch 若在文件管理器/安装器中提示「解析软件包出现问题」：请确认安装的是 1.1.1 及以上版本（旧版 minSdk 33 会触发该提示）。

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

扫描耳机时按系统版本申请权限：

| 系统 | 申请权限 |
|---|---|
| Android 12+（API 31+，Wear OS 4/5/6） | `BLUETOOTH_SCAN`、`BLUETOOTH_CONNECT` |
| Android 11 及以下（API 26–30，多数 ColorOS Watch） | `ACCESS_FINE_LOCATION`（扫描 BLE 的系统要求；不采集位置） |

| 权限 | 用途 | 说明 |
|---|---|---|
| `BLUETOOTH_SCAN` | 扫描附近 BLE 耳机 | Android 12+；声明 `neverForLocation`，**不需要定位权限**、不采集位置 |
| `BLUETOOTH_CONNECT` | 连接耳机、读写 GATT、获取设备名 | Android 12+；连接与控制的必需权限 |
| `ACCESS_FINE_LOCATION` | Android 11 及以下扫描 BLE | 仅旧系统申请；同时需要系统定位开关打开 |
| `VIBRATE` | 降噪切换/开关/连接成功的震动反馈 | 普通权限，安装即授予 |
| `POST_NOTIFICATIONS` | 预留通知能力 | Android 13+ 运行时权限，本版本核心流程不依赖 |
| `WAKE_LOCK` | 扫描/连接期间保持唤醒 | 普通权限 |

说明：
- `minSdk = 26`：`BLUETOOTH` / `BLUETOOTH_ADMIN` / `ACCESS_FINE_LOCATION` 在清单中带 `maxSdkVersion=30`，Android 12+ 不会请求这些旧权限。
- 权限被拒绝时，扫描页会显示「需要蓝牙权限才能扫描设备」与「授予权限」按钮；也可到 **设置 → 应用 → 耳机控制器 → 权限** 手动开启。
- 可用 adb 预授权（调试方便，注意包名后缀）：

  ```powershell
  # Android 12+
  adb shell pm grant com.cyhao.realmebuds.debug android.permission.BLUETOOTH_SCAN
  adb shell pm grant com.cyhao.realmebuds.debug android.permission.BLUETOOTH_CONNECT
  # Android 11 及以下
  adb shell pm grant com.cyhao.realmebuds.debug android.permission.ACCESS_FINE_LOCATION
  ```

---

## 6. 常见问题（FAQ）

### 6.1 打开应用是「演示数据」，不是我的耳机？

**演示模式默认关闭**，首次启动为真实设备模式：主页显示「未连接」，点「搜索并连接」扫描耳机。

**想体验界面**：主页 →「设置」→ 打开 **「演示模式」** 开关（模拟 realme Buds Air 6 Pro，电量 82%/78%/91%）；关闭后恢复真实设备模式，无需重启应用。

### 6.2 扫描不到耳机

按顺序排查：

1. **耳机进入配对态**：耳机放入充电盒、开盖，长按充电盒功能键约 3 秒，直到指示灯闪烁（不同型号以说明书为准）。
2. **手机抢连**：耳机可能已连接手机。先在手机蓝牙中「断开」或关闭手机蓝牙，再在手表扫描。
3. **距离与遮挡**：耳机与手表保持 1 米内，避开金属桌面/微波炉等干扰源。
4. **权限**：Android 12+ 确认蓝牙权限；**Android 11 及以下需要定位权限且系统定位开关打开**。
5. **保持前台**：应用需在前台且屏幕常亮；系统可能限制后台 BLE 扫描。
6. **重新扫描**：一次扫描约 10–12 秒，结束后点「重新扫描」；或重启手表蓝牙后再试。
7. 若列表始终为空但演示模式正常，说明是 BLE 扫描/协议问题而非界面问题（见 §6.3、§7）。

### 6.3 扫描到但连不上 / 连接后控制无效

- **协议兼容性**：控制协议已在 `realme Buds T200`、`OPPO Enco Air 5 Pro` 上验证；不同型号/固件的命令支持子集可能不同，可能出现「能连上但个别指令无响应」「ANC 等级/EQ 档位不符」，属于已知限制。
- **传输通道是双通道**：realme Link 官方默认走 **SPP（经典蓝牙 RFCOMM）**，BLE GATT 为并存通道；本应用实现为「BLE 扫描/连接 + RFCOMM 控制通道兜底」。若提示连接成功但数据不更新：先确认耳机已在**系统蓝牙设置中完成配对（BR/EDR 绑定）**，再重试。
- 先解除耳机与手机的配对，并关闭手机上的 realme Link（它可能占用同一耳机连接）。
- 让耳机远离其他已配对设备，手表重启蓝牙后重试。
- 若连接后立即断开：确认耳机未处于低电量/固件异常状态，放回充电盒再取出。
- 非 realme/OPPO 品牌耳机可能只支持标准 BLE 电量/信息读取，无法控制降噪/EQ。

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
| OPPO Watch 提示「解析软件包出现问题」 | 旧版 minSdk 33 导致；1.1.1 起 minSdk 26，可正常安装。仍失败请提供手表系统版本（设置 → 关于 → 版本 / `adb shell getprop ro.build.version.sdk`） |

### 6.5 权限弹窗不再出现

若曾点「拒绝」且勾选不再询问：**设置 → 应用 → 耳机控制器 → 权限 → 附近设备 / 位置** 手动开启；或按 §5 用 adb `pm grant` 授予。

### 6.6 手表上没有应用图标

- 确认安装到了手表而不是手机：`adb shell pm list packages | findstr realmebuds` 在手表设备上执行应能看到包名。
- 部分表盘启动器把应用收在「应用列表」第二页/下滑菜单。
- 确认 APK 为 `minSdk 26` 且手表 API ≥ 26（Android 8.0+）。

---

## 7. 已知限制（与安装相关）

1. **型号差异**：协议已在 realme Buds T200、OPPO Enco Air 5 Pro 验证；其他型号的 ANC 等级、EQ 档位、手势「切换降噪」等枚举可能不同或未下发，恢复出厂设置命令未确认。
2. **需先系统配对（RFCOMM）**：部分 realme/OPPO 型号的控制通道走经典蓝牙 RFCOMM，需先在手表「设置 → 蓝牙」中完成耳机配对（BR/EDR 绑定）；仅 BLE 扫描到设备不代表控制通道可用。
3. **固件升级未实现**：手表端不提供固件更新（realme Link 的 OTA 能力未移植）。
4. **release 未配置签名**：仓库默认输出未签名 release APK，正式分发需自行签名（交付版见 `dist/`）。
5. **功能受耳机能力位限制**：开关/选项是否显示取决于耳机上报的能力（如双设备、入耳检测、降噪等级）。
6. **无手机伴侣 App**：本应用为 standalone（`com.google.android.wearable.standalone=true`），不依赖手机 realme Link，也无法读取 realme Link 的云端数据。
7. **图片说明**：README/docs 中的界面图为真机截图（Wear OS 手表），启动图标 PNG 为矢量图标导出的预览图。
