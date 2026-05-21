from __future__ import annotations

from pathlib import Path
import sys

from reportlab.lib.pagesizes import LETTER
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Preformatted


def md_to_flowables(md_text: str):
    styles = getSampleStyleSheet()
    h1 = styles["Heading1"]
    h2 = styles["Heading2"]
    normal = styles["BodyText"]
    mono = styles["Code"]

    story = []
    in_code_block = False
    code_lines: list[str] = []

    for raw_line in md_text.splitlines():
        line = raw_line.rstrip("\n")

        if line.strip().startswith("```"):
            if in_code_block:
                story.append(Preformatted("\n".join(code_lines), mono))
                story.append(Spacer(1, 8))
                code_lines = []
                in_code_block = False
            else:
                in_code_block = True
            continue

        if in_code_block:
            code_lines.append(line)
            continue

        if not line.strip():
            story.append(Spacer(1, 8))
            continue

        if line.startswith("# "):
            story.append(Paragraph(line[2:].strip(), h1))
            story.append(Spacer(1, 10))
            continue

        if line.startswith("## "):
            story.append(Paragraph(line[3:].strip(), h2))
            story.append(Spacer(1, 8))
            continue

        # Keep markdown tables as monospaced text blocks for reliability
        if line.startswith("|") and line.endswith("|"):
            story.append(Preformatted(line, mono))
            continue

        if line.startswith("- "):
            story.append(Paragraph("• " + line[2:].strip(), normal))
            continue

        story.append(Paragraph(line.replace("  ", " "), normal))

    if in_code_block and code_lines:
        story.append(Preformatted("\n".join(code_lines), mono))

    return story


def main() -> None:
    docs_dir = Path(__file__).resolve().parent

    md_name = sys.argv[1] if len(sys.argv) >= 2 else "resumen-spring-security.md"
    pdf_name = sys.argv[2] if len(sys.argv) >= 3 else "resumen-spring-security.pdf"

    md_path = docs_dir / md_name
    pdf_path = docs_dir / pdf_name

    md_text = md_path.read_text(encoding="utf-8")

    doc = SimpleDocTemplate(
        str(pdf_path),
        pagesize=LETTER,
        title="Resumen Spring Security - AsistenciaFGK",
        author="AsistenciaFGK",
    )

    story = md_to_flowables(md_text)
    doc.build(story)

    print(f"OK: {pdf_path}")


if __name__ == "__main__":
    main()
