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

from item_icons import ICONS

ROOT = Path(__file__).resolve().parent.parent
TEXTURES = ROOT / "src/main/resources/assets/cyberfarm/textures"

PALETTE = {
    "K": "#050a0b",  # deep outline
    "k": "#0b1515",  # outline
    "s": "#081011",  # screen / recess
    "D": "#1b2a29",  # shadow panel
    "d": "#233433",  # dark panel
    "m": "#405453",  # mid panel
    "l": "#728583",  # light panel
    "w": "#c9d4d2",  # white edge / steel
    "b": "#055b57",  # light, off
    "c": "#d1fee7",  # light glow (near-white)
    "C": "#ffffff",  # bright core
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


# --------------------------------------------------------------------------- nuclear

CONCRETE = "#4a6b65"


def pixel_art(rows, tones):
    """A transparent texture from 16 rows of 16 characters; each key of tones is a colour, '.' stays clear."""
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    c = Canvas()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                c.set(x, y, tones[ch])
    return c


def uranium_ore_overlay():
    """Over vanilla stone or deepslate: soft clumps with a muted rim, in a sickly yellow green."""
    return pixel_art([
        "..........112...",
        ".11......12331..",
        "1233......2342..",
        ".2342.......3...",
        "..23.....1......",
        "......11223.....",
        "....1223344.....",
        ".....23455332...",
        ".......2343.....",
        "..............11",
        "..112.......1233",
        ".12332......2344",
        "..23442..112..23",
        "...2.3..12331...",
        "..........234...",
        "................",
    ], {"1": "#6b7250", "2": "#728a3a", "3": "#8fae3e", "4": "#b6d254", "5": "#dcee8c"})


def reactor_casing(formed):
    """Enamel plating with seams; once formed, status lines light up along the seams."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "#233e3b")
    c.rect(1, 1, 14, 14, CONCRETE, noise=6, seed=50)
    for y in (5, 10):
        c.rect(1, y, 14, y, "#335550")
    c.rect(1, 1, 14, 1, "#71908a")
    if formed:
        c.rect(3, 5, 12, 5, "b")
        c.rect(5, 5, 10, 5, "c")
        for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
            c.set(x, y, "c")
    return c


def reactor_port(formed):
    c = reactor_casing(formed)
    c.frame(0, 0, 15, 15, "#bdc6c5")
    c.frame(1, 1, 14, 14, "#7c8989")
    c.rect(2, 2, 13, 13, "#233e3b")
    c.disc(7.5, 7.5, 5.6, "K")
    c.disc(7.5, 7.5, 4.6, "c" if formed else "#5ec2b8")
    c.disc(7.5, 7.5, 3.4, "s")
    c.rect(7, 4, 8, 11, "#7c8989")
    c.rect(4, 7, 11, 8, "#7c8989")
    return c


def reactor_controller(state):
    """state: 'off' (incomplete), 'formed' (ready) or 'on' (burning)."""
    c = reactor_casing(state != "off")
    c.rect(2, 2, 13, 11, "K")
    c.rect(3, 3, 12, 10, "s")
    if state == "off":
        c.set(7, 6, "b")
        c.set(8, 6, "b")
    else:
        # Radiation trefoil on the screen, emerald when burning
        lit = "#00b894" if state == "on" else "b"
        c.disc(7.5, 6.5, 1.2, lit)
        for (x0, y0, x1, y1) in ((6, 3, 9, 4), (3, 7, 5, 9), (10, 7, 12, 9)):
            c.rect(x0, y0, x1, y1, lit)
    c.rect(3, 13, 12, 13, "c" if state == "on" else "b" if state == "formed" else "b")
    return c


TURBINE_WHITE = "#4a6b65"


def turbine_casing(formed):
    """Enamel turbine plating with a mint stripe; the stripe lights up once the hall is formed."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "#233e3b")
    c.rect(1, 1, 14, 14, TURBINE_WHITE, noise=4, seed=61)
    c.rect(1, 1, 14, 1, "#71908a")
    c.rect(1, 7, 14, 8, "b" if formed else "#335550")
    if formed:
        c.rect(4, 7, 11, 8, "c")
    for x, y in ((2, 3), (13, 3), (2, 12), (13, 12)):
        c.set(x, y, "m")
    return c


def turbine_port(formed):
    c = turbine_casing(formed)
    c.frame(0, 0, 15, 15, "#bdc6c5")
    c.frame(1, 1, 14, 14, "#7c8989")
    c.rect(2, 2, 13, 13, "#233e3b")
    c.disc(7.5, 7.5, 5.6, "K")
    c.disc(7.5, 7.5, 4.6, "c" if formed else "#5ec2b8")
    c.disc(7.5, 7.5, 3.4, "s")
    c.rect(7, 4, 8, 11, "#7c8989")
    c.rect(4, 7, 11, 8, "#7c8989")
    return c


def turbine_controller(state):
    """state: 'off' (incomplete), 'formed' (ready) or 'on' (spinning): a screen with the rotor on it."""
    c = turbine_casing(state != "off")
    c.rect(2, 2, 13, 12, "K")
    c.rect(3, 3, 12, 11, "s")
    blade = "c" if state == "on" else "b" if state == "formed" else "b"
    for i in range(-3, 4):
        c.set(round(7.5 + i), round(7 + i), blade)
        c.set(round(7.5 + i), round(7 - i), blade)
    c.disc(7.5, 7, 1.1, "w" if state != "off" else "m")
    c.rect(3, 13, 12, 13, "c" if state == "on" else "b" if state == "formed" else "b")
    return c


def blueprint_disc():
    """A data disc in dark teal: a pale rim, a lighter track, a silver hub, a glint."""
    c = Canvas()
    c.disc(7.5, 7.5, 7.0, "k")
    c.disc(7.5, 7.5, 6.2, "C")
    c.disc(7.5, 7.5, 5.4, "#233e3b")
    c.disc(7.5, 7.5, 4.4, "#488d91")
    c.disc(7.5, 7.5, 3.4, "#233e3b")
    c.disc(7.5, 7.5, 2.0, "m")
    c.disc(7.5, 7.5, 0.9, "k")
    for x, y in ((3, 6), (4, 4), (6, 3), (11, 10), (10, 11)):
        c.set(x, y, "c")
    return c


# Uranium shades for the item sprites: outline, dark, mid, light, glow; then rock greys
URANIUM_SHADES = {"1": "#14200d", "2": "#26501a", "3": "#3c8a24", "4": "#62d13a", "5": "#c4ff9a",
                  "6": "#2b2c28", "7": "#454640", "8": "#62635b", "9": "#7f8077"}
PALETTE.update(URANIUM_SHADES)

# Drone icon shades, the drone model's own ramps (DRONE_BODY, DRONE_COPPER[2], DRONE_CREAM, DRONE_GUNMETAL, ...)
DRONE_ICON_SHADES = {
    "H": "#5b7771", "M": "#395853", "Z": "#132423",                    # dark teal body
    "u": "#5ec2b8",                                                    # the one flat mint enamel
    "t": "#eff5f0", "T": "#d3ddd9", "e": "#a6b5b2",                    # white ceramic tank
    "n": "#5f6c6c", "q": "#414d4e", "Q": "#2d3839",                    # gunmetal motors
    "p": "#303737", "P": "#131819",                                    # carbon blades
    "I": "#b1bebc", "j": "#7c8989",                                    # steel mast
}
PALETTE.update(DRONE_ICON_SHADES)


def turret_side():
    """Laser Turret pedestal side (6 px high on the block): dark plate, hazard band, red lamp."""
    c = panel(80)
    c.rect(0, 9, 15, 9, "k")
    c.rect(0, 10, 15, 14, "D", noise=3, seed=81)
    c.rect(0, 15, 15, 15, "k")
    for x in range(1, 15):
        c.set(x, 13, "y" if x % 4 < 2 else "k")
    c.rect(7, 11, 8, 11, "#ff2a1f")
    return c


def turret_top():
    """The pedestal's top with the bearing ring the head turns on."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "k")
    c.rect(1, 1, 14, 14, "D", noise=3, seed=82)
    c.disc(7.5, 7.5, 5.0, "m", 3.6)
    c.disc(7.5, 7.5, 3.6, "k")
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        c.set(x, y, "l")
    return c


def turret_ring():
    """Steel of the bearing neck and, in the item, of the head."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "l", noise=3, seed=83)
    c.rect(0, 0, 15, 0, "w")
    c.rect(0, 15, 15, 15, "m")
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


# --------------------------------------------------------------------------- reactor and turbine skin

def shell_plate(c, shade=0):
    c.rect(0, 0, 15, 15, "#4a6b65", noise=0)
    if shade:
        c.rect(0, 0, 15, 15, "#4a6b65")
    c.rect(0, 15, 15, 15, "#335550")
    c.rect(15, 0, 15, 15, "#335550")
    c.rect(0, 0, 14, 0, "#4a6b65")
    for x in range(0, 15):
        c.set(x, 0, "#4a6b65", 9)
    for y in range(0, 15):
        c.set(0, y, "#4a6b65", 6)


def shell_rivet(c, x, y):
    c.set(x, y, "#71908a")
    c.set(x + 1, y + 1, "#233e3b")


def shell_trimmed(src, painter):
    c = src.copy()
    painter(c)
    return c


# ============================================================ skin A: plates, rivets, light bands
def shell_trim(c):
    c.rect(0, 0, 0, 15, "#233e3b")
    c.rect(1, 0, 2, 15, "#71908a")
    c.rect(1, 0, 1, 15, "#7c8989")
    c.rect(3, 0, 3, 15, "#335550")
    c.set(1, 2, "#bdc6c5"); c.set(2, 2, "#bdc6c5")
    c.set(1, 12, "#bdc6c5"); c.set(2, 12, "#bdc6c5")


def shell_tiles():
    t = {}
    c = Canvas(); shell_plate(c)
    c.frame(4, 4, 11, 11, "#335550")
    c.rect(5, 5, 10, 10, "#4a6b65", noise=0)
    for x in range(5, 11): c.set(x, 5, "#4a6b65", -8)
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)): shell_rivet(c, x, y)
    t["body"] = c
    c = Canvas(); shell_plate(c)
    c.rect(1, 7, 14, 7, "#335550"); c.rect(1, 8, 14, 8, "#4a6b65", 8)  # weld line
    for x, y in ((2, 3), (12, 3), (2, 11), (12, 11), (7, 3), (7, 11)): shell_rivet(c, x, y)
    t["body2"] = c
    c = Canvas(); c.rect(0, 0, 15, 15, "#233e3b")
    c.rect(0, 0, 15, 0, "#7c8989"); c.rect(0, 1, 15, 1, "#4a6b65")
    c.rect(0, 5, 15, 5, "#335550")
    c.rect(0, 6, 15, 6, "#5ec2b8")
    c.rect(0, 7, 15, 7, "c")
    c.rect(0, 8, 15, 8, "#5ec2b8")
    c.rect(0, 9, 15, 10, "#335550")
    c.rect(0, 14, 15, 14, "#71908a"); c.rect(0, 15, 15, 15, "#335550")
    for x in (3, 11): c.set(x, 3, "#bdc6c5"); c.set(x, 12, "#bdc6c5")
    t["band"] = c
    c = Canvas(); c.rect(0, 0, 15, 15, "#233e3b")
    c.rect(0, 0, 15, 0, "#bdc6c5"); c.rect(0, 1, 15, 1, "#7c8989")
    c.rect(0, 2, 15, 2, "#335550")
    c.rect(2, 4, 4, 6, "#7c8989"); c.rect(11, 4, 13, 6, "#7c8989")  # studs
    c.rect(2, 4, 4, 4, "#bdc6c5"); c.rect(11, 4, 13, 4, "#bdc6c5")
    c.rect(5, 9, 10, 12, "#071314"); c.rect(5, 9, 10, 9, "#030909")
    c.rect(0, 14, 15, 15, "#071314")
    c.rect(0, 8, 15, 8, "#335550")
    t["base"] = c
    c = Canvas(); c.rect(0, 0, 15, 15, "#4a6b65")
    c.rect(0, 0, 15, 0, "#bdc6c5"); c.rect(0, 1, 15, 1, "#bdc6c5"); c.rect(0, 2, 15, 2, "#7c8989")
    c.rect(0, 3, 15, 3, "#233e3b")
    c.rect(0, 4, 15, 4, "#5ec2b8"); c.rect(0, 5, 15, 5, "c")
    c.rect(0, 6, 15, 6, "#233e3b")
    c.rect(0, 7, 15, 7, "#4a6b65", 8)
    for x, y in ((2, 9), (12, 9)): shell_rivet(c, x, y)
    c.rect(0, 13, 15, 13, "#335550"); c.rect(0, 14, 15, 15, "#233e3b")
    t["rim"] = c
    for k in ("body", "band", "base", "rim"):
        t[k + "_L"] = shell_trimmed(t[k], shell_trim)
    t["body2_L"] = shell_trimmed(t["body2"], shell_trim)
    return t


def machine_shell(glow):
    """The formed reactor's and turbine's skin: a strip of 16-pixel tiles (see ShellTiles.java) ending in a white one for flat colours.
    With glow, only the lit pixels: the renderer draws them full-bright."""
    t = shell_tiles()
    names = ("body", "body2", "band", "base", "rim", "body_L", "body2_L", "band_L", "base_L", "rim_L")
    c = Canvas(16 * (len(names) + 1), 16)
    for i, name in enumerate(names):
        for y in range(16):
            for x in range(16):
                c.pixels[y][16 * i + x] = t[name].pixels[y][x]
                c.keys[y][16 * i + x] = t[name].keys[y][x]
    c.rect(16 * len(names), 0, 16 * len(names) + 15, 15, (255, 255, 255, 255))
    return c.glow_mask() if glow else c


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


HAZMAT = "#e8c21f"


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


# Pipe tiers are told apart by the rims of the sleeve: plain, gold, violet
# Cable sleeves: a light tube with the content as a coloured band down the middle. Arms use rows
# 6..9 (a straight run joins them into one tube), the coupling box at junctions columns/rows 5..10.
PIPE_CONTENT = {
    # main colour, highlight
    "energy": ((209, 254, 231, 255), (255, 255, 255, 255)),
    "gas": ((255, 176, 40, 255), (255, 226, 140, 255)),
    "liquid": ((52, 124, 230, 255), (150, 200, 255, 255)),
}
PIPE_RIMS = {"": ((125, 138, 150, 255), (74, 85, 97, 255)), "advanced_": ("O", "o"), "elite_": ("V", "v")}


def clean_pipe(kind, tier):
    main, light = PIPE_CONTENT[kind]
    rim, rim_dark = PIPE_RIMS[tier]
    c = Canvas()
    c.rect(0, 0, 15, 15, (200, 210, 218, 255))
    # The band wraps all four sides of a tube: the same edge top and bottom, so it meets itself at every corner
    c.rect(0, 6, 15, 9, (150, 162, 172, 255))
    c.rect(0, 7, 15, 8, main)
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


# --------------------------------------------------------------------------- animation

def stabilizer_pylon_light():
    """The edges of light up a Stabilizer Pylon: a brighter pulse climbs through the part a model shows (rows 4..12),
    then starts again at the bottom. Glow colour only, as the edges are 1 px wide."""
    dim, glow = "#6fa89a", "#d1fee7"
    frames = []
    for step in range(10):
        c = Canvas()
        c.rect(0, 0, 15, 15, dim)
        y = 12 - step
        if 2 <= y <= 13:
            c.rect(0, max(0, y - 1), 15, min(15, y + 1), mix(dim, glow, 0.55))
            c.rect(0, y, 15, y, glow)
        frames.append(c)
    return strip(frames, frametime=2, interpolate=True)


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
POWERED_OFF = {"C": "c", "c": "b", "b": "#033a37", "G": "g", "g": "#1f5f37", "5": "4", "4": "3",
               "O": "l", "o": "m", "r": "d"}
# ...and while it works: a band of light sweeping down its lights, one step brighter
SWEEP = {"b": "c", "c": "C", "C": "#ffffff", "g": "G", "G": "#e6ffe9", "4": "5", "5": "#e8ffd0"}
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
    # Drone seen from the front, wide and low: motors at the edges on thin arms, cyan visor, cream tank on the copper band
    "item/drone": [
        "................",
        "................",
        "................",
        ".......kk.......",
        "kkkk..kccK..kkkk",
        "pPPpK.kIjK.kpPPp",
        "knq...tttT...nqK",
        "kqQMMMtTTeMMMqQK",
        "kuu..HuuuuH..uuK",
        ".KK.kcCCCCcK.KK.",
        "....kHMMMMMK....",
        "......M..M......",
        "......Z..Z......",
        "......K..K......",
        "................",
        "................",
    ],
    # Mini Drone: the same drone drawn smaller (12x9 against 16x11)
    "item/mini_drone": [
        "................",
        "................",
        "................",
        "................",
        ".......kk.......",
        "...kk.kccK.kk...",
        "..kpPK.tT.kPpK..",
        "..knq..Te..nqK..",
        "..kuuMHuuHMuuK..",
        "...KKKcCCcKKK...",
        "......M..M......",
        "......Z..Z......",
        "......K..K......",
        "................",
        "................",
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


DRONE_BODY = ("#5b7771", "#395853", "#233e3b", "#132423")      # same ramps as the machine models
DRONE_COPPER = ("#5ec2b8",) * 4                                # the one flat mint enamel accent
DRONE_CREAM = ("#eff5f0", "#d3ddd9", "#a6b5b2", "#6f7e7d")    # white ceramic tank
DRONE_GUNMETAL = ("#5f6c6c", "#414d4e", "#2d3839", "#1b2325")
DRONE_CARBON = ("#303737", "#131819")
DRONE_STEEL = ("#e1eae9", "#b1bebc", "#7c8989", "#515e5e")


def mix(a, b, t):
    a, b = rgba(a), rgba(b)
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


# A machine's front with the power off: its lights (cyan, green, the uranium readout) one step
# dimmer and its fire out
POWERED_OFF = {"C": "c", "c": "b", "b": "#033a37", "G": "g", "g": "#1f5f37", "5": "4", "4": "3",
               "O": "l", "o": "m", "r": "d"}
# ...and while it works: a band of light sweeping down its lights, one step brighter
SWEEP = {"b": "c", "c": "C", "C": "#ffffff", "g": "G", "G": "#e6ffe9", "4": "5", "5": "#e8ffd0"}
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
    # Drone seen from the front, wide and low: motors at the edges on thin arms, cyan visor, cream tank on the copper band
    "item/drone": [
        "................",
        "................",
        "................",
        ".......kk.......",
        "kkkk..kccK..kkkk",
        "pPPpK.kIjK.kpPPp",
        "knq...tttT...nqK",
        "kqQMMMtTTeMMMqQK",
        "kuu..HuuuuH..uuK",
        ".KK.kcCCCCcK.KK.",
        "....kHMMMMMK....",
        "......M..M......",
        "......Z..Z......",
        "......K..K......",
        "................",
        "................",
    ],
    # Mini Drone: the same drone drawn smaller (12x9 against 16x11)
    "item/mini_drone": [
        "................",
        "................",
        "................",
        "................",
        ".......kk.......",
        "...kk.kccK.kk...",
        "..kpPK.tT.kPpK..",
        "..knq..Te..nqK..",
        "..kuuMHuuHMuuK..",
        "...KKKcCCcKKK...",
        "......M..M......",
        "......Z..Z......",
        "......K..K......",
        "................",
        "................",
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


DRONE_BODY = ("#5b7771", "#395853", "#233e3b", "#132423")      # same ramps as the machine models
DRONE_COPPER = ("#5ec2b8",) * 4                                # the one flat mint enamel accent
DRONE_CREAM = ("#eff5f0", "#d3ddd9", "#a6b5b2", "#6f7e7d")    # white ceramic tank
DRONE_STEEL = ("#e1eae9", "#b1bebc", "#7c8989", "#515e5e")


def bevel_box(c, u, v, w, h, d, ramp):
    """A model box painted like the machine models: lit top rows and edges, darker toward the bottom."""
    hi, mid, lo, deep = ramp
    faces = box_faces(u, v, w, h, d)
    for name, (x0, y0, x1, y1) in faces.items():
        if name == "top":
            c.rect(x0, y0, x1, y1, mid)
            c.rect(x0, y0, x1, y0, hi)
            c.rect(x0, y0, x0, y1, hi)
        elif name == "bottom":
            c.rect(x0, y0, x1, y1, deep)
        else:
            c.rect(x0, y0, x1, y1, mid)
            c.rect(x0, y0, x1, y0, hi)
            if y1 > y0:
                c.rect(x0, y1, x1, y1, lo)
    return faces


def mix(a, b, t):
    """Blend two hex colours, t = 0 gives a, t = 1 gives b."""
    ca, cb = rgba(a), rgba(b)
    return tuple(round(ca[i] + (cb[i] - ca[i]) * t) for i in range(3)) + (255,)


def shaded_box(c, u, v, w, h, d, ramp):
    """Like bevel_box, with smooth gradients: sides fade from the lit top row to the dark bottom row,
    the left column catches light and the right one falls into shade; the top fades away from its lit corner."""
    hi, mid, lo, deep = ramp
    faces = box_faces(u, v, w, h, d)
    for name, (x0, y0, x1, y1) in faces.items():
        if name == "bottom":
            c.rect(x0, y0, x1, y1, deep)
            continue
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                if name == "top":
                    t = ((x - x0) / max(1, x1 - x0) + (y - y0) / max(1, y1 - y0)) / 2
                    color = mix(hi, lo, 0.25 + 0.55 * t)
                    if x == x0 or y == y0:
                        color = mix(hi, mid, 0.2)
                else:
                    t = (y - y0) / max(1, y1 - y0)
                    color = mix(mid, lo, t) if y > y0 or y1 == y0 else hi
                    if y1 > y0 and y == y1:
                        color = mix(lo, deep, 0.5)
                    if x == x0 and x1 > x0:
                        color = mix(color, hi, 0.25)
                    elif x == x1 and x1 > x0:
                        color = mix(color, deep, 0.3)
                c.set(x, y, color)
    return faces


def drone_tools(c):
    """The module tools, in the texture's right half. DroneModel.java holds the same coordinates."""
    gun, steel, cream = DRONE_GUNMETAL, DRONE_STEEL, DRONE_CREAM
    mint, dark, lamp = "#5ec2b8", "#132423", "#d1fee7"
    flat = (mint,) * 4
    around = ("right", "front", "left", "back")

    # Harvester: a striped head and a notched rail with toothed blades sliding along it
    head = shaded_box(c, 64, 0, 3, 2, 4, gun)
    for name in around:
        x0, y0, x1, y1 = head[name]
        for x in range(x0, x1 + 1):
            c.rect(x, y0, x, y1, mint if (x - x0) % 2 == 0 else dark)
    x0, y0, x1, y1 = head["bottom"]
    c.rect(x0, y0, x1, y1, dark)
    c.rect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, lamp)
    x0, y0, x1, y1 = head["top"]
    c.rect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, gun[2])
    rail = shaded_box(c, 80, 0, 6, 1, 1, steel)
    tooth = shaded_box(c, 94, 0, 2, 1, 1, steel)
    for name in ("front", "back"):
        x0, y0, x1, _ = rail[name]
        for x in range(x0, x1 + 1, 2):
            c.set(x, y0, dark)
        _, y0, x1, _ = tooth[name]
        c.set(x1, y0, mint)

    # Transport: a housing with two lights, steel jaws with mint toes
    housing = shaded_box(c, 100, 0, 3, 2, 2, gun)
    x0, y0, x1, _ = housing["front"]
    c.rect(x0, y0, x1, y0, mint)
    c.set(x0, y0 + 1, lamp)
    c.set(x1, y0 + 1, lamp)
    jaw = shaded_box(c, 112, 0, 1, 2, 1, steel)
    for name in around:
        x0, _, _, y1 = jaw[name]
        c.set(x0, y1, dark)
    shaded_box(c, 116, 0, 1, 1, 1, flat)

    # Logger: a disc with real teeth, a mint hub, a bracket to the hull
    for x0, y0, x1, y1 in box_faces(64, 6, 0, 5, 5).values():
        if x1 - x0 != 4 or y1 - y0 != 4:
            continue
        for j in range(5):
            for i in range(5):
                if i in (0, 4) and j in (0, 4):
                    c.set(x0 + i, y0 + j, None)
                elif i in (0, 4) or j in (0, 4):
                    c.set(x0 + i, y0 + j, dark if (i + j) % 2 else steel[0])
                else:
                    c.set(x0 + i, y0 + j, steel[1] if (i + j) % 2 else steel[2])
        c.set(x0 + 2, y0 + 2, mint)
    shaded_box(c, 74, 6, 1, 1, 1, flat)
    shaded_box(c, 78, 6, 2, 1, 1, gun)

    # Taxi: a winch drum, a braided cable, a steel hook with a mint tip
    drum = shaded_box(c, 84, 6, 3, 1, 2, gun)
    x0, y0, x1, _ = drum["front"]
    c.set(x0, y0, mint)
    c.set(x1, y0, mint)
    cable = shaded_box(c, 94, 6, 1, 10, 1, gun)
    for name in around:
        x0, y0, _, y1 = cable[name]
        for y in range(y0 + 1, y1 + 1, 2):
            c.set(x0, y, gun[2])
    shaded_box(c, 98, 6, 3, 1, 1, steel)
    tip = shaded_box(c, 106, 6, 1, 1, 1, steel)
    x0, y0, _, _ = tip["top"]
    c.set(x0, y0, mint)

    # Rancher: long blades, bright towards the tip, a mint pivot bolt, a bracket to the hull
    blade = shaded_box(c, 110, 6, 1, 1, 6, steel)
    for name in ("right", "left"):
        x0, y0, x1, _ = blade[name]
        c.rect(x0, y0, x0 + 3, y0, steel[0])
        c.rect(x0 + 4, y0, x1, y0, gun[1])
    shaded_box(c, 124, 6, 1, 3, 1, flat)

    # Bomb: a banded casing, a red detonator, an axle and an arming vane
    case = shaded_box(c, 68, 17, 3, 3, 3, gun)
    for name in around:
        x0, y0, x1, _ = case[name]
        c.rect(x0, y0 + 1, x1, y0 + 1, mint)
    x0, y0, x1, y1 = case["top"]
    c.rect(x0, y0, x1, y1, gun[1])
    x0, y0, _, _ = case["front"]
    c.set(x0 + 1, y0 + 2, dark)
    shaded_box(c, 80, 17, 1, 1, 1, ("#ff2a1f",) * 4)
    shaded_box(c, 84, 17, 1, 1, 1, steel)
    for u, colour, w, h in ((88, steel[1], 3, 1), (96, steel[2], 1, 3)):
        for x0, y0, x1, y1 in (v for n, v in box_faces(u, 17, w, h, 0).items() if n in ("front", "back")):
            c.rect(x0, y0, x1, y1, colour)
            c.set(x0, y0, mint)

    # Small details: the Builder's level, the Breeder's feed trough with grain showing, the Fisher's float
    level = shaded_box(c, 98, 17, 3, 1, 1, flat)
    x0, y0, _, _ = level["front"]
    c.set(x0 + 1, y0, lamp)
    shaded_box(c, 76, 24, 1, 2, 1, gun)
    rack = shaded_box(c, 64, 28, 5, 2, 3, steel)
    hay, straw = "#c9a84a", "#e6cf7a"
    for name in around:
        x0, y0, x1, y1 = rack[name]
        for x in range(x0, x1 + 1):
            if (x - x0) % 2:
                c.rect(x, y0, x, y1, straw if (x - x0) % 4 == 1 else hay)
    for name, step in (("bottom", 2), ("top", 2)):
        x0, y0, x1, y1 = rack[name]
        for x in range(x0, x1 + 1):
            c.rect(x, y0, x, y1, steel[2] if (x - x0) % step == 0 else straw if (x - x0) % 4 == 1 else hay)
    # Planter and Fertilizer: the pump's stem, and its spray bar with three nozzles along the bottom
    shaded_box(c, 106, 17, 1, 1, 1, gun)
    bar = shaded_box(c, 84, 24, 3, 1, 1, steel)
    x0, y0, x1, _ = bar["bottom"]
    c.rect(x0, y0, x1, y0, dark)
    for x in range(x0, x1 + 1, 2):
        c.set(x, y0, mint)
    for name in ("front", "back"):
        x0, y0, x1, _ = bar[name]
        c.set(x0, y0, mint)
        c.set(x1, y0, mint)
    bob = shaded_box(c, 112, 17, 2, 2, 2, cream)
    for name in around:
        x0, y0, x1, _ = bob[name]
        c.rect(x0, y0, x1, y0, mint)
    x0, y0, x1, y1 = bob["top"]
    c.rect(x0, y0, x1, y1, mint)


def drone_texture():
    c = Canvas(128, 64)
    # Chassis 5x2x6: engraved top with bolts and a vent, side seams, a status light on the back
    chassis = shaded_box(c, 0, 0, 5, 2, 6, DRONE_BODY)
    x0, y0, x1, y1 = chassis["top"]
    c.rect(x0 + 1, y0 + 1, x1 - 1, y0 + 1, DRONE_BODY[3])
    c.rect(x0 + 1, y1 - 1, x1 - 1, y1 - 1, mix(DRONE_BODY[0], DRONE_BODY[1], 0.5))
    for x, y in ((x0, y0), (x1, y0), (x0, y1), (x1, y1)):
        c.set(x, y, DRONE_STEEL[1])
    for y in range(y0 + 3, y1 - 1, 2):
        c.set(x0 + 2, y, DRONE_BODY[3])
    for name in ("right", "left"):
        x0, y0, x1, y1 = chassis[name]
        c.set((x0 + x1) // 2, y0, DRONE_BODY[3])
        c.set((x0 + x1) // 2 + 1, y0, DRONE_BODY[0])
    x0, y0, x1, y1 = chassis["back"]
    c.set((x0 + x1) // 2, y0, "c")
    # Visor 4x1x1: glowing strip, brighter in the middle
    for name, (x0, y0, x1, y1) in box_faces(54, 56, 4, 1, 1).items():
        c.rect(x0, y0, x1, y1, "c")
        if name == "front":
            c.rect(x0 + 1, y0, x1 - 1, y1, "C")
    # Low tank 3x2x3: cream with a cylinder-like sheen and a green level window, flat dark copper band 3x1x3
    tank = shaded_box(c, 0, 11, 3, 2, 3, DRONE_CREAM)
    for name in ("right", "front", "left", "back"):
        x0, y0, x1, y1 = tank[name]
        c.set(x0, y0, DRONE_CREAM[0])
        c.set(x1, y1, DRONE_CREAM[3])
    x0, y0, x1, y1 = tank["front"]
    c.set(x0 + 1, y0, "#9be08a")
    c.set(x0 + 1, y1, "#4fa85a")
    x0, y0, x1, y1 = tank["top"]
    c.set(x0 + 1, y0 + 1, DRONE_STEEL[1])                # filler cap
    for x0, y0, x1, y1 in box_faces(28, 32, 3, 1, 3).values():
        c.rect(x0, y0, x1, y1, DRONE_COPPER[2])
    # Antenna: steel mast 1x2x1, glowing tip 1x1x1
    shaded_box(c, 54, 40, 1, 2, 1, DRONE_STEEL)
    for name, (x0, y0, x1, y1) in box_faces(44, 46, 1, 1, 1).items():
        c.rect(x0, y0, x1, y1, "C" if name == "top" else "c")
    # Arms 4x1x1: dark tube with a lit top edge and a copper joint at the hull end
    arm = shaded_box(c, 0, 18, 4, 1, 1, DRONE_BODY)
    for name in ("right", "front", "left", "back", "top", "bottom"):
        if name in arm:
            x0, y0, x1, y1 = arm[name]
            c.set(x0, y0, DRONE_COPPER[2])
    # Motors 2x2x2: gunmetal can with a flat dark copper ring round its base and a lighter hub on top
    motor = shaded_box(c, 16, 18, 2, 2, 2, DRONE_GUNMETAL)
    for name in ("right", "front", "left", "back"):
        x0, y0, x1, y1 = motor[name]
        c.rect(x0, y1, x1, y1, DRONE_COPPER[2])
    x0, y0, _, _ = motor["top"]
    c.set(x0, y0, DRONE_GUNMETAL[0])
    nozzle = shaded_box(c, 44, 56, 1, 1, 1, DRONE_STEEL)
    x0, y0, _, _ = nozzle["bottom"]
    c.set(x0, y0, DRONE_BODY[3])
    # Rotor blades (zero-height boxes): matte carbon, a touch lighter toward the tips
    for u, v, w, d in ((0, 24, 6, 1), (0, 26, 1, 6)):
        for name, (x0, y0, x1, y1) in box_faces(u, v, w, 0, d).items():
            length = max(x1 - x0, y1 - y0)
            for i in range(length + 1):
                tone = mix(DRONE_CARBON[1], DRONE_CARBON[0], abs(i - length / 2) / (length / 2 or 1))
                if x1 - x0 >= y1 - y0:
                    c.rect(x0 + i, y0, x0 + i, y1, tone)
                else:
                    c.rect(x0, y0 + i, x1, y0 + i, tone)
    # Landing gear: legs 1x2x1, skids 1x1x7 with a rubber-dark sole
    shaded_box(c, 44, 49, 1, 2, 1, DRONE_BODY)
    skid = shaded_box(c, 0, 32, 1, 1, 7, DRONE_BODY)
    x0, y0, x1, y1 = skid["bottom"]
    c.rect(x0, y0, x1, y1, "#15181d")
    # Quarry drill: base 2x1x2 and bit 1x2x1, steel with a cyan ring
    paint_box(c, 48, 0, 2, 1, 2, "m", top="l", bottom="b", seed=9)
    bit = paint_box(c, 56, 0, 1, 2, 1, "w", bottom="l", seed=10)
    side_rows(c, bit, ["w", "l"])
    # Planter seed tank 3x2x1: green glass with a light rim
    tank = paint_box(c, 0, 40, 3, 2, 1, "g", top="m", seed=13)
    side_rows(c, tank, ["G", "g"])
    # Fertilizer canister 2x3x2: white with a green band and a dark cap
    can = paint_box(c, 44, 40, 2, 3, 2, "w", top="k", bottom="m", seed=18)
    side_rows(c, can, ["w", "g", "w"])
    # Guard laser: steel mount 2x2x2, barrel 1x1x4 with a dark band, red lens 1x1x1
    paint_box(c, 0, 52, 2, 2, 2, "l", top="w", bottom="d", seed=19)
    barrel = paint_box(c, 8, 52, 1, 1, 4, "m", top="l", bottom="d", seed=20)
    for name in ("right", "left"):
        x0, y0, x1, y1 = barrel[name]
        c.set(x0 + 1, y0, "k")
    paint_box(c, 20, 52, 1, 1, 1, "#ff2a1f", seed=21)
    drone_tools(c)
    return c


# --------------------------------------------------------------------------- output
ITEM_ASCII.update({
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
ITEM_ASCII.update({
    # Guard Module: the drone's cap with a laser gun under it and a red shot leaving the lens
    "item/guard_module": [
        "................",
        "......kkkk......",
        "......kmmk......",
        "...kkkkkkkkkk...",
        "...kmmbccbmmk...",
        "...kkkkkkkkkk...",
        "......kkkk......",
        "....kkkwwlkkkk..",
        "....kllwwllllkr.",
        "....kmmmmmmmmkro",
        "....kkkkkkkkkk..",
        "......kmmk......",
        "......kddk......",
        ".......kk.......",
        "................",
        "................",
    ],
})
for _name, _rows in list(ITEM_ASCII.items())[-7:]:
    assert len(_rows) == 16 and all(len(r) == 16 for r in _rows), _name


# --------------------------------------------------------------------------- acid and steam engine

ACID = "#9be04a"


def sulfur_ore_overlay():
    """Over netherrack: soft clumps with a muted rim, in a pale lemon that nether gold's orange can't be taken for."""
    return pixel_art([
        "................",
        "...112.....11...",
        "..12332...1233..",
        "...2344....2342.",
        "....23......23..",
        "........1.......",
        ".......12231....",
        "..11..12334421..",
        ".1233..234552...",
        "..2343..2332....",
        "...22..........1",
        ".........1122.23",
        "...11...123342..",
        "..1233...23442..",
        "...23.....22....",
        "................",
    ], {"1": "#7d5c28", "2": "#a88a2e", "3": "#cdb643", "4": "#e7d966", "5": "#f6eea0"})


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
    """A hangar pillar: enamel plating, mint stripes; formed, its lines light up."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "k")
    c.rect(1, 1, 14, 14, "#395853", noise=3, seed=70)
    c.frame(1, 1, 14, 14, "#233e3b")
    for y in range(2, 14):
        for x in range(2, 14):
            if y in (2, 3, 12, 13):
                c.set(x, y, "#5ec2b8" if (x + y) // 2 % 2 else "#395853")
    c.rect(6, 5, 9, 10, "#132423")
    if formed:
        c.rect(7, 5, 8, 10, "c")
        for x, y in ((2, 7), (13, 7), (2, 8), (13, 8)):
            c.set(x, y, "c")
    else:
        c.rect(7, 6, 8, 9, "b")
    return c


def hangar_pad(formed):
    """The charging pad's top, like the rack: an enamel plate with a mint rim and ring; formed, the ring glows."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "#395853", noise=3, seed=71)
    c.frame(0, 0, 15, 15, "#5ec2b8")
    c.frame(1, 1, 14, 14, "#233e3b")
    c.disc(7.5, 7.5, 5.8, "c" if formed else "#5ec2b8", r_inner=4.6)
    c.disc(7.5, 7.5, 2.2, "C" if formed else "#233e3b")
    for x, y in ((7, 2), (8, 2), (7, 13), (8, 13), (2, 7), (2, 8), (13, 7), (13, 8)):
        c.set(x, y, "c" if formed else "#132423")
    return c


def hangar_pad_side():
    """The pad's sides, like the rails: enamel with a mint band and a light line."""
    c = Canvas()
    c.rect(0, 0, 15, 15, "#395853", noise=3, seed=74)
    c.frame(0, 0, 15, 15, "#233e3b")
    c.rect(0, 9, 15, 11, "#5ec2b8")
    c.rect(2, 10, 13, 10, "c")
    c.rect(0, 15, 15, 15, "#132423")
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
    """Over stone or deepslate: clumps of two minerals side by side, rust beside pale ochre."""
    return pixel_art([
        "................",
        "..aa.......11...",
        ".abca.....1231..",
        "..bb..11..2343..",
        "..a..1231..22ab.",
        "....12343...abca",
        "....2343.....bb.",
        "...aa22.........",
        "..abca.....11...",
        "...bb.....1231..",
        "..........2343aa",
        ".11........22abc",
        "1231...aa.....bb",
        "2343..abca..1...",
        ".22....bb...23..",
        "................",
    ], {"1": "#6f5a4a", "2": "#8a4426", "3": "#b35f33", "4": "#d98a58",
        "a": "#94805a", "b": "#c9a86a", "c": "#e8cf9a"})


def raw_bauxite():
    return tinted(raw_uranium(), BAUXITE)


def aluminum_ingot():
    return tinted(uranium_ingot(), ALUMINUM)


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


def villager_drone_mechanist():
    """The Drone Mechanist's outfit over the villager model (64x64, the model's own UV layout):
    goggles pushed up on his forehead with their strap round his head, and a white lab coat with
    a blue collar and hem, a pocket with a cyan stylus, and white sleeves meeting over his arms."""
    c = Canvas(64, 64)
    coat, shade, blue = "#e9edf0", "#c9d0d6", "#1f8fd0"
    # Head (0,0): the strap round all four sides on the row above the brow, the goggles on the forehead
    for x in range(0, 32):
        c.set(x, 11, "#262b31")
    c.rect(8, 10, 15, 12, "#3a4048")
    for x in (9, 10, 13, 14):
        c.set(x, 11, "c")
    c.set(9, 11, "C")
    c.set(13, 11, "C")
    # Coat over the jacket (0,38): top, right side, front, left side and back
    c.rect(6, 38, 13, 43, coat)
    for x0, x1 in ((0, 5), (6, 13), (14, 19), (20, 27)):
        c.rect(x0, 44, x1, 63, coat, noise=3, seed=x0 + 7)
        c.rect(x0, 62, x1, 63, blue)
        c.set(x0, 61, shade)
        c.set(x1, 61, shade)
    c.rect(6, 44, 13, 45, blue)
    c.rect(10, 46, 10, 61, shade)
    c.frame(11, 53, 13, 56, shade)
    c.rect(12, 51, 12, 53, "c")
    # Sleeves: the two upper arms (44,22) and the middle of the folded forearms (40,38), hands left bare
    c.rect(44, 22, 59, 31, coat)
    c.rect(44, 32, 59, 33, blue)
    c.rect(46, 38, 49, 45, coat)
    c.rect(58, 42, 61, 45, coat)
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
        "block/uranium_ore_overlay": uranium_ore_overlay(),
        "block/turbine_casing": turbine_casing(False),
        "block/turbine_casing_formed": turbine_casing(True),
        "block/turbine_port": turbine_port(False),
        "block/turbine_port_formed": turbine_port(True),
        "block/turbine_controller": turbine_controller("off"),
        "block/turbine_controller_formed": turbine_controller("formed"),
        "block/turbine_controller_on": turbine_controller("on"),
        "item/blueprint_disc": blueprint_disc(),
        "block/reactor_casing": reactor_casing(False),
        "block/reactor_casing_formed": reactor_casing(True),
        "block/reactor_port": reactor_port(False),
        "block/reactor_port_formed": reactor_port(True),
        "block/reactor_controller": reactor_controller("off"),
        "block/reactor_controller_formed": reactor_controller("formed"),
        "block/reactor_controller_on": reactor_controller("on"),
        "block/laser_turret_side": turret_side(),
        "block/laser_turret_top": turret_top(),
        "block/laser_turret_ring": turret_ring(),
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
        "entity/machine_shell": machine_shell(False),
        "block/vitrified_waste": vitrified_waste(),
        "entity/equipment/humanoid/hazmat": hazmat_layer(False),
        "entity/equipment/humanoid_leggings/hazmat": hazmat_layer(True),
        "mob_effect/radiation_sickness": radiation_icon(),
        "entity/machine_shell_glow": machine_shell(True),
        "item/raw_uranium": raw_uranium(),
        "item/uranium_ingot": uranium_ingot(),
        **lit_front("bio_digester", digester_front),
        "block/bio_digester_top": digester_top(),
        **{f"block/{tier}gas_pipe": clean_pipe("gas", tier) for tier in PIPE_RIMS},
        **{f"block/{tier}liquid_pipe": clean_pipe("liquid", tier) for tier in PIPE_RIMS},
        "block/pipe_extract": pipe_extract(),
        "block/biogas_still": gas("#5fa83a", "#a6e07a", 1),
        "block/biogas_flow": gas("#5fa83a", "#a6e07a", 2),
        "block/methane_still": gas("#6cc4dc", "#d4f4ff", 3),
        "block/methane_flow": gas("#6cc4dc", "#d4f4ff", 4),
        "block/stabilizer_pylon_light": stabilizer_pylon_light(),
        "block/resonance_cell": resonance_cell(),
        "entity/resonance_crystal": crystal_shell(),
        "entity/resonance_glow": crystal_glow(),
        "entity/stabilizer_beam": stabilizer_beam(),
        "block/nether_sulfur_ore_overlay": sulfur_ore_overlay(),
        "item/acid_bucket": bucket(ACID, gas=False),
        **lit_front("fabricator", fabricator_front),
        "entity/villager/profession/drone_mechanist": villager_drone_mechanist(),
        "entity/zombie_villager/profession/drone_mechanist": villager_drone_mechanist(),
        "block/bauxite_ore_overlay": bauxite_ore_overlay(),
        "item/raw_bauxite": raw_bauxite(),
        "item/aluminum_ingot": aluminum_ingot(),
        "item/carbon_fiber": carbon_fiber(),
        "block/crystallizer_port": crystallizer_port(),
        "item/isotope_shard": isotope_shard(),
        "item/fusion_crystal": fusion_crystal(),
        "block/containment_glass": containment_glass(),
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
    for name, icon in ADDON_ICONS.items():
        textures[name] = addon_chip(icon)
    mech = mech_texture()
    textures["entity/mech"] = mech
    textures["entity/mech_glow"] = mech.glow_mask()
    textures["block/hangar_frame"] = hangar_frame(False)
    textures["block/hangar_frame_formed"] = hangar_frame(True)
    textures["block/hangar_pad"] = hangar_pad(False)
    textures["block/hangar_pad_formed"] = hangar_pad(True)
    textures["block/hangar_pad_side"] = hangar_pad_side()
    textures["block/mech_hangar_front"] = mech_hangar_front(False)
    textures["block/mech_hangar_front_on"] = mech_hangar_front(True)
    drone = drone_texture()
    textures["entity/drone"] = drone
    textures["entity/drone_glow"] = drone.glow_mask()
    for name, (rows, tones) in ICONS.items():
        textures[name] = pixel_art(rows, tones)
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
