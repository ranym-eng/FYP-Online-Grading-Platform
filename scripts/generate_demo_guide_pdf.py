from __future__ import annotations

import html
import re
import sys
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import PageBreak, Paragraph, Preformatted, SimpleDocTemplate, Spacer, Table, TableStyle


NAVY = colors.HexColor("#0B2452")
BLUE = colors.HexColor("#205FB2")
GREEN = colors.HexColor("#157A6E")
INK = colors.HexColor("#172033")
MUTED = colors.HexColor("#5D687B")
GRID = colors.HexColor("#CBD5E1")
PALE_BLUE = colors.HexColor("#EDF4FC")
PALE_GREEN = colors.HexColor("#EDF8F5")


def load_fonts():
    font_dir = Path("C:/Windows/Fonts")
    if (font_dir / "arial.ttf").exists():
        pdfmetrics.registerFont(TTFont("GuideSans", str(font_dir / "arial.ttf")))
        pdfmetrics.registerFont(TTFont("GuideSansBold", str(font_dir / "arialbd.ttf")))
        sans, bold = "GuideSans", "GuideSansBold"
    else:
        sans, bold = "Helvetica", "Helvetica-Bold"
    if (font_dir / "consola.ttf").exists():
        pdfmetrics.registerFont(TTFont("GuideMono", str(font_dir / "consola.ttf")))
        mono = "GuideMono"
    else:
        mono = "Courier"
    return sans, bold, mono


SANS, BOLD, MONO = load_fonts()


def markup(text: str) -> str:
    escaped = html.escape(text.strip())
    tick = re.escape(chr(96))
    escaped = re.sub(
        tick + r"([^" + tick + r"]+)" + tick,
        lambda match: f'<font name="{MONO}" color="#0B4F6C">{match.group(1)}</font>',
        escaped,
    )
    return re.sub(r"\*\*([^*]+)\*\*", r"<b>\1</b>", escaped)


def make_styles():
    base = getSampleStyleSheet()
    return {
        "title": ParagraphStyle("Title", parent=base["Title"], fontName=BOLD, fontSize=23, leading=28, textColor=NAVY, alignment=TA_CENTER, spaceAfter=8 * mm),
        "subtitle": ParagraphStyle("Subtitle", parent=base["BodyText"], fontName=SANS, fontSize=11, leading=15, textColor=MUTED, alignment=TA_CENTER, spaceAfter=7 * mm),
        "h2": ParagraphStyle("H2", parent=base["Heading2"], fontName=BOLD, fontSize=15, leading=19, textColor=NAVY, spaceBefore=5 * mm, spaceAfter=2.5 * mm, keepWithNext=True),
        "h3": ParagraphStyle("H3", parent=base["Heading3"], fontName=BOLD, fontSize=12, leading=15, textColor=GREEN, spaceBefore=4 * mm, spaceAfter=2 * mm, keepWithNext=True),
        "h4": ParagraphStyle("H4", parent=base["Heading4"], fontName=BOLD, fontSize=10, leading=13, textColor=BLUE, spaceBefore=3 * mm, spaceAfter=1.5 * mm, keepWithNext=True),
        "body": ParagraphStyle("Body", parent=base["BodyText"], fontName=SANS, fontSize=8.7, leading=12, textColor=INK, spaceAfter=1.7 * mm),
        "list": ParagraphStyle("List", parent=base["BodyText"], fontName=SANS, fontSize=8.7, leading=11.5, leftIndent=5 * mm, firstLineIndent=-3 * mm, textColor=INK, spaceAfter=1.1 * mm),
        "quote": ParagraphStyle("Quote", parent=base["BodyText"], fontName=SANS, fontSize=9, leading=13, textColor=NAVY),
        "code": ParagraphStyle("Code", parent=base["Code"], fontName=MONO, fontSize=7.1, leading=9.4, textColor=colors.HexColor("#102A43")),
        "th": ParagraphStyle("TH", parent=base["BodyText"], fontName=BOLD, fontSize=7.2, leading=9, textColor=colors.white),
        "td": ParagraphStyle("TD", parent=base["BodyText"], fontName=SANS, fontSize=7.1, leading=9, textColor=INK),
    }


def make_table(lines, width, style_map):
    raw_rows = []
    for line in lines:
        cells = [cell.strip() for cell in line.strip().strip("|").split("|")]
        if all(re.fullmatch(r":?-{3,}:?", cell) for cell in cells):
            continue
        raw_rows.append(cells)
    count = max(len(row) for row in raw_rows)
    for row in raw_rows:
        row.extend([""] * (count - len(row)))
    rows = [
        [Paragraph(markup(cell), style_map["th"] if index == 0 else style_map["td"]) for cell in row]
        for index, row in enumerate(raw_rows)
    ]
    if count == 2:
        widths = [width * 0.35, width * 0.65]
    elif count == 3:
        widths = [width * 0.25, width * 0.35, width * 0.40]
    else:
        widths = [width / count] * count
    table = Table(rows, colWidths=widths, repeatRows=1, hAlign="LEFT")
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), NAVY),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#F7FAFC")]),
        ("GRID", (0, 0), (-1, -1), 0.45, GRID),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 4),
        ("RIGHTPADDING", (0, 0), (-1, -1), 4),
        ("TOPPADDING", (0, 0), (-1, -1), 4),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
    ]))
    return table


def boxed(flowable, width, background, accent=None):
    box = Table([[flowable]], colWidths=[width])
    commands = [
        ("BACKGROUND", (0, 0), (-1, -1), background),
        ("BOX", (0, 0), (-1, -1), 0.5, GRID),
        ("LEFTPADDING", (0, 0), (-1, -1), 7),
        ("RIGHTPADDING", (0, 0), (-1, -1), 7),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ]
    if accent:
        commands.append(("LINEBEFORE", (0, 0), (0, -1), 3, accent))
    box.setStyle(TableStyle(commands))
    return box


def parse(markdown, width, style_map):
    story = []
    lines = markdown.replace("\ufeff", "").splitlines()
    fence = chr(96) * 3
    index = 0
    first_title = True
    while index < len(lines):
        text = lines[index].strip()
        if not text:
            story.append(Spacer(1, 1.2 * mm))
            index += 1
            continue
        if text.startswith(fence):
            language = text[3:].strip()
            code_lines = []
            index += 1
            while index < len(lines) and not lines[index].strip().startswith(fence):
                code_lines.append(lines[index].rstrip())
                index += 1
            label = f"[{language}]\n" if language else ""
            story.append(boxed(Preformatted(label + "\n".join(code_lines), style_map["code"]), width, PALE_BLUE))
            story.append(Spacer(1, 2 * mm))
            index += 1
            continue
        if text.startswith("|"):
            table_lines = []
            while index < len(lines) and lines[index].strip().startswith("|"):
                table_lines.append(lines[index].strip())
                index += 1
            story.extend([make_table(table_lines, width, style_map), Spacer(1, 2 * mm)])
            continue
        if text.startswith("# "):
            if not first_title:
                story.append(PageBreak())
            story.append(Paragraph(markup(text[2:]), style_map["title"]))
            story.append(Paragraph("Sultan Qaboos University - Final Year Project assessment workflow", style_map["subtitle"]))
            first_title = False
            index += 1
            continue
        if text.startswith("## "):
            story.append(Paragraph(markup(text[3:]), style_map["h2"]))
            index += 1
            continue
        if text.startswith("### "):
            story.append(Paragraph(markup(text[4:]), style_map["h3"]))
            index += 1
            continue
        if text.startswith("#### "):
            story.append(Paragraph(markup(text[5:]), style_map["h4"]))
            index += 1
            continue
        if text.startswith(">"):
            quote_lines = []
            while index < len(lines) and lines[index].strip().startswith(">"):
                quote_lines.append(lines[index].strip().lstrip(">").strip())
                index += 1
            story.extend([boxed(Paragraph(markup(" ".join(quote_lines)), style_map["quote"]), width, PALE_GREEN, GREEN), Spacer(1, 2 * mm)])
            continue
        numbered = re.match(r"^(\d+)\.\s+(.*)$", text)
        if numbered:
            story.append(Paragraph(markup(numbered.group(2)), style_map["list"], bulletText=numbered.group(1) + "."))
            index += 1
            continue
        if text.startswith("- "):
            story.append(Paragraph(markup(text[2:]), style_map["list"], bulletText="-"))
            index += 1
            continue
        paragraph = [text]
        index += 1
        while index < len(lines):
            candidate = lines[index].strip()
            if not candidate or candidate.startswith(("#", "|", fence, ">", "- ")) or re.match(r"^\d+\.\s+", candidate):
                break
            paragraph.append(candidate)
            index += 1
        story.append(Paragraph(markup(" ".join(paragraph)), style_map["body"]))
    return story


def decorate(canvas, document):
    canvas.saveState()
    width, height = A4
    canvas.setStrokeColor(GRID)
    canvas.line(document.leftMargin, height - 17 * mm, width - document.rightMargin, height - 17 * mm)
    canvas.setFont(BOLD, 7.5)
    canvas.setFillColor(NAVY)
    canvas.drawString(document.leftMargin, height - 13 * mm, "Sultan Qaboos University")
    canvas.setFont(SANS, 7.2)
    canvas.setFillColor(MUTED)
    canvas.drawRightString(width - document.rightMargin, height - 13 * mm, "FYP Online Grading Platform")
    canvas.line(document.leftMargin, 15 * mm, width - document.rightMargin, 15 * mm)
    canvas.drawString(document.leftMargin, 10.5 * mm, "Guide de démonstration réelle")
    canvas.drawRightString(width - document.rightMargin, 10.5 * mm, f"Page {document.page}")
    canvas.restoreState()


def main():
    if len(sys.argv) != 3:
        raise SystemExit("Usage: generate_demo_guide_pdf.py INPUT.md OUTPUT.pdf")
    source = Path(sys.argv[1]).resolve()
    output = Path(sys.argv[2]).resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    document = SimpleDocTemplate(
        str(output),
        pagesize=A4,
        leftMargin=16 * mm,
        rightMargin=16 * mm,
        topMargin=23 * mm,
        bottomMargin=21 * mm,
        title="Guide de démonstration réelle - FYP Online Grading Platform",
        author="Sultan Qaboos University",
    )
    document.build(
        parse(source.read_text(encoding="utf-8-sig"), document.width, make_styles()),
        onFirstPage=decorate,
        onLaterPages=decorate,
    )
    print(output)


if __name__ == "__main__":
    main()

