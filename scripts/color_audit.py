#!/usr/bin/env python3
"""Audit active Vue/CSS literal colors, including encoded SVG hex and faint violet grays."""
import colorsys
import json
from pathlib import Path
import re
import sys


def audit(root):
    files = sorted(path for path in (root / 'Vue/src').rglob('*') if path.suffix in {'.vue', '.css'})
    literals, findings = 0, []
    color = re.compile(r'(?<![\w])(?:#|%23)([a-fA-F0-9]{8}|[a-fA-F0-9]{6}|[a-fA-F0-9]{4}|[a-fA-F0-9]{3})(?![\w])|\b(rgba?|hsla?)\(([^)]+)\)', re.I)
    for path in files:
        source = path.read_text(encoding='utf-8')
        for match in re.finditer(r'\b(?:purple|violet|indigo|magenta|fuchsia|rebeccapurple)\b', source, re.I):
            if source[max(0, match.start()-7):match.start()] == "name: '" and source[match.start():].startswith("purple heart'"):
                continue  # Existing searchable message emoji label, not interface styling.
            findings.append({'path': path.relative_to(root).as_posix(), 'line': source.count('\n', 0, match.start()) + 1, 'literal': match.group()})
        for match in color.finditer(source):
            literals += 1
            if match.group(1):
                value = match.group(1)
                if len(value) in (3, 4): value = ''.join(char * 2 for char in value)
                rgb = [int(value[i:i+2], 16) / 255 for i in (0, 2, 4)]
                hue, _, saturation = colorsys.rgb_to_hls(*rgb)
                hue *= 360
            else:
                parts = re.split(r'[,/\s]+', match.group(3).strip())
                try:
                    if match.group(2).lower().startswith('hsl'):
                        hue, saturation = float(parts[0].removesuffix('deg')) % 360, float(parts[1].rstrip('%')) / 100
                    else:
                        rgb = [float(value.rstrip('%')) / (100 if value.endswith('%') else 255) for value in parts[:3]]
                        hue, _, saturation = colorsys.rgb_to_hls(*rgb)
                        hue *= 360
                except ValueError:
                    continue  # Theme-variable expressions are covered by their literal definitions.
            if saturation > 0 and ((225 <= hue < 235 and saturation >= .35) or 235 <= hue <= 330):
                findings.append({'path': path.relative_to(root).as_posix(), 'line': source.count('\n', 0, match.start()) + 1, 'literal': match.group()})
    return {'status': 'FAIL' if findings else 'PASS', 'files': len(files), 'literalOccurrences': literals, 'findings': findings}


if __name__ == '__main__':
    result = audit(Path(__file__).resolve().parents[1])
    raw = json.dumps(result, indent=2) + '\n'
    if len(sys.argv) > 1: Path(sys.argv[1]).write_text(raw, encoding='utf-8')
    else: print(raw, end='')
    sys.exit(0 if result['status'] == 'PASS' else 1)
