from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs" / "hidden-mobile-finance-lab.md"
OUTPUT = ROOT / "docs" / "hidden-mobile-finance-lab.pdf"
FONT = Path("/mnt/c/Windows/Fonts/malgunbd.ttf")

PAGE_W, PAGE_H = 1240, 1754
MARGIN_X, MARGIN_Y = 88, 78
BLUE = "#2563eb"
INK = "#0f172a"
MUTED = "#263447"
LINE = "#d7e0ee"
CODE_BG = "#eef4ff"


def font(size):
    return ImageFont.truetype(str(FONT), size=size)


FONTS = {"h1": font(50), "h2": font(35), "h3": font(28), "body": font(25), "code": font(22)}


def text_width(draw, text, selected):
    box = draw.textbbox((0, 0), text, font=selected)
    return box[2] - box[0]


def wrap(draw, text, selected, max_width):
    lines = []
    for para in text.splitlines() or [""]:
        current = ""
        for token in para.split(" "):
            candidate = token if not current else f"{current} {token}"
            if text_width(draw, candidate, selected) <= max_width:
                current = candidate
            else:
                if current:
                    lines.append(current)
                current = token
        lines.append(current)
    return lines


def new_page(pages):
    image = Image.new("RGB", (PAGE_W, PAGE_H), "#f6f8fb")
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((38, 34, PAGE_W - 38, PAGE_H - 34), radius=30, fill="white")
    draw.rounded_rectangle((70, 56, PAGE_W - 70, 82), radius=13, fill=BLUE)
    pages.append(image)
    return draw, MARGIN_Y


def parse(md):
    blocks = []
    code = []
    in_code = False
    for raw in md.splitlines():
        line = raw.rstrip()
        if line.startswith("```"):
            if in_code:
                blocks.append(("code", "\n".join(code)))
                code = []
                in_code = False
            else:
                in_code = True
            continue
        if in_code:
            code.append(line)
        elif not line:
            blocks.append(("space", ""))
        elif line.startswith("# "):
            blocks.append(("h1", line[2:]))
        elif line.startswith("## "):
            blocks.append(("h2", line[3:]))
        elif line.startswith("### "):
            blocks.append(("h3", line[4:]))
        elif line.startswith("- "):
            blocks.append(("bullet", line[2:]))
        elif len(line) > 3 and line[0].isdigit() and ". " in line[:4]:
            blocks.append(("bullet", line))
        else:
            blocks.append(("body", line))
    return blocks


def render():
    pages = []
    draw, y = new_page(pages)
    max_width = PAGE_W - MARGIN_X * 2
    for kind, value in parse(SOURCE.read_text(encoding="utf-8")):
        if kind == "space":
            y += 8
            continue
        if kind == "code":
            selected = FONTS["code"]
            lines = []
            for line in value.splitlines() or [""]:
                lines.extend(wrap(draw, line, selected, max_width - 48))
            needed = 36 + len(lines) * 33
            if y + needed > PAGE_H - MARGIN_Y:
                draw, y = new_page(pages)
            draw.rounded_rectangle((MARGIN_X, y, PAGE_W - MARGIN_X, y + needed), radius=12, fill=CODE_BG, outline=LINE, width=2)
            cy = y + 18
            for line in lines:
                draw.text((MARGIN_X + 22, cy), line, font=selected, fill=INK)
                cy += 33
            y += needed + 16
            continue

        selected = FONTS.get(kind, FONTS["body"])
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
            color = BLUE
            gap = 20
        elif kind == "h3":
            gap = 12
        elif kind == "bullet":
            prefix = "- "
            color = MUTED
        lines = wrap(draw, value, selected, max_width - text_width(draw, prefix, selected) - 8)
        line_h = selected.size + 13
        needed = len(lines) * line_h + gap
        if y + needed > PAGE_H - MARGIN_Y:
            draw, y = new_page(pages)
        for index, line in enumerate(lines):
            if prefix and index == 0:
                draw.text((MARGIN_X, y), prefix, font=selected, fill=color)
            x = MARGIN_X + (text_width(draw, prefix, selected) if index == 0 else 30)
            draw.text((x, y), line, font=selected, fill=color)
            y += line_h
        y += gap
    pages[0].save(OUTPUT, "PDF", resolution=150.0, save_all=True, append_images=pages[1:])
    print(OUTPUT)


if __name__ == "__main__":
    render()
