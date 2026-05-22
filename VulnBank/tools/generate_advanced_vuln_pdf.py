from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs" / "advanced-vulnerabilities-guide.md"
OUTPUT = ROOT / "docs" / "advanced-vulnerabilities-guide.pdf"
FONT_REGULAR = Path("/mnt/c/Windows/Fonts/NotoSansKR-VF.ttf")
FONT_BOLD = Path("/mnt/c/Windows/Fonts/malgunbd.ttf")

PAGE_W, PAGE_H = 1240, 1754
MARGIN_X, MARGIN_Y = 96, 88
TEXT = "#172033"
MUTED = "#475569"
BLUE = "#2563eb"
CODE_BG = "#f1f5f9"
LINE = "#d9e2ec"


def font(size, bold=False):
    path = FONT_BOLD if bold and FONT_BOLD.exists() else FONT_REGULAR
    return ImageFont.truetype(str(path), size=size)


FONTS = {
    "h1": font(42, True),
    "h2": font(30, True),
    "h3": font(25, True),
    "body": font(23),
    "code": font(20),
}


def width(draw, text, selected):
    box = draw.textbbox((0, 0), text, font=selected)
    return box[2] - box[0]


def wrap(draw, text, selected, max_width):
    lines = []
    for paragraph in text.splitlines() or [""]:
        current = ""
        for token in paragraph.split(" "):
            candidate = token if not current else f"{current} {token}"
            if width(draw, candidate, selected) <= max_width:
                current = candidate
            else:
                if current:
                    lines.append(current)
                current = token
        lines.append(current)
    return lines


def new_page(pages):
    page = Image.new("RGB", (PAGE_W, PAGE_H), "white")
    draw = ImageDraw.Draw(page)
    draw.rectangle((0, 0, PAGE_W, 18), fill=BLUE)
    pages.append(page)
    return page, draw, MARGIN_Y


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
    _, draw, y = new_page(pages)
    max_width = PAGE_W - MARGIN_X * 2

    for kind, text in parse(SOURCE.read_text(encoding="utf-8")):
        if kind == "space":
            y += 10
            continue
        if kind == "code":
            selected = FONTS["code"]
            code_lines = []
            for line in text.splitlines() or [""]:
                code_lines.extend(wrap(draw, line, selected, max_width - 42))
            needed = 32 + len(code_lines) * 30
            if y + needed > PAGE_H - MARGIN_Y:
                _, draw, y = new_page(pages)
            draw.rounded_rectangle((MARGIN_X, y, PAGE_W - MARGIN_X, y + needed), radius=10, fill=CODE_BG, outline=LINE)
            cy = y + 16
            for line in code_lines:
                draw.text((MARGIN_X + 20, cy), line, font=selected, fill="#111827")
                cy += 30
            y += needed + 14
            continue

        selected = FONTS.get(kind, FONTS["body"])
        fill = TEXT
        prefix = ""
        gap = 13
        if kind == "h1":
            fill = "#0f172a"
            gap = 24
        elif kind == "h2":
            if y > MARGIN_Y + 20:
                y += 12
            draw.line((MARGIN_X, y, PAGE_W - MARGIN_X, y), fill=LINE, width=2)
            y += 14
            gap = 16
        elif kind == "h3":
            fill = "#1e293b"
            gap = 10
        elif kind == "bullet":
            prefix = "- "
            fill = MUTED

        text_width = max_width - width(draw, prefix, selected) - 8
        lines = wrap(draw, text, selected, text_width)
        line_h = selected.size + 11
        needed = len(lines) * line_h + gap
        if y + needed > PAGE_H - MARGIN_Y:
            _, draw, y = new_page(pages)

        for i, line in enumerate(lines):
            if prefix and i == 0:
                draw.text((MARGIN_X, y), prefix, font=selected, fill=fill)
            x = MARGIN_X + (width(draw, prefix, selected) if i == 0 else 28)
            draw.text((x, y), line, font=selected, fill=fill)
            y += line_h
        y += gap

    pages[0].save(OUTPUT, "PDF", resolution=150.0, save_all=True, append_images=pages[1:])
    print(OUTPUT)


if __name__ == "__main__":
    render()
