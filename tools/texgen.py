"""Pixel-art texture generator for CyberFarm (pure Python, no dependencies).

Every texture is drawn in code with a shared clean sci-fi palette: block faces from a
procedural panel plus details, items from ASCII grids, the drone entity from its box UV layout.

Run from the project root:
    python tools/texgen.py                 writes all textures
    python tools/texgen.py --preview DIR   also writes an enlarged preview sheet to DIR
"""
import inspect
import json
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
        self.animation = None  # set by strip(): written next to the PNG as its .mcmeta

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

    def glow_mask(self, keys=GLOW_KEYS):
        glow = Canvas(self.width, self.height)
        glow.animation = self.animation
        for y in range(self.height):
            for x in range(self.width):
                if self.keys[y][x] in keys:
                    glow.pixels[y][x] = self.pixels[y][x]
        return glow

    def copy(self):
        out = Canvas(self.width, self.height)
        out.pixels = [list(row) for row in self.pixels]
        out.keys = [list(row) for row in self.keys]
        return out

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


def assembler_base_side():
    """Drone Assembler base, a slab four pixels tall: only the bottom four rows show."""
    c = panel(8)
    c.rect(1, 12, 14, 12, "m")
    c.rect(1, 13, 14, 14, "d", noise=3, seed=8)
    light_strip(c, 13, 2, 13)
    c.rect(0, 15, 15, 15, "k")
    return c


def assembler_base_front():
    """The base's front: the control panel, with its status lights and a readout strip."""
    c = assembler_base_side()
    c.rect(1, 13, 14, 14, "s")
    c.set(2, 13, "c")
    c.set(4, 13, "g")
    c.set(6, 13, "O")
    c.rect(8, 13, 13, 13, "b")
    c.rect(9, 13, 11, 13, "c")
    return c


def assembler_pad():
    """The top of the base, where the drone takes shape: a landing ring with hazard corners."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "k")
    c.rect(1, 1, 14, 14, "D", noise=3, seed=6)
    c.disc(7.5, 7.5, 5.8, "b", r_inner=4.9)
    c.disc(7.5, 7.5, 4.0, "s")
    c.rect(7, 4, 8, 11, "K")
    c.rect(4, 7, 11, 8, "K")
    c.rect(7, 7, 8, 8, "c")
    for x, y, dx, dy in ((1, 1, 1, 1), (14, 1, -1, 1), (1, 14, 1, -1), (14, 14, -1, -1)):
        c.set(x, y, "y")
        c.set(x + dx, y, "y")
        c.set(x, y + dy, "y")
    return c


def assembler_pillar(frame=None):
    """A corner pillar (faces show columns 7-8, rows 2-11): steel with a light line; while working
    a bright spark runs up it, once round per 16 frames."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "l", noise=3, seed=9)
    c.rect(7, 2, 7, 11, "w")
    c.rect(8, 2, 8, 11, "c")
    if frame is not None:
        head = 11 - frame * 10 // 16
        for y in (head, head + 1):
            c.set(8, 2 + (y - 2) % 10, "C")
    return c


def assembler_cap_side():
    """The cap's edge, two pixels tall: only the top two rows show."""
    c = panel(10)
    c.rect(0, 0, 15, 0, "l")
    c.rect(0, 1, 15, 1, "k")
    return c


def assembler_emitter():
    """The cap's underside, looking down into the chamber: the assembly emitter."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "k")
    c.rect(1, 1, 14, 14, "D", noise=3, seed=12)
    c.disc(7.5, 7.5, 5.0, "K")
    c.disc(7.5, 7.5, 4.2, "b", r_inner=3.2)
    c.disc(7.5, 7.5, 2.4, "c")
    c.disc(7.5, 7.5, 1.0, "C")
    return c


def cyber_frame():
    """Structural frame: thick beveled border, recessed centre, cyan corner lights."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "k")
    c.rect(1, 1, 14, 14, "m", noise=4, seed=20)
    c.rect(1, 1, 14, 1, "l")
    c.rect(1, 1, 1, 14, "l")
    c.rect(4, 4, 11, 11, "k")
    c.rect(5, 5, 10, 10, "D", noise=3, seed=21)
    c.rect(5, 7, 10, 8, "d")
    c.rect(7, 5, 8, 10, "d")
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        c.set(x, y, "c")
    return c


def cyber_frame_formed():
    """A frame of a formed multiblock: energy lines along the border and a glowing core."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "k")
    c.rect(1, 1, 14, 14, "d", noise=4, seed=20)
    c.rect(1, 1, 14, 1, "m")
    c.rect(1, 1, 1, 14, "m")
    c.frame(2, 2, 13, 13, "b")
    for x, y in ((7, 2), (8, 2), (7, 13), (8, 13), (2, 7), (2, 8), (13, 7), (13, 8)):
        c.set(x, y, "c")
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        c.set(x, y, "C")
    c.frame(3, 3, 12, 12, "K")
    c.rect(4, 4, 11, 11, "s")
    c.rect(7, 4, 8, 11, "b")
    c.rect(4, 7, 11, 8, "b")
    c.rect(6, 6, 9, 9, "c")
    c.rect(7, 7, 8, 8, "C")
    return c


def muffler():
    """Padded sound-dampening panel with a covered speaker grille."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "k")
    c.rect(1, 1, 14, 14, "D", noise=3, seed=40)
    for i in range(1, 15, 4):  # quilted padding seams
        c.rect(i, 1, i, 14, "d")
        c.rect(1, i, 14, i, "d")
    c.disc(7.5, 7.5, 5.2, "K")
    c.disc(7.5, 7.5, 4.3, "s")
    for y in range(4, 12, 2):
        for x in range(4, 12, 2):
            if math.hypot(x + 0.5 - 7.5, y + 0.5 - 7.5) < 4.0:
                c.set(x + 1, y + 1, "m")
    c.rect(5, 7, 10, 8, "b")
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        c.set(x, y, "l")
    return c


# --------------------------------------------------------------------------- nuclear

URANIUM = "#5fdc3a"
URANIUM_DARK = "#2f7a22"
CONCRETE = "#a7adb2"


def uranium_ore_overlay():
    """Transparent overlay drawn over vanilla stone or deepslate: glowing uranium specks."""
    c = Canvas()
    for cx, cy in ((3, 3), (11, 4), (6, 9), (12, 11), (3, 13)):
        c.rect(cx, cy, cx + 1, cy + 1, URANIUM_DARK)
        c.set(cx, cy, URANIUM)
        c.set(cx + 1, cy + 1, "G")
    return c


def reactor_casing(formed):
    """Cooling-tower concrete with seams; once formed, cyan status lines light up along the seams."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "#7d848a")
    c.rect(1, 1, 14, 14, CONCRETE, noise=6, seed=50)
    for y in (5, 10):
        c.rect(1, y, 14, y, "#8c9298")
    c.rect(1, 1, 14, 1, "#c3c8cc")
    if formed:
        c.rect(3, 5, 12, 5, "b")
        c.rect(5, 5, 10, 5, "c")
        for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
            c.set(x, y, "c")
    return c


def reactor_port(formed):
    c = reactor_casing(formed)
    c.disc(7.5, 7.5, 5.2, "K")
    c.disc(7.5, 7.5, 4.2, "c" if formed else "y")
    c.disc(7.5, 7.5, 3.0, "s")
    c.rect(7, 4, 8, 11, "m")
    c.rect(4, 7, 11, 8, "m")
    return c


def reactor_controller(state):
    """state: 'off' (incomplete), 'formed' (ready) or 'on' (burning)."""
    c = reactor_casing(state != "off")
    c.rect(2, 2, 13, 11, "K")
    c.rect(3, 3, 12, 10, "s")
    if state == "off":
        c.set(7, 6, "r")
        c.set(8, 6, "r")
    else:
        # Radiation trefoil on the screen, green when burning
        lit = URANIUM if state == "on" else "b"
        c.disc(7.5, 6.5, 1.2, lit)
        for (x0, y0, x1, y1) in ((6, 3, 9, 4), (3, 7, 5, 9), (10, 7, 12, 9)):
            c.rect(x0, y0, x1, y1, lit)
    c.rect(3, 13, 12, 13, "c" if state == "on" else "b" if state == "formed" else "r")
    return c


TURBINE_WHITE = "#dfe5e9"


def turbine_casing(formed):
    """White turbine plating with a cyan stripe; the stripe lights up once the hall is formed."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "#8f989f")
    c.rect(1, 1, 14, 14, TURBINE_WHITE, noise=4, seed=61)
    c.rect(1, 1, 14, 1, "#f4f7f9")
    c.rect(1, 7, 14, 8, "b" if formed else "#9aa4ab")
    if formed:
        c.rect(4, 7, 11, 8, "c")
    for x, y in ((2, 3), (13, 3), (2, 12), (13, 12)):
        c.set(x, y, "m")
    return c


def turbine_port(formed):
    c = turbine_casing(formed)
    c.disc(7.5, 7.5, 5.2, "K")
    c.disc(7.5, 7.5, 4.2, "c" if formed else "y")
    c.disc(7.5, 7.5, 3.0, "s")
    c.rect(7, 4, 8, 11, "m")
    c.rect(4, 7, 11, 8, "m")
    return c


def turbine_controller(state):
    """state: 'off' (incomplete), 'formed' (ready) or 'on' (spinning): a screen with the rotor on it."""
    c = turbine_casing(state != "off")
    c.rect(2, 2, 13, 12, "K")
    c.rect(3, 3, 12, 11, "s")
    blade = "c" if state == "on" else "b" if state == "formed" else "r"
    for i in range(-3, 4):
        c.set(round(7.5 + i), round(7 + i), blade)
        c.set(round(7.5 + i), round(7 - i), blade)
    c.disc(7.5, 7, 1.1, "w" if state != "off" else "m")
    c.rect(3, 13, 12, 13, "c" if state == "on" else "b" if state == "formed" else "r")
    return c


def multiblock_projector():
    """A blueprint tablet: a grid with the outline of a cooling tower, so it reads as 'shows a structure'."""
    c = Canvas()
    c.rect(1, 1, 14, 14, "k")
    c.rect(2, 2, 13, 13, (14, 52, 90, 255))
    for i in range(2, 14, 3):
        c.rect(i, 2, i, 13, (30, 80, 128, 255))
        c.rect(2, i, 13, i, (30, 80, 128, 255))
    for x0, x1, y in ((4, 11, 12), (5, 10, 11), (6, 9, 8), (6, 9, 7), (5, 10, 4), (4, 11, 3)):
        c.set(x0, y, "C")
        c.set(x1, y, "C")
    c.rect(4, 12, 11, 12, "C")
    c.rect(4, 3, 11, 3, "c")
    for y in (5, 6, 9, 10):
        c.set(5 if y in (5, 10) else 6, y, "c")
        c.set(10 if y in (5, 10) else 9, y, "c")
    c.rect(1, 14, 14, 14, "m")
    c.set(12, 14, "c")
    return c


def water_pump_front(frame=None):
    c = panel(52)
    c.rect(3, 3, 12, 12, "K")
    c.rect(4, 4, 11, 11, "#2a4f9a", noise=8, seed=52)
    for x, y in rising(((5, 9), (8, 6), (10, 10), (6, 5)), frame, 4, 8):
        c.set(x, y, "#7fb0ff")
    c.rect(3, 13, 12, 13, "b")
    return c


def water_pump_top():
    c = panel(53)
    c.disc(7.5, 7.5, 5.2, "K")
    c.disc(7.5, 7.5, 4.2, "#2a4f9a")
    c.rect(4, 7, 11, 8, "m")
    return c


# Uranium shades for the item sprites: outline, dark, mid, light, glow; then rock greys
URANIUM_SHADES = {"1": "#14200d", "2": "#26501a", "3": "#3c8a24", "4": "#62d13a", "5": "#c4ff9a",
                  "6": "#2b2c28", "7": "#454640", "8": "#62635b", "9": "#7f8077"}
PALETTE.update(URANIUM_SHADES)


def coolant_mixer_front():
    c = panel(54)
    c.rect(3, 3, 12, 12, "K")
    c.rect(4, 4, 7, 11, "#1fb8a8", noise=8, seed=54)
    c.rect(8, 4, 11, 11, "#a8e8ff", noise=6, seed=55)
    c.rect(7, 4, 8, 11, "m")
    c.rect(3, 13, 12, 13, "c")
    return c


# The vanilla bucket's shape: A..L are its metal greys, P..T the five shades of the fluid
BUCKET_TEMPLATE = [
    "................",
    ".....AAAAAA.....",
    "...AAEEHHHHAA...",
    "..AEPQSRRQQPEA..",
    "..APSTTSRTRSPA..",
    "..AAAPRSSRPAAA..",
    "..AJIAAAAAABFA..",
    "..AJJJJIIFBBFA..",
    "..AJJLJIIFBBIA..",
    "..AIJLJIIFBBIA..",
    "..ABJLJIIFBBIA..",
    "...AJJJIIFBFA...",
    "...AIJJIFFBFA...",
    "....AIJIFBFA....",
    ".....AAAAAA.....",
    "................",
]
BUCKET_METAL = {"A": "#353535", "B": "#727272", "E": "#787878", "F": "#969696",
                "H": "#989898", "I": "#a8a8a8", "J": "#d8d8d8", "L": "#ffffff"}


def bucket(fluid, gas):
    """A bucket of a CyberFarm fluid. Gases are held upside down, trapped against the bucket's bottom."""
    base = rgba(fluid)
    shades = {}
    for key, factor in zip("PQRST", (0.55, 0.7, 0.82, 0.95, 1.12)):
        shades[key] = tuple(max(0, min(255, int(ch * factor))) for ch in base[:3]) + (255,)
    rows = BUCKET_TEMPLATE[::-1] if gas else BUCKET_TEMPLATE
    c = Canvas()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in BUCKET_METAL:
                c.set(x, y, BUCKET_METAL[ch])
            elif ch in shades:
                c.set(x, y, shades[ch])
    return c


def cooling_tower(glow):
    """Skin of the formed reactor: white panels, seams, a grey footing and glowing cyan bands.

    32 px wide (8 segments around the tower) and 64 high (the 7 blocks, top of the texture at the rim).
    With glow, only the cyan pixels: the renderer draws them full-bright.
    """
    c = Canvas(32, 64)
    c.rect(0, 0, 31, 63, "#e6ecf0", noise=5, seed=60)
    for y in range(7, 64, 9):
        c.rect(0, y, 31, y, "#c3ccd3")
    c.rect(0, 0, 0, 63, "#c3ccd3")
    c.rect(16, 0, 16, 63, "#d2d9de")
    c.rect(0, 58, 31, 63, "#9aa4ac", noise=4, seed=61)
    c.rect(0, 57, 31, 57, "#7d8a96")
    # Cyan bands: one around the base, one under the rim, dashes in between
    c.rect(0, 53, 31, 54, "c")
    c.rect(0, 3, 31, 3, "c")
    c.rect(0, 4, 31, 4, "b")
    for x in (4, 20):
        c.rect(x, 24, x + 7, 24, "c")
        c.rect(x + 2, 24, x + 5, 24, "C")
    for x in (8, 24):
        c.set(x, 40, "C")
    return c.glow_mask() if glow else c


HAZMAT = "#e8c21f"
HAZMAT_DARK = "#a8860f"


def vitrified_waste():
    """Treated waste: dark green glass with faint glowing streaks."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "#1d3322", noise=6, seed=70)
    for x0, y0, length in ((2, 3, 5), (8, 9, 6), (4, 12, 4), (11, 2, 3)):
        for i in range(length):
            c.set(x0 + i, y0 + i // 2, "3")
    for x, y in ((5, 4), (11, 10), (6, 13)):
        c.set(x, y, "4")
    c.frame(0, 0, 15, 15, "#14261a")
    return c


def waste_reprocessor_front():
    c = panel(71)
    c.rect(3, 3, 12, 9, "K")
    c.rect(4, 4, 11, 8, "#1d3322")
    c.rect(5, 6, 10, 7, "4")
    c.rect(6, 6, 9, 6, "5")
    for x in range(3, 13):
        c.set(x, 11, "y" if (x // 2) % 2 == 0 else "K")
        c.set(x, 12, "K" if (x // 2) % 2 == 0 else "y")
    return c


def hazmat_item(piece):
    """Inventory icons of the yellow Hazmat Suit."""
    c = Canvas()
    if piece == "helmet":
        c.rect(3, 3, 12, 12, "k")
        c.rect(4, 4, 11, 11, HAZMAT)
        c.rect(4, 4, 11, 4, "#f4dc6a")
        c.rect(5, 6, 10, 9, "K")
        c.rect(6, 7, 9, 8, "b")
        c.set(6, 7, "c")
        c.rect(3, 12, 12, 12, HAZMAT_DARK)
    elif piece == "suit":
        c.rect(2, 2, 13, 14, "k")
        c.rect(3, 3, 12, 13, HAZMAT)
        c.rect(6, 2, 9, 4, "k")
        c.rect(6, 3, 9, 3, None)
        c.rect(1, 3, 2, 9, "k")
        c.rect(13, 3, 14, 9, "k")
        c.rect(7, 5, 8, 13, HAZMAT_DARK)
        c.rect(3, 10, 12, 10, "K")
        c.set(10, 6, "c")
    elif piece == "pants":
        c.rect(3, 2, 12, 14, "k")
        c.rect(4, 3, 11, 13, HAZMAT)
        c.rect(7, 7, 8, 14, None)
        c.rect(7, 7, 8, 7, "k")
        c.rect(6, 8, 6, 13, "k")
        c.rect(9, 8, 9, 13, "k")
        c.rect(4, 3, 11, 3, "K")
        c.rect(4, 12, 6, 13, HAZMAT_DARK)
        c.rect(9, 12, 11, 13, HAZMAT_DARK)
    else:  # boots
        for x0 in (2, 9):
            c.rect(x0, 6, x0 + 5, 13, "k")
            c.rect(x0 + 1, 7, x0 + 4, 12, HAZMAT)
            c.rect(x0 + 1, 11, x0 + 4, 12, "K")
    return c


HAZMAT_SHADE = "#c9a414"
HAZMAT_SEAM = "#8f720b"
SUIT_GREY = "#4a5058"
SUIT_GREY_DARK = "#2e3238"
REFLECTIVE = "#dfe5ea"


def _suit_box(c, u, v, w, h, d, seed):
    """A box of the suit: yellow, shaded sides, a darker back and seams along every face edge."""
    faces = box_faces(u, v, w, h, d)
    for name, (x0, y0, x1, y1) in faces.items():
        base = {"right": HAZMAT_SHADE, "left": HAZMAT_SHADE, "back": HAZMAT_SHADE, "bottom": HAZMAT_SEAM}.get(name, HAZMAT)
        c.rect(x0, y0, x1, y1, base, noise=3, seed=seed + len(name))
        # Seams only on the helmet: on the suit they clash with the zip, on 4-pixel limbs they look like stripes
        if x1 - x0 >= 7 and y1 - y0 == x1 - x0:
            c.frame(x0, y0, x1, y1, HAZMAT_SEAM)
    return faces


def _band(c, faces, row, color, names=("right", "front", "left", "back")):
    """A horizontal band at the given row of the side faces (row 0 is the top row of each face)."""
    for name in names:
        if name in faces:
            x0, y0, x1, y1 = faces[name]
            if y0 + row <= y1:
                c.rect(x0, y0 + row, x1, y0 + row, color)


def hazmat_layer(leggings):
    """Worn Hazmat Suit in the vanilla humanoid armor layout (64x32).

    Helmet with a dark visor, respirator and filters; suit with a zip, a radiation patch, reflective
    stripes and grey gloves; boots in grey rubber. The leggings layer has the trousers and the belt.
    """
    c = Canvas(64, 32)
    if leggings:
        legs = _suit_box(c, 0, 16, 4, 12, 4, 80)
        waist = _suit_box(c, 16, 16, 8, 12, 4, 81)
        # Above the boots, which cover the lower half
        _band(c, legs, 3, REFLECTIVE)
        _band(c, legs, 4, "#ffffff")
        _band(c, waist, 0, SUIT_GREY_DARK)
        _band(c, waist, 1, SUIT_GREY)
        return c

    # Helmet
    head = _suit_box(c, 0, 0, 8, 8, 8, 82)
    x0, y0, x1, y1 = head["front"]
    c.rect(x0 + 1, y0 + 1, x1 - 1, y0 + 4, SUIT_GREY_DARK)      # visor frame
    c.rect(x0 + 2, y0 + 2, x1 - 2, y0 + 4, "#16242c")           # dark glass
    c.set(x0 + 2, y0 + 2, "c")                                  # reflection
    c.set(x0 + 3, y0 + 2, "b")
    c.rect(x0 + 3, y0 + 5, x1 - 3, y0 + 7, SUIT_GREY)           # respirator
    c.set(x0 + 3, y0 + 6, SUIT_GREY_DARK)
    c.set(x1 - 3, y0 + 6, SUIT_GREY_DARK)
    for side in ("right", "left"):                              # filter canisters
        sx0, sy0, sx1, sy1 = head[side]
        c.rect(sx0 + 2, sy0 + 4, sx0 + 5, sy0 + 7, SUIT_GREY)
        c.rect(sx0 + 3, sy0 + 5, sx0 + 4, sy0 + 6, SUIT_GREY_DARK)

    # Suit body
    body = _suit_box(c, 16, 16, 8, 12, 4, 83)
    fx0, fy0, fx1, fy1 = body["front"]
    c.rect(fx0 + 3, fy0, fx0 + 4, fy1, HAZMAT_SEAM)             # zip
    c.rect(fx0 + 5, fy0 + 2, fx0 + 6, fy0 + 3, SUIT_GREY_DARK)  # radiation patch
    c.set(fx0 + 5, fy0 + 2, "#e8c21f")
    _band(c, body, 9, SUIT_GREY_DARK)                           # belt
    _band(c, body, 6, REFLECTIVE, ("front", "back"))

    # Arms: reflective stripes and grey gloves
    arms = _suit_box(c, 40, 16, 4, 12, 4, 84)
    _band(c, arms, 5, REFLECTIVE)
    _band(c, arms, 6, "#ffffff")
    for row in range(9, 12):
        _band(c, arms, row, SUIT_GREY if row < 11 else SUIT_GREY_DARK)

    # Boots: grey rubber up the shin
    boots = _suit_box(c, 0, 16, 4, 12, 4, 85)
    for row in range(6, 12):
        _band(c, boots, row, SUIT_GREY if row < 11 else SUIT_GREY_DARK)
    _band(c, boots, 6, SUIT_GREY_DARK)
    return c


def geiger_counter():
    c = Canvas()
    c.rect(2, 4, 11, 14, "k")
    c.rect(3, 5, 10, 13, HAZMAT)
    c.rect(4, 6, 9, 9, "K")
    c.rect(5, 7, 8, 8, "#c4ff9a")
    c.set(7, 8, "r")
    c.set(6, 7, "K")
    c.rect(4, 11, 5, 12, "K")
    c.rect(8, 11, 9, 12, "r")
    c.rect(12, 6, 12, 10, "K")
    c.rect(13, 2, 13, 7, "m")
    c.rect(12, 1, 14, 2, "l")
    return c


def radiation_icon():
    """Mob effect icon (18x18): the radiation trefoil, three black blades on yellow."""
    c = Canvas(18, 18)
    c.disc(8.5, 8.5, 8.4, "k")
    c.disc(8.5, 8.5, 7.5, "#e8c21f")
    for y in range(18):
        for x in range(18):
            dx, dy = x + 0.5 - 9.0, 9.0 - (y + 0.5)
            r = math.hypot(dx, dy)
            angle = math.degrees(math.atan2(dy, dx)) % 360
            blade = any(min(abs(angle - centre), 360 - abs(angle - centre)) <= 30 for centre in (90, 210, 330))
            if (2.6 <= r <= 6.6 and blade) or r <= 1.6:
                c.set(x, y, "K")
    return c


def raw_uranium():
    """A lump of dark ore rock with glowing uranium crystals, shaped like the vanilla raw ores."""
    return from_ascii([
        "................",
        "................",
        "......6666......",
        "....66799866....",
        "...6798888976...",
        "..679834487976..",
        "..6783455438976.",
        ".67884554437876.",
        ".67983443987876.",
        ".678898778834476",
        ".6778877783455 6".replace(" ", "4"),
        "..677787778344 6".replace(" ", "4"),
        "..66777777733766",
        "....6667777666..",
        ".......6666.....",
        "................",
    ])


def uranium_ingot():
    """The vanilla gold ingot shape, shaded in glowing uranium green."""
    return from_ascii([
        "................",
        "................",
        "..........11....",
        ".......111331...",
        "....1114444441..",
        ".11134444444431.",
        "1544444444445541",
        "1354444445554231",
        "1345445554322231",
        "1343554322223331",
        "134343222233311.",
        ".133432233111...",
        "..13333111......",
        "...1111.........",
        "................",
        "................",
    ])


def fuel_rod(depleted):
    """Steel rod with a uranium window; a depleted rod is dark with a hazard band."""
    c = Canvas()
    for i in range(12):
        x, y = 2 + i, 13 - i
        c.rect(x, y - 1, x + 1, y, "k")
        c.set(x, y - 1, "l" if not depleted else "m")
    for i in range(3, 9):
        x, y = 2 + i, 13 - i
        c.set(x + 1, y, "#3a3f2a" if depleted else URANIUM)
        c.set(x, y - 1, "y" if depleted and i in (4, 7) else ("#5c6146" if depleted else "G"))
    c.rect(1, 13, 2, 14, "w")
    c.rect(13, 1, 14, 2, "w")
    return c


def digester_front(frame=None):
    c = panel(22)
    c.rect(3, 2, 12, 12, "K")
    c.rect(4, 3, 11, 6, "s")
    c.rect(4, 7, 11, 11, "#4f9a2f", noise=8, seed=22)
    for x, y in ((6, 6), (9, 5)):
        c.set(x, y, "G")
    for x, y in rising(((5, 9), (8, 8), (10, 10)), frame, 7, 5):
        c.set(x, y, "G")
    c.rect(3, 13, 12, 13, "g")
    return c


def digester_top():
    c = panel(23)
    c.disc(7.5, 7.5, 5.2, "K")
    c.disc(7.5, 7.5, 4.2, "D")
    c.rect(4, 7, 11, 8, "m")
    c.rect(7, 7, 8, 8, "g")
    return c


def turbine_front():
    """Gas Turbine front: a dark duct round the burner; the fan in it is drawn by MovingPartsRenderer."""
    c = panel(24)
    c.disc(7.5, 7.5, 6.2, "K")
    c.disc(7.5, 7.5, 5.2, "s")
    c.disc(7.5, 7.5, 5.2, "D", r_inner=4.6)
    c.disc(7.5, 7.5, 1.6, "O")
    return c


def extractor_bottom(formed):
    """The shower head: faces down into the chamber."""
    c = panel(25)
    c.rect(3, 3, 12, 12, "K")
    c.rect(4, 4, 11, 11, "D")
    for y in range(4, 12, 2):
        for x in range(4, 12, 2):
            c.set(x + (y // 2) % 2, y, "C" if formed else "b")
    return c


def extractor_top(formed):
    c = panel(26)
    c.disc(7.5, 7.5, 3.6, "K")
    c.disc(7.5, 7.5, 2.6, "c" if formed else "b")
    c.rect(7, 7, 8, 8, "C" if formed else "s")
    return c


def extractor_side(formed):
    """Status strip: red while the chamber is incomplete, cyan once it is formed."""
    if formed:
        c = cyber_frame_formed()
        c.rect(4, 7, 11, 8, "c")
        c.rect(6, 7, 9, 8, "C")
        return c
    c = cyber_frame()
    c.rect(5, 7, 10, 8, "r")
    return c


# Pipe tiers are told apart by the rims of the sleeve: plain, gold, violet
# Cable sleeves: a light tube with the content as a coloured band down the middle. Arms use rows
# 6..9 (a straight run joins them into one tube), the coupling box at junctions columns/rows 5..10.
PIPE_CONTENT = {
    # main colour, highlight
    "energy": ((47, 224, 255, 255), (184, 246, 255, 255)),
    "gas": ((255, 176, 40, 255), (255, 226, 140, 255)),
    "liquid": ((52, 124, 230, 255), (150, 200, 255, 255)),
}
PIPE_RIMS = {"": ((125, 138, 150, 255), (74, 85, 97, 255)), "advanced_": ("O", "o"), "elite_": ("V", "v")}


def clean_pipe(kind, tier):
    main, light = PIPE_CONTENT[kind]
    rim, rim_dark = PIPE_RIMS[tier]
    c = Canvas()
    c.rect(0, 0, 15, 15, (200, 210, 218, 255))
    c.rect(0, 6, 15, 6, (232, 238, 242, 255))
    c.rect(0, 7, 15, 8, main)
    c.rect(0, 7, 15, 7, light)
    c.rect(0, 9, 15, 9, (150, 162, 172, 255))
    # A ring in the tier's colour where two pipes meet
    for x in (0, 15):
        c.rect(x, 6, x, 9, rim)
    c.rect(5, 5, 10, 10, rim_dark)
    c.rect(6, 6, 9, 9, (60, 68, 78, 255))
    c.rect(7, 7, 8, 8, main)
    c.set(7, 7, light)
    return c


def pipe_extract():
    """The pulling collar where a pipe arm meets a block it pulls from: orange hazard ring."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "o", noise=4, seed=34)
    c.frame(0, 0, 15, 15, "k")
    for i in range(0, 16, 4):
        c.rect(i, 0, i + 1, 15, "y")
    return c


def tank_side(accent):
    """Tank wall: a steel frame around a dark window (columns 4..10, rows 3..12) where the
    tank's renderer draws the fluid at its level, with gauge marks beside it."""
    c = panel(61 if accent == "y" else 62)
    c.rect(3, 2, 11, 13, "K")
    c.rect(4, 3, 10, 12, (24, 30, 36, 255))
    # Opaque glint: one see-through pixel would put the whole block in the translucent layer,
    # and the gas drawn in the window would then hide its wall and show the world behind it.
    c.set(4, 3, (133, 136, 139, 255))
    for y in range(3, 13, 3):
        c.set(12, y, "m")
    c.rect(3, 14, 11, 14, accent)
    return c


def tank_top():
    c = panel(64)
    c.disc(7.5, 7.5, 4.5, "K")
    c.disc(7.5, 7.5, 3.5, "m")
    c.rect(6, 6, 9, 9, "l")
    return c


# --------------------------------------------------------------------------- animation

def strip(frames, frametime=2, interpolate=False):
    """Stacks equal frames into one tall texture, marked animated so it is saved with a .mcmeta."""
    height = frames[0].height
    out = Canvas(frames[0].width, height * len(frames))
    for i, frame in enumerate(frames):
        for y in range(height):
            out.pixels[i * height + y] = list(frame.pixels[y])
            out.keys[i * height + y] = list(frame.keys[y])
    out.animation = {"frametime": frametime, **({"interpolate": True} if interpolate else {})}
    return out


def mix(a, b, t):
    a, b = rgba(a), rgba(b)
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


# A machine's front with the power off: its lights (cyan, green, the uranium readout) one step
# dimmer and its fire out
POWERED_OFF = {"C": "c", "c": "b", "b": "#0a4a5c", "G": "g", "g": "#1f5f37", "5": "4", "4": "3",
               "O": "l", "o": "m", "r": "d"}
# ...and while it works: a band of light sweeping down its lights, one step brighter
SWEEP = {"b": "c", "c": "C", "C": "#ecfeff", "g": "G", "G": "#e6ffe9", "4": "5", "5": "#e8ffd0"}
FLAMES = ("r", "o", "O")
# What glows in the dark on a working machine's front: its lights and any fire
FRONT_GLOW = GLOW_KEYS | {"#ecfeff", "g", "G", "#e6ffe9", "4", "5", "#e8ffd0"} | set(FLAMES)


def powered_off(front):
    out = front.copy()
    for y in range(front.height):
        for x in range(front.width):
            if front.keys[y][x] in POWERED_OFF:
                out.set(x, y, POWERED_OFF[front.keys[y][x]])
    return out


def working(fronts):
    """A working machine's front, looping: a band of light sweeps down its lights, fire flickers,
    and the colour of any window (a fluid, a screen) ripples gently."""
    count = len(fronts)
    frames = []
    for f, front in enumerate(fronts):
        out = front.copy()
        rng = random.Random(f * 977 + 13)
        for y in range(front.height):
            for x in range(front.width):
                key, px = front.keys[y][x], front.pixels[y][x]
                if px is None:
                    continue
                if key in SWEEP:
                    behind = (f * front.height // count - y) % front.height
                    if behind == 0 or (behind == 1 and key == "b"):
                        out.set(x, y, SWEEP[key])
                elif key in FLAMES:
                    out.set(x, y, FLAMES[max(0, min(2, FLAMES.index(key) + rng.randint(-1, 1)))])
                elif key is None or key.startswith("#"):
                    shade = round(16 * math.sin(2 * math.pi * (y / 8 - f / count) + x * 0.7))
                    out.pixels[y][x] = tuple(max(0, min(255, ch + shade)) for ch in px[:3]) + (px[3],)
        frames.append(out)
    return strip(frames)


def lit_textures(name, draw, off=None):
    """The three textures of a machine part that lights up while the machine works: off (dimmed
    unless given), working (animated), and what of that glows in the dark. `draw` draws the part;
    if it takes a frame number (0-15) it draws that frame of a motion of its own, such as bubbles."""
    moving = bool(inspect.signature(draw).parameters)
    on = working([draw(f) if moving else draw() for f in range(16)])
    return {f"block/{name}": off or powered_off(draw()), f"block/{name}_on": on, f"block/{name}_glow": on.glow_mask(FRONT_GLOW)}


def lit_front(name, draw, off=None):
    return lit_textures(f"{name}_front", draw, off)


def rising(points, frame, top, height):
    """Bubble positions for a frame: each rises through a window `height` rows tall from row
    `top`, once round per 16 frames; the still picture (frame None) keeps them where they are."""
    if frame is None:
        return points
    return [(x, top + (y - top - frame * height // 16) % height) for x, y in points]


def gas(base, light, seed, count=32):
    """Swirling gas, looping in place and in time: three waves that tile across the texture and
    a few motes of light drifting up."""
    rng = random.Random(seed)
    motes = [(rng.randrange(16), rng.randrange(16)) for _ in range(8)]
    frames = []
    for f in range(count):
        t = f / count
        c = Canvas()
        for y in range(16):
            for x in range(16):
                v = (math.sin(2 * math.pi * (x / 16 + t)) + math.sin(2 * math.pi * (y / 16 + 2 * t + seed / 7))
                     + 0.7 * math.sin(2 * math.pi * ((x + y) / 16 - t)))
                level = max(0, min(3, int((v + 2.7) / 5.4 * 4)))
                c.set(x, y, mix(base, light, level * 0.2))
        for mx, my in motes:
            c.set(mx, (my - f // 2) % 16, light)
        frames.append(c)
    return strip(frames, interpolate=True)


def gas_texture(base, light, seed):
    """Swirly gas fill used for both the still and the flowing texture."""
    c = Canvas()
    c.rect(0, 0, 15, 15, base, noise=10, seed=seed)
    for i in range(16):
        c.set((i * 5 + seed) % 16, (i * 3) % 16, light)
        c.set((i * 7 + seed) % 16, (i * 11 + 4) % 16, light)
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


def hologram_table_top():
    """Hologram Table plate: a dark top with a ring of cyan lenses round the emitter."""
    c = panel(14)
    c.disc(7.5, 7.5, 5.2, "k")
    c.disc(7.5, 7.5, 4.4, "b", r_inner=3.4)
    c.disc(7.5, 7.5, 2.2, "c")
    c.disc(7.5, 7.5, 1.0, "C")
    for x, y in ((7, 3), (8, 3), (7, 12), (8, 12), (3, 7), (3, 8), (12, 7), (12, 8)):
        c.set(x, y, "C")
    return c


def hologram_table_side():
    """Hologram Table side: only the bottom five rows show, the plate's rim with a lit strip."""
    c = panel(15)
    c.rect(0, 11, 15, 11, "k")
    c.rect(1, 12, 14, 14, "m")
    c.rect(2, 13, 13, 13, "c")
    c.rect(0, 15, 15, 15, "k")
    return c


def swarm_dock_top():
    """Builder Swarm Dock pad: four small landing rings, one per Mini Drone, round a cyan cross. The
    rings are dim: each lights up while a drone stands on it (see DockRenderer)."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "k")
    c.rect(1, 1, 14, 14, "D", noise=3, seed=12)
    # Lead-in marks from the middle of each edge
    c.rect(7, 1, 8, 2, "b")
    c.rect(7, 13, 8, 14, "b")
    c.rect(1, 7, 2, 8, "b")
    c.rect(13, 7, 14, 8, "b")
    # One ring under each parking spot (0.15 from the middle, as DroneEntity.CORNER_OFFSET)
    for cx, cy in ((5.1, 5.1), (9.9, 5.1), (5.1, 9.9), (9.9, 9.9)):
        c.disc(cx, cy, 2.3, "b", r_inner=1.4)
        c.set(round(cx), round(cy), "c")
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


# --------------------------------------------------------------------------- energy storage

def core_side():
    """Pedestal side (6 px high on the block): dark plate with a cyan status strip."""
    c = panel(70)
    c.rect(0, 9, 15, 9, "k")
    c.rect(0, 10, 15, 14, "D", noise=3, seed=71)
    c.rect(0, 15, 15, 15, "k")
    c.rect(2, 12, 13, 12, "b")
    for x in (4, 8, 12):
        c.set(x, 12, "c")
    return c


def core_front():
    """Like the side, with the steel name plate of the R-ZPE cell."""
    c = core_side()
    c.rect(4, 10, 11, 14, "w")
    c.frame(4, 10, 11, 14, "l")
    c.rect(5, 12, 10, 12, "m")
    c.set(2, 12, "g")
    c.set(13, 12, "g")
    return c


def core_top():
    c = Canvas()
    c.rect(0, 0, 15, 15, "k")
    c.rect(1, 1, 14, 14, "D", noise=3, seed=72)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        c.set(x, y, "l")
    return c


def core_ring():
    """The emitter ring the crystal floats over: a glowing circle in a steel collar."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "m", noise=3, seed=73)
    c.disc(7.5, 7.5, 7.0, "l", r_inner=6.0)
    c.disc(7.5, 7.5, 5.6, "b")
    c.disc(7.5, 7.5, 4.2, "c", r_inner=3.2)
    c.disc(7.5, 7.5, 1.6, "C")
    c.rect(0, 0, 15, 1, "c")   # side faces of the ring: lit rim
    return c


def pylon_side():
    """Obelisk body: near-black panels split by a glowing violet strip."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "K")
    c.rect(1, 0, 14, 15, "D", noise=3, seed=74)
    c.rect(1, 0, 2, 15, "d")
    c.rect(7, 0, 8, 15, "v")
    c.rect(7, 2, 8, 13, "V")
    c.rect(4, 0, 4, 15, "k")
    c.rect(11, 0, 11, 15, "k")
    return c


def pylon_top():
    c = Canvas()
    c.rect(0, 0, 15, 15, "K")
    c.rect(1, 1, 14, 14, "d", noise=3, seed=75)
    c.disc(7.5, 7.5, 3.6, "v")
    c.disc(7.5, 7.5, 2.0, "V")
    return c


def resonance_cell():
    """Shell block of the Resonance Core: a steel frame around a dark crystal lattice."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "k")
    c.rect(1, 1, 14, 14, "s", noise=2, seed=76)
    for i in range(2, 14):
        c.set(i, i, "b")
        c.set(15 - i, i, "b")
    for i in (5, 6, 9, 10):
        c.set(i, i, "c")
        c.set(15 - i, i, "c")
    c.set(7, 7, "C")
    c.set(8, 8, "C")
    c.frame(1, 1, 14, 14, "l")
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        c.set(x, y, "w")
    return c


def pylon_emitter():
    """The emitter ring at the top of a pylon (5 rows high on the model): steel rims around a
    glowing violet lens band that runs all the way round, so the beam leaves it on any side."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "K")
    c.rect(0, 0, 15, 0, "l")
    c.rect(0, 1, 15, 3, "v")
    c.rect(0, 2, 15, 2, "V")
    for x in range(1, 16, 4):
        c.set(x, 2, "w")
    c.rect(0, 4, 15, 4, "l")
    return c


def pylon_port():
    """Face of the socket where a cable meets a pylon: a cyan ring like the conduits' energy band."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "K")
    c.rect(1, 1, 14, 14, "d", noise=3, seed=77)
    c.disc(7.5, 7.5, 6.0, "b", r_inner=4.5)
    c.disc(7.5, 7.5, 3.5, "s")
    c.disc(7.5, 7.5, 1.5, "c")
    return c


def crystal_shell():
    """Glass panel of the floating crystal. The renderer maps each rhombus face onto the diamond
    inscribed in the texture: a bright frame along its edges, see-through blue glass inside."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            d = abs(x - 7.5) + abs(y - 7.5)
            if d > 8.2:
                continue
            if d > 6.7:
                c.set(x, y, "#e6f6ff")
            elif d > 5.7:
                c.set(x, y, (120, 200, 235, 230))
            else:
                c.set(x, y, (60, 150, 220, 70 + int(d * 6)))
    return c


def crystal_glow():
    """The crystal's heart: white, brightest in the middle; the renderer tints it."""
    c = Canvas()
    for y in range(16):
        for x in range(16):
            v = max(0, 255 - int((abs(x - 7.5) + abs(y - 7.5)) * 14))
            c.set(x, y, (v, v, v, v))
    return c


def stabilizer_beam():
    """Beam strip: bright along the middle (u = 0.5), fading to nothing at the edges, in colour
    and in alpha, so it never shows a dark outline however it is blended."""
    c = Canvas()
    for x in range(16):
        v = max(0, 255 - int(abs(x - 7.5) * 34))
        for y in range(16):
            c.set(x, y, (v, v, v, v))
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
    "item/planter_module": [
        "................",
        "......kkkk......",
        "......kmmk......",
        "....kkkkkkkk....",
        "....kwGGGGmk....",
        "....kGgyggGk....",
        "....kggggygk....",
        "....kgyggggk....",
        "....kgggyggk....",
        "....kkkkkkkk....",
        ".....kmmmmk.....",
        "......kmmk......",
        ".......kk.......",
        ".......gg.......",
        "......g..g......",
        "................",
    ],
    "item/taxi_module": [
        "................",
        "....kkkkkkkk....",
        "....kmbccbmk....",
        "....kkkkkkkk....",
        ".......kk.......",
        ".......mk.......",
        ".......mk.......",
        ".......mk.......",
        ".......mk.......",
        "......kllk......",
        ".....kl..lk.....",
        ".....kl...k.....",
        ".....kl..kc.....",
        "......kllk......",
        ".......kk.......",
        "................",
    ],
    "item/mob_capsule": [
        "................",
        "......kkkk......",
        ".....kmllmk.....",
        "....kmkkkkmk....",
        "....kw....mk....",
        "....kw....mk....",
        "....kw....mk....",
        "....kw....mk....",
        "....kw....mk....",
        "....kw....mk....",
        "....kmkkkkmk....",
        "....kbccccbk....",
        ".....kbbbbk.....",
        "......kkkk......",
        "................",
        "................",
    ],
    "item/mob_capsule_filled": [
        "................",
        "......kkkk......",
        ".....kmllmk.....",
        "....kmkkkkmk....",
        "....kw.gg.mk....",
        "....kwgGGgmk....",
        "....kwgkkgmk....",
        "....kwggggmk....",
        "....kw.gg.mk....",
        "....kwg..gmk....",
        "....kmkkkkmk....",
        "....kbccccbk....",
        ".....kbbbbk.....",
        "......kkkk......",
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
    # Mini Drone: the drone drawn smaller, with the Mini Frame's white arms
    "item/mini_drone": [
        "................",
        "................",
        "................",
        "...kkk....kkk...",
        "...kwk....kwk...",
        "...kkkk..kkkk...",
        ".....kwkkwk.....",
        "......kCck......",
        "......kcCk......",
        ".....kwkkwk.....",
        "...kkkk..kkkk...",
        "...kwk....kwk...",
        "...kkk....kkk...",
        "................",
        "................",
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
    "item/anchor_addon": [
        "..cc..",
        ".cCCc.",
        "..cc..",
        "..cc..",
        "c.cc.c",
        ".cccc.",
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
    # Planter seed tank 3x2x1: green glass with a light rim
    tank = paint_box(c, 0, 40, 3, 2, 1, "g", top="m", seed=13)
    side_rows(c, tank, ["G", "g"])
    # Taxi cable 1x11x1 and hook (bar 3x1x1, tip 1x1x1): dark cable, steel hook with a cyan tip
    paint_box(c, 8, 40, 1, 11, 1, "k", top="m", seed=14)
    paint_box(c, 12, 40, 3, 1, 1, "l", top="w", bottom="m", seed=15)
    paint_box(c, 20, 40, 1, 1, 1, "c", seed=16)
    # Rancher shear blades 1x1x4: steel with a dark edge
    blade = paint_box(c, 32, 40, 1, 1, 4, "w", top="l", bottom="k", seed=17)
    side_rows(c, blade, ["w"])
    # Fertilizer canister 2x3x2: white with a green band and a dark cap
    can = paint_box(c, 44, 40, 2, 3, 2, "w", top="k", bottom="m", seed=18)
    side_rows(c, can, ["w", "g", "w"])
    # Logger saw blade 0x5x5 (a flat disc): steel with dark teeth
    for name, (x0, y0, x1, y1) in box_faces(48, 10, 0, 5, 5).items():
        c.rect(x0, y0, x1, y1, "w")
        c.frame(x0, y0, x1, y1, "k")
        c.set((x0 + x1) // 2, (y0 + y1) // 2, "m")
    return c


# --------------------------------------------------------------------------- output
ITEM_ASCII.update({
    # Rancher Module: open shears over a tuft of wool
    "item/rancher_module": [
        "................",
        "..kk........kk..",
        ".kwwk......kwwk.",
        ".kwlk......klwk.",
        "..kwwk....kwwk..",
        "...kwwk..kwwk...",
        "....kwwkkwwk....",
        ".....kwwwwk.....",
        "......kcck......",
        ".....kmkkmk.....",
        "....kmk..kmk....",
        "...kok....kok...",
        "..kook....kook..",
        "..kook....kook..",
        "...kk......kk...",
        "................",
    ],
    # Fertilizer Module: a white canister with a green band, dripping
    "item/fertilizer_module": [
        "................",
        "......kkkk......",
        "......kmmk......",
        ".....kkkkkk.....",
        "....kwwwwwlk....",
        "....kwwwwwlk....",
        "....kggggggk....",
        "....kgGggggk....",
        "....kggggggk....",
        "....kwwwwwlk....",
        "....kwwwwwlk....",
        "....kkkkkkkk....",
        "......kmmk......",
        ".......kk.......",
        ".......GG.......",
        "........G.......",
    ],
    # Fisher Module: the drone's cap with a line down to a hook and a fish on it
    "item/fisher_module": [
        "................",
        "......kkkk......",
        "......kmmk......",
        "...kkkkkkkkkk...",
        "...kmmbccbmmk...",
        "...kkkkkkkkkk...",
        ".......kk.......",
        ".......kw.......",
        ".......kw.......",
        ".......kw.......",
        "....kk.kw.......",
        "...kbck.kw......",
        "..kbcCCk.kw.....",
        "...kbck..kwk....",
        "....kk...kk.....",
        "................",
    ],
    # Breeder Module: a feed canister with a heart on it
    "item/breeder_module": [
        "................",
        "......kkkk......",
        "......kmmk......",
        ".....kkkkkk.....",
        "....kwwwwwlk....",
        "....kwrwwrlk....",
        "....krrrrrrk....",
        "....krrrrrrk....",
        "....kwrrrrlk....",
        "....kwwrrwlk....",
        "....kwwwwwlk....",
        "....kkkkkkkk....",
        "......kyyk......",
        ".......kk.......",
        ".......yy.......",
        "........y.......",
    ],
    # Builder Module: the claw holds a block over the cyan ghost of where it goes
    "item/builder_module": [
        "................",
        "......kkkk......",
        "......kmmk......",
        "...kkkkkkkkkk...",
        "...kmmbccbmmk...",
        "...kkkkkkkkkk...",
        "...km.kkkk.mk...",
        "...kmkkwwkkmk...",
        "...kl.kwlk.lk...",
        "...kl.kkkk.lk...",
        "...kk......kk...",
        "................",
        "......cbcb......",
        "......b..c......",
        "......c..b......",
        "......bcbc......",
    ],
})
for _name, _rows in list(ITEM_ASCII.items())[-5:]:
    assert len(_rows) == 16 and all(len(r) == 16 for r in _rows), _name


def saw_icon():
    """Logger Module icon: a circular saw blade with teeth and a glowing hub."""
    c = Canvas()
    c.disc(7.5, 7.5, 6.9, "k")
    c.disc(7.5, 7.5, 6.0, "w")
    c.disc(7.5, 7.5, 4.6, "l")
    for i in range(12):
        angle = math.tau * i / 12
        c.set(round(7.5 + 7.2 * math.cos(angle)), round(7.5 + 7.2 * math.sin(angle)), "k")
        c.set(round(7.5 + 5.4 * math.cos(angle)), round(7.5 + 5.4 * math.sin(angle)), "m")
    c.disc(7.5, 7.5, 2.0, "k")
    c.disc(7.5, 7.5, 1.2, "c")
    return c


# --------------------------------------------------------------------------- acid and steam engine

SULFUR = "#e8d43a"
SULFUR_DARK = "#9a8a16"
ACID = "#9be04a"


def sulfur_ore_overlay():
    """Transparent overlay drawn over netherrack: clusters of yellow sulfur crystals."""
    c = Canvas()
    for cx, cy in ((2, 3), (10, 2), (6, 8), (12, 10), (3, 12), (9, 13)):
        c.rect(cx, cy, cx + 1, cy + 1, SULFUR_DARK)
        c.set(cx, cy, SULFUR)
        c.set(cx + 1, cy, "#fff3a0")
    return c


def sulfur_dust():
    """A small heap of yellow powder."""
    c = Canvas()
    for y in range(7, 14):
        half = (y - 6) + 1
        c.rect(8 - half, y, 7 + half, y, SULFUR, noise=18, seed=y)
    c.rect(2, 14, 13, 14, SULFUR_DARK)
    for x, y in ((6, 8), (9, 10), (5, 12), (11, 12)):
        c.set(x, y, "#fff3a0")
    return c


def acid_synthesizer_front():
    """Two framed glass windows, water (columns 3..6) and acid (9..12): the renderer fills them to their tanks' levels."""
    c = panel(60)
    for x0 in (2, 8):
        c.rect(x0, 2, x0 + 5, 12, "K")
        c.rect(x0 + 1, 3, x0 + 4, 11, "#14222a")
    c.rect(7, 4, 7, 10, "m")
    c.set(7, 7, "O")
    c.rect(3, 13, 12, 13, "c")
    return c

def containment_glass():
    """Framed, tinted window: steel frame, cyan corner lights, a see-through pane."""
    c = Canvas()
    c.rect(0, 0, 15, 15, (40, 70, 80, 70))
    c.frame(0, 0, 15, 15, "k")
    c.frame(1, 1, 14, 14, "m")
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        c.set(x, y, "c")
    for i in range(3, 7):
        c.set(i, 9 - i, (200, 240, 255, 110))
    return c


def steam_engine_front():
    """The boiler's face: an induction coil glowing behind a grille, and a pressure gauge."""
    c = panel(61)
    c.rect(3, 3, 12, 10, "K")
    for y in range(4, 10, 2):
        c.rect(4, y, 11, y, "o")
        c.rect(5, y, 10, y, "O")
    c.disc(7.5, 12.5, 1.6, "w")
    c.set(8, 12, "r")
    return c


def steam_engine_cylinder():
    """A ribbed steel cylinder with a brass band."""
    c = panel(62, base="m")
    for x in range(3, 13, 2):
        c.rect(x, 2, x, 13, "l")
    c.rect(2, 7, 13, 8, "O")
    return c


def steam_engine_dynamo():
    """Copper windings behind a grille, cyan output lights."""
    c = panel(63)
    c.rect(3, 3, 12, 12, "K")
    for y in range(4, 12):
        c.rect(4, y, 11, y, "#b86b2e" if y % 2 else "#e08a45")
    c.rect(3, 13, 12, 13, "c")
    return c


# --------------------------------------------------------------------------- methane chamber

def chamber_port(color, light):
    """A Cyber Frame with a round pipe socket ringed in the fluid's colour."""
    c = cyber_frame()
    c.disc(7.5, 7.5, 4.6, "k")
    c.disc(7.5, 7.5, 3.8, color, r_inner=2.6)
    c.disc(7.5, 7.5, 2.5, "K")
    c.set(6, 6, light)
    return c


def intake_gate():
    """A shutter in a hazard-striped frame: the door monsters are pulled through."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "k")
    for y in range(16):
        for x in range(16):
            if x in (1, 14) or y in (1, 14):
                c.set(x, y, "y" if (x + y) // 2 % 2 else "K")
    c.rect(2, 2, 13, 13, "m", noise=3, seed=30)
    for y in range(3, 13, 2):
        c.rect(3, y, 12, y, "l")
    c.rect(3, 12, 12, 13, "o")
    return c


def chamber_shell():
    """The formed chamber's metal skin: white panels with faint seams."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "#f0f3f5", noise=3, seed=31)
    for x in (0, 8):
        c.rect(x, 0, x, 15, "#c4ccd2")
    c.rect(0, 15, 15, 15, "#c4ccd2")
    return c


def chamber_glass():
    """The chamber's glass: almost clear, with soft vertical streaks; the renderer's tint sets how much shows."""
    c = Canvas()
    c.rect(0, 0, 15, 15, (255, 255, 255, 200))
    for x in (3, 4, 11):
        c.rect(x, 0, x, 15, (255, 255, 255, 255))
    return c


def dissolve_mask():
    """Alpha noise for a mob dissolving in acid, 64x64 like a mob skin: patches of low alpha vanish first."""
    rng = random.Random(77)
    cells = 8
    grid = [[rng.random() for _ in range(cells + 1)] for _ in range(cells + 1)]
    c = Canvas(64, 64)
    for y in range(64):
        for x in range(64):
            gx, gy = x / 64 * cells, y / 64 * cells
            x0, y0 = int(gx), int(gy)
            fx, fy = gx - x0, gy - y0
            top = grid[y0][x0] * (1 - fx) + grid[y0][x0 + 1] * fx
            bottom = grid[y0 + 1][x0] * (1 - fx) + grid[y0 + 1][x0 + 1] * fx
            value = (top * (1 - fy) + bottom * fy) * 0.75 + rng.random() * 0.25
            c.pixels[y][x] = (255, 255, 255, max(8, min(250, int(value * 255))))
    return c


def plain_white():
    c = Canvas(4, 4)
    c.rect(0, 0, 3, 3, (255, 255, 255, 255))
    return c


# --------------------------------------------------------------------------- mech

MECH_WHITE = "#e9edf0"
MECH_SEAM = "#c3cad1"
MECH_RED = "#1f8fd0"  # accent: the mod's blue
MECH_RED_DARK = "#0e5f99"
MECH_DARK = "#2b3036"
MECH_BLACK = "#16191d"


def _mech_box(c, u, v, w, h, d, color=MECH_WHITE, seam=MECH_SEAM, seed=0):
    """A box of the mech: one colour with a seam around every face big enough to show one."""
    faces = paint_box(c, u, v, w, h, d, color, seed=seed)
    for x0, y0, x1, y1 in faces.values():
        if x1 - x0 >= 3 and y1 - y0 >= 3:
            c.frame(x0, y0, x1, y1, seam)
    return faces


def mech_frame_icon():
    """The folded mech as an item: a small white and blue figure with a cyan visor."""
    rows = [
        "................",
        ".......RR.......",
        "......wCCw......",
        "......wwww......",
        "...RRwwccwwRR...",
        "..RwwwwccwwwwR..",
        "..ww.wwwwww.ww..",
        "..ww.wRRRRw.ww..",
        "..dd..wddw..dd..",
        "......wwww......",
        ".....ww..ww.....",
        ".....ww..ww.....",
        ".....ww..ww.....",
        ".....RR..RR.....",
        "....RRR..RRR....",
        "................",
    ]
    mapping = {"w": MECH_WHITE, "R": MECH_RED, "d": MECH_DARK, "c": "c", "C": "C"}
    c = Canvas()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in mapping:
                c.set(x, y, mapping[ch])
    # A dark outline so it reads on any background
    outline = []
    for y in range(16):
        for x in range(16):
            if c.pixels[y][x] is None and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and c.pixels[y + dy][x + dx] is not None
                                              for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                outline.append((x, y))
    for x, y in outline:
        c.set(x, y, "K")
    return c


def mech_texture():
    """The mech's skin, 128x128 in the box layout of MechModel: white armour, blue trim, dark joints and bay, cyan lights."""
    c = Canvas(128, 128)
    # The pilot's bay: white outside, dark inside with a few lights
    back_wall = _mech_box(c, 0, 92, 16, 22, 3, seed=1)
    x0, y0, x1, y1 = back_wall["front"]
    c.rect(x0, y0, x1, y1, MECH_BLACK)
    for y in range(y0 + 3, y1 - 2, 4):
        c.set(x0 + 2, y, "c")
        c.set(x1 - 2, y, "c")
    tx0, ty0, tx1, ty1 = back_wall["top"]
    c.rect(tx0, ty0, tx1, ty1, MECH_RED)
    c.rect(*back_wall["back"], MECH_DARK)
    side_wall = _mech_box(c, 38, 92, 2, 22, 7, seed=2)
    c.rect(*side_wall["left"], MECH_BLACK)
    x0, y0, x1, y1 = side_wall["right"]
    c.rect(x0, y0 + 9, x1, y0 + 10, MECH_RED)
    floor = _mech_box(c, 56, 92, 12, 2, 7, seed=3)
    c.rect(*floor["top"], MECH_DARK)
    x0, y0, x1, y1 = floor["front"]
    c.rect(x0 + 3, y0, x1 - 3, y1, MECH_RED)
    roof = _mech_box(c, 18, 74, 12, 1, 7, seed=4)
    c.rect(*roof["bottom"], MECH_BLACK)
    # Harness: a dark spine with lights, a shoulder bar
    spine = paint_box(c, 92, 102, 2, 14, 1, MECH_DARK, seed=5)
    x0, y0, x1, y1 = spine["front"]
    for y in range(y0 + 1, y1, 3):
        c.set(x0, y, "c")
    paint_box(c, 98, 102, 8, 1, 1, MECH_DARK, seed=6)
    # Petals: white outside with a blue edge and a light slit, dark inside
    upper = _mech_box(c, 56, 102, 8, 11, 1, seed=7)
    x0, y0, x1, y1 = upper["front"]
    c.rect(x0 + 1, y0 + 1, x1 - 1, y0 + 1, MECH_RED)
    for i in range(5):
        c.set(x0 + 1 + i, y0 + 4 + i, MECH_RED)
    c.rect(x1 - 2, y0 + 3, x1 - 2, y1 - 3, "c")
    c.rect(*upper["back"], MECH_DARK)
    lower = _mech_box(c, 74, 102, 8, 9, 1, seed=8)
    x0, y0, x1, y1 = lower["front"]
    for i in range(4):
        c.rect(x1 - 1 - i, y1 - 1 - 2 * i, x1 - 1, y1 - 2 * i, MECH_RED)
    c.rect(*lower["back"], MECH_DARK)
    # Head 8x8x8: dark face behind the visor plate with two lit eyes, a blue crest and chin, ear vents;
    # the plate itself carries the visor slit
    head = _mech_box(c, 92, 0, 8, 8, 8, seed=9)
    x0, y0, x1, y1 = head["front"]
    c.rect(x0, y0, x1, y1 - 2, MECH_BLACK)
    c.rect(x0 + 1, y0 + 3, x0 + 2, y0 + 3, "c")
    c.rect(x1 - 2, y0 + 3, x1 - 1, y0 + 3, "c")
    c.rect(x0 + 2, y1 - 1, x1 - 2, y1, MECH_RED)
    tx0, ty0, tx1, ty1 = head["top"]
    c.rect(tx0 + 3, ty0, tx1 - 3, ty1, MECH_RED)
    for side in ("right", "left"):
        x0, y0, x1, y1 = head[side]
        c.rect(x0 + 2, y0 + 3, x1 - 2, y0 + 5, MECH_DARK)
        c.rect(x0 + 3, y0 + 4, x1 - 3, y0 + 4, "b")
    visor = paint_box(c, 98, 106, 8, 5, 1, MECH_WHITE, seed=10)
    x0, y0, x1, y1 = visor["front"]
    c.rect(x0, y0 + 2, x1, y0 + 2, "C")
    c.rect(x0 + 1, y0 + 3, x1 - 1, y0 + 3, "c")
    c.rect(x0 + 3, y0, x1 - 3, y0, MECH_RED)
    c.rect(*visor["back"], MECH_DARK)
    paint_box(c, 124, 0, 1, 5, 1, MECH_RED, seed=11)                      # V fins
    # Back unit 12x10x4: dark, two glowing thruster slots
    back = _mech_box(c, 34, 22, 12, 10, 4, MECH_DARK, MECH_BLACK, seed=12)
    x0, y0, x1, y1 = back["back"]
    c.rect(x0 + 2, y0 + 2, x0 + 3, y1 - 2, "c")
    c.rect(x1 - 3, y0 + 2, x1 - 2, y1 - 2, "c")
    # Shoulder pads 7x7x9: white with a blue lower edge
    pad = _mech_box(c, 66, 22, 7, 7, 9, seed=13)
    side_rows(c, pad, [MECH_WHITE] * 5 + [MECH_RED, MECH_RED_DARK])
    # Arms: dark upper arm, white forearm with a blue cuff, dark hand
    _mech_box(c, 84, 40, 4, 10, 4, MECH_DARK, MECH_BLACK, seed=14)
    forearm = _mech_box(c, 98, 22, 5, 10, 5, seed=15)
    side_rows(c, forearm, [MECH_WHITE] * 8 + [MECH_RED, MECH_RED])
    paint_box(c, 100, 40, 4, 4, 4, MECH_DARK, seed=16)
    # Legs: white thigh, white shin with a blue band and a light, blue knee pad, blue-capped foot
    _mech_box(c, 60, 40, 6, 11, 6, seed=17)
    shin = _mech_box(c, 0, 40, 7, 13, 7, seed=18)
    side_rows(c, shin, [MECH_WHITE] * 10 + [MECH_RED, MECH_RED, MECH_RED_DARK])
    x0, y0, x1, y1 = shin["front"]
    c.rect(x0 + 3, y0 + 3, x0 + 3, y0 + 5, "c")
    paint_box(c, 100, 48, 5, 4, 2, MECH_RED, seed=19)
    foot = _mech_box(c, 28, 40, 6, 4, 10, seed=20)
    tx0, ty0, tx1, ty1 = foot["top"]
    c.rect(tx0, ty0, tx1, ty0 + 4, MECH_RED)
    c.rect(*foot["front"], MECH_RED)
    # Module parts. Jet: dark wing segments with a blue leading edge and cyan lines, glowing nozzles
    for u, v, w, d in ((18, 82, 8, 4), (18, 87, 8, 3), (42, 82, 6, 2)):
        wing = paint_box(c, u, v, w, 1, d, MECH_DARK, top=MECH_DARK, bottom=MECH_DARK, seed=40 + u)
        x0, y0, x1, y1 = wing["top"]
        c.rect(x0, y0, x1, y0, MECH_RED)
        c.rect(x0 + 1, y0 + 1, x1, y0 + 1, "c")
        x0, y0, x1, y1 = wing["bottom"]
        c.rect(x0, y0, x1, y0, MECH_RED)
    nozzle = paint_box(c, 0, 74, 3, 3, 3, MECH_DARK, top=MECH_DARK, bottom="c", seed=44)
    side_rows(c, nozzle, [MECH_SEAM, MECH_DARK, "b"])
    # Drill: a dark collar, steel bit with spiral grooves, a lit tip
    collar = _mech_box(c, 0, 62, 6, 3, 6, MECH_DARK, MECH_BLACK, seed=45)
    side_rows(c, collar, [MECH_RED, MECH_DARK, MECH_DARK])
    for u, v, w, h in ((24, 62, 4, 4), (40, 62, 3, 3)):
        bit = paint_box(c, u, v, w, h, w, "l", seed=46 + u)
        for name in ("right", "front", "left", "back"):
            x0, y0, x1, y1 = bit[name]
            for i in range(y1 - y0 + 1):
                c.set(x0 + (i % (x1 - x0 + 1)), y0 + i, "m")
    paint_box(c, 40, 68, 2, 3, 2, "c", seed=48)
    # Blade: a dark hilt, the blade itself all light
    paint_box(c, 0, 80, 3, 2, 3, MECH_DARK, top=MECH_RED, seed=49)
    blade = paint_box(c, 112, 67, 1, 14, 3, "c", seed=50)
    for name in ("right", "left"):
        x0, y0, x1, y1 = blade[name]
        c.rect(x0 + 1, y0, x0 + 1, y1, "C")
    # Armour: white plates with blue trim
    shoulder = _mech_box(c, 56, 57, 9, 3, 11, seed=51)
    side_rows(c, shoulder, [MECH_WHITE, MECH_RED, MECH_RED_DARK])
    tx0, ty0, tx1, ty1 = shoulder["top"]
    c.rect(tx0 + 3, ty0 + 1, tx1 - 3, ty1 - 1, MECH_RED)
    chest = _mech_box(c, 96, 57, 14, 9, 1, seed=52)
    x0, y0, x1, y1 = chest["front"]
    for i in range(5):
        c.set(x0 + 2 + i, y0 + 2 + i, MECH_RED)
        c.set(x1 - 2 - i, y0 + 2 + i, MECH_RED)
    c.rect(x0 + 6, y0 + 1, x1 - 6, y0 + 2, "c")
    thigh = _mech_box(c, 96, 67, 7, 6, 1, seed=53)
    x0, y0, x1, y1 = thigh["front"]
    c.rect(x0 + 1, y0 + 3, x1 - 1, y0 + 4, MECH_RED)
    return c

# --------------------------------------------------------------------------- mech hangar

def hangar_frame(formed):
    """A hangar pillar: white plating, blue and white hazard stripes; formed, cyan lines light up."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "k")
    c.rect(1, 1, 14, 14, MECH_WHITE, noise=3, seed=70)
    c.frame(1, 1, 14, 14, MECH_SEAM)
    for y in range(2, 14):
        for x in range(2, 14):
            if y in (2, 3, 12, 13):
                c.set(x, y, MECH_RED if (x + y) // 2 % 2 else MECH_WHITE)
    c.rect(6, 5, 9, 10, MECH_DARK)
    if formed:
        c.rect(7, 5, 8, 10, "c")
        for x, y in ((2, 7), (13, 7), (2, 8), (13, 8)):
            c.set(x, y, "c")
    else:
        c.rect(7, 6, 8, 9, "b")
    return c


def hangar_pad(formed):
    """The charging pad's top, like the rack: a white plate with a blue rim and ring; formed, the ring glows."""
    c = Canvas()
    c.rect(0, 0, 15, 15, MECH_WHITE, noise=3, seed=71)
    c.frame(0, 0, 15, 15, MECH_RED)
    c.frame(1, 1, 14, 14, MECH_SEAM)
    c.disc(7.5, 7.5, 5.8, "c" if formed else MECH_RED, r_inner=4.6)
    c.disc(7.5, 7.5, 2.2, "C" if formed else MECH_SEAM)
    for x, y in ((7, 2), (8, 2), (7, 13), (8, 13), (2, 7), (2, 8), (13, 7), (13, 8)):
        c.set(x, y, "c" if formed else MECH_DARK)
    return c


def hangar_pad_side():
    """The pad's sides, like the rails: white with a blue band and a light line."""
    c = Canvas()
    c.rect(0, 0, 15, 15, MECH_WHITE, noise=3, seed=74)
    c.frame(0, 0, 15, 15, MECH_SEAM)
    c.rect(0, 9, 15, 11, MECH_RED)
    c.rect(2, 10, 13, 10, "c")
    c.rect(0, 15, 15, 15, MECH_DARK)
    return c

def mech_hangar_front(lit):
    """The console: a screen showing the mech's outline and two status lights."""
    c = panel(72)
    c.rect(2, 2, 13, 10, "K")
    c.rect(3, 3, 12, 9, "s")
    outline = "c" if lit else "b"
    c.rect(7, 3, 8, 4, outline)
    c.frame(6, 5, 9, 7, outline)
    c.rect(5, 5, 5, 7, outline)
    c.rect(10, 5, 10, 7, outline)
    c.rect(6, 8, 6, 9, outline)
    c.rect(9, 8, 9, 9, outline)
    c.rect(3, 12, 5, 13, "g" if lit else "D")
    c.rect(10, 12, 12, 13, "c" if lit else "D")
    return c


def mech_module_icon(kind):
    """A module: a white casing with a blue edge and the module's sign."""
    c = Canvas()
    c.rect(2, 3, 13, 13, "k")
    c.rect(3, 4, 12, 12, MECH_WHITE, noise=4, seed=73)
    c.rect(3, 12, 12, 12, MECH_RED)
    c.rect(3, 4, 12, 4, MECH_SEAM)
    sign = {
        "jet": [(5, 9), (6, 8), (7, 7), (8, 7), (9, 8), (10, 9), (7, 9), (8, 9), (7, 10), (8, 10)],
        "drill": [(7, 5), (8, 5), (6, 6), (9, 6), (7, 7), (8, 7), (6, 8), (9, 8), (7, 9), (8, 9), (7, 10), (8, 10)],
        "blade": [(10, 5), (9, 6), (8, 7), (7, 8), (6, 9), (5, 10), (9, 5), (8, 6), (7, 7), (6, 8)],
        "armor": [(5, 6), (6, 6), (9, 6), (10, 6), (5, 7), (10, 7), (5, 8), (10, 8), (6, 9), (9, 9), (7, 10), (8, 10), (7, 6), (8, 6)],
    }[kind]
    for x, y in sign:
        c.set(x, y, "c")
    c.set(2, 3, None)
    c.set(13, 3, None)
    return c


def gem(c, cx, cy, rx, ry, colors, angle=0.0):
    """A faceted gem on `c`: a rhombus centred on (cx, cy), half-axes rx and ry, turned by `angle`
    radians. colors = (outline, dark facet, light facet, glint): lit from the top left."""
    import math
    outline, dark, light, glint = colors
    ca, sa = math.cos(angle), math.sin(angle)
    for y in range(c.height):
        for x in range(c.width):
            dx, dy = x - cx, y - cy
            u, v = dx * ca + dy * sa, -dx * sa + dy * ca
            d = abs(u) / rx + abs(v) / ry
            if d > 1.0:
                continue
            if d > 1.0 - 1.4 / min(rx, ry):
                c.set(x, y, outline)
            elif u + v < 0:
                c.set(x, y, light if d > 0.35 else glint)
            else:
                c.set(x, y, dark)


def isotope_shard():
    """Slivers of glowing isotope recovered from spent fuel: three thin green shards."""
    c = Canvas()
    colors = ("#1d4a14", "#4fb02a", "#a6f05a", "#f4ffb0")
    gem(c, 4.2, 10.0, 2.7, 5.0, colors, angle=0.45)
    gem(c, 11.0, 10.5, 2.7, 4.6, colors, angle=-0.45)
    gem(c, 7.5, 7.5, 3.4, 7.4, colors)
    return c


def fusion_crystal():
    """The Fusion Crystal: a tall violet gem with a white-hot heart."""
    c = Canvas()
    gem(c, 7.5, 7.5, 4.6, 7.4, ("#2a1250", "#6a34c0", "#b88cff", "#ffffff"))
    gem(c, 7.5, 7.5, 1.6, 2.6, ("#f0e0ff", "#f0e0ff", "#ffffff", "#ffffff"))
    return c


def mech_core():
    """The heart of a mech: a white ring with a blue band holding a Fusion Crystal."""
    c = Canvas()
    c.disc(7.5, 7.5, 7.0, "k")
    c.disc(7.5, 7.5, 6.2, MECH_WHITE)
    c.disc(7.5, 7.5, 6.2, MECH_RED, r_inner=5.2)
    c.disc(7.5, 7.5, 4.4, "k")
    gem(c, 7.5, 7.5, 3.0, 4.2, ("#2a1250", "#6a34c0", "#b88cff", "#ffffff"))
    for x, y in ((7, 1), (8, 1), (7, 14), (8, 14), (1, 7), (1, 8), (14, 7), (14, 8)):
        c.set(x, y, MECH_SEAM)
    return c


def crystallizer_front():
    """A violet screen with the crystal growing on it, over a progress strip."""
    c = panel(64)
    c.rect(3, 2, 12, 11, "K")
    c.rect(4, 3, 11, 10, "#160c26")
    gem(c, 7.5, 6.5, 2.4, 3.4, ("#6a34c0", "#6a34c0", "#b88cff", "#ffffff"))
    c.rect(3, 13, 12, 13, "k")
    c.rect(4, 13, 9, 13, "v")
    return c


def crystallizer_top():
    """The emitter the crystal floats over: a violet ring around a lens."""
    c = panel(65)
    c.disc(7.5, 7.5, 6.0, "K")
    c.disc(7.5, 7.5, 5.2, "v", r_inner=3.8)
    c.disc(7.5, 7.5, 3.8, "s")
    c.disc(7.5, 7.5, 2.0, "V")
    c.disc(7.5, 7.5, 0.9, "#ffffff")
    return c


def crystallizer_port():
    """A Cyber Frame with a pipe socket ringed in violet."""
    return chamber_port("#8a4fd8", "#d6b8ff")

# --------------------------------------------------------------------------- materials and circuits

def tinted(canvas, mapping):
    """`canvas` with its palette keys swapped for other colours, e.g. an ore sprite in a new metal."""
    out = canvas.copy()
    for y in range(canvas.height):
        for x in range(canvas.width):
            if canvas.keys[y][x] in mapping:
                out.set(x, y, mapping[canvas.keys[y][x]])
    return out


BAUXITE = {"1": "#2a140c", "2": "#6a2e18", "3": "#a24a24", "4": "#cf6f3a", "5": "#f0a070",
           "6": "#3a2a20", "7": "#5a4234", "8": "#7a5c48", "9": "#9a7a62"}
ALUMINUM = {"1": "#2c3238", "2": "#5e6872", "3": "#8e98a2", "4": "#c2cad2", "5": "#f2f6fa"}


def bauxite_ore_overlay():
    """Transparent overlay over stone or deepslate: rusty clumps of bauxite."""
    c = Canvas()
    for cx, cy in ((2, 3), (10, 2), (6, 8), (12, 10), (3, 12), (9, 13)):
        c.rect(cx, cy, cx + 2, cy + 1, "#7a3418")
        c.set(cx, cy, "#cf6f3a")
        c.set(cx + 1, cy, "#a24a24")
        c.set(cx + 2, cy + 1, "#4a1e0e")
    return c


def raw_bauxite():
    return tinted(raw_uranium(), BAUXITE)


def aluminum_ingot():
    return tinted(uranium_ingot(), ALUMINUM)


def alumina():
    """A small heap of white alumina powder."""
    c = Canvas()
    for y in range(7, 14):
        half = (y - 6) + 1
        c.rect(8 - half, y, 7 + half, y, "#e8ecf0", noise=10, seed=y + 40)
    c.rect(2, 14, 13, 14, "#a8b0b8")
    for x, y in ((6, 8), (9, 10), (5, 12), (11, 12)):
        c.set(x, y, "#ffffff")
    return c


def carbon_fiber():
    """A swatch of woven carbon fiber: a diagonal twill in black and graphite, with a sheen."""
    c = Canvas()
    for y in range(2, 14):
        for x in range(2, 14):
            weave = ((x + y) // 2 + (x // 2)) % 2
            c.set(x, y, "#3a3f46" if weave else "#16191d")
    c.frame(2, 2, 13, 13, "#0b0d10")
    for i in range(3, 9):
        c.set(i + 2, i - 1, "#6a7480")
    return c


def circuit(board, trace, chip_light):
    """A circuit board: a coloured board with copper traces, gold pins and a chip whose light shows the tier."""
    c = Canvas()
    c.rect(1, 3, 14, 12, "k")
    c.rect(2, 4, 13, 11, board, noise=6, seed=91)
    for y in (5, 10):
        c.rect(3, y, 12, y, trace)
    for x in (3, 12):
        c.rect(x, 5, x, 10, trace)
    c.rect(5, 6, 10, 9, "K")
    c.rect(6, 7, 9, 8, "#22262c")
    c.set(7, 7, chip_light)
    for x in (5, 7, 9, 10):
        c.set(x, 5, "O")
        c.set(x, 10, "O")
    return c


def electrolyzer_front(frame=None):
    """An electrolysis cell: two electrodes in a bath, bubbles rising off them while it works."""
    c = panel(66)
    c.rect(3, 2, 12, 12, "K")
    c.rect(4, 3, 11, 11, "#1a2c3a")
    c.rect(4, 7, 11, 11, "#2a5a7a")
    c.rect(5, 3, 5, 10, "l")
    c.rect(10, 3, 10, 10, "#b07040")
    for x, y in rising([(6, 10), (9, 9), (6, 7), (9, 11)], frame, 7, 5):
        c.set(x, y, "#a8e0ff")
    c.rect(3, 13, 12, 13, "c")
    return c


def pyrolysis_oven_front():
    """A kiln door with a grate of glowing coals, a gauge and a methane feed pipe."""
    c = panel(67)
    c.rect(3, 3, 12, 11, "K")
    c.rect(4, 4, 11, 10, "#1c1410")
    for x in range(4, 12):
        c.set(x, 9, "o" if x % 2 else "r")
        c.set(x, 8, "O" if x % 3 == 0 else "o")
    for x in range(4, 12, 2):
        c.set(x, 6, "r")
    c.rect(3, 12, 12, 12, "m")
    c.set(13, 4, "c")
    return c


def fabricator_front():
    """A clean-room window over a circuit board with a printing head and status lights."""
    c = panel(68)
    c.rect(3, 2, 12, 11, "K")
    c.rect(4, 3, 11, 10, "#123a24")
    for y in (5, 8):
        c.rect(4, y, 11, y, "#c87a3a")
    c.rect(6, 6, 9, 7, "K")
    c.rect(4, 3, 11, 3, "m")
    c.rect(7, 4, 8, 4, "C")
    c.rect(3, 13, 12, 13, "c")
    c.set(4, 12, "g")
    c.set(6, 12, "G")
    return c


def build_all():
    textures = {
        "block/machine_side": machine_side(),
        "block/machine_side_glow": machine_side().glow_mask(),
        "block/machine_bottom": machine_bottom(),
        "block/machine_top": machine_top(),
        **lit_front("combustion_generator", lambda: generator_front(True), off=generator_front(False)),
        "block/combustion_generator_top": generator_top(),
        **lit_front("drone_assembler", assembler_base_front),
        "block/drone_assembler_top": assembler_pad(),
        "block/drone_assembler_side": assembler_base_side(),
        "block/drone_assembler_side_glow": assembler_base_side().glow_mask(),
        **lit_textures("drone_assembler_pillar", assembler_pillar),
        **lit_textures("drone_assembler_emitter", assembler_emitter),
        "block/drone_assembler_cap": assembler_cap_side(),
        "block/drone_dock_top": dock_top(),
        "block/swarm_dock_top": swarm_dock_top(),
        "block/hologram_table_top": hologram_table_top(),
        "block/hologram_table_side": hologram_table_side(),
        "entity/hologram": plain_white(),
        "block/drone_dock_side": dock_side(),
        "block/drone_dock_beacon": dock_beacon(),
        **{f"block/{tier}energy_conduit": clean_pipe("energy", tier) for tier in PIPE_RIMS},
        "block/cyber_frame": cyber_frame(),
        "block/cyber_frame_formed": cyber_frame_formed(),
        "block/muffler": muffler(),
        "block/uranium_ore_overlay": uranium_ore_overlay(),
        "block/turbine_casing": turbine_casing(False),
        "block/turbine_casing_formed": turbine_casing(True),
        "block/turbine_port": turbine_port(False),
        "block/turbine_port_formed": turbine_port(True),
        "block/turbine_controller": turbine_controller("off"),
        "block/turbine_controller_formed": turbine_controller("formed"),
        "block/turbine_controller_on": turbine_controller("on"),
        "item/multiblock_projector": multiblock_projector(),
        "block/reactor_casing": reactor_casing(False),
        "block/reactor_casing_formed": reactor_casing(True),
        "block/reactor_port": reactor_port(False),
        "block/reactor_port_formed": reactor_port(True),
        "block/reactor_controller": reactor_controller("off"),
        "block/reactor_controller_formed": reactor_controller("formed"),
        "block/reactor_controller_on": reactor_controller("on"),
        **lit_front("water_pump", water_pump_front),
        "block/water_pump_top": water_pump_top(),
        **lit_front("coolant_mixer", coolant_mixer_front),
        "block/coolant_still": gas_texture("#178f84", "#4fe0d0", 7),
        "block/coolant_flow": gas_texture("#178f84", "#4fe0d0", 8),
        "block/cryo_coolant_still": gas_texture("#8fd8f5", "#ffffff", 9),
        "block/cryo_coolant_flow": gas_texture("#8fd8f5", "#ffffff", 10),
        "block/steam_still": gas("#d8dee3", "#ffffff", 5),
        "block/steam_flow": gas("#d8dee3", "#ffffff", 6),
        "item/biogas_bucket": bucket("#5fa83a", gas=True),
        "item/methane_bucket": bucket("#6cc4dc", gas=True),
        "item/steam_bucket": bucket("#aebfcb", gas=True),
        "item/coolant_bucket": bucket("#1fb8a8", gas=False),
        "item/cryo_coolant_bucket": bucket("#8fd8f5", gas=False),
        "entity/cooling_tower": cooling_tower(False),
        "block/vitrified_waste": vitrified_waste(),
        **lit_front("waste_reprocessor", waste_reprocessor_front),
        "item/hazmat_helmet": hazmat_item("helmet"),
        "item/hazmat_suit": hazmat_item("suit"),
        "item/hazmat_pants": hazmat_item("pants"),
        "item/hazmat_boots": hazmat_item("boots"),
        "item/geiger_counter": geiger_counter(),
        "entity/equipment/humanoid/hazmat": hazmat_layer(False),
        "entity/equipment/humanoid_leggings/hazmat": hazmat_layer(True),
        "mob_effect/radiation_sickness": radiation_icon(),
        "entity/cooling_tower_glow": cooling_tower(True),
        "item/raw_uranium": raw_uranium(),
        "item/uranium_ingot": uranium_ingot(),
        "item/fuel_rod": fuel_rod(False),
        "item/depleted_fuel_rod": fuel_rod(True),
        **lit_front("bio_digester", digester_front),
        "block/bio_digester_top": digester_top(),
        **lit_front("gas_turbine", turbine_front),
        "block/gas_turbine_top": generator_top(),
        "block/methane_extractor_bottom": extractor_bottom(False),
        "block/methane_extractor_top": extractor_top(False),
        "block/methane_extractor_side": extractor_side(False),
        "block/methane_extractor_bottom_formed": extractor_bottom(True),
        "block/methane_extractor_top_formed": extractor_top(True),
        "block/methane_extractor_side_formed": extractor_side(True),
        **{f"block/{tier}gas_pipe": clean_pipe("gas", tier) for tier in PIPE_RIMS},
        **{f"block/{tier}liquid_pipe": clean_pipe("liquid", tier) for tier in PIPE_RIMS},
        "block/pipe_extract": pipe_extract(),
        "block/liquid_tank_side": tank_side("c"),
        "block/gas_tank_side": tank_side("y"),
        "block/tank_top": tank_top(),
        "block/biogas_still": gas("#5fa83a", "#a6e07a", 1),
        "block/biogas_flow": gas("#5fa83a", "#a6e07a", 2),
        "block/methane_still": gas("#6cc4dc", "#d4f4ff", 3),
        "block/methane_flow": gas("#6cc4dc", "#d4f4ff", 4),
        "block/resonance_core_side": core_side(),
        "block/resonance_core_front": core_front(),
        "block/resonance_core_top": core_top(),
        "block/resonance_core_ring": core_ring(),
        "block/stabilizer_pylon_side": pylon_side(),
        "block/stabilizer_pylon_top": pylon_top(),
        "block/stabilizer_pylon_emitter": pylon_emitter(),
        "block/stabilizer_pylon_port": pylon_port(),
        "block/resonance_cell": resonance_cell(),
        "entity/resonance_crystal": crystal_shell(),
        "entity/resonance_glow": crystal_glow(),
        "entity/stabilizer_beam": stabilizer_beam(),
        "block/nether_sulfur_ore_overlay": sulfur_ore_overlay(),
        "item/sulfur": sulfur_dust(),
        "item/acid_bucket": bucket(ACID, gas=False),
        **lit_front("acid_synthesizer", acid_synthesizer_front),
        **lit_front("electrolyzer", electrolyzer_front),
        **lit_front("pyrolysis_oven", pyrolysis_oven_front),
        **lit_front("fabricator", fabricator_front),
        "block/bauxite_ore_overlay": bauxite_ore_overlay(),
        "item/raw_bauxite": raw_bauxite(),
        "item/alumina": alumina(),
        "item/aluminum_ingot": aluminum_ingot(),
        "item/carbon_fiber": carbon_fiber(),
        "item/basic_circuit": circuit("#2f8a4a", "#c87a3a", "g"),
        "item/advanced_circuit": circuit("#1f4f9a", "#d8a040", "c"),
        "item/elite_circuit": circuit("#5a2f9a", "#e8c050", "V"),
        "block/crystallizer_front": crystallizer_front(),
        "block/crystallizer_top": crystallizer_top(),
        "block/crystallizer_port": crystallizer_port(),
        "item/isotope_shard": isotope_shard(),
        "item/fusion_crystal": fusion_crystal(),
        "item/mech_core": mech_core(),
        "block/containment_glass": containment_glass(),
        "block/steam_engine_front": steam_engine_front(),
        "block/steam_engine_cylinder": steam_engine_cylinder(),
        "block/steam_engine_dynamo": steam_engine_dynamo(),
        "block/acid_inlet": chamber_port(ACID, "#e4ffb0"),
        "block/methane_outlet": chamber_port("c", "C"),
        "block/intake_gate": intake_gate(),
        "entity/methane_chamber": chamber_shell(),
        "entity/methane_chamber_glass": chamber_glass(),
        "entity/methane_chamber_glow": plain_white(),
        "entity/dissolve_mask": dissolve_mask(),
    }
    for name, rows in ITEM_ASCII.items():
        textures[name] = from_ascii(rows)
    textures["item/logger_module"] = saw_icon()
    for name, icon in ADDON_ICONS.items():
        textures[name] = addon_chip(icon)
    # Higher frame tiers reuse the basic frame with their own accent colors
    # The Mini Frame is drawn white, like the mod's clean panels
    for name, mapping in (("item/advanced_frame", {"l": "O", "c": "o"}), ("item/elite_frame", {"l": "V", "m": "v", "c": "C"}),
                          ("item/mini_frame", {"l": "w", "m": "l", "c": "C"})):
        textures[name] = from_ascii(recolor(ITEM_ASCII["item/basic_frame"], mapping))
    mech = mech_texture()
    textures["entity/mech"] = mech
    textures["entity/mech_glow"] = mech.glow_mask()
    textures["item/mech_frame"] = mech_frame_icon()
    textures["block/hangar_frame"] = hangar_frame(False)
    textures["block/hangar_frame_formed"] = hangar_frame(True)
    textures["block/hangar_pad"] = hangar_pad(False)
    textures["block/hangar_pad_formed"] = hangar_pad(True)
    textures["block/hangar_pad_side"] = hangar_pad_side()
    textures["block/mech_hangar_front"] = mech_hangar_front(False)
    textures["block/mech_hangar_front_on"] = mech_hangar_front(True)
    for kind in ("jet", "drill", "blade", "armor"):
        textures[f"item/mech_{kind}_module"] = mech_module_icon(kind)
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
        for y in range(min(tex.height, tex.width)):
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
        meta = TEXTURES / f"{texture_name}.png.mcmeta"
        if canvas.animation:
            meta.write_text(json.dumps({"animation": canvas.animation}, indent=2) + "\n")
        elif meta.exists():
            meta.unlink()
    print(f"wrote {len(all_textures)} textures to {TEXTURES.relative_to(ROOT)}")
    if "--preview" in sys.argv:
        write_preview(all_textures, sys.argv[sys.argv.index("--preview") + 1])
