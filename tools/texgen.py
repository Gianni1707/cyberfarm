"""Pixel-art texture generator for CyberFarm (pure Python, no dependencies).

Every texture is drawn in code with a shared clean sci-fi palette: block faces from a
procedural panel plus details, items from ASCII grids, the drone entity from its box UV layout.

Run from the project root:
    python tools/texgen.py                 writes all textures
    python tools/texgen.py --preview DIR   also writes an enlarged preview sheet to DIR
"""
import math
import random
import struct
import sys
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TEXTURES = ROOT / "src/main/resources/assets/cyberfarm/textures"

PALETTE = {
    "K": "#0b0e11",  # deep outline
    "k": "#14181d",  # outline
    "s": "#101418",  # screen / recess
    "D": "#22282e",  # shadow panel
    "d": "#2c333b",  # dark panel
    "m": "#4a5561",  # mid panel
    "l": "#7d8a96",  # light panel
    "w": "#c9d3dc",  # white edge / steel
    "b": "#0e7c99",  # deep cyan
    "c": "#2fe0ff",  # cyan glow
    "C": "#b8f6ff",  # bright core
    "o": "#ff8c1a",  # orange
    "O": "#ffd04d",  # amber
    "r": "#a8321f",  # red
    "g": "#3fbf6f",  # green
    "y": "#e0b020",  # hazard yellow
    "G": "#9dffb8",  # light green
    "v": "#8a4fd8",  # violet (elite tier)
    "V": "#d6b8ff",  # light violet
}
GLOW_KEYS = {"c", "C"}


def rgba(color):
    if color is None:
        return None
    if isinstance(color, tuple):
        return color
    value = PALETTE.get(color, color).lstrip("#")
    return (int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), 255)


class Canvas:
    def __init__(self, width=16, height=16):
        self.width, self.height = width, height
        self.pixels = [[None] * width for _ in range(height)]
        self.keys = [[None] * width for _ in range(height)]  # palette key per pixel, for the glow mask

    def set(self, x, y, color, shade=0):
        if not (0 <= x < self.width and 0 <= y < self.height):
            return
        value = rgba(color)
        if value is not None and shade:
            value = tuple(max(0, min(255, ch + shade)) for ch in value[:3]) + (value[3],)
        self.pixels[y][x] = value
        self.keys[y][x] = color if isinstance(color, str) else None

    def rect(self, x0, y0, x1, y1, color, noise=0, seed=0):
        """Fills the inclusive rectangle, optionally with a subtle brightness noise."""
        rng = random.Random(seed * 7919 + x0 * 31 + y0)
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, color, rng.randint(-noise, noise) if noise else 0)

    def frame(self, x0, y0, x1, y1, color):
        self.rect(x0, y0, x1, y0, color)
        self.rect(x0, y1, x1, y1, color)
        self.rect(x0, y0, x0, y1, color)
        self.rect(x1, y0, x1, y1, color)

    def disc(self, cx, cy, r_outer, color, r_inner=-1.0):
        for y in range(self.height):
            for x in range(self.width):
                dist = math.hypot(x - cx, y - cy)
                if r_inner <= dist <= r_outer:
                    self.set(x, y, color)

    def ascii(self, x0, y0, rows):
        for dy, row in enumerate(rows):
            for dx, ch in enumerate(row):
                if ch not in ". ":
                    self.set(x0 + dx, y0 + dy, ch)

    def glow_mask(self):
        glow = Canvas(self.width, self.height)
        for y in range(self.height):
            for x in range(self.width):
                if self.keys[y][x] in GLOW_KEYS:
                    glow.pixels[y][x] = self.pixels[y][x]
        return glow

    def save(self, path):
        raw = bytearray()
        for row in self.pixels:
            raw.append(0)
            for px in row:
                raw.extend(px if px is not None else (0, 0, 0, 0))

        def chunk(tag, data):
            body = tag + data
            return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

        png = b"\x89PNG\r\n\x1a\n"
        png += chunk(b"IHDR", struct.pack(">IIBBBBB", self.width, self.height, 8, 6, 0, 0, 0))
        png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        png += chunk(b"IEND", b"")
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(png)


def recolor(rows, mapping):
    return ["".join(mapping.get(ch, ch) for ch in row) for row in rows]


def from_ascii(rows, size=16):
    canvas = Canvas(size, size)
    for y, row in enumerate(rows):
        if len(row) != size:
            raise ValueError(f"row {y} has {len(row)} pixels, expected {size}: {row!r}")
    canvas.ascii(0, 0, rows)
    return canvas


# --------------------------------------------------------------------------- blocks

def panel(seed, base="d"):
    """Standard machine face: outlined, beveled metal plate with corner screws."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "k")
    c.rect(1, 1, 14, 14, base, noise=4, seed=seed)
    c.rect(1, 1, 14, 1, "m")
    c.rect(1, 1, 1, 14, "m")
    c.rect(2, 14, 14, 14, "D")
    c.rect(14, 2, 14, 14, "D")
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        c.set(x, y, "l")
    return c


def light_strip(c, y, x0=3, x1=12):
    c.rect(x0, y, x1, y, "b")
    c.rect(x0 + 2, y, x1 - 2, y, "c")


def machine_side():
    c = panel(1)
    for y in (4, 6, 8):
        c.rect(4, y, 11, y, "K")
    light_strip(c, 11)
    return c


def machine_bottom():
    c = panel(2, base="D")
    c.rect(6, 6, 9, 9, "k")
    return c


def machine_top():
    c = panel(3)
    c.frame(4, 4, 11, 11, "K")
    c.rect(5, 5, 10, 10, "m", noise=3, seed=3)
    c.rect(7, 7, 8, 8, "b")
    return c


def generator_front(lit):
    c = panel(4)
    c.rect(3, 3, 12, 9, "K")
    if lit:
        for y, color in ((4, "r"), (5, "o"), (6, "o"), (7, "O"), (8, "O")):
            c.rect(4, y, 11, y, color, noise=12, seed=y)
    else:
        c.rect(4, 4, 11, 8, "s")
    for x in (5, 8, 11):
        c.rect(x, 4, x, 8, "m")
    c.set(4, 12, "g" if lit else "r")
    c.rect(7, 12, 12, 12, "c" if lit else "b")
    return c


def generator_top():
    c = panel(5)
    c.disc(7.5, 7.5, 4.8, "K")
    c.disc(7.5, 7.5, 3.8, "s")
    c.rect(4, 7, 11, 8, "m")
    c.rect(7, 4, 8, 11, "m")
    c.rect(7, 7, 8, 8, "l")
    return c


def assembler_front():
    c = panel(6)
    c.rect(2, 2, 13, 10, "K")
    c.rect(3, 3, 12, 9, "s")
    c.ascii(4, 4, [
        "cc....cc",
        "..cbbc..",
        "..bccb..",
        "..cbbc..",
        "cc....cc",
    ])
    c.set(3, 12, "c")
    c.set(5, 12, "g")
    c.set(7, 12, "O")
    c.rect(9, 12, 12, 12, "K")
    return c


def assembler_top():
    c = panel(7)
    c.rect(2, 2, 13, 13, "K")
    c.rect(3, 3, 12, 12, "s", noise=2, seed=7)
    c.rect(7, 5, 8, 10, "b")
    c.rect(5, 7, 10, 8, "b")
    c.rect(7, 7, 8, 8, "c")
    for (x, y, dx, dy) in ((3, 3, 1, 1), (12, 3, -1, 1), (3, 12, 1, -1), (12, 12, -1, -1)):
        c.set(x, y, "c")
        c.set(x + dx, y, "c")
        c.set(x, y + dy, "c")
    return c


def assembler_side():
    c = panel(8)
    c.rect(3, 3, 3, 12, "b")
    c.rect(3, 5, 3, 10, "c")
    for y in (4, 6, 8, 10):
        c.rect(6, y, 12, y, "K")
    return c


def dock_top():
    c = Canvas()
    c.rect(0, 0, 15, 15, "k")
    c.rect(1, 1, 14, 14, "D", noise=3, seed=9)
    c.disc(7.5, 7.5, 6.6, "b", r_inner=5.6)
    for x, y in ((7, 1), (8, 1), (7, 14), (8, 14), (1, 7), (1, 8), (14, 7), (14, 8)):
        c.set(x, y, "c")
    c.ascii(6, 5, ["c..c", "c..c", "cccc", "cccc", "c..c", "c..c"])
    for x, y, dx, dy in ((2, 2, 1, 1), (13, 2, -1, 1), (2, 13, 1, -1), (13, 13, -1, -1)):
        c.set(x, y, "y")
        c.set(x + dx, y, "y")
        c.set(x, y + dy, "y")
    return c


def dock_side():
    c = panel(10)
    c.rect(0, 10, 15, 10, "k")
    c.rect(0, 11, 15, 14, "d", noise=3, seed=10)
    c.rect(0, 15, 15, 15, "k")
    c.rect(1, 12, 14, 12, "b")
    for x in (3, 7, 11):
        c.rect(x, 12, x + 1, 12, "c")
    return c


def dock_beacon():
    c = Canvas()
    c.rect(0, 0, 15, 15, "m", noise=3, seed=11)
    c.rect(0, 0, 2, 0, "c")      # side faces: lit rim on top
    c.rect(0, 1, 2, 2, "l")
    c.rect(4, 0, 6, 2, "c")      # top face: lamp
    c.set(5, 1, "C")
    return c


# --------------------------------------------------------------------------- items

ITEM_ASCII = {
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
    "item/basic_frame": [
        "................",
        "................",
        "..kkkk....kkkk..",
        "..kllk....kllk..",
        "..klmkkkkkkmlk..",
        "..kkmmmmmmmmkk..",
        "....km....mk....",
        "....km.cc.mk....",
        "....km.cc.mk....",
        "....km....mk....",
        "..kkmmmmmmmmkk..",
        "..klmkkkkkkmlk..",
        "..kllk....kllk..",
        "..kkkk....kkkk..",
        "................",
        "................",
    ],
    "item/rotor": [
        "................",
        ".ww..........ww.",
        ".wlk........klw.",
        "..lmk......kml..",
        "...kmk....kmk...",
        "....kmkkkkmk....",
        ".....kkbbkk.....",
        ".....kbccbk.....",
        ".....kbccbk.....",
        ".....kkbbkk.....",
        "....kmkkkkmk....",
        "...kmk....kmk...",
        "..lmk......kml..",
        ".wlk........klw.",
        ".ww..........ww.",
        "................",
    ],
    "item/battery_cell": [
        "................",
        "......kkkk......",
        "......kllk......",
        "....kkkkkkkk....",
        "....kmwmmmmk....",
        "....kmbbbbmk....",
        "....kmccccmk....",
        "....kmbbbbmk....",
        "....kmccccmk....",
        "....kmbbbbmk....",
        "....kmccccmk....",
        "....kmbbbbmk....",
        "....kmccccmk....",
        "....kmmmmmmk....",
        "....kkkkkkkk....",
        "................",
    ],
    "item/harvester_module": [
        "................",
        "................",
        "......kkkk......",
        "......kmmk......",
        "....kkkkkkkk....",
        "....kmllllmk....",
        "....kmbccbmk....",
        "....kmmmmmmk....",
        "....kkkkkkkk....",
        "..kkkkkkkkkkkk..",
        "..kwwwwwwwwwwk..",
        "..kllllllllllk..",
        "..kwkkwkkwkkwk..",
        "...k..k..k..k...",
        "................",
        "................",
    ],
    "item/quarry_module": [
        "................",
        "......kkkk......",
        "......kmmk......",
        "....kkkkkkkk....",
        "....kmbccbmk....",
        "....kmmmmmmk....",
        "....kkkkkkkk....",
        "....kwlwlwlk....",
        "....klwlwlwk....",
        ".....kwlwlk.....",
        ".....klwlwk.....",
        "......kwlk......",
        "......klwk......",
        ".......kk.......",
        "................",
        "................",
    ],
    "item/transport_module": [
        "................",
        "......kkkk......",
        "......kmmk......",
        "...kkkkkkkkkk...",
        "...kmmbccbmmk...",
        "...kkkkkkkkkk...",
        "...km......mk...",
        "...km.kkkk.mk...",
        "...km.kOOk.mk...",
        "...kmkkOOkkmk...",
        "...kl.kkkk.lk...",
        "...kl......lk...",
        "...kk......kk...",
        "................",
        "................",
        "................",
    ],
    "item/drone": [
        "................",
        ".kkk........kkk.",
        ".kwk........kwk.",
        ".kkkk......kkkk.",
        "...kmk....kmk...",
        "....kmkkkkmk....",
        ".....kmccmk.....",
        ".....kcCCck.....",
        ".....kcCCck.....",
        ".....kmccmk.....",
        "....kmkkkkmk....",
        "...kmk....kmk...",
        ".kkkk......kkkk.",
        ".kwk........kwk.",
        ".kkk........kkk.",
        "................",
    ],
    "item/survey_tool": [
        "................",
        ".....k..........",
        ".....kc.........",
        "......k.........",
        "....kkkkkkkk....",
        "....kssssssk....",
        "....kscbbcsk....",
        "....ksbccbsk....",
        "....kscbbcsk....",
        "....kssssssk....",
        "....kmmmmmmk....",
        "....kmcmmOmk....",
        "....kkkkkkkk....",
        "......kmmk......",
        "......kmmk......",
        "......kkkk......",
    ],
    "item/handbook": [
        "................",
        "..kkkkkkkkkkk...",
        "..kbbbbbbbbbkw..",
        "..kbccccccbbkw..",
        "..kbcbbbbcbbkw..",
        "..kbcbCCbcbbkw..",
        "..kbcbCCbcbbkw..",
        "..kbcbbbbcbbkw..",
        "..kbccccccbbkw..",
        "..kbbbbbbbbbkw..",
        "..kbbbbbbbbbkw..",
        "..kbbbbbbbbbkw..",
        "..kbbbbbbbbbkw..",
        "..kkkkkkkkkkkw..",
        "...wwwwwwwwwww..",
        "................",
    ],
}


# Addon chips: a shared cartridge with a 6x6 icon in the window
ADDON_CHIP = [
    "................",
    "................",
    "..kkkkkkkkkkkk..",
    "..kllllllllllk..",
    "..klssssssssmk..",
    "..klssssssssmk..",
    "..klssssssssmk..",
    "..klssssssssmk..",
    "..klssssssssmk..",
    "..klssssssssmk..",
    "..klssssssssmk..",
    "..klssssssssmk..",
    "..kmmmmmmmmmmk..",
    "..kkkkkkkkkkkk..",
    "...y.y.y.y.y....",
    "................",
]

ADDON_ICONS = {
    "item/speed_addon": [
        "c..c..",
        ".c..c.",
        "..c..c",
        "..c..c",
        ".c..c.",
        "c..c..",
    ],
    "item/stack_addon": [
        "OOOOOO",
        "......",
        "OOOOOO",
        "......",
        "OOOOOO",
        "......",
    ],
    "item/battery_addon": [
        "..gg..",
        ".gggg.",
        ".gGGg.",
        ".gggg.",
        ".gGGg.",
        ".gggg.",
    ],
    "item/fortune_addon": [
        "..gg..",
        ".gGGg.",
        "gGGGGg",
        "gggggg",
        ".gggg.",
        "..gg..",
    ],
    "item/silk_touch_addon": [
        "....VV",
        "...VVv",
        "..VVv.",
        ".VVv..",
        ".Vv...",
        "v.....",
    ],
}


def addon_chip(icon):
    c = from_ascii(ADDON_CHIP)
    c.ascii(5, 5, icon)
    return c


# --------------------------------------------------------------------------- drone entity

def box_faces(u, v, w, h, d):
    """Texture regions (x0, y0, x1, y1 inclusive) of a model box, in Minecraft's box UV layout."""
    faces = {
        "top": (u + d, v, u + d + w - 1, v + d - 1),
        "bottom": (u + d + w, v, u + d + 2 * w - 1, v + d - 1),
        "right": (u, v + d, u + d - 1, v + d + h - 1),
        "front": (u + d, v + d, u + d + w - 1, v + d + h - 1),
        "left": (u + d + w, v + d, u + 2 * d + w - 1, v + d + h - 1),
        "back": (u + 2 * d + w, v + d, u + 2 * d + 2 * w - 1, v + d + h - 1),
    }
    return {name: r for name, r in faces.items() if r[2] >= r[0] and r[3] >= r[1]}


def paint_box(c, u, v, w, h, d, side, top=None, bottom=None, seed=0):
    faces = box_faces(u, v, w, h, d)
    for name, (x0, y0, x1, y1) in faces.items():
        color = {"top": top or side, "bottom": bottom or side}.get(name, side)
        c.rect(x0, y0, x1, y1, color, noise=3, seed=seed)
    return faces


def side_rows(c, faces, rows):
    """Paints the side faces row by row (rows[0] is the top row)."""
    for name in ("right", "front", "left", "back"):
        if name in faces:
            x0, y0, x1, y1 = faces[name]
            for i, color in enumerate(rows):
                if y0 + i <= y1:
                    c.rect(x0, y0 + i, x1, y0 + i, color)


def drone_texture():
    c = Canvas(64, 64)
    # Hull 8x3x8: light top band, glowing strip, dark base
    hull = paint_box(c, 0, 0, 8, 3, 8, "m", top="m", bottom="d", seed=1)
    side_rows(c, hull, ["l", "c", "d"])
    x0, y0, x1, y1 = hull["top"]
    c.frame(x0, y0, x1, y1, "d")
    c.set(x0 + 1, y0 + 1, "l"); c.set(x1 - 1, y0 + 1, "l"); c.set(x0 + 1, y1 - 1, "l"); c.set(x1 - 1, y1 - 1, "l")
    bx0, by0, bx1, by1 = hull["bottom"]
    c.rect(bx0 + 3, by0 + 3, bx0 + 4, by0 + 4, "b")
    # Core dome 5x2x5: glowing
    dome = paint_box(c, 0, 11, 5, 2, 5, "b", top="c", seed=2)
    side_rows(c, dome, ["c", "b"])
    tx0, ty0, _, _ = dome["top"]
    c.rect(tx0 + 1, ty0 + 1, tx0 + 3, ty0 + 3, "c")
    c.set(tx0 + 2, ty0 + 2, "C")
    # Front sensor 3x2x1
    sensor = paint_box(c, 20, 11, 3, 2, 1, "k", seed=3)
    fx0, fy0, fx1, fy1 = sensor["front"]
    c.rect(fx0, fy0, fx1, fy1, "C")
    # Arms 4x1x2 and motors 2x2x2
    paint_box(c, 0, 18, 4, 1, 2, "d", top="m", seed=4)
    motor = paint_box(c, 16, 18, 2, 2, 2, "D", top="m", bottom="k", seed=5)
    side_rows(c, motor, ["l", "k"])
    # Rotor blades (zero-height boxes): steel with cyan tips
    for u, v, w, d in ((0, 24, 6, 1), (0, 26, 1, 6)):
        for name, (x0, y0, x1, y1) in box_faces(u, v, w, 0, d).items():
            c.rect(x0, y0, x1, y1, "w")
            if w > d:
                c.set(x0, y0, "b"); c.set(x1, y1, "b")
            else:
                c.set(x0, y0, "b"); c.set(x1, y1, "b")
    # Landing skids 1x2x6
    for u in (0, 14):
        skid = paint_box(c, u, 32, 1, 2, 6, "d", top="m", bottom="k", seed=6)
        side_rows(c, skid, ["m", "k"])
    # Harvester head 4x2x4 with hazard stripes, and its blade 6x1x1
    head = paint_box(c, 32, 0, 4, 2, 4, "m", bottom="k", seed=7)
    for name in ("right", "front", "left", "back"):
        x0, y0, x1, y1 = head[name]
        for x in range(x0, x1 + 1):
            c.rect(x, y0, x, y1, "y" if (x - y0) % 2 == 0 else "k")
    hx0, hy0, hx1, hy1 = head["bottom"]
    c.rect(hx0 + 1, hy0 + 1, hx1 - 1, hy1 - 1, "c")
    paint_box(c, 32, 6, 6, 1, 1, "w", top="l", seed=8)
    # Quarry drill: base 2x1x2 and bit 1x2x1, steel with a cyan ring
    paint_box(c, 48, 0, 2, 1, 2, "m", top="l", bottom="b", seed=9)
    bit = paint_box(c, 56, 0, 1, 2, 1, "w", bottom="l", seed=10)
    side_rows(c, bit, ["w", "l"])
    # Transport claw: body 4x1x2 and two amber prongs 1x2x1
    claw = paint_box(c, 32, 10, 4, 1, 2, "d", top="m", bottom="k", seed=11)
    side_rows(c, claw, ["b"])
    prong = paint_box(c, 44, 10, 1, 2, 1, "y", bottom="k", seed=12)
    side_rows(c, prong, ["k", "y"])
    return c


# --------------------------------------------------------------------------- output

def build_all():
    textures = {
        "block/machine_side": machine_side(),
        "block/machine_bottom": machine_bottom(),
        "block/machine_top": machine_top(),
        "block/combustion_generator_front": generator_front(False),
        "block/combustion_generator_front_on": generator_front(True),
        "block/combustion_generator_top": generator_top(),
        "block/drone_assembler_front": assembler_front(),
        "block/drone_assembler_top": assembler_top(),
        "block/drone_assembler_side": assembler_side(),
        "block/drone_dock_top": dock_top(),
        "block/drone_dock_side": dock_side(),
        "block/drone_dock_beacon": dock_beacon(),
    }
    for name, rows in ITEM_ASCII.items():
        textures[name] = from_ascii(rows)
    for name, icon in ADDON_ICONS.items():
        textures[name] = addon_chip(icon)
    # Higher frame tiers reuse the basic frame with their own accent colors
    for name, mapping in (("item/advanced_frame", {"l": "O", "c": "o"}), ("item/elite_frame", {"l": "V", "m": "v", "c": "C"})):
        textures[name] = from_ascii(recolor(ITEM_ASCII["item/basic_frame"], mapping))
    drone = drone_texture()
    textures["entity/drone"] = drone
    textures["entity/drone_glow"] = drone.glow_mask()
    return textures


def write_preview(textures, out_dir, scale=8):
    """Lays every texture out on one sheet (scaled up) on a checkered background."""
    out_dir = Path(out_dir)
    cell = 64 * scale // 2
    names = list(textures)
    cols = 6
    rows = (len(names) + cols - 1) // cols
    sheet = Canvas(cols * (cell + 8), rows * (cell + 8))
    for y in range(sheet.height):
        for x in range(sheet.width):
            sheet.pixels[y][x] = (90, 90, 96, 255) if ((x // 8) + (y // 8)) % 2 else (110, 110, 118, 255)
    for i, name in enumerate(names):
        tex = textures[name]
        s = cell // tex.width
        ox = (i % cols) * (cell + 8) + 4
        oy = (i // cols) * (cell + 8) + 4
        for y in range(tex.height):
            for x in range(tex.width):
                px = tex.pixels[y][x]
                if px is None:
                    continue
                for yy in range(s):
                    for xx in range(s):
                        sheet.pixels[oy + y * s + yy][ox + x * s + xx] = px
    sheet.save(out_dir / "texture_preview.png")
    (out_dir / "texture_preview.txt").write_text("\n".join(f"{i}: {n}" for i, n in enumerate(names)))


if __name__ == "__main__":
    all_textures = build_all()
    for texture_name, canvas in all_textures.items():
        canvas.save(TEXTURES / f"{texture_name}.png")
    print(f"wrote {len(all_textures)} textures to {TEXTURES.relative_to(ROOT)}")
    if "--preview" in sys.argv:
        write_preview(all_textures, sys.argv[sys.argv.index("--preview") + 1])
