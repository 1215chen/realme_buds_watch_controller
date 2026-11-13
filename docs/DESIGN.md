# 设计规范 · 耳机控制器（Wear Material 3 落地）

> 适用范围：`com.cyhao.realmebuds`（耳机控制器 / Buds Wear）
> 设计基线：**Wear Compose Material 3 1.6.2**（本版本无 Chip 组件）
> 配色基线：**Wear Material 3 官方默认 token**（primary `#E9DDFF` / background `#000000`，**非自定义配色**）
> 实现源：`app/src/main/java/com/cyhao/realmebuds/ui/theme/Theme.kt`、`ui/components/WearComponents.kt`、`ui/screens/*`
> 预览图：`docs/mockups/home.png`、`noise.png`、`equalizer.png`、`gesture.png`、`scan.png`（设计稿预览，非真机截图）

---

## 1. 设计原则

1. **深色优先**：采用 Wear Material 3 官方默认深色 token（`background = #000000`），降低 AMOLED 功耗并保持系统默认观感。
2. **一屏一件事**：每页只承载一个任务（降噪 / 音效 / 手势 / 扫描），主操作放在列表中上部，避免圆形屏幕顶/底边缘。
3. **primary 只做强调**：官方 primary 淡紫 `#E9DDFF` 仅用于「选中态、主 CTA、关键状态、小节标题」，不用于大面积背景与长段落文字；**不使用任何自定义配色/品牌色**。
4. **大字号 + 大触控**：正文不小于 `labelMedium`（15sp）级别，列表行高度 ≥ 48dp（`Button` 默认 52dp），保证在 1.2 英寸表盘上可点可读。
5. **无 Chip 约束**：Wear M3 1.6.2 没有 Chip / SegmentedButton，单选一律用 `TextToggleButton`（横排/列表）或 `ButtonGroup` 组合，不做自定义 Chip 仿制。

---

## 2. 色彩 Token 表（Wear Material 3 官方默认 token，非自定义配色）

> **配色基线：Wear Material 3 官方默认 token**（Wear Compose Material3 **1.6.2** 的 `MaterialTheme` 默认 `ColorScheme`，实际值取自上游源码 `ColorTokens.kt` / `PaletteTokens.kt`）。本项目**不使用任何自定义配色/品牌色**，`ui/theme/Theme.kt` 不传自定义 `ColorScheme`，设计稿与真机默认主题一致。
> 所有颜色经 `MaterialTheme.colorScheme.<角色>` 获取；业务 UI 中不允许出现十六进制字面量（`batteryColor()` 等语义映射函数只返回官方角色）。

### 2.1 主色与容器色

| 角色 | HEX | 用途 |
|---|---|---|
| primary | `#E9DDFF` | 选中态、主操作/主按钮、强调图标、小节标题、已连接状态点、电量正常 |
| onPrimary | `#210F48` | primary 底上的文字/图标 |
| primaryContainer | `#4D3D76` | 选中行容器底色（降噪等级「智能」、EQ 选中行等） |
| onPrimaryContainer | `#F6EDFF` | primaryContainer 上的文字/图标 |
| primaryDim | `#D0BCFF` | 主色的低亮变体（Wear 角色）：次级强调、禁用态主按钮、演示模式提示文字 |

### 2.2 辅助色（secondary / tertiary）

| 角色 | HEX | 用途 |
|---|---|---|
| secondary | `#DEE0FF` | 次要强调、偏低电量、连接中状态 |
| onSecondary | `#0C1649` | secondary 上的文字/图标 |
| secondaryContainer | `#3A4376` | 次要容器（预留：分组/次级选中） |
| onSecondaryContainer | `#F0EFFF` | secondaryContainer 上的文字/图标 |
| secondaryDim | `#BAC3FF` | secondary 低亮变体（官方默认） |
| tertiary | `#FFDCC2` | 第三层强调（预留：特殊/推荐状态，本版本 UI 未强依赖） |
| onTertiary | `#2E1500` | tertiary 上的文字/图标 |
| tertiaryContainer | `#6C3A03` | 第三层容器（预留） |
| onTertiaryContainer | `#FFEEE2` | tertiaryContainer 上的文字/图标 |
| tertiaryDim | `#FFB77A` | tertiary 低亮变体（官方默认） |

### 2.3 背景与表面

| 角色 | HEX | 用途 |
|---|---|---|
| background | `#000000` | 全局背景，表盘底色 |
| onBackground | `#FFFFFF` | 背景上的主文字（标题、弧形时间） |
| surfaceContainerLow | `#272430` | 最低层容器：说明卡、对话框底色 |
| surfaceContainer | `#332E3C` | 标准列表行/卡片底色（`Button`、`Card` 默认容器） |
| surfaceContainerHigh | `#494453` | 浮起层：图标底板、次级容器、悬浮控件 |
| onSurface | `#F6EDFF` | 表面上的主文字与主图标 |
| onSurfaceVariant | `#CAC4D0` | 副标签、说明文字、非激活图标 |
| outline | `#948F9A` | 按钮/输入框描边、分割线（强） |
| outlineVariant | `#615D67` | 弱分割线、电量条轨道、卡片细描边 |

### 2.4 状态色（error）与状态语义

| 角色 | HEX | 用途 |
|---|---|---|
| error | `#F2B8B5` | 低电量、错误提示、恢复出厂（危险操作） |
| onError | `#410E0B` | error 上的文字/图标 |
| errorContainer | `#8C1D18` | 危险操作按钮容器 |
| onErrorContainer | `#FCEEEE` | errorContainer 上的文字/图标 |

状态语义映射（只用官方角色，不引入绿色/黄色等自定义状态色）：

| 状态 | 角色 / HEX | 说明 |
|---|---|---|
| 已连接 | `primary` `#E9DDFF` | 官方无 success 角色；用 primary 表达正常/激活 |
| 连接中 | `secondary` `#DEE0FF` | 进行中、低强调，不抢主色 |
| 未连接 | `onSurfaceVariant` `#CAC4D0` | 中性状态，非错误 |
| 连接失败 | `error` `#F2B8B5` | 错误提示，配合 Toast 短期出现 |
| 电量 ≥ 60% | `primary` | 正常 |
| 电量 30%–59% | `secondary` | 偏低，需留意 |
| 电量 < 30% | `error` | 低电量，建议充电 |
| 电量未知（null） | `onSurfaceVariant` + 显示「—」 | 单耳取出/未上报 |

### 2.5 对比度核对（WCAG 2.1 相对亮度）

| 前景 / 背景 | 对比度 | 结论 |
|---|---|---|
| `#E9DDFF` / `#000000` | 16.25:1 | AAA |
| `#FFFFFF` / `#000000` | 21.00:1 | AAA |
| `#CAC4D0` / `#332E3C` | 7.71:1 | AAA |
| `#F6EDFF` / `#4D3D76` | 8.23:1 | AAA |
| `#210F48` / `#E9DDFF`（primary 底深字） | 13.25:1 | AAA |
| `#F2B8B5` / `#332E3C` | 7.70:1 | AAA |
| `#DEE0FF` / `#332E3C` | 10.15:1 | AAA |
| `#948F9A` / `#000000` | 6.65:1 | AA（小字仅用于次要提示） |

> 结论：主文字用 `onSurface`/`onBackground`，primary 只做强调；error 只用于低电量/错误/危险操作，不用于长段落。

### 2.6 实现口径与色值说明

- 本表即 **Wear M3 1.6.2 默认 `ColorScheme` 的实际 token 值**，已逐项对照 `Wear Compose Material3 1.6.2 源码的 ColorTokens.kt` 与 `PaletteTokens.kt`；`Theme.kt` 不传自定义 `ColorScheme`，因此**真机渲染与设计稿使用同一套颜色**。
- 应用图标：背景 `#1C1B1F`、前景 `#D0BCFF`（与 `res/values/colors.xml`、`ic_launcher_foreground.xml` 一致；图标是静态资源，不随主题变化）。
- 与 realme Link 的品牌色无关；`公开资料` 中的 realme 品牌色仅作背景资料，不进入本应用 UI。

---

## 3. 字体层级

使用 Wear M3 默认 `Typography()`，不做自定义字阶。**全部文字经 `MaterialTheme.typography` 取用**，不写死 `sp`。
下表字号/行高取自本机 Wear M3 1.6.2 源码 tokens（`Wear Compose Material3 1.6.2 源码的 TypeScaleTokens.kt`），供设计稿与实现对照。

| 场景 | 角色 | 字号 / 行高 | 说明 |
|---|---|---|---|
| 页面标题（ListHeader） | `titleLarge` | 18sp / 20sp，weight 500 | 每页唯一，居中；如「降噪控制」「音效」 |
| 列表行主标签 | `labelMedium` | 15sp / 18sp | **Button/SwitchButton 默认 label 字阶**（1.6.2 `FilledButtonTokens.LabelFont = LabelMedium`）；单行 + Ellipsis |
| 列表行副标签 | `labelSmall` | 13sp / 16sp | `Button.secondaryLabel` 默认字阶；`onSurfaceVariant` |
| 小节标题 | `titleSmall` | 14sp / 16sp | `ListHeader` 内覆写成 `titleSmall` + `primary`（如「降噪等级」） |
| 强强调数值/大按钮 | `labelLarge` | 20sp / 22sp | 比 titleLarge 更大，适合全屏倒计时/大按钮；本版本少量使用 |
| 说明/提示 | `bodySmall` | 12sp / 14sp | 页面底部说明、扫描提示；`onSurfaceVariant` |
| 电量大数字 | `titleMedium` | 16sp / 18sp | 卡片内 L/R/盒 百分比，颜色由 `batteryColor()` 决定 |
| 超大数字（可选） | `numeralMedium` / `numeralLarge` | 40sp / 50sp | 预留：全屏电量、查找耳机倒计时；三列电量卡用它会过宽，慎用 |
| 弧形时间 | `arcMedium`（TimeText 默认） | 15sp / 18sp | `TimeText` 内部默认使用 `MaterialTheme.typography.arcMedium`，不手写 |
| 极短辅助文案 | `labelSmall` / `bodyExtraSmall` | 13sp / 10sp | 状态点旁「已连接」、单位等 |

规则：
- 单行最多约 **10 个汉字**；超长文案拆到 `secondaryLabel` 或换行（`bodySmall`），禁止在圆形边缘做超长单行。
- 数字（电量、RSSI、进度）统一用 `titleMedium` 或 `numeral*`，保持等宽感与对齐；行高按上表，避免三行文字挤在 52dp 行内。
- 对齐：列表行左对齐（图标后文字），标题与小节标题居中/左对齐均可，本版本统一居中，避免圆形边缘截断。

---

## 4. 形状（Shapes）

使用 Wear M3 默认 `Shapes()`，业务代码不硬编码 `RoundedCornerShape`。源码 token：`CornerExtraSmall 4dp`、`CornerSmall 8dp`、`CornerMedium 18dp`、`CornerLarge 26dp`、`CornerExtraLarge 36dp`、`CornerFull = CircleShape`。

| 元素 | 形状来源 | 视觉 / 尺寸 |
|---|---|---|
| 列表行 `Button` | `ButtonDefaults.shape` = `ShapeTokens.CornerLarge` | 26dp 圆角，容器高度 **52dp**（`FilledButtonTokens.ContainerHeight`），图标默认 26dp |
| `TextToggleButton` | `TextToggleButtonDefaults.shapes()` = `ContainerShape CornerFull` | 52dp 默认尺寸、**全圆胶囊**；横排多选用 `Row`/`ButtonGroup` 排列 |
| `Card` / 设备卡 | `CardDefaults.shape` = `ShapeTokens.CornerLarge` | 26dp 圆角，最小高度 **64dp**（`CardTokens.ContainerMinHeight`），内容多时自动增高 |
| `SwitchButton` | 默认 `ContainerShape CornerLarge` | 26dp 大圆角轨道 + 圆形滑块 |
| `IconButton` | `ContainerShape CornerFull` | 圆形（52dp 默认 / 48dp small / 60dp large），图标 26dp |
| 对话框 `AlertDialog` | `DialogDefaults` | 大圆角，宽度受圆形屏幕限制 |
| 电量条 | 自定义 `CircleShape` | 高度 3dp 的胶囊轨道，内部填充同形状 |

> 圆形表盘内不使用方角/直角卡片；所有容器至少 16dp 圆角。`Card` 宽度不超过安全区（见 §7）。

---

## 5. 组件选型（Wear M3 1.6.2）

| 组件 | 本版本用途 | 关键参数/约定 |
|---|---|---|
| `Button`（列表行重载） | 主页 6 个功能入口、扫描结果行、Info 页面动作行 | `icon` + `label` + `secondaryLabel`；`fillMaxWidth()`；`enabled` 由连接状态控制 |
| `Button`（普通） | 「扫描并连接」「重新扫描」「授予权限」「恢复出厂设置」 | 危险操作 `colors = ButtonDefaults.buttonColors(containerColor = errorContainer, contentColor = onErrorContainer)`（实现时按 1.6.2 API 调整） |
| `TextToggleButton` | 降噪 3 模式单选、降噪等级 4 选 1、EQ 6 预设 | `checked` + `onCheckedChange`；颜色/形状用 `TextToggleButtonDefaults.colors()` / `shapes()`（选中态走主题 `primary` 系列）；横排 3 选项用 `Row`/`ButtonGroup` 排列，间距 8dp |
| `SwitchButton` | 游戏模式、双设备连接、入耳检测、演示模式 | `label` + `secondaryLabel`；开关文案用「游戏模式 / 降低延迟，音画更同步」 |
| `Card` | 主页设备卡（设备名 + 连接状态 + 电量三列） | 单卡片承载核心信息，`surfaceContainer` 底 + `outlineVariant` 细描边；不叠多层 Card |
| `TitleCard` | 设备信息页（型号/固件/蓝牙地址） | `title` + `subtitle` + `content`；用于静态信息展示 |
| `ListHeader` / `ListSubHeader` | 页面标题、小节标题 | 标题 `titleLarge`；小节标题 `titleSmall` + `primary` |
| `CircularProgressIndicator` | 扫描中/连接中 | `primary` 进度弧 + `onSurfaceVariant`/`surfaceContainerHigh` 轨道；扫描页保持屏幕常亮 |
| `AlertDialog` | 手势动作选择、恢复出厂二次确认 | 选项列表用可点击 `Button` 行；避免超长列表，必要时滚动 |
| `Dialog` / `ConfirmationDialog` | 权限引导、危险操作确认 | 圆角、最大宽度受圆形屏幕约束（≤ 300dp） |
| `Slider` | **预留**：音量调节/连续强度 | 本版本核心页未启用；若后续加「音量」页，用 `Slider(steps = ...)` + `decreaseIcon`/`increaseIcon` |
| `Picker` | **预留**：手势动作等长列表的选择 | 本版本用 `AlertDialog` + 列表按钮更符合圆形表盘；Picker 留给未来大列表场景 |
| `ScrollIndicator` | `ScreenScaffold` 默认滚动指示 | 不中途禁用，保持表冠滚动可发现性 |
| `TimeText` | 所有页面顶部弧形时间 | 由 `AppScaffold` 默认提供；子页用 `ScreenScaffold(timeText = null)` 继承外层 |
| `SwipeToDismissBox` / `SwipeDismissableNavHost` | 右滑返回 | 导航层自动提供，页面不自行处理 |

### 5.1 为什么不用 Chip
Wear Compose Material3 **1.6.2 没有 `Chip` / `ToggleChip` / `SplitToggleChip` / `SegmentedButton`**（见 `docs/WEAR_M3_API.md`）。因此：
- 单选组 → `TextToggleButton`（横排 3 项）或 `ButtonGroup`；
- 多选/开关 → `SwitchButton`；
- 分段选择 → 用一排 `TextToggleButton` 模拟，不做自定义 Chip 外观，保证与系统组件观感一致。

### 5.2 统一封装（`ui/components/WearComponents.kt`）
| 封装 | 组成 | 用途 |
|---|---|---|
| `WearSectionHeader` | `ListHeader` + `titleSmall` + `primary` | 页面内小节标题 |
| `WearListButton` | `Button` + `icon` + `label` + `secondaryLabel` | 所有列表行入口 |
| `WearSwitchRow` | `SwitchButton` + 双标签 | 所有开关行 |
| `ConnectionStatusRow` | 8dp 圆点 + `labelSmall` | 连接状态：绿=已连接、黄=连接中、灰=未连接 |
| `BatterySummary` / `BatteryColumn` / `BatteryBar` | `Row` + 百分比 + 3dp 迷你条 | 主页电量三列 |
| `batteryColor(percent)` | 颜色映射函数 | 电量语义色统一出口 |

> 新增页面优先复用以上封装；只有布局差异明显时才在页面内写一次性组合。

---

## 6. 页面结构与导航

### 6.1 导航图

```
AppScaffold (TimeText 弧形时间)
└── SwipeDismissableNavHost(startDestination = "home")
    ├── home        HomeScreen        设备卡 + 电量 + 6 个功能入口
    ├── noise       NoiseControlScreen 降噪模式 3 选 1 + 等级 4 选 1
    ├── equalizer   EqualizerScreen    音效 6 预设
    ├── gestures    GesturesScreen     双击/三击/长按 → 动作
    ├── info        DeviceInfoScreen   型号/固件/地址 + 双设备/入耳检测 + 恢复出厂
    ├── scan        ScanScreen         扫描/连接
    └── about       AboutScreen        版本、设计语言、演示模式、免责声明
```
- 路由常量：`BudsRoutes`（`BudsWearApp.kt`）。
- 进入：`navController.navigate(BudsRoutes.NOISE)`。
- 返回：Wear 标准**右滑返回** + 系统返回键，由 `SwipeDismissableNavHost` 自动处理；页面内不画返回箭头。
- 连接成功：从 `scan` 自动 `popBackStack()` 回 `home`。
- 单 Activity：`MainActivity` 只有一个 `setContent`，`BudsViewModel` 在 Activity 级共享（`viewModel(factory = ...)`）。

### 6.2 各页面骨架与内容

| 页面 | 骨架 | 内容顺序（从上到下） |
|---|---|---|
| Home | `ScreenScaffold` + `ScalingLazyColumn` | 设备卡（名称+状态点+电量三列）→ 6 功能行（降噪/音效/手势/游戏模式/查找耳机/设备信息）→ 设置入口 |
| Noise | 同上 | 3 模式 `TextToggleButton` 横排（关闭/通透/降噪）→ 选中「降噪」时显示「降噪等级」4 选 1 → 说明文字（`bodySmall`） |
| Equalizer | 同上 | 6 预设单选行 → 底部说明「不同耳机型号支持的音效可能不同」 |
| Gestures | 同上 | 双击 / 三击 / 长按 三行，右侧显示当前动作 → 点击弹 `AlertDialog` 选择动作 |
| Scan | 同上 + 常亮 | 权限未授予时：权限说明 + 「授予蓝牙权限」→ 扫描中：进度圈 + 「正在扫描…」→ 结果行（名称+信号）→ 「重新扫描」 |
| Info | 同上 | `TitleCard`（型号/固件/地址）→ 双设备连接、入耳检测 `SwitchButton` → 恢复出厂（错误色，二次确认）→ 「关于本应用」 |
| About | 同上 | 应用名/版本 → 设计语言说明 → 演示模式 `SwitchButton` → 免责声明 |

### 6.3 状态与反馈
- 连接状态三态：`Disconnected` / `Connecting` / `Connected`，体现在设备卡状态点 + 文案。
- 所有控制指令**立即写回耳机**；失败提示走 `BudsViewModel.messages`（`SharedFlow<Int>`）→ `MainActivity` 统一 `Toast`，不在圆形屏底部用 Snackbar（会遮挡内容）。
- 演示模式：`AboutScreen` 中开关，切换后 ViewModel 重建仓库（`DemoBudsRepository` ↔ `BleBudsRepository`），数据立即变化。

---

## 7. 圆形表盘适配规则

Wear OS 4+ 以圆形表盘为主（1.2–1.4 英寸，约 384–454px 直径），设计稿按 **450×450** 输出。

### 7.1 安全区
- **水平安全宽度**：以表盘直径 450 计，内容最大宽度约 **330px（≈ 73%）**，即左右各留 13.5% 边距；列表行 `fillMaxWidth()` 由 `ScalingLazyColumn` 默认 10dp 水平边距保证。
- **顶部**：弧形 `TimeText` 占顶部约 **0–60px**（半径方向），页面标题从时间下方开始。
- **底部**：最底部约 **60px** 为圆形收窄区，不放可点击控件/长文字；设计稿中该区域仅放「设计稿预览」标注。
- **首尾项**：`ScalingLazyColumn` 的 `contentPadding` 由 `ScreenScaffold` 提供，首项需让出时间区域，末项需让出边缘按钮区域。

### 7.2 文字规则
| 规则 | 说明 |
|---|---|
| 单行 ≤ 10 汉字 | 超出用 `secondaryLabel` 或换行；Ellipsis 兜底 |
| 不贴边 | 文字与圆边至少 12dp 视觉间距；标题居中 |
| 关键词居中 | 状态、数值、选中项放在屏幕水平中线附近 |
| 长说明降级 | 页面底部说明用 `bodySmall`，最多 2 行，居中 |
| 数字右对齐 | 列表行右侧状态值（如「降噪 · 智能」）右对齐，避免长度变化导致跳动 |

### 7.3 控件规则
- 触控目标：列表行 ≥ 48dp 高（默认 52dp），`TextToggleButton` 横排时每个 ≥ 64dp 宽 × 52dp 高（默认尺寸 52dp）。
- 一行最多 3 个 `TextToggleButton`（3 选项 72–94dp/个）；4 个等级用 2×2 网格或单行 4 个小按钮（本版本单行 4 个，字号 `labelSmall`）。
- 滚动：只用 `ScalingLazyColumn`，表冠（rotary）自动支持；**不要**自己写滚动或 `verticalScroll`。
- 边缘按钮（`edgeButton`）：本版本不启用底部圆形 edge button；「重新扫描」作为列表最后一个 `Button` 放在滚动区内。
- 对话框：宽度 ≤ 300dp，选项行用 `Button`，最多 8 项；超长用滚动列表。
- 图标：22dp 左右，放在 `Button.icon` 槽位；颜色默认 `onSurfaceVariant`，选中/激活态用 `primary`。

### 7.4 设计稿检查清单（每张 mockup 必过）
1. 所有文字/图标在表盘圆内（与圆边 ≥ 8px）；
2. 顶部弧形时间不压标题；
3. 底部标注「设计稿预览 · 非真机截图」完整、不越界、不遮挡内容；
4. 强调色只出现在选中态/主操作/关键状态（primary 紫，无自定义色）；
5. 行高与行距一致，左右对齐，无半行被裁切。

---

## 8. 色彩语义使用规范（Wear M3 官方默认 token，非自定义配色）

### 8.1 主色（Primary）
- 用于**当前选中项**：降噪模式/降噪等级/EQ 预设选中行、`TextToggleButton` 选中态（`primary` 填充 + `onPrimary` 文字，或 `primaryContainer` + `onPrimaryContainer`）。
- 用于**主操作**：连接、确认、重新扫描等主 CTA；连接、查找耳机等按钮的激活图标。
- 用于**节奏点**：`ListHeader` 小节标题、已连接状态点、手势当前动作值、电量正常数值。
- 用量：单屏 primary 面积建议 ≤ 15%，避免整屏高亮；不使用自定义品牌色作强调。

### 8.2 容器色（Container）
- 选中行容器：`primaryContainer` 底色 + `primary` 1dp 描边 + `onPrimaryContainer` 文字（EQ 选中行、降噪等级选中）。
- 次级强调/分组：`secondaryContainer`（预留）；说明卡/普通行用 `surfaceContainerLow`/`surfaceContainer`，**不用强调色铺底**。
- 状态点/图标底板：`surfaceContainerHigh`；选中态图标底板可用 `primaryContainer`。

### 8.3 描边（Outline）
- 选中态容器：`border = BorderStroke(1.dp, primary)` + `primaryContainer` 底色。
- 未选中态：`outlineVariant` 细描边；分隔线 `outlineVariant` 1dp。
- 描边宽度统一 1dp（选中/普通），不使用 2dp 以上粗描边；不使用高饱和强调色做分割线。

### 8.4 状态色（Connection / Battery / Error）
| 状态 | 角色 / HEX | 依据 |
|---|---|---|
| 已连接 | `primary` `#E9DDFF` | 官方无 success 角色；primary 表达正常/激活 |
| 连接中 | `secondary` `#DEE0FF` | 低强调的进行中状态，不抢主色 |
| 未连接 | `onSurfaceVariant` `#CAC4D0` | 中性，非错误（未连接是常态） |
| 连接失败 | `error` `#F2B8B5` | 错误提示，短期出现，配合 Toast |
| 电量正常 / 偏低 / 低电 | `primary` / `secondary` / `error` | 与 `batteryColor()` 实现一致（≥60 / 30–59 / <30） |

规范：
- 状态点 8dp 圆形 + `labelSmall` 文案同行，文案颜色用 `onSurfaceVariant`，状态信息不依赖颜色单独传达（色盲友好）。
- 已连接与电量正常同为 primary 时，用「状态点」与「数字/进度条」的形状差异区分，不额外引入绿色。
- 错误只在低电量/失败/危险操作使用，普通说明文字不得用 error。

### 8.5 禁止用法
- ❌ 引入非 M3 色值：自定义品牌色、成功绿、品牌蓝等一律不使用。
- ❌ primary 大面积背景或长段落正文（> 2 行）。
- ❌ 用 primary 同时标识「已连接」与「错误」等互斥语义。
- ❌ 在未选中态使用 primary 图标/文字制造伪选中。
- ❌ 用 error 红做普通说明文字。

### 8.6 色值来源
- Wear Material 3 1.6.2 官方默认 token（`ColorTokens`/`PaletteTokens`，见 §2）；不使用任何自定义配色或品牌色。
- 应用图标：背景 `#1C1B1F`、前景 `#D0BCFF`（`res/values/colors.xml` / `ic_launcher_foreground.xml`）。
- 与 realme Link 品牌色无关；`公开资料` 中提取的 realme 品牌色仅作背景资料，不进入本应用 UI。

---

## 9. 动效与反馈（简）

- 页面切换：`SwipeDismissableNavHost` 默认滑动过渡，不自定义转场。
- 选中态切换：`TextToggleButton` 默认涟漪 + 容器色渐变，不额外加动画。
- 扫描中：`CircularProgressIndicator` 持续旋转；连接成功自动返回主页，无需手动点「完成」。
- 触觉反馈：查找耳机、恢复出厂等破坏性/重要操作可加 `VIBRATE`（清单已声明），由实现层决定，视觉稿不体现。

---

## 10. 与设计稿的对应关系

| 设计稿 | 对应页面 | 展示要点 |
|---|---|---|
| `docs/mockups/home.png` | Home | 标题「耳机控制器」、弧形时间 09:41、设备卡「realme Buds Air 6 Pro」、电量 82%/78%/91%、6 功能行、游戏模式开关 |
| `docs/mockups/noise.png` | Noise | 关闭/通透/降噪三选一（降噪选中）、等级 智能/深度/中等/轻度（智能选中） |
| `docs/mockups/equalizer.png` | Equalizer | 6 预设，低音增强选中 + EQ 曲线示意 |
| `docs/mockups/gesture.png` | Gestures | 双击/三击/长按 + 当前动作（播放/暂停、上一首、切换降噪） |
| `docs/mockups/scan.png` | Scan | 扫描转圈、2 台设备列表、重新扫描 |

> 所有图片由 `docs/mockups/render_mockups.py`（Python + Pillow）生成，可重复渲染；图中标注「**设计稿预览 · 非真机截图**」，不代表最终真机像素级效果。
