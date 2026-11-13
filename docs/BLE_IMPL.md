# BLE 层实现说明（realme 耳机控制）

> 代码：`app/src/main/java/com/cyhao/realmebuds/ble/`
> 更新：2026-11-13 ｜ 目标：让「真我耳机手表控制台」在 Wear OS 上通过真实蓝牙控制 realme 系 TWS 耳机。

---

## 1. 结论先行（实现程度与置信度）

| 能力 | 实现 | 置信度 | 依据 |
|---|---|---|---|
| BLE 扫描、设备识别、去重、RSSI 刷新、15s 超时 | ✅ | 高 | 平台 API 直接实现 |
| GATT 连接 / 服务发现 / MTU 247 / CCCD 订阅 | ✅ | 高 | 平台 API 直接实现 |
| BAS 0x180F/0x2A19 电量、DIS 0x180A 型号/固件 | ✅ | 高 | 标准 GATT |
| 私有协议：电量（左/右/盒 + 充电） | ✅ | 高 | earFrame HCI 抓包 + Gadgetbridge 双实现 |
| ANC 模式（关/通透/降噪） | ✅ | 高 | 抓包 + Gadgetbridge，物理效果已验证 |
| ANC 等级（智能/深度/中等/轻度） | ✅ | 中 | NTeditor Gadgetbridge 实现，未真机验证 |
| EQ 预设 | ✅ | 中 | 抓包确认 0x0406/0x010F；6 档预设映射为推断 |
| 触控手势（双击/三击/长按） | ✅ | 中 | Gadgetbridge 实现；`切换降噪` 动作无确认值 |
| 游戏模式 | ✅ | 高 | 抓包 + Gadgetbridge |
| 双设备（多点）开关 | ✅ | 中 | NTeditor Gadgetbridge 的 MISC 0x11 |
| 入耳检测开关 | ✅ | 中 | NTeditor Gadgetbridge 的 MISC 0x04 |
| 查找耳机 | ✅ | 高 | Gadgetbridge `0x0400` |
| 固件版本 | ✅ | 高 | Gadgetbridge 解析规则 |
| 恢复出厂设置 | ❌ 未下发 | 低 | APK 有 `setRestoreData`，opcode 未确认，属高风险操作 |

> **重要事实**：realme Air 系列（如 Buds Air 7 Pro）的控制协议**实际走经典蓝牙 RFCOMM（SPP）**，而不是 BLE GATT。realme Link 的传输层两套都有（APK 静态分析确认 `BRClientConnection` 与 `GattConnection` 并存）。因此本实现采用「BLE 扫描/连接 + RFCOMM 控制通道兜底」的双通道策略：
> - BLE GATT：满足发现、标准电量/信息、并在部分 OPPO/OnePlus 机型上承载 OPO v1 私有通道；
> - RFCOMM：realme Air 系列真正可用的私有控制通道。

---

## 2. 文件结构

```
ble/
├── BleBudsRepository.kt    仓库实现：状态机、命令队列、状态映射、生命周期
├── BleScanner.kt           BLE 扫描 + DeviceMatcher 设备识别
├── GattTransport.kt        BLE GATT 传输（连接/服务/MTU/通知/BAS/DIS）
├── RfcommTransport.kt      经典蓝牙 RFCOMM 传输（realme 实际控制通道）
├── StandardGattProfile.kt  BAS/DIS 标准特征解析
├── ProtocolFrames.kt       帧方言（OPO / Telink）、编码器、流式组装器
├── RealmeProtocol.kt       命令构造、通知解析（payload 编码/解码）
├── RealmeCommandMap.kt     ★「功能 ↔ 命令字」唯一映射表 + 置信度 + 备注
└── BleConstants.kt         UUID / 名称识别 / 时序 / 握手常量
```

> 命令语义分层：`RealmeCommandMap`（功能↔命令字+置信度）→ `RealmeProtocol`（命令 payload / 解析）
> → `BleBudsRepository`（队列/状态）。APK 分析报告指出「165 个命令常量 → 业务语义」尚未由反汇编直接绑定，
> 本表取值来自 HCI 抓包/成熟开源实现交叉验证，每条都标注了 `CONFIRMED/LIKELY/UNVERIFIED`；
> 真机抓包后只需改 `RealmeCommandMap`（必要时改 `OpoCommand` 原始常量），上层无需改动。

线程模型：

```
UI/ViewModel ──(public methods)──> HandlerThread "budswear-ble" ──> StateFlow / SharedFlow
                                         ▲
     BLE ScanCallback / GATT callback / RFCOMM 读写线程 ──(post)──┘
```

- 所有状态只在单个 HandlerThread 上修改，`StateFlow` 本身线程安全，无需锁。
- RFCOMM 的阻塞 connect/read 在独立单线程执行器上，写操作在另一个单线程执行器，避免阻塞命令队列。
- `close()` 在该 HandlerThread 上停止扫描、断开 GATT/RFCOMM、移除全部回调后 `quitSafely()`。

---

## 3. 扫描（`BleScanner`）

- 使用 `BluetoothLeScanner.startScan(null, SCAN_MODE_LOW_LATENCY, callback)`。
- 识别规则（`DeviceMatcher`）：
  - 名称包含 `realme / oppo / oneplus / enco / buds`（忽略大小写）；
  - 广播 Service UUID 命中 `0000079a-…`（OPO）、`FE2C`、`99999999-…`、`df21fe2c-…`；
  - 厂商数据 ASCII 包含上述品牌片段。
- 去重：按 MAC 地址存 `LinkedHashMap`，同名同 RSSI 不重复发射；列表按「真我系优先 + RSSI 降序」排序。
- 15 秒自动停止（`SCAN_TIMEOUT_MS`），超时 emit `R.string.msg_scan_finished`。
- 权限缺失（`BLUETOOTH_SCAN`）、蓝牙关闭、`startScan` 失败时安全返回 false，仓库 emit `R.string.msg_bt_not_ready`，不崩溃。

---

## 4. 连接状态机（`BleBudsRepository`）

```
Disconnected
   │ connect()
   ▼
Connecting ── GATT 就绪且命中私有写/通知特征 ──> Connected（控制通道 = BLE_GATT）
   │
   ├── GATT 就绪但只有标准服务 ──> Connected（标准兜底：电量/型号/固件）
   │        └── 若设备为真我系 → 尝试 RFCOMM
   │                 └── RFCOMM 就绪 ──> Connected（控制通道 = RFCOMM，开始初始化序列）
   │
   ├── GATT 失败/超时 → 尝试 RFCOMM → 失败则 Disconnected + msg_failed
   │
   └── 连接后异常断开 → 自动重连 1 次（`MAX_RECONNECT_ATTEMPTS`）；仍失败 → Disconnected + msg_disconnected
```

- GATT：`connectGatt(context, false, callback, TRANSPORT_LE)` → `discoverServices()` → 选择私有通道 → `requestMtu(247)` → `setCharacteristicNotification` + 写 CCCD → 读 BAS/DIS → 就绪。
- 私有通道选择：优先 OPO 服务（写 `0100079a-…`/通知 `0200079a-…`）；找不到时退化为「非标准服务中同时有可写与可通知特征」的通用通道（realme Link 的私有 UUID 可能随型号配置下发，APK 静态分析未拿到字面量）。
- RFCOMM：依次尝试 `OPO_SERVICE`、`SPP_SERVICE` 的 secure → insecure socket；realme 系列真实可用。
- 连接整体超时 12s，RFCOMM 额外 10s；所有 `SecurityException`/`IOException` 均捕获。

初始化序列（控制通道就绪后，串行队列逐条发送，静默不提示失败）：

1. `0x0106` 电量查询（同时用于帧方言探测）
2. `0x010D` MISC 配置查询（游戏模式 0x06 / 多点 0x11 / 入耳 0x04）
3. `0x0109` 状态查询
4. `0x010C` ANC 模式查询（payload `01 01`）
5. `0x010C` ANC 等级查询（payload `04 01`）
6. `0x0108` 触控配置查询（payload `02 03 01`）
7. `0x0205` 订阅（电量/状态/ANC/游戏模式/多点）
8. `0x0105` 固件查询
9. `0x010F` EQ 查询

BLE GATT 私有通道额外先做 HELLO（`0x0100`）→ REGISTER（`0x8500`，token `B5 50 A0 69`）——这是 OPO v1 over BLE GATT 的会话建立要求；realme Air 的 RFCOMM 通道不需要应用层握手。

---

## 5. 命令队列与 ACK 机制

- 串行队列：同一时刻只有一条命令在等待响应，避免 SPP 流式写入竞争。
- 发送后启动 2.5s 超时；响应帧命令字 == `请求 cmd | 0x8000` 视为 ACK/响应（例如 `0x0106 → 0x8106`、`0x0404 → 0x8404`）。
- 超时重试：非静默命令重试 1 次；静默初始化命令不重试，只继续下一条，不打扰用户。
- 用户操作优先：用户命令入队时丢弃尚未发送的静默初始化查询，避免等待过久。
- 失败：emit `R.string.msg_failed`；成功：emit `R.string.msg_command_sent`（初始化/查询不提示）。
- 收到响应帧后先按 ACK 完成队列项，再交给协议层解析状态事件，保证「先解析真实状态、后乐观更新」。

---

## 6. 协议层

### 6.1 帧方言（`ProtocolFrames.kt`）

逆向资料显示同一套 16 位命令字被两种 SDK 使用，帧头不同：

**OPO v1（realme Air 系列 RFCOMM / 部分 OPPO、OnePlus BLE GATT）**，little-endian，9 字节头：

```
AA | LL | 00 | 00 | CC CC | SS | PP PP | payload
```

- `LL` = 整帧长度 − 2 = 7 + payload.size
- `CC CC` = 16 位命令字，响应帧 bit15 置 1
- `SS` = 序号；`PP PP` = payload 长度
- 无应用层校验字段（依赖 SPP/L2CAP）

证据：realme Buds Air 7 Pro HCI 抓包（earFrame `docs/protocol.md`）、Gadgetbridge/NTeditor 的 OPlus 实现。

**Telink SDK（realme Link 新版 `VENDOR_TL` 型号）**，big-endian，5 字节头：

```
CC CC | SS | LL LL | payload
```

- `CC CC` = 16 位命令字（bit15 = 响应标志），`SS` = 序号，`LL LL` = payload 长度
- 证据：APK `com/realme/iot/headset/tl/protocol/packet/Packet`（`docs/PROTOCOL.md`）

**运行时自适应**：默认按 OPO 编码发送首条电量查询；若 2.5s 内**完全没有收到任何帧**，自动切换 TL 方言重发；一旦收到帧，按帧首字节自动识别并锁定方言（`AA` → OPO，`0x01..0x05/0x0F/0x8x` → TL）。两种方言共用命令字，因此 ACK 匹配逻辑一致。

> 补充：APK 分析（`docs/PROTOCOL.md`）显示 realme Link 5.5.514 国内版对 34 个 `VENDOR_TL` 型号使用 TL 5 字节帧；而公开 HCI 抓包显示 realme Buds Air 7 Pro 使用 OPO 9 字节帧（两者很可能对应不同 SDK/固件代际）。命令字空间二者高度重合（0x0106 电量、0x0105 固件、0x0404 降噪、0x0406 EQ…），因此本实现用同一张功能映射表 + 双帧编码器覆盖两代设备。

### 6.2 命令表（代码：`OpoCommand`）

| 功能 | 请求/设置 | 响应/ACK | payload |
|---|---:|---:|---|
| 电量查询 | `0x0106` | `0x8106` | 空 |
| 电量上报（订阅） | — | `0x0204` type `0x01` | `01, count, (side, level)*` |
| 固件查询 | `0x0105` | `0x8105` | 空 |
| 状态查询 | `0x0109` | `0x8109` | 空 |
| ANC 查询 | `0x010C` | `0x810C` | `01 01`（模式）/ `04 01`（等级） |
| ANC 设置 | `0x0404` | `0x8404` | `01 01 <mode>` |
| EQ 查询 | `0x010F` | `0x810F` | 空 |
| EQ 设置 | `0x0406` | `0x8406` | `<preset>` |
| 触控查询 | `0x0108` | `0x8108` | `02 03 01` |
| 触控设置 | `0x0401` | `0x8401` | `01 <side> <typeLE> <action>` |
| MISC 查询 | `0x010D` | `0x810D` | `count, type...` |
| MISC 设置 | `0x0403` | `0x8403` | `<type> <0/1>` |
| 查找耳机 | `0x0400` | `0x8400` | `01`（开始）/ `00`（停止） |
| 订阅 | `0x0205` | `0x8205` | `09, type...` |
| 会话 HELLO（BLE GATT） | `0x0100` | `0x8100` | 空 |
| 会话 REGISTER（BLE GATT） | `0x8500` | — | `00 B5 50 A0 69` |

取值映射：

- ANC 模式：关 `0x01`、通透 `0x02`、降噪按等级：轻度 `0x04`、深度 `0x08`、中等 `0x10`、智能 `0x20`。
- EQ 预设：0 默认、1 低音增强、2 均衡、3 高音增强、4 人声、5 动态（realme Air 7 Pro 抓包确认 0–3，4/5 为按枚举顺延的推断）。
- 触控类型：双击 `0x0201`、三击 `0x0301`、长按 `0x0401`；左右耳都下发（side `0x01`/`0x02`）。
- 触控动作：无操作 `0x00`、播放/暂停 `0x01`、语音助手 `0x04`、上一首 `0x05`、下一首 `0x06`、音量+ `0x0B`、音量− `0x0C`。
- 电量：side 左 `1`/右 `2`/盒 `3`；level 低 7 位百分比、bit7 充电；盒 0 表示不可用。

### 6.3 解析与状态映射

`RealmeProtocol.parse()` 返回 `ProtocolEvent`，仓库映射到 `BudsState`：

| 事件 | 字段 |
|---|---|
| Battery | `battery`（左/右/盒 + charging） |
| Anc | `ancMode` / `ancLevel` |
| Eq | `eqPreset` |
| GameMode | `gameMode` |
| DualConnection | `dualConnection` |
| InEar | `inEarDetection` |
| Gestures | `gestures`（左/右耳合并） |
| Firmware | `firmware` |

---

## 7. 标准 GATT 兜底

若设备没有私有控制通道（或私有协议不响应），仍保持 `Connected` 并展示：

- BAS 服务 `0x180F` / 特征 `0x2A19`：整机电量（映射到 `BatteryInfo.right`，UI 上作为「单节电量」显示）。
- DIS 服务 `0x180A`：`0x2A24` 型号、`0x2A26` 固件版本。
- 私有控制类操作在无控制通道时 emit `msg_failed`，UI 不会崩溃。

---

## 8. 权限与边界

- 必需权限：`BLUETOOTH_SCAN`、`BLUETOOTH_CONNECT`（manifest 已声明）。
- 权限缺失/蓝牙关闭：扫描返回 false、连接直接 emit `msg_bt_not_ready`，不做任何 BLE 调用。
- 所有系统回调包 try/catch（`SecurityException`、`IOException`、`IllegalArgumentException`），异常只记录日志，不向上抛。
- `close()` 幂等；扫描回调、GATT 回调、RFCOMM 线程均在关闭后停止回调。

---

## 9. 已知限制 / 待真机验证

1. **真机验证项**
   - realme Air 系列：BLE 扫描能否发现处于配对/已断开状态的耳机；BLE 地址与 BR/EDR 地址是否一致（RFCOMM 连接依赖此点）。
   - RFCOMM 首次连接是否需要系统配对/绑定；未配对时 secure/insecure 的兼容性（Wear OS 表端表现）。
   - ANC 等级 `0x04/0x10/0x20`、EQ 4/5 档、双设备/入耳检测开关的真实效果。
   - BLE GATT 形态（OPPO/OnePlus 机型）的 HELLO/REGISTER token 是否与默认值一致。
2. **未实现/未下发**：恢复出厂设置（`setRestoreData` opcode 未确认，避免误触发）；空间音效、佩戴状态展示等 UI 未覆盖的能力。
3. **触控「切换降噪」**：realme 协议层无确认动作值，仓库会提示 `msg_failed`，不会发送错误命令。
4. **多设备**：当前只实现开关（MISC `0x11`），未实现多点设备列表查询/连接管理。
5. **帧方言不确定性**：型号→方言没有可靠映射，依赖运行时探测；若设备对 OPO 探测帧返回异常响应，可能误判，需真机抓包收敛。
6. **BLE 私有 UUID**：APK 静态分析未拿到字面量，已实现通用属性发现兜底；若真机私有通道 UUID 已知，只需改 `BleConstants`。
7. **无设备环境**：本实现未连接任何真实耳机验证，编译通过与协议正确性需真机回归。

---

## 10. 测试方法

### 10.1 编译

```bash
gradle.bat :app:compileDebugKotlin
```

### 10.2 无耳机（模拟器/手表）

- 演示模式（About 页）应完整可用；
- 关闭蓝牙或拒绝权限时，扫描/连接应弹出「蓝牙未就绪或未授权」，不崩溃；
- 扫描页 15 秒后自动停止并提示「扫描完成」；无结果时列表为空。

### 10.3 真机（建议步骤）

1. 提前在系统设置中配对耳机（realme Air 系列走 RFCOMM 需要 BR/EDR 绑定）。
2. 打开 App → 扫描并连接 → 观察主页电量/固件/降噪状态是否被初始化序列拉取。
3. 依次操作：降噪模式/等级、EQ、触控手势、游戏模式、双设备、入耳检测、查找耳机，观察 `msg_command_sent` 与耳机实际反馈。
4. 断开：主动断开应显示「已断开」；走到蓝牙范围外应自动重连 1 次后提示断开。
5. 抓包（Android「蓝牙 HCI 监听日志」/ btsnoop）核对命令字与载荷，日志 TAG：`BleBudsRepository`、`GattTransport`、`RfcommTransport`、`BleScanner`。

---

## 11. 参考资料

- earFrame（realme Buds Air 7 Pro 协议逆向与 HCI 抓包）：<https://github.com/rosenholt/earFrame>（`docs/protocol.md`、`docs/protocol-status.md`）
- Gadgetbridge OPPO/realme OPlus 实现：<https://codeberg.org/Freeyourgadget/Gadgetbridge>
- NTeditor Gadgetbridge fork（ANC 等级/多点/入耳检测）：<https://codeberg.org/NTeditor/GadgetBridge>
- OPO v1 over BLE GATT（OnePlus Nord Buds 3 Pro）：<https://github.com/AasheeshLikePanner/cracked-oneplus-buds>
- OBudsManager（Windows BLE GATT OPOv1）：<https://github.com/thesidproject/OBudsManager>
- 本仓库 APK 分析：`docs/PROTOCOL.md`、`docs/PROTOCOL.md`

> 说明：以上外部资料仅作为协议证据使用；耳机控制协议来自对 realme Link 客户端的学习，本项目为非官方开源项目，与 realme 官方无关。

