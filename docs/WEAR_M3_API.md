# Wear Compose Material3 (1.6.2) API 速查 — 已验证源码

来源：Wear Compose Material3 1.6.2 源码（Google Maven）
（来自 Google Maven 的 sources jar；**本版本没有 Chip 组件**）

## 依赖坐标（Gradle）
- `androidx.wear.compose:compose-material3:1.6.2`
- `androidx.wear.compose:compose-foundation:1.6.2`（ScalingLazyColumn/TransformingLazyColumn/CurvedText）
- `androidx.wear.compose:compose-navigation:1.6.2`（SwipeDismissableNavHost）
- Compose 版本随 Wear Compose 传递；`androidx.activity:activity-compose`
- `androidx.navigation:navigation-compose`（由 wear navigation 传递，建议显式声明）

## 主题
```kotlin
MaterialTheme(
    colorScheme: ColorScheme = ...,   // 构造参数：primary, primaryDim, primaryContainer, onPrimary, ...
    typography: Typography = ...,
    shapes: Shapes = ...,
    motionScheme: MotionScheme = ...,
    content: @Composable () -> Unit
)
// 取用：MaterialTheme.colorScheme / typography / shapes
```
`ColorScheme` 构造参数（全部有默认值）：
`primary, primaryDim, primaryContainer, onPrimary, onPrimaryContainer, secondary, secondaryDim, secondaryContainer, onSecondary, onSecondaryContainer, tertiary, tertiaryDim, tertiaryContainer, onTertiary, onTertiaryContainer, surfaceContainerLow, surfaceContainer, surfaceContainerHigh, onSurface, onSurfaceVariant, outline, outlineVariant, background, onBackground, error, errorDim, errorContainer, onError, onErrorContainer`
（没有 `surface` / `surfaceVariant` 角色）

`Typography` 角色：
`arcLarge/arcMedium/arcSmall`（CurvedTextStyle）、`displayLarge/Medium/Small`、`titleLarge/Medium/Small`、`labelLarge/Medium/Small`、`bodyLarge/Medium/Small`、`bodyExtraSmall`、`numeralExtraLarge/Large/Medium/Small/ExtraSmall`
构造：`Typography()`（全部有默认）；`MaterialTheme.typography.titleLarge` 等。

## 脚手架
```kotlin
AppScaffold(
    modifier: Modifier = Modifier,
    timeText: @Composable () -> Unit = { TimeText() },
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable BoxScope.() -> Unit
)

ScreenScaffold(
    scrollState: ScalingLazyListState,          // 或 TransformingLazyColumnState / LazyListState / ScrollState
    edgeButton: @Composable BoxScope.() -> Unit, // 可选重载（不带 edgeButton 的版本也有）
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ScreenScaffoldDefaults.contentPadding,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = { ScrollIndicator(scrollState) },
    edgeButtonSpacing: Dp = ScreenScaffoldDefaults.EdgeButtonSpacing,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    content: @Composable BoxScope.(PaddingValues) -> Unit
)
```
典型用法：
```kotlin
AppScaffold {   // 或直接在 Activity 里
  val state = rememberScalingLazyListState()
  ScreenScaffold(scrollState = state) { contentPadding ->
    ScalingLazyColumn(state = state, contentPadding = contentPadding) { /* items */ }
  }
}
```

## 列表
```kotlin
ScalingLazyColumn(modifier, state, contentPadding, reverseLayout, verticalArrangement, horizontalAlignment,
    flingBehavior, userScrollEnabled, scalingParams, anchorType, autoCentering, content: ScalingLazyListScope.() -> Unit)
// item { }, items(count) { }, items(list) { }
```
`ScalingLazyListScope` 支持 `item {}`/`items()`；表冠滚动内置。
`TransformingLazyColumn` 也在 foundation 中（大屏/新版滚动）。

## 组件签名（节选）
```kotlin
Button(onClick, modifier, onLongClick, onLongClickLabel, secondaryLabel: (@Composable RowScope.() -> Unit)?, 
       icon: (@Composable BoxScope.() -> Unit)?, enabled, shape, colors, border, contentPadding, interactionSource,
       transformation, label: @Composable RowScope.() -> Unit)          // 列表行首选
Button(onClick, modifier, ..., content: @Composable RowScope.() -> Unit) // 普通按钮
FilledTonalButton / OutlinedButton / ChildButton / TextButton(同上)

Card(onClick, modifier, onLongClick, onLongClickLabel, enabled, shape, colors, border, contentPadding,
     interactionSource, transformation, content: @Composable ColumnScope.() -> Unit)
TitleCard(onClick, title: @Composable RowScope.() -> Unit, modifier, onLongClick, onLongClickLabel,
          time: (@Composable () -> Unit)?, subtitle: (@Composable ColumnScope.() -> Unit)?, enabled, shape,
          colors, border, contentPadding, interactionSource, transformation, content: (@Composable () -> Unit)?)
AppCard(onClick, appName, title, modifier, ..., appImage, time, content)
OutlinedCard(...)

TextToggleButton(checked, onCheckedChange, modifier, enabled, colors, interactionSource, shapes, border,
                 content: @Composable BoxScope.() -> Unit)         // 单选/开关式文字按钮
SwitchButton(checked, onCheckedChange, modifier, enabled, shape, colors, contentPadding, interactionSource,
             transformation, icon: (@Composable BoxScope.() -> Unit)?, 
             secondaryLabel: (@Composable RowScope.() -> Unit)?, label: @Composable RowScope.() -> Unit)
SplitSwitchButton(checked, onCheckedChange, toggleContentDescription, onContainerClick, ...)
IconButton(onClick, modifier, enabled, shapes, colors, interactionSource, content: @Composable BoxScope.() -> Unit)
IconToggleButton(checked, onCheckedChange, ...)
CheckboxButton / RadioButton / SplitCheckboxButton / SplitRadioButton

Slider(value: Float, onValueChange, steps, modifier, decreaseIcon, increaseIcon, enabled, valueRange,
       segmented, shape, colors)
Slider(value: Int, onValueChange, valueProgression: IntProgression, ...)

Picker(state: PickerState, contentDescription: (() -> String)?, modifier, readOnly, readOnlyLabel,
       onSelected, verticalSpacing, gradientRatio, gradientColor, userScrollEnabled, rotaryScrollableBehavior,
       option: @Composable PickerScope.(index: Int) -> Unit)
rememberPickerState(initialNumberOfOptions: Int, initiallySelectedIndex: Int = 0, shouldRepeatOptions: Boolean = true)
PickerGroup / PickerGroupItem

ListHeader(modifier, backgroundColor, contentColor, contentPadding, transformation, content: RowScope.() -> Unit)
ListSubHeader(modifier, backgroundColor, contentColor, contentPadding, transformation, icon, label: RowScope.() -> Unit)

ButtonGroup(modifier, spacing, expansionWidth, contentPadding, verticalAlignment, transformation,
            content: @Composable ButtonGroupScope.() -> Unit)

TimeText(modifier, curvedModifier, maxSweepAngle, backgroundColor, timeSource, contentPadding, content)
ScrollIndicator(...)
CircularProgressIndicator(...)
LinearProgressIndicator(...)
LevelIndicator / StepperLevelIndicator
ArcProgressIndicator(...)
AlertDialog(...) / ConfirmationDialog(...) / Dialog(...) / OpenOnPhoneDialog(...)
SwipeToDismissBox(onDismissed / state, modifier, backgroundScrimColor, contentScrimColor, ...)
```

## 导航（compose-navigation 1.6.2）
```kotlin
val navController = rememberSwipeDismissableNavController()
SwipeDismissableNavHost(navController, startDestination = "home") {
    composable("home") { ... }
}
// 返回：navController.popBackStack()；跳转：navController.navigate("noise")
```
右滑返回由 `SwipeDismissableNavHost` 自动处理。

## 注意
- 没有 `Chip` / `ToggleChip` / `SplitToggleChip` / `SegmentedButton`（本版本用 Button/TextToggleButton/ButtonGroup 代替）。
- 没有 `Surface`? —— 本版本有 `Surface.kt`（内部使用较多），业务 UI 优先用 Card/Button。
- `Icon` 来自 `androidx.wear.compose.material3.Icon` 或 `androidx.compose.material.icons`；图标依赖 `androidx.compose.material:material-icons-core`（若未显式加入，可只用内置 `Icons.Filled.*` 需该依赖；优先用 `androidx.compose.material.icons.Icons` + 自行画 vector drawable）。
