"""Pixel-art texture generator for CyberFarm (pure Python, no dependencies).

Each texture is an ASCII grid where every character maps to a palette color.
Run:  python tools/texgen.py   (from the project root)
"""
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TEXTURES = ROOT / "src/main/resources/assets/cyberfarm/textures"

# Clean sci-fi palette shared by all textures.
PALETTE = {
    ".": None,          # transparent
    "k": "#14181d",     # outline
    "d": "#2c333b",     # dark panel
    "m": "#4a5561",     # mid panel
    "l": "#7d8a96",     # light panel / highlight
    "w": "#c9d3dc",     # white edge
    "b": "#0e7c99",     # deep cyan
    "c": "#2fe0ff",     # cyan glow
    "C": "#b8f6ff",     # bright core
}

TEXTURE_DEFS = {
    "item/drone_core": [
        "................",
        ".....kkkkkk.....",
        "...kkmmmmmmkk...",
        "..kmlllmmmmmdk..",
        ".kmllddddddmmdk.",
        ".kmldbbbbbbdmdk.",
        "kmldbbccccbbdmdk",
        "kmldbcCCCCcbdmdk",
        "kmldbcCCCCcbdmdk",
        "kmldbbccccbbdmdk",
        ".kmdbbbbbbbbddk.",
        ".kmdddddddddddk.",
        "..kddddddddddk..",
        "...kkddddddkk...",
        ".....kkkkkk.....",
        "................",
    ],
}


def hex_to_rgba(value):
    if value is None:
        return (0, 0, 0, 0)
    value = value.lstrip("#")
    return (int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), 255)


def write_png(path, rows):
    height, width = len(rows), len(rows[0])
    raw = bytearray()
    for y, row in enumerate(rows):
        if len(row) != width:
            raise ValueError(f"{path}: row {y} has {len(row)} pixels, expected {width}")
        raw.append(0)  # filter: none
        for ch in row:
            raw.extend(hex_to_rgba(PALETTE[ch]))

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


if __name__ == "__main__":
    for name, rows in TEXTURE_DEFS.items():
        out = TEXTURES / f"{name}.png"
        write_png(out, rows)
        print(f"wrote {out.relative_to(ROOT)}")
