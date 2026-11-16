#!/usr/bin/env python3
"""Generates the process machines' GUI backgrounds in textures/gui/: the machine's own surface, a header,
the fixed parts of a drawing of the process in a column on the left (x 5..41), bright wells for what goes in
and comes out, an engraved arrow, and a plain inventory tray. Everything that moves or changes (the drawing's
moving parts, tanks, energy, ghost items) is drawn by the screens in Java. Run from tools/."""
import math

from machinemodels import MATERIALS, PALETTE
from texgen import TEXTURES, Canvas

OUT = 0x071314
ENAMEL, HI, LO = 0x1F3835, 0x5B7771, 0x0D1C1C
S = (0xE4EAEA, 0xBDC6C5, 0x96A3A2, 0x6A7878, 0x465353)
MINT, DEEP_MINT, EMERALD = 0x5EC2B8, 0x2F6468, 0x00B894
WELL, RECESS = 0x071314, 0x030909
TRAY, TRAY_HI, TRAY_LO, TRAY_SLOT = 0x1B2B2B, 0x2E4240, 0x0D1616, 0x101C1C
WIDTH, HEIGHT = 176, 166
X = 5


class Gui(Canvas):
    def fill(self, x, y, x2, y2, color):
        value = (color >> 16 & 255, color >> 8 & 255, color & 255, 255)
        for py in range(y, y2):
            for px in range(x, x2):
                self.set(px, py, value)


def surface(g, upper=72, strip=True):
    """The machine's real enamel, tiled, with its strip of light along the base."""
    g.fill(0, 0, WIDTH, g.height, OUT)
    tile = MATERIALS["oven_body"].g
    for y in range(1, upper):
        for x in range(1, WIDTH - 1):
            g.fill(x, y, x + 1, y + 1, int(PALETTE[tile[y % 16][x % 16]].lstrip("#"), 16))
    if strip:
        g.fill(2, upper - 4, WIDTH - 2, upper - 1, 0x132423)
        g.fill(2, upper - 3, WIDTH - 2, upper - 2, MINT)


BOLT = ("....##..", "...##...", "..##....", ".#####..", "...##...", "..##....", ".##.....", ".#......")


def header(g, bolt=True):
    """The title's band, and the bolt that marks the power gauge under it."""
    right = 151 if bolt else WIDTH - 2
    g.fill(2, 2, right, 13, LO)
    g.fill(2, 13, right, 14, 0x488D91)
    if not bolt:
        return
    g.fill(158, 2, 168, 14, LO)
    for j, row in enumerate(BOLT):
        for i, ch in enumerate(row):
            if ch == "#":
                g.fill(159 + i, 4 + j, 160 + i, 5 + j, 0xFFB347)


def tray(g, upper=72):
    height = g.height
    g.fill(0, upper, WIDTH, height, OUT)
    g.fill(1, upper + 1, WIDTH - 1, height - 1, TRAY)
    g.fill(1, upper + 1, WIDTH - 1, upper + 2, TRAY_HI)
    g.fill(1, height - 2, WIDTH - 1, height - 1, TRAY_LO)
    for row in range(4):
        for col in range(9):
            x, y = 8 + 18 * col, upper + 12 + 18 * row + (4 if row == 3 else 0)
            g.fill(x - 1, y - 1, x + 17, y + 17, TRAY_LO)
            g.fill(x, y, x + 17, y + 17, TRAY_HI)
            g.fill(x, y, x + 16, y + 16, TRAY_SLOT)


def block(g, x, y, x2, y2, band=None):
    g.fill(x - 1, y - 1, x2 + 1, y2 + 1, OUT)
    g.fill(x, y, x2, y2, ENAMEL)
    g.fill(x, y, x2, y + 1, HI)
    g.fill(x, y, x + 1, y2, HI)
    g.fill(x, y2 - 1, x2, y2, LO)
    g.fill(x2 - 1, y, x2, y2, LO)
    if band:
        g.fill(x, band, x2, band + 1, MINT)


def steel_h(g, x, y, x2, y2):
    g.fill(x - 1, y - 1, x2 + 1, y2 + 1, OUT)
    g.fill(x, y, x2, y2, S[2])
    g.fill(x, y, x2, y + max(1, (y2 - y) // 3), S[0])
    g.fill(x, y + max(1, (y2 - y) // 3), x2, y + max(2, (y2 - y) // 2), S[1])
    g.fill(x, y2 - 1, x2, y2, S[4])


def steel_v(g, x, y, x2, y2):
    g.fill(x - 1, y - 1, x2 + 1, y2 + 1, OUT)
    width = x2 - x
    for i in range(width):
        t = i / max(1, width - 1)
        color = S[1] if t < 0.2 else S[0] if t < 0.4 else S[1] if t < 0.65 else S[2] if t < 0.85 else S[3]
        g.fill(x + i, y, x + i + 1, y2, color)


def window(g, x, y, x2, y2):
    g.fill(x - 2, y - 2, x2 + 2, y2 + 2, OUT)
    g.fill(x - 1, y - 1, x2 + 1, y2 + 1, S[2])
    g.fill(x - 1, y - 1, x2 + 1, y, S[0])
    g.fill(x - 1, y2, x2 + 1, y2 + 1, S[4])
    g.fill(x, y, x2, y2, WELL)


def well(g, x, y):
    """A slot: a bright steel rim round a deep well, the first thing the eye finds."""
    g.fill(x - 3, y - 3, x + 19, y + 19, OUT)
    g.fill(x - 2, y - 2, x + 18, y + 18, S[1])
    g.fill(x - 2, y - 2, x + 18, y - 1, S[0])
    g.fill(x - 2, y - 2, x - 1, y + 18, S[0])
    g.fill(x - 2, y + 17, x + 18, y + 18, S[3])
    g.fill(x + 17, y - 2, x + 18, y + 18, S[3])
    g.fill(x - 1, y - 1, x + 17, y + 17, OUT)
    g.fill(x, y, x + 16, y + 16, RECESS)


def wells(g, slots):
    """Slots: the wide rim for one standing alone, a thin shared frame for those standing close together."""
    def close(a, b):
        return a != b and abs(a[0] - b[0]) <= 22 and abs(a[1] - b[1]) <= 22
    grouped = [s for s in slots if any(close(s, o) for o in slots)]
    for x, y in slots:
        if (x, y) not in grouped:
            well(g, x, y)
    for x, y in grouped:
        g.fill(x - 2, y - 2, x + 18, y + 18, OUT)
    for x, y in grouped:
        g.fill(x - 1, y - 1, x + 17, y + 17, S[1])
    for x, y in grouped:
        g.fill(x - 1, y - 1, x + 17, y, S[0])
        g.fill(x, y, x + 16, y + 16, RECESS)


def arrow(g, x, x2, y=40):
    """Only the direction, engraved: it does not fill, the drawing shows the work."""
    body = x2 - x - 9
    cells = {(x + i, y + j) for i in range(body) for j in range(6)}
    for i in range(9):
        half = 6 - i * 6 // 9
        cells |= {(x + body + i, y + j) for j in range(-half + 1, 6 + half - 1)}
    for px, py in cells:
        g.fill(px - 1, py - 1, px + 2, py + 2, OUT)
    for px, py in cells:
        g.fill(px, py, px + 1, py + 1, 0x395853)
    for px, py in cells:
        if (px, py - 1) not in cells:
            g.fill(px, py, px + 1, py + 1, HI)


def panel(g, x, y, x2, y2):
    """A dark recess in the plate, where text stays readable."""
    g.fill(x - 1, y - 1, x2 + 1, y2 + 1, OUT)
    g.fill(x, y, x2, y2, LO)
    g.fill(x - 1, y2, x2 + 1, y2 + 1, 0x395853)
    g.fill(x2, y - 1, x2 + 1, y2 + 1, 0x395853)


def foot(g):
    steel_h(g, X, 62, X + 36, 66)


def press(g):
    x, y = X + 3, 16
    for px in (x, x + 26):
        g.fill(px - 1, y - 1, px + 5, 67, OUT)
        g.fill(px, y, px + 4, 66, ENAMEL)
        g.fill(px, y, px + 1, 66, HI)
        g.fill(px + 3, y, px + 4, 66, LO)
    g.fill(x + 4, y + 5, x + 26, 62, 0x0B1616)
    block(g, x, y, x + 30, y + 6)
    g.fill(x, y + 4, x + 30, y + 5, MINT)
    g.fill(x + 11, y + 1, x + 19, y + 4, WELL)
    steel_h(g, x - 1, 62, x + 31, 66)


def digester(g):
    block(g, X + 3, 17, X + 33, 62)
    g.fill(X + 1, 21, X + 35, 58, OUT)
    g.fill(X + 2, 22, X + 34, 57, ENAMEL)
    g.fill(X + 2, 22, X + 3, 57, HI)
    for ring in (24, 54):
        steel_h(g, X + 1, ring, X + 35, ring + 3)
    window(g, X + 8, 31, X + 28, 50)
    foot(g)


def oven(g):
    block(g, X, 18, X + 36, 40)
    for band in (X, X + 16, X + 32):
        g.fill(band, 18, band + 4, 40, MINT)
        g.fill(band + 3, 18, band + 4, 40, DEEP_MINT)
    window(g, X + 5, 24, X + 31, 33)
    block(g, X, 40, X + 36, 62)
    g.fill(X + 3, 44, X + 33, 59, 0x2A1810)
    foot(g)


def cell(g):
    for j in range(7):
        w = 11 - j
        g.fill(X + 18 - w - 1, 15 + j, X + 18 + w + 1, 17 + j, OUT)
    for j in range(7):
        w = 11 - j
        g.fill(X + 18 - w, 16 + j, X + 18 + w, 17 + j, S[1])
    g.fill(X + 8, 16, X + 28, 18, 0xE8ECEC)
    block(g, X, 28, X + 36, 62, band=31)
    window(g, X + 4, 35, X + 32, 59)
    g.fill(X + 4, 42, X + 32, 59, 0xC8D0D8)
    g.fill(X + 4, 42, X + 32, 43, 0xFFFFFF)
    steel_v(g, X + 7, 24, X + 12, 46)
    steel_v(g, X + 24, 24, X + 29, 46)
    foot(g)


def vat(g):
    block(g, X, 24, X + 36, 62, band=27)
    window(g, X + 5, 31, X + 31, 57)
    foot(g)


def mixer(g):
    vat(g)
    steel_h(g, X + 10, 16, X + 26, 20)


def acid(g):
    vat(g)
    steel_h(g, X + 6, 21, X + 30, 24)
    steel_v(g, X + 15, 15, X + 21, 21)


def fabricator(g):
    block(g, X, 16, X + 5, 58)
    block(g, X + 31, 16, X + 36, 58)
    block(g, X, 16, X + 36, 23, band=19)
    g.fill(X + 3, 40, X + 33, 49, OUT)
    g.fill(X + 4, 41, X + 32, 49, 0x1F4A33)
    g.fill(X + 4, 41, X + 32, 42, 0x3F7A52)
    for tx in range(X + 6, X + 30, 4):
        g.fill(tx, 44, tx + 3, 45, 0x0F2A1C)
    steel_h(g, X, 50, X + 36, 56)
    g.fill(X + 5, 57, X + 31, 62, ENAMEL)
    foot(g)


def waste(g):
    block(g, X, 16, X + 36, 62, band=19)
    window(g, X + 4, 24, X + 32, 57)
    foot(g)
    # The radioactive mark, computed: three even blades round a hub, in the free place of the slot grid
    g.fill(127, 25, 145, 43, S[2])
    g.fill(128, 26, 144, 42, WELL)
    for j in range(16):
        for i in range(16):
            dx, dy = i - 7.5, j - 8.5
            radius = math.hypot(dx, dy)
            angle = math.degrees(math.atan2(dx, -dy)) % 120
            if radius < 1.6 or (3 <= radius <= 7.6 and (angle < 30 or angle > 90)):
                g.fill(128 + i, 26 + j, 129 + i, 27 + j, EMERALD)


def crystallizer(g):
    block(g, X, 16, X + 36, 62, band=19)
    window(g, X + 4, 23, X + 32, 57)
    g.fill(X + 4, 51, X + 32, 57, 0x7FC23A)
    g.fill(X + 4, 51, X + 32, 52, 0xC8F890)
    steel_h(g, X + 10, 49, X + 26, 53)
    foot(g)


# name: (the drawing's fixed parts, slots (the menus' positions), the arrow)
MACHINES = {
    "biomass_press": (press, ((58, 35), (120, 35)), (82, 112)),
    "bio_digester": (digester, ((58, 35),), (82, 112)),
    "pyrolysis_oven": (oven, ((74, 35), (128, 35)), (96, 120)),
    "electrolyzer": (cell, ((58, 35), (120, 35)), (82, 112)),
    "coolant_mixer": (mixer, ((74, 35),), (96, 120)),
    "acid_synthesizer": (acid, ((74, 35),), (96, 120)),
    "fabricator": (fabricator, ((74, 17), (74, 35), (74, 53), (128, 35)), (96, 120)),
    "waste_reprocessor": (waste, ((52, 35), (106, 26), (106, 46), (128, 46)), (74, 98)),
    "crystallizer": (crystallizer, ((74, 26), (74, 46), (128, 35)), (96, 120)),
}


def draw(name):
    art, slots, way = MACHINES[name]
    g = Gui(WIDTH, HEIGHT)
    surface(g)
    header(g)
    art(g)
    arrow(g, *way)
    wells(g, slots)
    tray(g)
    return g


def generator(g):
    """The firebox and the chimney the fan sits on; fire and fan are in the screen."""
    block(g, X + 12, 16, X + 24, 32)
    block(g, X, 30, X + 36, 62, band=33)
    window(g, X + 6, 38, X + 30, 58)
    g.fill(X + 6, 38, X + 30, 58, 0x2A1810)
    foot(g)


def rotor(g):
    """The casing and the glass over the rotor, which the screen spins."""
    block(g, X, 18, X + 36, 62, band=21)
    window(g, X + 5, 27, X + 31, 53)
    foot(g)


def assembler(g):
    """The gantry the welding head runs on."""
    block(g, 132, 15, 152, 20)


BELT_X, BELT_PITCH, BELT_HOLDERS = 31, 26, 5   # as UtilityBeltMenu: SLOT_X, SLOT_PITCH, the holders


def belt_gui(g):
    """The Utility Belt's panel (the user's "tecno-enamel" pick): a bus of LEDs over the five holders, the brown strap
    with its mint stripe and studs behind them, a buckle at the left, and the charge channels UtilityBeltScreen fills."""
    g.fill(2, 15, 174, 31, 0x0D1C1C)
    g.fill(2, 31, 174, 32, 0x488D91)
    # bus line with one LED pad per holder and the end module on the right
    g.fill(26, 17, 156, 19, 0x2F6468)
    for h in range(BELT_HOLDERS):
        x = BELT_X + h * BELT_PITCH + 8
        g.fill(x - 3, 16, x + 3, 20, 0x132423)
        g.fill(x - 2, 17, x + 2, 19, 0x5EC2B8)
        g.fill(x - 1, 17, x + 1, 19, 0xD1FEE7)
    g.fill(156, 16, 172, 29, 0x132423)
    g.fill(158, 18, 162, 22, 0x00B894)
    g.fill(158, 24, 170, 26, 0x2F6468)
    # the belt behind the holders: brown strap, lit top edge, dark bottom edge, mint stripe, studs
    g.fill(2, 33, 174, 53, 0x7A5230)
    g.fill(2, 33, 174, 35, 0x9C6B42)
    g.fill(2, 51, 174, 53, 0x5A3A20)
    g.fill(2, 41, 174, 45, 0x2F6468)
    g.fill(2, 42, 174, 44, 0x5EC2B8)
    for x in range(28, 172, 8):
        g.fill(x, 36, x + 2, 38, 0xE4EAEA)
        g.fill(x, 48, x + 2, 50, 0xE4EAEA)
    # buckle at the left end
    g.fill(5, 34, 25, 52, 0x071314)
    g.fill(6, 35, 24, 51, 0xBDC6C5)
    g.fill(6, 35, 24, 36, 0xE4EAEA)
    g.fill(9, 38, 21, 48, 0x132423)
    g.fill(11, 42, 19, 44, 0xD1FEE7)
    # bar channels under the sphere's and the drone's holders (the Java fills them, 16x2 at y 54), a dark housing
    for h in (0, 3):
        x = BELT_X + h * BELT_PITCH
        g.fill(x - 1, 54 - 1, x + 17, 54 + 3, 0x071314)
        g.fill(x, 54, x + 16, 54 + 2, 0x0D1C1C)


# name: (height, upper part's height, bolt, drawing, slots, arrow, text panels)
OTHERS = {
    "combustion_generator": (166, 72, True, generator, ((74, 35),), (96, 150), ()),
    "gas_turbine": (166, 72, True, rotor, (), None, ((70, 17, 152, 67),)),
    "turbine": (166, 72, True, rotor, (), None, ((70, 17, 152, 67),)),
    "steam_engine": (196, 102, True, None, (), None, ((6, 76, 132, 92),)),
    "methane_extractor": (210, 116, False, None, (), None, ((5, 17, 31, 28), (147, 17, 171, 28), (30, 95, 146, 116))),
    "reactor": (218, 124, False, None, tuple((50 + i % 4 * 18, 20 + i // 4 * 18) for i in range(8)) + tuple((50 + i * 18, 62) for i in range(4)), None, ((122, 64, 150, 76),)),
    "mech_hangar": (210, 116, False, None, (), None, ()),
    "drone_assembler": (184, 90, True, assembler, ((12, 36), (40, 18), (40, 36), (40, 54), (66, 27), (84, 27), (66, 45), (84, 45), (132, 37)), (104, 128), ()),
    "drone_dock": (186, 92, True, None, tuple((98 + c * 18, 17 + r * 18) for r in range(3) for c in range(3)), None, ((6, 16, 92, 70),)),
    "supply_terminal": (194, 100, False, None, ((151, 25), (151, 61)), None, ((6, 15, 146, 98),)),
    "resonance_core": (134, 134, True, None, (), None, ((6, 17, 152, 128),)),
    "muffler": (146, 146, False, None, (), None, ((5, 16, 171, 142),)),
    # The Utility Belt's four holders in a row, a panel under them for the sphere's and the drone's charge
    # The Battery Bank: the slot that charges an item, a panel for the bank's gauge and total
    "battery_bank": (166, 72, True, None, ((12, 28),), None, ((6, 48, 154, 69),)),
    # The Human Dock: no slots, a panel for who is charging and how fast
    "utility_belt": (166, 72, False, belt_gui, tuple((BELT_X + h * BELT_PITCH, 35) for h in range(BELT_HOLDERS)), None, ((8, 56, 168, 68),)),
}


def draw_other(name):
    height, upper, bolt, art, slots, way, panels = OTHERS[name]
    g = Gui(WIDTH, height)
    # The extractor's two lines under its window need that room
    surface(g, upper, name != "methane_extractor")
    header(g, bolt)
    for area in panels:
        panel(g, *area)
    if art:
        art(g)
    if way:
        arrow(g, *way)
    wells(g, slots)
    if upper < height:
        tray(g, upper)
    return g




TABLE_CHIP = (3, 30, 15, 42)        # the chip on the left: x, y, x2, y2 (TrinketTableScreen lights its LEDs)
TABLE_PIPE = (154, 14, 160, 72)     # the pipe down the right side (TrinketTableScreen runs a pulse along it)


def trinket_table_gui():
    """The Trinket Table's grid in the machines' own enamel, unlike the crafting table it works like: the 3x3 grid, the
    engraved arrow and the result; on the left a chip on its board with traces running into the grid, on the right a
    steel pipe with mint couplings, a branch into the result's side. No power gauge: it takes none."""
    g = Gui(WIDTH, HEIGHT)
    surface(g)
    header(g, bolt=False)
    # The board under the chip and its traces into the grid's edge
    x0, y0, x1, y1 = TABLE_CHIP
    g.fill(2, 18, 27, 69, LO)
    g.fill(2, 18, 27, 19, 0x488D91)
    for i, y in enumerate((22, 26, 47, 52, 57, 62)):
        g.fill(5 if i % 2 else 8, y, 27, y + 1, DEEP_MINT)
        g.fill(26, y - 1, 28, y + 2, MINT)
    for x in (6, 9, 12):
        g.fill(x, 22, x + 1, y0, DEEP_MINT)
        g.fill(x, y1, x + 1, 47, DEEP_MINT)
    # The chip: a dark body with steel pins down both sides and a mint die mark
    g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, OUT)
    g.fill(x0, y0, x1, y1, 0x132423)
    for y in range(y0 + 1, y1 - 1, 2):
        g.fill(x0 - 2, y, x0, y + 1, S[1])
        g.fill(x1, y, x1 + 2, y + 1, S[1])
    g.fill(x0 + 2, y0 + 2, x0 + 4, y0 + 4, MINT)
    # The pipe down the right side, its couplings, and a branch into the result
    px0, py0, px1, py1 = TABLE_PIPE
    steel_v(g, px0, py0, px1, py1)
    for y in (20, 40, 62):
        g.fill(px0 - 2, y, px1 + 2, y + 3, OUT)
        g.fill(px0 - 1, y + 1, px1 + 1, y + 2, MINT)
    steel_h(g, 145, 41, px0, 45)
    g.fill(144, 40, 146, 46, OUT)
    g.fill(145, 41, 146, 45, DEEP_MINT)
    wells(g, tuple((30 + c * 18, 17 + r * 18) for r in range(3) for c in range(3)) + ((124, 35),))
    arrow(g, 92, 118, 38)
    tray(g)
    return g


def vanilla_trinkets():
    """The inventory's Trinkets tab, drawn like the game's own inventory (the user wants no jump in style between the
    two tabs): the light grey panel with its white and dark bevels, the trinket slots in the armor's column, the black
    window the player stands in, and the inventory under them."""
    g = Gui(WIDTH, HEIGHT)
    grey, light, shade, black = 0xC6C6C6, 0xFFFFFF, 0x555555, 0x000000
    g.fill(2, 0, WIDTH - 3, 1, black)
    g.fill(0, 2, 1, HEIGHT - 3, black)
    g.fill(2, HEIGHT - 1, WIDTH - 3, HEIGHT, black)
    g.fill(WIDTH - 1, 3, WIDTH, HEIGHT - 2, black)
    for x, y in ((1, 1), (WIDTH - 3, 1), (WIDTH - 2, 2), (1, HEIGHT - 3), (2, HEIGHT - 2), (WIDTH - 2, HEIGHT - 3), (WIDTH - 3, HEIGHT - 2)):
        g.fill(x, y, x + 1, y + 1, black)
    g.fill(1, 2, WIDTH - 1, HEIGHT - 2, grey)
    g.fill(2, 1, WIDTH - 3, HEIGHT - 1, grey)
    g.fill(2, 1, WIDTH - 4, 3, light)
    g.fill(1, 2, 3, HEIGHT - 4, light)
    g.fill(4, HEIGHT - 3, WIDTH - 2, HEIGHT - 1, shade)
    g.fill(WIDTH - 3, 4, WIDTH - 1, HEIGHT - 2, shade)
    g.fill(WIDTH - 3, 3, WIDTH - 2, 4, grey)
    g.fill(3, HEIGHT - 3, 4, HEIGHT - 2, grey)

    def slot(x, y):
        g.fill(x - 1, y - 1, x + 17, y + 17, 0x8B8B8B)
        g.fill(x - 1, y - 1, x + 16, y, 0x373737)
        g.fill(x - 1, y - 1, x, y + 16, 0x373737)
        g.fill(x, y + 16, x + 17, y + 17, light)
        g.fill(x + 16, y, x + 17, y + 17, light)

    for i in range(3):
        slot(8, 8 + i * 18)
    # The player's window, framed like a slot
    g.fill(25, 7, 77, 79, 0x373737)
    g.fill(26, 78, 77, 79, light)
    g.fill(76, 8, 77, 79, light)
    g.fill(26, 8, 76, 78, black)
    for row in range(3):
        for col in range(9):
            slot(8 + col * 18, 84 + row * 18)
    for col in range(9):
        slot(8 + col * 18, 142)
    return g


def main():
    for name in MACHINES:
        draw(name).save(TEXTURES / f"gui/{name}.png")
    for name in OTHERS:
        draw_other(name).save(TEXTURES / f"gui/{name}.png")
    vanilla_trinkets().save(TEXTURES / "gui/trinkets.png")
    trinket_table_gui().save(TEXTURES / "gui/trinket_table.png")


if __name__ == "__main__":
    main()
