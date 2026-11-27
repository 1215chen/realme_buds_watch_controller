# 设计规范 · 耳机控制器

> 适用范围：`com.cyhao.realmebuds`（耳机控制器 / Buds Wear Controller）
> 设计基线：**Wear Compose Material 3 1.6.2**
> 视觉来源：用户提供的 3 页 Wear OS 设计稿（主页已连接/未连接、设置页），实现时修正了图标、间距与时间显示
> 配色：**Material Design 3 蓝色系**（蓝色 tonal palette 的语义角色，不使用任何品牌定制色）
> 实现源：`ui/theme/Theme.kt`、`ui/components/WearComponents.kt`、`ui/screens/*`
> 真机截图：`docs/screenshots/`

---

## 1. 设计原则

1. **深色优先**：蓝色系 dark ColorScheme（`background #101418`），适配 AMOLED 并保持系统一致性。
2. **一屏一件事**：主页聚焦「设备状态 + 快捷控制」，其余页面各自只承载一个任务。
3. **主色只做强调**：`primary (#A8C7FA)` 用于选中态、图标、电量进度、关键 CTA；大面积背景使用 `secondaryContainer (#3F4759)`。
4. **圆形表盘安全区**：列表左右留 10dp 边距，顶部额外 20dp 避让 `TimeText`，内容不贴边。
5. **M3 原生控件优先**：`ScreenScaffold` + `ScalingLazyColumn` + `TimeText`（真实弧形时间，不用自绘时间）；单选使用自定义分段选择器（M3 1.6.2 无 SegmentedButton）。

---

## 2. 色彩 Token（M3 蓝色系）

> 全部通过 `MaterialTheme.colorScheme.<角色>` 使用；`Theme.kt` 中显式定义一套蓝色系 `ColorScheme`（仍是 M3 语义角色，非品牌色）。

| 角色 | HEX | 用途 |
|---|---|---|
| primary | `#A8C7FA` | 选中块、图标、电量进度、标题强调 |
| onPrimary | `#062E6F` | primary 上的文字 |
| primaryContainer | `#0842A0` | 启动图标背景、深色强调容器 |
| onPrimaryContainer | `#D3E3FD` | primaryContainer 上的文字 |
| primaryDim | `#7FA9E8` | 低亮主色变体 |
| secondary | `#BFC6DC` | 次强调、偏低电量 |
| secondaryContainer | `#3F4759` | 设备卡、列表行、开关行底色 |
| onSecondaryContainer | `#DBE2F9` | 卡片/行内文字与图标 |
| tertiary | `#9FCAFF` | 第三层强调（备用） |
| background | `#101418` | 全局背景 |
| onBackground | `#E1E7F0` | 背景主文字 |
| surfaceContainerLow | `#1A222C` | 低层容器 |
| surfaceContainer | `#222B37` | 标准容器 |
| surfaceContainerHigh | `#2C3847` | 浮起容器 |
| onSurface | `#E1E7F0` | 表面文字 |
| onSurfaceVariant | `#C0CADC` | 副标签、说明文字 |
| outline | `#8E9099` | 强描边 |
| outlineVariant | `#44464F` | 弱分割线 |
| error | `#F2B8B5` | 低电量/错误/危险操作 |

**语义状态色**：
- 已连接圆点：`ConnectedGreen #6DD58C`（状态语义色，非品牌色；M3 无 success 角色）
- 未连接圆点：`error`
- 连接中：`primary`
- 电量：≥60% 用 `primary`，30–59% 用 `secondary`，<30% 用 `error`，未知显示 `-`

**启动图标**：背景 `#0842A0`，前景 `#A8C7FA`（`ic_launcher_foreground.xml` / `colors.xml`）。

---

## 3. 字体与字号

使用 Wear M3 默认 `Typography()`，不自定义字阶：

| 场景 | 角色 | 说明 |
|---|---|---|
| 页面标题 | `titleLarge` | 每页唯一、居中；如「耳机控制器」「设置」 |
| 设备名 | `titleSmall` | 单行 Ellipsis，避免挤占状态/电量区域 |
| 列表行主标签 | `labelLarge` | 自定义行内文字 |
| 列表行副标签 | `labelSmall` | `onSecondaryContainer` 75% 透明度 |
| 电量环中心 | `labelSmall`（标签）+ `labelMedium`（百分比） | L / R / Case |
| 小节标题 | `titleSmall` | 居中，如「更多」 |
| 弧形时间 | `arcMedium` | `TimeText` 默认，不手写时间 |

---

## 4. 布局与组件

### 4.1 主页（已连接）

```
[TimeText 弧形时间]
标题「耳机控制器」（居中）
┌ 设备卡（secondaryContainer，圆角 26dp，内边距 18dp）
│  [earbuds 图标] 设备名
│  ● 已连接
│  ◯L   ◯R   ◯Case      ← 三个等大圆环，8dp 间距
└
┌ 降噪分段选择器（高 52dp，圆角 50）
│  降噪 | 关闭 | 通透    ← 选中块平滑滑动（M3 primary）
└
[icon] EQ模式 / 当前音效        >
[      游戏模式          开关 ]
[icon] 按键设置                 >
[icon] 设备信息 / 固件版本      >
（查找耳机，硬件支持时）
「更多」（小节标题）
[      双设备连接        开关 ]
[icon] 断开连接
[icon] 设置 / v1.1.1           >
```

### 4.2 主页（未连接）

设备卡显示 `-` 电量与红色「未连接」+「搜索并连接」按钮（primary 填充、放大镜图标），下方保留「设置」入口。

### 4.3 设置页

```
标题「设置」
圆形图标底板（84dp，secondaryContainer）+ earbuds 图标
应用名（titleLarge）
版本号（labelSmall）
手表型号：SM-L3150
耳机型号：realme Buds T200 / 未连接
[演示模式 开关]
底部声明文案（labelSmall，居中）
```

### 4.4 组件规格

| 组件 | 规格 |
|---|---|
| 设备卡 | `RoundedCornerShape(26dp)`、`secondaryContainer`、padding 18dp |
| 电量环 `BatteryRing` | 直径 54dp，描边 4dp，圆角端帽；轨道 `onSecondaryContainer` 30%，进度 `primary`；三等分槽位 + 8dp 间距 |
| 导航行 `WearNavRow` | 高约 52dp，`RoundedCornerShape(50)`，`secondaryContainer`；图标 22dp（`primary`）+ 14dp 间距 + 主/副标签 + 右侧 18dp 箭头 |
| 开关行 | Wear M3 `SwitchButton`（填宽） |
| 分段选择器 | 高 52dp，容器 `secondaryContainer`，选中块 `primary` + `RoundedCornerShape(50)`，内缩 4dp |
| 页面标题 | `ListHeader` + `titleLarge`，居中 |

---

## 5. 动效

| 动效 | 参数 |
|---|---|
| 降噪选中块滑动 | `Animatable` + `spring(dampingRatio = 0.60, stiffness = 500)`，带轻微回弹 |
| 滑动中的形体变化 | 按速度横向拉伸 ≤ 8%、纵向压缩 ≤ 4%（`graphicsLayer`） |
| 文字颜色过渡 | `animateColorAsState` + `tween(200ms)` |
| 触觉反馈 | 切换时 `HapticFeedbackType.TextHandleMove` |
| 时间显示 | `TimeText` 随列表滚动收放（Wear M3 原生） |

> 不使用真正的背景模糊/毛玻璃，仅保留滑动动画本身。

---

## 6. 间距与安全区

- 列表水平内容边距：10dp；
- 顶部在 `ScreenScaffold` 默认 padding 基础上 +20dp，避免与弧形时间重叠；
- 卡片/行内边距：水平 18dp、垂直 12–16dp；
- 行间距：`ScalingLazyColumn` 默认间距（约 4dp）+ 组件自身高度；
- 圆形屏幕安全区：所有内容距屏幕边缘 ≥ 8dp。

---

## 7. 对比度核对（WCAG 2.1）

| 前景 / 背景 | 对比度 | 结论 |
|---|---|---|
| `#E1E7F0` / `#101418` | 15.4:1 | AAA |
| `#DBE2F9` / `#3F4759` | 7.9:1 | AAA |
| `#A8C7FA` / `#3F4759` | 5.9:1 | AA（图标/强调可用） |
| `#062E6F` / `#A8C7FA` | 7.4:1 | AAA |
| `#F2B8B5` / `#3F4759` | 6.7:1 | AA |

---

## 8. 图标

- 全部使用 Material Symbols 风格的 24dp 矢量（`res/drawable/ic_*.xml`）：
  `ic_buds`（耳机）、`ic_tune`（EQ）、`ic_grid`（按键设置）、`ic_more`（设备信息）、`ic_menu`（设置）、
  `ic_bluetooth_off`（断开连接）、`ic_find`（查找）、`ic_search`（搜索连接）、`ic_chevron_right`（箭头）；
- 图标不硬编码颜色，统一由 `Icon(tint = ...)` 按语义着色；
- 启动图标为自适应图标（蓝色背景 + 耳机描边前景），并提供 PNG 预览（`docs/screenshots/app-icon-*.png`）。
