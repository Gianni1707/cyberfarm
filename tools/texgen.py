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
    """Handheld projector: a dark body with a cyan lens throwing a beam up-right."""
    c = Canvas()
    c.rect(2, 8, 10, 13, "k")
    c.rect(3, 9, 9, 12, "m")
    c.rect(3, 9, 9, 9, "l")
    c.rect(4, 13, 6, 15, "k")
    c.rect(5, 13, 5, 14, "s")
    c.disc(10.5, 9.5, 2.2, "k")
    c.disc(10.5, 9.5, 1.3, "c")
    c.set(10, 9, "w")
    for i in range(1, 5):
        c.set(11 + i, 8 - i, "b")
        c.set(12 + i, 8 - i, "c") if 12 + i < 16 else None
        c.set(11 + i, 9 - i, "c")
    return c


def water_pump_front():
    c = panel(52)
    c.rect(3, 3, 12, 12, "K")
    c.rect(4, 4, 11, 11, "#2a4f9a", noise=8, seed=52)
    for x, y in ((5, 9), (8, 6), (10, 10), (6, 5)):
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


def digester_front():
    c = panel(22)
    c.rect(3, 2, 12, 12, "K")
    c.rect(4, 3, 11, 6, "s")
    c.rect(4, 7, 11, 11, "#4f9a2f", noise=8, seed=22)
    for x, y in ((5, 9), (8, 8), (10, 10), (6, 6), (9, 5)):
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
    c = panel(24)
    c.disc(7.5, 7.5, 6.2, "K")
    c.disc(7.5, 7.5, 5.2, "s")
    for i in range(-4, 5):
        c.set(round(7.5 + i), round(7.5 + i), "l")
        c.set(round(7.5 + i), round(7.5 - i), "l")
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
TIER_RIMS = {"": None, "advanced_": ("O", "o"), "elite_": ("V", "v")}


def tier_rims(c, tier):
    """Colours the sleeve's edge rows and the core frame with the tier's accent."""
    rims = TIER_RIMS[tier]
    if rims:
        c.rect(0, 6, 15, 6, rims[0])
        c.rect(0, 9, 15, 9, rims[1])
        c.frame(5, 5, 10, 10, rims[1])
    return c


def gas_pipe():
    """Like the conduit sleeve, with an amber stripe so gas and power lines are told apart."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "m", noise=3, seed=27)
    c.rect(0, 7, 15, 8, "y")
    for x in range(0, 16, 3):
        c.set(x, 7, "O")
        c.set(x, 8, "O")
    c.frame(5, 5, 10, 10, "k")
    c.rect(0, 6, 15, 6, "l")
    c.rect(0, 9, 15, 9, "d")
    return c


def gas_texture(base, light, seed):
    """Swirly gas fill used for both the still and the flowing texture."""
    c = Canvas()
    c.rect(0, 0, 15, 15, base, noise=10, seed=seed)
    for i in range(16):
        c.set((i * 5 + seed) % 16, (i * 3) % 16, light)
        c.set((i * 7 + seed) % 16, (i * 11 + 4) % 16, light)
    return c


def energy_conduit():
    """Sleeve texture: the core uses the centre (5..10), the arms the middle rows (6..9)."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "d", noise=3, seed=12)
    c.rect(0, 7, 15, 8, "b")
    for x in range(0, 16, 2):
        c.set(x, 7, "c")
        c.set(x + 1, 8, "c")
    c.frame(5, 5, 10, 10, "k")
    c.rect(0, 6, 15, 6, "m")
    c.rect(0, 9, 15, 9, "D")
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
    # Planter seed tank 3x2x1: green glass with a light rim
    tank = paint_box(c, 0, 40, 3, 2, 1, "g", top="m", seed=13)
    side_rows(c, tank, ["G", "g"])
    # Taxi cable 1x11x1 and hook (bar 3x1x1, tip 1x1x1): dark cable, steel hook with a cyan tip
    paint_box(c, 8, 40, 1, 11, 1, "k", top="m", seed=14)
    paint_box(c, 12, 40, 3, 1, 1, "l", top="w", bottom="m", seed=15)
    paint_box(c, 20, 40, 1, 1, 1, "c", seed=16)
    # Logger saw blade 0x5x5 (a flat disc): steel with dark teeth
    for name, (x0, y0, x1, y1) in box_faces(48, 10, 0, 5, 5).items():
        c.rect(x0, y0, x1, y1, "w")
        c.frame(x0, y0, x1, y1, "k")
        c.set((x0 + x1) // 2, (y0 + y1) // 2, "m")
    return c


# --------------------------------------------------------------------------- output

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
        **{f"block/{tier}energy_conduit": tier_rims(energy_conduit(), tier) for tier in TIER_RIMS},
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
        "block/water_pump_front": water_pump_front(),
        "block/water_pump_top": water_pump_top(),
        "block/coolant_mixer_front": coolant_mixer_front(),
        "block/coolant_still": gas_texture("#178f84", "#4fe0d0", 7),
        "block/coolant_flow": gas_texture("#178f84", "#4fe0d0", 8),
        "block/cryo_coolant_still": gas_texture("#8fd8f5", "#ffffff", 9),
        "block/cryo_coolant_flow": gas_texture("#8fd8f5", "#ffffff", 10),
        "block/steam_still": gas_texture("#d8dee3", "#ffffff", 5),
        "block/steam_flow": gas_texture("#d8dee3", "#ffffff", 6),
        "item/biogas_bucket": bucket("#5fa83a", gas=True),
        "item/methane_bucket": bucket("#6cc4dc", gas=True),
        "item/steam_bucket": bucket("#aebfcb", gas=True),
        "item/coolant_bucket": bucket("#1fb8a8", gas=False),
        "item/cryo_coolant_bucket": bucket("#8fd8f5", gas=False),
        "entity/cooling_tower": cooling_tower(False),
        "block/vitrified_waste": vitrified_waste(),
        "block/waste_reprocessor_front": waste_reprocessor_front(),
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
        "block/bio_digester_front": digester_front(),
        "block/bio_digester_top": digester_top(),
        "block/gas_turbine_front": turbine_front(),
        "block/gas_turbine_top": generator_top(),
        "block/methane_extractor_bottom": extractor_bottom(False),
        "block/methane_extractor_top": extractor_top(False),
        "block/methane_extractor_side": extractor_side(False),
        "block/methane_extractor_bottom_formed": extractor_bottom(True),
        "block/methane_extractor_top_formed": extractor_top(True),
        "block/methane_extractor_side_formed": extractor_side(True),
        **{f"block/{tier}gas_pipe": tier_rims(gas_pipe(), tier) for tier in TIER_RIMS},
        "block/biogas_still": gas_texture("#5fa83a", "#a6e07a", 1),
        "block/biogas_flow": gas_texture("#5fa83a", "#a6e07a", 2),
        "block/methane_still": gas_texture("#6cc4dc", "#d4f4ff", 3),
        "block/methane_flow": gas_texture("#6cc4dc", "#d4f4ff", 4),
    }
    for name, rows in ITEM_ASCII.items():
        textures[name] = from_ascii(rows)
    textures["item/logger_module"] = saw_icon()
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
