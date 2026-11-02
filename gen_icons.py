import os
from PIL import Image, ImageDraw, ImageFont

BASE = "app/src/main/res"
DENSITIES = {
    "mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192,
}

BG = (10, 12, 26, 255)      # deep navy
ACCENT = (233, 30, 99, 255)  # pinkish red (matches menu)
ACCENT2 = (255, 193, 7, 255) # amber
WHITE = (255, 255, 255, 255)


def draw_icon(size, round_icon=False):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    pad = max(1, size // 24)
    box = [pad, pad, size - pad, size - pad]
    if round_icon:
        d.ellipse(box, fill=BG, outline=ACCENT, width=max(1, size // 24))
    else:
        r = size // 6
        d.rounded_rectangle(box, radius=r, fill=BG, outline=ACCENT, width=max(1, size // 24))

    # stylized "N64" / controller glyph
    cx, cy = size / 2, size / 2
    body_w, body_h = size * 0.60, size * 0.40
    x0, y0 = cx - body_w / 2, cy - body_h / 2
    x1, y1 = cx + body_w / 2, cy + body_h / 2
    d.rounded_rectangle([x0, y0, x1, y1], radius=int(size * 0.14), fill=(28, 32, 54, 255),
                        outline=WHITE, width=max(1, int(size * 0.02)))

    # d-pad cross (left)
    cross_r = size * 0.075
    dcx, dcy = x0 + body_w * 0.22, cy
    t = cross_r * 0.38
    d.rectangle([dcx - t, dcy - cross_r, dcx + t, dcy + cross_r], fill=ACCENT2)
    d.rectangle([dcx - cross_r, dcy - t, dcx + cross_r, dcy + t], fill=ACCENT2)

    # action buttons (right)
    br = size * 0.055
    bcx, bcy = x1 - body_w * 0.24, cy
    for dx, dy, col in ((-1, -1, ACCENT), (1, -1, (56, 142, 60, 255)),
                        (-1, 1, (25, 118, 210, 255)), (1, 1, ACCENT2)):
        px, py = bcx + dx * br * 1.5, bcy + dy * br * 1.5
        d.ellipse([px - br, py - br, px + br, py + br], fill=col)

    return img


for name, size in DENSITIES.items():
    folder = os.path.join(BASE, f"mipmap-{name}")
    os.makedirs(folder, exist_ok=True)
    draw_icon(size, False).save(os.path.join(folder, "ic_launcher.png"))
    draw_icon(size, True).save(os.path.join(folder, "ic_launcher_round.png"))
    # adaptive icon foreground needs extra padding (safe zone = 66%)
    fg_size = int(size * 1.4)
    draw_icon(fg_size, False).save(os.path.join(folder, "ic_launcher_foreground.png"))

print("Icons generated:", ", ".join(DENSITIES.keys()))
