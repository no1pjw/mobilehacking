from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs" / "comprehensive-vulnbank-lab-guide.md"
OUTPUT = ROOT / "docs" / "comprehensive-vulnbank-lab-guide.pdf"
FONT_REGULAR = Path("/mnt/c/Windows/Fonts/malgunbd.ttf")
FONT_BOLD = Path("/mnt/c/Windows/Fonts/malgunbd.ttf")

PAGE_W, PAGE_H = 1240, 1754
MARGIN_X, MARGIN_Y = 92, 82
TEXT = "#0f172a"
MUTED = "#334155"
BLUE = "#1d4ed8"
CODE_BG = "#e2e8f0"
LINE = "#94a3b8"


def font(size):
    return ImageFont.truetype(str(FONT_BOLD if FONT_BOLD.exists() else FONT_REGULAR), size=size)


FONTS = {
    "h1": font(48),
    "h2": font(34),
    "h3": font(28),
    "body": font(25),
    "code": font(23),
}


def tw(draw, text, selected):
    box = draw.textbbox((0, 0), text, font=selected)
    return box[2] - box[0]


def wrap(draw, text, selected, max_width):
    out = []
    for paragraph in text.splitlines() or [""]:
        current = ""
        for token in paragraph.split(" "):
            candidate = token if not current else f"{current} {token}"
            if tw(draw, candidate, selected) <= max_width:
                current = candidate
            else:
                if current:
                    out.append(current)
                current = token
        out.append(current)
    return out


def page(pages):
    image = Image.new("RGB", (PAGE_W, PAGE_H), "white")
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, PAGE_W, 22), fill=BLUE)
    pages.append(image)
    return draw, MARGIN_Y


def blocks(md):
    result = []
    code = []
    in_code = False
    for raw in md.splitlines():
        line = raw.rstrip()
        if line.startswith("```"):
            if in_code:
                result.append(("code", "\n".join(code)))
                code = []
                in_code = False
            else:
                in_code = True
            continue
        if in_code:
            code.append(line)
        elif not line:
            result.append(("space", ""))
        elif line.startswith("# "):
            result.append(("h1", line[2:]))
        elif line.startswith("## "):
            result.append(("h2", line[3:]))
        elif line.startswith("### "):
            result.append(("h3", line[4:]))
        elif line.startswith("- "):
            result.append(("bullet", line[2:]))
        elif len(line) > 3 and line[0].isdigit() and ". " in line[:4]:
            result.append(("bullet", line))
        else:
            result.append(("body", line))
    return result


def render():
    pages = []
    draw, y = page(pages)
    max_width = PAGE_W - MARGIN_X * 2

    for kind, text in blocks(SOURCE.read_text(encoding="utf-8")):
        if kind == "space":
            y += 8
            continue

        if kind == "code":
            selected = FONTS["code"]
            lines = []
            for line in text.splitlines() or [""]:
                lines.extend(wrap(draw, line, selected, max_width - 42))
            needed = 34 + len(lines) * 33
            if y + needed > PAGE_H - MARGIN_Y:
                draw, y = page(pages)
            draw.rounded_rectangle((MARGIN_X, y, PAGE_W - MARGIN_X, y + needed), radius=10, fill=CODE_BG, outline=LINE, width=2)
            cy = y + 17
            for line in lines:
                draw.text((MARGIN_X + 20, cy), line, font=selected, fill="#111827")
                cy += 33
            y += needed + 14
            continue

        selected = FONTS.get(kind, FONTS["body"])
        color = TEXT
        prefix = ""
        gap = 14
        if kind == "h1":
            color = "#0b1220"
            gap = 28
        elif kind == "h2":
            if y > MARGIN_Y + 20:
                y += 14
            draw.line((MARGIN_X, y, PAGE_W - MARGIN_X, y), fill=LINE, width=3)
            y += 16
            gap = 20
        elif kind == "h3":
            color = "#1e293b"
            gap = 12
        elif kind == "bullet":
            color = MUTED
            prefix = "- "

        available = max_width - tw(draw, prefix, selected) - 8
        lines = wrap(draw, text, selected, available)
        line_h = selected.size + 12
        needed = len(lines) * line_h + gap
        if y + needed > PAGE_H - MARGIN_Y:
            draw, y = page(pages)
        for i, line in enumerate(lines):
            if prefix and i == 0:
                draw.text((MARGIN_X, y), prefix, font=selected, fill=color)
            x = MARGIN_X + (tw(draw, prefix, selected) if i == 0 else 30)
            draw.text((x, y), line, font=selected, fill=color)
            y += line_h
        y += gap

    pages[0].save(OUTPUT, "PDF", resolution=150.0, save_all=True, append_images=pages[1:])
    print(OUTPUT)


if __name__ == "__main__":
    render()
