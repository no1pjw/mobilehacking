from pathlib import Path
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs" / "transfer-vulnerability-test.md"
OUTPUT = ROOT / "docs" / "transfer-vulnerability-test.pdf"
FONT_REGULAR = Path("/mnt/c/Windows/Fonts/NotoSansKR-VF.ttf")
FONT_BOLD = Path("/mnt/c/Windows/Fonts/malgunbd.ttf")

PAGE_W, PAGE_H = 1240, 1754
MARGIN_X, MARGIN_Y = 100, 90
LINE_GAP = 12
BG = "white"
TEXT = "#1f2933"
MUTED = "#475569"
CODE_BG = "#f1f5f9"
ACCENT = "#1d4ed8"


def font(size, bold=False):
    path = FONT_BOLD if bold and FONT_BOLD.exists() else FONT_REGULAR
    return ImageFont.truetype(str(path), size=size)


FONTS = {
    "h1": font(42, True),
    "h2": font(30, True),
    "body": font(24),
    "bullet": font(24),
    "code": font(21),
}


def text_width(draw, value, selected_font):
    box = draw.textbbox((0, 0), value, font=selected_font)
    return box[2] - box[0]


def wrap_text(draw, value, selected_font, max_width):
    lines = []
    for paragraph in value.splitlines() or [""]:
        current = ""
        for token in paragraph.split(" "):
            candidate = token if not current else f"{current} {token}"
            if text_width(draw, candidate, selected_font) <= max_width:
                current = candidate
                continue
            if current:
                lines.append(current)
            current = token
        lines.append(current)
    return lines


def new_page(pages):
    image = Image.new("RGB", (PAGE_W, PAGE_H), BG)
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, PAGE_W, 16), fill=ACCENT)
    pages.append(image)
    return image, draw, MARGIN_Y


def draw_line(draw, y):
    draw.line((MARGIN_X, y, PAGE_W - MARGIN_X, y), fill="#d9e2ec", width=2)


def parse_blocks(markdown):
    blocks = []
    code_lines = []
    in_code = False

    for raw in markdown.splitlines():
        line = raw.rstrip()
        if line.startswith("```"):
            if in_code:
                blocks.append(("code", "\n".join(code_lines)))
                code_lines = []
                in_code = False
            else:
                in_code = True
            continue
        if in_code:
            code_lines.append(line)
            continue
        if not line:
            blocks.append(("space", ""))
        elif line.startswith("# "):
            blocks.append(("h1", line[2:]))
        elif line.startswith("## "):
            blocks.append(("h2", line[3:]))
        elif line.startswith("- "):
            blocks.append(("bullet", line[2:]))
        elif line[0:2].isdigit() and ". " in line[:4]:
            blocks.append(("body", line))
        else:
            blocks.append(("body", line))

    return blocks


def render():
    markdown = SOURCE.read_text(encoding="utf-8")
    pages = []
    image, draw, y = new_page(pages)
    max_width = PAGE_W - (MARGIN_X * 2)

    for kind, value in parse_blocks(markdown):
        if kind == "space":
            y += 14
            continue

        selected_font = FONTS.get(kind, FONTS["body"])
        fill = TEXT
        prefix = ""
        block_gap = 18
        line_height = selected_font.size + LINE_GAP

        if kind == "h1":
            y += 10
            fill = "#0f172a"
            block_gap = 28
        elif kind == "h2":
            y += 16
            fill = "#0f172a"
            block_gap = 18
            draw_line(draw, y - 8)
        elif kind == "bullet":
            prefix = "- "
            fill = MUTED

        if kind == "code":
            code_lines = value.splitlines() or [""]
            wrapped_lines = []
            for code_line in code_lines:
                wrapped_lines.extend(wrap_text(draw, code_line, FONTS["code"], max_width - 40))
            needed = 34 + len(wrapped_lines) * (FONTS["code"].size + 10)
            if y + needed > PAGE_H - MARGIN_Y:
                image, draw, y = new_page(pages)
            draw.rounded_rectangle(
                (MARGIN_X, y, PAGE_W - MARGIN_X, y + needed),
                radius=12,
                fill=CODE_BG,
                outline="#d9e2ec",
                width=1,
            )
            code_y = y + 18
            for code_line in wrapped_lines:
                draw.text((MARGIN_X + 20, code_y), code_line, font=FONTS["code"], fill="#111827")
                code_y += FONTS["code"].size + 10
            y += needed + 18
            continue

        available_width = max_width - text_width(draw, prefix, selected_font)
        lines = wrap_text(draw, value, selected_font, available_width)
        needed = len(lines) * line_height + block_gap
        if y + needed > PAGE_H - MARGIN_Y:
            image, draw, y = new_page(pages)

        x = MARGIN_X
        for index, line in enumerate(lines):
            draw.text((x, y), prefix if index == 0 else "", font=selected_font, fill=fill)
            line_x = x + text_width(draw, prefix, selected_font) if index == 0 else x + 24
            draw.text((line_x, y), line, font=selected_font, fill=fill)
            y += line_height
        y += block_gap

    pages[0].save(OUTPUT, "PDF", resolution=150.0, save_all=True, append_images=pages[1:])


if __name__ == "__main__":
    render()
    print(OUTPUT)
