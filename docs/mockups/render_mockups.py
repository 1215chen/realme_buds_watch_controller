#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
render_mockups.py — 真我耳机手表控制台 · Wear M3 圆形表盘设计稿生成器

生成 5 张 450x450 PNG（docs/mockups/）：
    home.png      主页（弧形时间 + 设备卡 + 电量三列 + 6 个功能行）
    noise.png     降噪控制（关闭/通透/降噪 + 降噪等级）
    equalizer.png 音效（6 个预设）
    gesture.png   触控手势（双击/三击/长按）
    scan.png      扫描设备（扫描中 + 设备列表）

设计基线：docs/SPEC.md、docs/DESIGN.md、ui/theme/Theme.kt
风格：Wear Material 3 官方默认 token（background #000000 / primary #E9DDFF，无自定义品牌色）；
      Wear M3 圆角按钮行；3 倍超采样抗锯齿。
每张图底部标注「设计稿预览 · 非真机截图」。

依赖：Python 3.9+、Pillow；中文字体 C:\\Windows\\Fonts\\msyh.ttc / msyhbd.ttc
运行：python render_mockups.py
"""

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

# ---------------------------------------------------------------- 基础参数
OUT_DIR = Path(__file__).resolve().parent
SIZE = 450          # 设计稿尺寸（正方形，圆形表盘外接）
SS = 3              # 超采样倍数（1350x1350 绘制后缩放到 450）
CX = CY = 225       # 表盘圆心
R = 225             # 表盘半径

FONT_REG = r"C:\Windows\Fonts\msyh.ttc"
FONT_BOLD = r"C:\Windows\Fonts\msyhbd.ttc"

# ---------------------------------------------------------------- 颜色 Token（Wear Material 3 官方默认 token，非自定义配色）
# 说明：全部取自 Wear M3 1.7.1 官方 ColorScheme 默认值（ColorTokens/PaletteTokens），
#       不定义任何品牌色；设计稿与 Theme.kt 真机默认主题一致。
BACKGROUND       = (0, 0, 0)       # #000000 background
ON_BACKGROUND    = (255, 255, 255) # #FFFFFF onBackground
PRIMARY          = (233, 221, 255) # #E9DDFF primary
PRIMARY_DIM      = (208, 188, 255) # #D0BCFF primaryDim
ON_PRIMARY       = (33, 15, 72)    # #210F48 onPrimary
PRIMARY_CONTAINER= (77, 61, 118)   # #4D3D76 primaryContainer
ON_PRIMARY_CONT  = (246, 237, 255) # #F6EDFF onPrimaryContainer
SECONDARY        = (222, 224, 255) # #DEE0FF secondary
ON_SECONDARY     = (12, 22, 73)    # #0C1649 onSecondary
SECONDARY_CONT   = (58, 67, 118)   # #3A4376 secondaryContainer
ON_SECONDARY_CONT= (240, 239, 255) # #F0EFFF onSecondaryContainer
TERTIARY         = (255, 220, 194) # #FFDCC2 tertiary
ON_TERTIARY      = (46, 21, 0)     # #2E1500 onTertiary
TERTIARY_CONT    = (108, 58, 3)    # #6C3A03 tertiaryContainer
ON_TERTIARY_CONT = (255, 238, 226) # #FFEEE2 onTertiaryContainer
SURFACE_LOW      = (39, 36, 48)    # #272430 surfaceContainerLow
SURFACE          = (51, 46, 60)    # #332E3C surfaceContainer
SURFACE_HIGH     = (73, 68, 83)    # #494453 surfaceContainerHigh
ON_SURFACE       = (246, 237, 255) # #F6EDFF onSurface
ON_SURFACE_VAR   = (202, 196, 208) # #CAC4D0 onSurfaceVariant
OUTLINE          = (148, 143, 154) # #948F9A outline
OUTLINE_VAR      = (97, 93, 103)   # #615D67 outlineVariant
ERROR            = (242, 184, 181) # #F2B8B5 error
ON_ERROR         = (65, 14, 11)    # #410E0B onError
ERROR_CONTAINER  = (140, 29, 24)   # #8C1D18 errorContainer
ON_ERROR_CONT    = (252, 238, 238) # #FCEEEE onErrorContainer
BEZEL            = OUTLINE_VAR     # 表圈：outlineVariant
CARD_BORDER      = OUTLINE_VAR     # 卡片/列表行描边
CAPTION          = ON_SURFACE_VAR  # 底部标注（圆内可读）

_font_cache = {}


def _font(size, bold=False):
    """按 450 空间字号取字体（内部按 SS 放大）。"""
    key = (round(size * SS), bold)
    if key not in _font_cache:
        path = FONT_BOLD if bold else FONT_REG
        _font_cache[key] = ImageFont.truetype(path, int(round(size * SS)), index=0)
    return _font_cache[key]


# ---------------------------------------------------------------- 绘制封装
class Canvas:
    """坐标/字号使用 450x450 设计空间，内部统一乘以 SS 超采样。"""

    def __init__(self):
        self.img = Image.new("RGB", (SIZE * SS, SIZE * SS), BACKGROUND)
        self.d = ImageDraw.Draw(self.img)

    # -- 基础图元 -------------------------------------------------
    def circle(self, cx, cy, r, fill=None, outline=None, width=1.0):
        box = ((cx - r) * SS, (cy - r) * SS, (cx + r) * SS, (cy + r) * SS)
        if fill is None and outline is None:
            return
        self.d.ellipse(box, fill=fill, outline=outline,
                       width=max(1, int(round(width * SS))))

    def ring(self, cx, cy, r, color, width=1.0):
        self.circle(cx, cy, r, outline=color, width=width)

    def round_rect(self, x0, y0, x1, y1, radius, fill=None, outline=None, width=1.0):
        box = (x0 * SS, y0 * SS, x1 * SS, y1 * SS)
        self.d.rounded_rectangle(box, radius=radius * SS, fill=fill,
                                  outline=outline,
                                  width=max(1, int(round(width * SS))))

    def line(self, pts, color, width=1.0):
        self.d.line([(x * SS, y * SS) for x, y in pts], fill=color,
                    width=max(1, int(round(width * SS))), joint="curve")

    def arc(self, cx, cy, r, start, end, color, width=1.0):
        box = ((cx - r) * SS, (cy - r) * SS, (cx + r) * SS, (cy + r) * SS)
        self.d.arc(box, start=start, end=end, fill=color,
                   width=max(1, int(round(width * SS))))

    def polygon(self, pts, fill):
        self.d.polygon([(x * SS, y * SS) for x, y in pts], fill=fill)

    def text(self, x, y, s, size, color, bold=False, anchor="mm"):
        self.d.text((x * SS, y * SS), s, font=_font(size, bold), fill=color, anchor=anchor)

    def text_len(self, s, size, bold=False):
        """返回 450 设计空间下的文本宽度。"""
        return self.d.textlength(s, font=_font(size, bold)) / SS

    # -- 弧形时间（顶部） -----------------------------------------
    def curved_text(self, cx, cy, radius, text, size, color, bold=False, tracking=0.06):
        f = _font(size, bold)
        widths = [self.d.textlength(ch, font=f) / SS for ch in text]
        track = tracking * size
        total_w = sum(widths) + track * (len(text) - 1)
        total_angle = total_w / radius          # 弧度
        angle = -total_angle / 2.0
        for ch, w in zip(text, widths):
            theta = angle + (w / 2.0) / radius
            px = cx + radius * math.sin(theta)
            py = cy - radius * math.cos(theta)
            pad = int(6 * SS)
            tw = int(math.ceil(f.getlength(ch))) + 2 * pad
            th = int(size * SS * 2.0)
            tile = Image.new("RGBA", (tw, th), (0, 0, 0, 0))
            td = ImageDraw.Draw(tile)
            td.text((tw / 2, th / 2), ch, font=f, fill=color + (255,), anchor="mm")
            rot = tile.rotate(-math.degrees(theta), resample=Image.BICUBIC, expand=True)
            self.img.paste(rot,
                           (int(round(px * SS - rot.width / 2)),
                            int(round(py * SS - rot.height / 2))), rot)
            angle += (w + track) / radius


# ---------------------------------------------------------------- 公共装饰
def draw_chrome(c, time_radius=176):
    """表圈 + 顶部弧形时间 + 底部「设计稿预览」标注。"""
    # 表圈（双环制造轻微立体感）
    c.ring(CX, CY, R - 2.5, BEZEL, 2.0)
    c.ring(CX, CY, R - 9, SURFACE_HIGH, 4.0)
    # 弧形时间 09:41
    c.curved_text(CX, CY, time_radius, "09:41", 30, ON_BACKGROUND, bold=True)
    # 底部标注
    c.line([(178, 404), (272, 404)], OUTLINE_VAR, 0.9)
    c.text(CX, 415, "设计稿预览 · 非真机截图", 10.5, CAPTION)


def draw_chevron(c, x, y, color=ON_SURFACE_VAR):
    c.line([(x - 2.5, y - 5), (x + 2.5, y), (x - 2.5, y + 5)], color, 1.5)


def draw_switch(c, x_right, cy, on=True):
    """Wear M3 SwitchButton 示意：胶囊轨道 + 圆形滑块。"""
    w, h = 36.0, 20.0
    x0 = x_right - w
    track = PRIMARY if on else OUTLINE_VAR
    c.round_rect(x0, cy - h / 2, x_right, cy + h / 2, h / 2, fill=track)
    knob_r = 7.5
    knob_cx = x_right - knob_r - 2.5 if on else x0 + knob_r + 2.5
    c.circle(knob_cx, cy, knob_r, fill=ON_PRIMARY if on else ON_SURFACE_VAR)


def draw_battery_bar(c, cx, cy, width, pct, color):
    h = 3.6
    c.round_rect(cx - width / 2, cy - h / 2, cx + width / 2, cy + h / 2, h / 2,
                 fill=OUTLINE_VAR)
    if pct > 0:
        w = width * min(pct, 100) / 100.0
        c.round_rect(cx - width / 2, cy - h / 2, cx - width / 2 + w, cy + h / 2,
                     h / 2, fill=color)


def draw_signal_bars(c, x_right, cy, strength=4):
    """4 格信号强度（1-4）。"""
    heights = [4.0, 6.0, 8.0, 10.0]
    for i, h in enumerate(heights):
        x = x_right - (4 - i) * 5.0
        color = PRIMARY if i < strength else OUTLINE_VAR
        c.round_rect(x - 1.1, cy + 5 - h, x + 1.1, cy + 5, 1.1, fill=color)


# ---------------------------------------------------------------- 图标（22-28px 线性小图标）
def icon_badge(c, cx, cy, r=11.0, active=False):
    c.circle(cx, cy, r, fill=PRIMARY_CONTAINER if active else SURFACE_HIGH)


def glyph_noise(c, cx, cy, color=ON_SURFACE_VAR):
    # 声波 + 斜杠 = 降噪
    for rr in (4.0, 7.0):
        c.arc(cx - 4.5, cy, rr, 300, 60, color, 1.3)
    c.line([(cx - 6.5, cy + 6.5), (cx + 6.5, cy - 6.5)], color, 1.4)


def glyph_eq(c, cx, cy, color=ON_SURFACE_VAR):
    for dx, h in ((-4.5, 7.0), (0.0, 12.0), (4.5, 9.0)):
        c.line([(cx + dx, cy - h / 2), (cx + dx, cy + h / 2)], color, 2.2)


def glyph_touch(c, cx, cy, color=ON_SURFACE_VAR):
    # 指尖点按：中心点 + 上方涟漪
    c.circle(cx, cy + 2.5, 2.2, fill=color)
    for rr in (6.0, 9.5):
        c.arc(cx, cy + 2.5, rr, 200, 340, color, 1.2)


def glyph_game(c, cx, cy, color=ON_SURFACE_VAR):
    c.round_rect(cx - 8.5, cy - 5.0, cx + 8.5, cy + 5.0, 4.0, outline=color, width=1.4)
    c.circle(cx - 4.2, cy, 1.4, fill=color)
    c.circle(cx + 4.2, cy, 1.4, fill=color)


def glyph_find(c, cx, cy, color=ON_SURFACE_VAR):
    c.circle(cx - 1.6, cy - 1.6, 4.6, outline=color, width=1.5)
    c.line([(cx + 1.8, cy + 1.8), (cx + 6.5, cy + 6.5)], color, 1.7)


def glyph_info(c, cx, cy, color=ON_SURFACE_VAR):
    c.circle(cx, cy - 5.8, 1.4, fill=color)
    c.round_rect(cx - 1.3, cy - 1.8, cx + 1.3, cy + 6.2, 1.3, fill=color)


def glyph_buds(c, cx, cy, color=PRIMARY):
    # 双耳机：两个圆点 + 短柄
    for dx in (-3.2, 3.2):
        c.circle(cx + dx, cy - 1.2, 2.6, fill=color)
        c.line([(cx + dx, cy + 1.0), (cx + dx, cy + 4.8)], color, 2.0)


# ---------------------------------------------------------------- 页面：Home
def render_home():
    c = Canvas()
    draw_chrome(c)

    # 页面标题（ListHeader，颜色 primary，与 HomeScreen home_title 一致）
    c.text(CX, 86, "耳机控制器", 15.5, PRIMARY, bold=True)

    # 设备卡
    c.round_rect(56, 100, 394, 198, 24, fill=SURFACE, outline=CARD_BORDER, width=1.0)
    c.text(76, 118, "realme Buds Air 6 Pro", 15.0, ON_SURFACE, bold=True, anchor="lm")
    # 连接状态点 + 文案
    c.circle(306, 118, 3.6, fill=PRIMARY)
    c.text(315, 118, "已连接", 10.0, ON_SURFACE_VAR, anchor="lm")

    # 电量三列
    cols = ((113, 82), (225, 78), (337, 91))
    labels = ("左耳", "右耳", "充电盒")
    for (x, pct), label in zip(cols, labels):
        color = PRIMARY if pct >= 60 else (SECONDARY if pct >= 30 else ERROR)
        c.text(x, 156, f"{pct}%", 20.0, color, bold=True)
        draw_battery_bar(c, x, 172, 54, pct, color)
        c.text(x, 186, label, 9.5, ON_SURFACE_VAR)

    # 功能行（6 行）
    rows = [
        (glyph_noise, "降噪控制", "降噪 · 智能", PRIMARY, True),
        (glyph_eq,    "音效",     "低音增强",      ON_SURFACE_VAR, True),
        (glyph_touch, "触控手势", "已自定义",      ON_SURFACE_VAR, True),
        (glyph_game,  "游戏模式", None,            None, False),   # 开关
        (glyph_find,  "查找耳机", "播放提示音",    ON_SURFACE_VAR, False),
        (glyph_info,  "设备信息", "v1.0.3",        ON_SURFACE_VAR, True),
    ]
    top, row_h, gap = 207.0, 27.0, 4.0
    for i, (glyph, label, value, value_color, chevron) in enumerate(rows):
        cy = top + i * (row_h + gap) + row_h / 2
        c.round_rect(77, cy - row_h / 2, 373, cy + row_h / 2, row_h / 2,
                     fill=SURFACE, outline=CARD_BORDER, width=0.9)
        icon_badge(c, 99, cy, 10.5)
        glyph(c, 99, cy, PRIMARY if (i == 0 or i == 3) else ON_SURFACE_VAR)
        c.text(117, cy, label, 12.5, ON_SURFACE, bold=(i == 0), anchor="lm")
        if i == 3:
            draw_switch(c, 367, cy, on=True)
        elif value is not None:
            right = 356 if chevron else 368
            c.text(right, cy, value, 9.8, value_color, anchor="rm")
            if chevron:
                draw_chevron(c, 364, cy)
    c.img.resize((SIZE, SIZE), Image.LANCZOS).save(OUT_DIR / "home.png", optimize=True)


# ---------------------------------------------------------------- 页面：Noise
def render_noise():
    c = Canvas()
    draw_chrome(c)

    c.text(CX, 92, "降噪控制", 16.0, ON_SURFACE, bold=True)

    # 3 个模式（关闭 / 通透 / 降噪），降噪选中
    modes = [("关闭", False), ("通透", False), ("降噪", True)]
    x = 74.0
    for label, selected in modes:
        if selected:
            c.round_rect(x, 110, x + 94, 145, 17.5, fill=PRIMARY)
            c.text(x + 47, 127.5, label, 12.5, ON_PRIMARY, bold=True)
        else:
            c.round_rect(x, 110, x + 94, 145, 17.5, fill=SURFACE,
                         outline=OUTLINE_VAR, width=1.2)
            c.text(x + 47, 127.5, label, 12.5, ON_SURFACE_VAR)
        x += 103

    # 小节标题
    c.text(80, 165, "降噪等级", 10.5, ON_SURFACE_VAR, anchor="lm")

    # 4 个等级（智能 / 深度 / 中等 / 轻度），智能选中
    levels = [("智能", True), ("深度", False), ("中等", False), ("轻度", False)]
    x = 75.0
    for label, selected in levels:
        if selected:
            c.round_rect(x, 179, x + 69, 209, 15, fill=PRIMARY_CONTAINER,
                         outline=PRIMARY, width=1.4)
            c.text(x + 34.5, 194, label, 11.0, ON_PRIMARY_CONT, bold=True)
        else:
            c.round_rect(x, 179, x + 69, 209, 15, fill=SURFACE,
                         outline=OUTLINE_VAR, width=1.1)
            c.text(x + 34.5, 194, label, 11.0, ON_SURFACE_VAR)
        x += 77

    # 当前状态说明卡
    c.round_rect(66, 230, 384, 300, 20, fill=SURFACE_LOW, outline=CARD_BORDER, width=0.9)
    c.text(CX, 252, "当前：降噪 · 智能", 13.0, ON_SURFACE, bold=True)
    c.text(CX, 274, "主动抵消环境噪音，适合通勤", 10.5, ON_SURFACE_VAR)
    c.text(CX, 291, "佩戴状态会影响降噪效果", 9.5, ON_SURFACE_VAR)

    # 通透模式说明（SPEC 4.2：底部说明文字）
    c.round_rect(66, 312, 384, 350, 19, fill=SURFACE, outline=CARD_BORDER, width=0.9)
    c.text(CX, 331, "通透模式：听到环境声，适合面对面交谈", 10.5, ON_SURFACE_VAR)

    # 底部提示
    c.text(CX, 372, "提示：降噪等级越高，耳压感可能越明显", 9.5, BUTTON_HINT)

    c.img.resize((SIZE, SIZE), Image.LANCZOS).save(OUT_DIR / "noise.png", optimize=True)


BUTTON_HINT = (147, 143, 153)  # #938F99 outline


# ---------------------------------------------------------------- 页面：Equalizer
def render_equalizer():
    c = Canvas()
    draw_chrome(c)

    c.text(CX, 92, "音效", 16.0, ON_SURFACE, bold=True)

    presets = ["默认", "低音增强", "均衡", "高音增强", "人声", "动态"]
    top, row_h, gap = 106.0, 29.0, 4.0
    for i, label in enumerate(presets):
        cy = top + i * (row_h + gap) + row_h / 2
        selected = (i == 1)  # 低音增强
        if selected:
            c.round_rect(77, cy - row_h / 2, 373, cy + row_h / 2, row_h / 2,
                         fill=PRIMARY_CONTAINER, outline=PRIMARY, width=1.4)
        else:
            c.round_rect(77, cy - row_h / 2, 373, cy + row_h / 2, row_h / 2,
                         fill=SURFACE, outline=OUTLINE_VAR, width=0.9)
        # 单选圈
        rx = 100
        if selected:
            c.circle(rx, cy, 8.0, fill=PRIMARY)
            c.line([(rx - 3.5, cy + 0.2), (rx - 0.8, cy + 3.0), (rx + 4.2, cy - 3.2)],
                   ON_PRIMARY, 1.8)
            c.text(120, cy, label, 13.0, ON_PRIMARY_CONT, bold=True, anchor="lm")
        else:
            c.circle(rx, cy, 8.0, outline=OUTLINE, width=1.5)
            c.text(120, cy, label, 13.0, ON_SURFACE, anchor="lm")

    # EQ 预设曲线示意
    c.text(CX, 314, "预设曲线示意", 9.0, BUTTON_HINT)
    base_y = 360.0
    c.line([(100, base_y), (350, base_y)], OUTLINE_VAR, 0.9)
    pts = [(100, .30), (150, .72), (200, .50), (250, .38), (300, .62), (350, .45)]
    coords = [(x, base_y - h * 36.0) for x, h in pts]
    for (x0, y0), (x1, y1) in zip(coords, coords[1:]):
        c.line([(x0, y0), (x1, y1)], PRIMARY, 2.0)
    for x, y in coords:
        c.circle(x, y, 2.4, fill=PRIMARY)
    c.text(CX, 388, "不同耳机型号支持的音效可能不同", 9.5, BUTTON_HINT)

    c.img.resize((SIZE, SIZE), Image.LANCZOS).save(OUT_DIR / "equalizer.png", optimize=True)


# ---------------------------------------------------------------- 页面：Gestures
def render_gesture():
    c = Canvas()
    draw_chrome(c)

    c.text(CX, 92, "触控手势", 16.0, ON_SURFACE, bold=True)

    rows = [
        ("double", "双击", "播放/暂停"),
        ("triple", "三击", "上一首"),
        ("long", "长按", "切换降噪"),
    ]
    top, row_h, gap = 110.0, 46.0, 9.0
    for i, (kind, label, action) in enumerate(rows):
        cy = top + i * (row_h + gap) + row_h / 2
        c.round_rect(75, cy - row_h / 2, 375, cy + row_h / 2, 23,
                     fill=SURFACE, outline=CARD_BORDER, width=1.0)
        icon_badge(c, 101, cy, 13.0)
        if kind == "double":
            c.circle(98.5, cy, 2.2, fill=ON_SURFACE_VAR)
            c.circle(103.5, cy, 2.2, fill=ON_SURFACE_VAR)
        elif kind == "triple":
            for dx in (-4.0, 0.0, 4.0):
                c.circle(101 + dx, cy, 2.0, fill=ON_SURFACE_VAR)
        else:
            c.round_rect(95, cy - 2.0, 107, cy + 2.0, 2.0, fill=ON_SURFACE_VAR)
        c.text(122, cy, label, 13.5, ON_SURFACE, bold=True, anchor="lm")
        c.text(352, cy, action, 12.0, PRIMARY, anchor="rm")
        draw_chevron(c, 364, cy)

    # 说明卡
    c.round_rect(74, 290, 376, 342, 18, fill=SURFACE_LOW, outline=CARD_BORDER, width=0.9)
    c.text(CX, 307, "点击任意一行可更换动作", 11.5, ON_SURFACE, bold=True)
    c.text(CX, 325, "支持播放/暂停、切歌、音量、语音助手、切换降噪等 8 种", 9.0, ON_SURFACE_VAR)

    c.text(CX, 366, "设置会立即写入耳机", 9.5, BUTTON_HINT)

    c.img.resize((SIZE, SIZE), Image.LANCZOS).save(OUT_DIR / "gesture.png", optimize=True)


# ---------------------------------------------------------------- 页面：Scan
def render_scan():
    c = Canvas()
    draw_chrome(c)

    c.text(CX, 92, "扫描设备", 16.0, ON_SURFACE, bold=True)

    # 扫描中进度环
    track_c = SURFACE_HIGH
    c.ring(CX, 148, 27, track_c, 4.5)
    c.arc(CX, 148, 27, 205, 335, PRIMARY, 4.5)
    c.arc(CX, 148, 27, 345, 375, SECONDARY, 4.5)
    c.circle(CX + 27 * math.sin(math.radians(205)), 148 - 27 * math.cos(math.radians(205)),
             2.6, fill=PRIMARY)
    c.text(CX, 198, "正在扫描…", 12.5, ON_SURFACE, bold=True)
    c.text(CX, 216, "请确保耳机已进入配对状态", 9.5, ON_SURFACE_VAR)

    # 设备列表
    devices = [
        ("realme Buds Air 6 Pro", 4, -42),
        ("realme Buds T300", 3, -67),
    ]
    top, row_h, gap = 238.0, 40.0, 8.0
    for i, (name, bars, rssi) in enumerate(devices):
        cy = top + i * (row_h + gap) + row_h / 2
        c.round_rect(70, cy - row_h / 2, 380, cy + row_h / 2, 20,
                     fill=SURFACE, outline=CARD_BORDER, width=1.0)
        icon_badge(c, 96, cy, 12.0)
        glyph_buds(c, 96, cy)
        c.text(116, cy, name, 12.0, ON_SURFACE, bold=True, anchor="lm")
        draw_signal_bars(c, 330, cy, bars)
        c.text(370, cy, f"{rssi} dBm", 9.0, ON_SURFACE_VAR, anchor="rm")

    # 重新扫描
    c.round_rect(155, 348, 295, 378, 15, fill=None, outline=OUTLINE, width=1.2)
    c.text(CX, 363, "重新扫描", 11.5, ON_SURFACE)

    c.img.resize((SIZE, SIZE), Image.LANCZOS).save(OUT_DIR / "scan.png", optimize=True)


# ---------------------------------------------------------------- main
def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    render_home()
    render_noise()
    render_equalizer()
    render_gesture()
    render_scan()
    for p in sorted(OUT_DIR.glob("*.png")):
        with Image.open(p) as im:
            print(f"{p.name}: {im.size[0]}x{im.size[1]} {im.mode}")


if __name__ == "__main__":
    main()
