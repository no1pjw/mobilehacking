from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs" / "real-finance-vulnbank-lab.md"
OUTPUT = ROOT / "docs" / "real-finance-vulnbank-lab.pdf"
FONT = Path("/mnt/c/Windows/Fonts/malgunbd.ttf")

PAGE_W, PAGE_H = 1240, 1754
MARGIN_X, MARGIN_Y = 88, 78
BLUE = "#2563eb"
INK = "#0f172a"
MUTED = "#1f2937"
LINE = "#cbd5e1"
CODE_BG = "#e5edf8"


def f(size):
    return ImageFont.truetype(str(FONT), size=size)


FONTS = {
    "h1": f(50),
    "h2": f(35),
    "h3": f(28),
    "body": f(25),
    "code": f(23),
}


def text_w(draw, text, font):
    box = draw.textbbox((0, 0), text, font=font)
    return box[2] - box[0]


def wrap(draw, text, font, max_w):
    lines = []
    for para in text.splitlines() or [""]:
        current = ""
        for token in para.split(" "):
            candidate = token if not current else f"{current} {token}"
            if text_w(draw, candidate, font) <= max_w:
                current = candidate
            else:
                if current:
                    lines.append(current)
                current = token
        lines.append(current)
    return lines


def new_page(pages):
    image = Image.new("RGB", (PAGE_W, PAGE_H), "#f8fafc")
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((42, 34, PAGE_W - 42, PAGE_H - 34), radius=26, fill="white")
    draw.rectangle((42, 34, PAGE_W - 42, 58), fill=BLUE)
    pages.append(image)
    return draw, MARGIN_Y


def parse(md):
    out = []
    code, in_code = [], False
    for raw in md.splitlines():
        line = raw.rstrip()
        if line.startswith("```"):
            if in_code:
                out.append(("code", "\n".join(code)))
                code, in_code = [], False
            else:
                in_code = True
            continue
        if in_code:
            code.append(line)
        elif not line:
            out.append(("space", ""))
        elif line.startswith("# "):
            out.append(("h1", line[2:]))
        elif line.startswith("## "):
            out.append(("h2", line[3:]))
        elif line.startswith("### "):
            out.append(("h3", line[4:]))
        elif line.startswith("- "):
            out.append(("bullet", line[2:]))
        elif len(line) > 3 and line[0].isdigit() and ". " in line[:4]:
            out.append(("bullet", line))
        else:
            out.append(("body", line))
    return out


def render():
    pages = []
    draw, y = new_page(pages)
    max_w = PAGE_W - MARGIN_X * 2

    for kind, text in parse(SOURCE.read_text(encoding="utf-8")):
        if kind == "space":
            y += 8
            continue
        if kind == "code":
            font = FONTS["code"]
            lines = []
            for line in text.splitlines() or [""]:
                lines.extend(wrap(draw, line, font, max_w - 44))
            needed = 36 + len(lines) * 34
            if y + needed > PAGE_H - MARGIN_Y:
                draw, y = new_page(pages)
            draw.rounded_rectangle((MARGIN_X, y, PAGE_W - MARGIN_X, y + needed), radius=12, fill=CODE_BG, outline=LINE, width=2)
            cy = y + 18
            for line in lines:
                draw.text((MARGIN_X + 22, cy), line, font=font, fill=INK)
                cy += 34
            y += needed + 16
            continue

        font = FONTS.get(kind, FONTS["body"])
        color = INK
        prefix = ""
        gap = 14
        if kind == "h1":
            gap = 30
        elif kind == "h2":
            if y > MARGIN_Y + 30:
                y += 16
            draw.line((MARGIN_X, y, PAGE_W - MARGIN_X, y), fill=LINE, width=3)
            y += 18
            gap = 20
            color = "#1d4ed8"
        elif kind == "h3":
            gap = 12
        elif kind == "bullet":
            prefix = "- "
            color = MUTED

        lines = wrap(draw, text, font, max_w - text_w(draw, prefix, font) - 8)
        line_h = font.size + 13
        needed = len(lines) * line_h + gap
        if y + needed > PAGE_H - MARGIN_Y:
            draw, y = new_page(pages)
        for i, line in enumerate(lines):
            if prefix and i == 0:
                draw.text((MARGIN_X, y), prefix, font=font, fill=color)
            x = MARGIN_X + (text_w(draw, prefix, font) if i == 0 else 30)
            draw.text((x, y), line, font=font, fill=color)
            y += line_h
        y += gap

    pages[0].save(OUTPUT, "PDF", resolution=150.0, save_all=True, append_images=pages[1:])
    print(OUTPUT)


if __name__ == "__main__":
    render()
