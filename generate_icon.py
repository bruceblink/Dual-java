"""
生成 Dual 游戏图标：
- 深色背景（近黑色）
- 两个交叉的箭（金色），象征双人对决
- 中央菱形光晕
"""
from PIL import Image, ImageDraw, ImageFilter
import math

def draw_arrow(draw, cx, cy, angle_deg, length, color, width):
    """绘制一支箭（箭杆 + 箭头 + 箭尾羽）"""
    angle = math.radians(angle_deg)
    dx = math.cos(angle)
    dy = math.sin(angle)

    # 箭杆
    x0 = cx - dx * length / 2
    y0 = cy - dy * length / 2
    x1 = cx + dx * length / 2
    y1 = cy + dy * length / 2
    draw.line([(x0, y0), (x1, y1)], fill=color, width=width)

    # 箭头三角
    tip_x = x1
    tip_y = y1
    perp_angle = angle + math.pi / 2
    head_len = length * 0.18
    head_w = width * 2.5
    base_x = tip_x - dx * head_len
    base_y = tip_y - dy * head_len
    left_x = base_x + math.cos(perp_angle) * head_w
    left_y = base_y + math.sin(perp_angle) * head_w
    right_x = base_x - math.cos(perp_angle) * head_w
    right_y = base_y - math.sin(perp_angle) * head_w
    draw.polygon([(tip_x, tip_y), (left_x, left_y), (right_x, right_y)], fill=color)

    # 箭尾羽（两侧小线段）
    tail_x = x0
    tail_y = y0
    for sign in [1, -1]:
        fx = tail_x + (math.cos(perp_angle) * sign * head_w * 0.9) - dx * head_len * 0.7
        fy = tail_y + (math.sin(perp_angle) * sign * head_w * 0.9) - dy * head_len * 0.7
        draw.line([(tail_x, tail_y), (fx, fy)], fill=color, width=max(1, width - 1))

SIZE = 256

def make_icon(size):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    s = size
    cx = s // 2
    cy = s // 2

    # 背景圆（深蓝黑色）
    margin = s * 0.04
    draw.ellipse([margin, margin, s - margin, s - margin],
                 fill=(18, 22, 38, 255))

    # 中央菱形光晕（低饱和金色）
    glow_size = s * 0.22
    for i in range(12, 0, -1):
        alpha = int(120 * (i / 12) ** 2)
        gs = glow_size * (1 + (12 - i) * 0.12)
        draw.polygon([
            (cx, cy - gs),
            (cx + gs * 0.55, cy),
            (cx, cy + gs),
            (cx - gs * 0.55, cy),
        ], fill=(200, 160, 60, alpha))

    # 菱形实心核心
    cs = glow_size * 0.35
    draw.polygon([
        (cx, cy - cs),
        (cx + cs * 0.55, cy),
        (cx, cy + cs),
        (cx - cs * 0.55, cy),
    ], fill=(240, 210, 100, 255))

    # 两支交叉箭（45° / -45°）
    arrow_len = s * 0.68
    arrow_w = max(2, s // 28)
    gold = (220, 175, 60, 255)
    dark_gold = (160, 120, 30, 255)

    # 后方箭（先画，被前方箭压住）
    draw_arrow(draw, cx, cy, -45, arrow_len, dark_gold, arrow_w)
    # 前方箭
    draw_arrow(draw, cx, cy, 45, arrow_len, gold, arrow_w)

    # 外边框圆环
    ring_w = max(2, s // 40)
    draw.ellipse([margin, margin, s - margin, s - margin],
                 outline=(120, 90, 30, 200), width=ring_w)

    return img

# 生成多尺寸 ICO
sizes = [16, 32, 48, 64, 128, 256]
images = [make_icon(s) for s in sizes]

output_path = "src/main/resources/icon.ico"
import os
os.makedirs("src/main/resources", exist_ok=True)
images[0].save(output_path, format="ICO", sizes=[(s, s) for s in sizes],
               append_images=images[1:])
print(f"Icon saved: {output_path}")

# 同时保存 256x256 PNG 预览
images[-1].save("src/main/resources/icon_preview.png")
print("Preview saved: src/main/resources/icon_preview.png")
