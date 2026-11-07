"""Builds the 3D machine models (prototype): Combustion Generator, Bio-Digester, Drone Dock, Builder Swarm Dock,
Drone Assembler, Fabricator and Acid Synthesizer.

Each machine is a list of boxes, like a Blockbench model. Every face gets its own patch in one texture atlas per
machine, painted from a small set of source materials and baked with bevelled edges and a top-to-bottom gradient,
so the look comes from the texture and the game only adds its usual face shading. Glowing parts (fire, status
lights) are separate boxes with light_emission in the lit model.

Usage: python tools/machinemodels.py   (writes textures/block/models/*.png and models/block/*.json)
"""
import json
import math
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/cyberfarm"
N = 16

PALETTE = {"\u03c6": "#5ec2b8", "\u03c3": "#b1bebc", "\u03c4": "#4a6b65", "\u03c5": "#5c807a", "\u03c8": "#5f847d", "\u03c7": "#597d76",
             # ramps shift hue: highlights warm, shadows cool
    "1": "#5b7771", "2": "#395853", "3": "#233e3b", "4": "#132423", "5": "#071314",     # dark teal body
    "a": "#bdc6c5", "b": "#b1bebc", "c": "#7c8989", "s": "#030909",                     # steel trim (kept under the lights), recess
    "g": "#ffffff", "G": "#d1fee7", "h": "#055b57",                                      # near-white light, off
    "y": "#e8b830", "u": "#fff2a8", "v": "#ffc23d", "w": "#ff7a1f", "x": "#b8391a",      # hazard, fire
    "m": "#b6f08a", "o": "#5fcf6a", "p": "#2f8a52", "j": "#d6f3f7",                      # liquid, glint
    "A": "#a5ead4", "B": "#5ec2b8", "C": "#488d91", "D": "#496973",                      # mint enamel accent
    "H": "#00b894",                                                                      # emerald marking
    "P": "#b88cff", "Q": "#6a34c0", "R": "#e5d2ff",                                      # the Fusion Crystal's violet, on its own seed only
    "K": "#3a1a12", "M": "#6e2c18",                                                      # cold firebox
}
UNSHADED = set("gG\u03c4\u03c5\u03c8\u03c7\u03c6\u03c3")   # a tank's frame is in pieces, which a bevel each would show


def rgba(key):
    value = PALETTE[key]
    return int(value[1:3], 16), int(value[3:5], 16), int(value[5:7], 16), 255


class Tex:
    """A 16x16 source material drawn with palette keys."""

    def __init__(self, fill):
        self.g = [[fill] * N for _ in range(N)]

    def px(self, x, y, key):
        if 0 <= x < N and 0 <= y < N:
            self.g[y][x] = key

    def rect(self, x0, y0, x1, y1, key):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.px(x, y, key)


def engraved(t, x0, y0, x1, y1, groove, lip):
    """A panel line cut into the plate: shadow on the top/left side, lit edge on the bottom/right."""
    t.rect(x0, y0, x1, y0, groove)
    t.rect(x0, y0, x0, y1, groove)
    t.rect(x0 + 1, y1, x1, y1, lip)
    t.rect(x1, y0 + 1, x1, y1, lip)


def plate(hi, mid, lo, deep):
    t = Tex(mid)
    t.rect(0, 0, 15, 0, hi)
    t.rect(0, 0, 0, 15, hi)
    t.rect(1, 11, 14, 14, lo)
    t.rect(0, 15, 15, 15, deep)
    t.rect(15, 1, 15, 15, deep)
    engraved(t, 3, 3, 12, 12, deep, hi)
    t.rect(4, 4, 8, 4, hi)                 # sheen on the inner plate
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        t.px(x, y, hi)
    return t


def tank_frame(line, shape):
    """A tank's frame, by where it falls in the block: a light outer line and, along the window, a line in the colour of its kind. An upright
    has them lengthwise, a strip crosswise, so they run on across a joint; only a true corner turns them, around the window's corner."""
    t = Tex("\u03c4")
    start, end = (2, 13) if shape == "corner" else (0, 15)
    if shape != "column":
        t.rect(0, 0, 15, 0, "\u03c5")
        t.rect(0, 15, 15, 15, "\u03c5")
        t.rect(start, 2, end, 2, line)
        t.rect(start, 13, end, 13, line)
    if shape != "strip":
        t.rect(0, 0, 0, 15, "\u03c5")
        t.rect(15, 0, 15, 15, "\u03c5")
        t.rect(2, start, 2, end, line)
        t.rect(13, start, 13, end, line)
    return t


def oven_body():
    """The body without a panel in each block, for a machine that must read as one."""
    t = Tex("3")
    t.rect(0, 0, 15, 1, "2")
    t.rect(0, 13, 15, 15, "4")
    t.rect(0, 2, 15, 2, "5")
    t.rect(0, 12, 15, 12, "5")
    return t


def oven_top():
    t = Tex("2")
    t.rect(0, 0, 15, 0, "1")
    t.rect(0, 15, 15, 15, "4")
    return t


def tank_plate():
    """A tank's lid and floor: a check that tiles, so nothing marks where its blocks meet."""
    t = Tex("\u03c8")
    for x in range(16):
        for y in range(16):
            if (x // 2 + y // 2) % 2:
                t.px(x, y, "\u03c7")
    return t


def body():
    t = Tex("3")
    t.rect(0, 0, 15, 1, "2")
    t.rect(0, 13, 15, 15, "4")
    engraved(t, 2, 2, 13, 13, "5", "2")
    for x, y in ((4, 4), (11, 4), (4, 11), (11, 11)):
        t.px(x, y, "b")
        t.px(x, y + 1, "4")
    t.rect(7, 5, 9, 5, "G")                # status light
    t.px(6, 5, "h")
    return t


def plinth():
    t = Tex("4")
    t.rect(0, 14, 15, 14, "c")
    t.rect(0, 15, 15, 15, "5")
    return t


def trim():
    t = Tex("b")
    t.rect(0, 0, 15, 1, "a")
    t.rect(0, 3, 15, 3, "c")
    return t


def lid():
    t = Tex("2")
    t.rect(0, 0, 15, 0, "1")
    t.rect(0, 0, 0, 15, "1")
    t.rect(0, 15, 15, 15, "4")
    t.rect(15, 0, 15, 15, "4")
    engraved(t, 3, 3, 12, 12, "4", "1")
    for x in range(5, 11, 2):
        t.rect(x, 5, x, 10, "4")
    return t


def grille():
    t = Tex("4")
    t.rect(0, 0, 15, 0, "2")
    t.rect(0, 15, 15, 15, "5")
    for y in range(2, 14, 3):
        t.rect(1, y, 14, y, "s")
        t.rect(1, y + 1, 14, y + 1, "5")
        t.rect(1, y + 2, 14, y + 2, "2")
    return t


def pipe():
    t = Tex("b")
    for y in range(N):
        for x in range(N):
            t.px(x, y, "a" if x % 4 == 0 else "b" if x % 4 in (1, 3) else "c")
    t.rect(0, 6, 15, 6, "5")               # coupling band
    return t


def pad():
    t = Tex("2")
    t.rect(0, 0, 15, 0, "1")
    t.rect(0, 0, 0, 15, "1")
    for y in range(N):
        for x in range(N):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if 4.2 < d <= 5.2:
                t.px(x, y, "G")               # landing ring
            elif d <= 4.2:
                t.px(x, y, "3")
            if d <= 1.6:
                t.px(x, y, "B" if x + y < 15 else "C")   # charging contact
    return t


def swarm_pad():
    """Builder Swarm Dock pad: a small landing frame under each Mini Drone's parking spot (DroneEntity.CORNER_OFFSET)."""
    t = Tex("2")
    t.rect(0, 0, 15, 0, "1")
    t.rect(0, 0, 0, 15, "1")
    for cx, cy in ((5, 5), (10, 5), (5, 10), (10, 10)):
        t.rect(cx - 1, cy - 1, cx + 1, cy + 1, "H")
        t.px(cx, cy, "G")
    return t


def print_bed():
    """Drone Assembler pad: dark build plate, engraved centre cross, dim cyan corner brackets, copper contact."""
    t = Tex("3")
    t.rect(0, 0, 15, 0, "2")
    t.rect(0, 0, 0, 15, "2")
    t.rect(3, 8, 12, 8, "4")
    t.rect(8, 3, 8, 12, "4")
    for cx, cy, dx, dy in ((3, 3, 1, 1), (12, 3, -1, 1), (3, 12, 1, -1), (12, 12, -1, -1)):
        t.px(cx, cy, "H")
        t.px(cx + dx, cy, "H")
        t.px(cx, cy + dy, "H")
    t.rect(7, 7, 8, 8, "C")
    return t


def bed():
    """The Fabricator's conveyor bed: dark, with slats every other texel where the card runs (x 4..11)."""
    t = Tex("4")
    for r in range(0, N, 2):
        t.rect(4, r, 11, r, "2")
    return t


def card():
    """A circuit card, painted by world position (game frame: x = column, z = row) where it rests on the tray
    (x 4.75..11.25, z 2.25..5.55): dark board with an emerald trace frame round its chip and steel vias.
    FabricatorRenderer samples the same texels from textures/entity/fabricator_card.png."""
    t = Tex("3")
    t.rect(6, 2, 10, 2, "H")
    t.rect(6, 5, 10, 5, "H")
    t.rect(6, 2, 6, 5, "H")
    t.rect(10, 2, 10, 5, "H")
    t.px(6, 2, "a")
    t.px(10, 2, "a")
    return t


def tray_front():
    """The Fabricator tray's front edge: its readout, four segments dark until the machine works (row y 8..9)."""
    t = lid()
    t.rect(0, 7, 15, 7, "h")
    return t


def chip_pins():
    """Chip sides: black moulding with steel pins every other pixel."""
    t = Tex("5")
    for x in range(0, N, 2):
        t.rect(x, 0, x, 15, "b")
    return t


def chip_top():
    """Chip lid: mid steel, so the chip reads against the dark board, with a lighter pin-1 corner."""
    t = Tex("c")
    for x in range(N):
        for y in range(N):
            if (x + y) % 5 == 0:
                t.px(x, y, "b")
    t.px(7, 3, "a")
    return t


def drum():
    """The Fabricator's acid drum: plain dark teal shell with a weld seam ring."""
    t = Tex("3")
    t.rect(8, 0, 8, 15, "4")
    t.rect(9, 0, 9, 15, "2")
    return t


def slot():
    """A dark recess: a sight glass's slot (the renderer draws the fluid in front of it), a socket."""
    t = Tex("s")
    t.rect(0, 0, 15, 0, "5")
    t.rect(0, 0, 0, 15, "5")
    return t


def rotor():
    """A turning collar at rest: steel with a dark seam every fourth texel, like a roller's faces."""
    t = Tex("b")
    for x in range(0, N, 4):
        t.rect(x, 0, x, 15, "5")
        t.rect(x + 1, 0, x + 1, 15, "a")
        t.rect(x + 3, 0, x + 3, 15, "c")
    return t


def liquid():
    t = Tex("o")
    for x, y in ((6, 5), (9, 9), (5, 11), (10, 4)):
        t.px(x, y, "m")                    # bubbles
    t.rect(0, 13, 15, 15, "p")
    return t


def fire(lit):
    t = Tex("x")
    for y in range(N):
        for x in range(N):
            if lit:
                key = "u" if y > 10 else "v" if y > 8 else "w" if y > 6 or (x * 5 + y) % 7 < 2 else "x"
            else:
                key = "M" if y > 11 and (x * 3 + y) % 5 < 2 else "K"
            t.px(x, y, key)
    return t


def light(lit):
    t = Tex("G" if lit else "h")
    t.rect(6, 6, 9, 9, "g" if lit else "h")
    return t


def strainer():
    """Steel slats with dark slits, to read through water. Upright: below y 0 bake_face() only varies a material by column."""
    t = Tex("b")
    for x in range(1, N, 3):
        t.rect(x, 0, x, N - 1, "4")
    return t


MATERIALS = {"body": body(), "plinth": plinth(), "trim": trim(), "lid": lid(), "grille": grille(), "pipe": pipe(),
             "pad": pad(), "swarm_pad": swarm_pad(), "print_bed": print_bed(), "bed": bed(), "card": card(), "tray_front": tray_front(), "chip_pins": chip_pins(),
             "chip_top": chip_top(), "drum": drum(), "slot": slot(), "rotor": rotor(), "liquid": liquid(),
             "strainer": strainer(), "beam": plate("c", "1", "2", "3"),
             **{f"{kind}_tank_{shape}": tank_frame(line, shape) for kind, line in (("liquid", "\u03c3"), ("gas", "\u03c6")) for shape in ("column", "strip", "corner")},
             "tank_plate": tank_plate(), "oven_body": oven_body(), "oven_top": oven_top(),
             "crystal": plate("P", "P", "Q", "Q"), "crystal_tip": plate("R", "R", "P", "P"), "copper": plate("A", "B", "C", "D")}
EMISSIVE = {"fire": "machine_fire", "light": "machine_light"}   # material -> texture name (+ "_off")
STILL_LIGHTS = {"light_off": "machine_light"}   # a lamp left off in both models: a block entity renderer lights it
PIPES = {"gas_pipe_up": "gas_pipe", "liquid_pipe_up": "liquid_pipe"}
DECALS = {"radiation_mark": "radiation_mark"}   # a box whose front shows a whole texture, finer than the atlas; the rest of it one texel   # vertical stubs drawn with the real pipe texture, like the pipe blocks


# --------------------------------------------------------------------------- models, front facing +z (south)
# Crossed boxes share their caps; each later box is pulled in by this much there, so one face wins where they
# overlap instead of the two flickering (z-fighting) with their differently baked patches
LAYER = 0.02


def octagon(x0, y0, z0, x1, y1, z1, cut, mat):
    return [((x0 + cut, y0, z0), (x1 - cut, y1, z1), mat),
            ((x0, y0 + LAYER, z0 + cut), (x1, y1 - LAYER, z1 - cut), mat)]


def round_tank(x0, y0, z0, x1, y1, z1, mat):
    """Twelve-sided column from three crossed boxes, close to a cylinder at block scale."""
    w = (x1 - x0) / 6
    return [((x0 + 2 * w, y0, z0), (x1 - 2 * w, y1, z1), mat),
            ((x0, y0 + LAYER, z0 + 2 * w), (x1, y1 - LAYER, z1 - 2 * w), mat),
            ((x0 + w, y0 + 2 * LAYER, z0 + w), (x1 - w, y1 - 2 * LAYER, z1 - w), mat)]


def roller(x0, x1, cy, cz, r, mat):
    """Twelve-sided cylinder lying along x: round_tank()'s section turned on its side."""
    return [((x0, cy - r / 3, cz - r), (x1, cy + r / 3, cz + r), mat),
            ((x0 + LAYER, cy - r, cz - r / 3), (x1 - LAYER, cy + r, cz + r / 3), mat),
            ((x0 + 2 * LAYER, cy - 2 * r / 3, cz - 2 * r / 3), (x1 - 2 * LAYER, cy + 2 * r / 3, cz + 2 * r / 3), mat)]


def zdisc(cx, cy, z0, z1, r, mat):
    """Twelve-sided disc facing the front: roller()'s section turned to lie along z."""
    assert z1 - z0 > 4 * LAYER, "too thin for its layers: the inner boxes would turn inside out"
    return [((cx - r / 3, cy - r, z0), (cx + r / 3, cy + r, z1), mat),
            ((cx - r, cy - r / 3, z0 + LAYER), (cx + r, cy + r / 3, z1 - LAYER), mat),
            ((cx - 2 * r / 3, cy - 2 * r / 3, z0 + 2 * LAYER), (cx + 2 * r / 3, cy + 2 * r / 3, z1 - 2 * LAYER), mat)]


def combustion_generator():
    cap = {"up": "lid", "south": "trim", "east": "trim"}
    return [
        ((1, 0, 1), (15, 1, 15), "plinth"),
        *octagon(2, 1, 2, 14, 9, 13, 2, {"up": "lid", "south": "body", "east": "body"}),
        *octagon(1.6, 6.5, 1.6, 14.4, 7.5, 13.4, 2, "copper"),     # copper band round the body
        ((14, 2, 5), (14.5, 6 - LAYER, 10 - LAYER), "grille"),      # intake grille, under the terminal's faces
        ((4 + LAYER, 2, 12.5), (12 - LAYER, 8, 13.5), "copper"),    # firebox door frame, inside the body's sides
        ((5, 3, 13), (11, 6.25, 13.75), "fire"),
        ((6.5, 3, 13.75), (7.25, 6.25, 14.25), "pipe"),             # grille bars
        ((8.75, 3, 13.75), (9.5, 6.25, 14.25), "pipe"),
        *octagon(4, 9, 4, 11, 10, 11, 0.5, cap),                    # dome, the top port's seat (5.5..10.5)
        ((2.25, 9, 4 + LAYER), (4.75, 15, 7), "pipe"),              # chimney, its cap clear of a top port's
        ((2, 12, 3.5), (5, 13, 7.5), "copper"),                     # collar (from x 5)
        ((2, 15, 3.5), (5, 16, 7.5), {"up": "plinth", "south": "trim", "east": "trim"}),
        ((8, 10, 3), (12, 12.5, 7), {"up": "grille", "south": "copper", "east": "copper"}),   # fuel hopper
        ((13.5, 3, 6), (15, 6, 10), "lid"),                         # FE terminal, dark so its light stands out
        ((15, 4, 7), (15.5, 5, 9), "light"),
    ]


def bio_digester():
    # same family as the generator: dark octagonal tank, copper bands, one window onto the sludge;
    # the gas leaves through a real Gas Pipe stub on top, so a pipe placed above joins it
    lid = {"up": "lid", "south": "trim", "east": "trim"}
    return [
        ((1, 0, 1), (15, 1, 15), "plinth"),
        *round_tank(2, 1, 2, 14, 13, 14, {"up": "lid", "south": "body", "east": "body"}),
        *round_tank(1.6, 3, 1.6, 14.4, 3.75, 14.4, "copper"),      # copper bands
        *round_tank(1.6, 10, 1.6, 14.4, 10.75, 14.4, "copper"),
        ((5.5, 4.5, 13.5), (10.5, 9.5, 14.5), "copper"),           # porthole
        ((4.5, 5.5, 13.5), (11.5, 8.5, 14.5 - LAYER), "copper"),
        ((6.5, 5.5, 14), (9.5, 8.5, 14.75), "liquid"),              # sludge behind it
        ((5.5, 6.5, 14), (10.5, 7.5, 14.75 - LAYER), "liquid"),
        ((7, 11.25, 13.75), (9, 12, 14.25), "light"),               # running light
        *round_tank(4, 13, 4, 12, 14.5, 12, lid),                   # domed top
        ((6, 14.5, 6), (10, 16, 10), "gas_pipe_up"),
    ]


def drone_dock(pad_top="pad"):
    # pad top stays at 6 px and the beacons sit on the corners, where the drone and DockRenderer expect them
    pad = {"up": pad_top, "south": "trim", "east": "trim"}
    parts = [
        ((0, 0, 0), (16, 1, 16), "plinth"),
        *octagon(1, 1, 1, 15, 4.5, 15, 3, {"up": "body", "south": "body", "east": "body"}),
        *octagon(0.6, 2.5, 0.6, 15.4, 3.25, 15.4, 3, "copper"),
        *octagon(2, 4.5, 2, 14, 6, 14, 2, pad),
    ]
    for x, z in ((0, 0), (13, 0), (0, 13), (13, 13)):
        parts += [
            ((x + 0.25, 1, z + 0.25), (x + 2.75, 8, z + 2.75), {"up": "lid", "south": "body", "east": "body"}),
            ((x, 5, z), (x + 3, 5.75, z + 3), "copper"),
            ((x + 0.75, 8, z + 0.75), (x + 2.25, 9, z + 2.25), "light"),
        ]
    return parts


def drone_assembler():
    # The pad top stays at 4 px and nothing solid enters the scan ring's square (2.2..13.8) below 13.2:
    # AssemblerRenderer draws the drone being built there as a hologram
    dark = {"up": "lid", "south": "body", "east": "body"}
    bed = {"up": "print_bed", "south": "trim", "east": "trim"}
    return [
        ((0, 0, 0), (16, 1, 16), "plinth"),
        *octagon(1, 1, 1, 15, 3, 15, 3, {"up": "body", "south": "body", "east": "body"}),
        *octagon(0.6, 1.75, 0.6, 15.4, 2.5, 15.4, 3, "copper"),
        *octagon(2, 3, 2, 14, 4, 14, 2, bed),
        ((3, 1, 0.5), (13, 14, 2), dark),                                                # spine, chamber face at z 2
        ((2.6, 6, 0.1), (13.4, 7, 1.9), "copper"),
        ((4.5, 4.5, 2), (5.5, 12.5, 2.15), "light"),                                     # emitter strips facing the drone
        ((10.5, 4.5, 2), (11.5, 12.5, 2.15), "light"),
        # service hatch, narrower than a back port's cheeks (5..6, 10..11) so their faces never meet
        ((5.25, 7.75, 0.2), (10.75, 12.5, 0.5), {"up": "lid", "south": "body", "east": "body", "north": "grille"}),
        ((7, 12.75, 0.25), (9, 13.5, 0.5), "light"),                                     # status lamp on the back
        ((2.5, 14, 0.25), (13.5, 15.25, 2.5), dark),                                     # spine cap
        ((6, 14.5, 0.5), (10, 16, 10.5), dark),                                          # cantilever arm
        ((5.5, 13.75, 5.25), (10.5, 15.25, 10.75), "copper"),                            # print head
        ((6.5, 13.25, 6.5), (9.5, 13.75, 9.5), "light"),                                 # lens, above the ring's top
    ]


def fabricator():
    # A card runs on the bed from under a steel feed roller and the horizontal acid drum, out between the pinch
    # roller and the bed onto the front tray. While it works FabricatorRenderer draws the moving card, chip, rollers,
    # print head and progress bar, so the lit model leaves the card out and the lip lamp off; the unlit model shows
    # the card at rest. The drum is 4.8 px across (z 5.6..10.4), so the top port's housing saddles it 0.1 px clear
    dark = {"up": "lid", "south": "body", "east": "body"}
    bed_spec = {"up": "bed", "south": "plinth", "east": "plinth"}
    card_spec = {"up": "card", "south": "plinth", "east": "plinth"}   # dark edges: the emerald traces never meet the mint
    chip = {"up": "chip_top", "south": "chip_pins", "east": "chip_pins"}
    return [
        ((1, 0, 1), (15, 1, 15), "plinth"),
        *octagon(2, 1, 2, 14, 8, 14, 2, dark),
        *octagon(1.6, 2.5, 1.6, 14.4, 3.25, 14.4, 2, "copper"),                  # foot band, below the ports (5..11)
        ((2.5, 5.5, 12), (3.5, 7, 12.15), "light"),                              # status lights on the front corners
        ((12.5, 5.5, 12), (13.5, 7, 12.15), "light"),
        ((5, 1.5, 1.85), (11, 4.75, 2), {"up": "lid", "south": "grille", "east": "grille", "north": "grille"}),  # back vent
        ((2, 8, 2.5), (3.5, 12.5, 10.75), dark),                                 # bearing blocks for the roller and drum
        ((12.5, 8, 2.5), (14, 12.5, 10.75), dark),
        ((1.6, 9.25, 2.75), (2, 11, 4.75), "copper"),                            # roller bearing caps, clear of a side
        ((14, 9.25, 2.75), (14.4, 11, 4.75), "copper"),                          # port's collar (from z 5)
        ((4, 8, 2), (12, 9.5, 5), "lid"),                                        # feed table
        ((4, 8, 10.25), (12, 9.5, 14), {"up": "lid", "south": "tray_front", "east": "lid"}),   # front tray, readout
        ((4.5, 9.5, 2.5), (11.5, 10, 13.75), bed_spec),                          # conveyor bed
        ((4.75, 10, 10.45), (11.25, 10.3, 13.75), card_spec, "unlit"),           # the card at rest, and its chip
        ((6.5, 10.3, 11.45), (9.5, 11.05, 13.25), chip, "unlit"),
        *roller(4.75, 11.25, 11.08, 10.6, 0.7, "pipe"),                          # pinch roller at the exit (the renderer
        ((4.25, 8, 9.75), (4.75, 11.9, 11.5), dark),                             # turns a 0.77 one over it), its bearings
        ((11.25, 8, 9.75), (11.75, 11.9, 11.5), dark),
        *roller(3.5, 12.5, 11.25, 3.75, 1.25, "pipe"),                           # feed roller, resting on the card
        *roller(4.25, 11.75, 12.75, 8, 2.4, "drum"),                             # acid drum
        *roller(3.5, 4.25, 12.75, 8, 2.75, "copper"),                            # its end bands
        *roller(11.75, 12.5, 12.75, 8, 2.75, "copper"),
        ((5.5, 11.98, 10.4), (10.5, 12.7, 10.55), "light_off"),                  # lip lamp: the renderer's print head
        ((12.5, 12.35, 7), (12.6, 13.85, 9), "plinth"),                         # level gauges on the end bands, each
        ((12.6, 12.6, 7.25), (12.75, 13.6, 8.75), "light"),                      # on a dark plate so it outshines the mint
        ((3.4, 12.35, 7), (3.5, 13.85, 9), "plinth"),
        ((3.25, 12.6, 7.25), (3.4, 13.6, 8.75), "light"),
    ]


def acid_synthesizer():
    # A contact tower between two tanks: water on the left and acid on the right from the front (the flip to north
    # mirrors x), piped straight into their sides. AcidSynthesizerRenderer draws the fluids in the slots and turns a
    # collar over the still one (keep ACID_* in step with it), at the tower's axis (8, 6)
    dark = {"up": "lid", "south": "body", "east": "body"}
    cap = {"up": "lid", "south": "trim", "east": "trim"}
    parts = [
        ((1, 0, 1), (15, 1, 15), "plinth"),
        *octagon(1, 1, 1, 15, 4, 15, 3, dark),                              # base
        *octagon(0.6, 2, 0.6, 15.4, 2.75, 15.4, 3, "copper"),               # foot band, below the ports
        *round_tank(5.5, 4, 3.5, 10.5, 10.5, 8.5, dark),                    # tower, below the collar
        *round_tank(5.1, 9, 3.1, 10.9, 9.75, 8.9, "copper"),
        *round_tank(5.55, 10.6, 3.55, 10.45, 12.65, 8.45, "rotor"),         # still collar, under the renderer's
        *round_tank(5.5, 12.75, 3.5, 10.5, 14, 8.5, dark),                  # tower, above the collar
        *round_tank(5.1, 13, 3.1, 10.9, 13.75, 8.9, "copper"),
        *round_tank(6, 14, 4, 10, 15, 8, cap),
        ((7.25, 15, 5.25), (8.75, 16, 6.75), "pipe"),                       # chimney
        ((6.5, 12.8, 1.75), (9.5, 13.75, 3.6), dark),                       # sulphur chute, above the collar
        ((6, 13.75, 1.25), (10, 15, 3.6), {"up": "grille", "south": "copper", "east": "copper"}),   # sulphur hopper
        *zdisc(8, 6.25, 8.4, 11.75, 2.25, dark),                            # pump between the tanks
        *zdisc(8, 6.25, 11.75, 12, 2.25, "copper"),
        *zdisc(8, 6.25, 12, 12.1, 1.25, "slot"),                            # its socket, and a status lamp
        ((7.5, 5.75, 12.1), (8.5, 6.75, 12.3), "light"),
    ]
    for x0 in (10.75, 1):                                                   # water, acid
        x1 = x0 + 4.25
        parts += [
            *round_tank(x0, 4, 6, x1, 12.5, 10.25, dark),
            *round_tank(x0 - 0.2, 4.75, 5.8, x1 + 0.2, 5.5, 10.45, "copper"),
            *round_tank(x0 - 0.2, 11.25, 5.8, x1 + 0.2, 12, 10.45, "copper"),
            *round_tank(x0 + 0.5, 12.5, 6.5, x1 - 0.5, 13.25, 9.75, cap),
            ((x0 + 0.625, 5.5, 10), (x1 - 0.625, 11.25, 10.6), "trim"),     # gauge frame, 3 px
            ((x0 + 1.125, 5.9, 10.6), (x1 - 1.125, 10.85, 10.7), "slot"),   # sight glass, 2 px
        ]
    return parts


def electrolyzer():
    """A low base, a tall bath cell, a cathode column behind and a hopper. The boxes tagged "unlit" are the feeder punch
    at rest, the pose ElectrolyzerRenderer starts each stroke from; that renderer draws them while it works."""
    dark = {"up": "lid", "south": "body", "east": "body"}
    return [((0, 0, 0), (16, 1, 16), {"up": "lid", "down": "lid", "south": "plinth", "east": "plinth"}),   # to the edge, under a port's post
            *octagon(2, 1, 2, 14, 8, 14, 2, dark),                     # wide low base
            *octagon(1.6, 2.5, 1.6, 14.4, 3.25, 14.4, 2, "copper"),    # foot band, below the port
            ((2.4, 5.35, 14), (3.6, 7.15, 14.08), "plinth"), ((2.5, 5.5, 14.08), (3.5, 7, 14.2), "light"),
            ((12.4, 5.35, 14), (13.6, 7.15, 14.08), "plinth"), ((12.5, 5.5, 14.08), (13.5, 7, 14.2), "light"),
            *octagon(3.5, 8, 7, 12.5, 15, 13.4, 1.5, dark),            # tall bath cell, narrower than the base
            *round_tank(2.5, 8, 2.5, 7.5, 15.2, 6.9, dark),            # cathode column, taller than the cell
            *round_tank(2.2, 11, 2.2, 7.8, 11.75, 7.2, "copper"),
            *round_tank(3, 15.2, 3, 7, 16, 6.4, {"up": "lid", "south": "trim", "east": "trim"}),
            # the window, with open gaps for the punch (x 5.5..8.5) and the grain (x 9..10.6)
            ((5.5, 10.2, 13.4), (10.6, 14.9, 13.8), "slot"),
            ((5.5, 10.2, 13.8), (10.6, 10.7, 14.2), "light"),          # the bath
            ((4.5, 9.2, 13.4), (11.5, 10.2, 14.5), dark),
            ((4.5, 10.2, 13.4), (5.5, 15, 14.5), dark),
            ((10.6, 10.2, 13.4), (11.5, 15, 14.5), dark),
            ((8.5, 10.2, 13.4), (9, 15, 14.5), dark),
            ((5.4, 15.05, 12.8), (8.6, 16, 14.8), dark),               # punch housing
            ((8.7, 15, 7.5), (12.5, 16, 14.8), dark),                  # hopper
            ((9, 14.6, 13.4), (10.6, 15, 14.5), dark),                 # spout over the grain's gap
            ((5.8, 13.9, 14.26), (8.3, 14.9, 14.55), "trim", "unlit"),      # the punch's head and rod at rest
            ((6.65, 14.9, 14.3), (7.85, 15, 14.5), "pipe", "unlit")]


def pyrolysis_oven():
    """A low body, a banded retort tube along the front with a glowing slit, the methane tank behind. The boxes tagged
    "unlit" are the shuttle and the fibre's stub at rest; PyrolysisOvenRenderer draws them while it works."""
    dark = {"up": "lid", "south": "body", "east": "body"}
    cap = {"up": "lid", "south": "trim", "east": "trim"}

    def lamp(x):
        return [((x, 4, 15), (x + 1.6, 6, 15.08), "plinth"), ((x + 0.1, 4.15, 15.08), (x + 1.5, 5.85, 15.2), "light")]

    return [((0, 0, 0), (16, 1, 16), {"up": "lid", "down": "lid", "south": "plinth", "east": "plinth"}),
            *octagon(1, 1, 1, 15, 8.5, 15, 2, dark),                   # low body, to 1 px of every side for the ports
            *lamp(2.2), *lamp(12.2),
            *round_tank(8.5, 8.5, 2, 14, 15.2, 6.9, dark),             # methane tank behind
            *round_tank(8.2, 11.5, 1.7, 14.3, 12.3, 7.2, "copper"),
            *round_tank(9, 15.2, 2.5, 13.5, 16, 6.4, cap),
            ((10, 10.8, 6.9), (12.5, 12.8, 7.6), "pipe"),              # tank to tube
            *roller(1.2, 14.8, 12, 11, 3.5, dark),                     # the retort tube
            *roller(1.8, 2.6, 12, 11, 3.9, "copper"),
            *roller(13.4, 14.2, 12, 11, 3.9, "copper"),
            ((2.8, 11.1, 14.5), (13.2, 12.9, 14.7), "slot"),
            ((3.2, 11.5, 14.7), (12.8, 12.5, 14.82), "fire"),          # the glowing slit
            ((10, 10.6, 15), (13, 13.4, 15.7), "copper", "unlit"),     # the shuttle and the fibre's stub at rest
            ((11.8, 11.5, 14.9), (13.1, 12.7, 15.2), "trim", "unlit")]


def bio_digester_lone():
    """Alone, the machine as it was, with a plate where the upper drum docks and one on the side the tank goes; no gas stub, as a frame goes up there."""
    return [*[box for box in bio_digester() if box[2] != "gas_pipe_up"],
            ((6, 14.5, 6), (10, 15.2, 10), "copper"), ((6.8, 15.2, 6.8), (9.2, 15.4, 9.2), "slot"),
            ((14, 1.8, 6.2), (14.55, 4.2, 9.8), "copper"), ((14.55, 2.3, 6.8), (14.7, 3.7, 9.2), "slot")]


def bio_digester_parts():
    """The drum two blocks tall and a tank beside it: the lower drum, then what moves in the opening (upper drum, its two rings, the tank),
    then what only the open machine shows. Upper parts are a hair inside the drum's planes, as they start inside it."""
    dark = {"up": "lid", "south": "body", "east": "body"}
    lid = {"up": "lid", "south": "trim", "east": "trim"}
    body = [box for box in bio_digester() if box[2] != "gas_pipe_up"][:-len(round_tank(4, 13, 4, 12, 14.5, 12, lid))]
    body[0] = ((1, 0, 1), (31, 1, 15), "plinth")      # one base under drum and tank
    top = [*round_tank(2.02, 13, 2.02, 13.98, 29, 13.98, dark), *round_tank(4, 29, 4, 12, 30.5, 12, lid),
           ((6, 30.5, 6), (10, 31.2, 10), "trim"), ((7.2, 31.2, 7.2), (8.8, 31.6, 8.8), "lid")]    # a closed vent cap

    def ring(y):
        return round_tank(1.62, y, 1.62, 14.38, y + 0.75, 14.38, "copper")

    z0, z1, face = 2.3, 13.7, 14.1
    tank = [*round_tank(18, 1, z0, 30, 13, z1, dark), *round_tank(17.6, 3, z0 - 0.4, 30.4, 3.75, z1 + 0.4, "copper"), *round_tank(17.6, 10, z0 - 0.4, 30.4, 10.75, z1 + 0.4, "copper"),
            *round_tank(20, 13, z0 + 2, 28, 14.5, z1 - 2, lid), ((22.5, 14.5, 7), (25.5, 14.9, 9), "trim"),
            ((21.5, 4.5, z1 - 0.5), (26.5, 9.5, face), "trim"), ((22.3, 5, face), (25.7, 9, face + 0.1), "slot"),     # gauge frame and sight glass
            ((21.7, 6, face), (22.1, 6.35, face + 0.07), "body"), ((25.9, 6, face), (26.3, 6.35, face + 0.07), "body"),
            ((21.7, 7.6, face), (22.1, 7.95, face + 0.07), "body"), ((25.9, 7.6, face), (26.3, 7.95, face + 0.07), "body"),
            *roller(13.5, 18.5, 6.2, 8, 1.3, dark), *roller(14, 14.5, 6.2, 8, 1.65, "copper"), *roller(17, 17.5, 6.2, 8, 1.65, "copper")]    # its pipe to the drum
    finish = [((7, 25.5, 13.75), (9, 26.25, 14.25), "light")]     # a second running light, and steel straps on the front flats
    for x in (6.5, 9.1):
        finish += [((x, 13.2, 13.98), (x + 0.4, 16.2, 14.18), "trim"), ((x, 17.3, 13.98), (x + 0.4, 22.7, 14.18), "trim"), ((x, 23.9, 13.98), (x + 0.4, 28.4, 14.18), "trim")]
    return body, top, ring(16.5), ring(23), tank, finish


def fabricator_parts():
    """The line one block longer, into a block behind the machine: a bed, two more rollers and the card magazine it starts from. All of it starts inside
    the machine: first the base slides out, then the rest rises out of the base (the base's planes a hair inside the machine's). FabricatorRenderer runs the card and turns the rollers."""
    dark = {"up": "lid", "south": "body", "east": "body"}
    base = [((1.02, 0, -15), (14.98, 1, 1), "plinth"), *octagon(2.02, 1, -14, 13.98, 7.98, 2, 2, dark), *octagon(1.62, 2.5, -13.6, 14.38, 3.25, 1.98, 2, "copper"),
            ((5, 1.5, -14.15), (11, 4.75, -14), {"up": "lid", "south": "grille", "east": "grille", "north": "grille"})]
    bed = [((4, 8, -14), (12, 9.5, 2), "lid"), ((4.5, 9.5, -13.5), (11.5, 10, 2.5), {"up": "bed", "south": "plinth", "east": "plinth"}),
           ((2, 8, -14), (3.5, 9.6, 2.5), dark), ((12.5, 8, -14), (14, 9.6, 2.5), dark)]

    def idler(z):
        return [*roller(3.5, 12.5, 11.2, z, 0.9, "pipe"), ((2, 9.6, z - 0.8), (3.5, 11.6, z + 0.8), dark), ((12.5, 9.6, z - 0.8), (14, 11.6, z + 0.8), dark),
                ((1.6, 9.25, z - 0.8), (2, 10.9, z + 0.8), "copper"), ((14, 9.25, z - 0.8), (14.4, 10.9, z + 0.8), "copper")]

    magazine = [((4.5, 10.4, -13.5), (11.5, 12.8, -10), dark), ((5.5, 10.5, -10), (10.5, 11.7, -9.88), "slot")]
    finish = [((2.5, 5.5, -12.15), (3.5, 7, -12), "light"), ((12.5, 5.5, -12.15), (13.5, 7, -12), "light")]
    return base, bed, idler(-3), idler(-9), magazine, finish


def fabricator_lone():
    return [*fabricator(), ((4, 1.2, 0.9), (12, 3.2, 1.6), "copper"), ((5, 1.7, 0.75), (11, 2.7, 0.9), "slot")]


def pyrolysis_oven_lone():
    return [*pyrolysis_oven(), ((15, 6.2, 6.2), (15.5, 8, 9.8), "copper"), ((15.5, 6.6, 6.8), (15.65, 7.6, 9.2), "slot")]


def pyrolysis_oven_open():
    """The oven on two blocks, one machine: one body, one tube with one slit, one long tank, in materials that don't repeat a panel every block."""
    long = {"up": "oven_top", "south": "oven_body", "east": "oven_body"}
    cap = {"up": "lid", "south": "trim", "east": "trim"}

    def lamp(x):
        return [((x, 4, 15), (x + 1.6, 6, 15.08), "plinth"), ((x + 0.1, 4.15, 15.08), (x + 1.5, 5.85, 15.2), "light")]

    fire = [((x, 11.5, 14.7), (min(x + 8, 28.8), 12.5, 14.82), "fire") for x in (3.2, 11.2, 19.2, 27.2)]
    return [((0, 0, 0), (32, 1, 16), {"up": "oven_top", "down": "oven_top", "south": "plinth", "east": "plinth"}),
            *octagon(1, 1, 1, 31, 8.5, 15, 2, long), *lamp(2.2), *lamp(28.2),
            *pyrolysis_oven_tank(),
            *roller(1.2, 30.8, 12, 11, 3.5, long), *roller(1.8, 2.6, 12, 11, 3.9, "copper"), *roller(29.4, 30.2, 12, 11, 3.9, "copper"),
            ((2.8, 11.1, 14.5), (29.2, 12.9, 14.7), "slot"), *fire,
            ((26, 10.6, 15), (29, 13.4, 15.7), "copper", "unlit"), ((27.8, 11.5, 14.9), (29.1, 12.7, 15.2), "trim", "unlit")]


def pyrolysis_oven_tank():
    long = {"up": "oven_top", "south": "oven_body", "east": "oven_body"}
    cap = {"up": "lid", "south": "trim", "east": "trim"}
    return [*octagon(8.4, 8.5, 1.9, 23.6, 15.4, 7, 1.5, long), *octagon(8.1, 11.5, 1.6, 23.9, 12.3, 7.3, 1.5, "copper"), *octagon(9, 15.4, 2.4, 23, 16.2, 6.5, 1.2, cap),
            ((14.75, 10.8, 7), (17.25, 12.8, 7.6), "pipe")]


def pyrolysis_oven_moving():
    """What moves in the opening: the base and the tube sliding out of the machine, a hair inside its planes, and the long tank rising round the short one."""
    long = {"up": "oven_top", "south": "oven_body", "east": "oven_body"}
    base = [((16, 0, 0.02), (32, 1, 15.98), {"up": "oven_top", "down": "oven_top", "south": "plinth", "east": "plinth"}), *octagon(15, 1, 1.02, 31, 8.48, 14.98, 2, long)]
    tube = [*roller(14.8, 30.8, 12, 11, 3.48, long), *roller(29.4, 30.2, 12, 11, 3.9, "copper"), ((15, 11.1, 14.5), (29.2, 12.9, 14.7), "slot")]
    return base, tube, pyrolysis_oven_tank()


def acid_synthesizer_lone():
    """Alone, the machine as it was on one block, with a docking plate at the foot of each end, where its frames go."""
    return [*acid_synthesizer(),
            ((0.3, 1.1, 4.5), (0.8, 3.7, 11.5), "copper"), ((0.15, 1.6, 5.5), (0.7, 3.2, 10.5), "slot"),
            ((15.2, 1.1, 4.5), (15.7, 3.7, 11.5), "copper"), ((15.3, 1.6, 5.5), (15.85, 3.2, 10.5), "slot")]


def acid_synthesizer_parts():
    """The same machine on 3x1x2, the machine block at the bottom of the middle: the base, then what rises out of it in the opening
    (water tank, acid tank, tower, tower top, chimney), and the pump's lamp. AcidSynthesizerRenderer keeps its glasses and its collar on these numbers."""
    dark = {"up": "lid", "south": "body", "east": "body"}
    cap = {"up": "lid", "south": "trim", "east": "trim"}
    cx, tz, tr, rt = 8, 6, 4.6, 6.5     # tower axis and radius, tank radius
    bt, col0, col1 = 30, 21.16, 25.26   # tower top, collar
    base = [((-15, 0, 1), (31, 1, 15), "plinth"), *octagon(-15, 1, 1, 31, 4, 15, 3.5, dark), *octagon(-15.4, 2, 0.6, 31.4, 2.75, 15.4, 3.5, "copper")]
    front = tz + tr

    def hoop(y):
        return round_tank(cx - tr - 0.4, y, tz - tr - 0.4, cx + tr + 0.4, y + 0.75, tz + tr + 0.4, "copper")

    def straps(y0, y1):
        return [((cx + dx - 0.35, y0, front + 0.02), (cx + dx + 0.35, y1, front + 0.22), "trim") for dx in (-tr * 0.38, tr * 0.38)]

    pump_r, pump_y, pump_z = 3.6, 8.5, front - 0.1
    tower = [*round_tank(cx - tr, 4, tz - tr, cx + tr, col0 - 0.1, tz + tr, dark),
             *round_tank(cx - tr + 0.05, col0, tz - tr + 0.05, cx + tr - 0.05, col1, tz + tr - 0.05, "rotor"),   # still collar, under the renderer's
             *hoop(6.06), *hoop(10.01), *hoop(17.73), *straps(4.3, col0 - 0.3),
             *zdisc(cx, pump_y, pump_z, pump_z + 2.5, pump_r, dark), *zdisc(cx, pump_y, pump_z + 2.5, pump_z + 2.75, pump_r, "copper"),
             *zdisc(cx, pump_y, pump_z + 2.75, pump_z + 2.85, pump_r * 0.55, "slot")]
    lamp = [((cx - 0.5, pump_y - 0.5, pump_z + 2.85), (cx + 0.5, pump_y + 0.5, pump_z + 3.05), "light")]
    scale = tr / 2.5
    top = [*round_tank(cx - tr, col1 + 0.1, tz - tr, cx + tr, bt, tz + tr, dark), *hoop(25), *hoop(29), *straps(col1 + 0.3, bt - 1.3),
           *round_tank(cx - tr + 0.6, bt, tz - tr + 0.6, cx + tr - 0.6, bt + 1, tz + tr - 0.6, cap),
           ((cx - 1.5 * scale, col1 + 0.15, tz - tr - 1.75 * scale), (cx + 1.5 * scale, col1 + 2.05, tz - tr + 0.1), dark),    # sulphur chute
           ((cx - 2 * scale, col1 + 2.05, tz - tr - 2.25 * scale), (cx + 2 * scale, bt + 1, tz - tr + 0.1), {"up": "grille", "south": "copper", "east": "copper"})]
    chimney_r = 1.88
    chimney = [((cx - chimney_r, bt + 1, tz - chimney_r), (cx + chimney_r, 32, tz + chimney_r), "pipe")]

    def tank(centre, sign):
        z, tall = tz + 2, 25
        x0, x1, glass = centre - rt, centre + rt, 2.82
        y0, y1, face = 5.5, 23.75, z + rt
        parts = [*round_tank(x0, 4, z - rt, x1, tall, z + rt, dark)]
        for a, b in ((4.75, 5.5), (14.1, 14.85), (23.75, 24.5)):
            parts += round_tank(x0 - 0.2, a, z - rt - 0.2, x1 + 0.2, b, z + rt + 0.2, "copper")
        parts += [*round_tank(x0 + 1, tall, z - rt + 1, x1 - 1, tall + 1.5, z + rt - 1, cap),
                  *round_tank(centre - 1.4, tall + 1.5, z - 1.4, centre + 1.4, tall + 1.95, z + 1.4, "trim"),
                  ((centre - glass, y0, face - 0.25), (centre + glass, y1, face + 0.35), "trim"),                   # gauge frame
                  ((centre - glass + 0.65, y0 + 0.45, face + 0.35), (centre + glass - 0.65, y1 - 0.45, face + 0.45), "slot")]   # sight glass
        y = y0 + 1.2
        while y < y1 - 1.2:     # scale ticks on the frame's margins
            parts += [((centre + side * (glass - 0.35) - 0.2, y, face + 0.35), (centre + side * (glass - 0.35) + 0.2, y + 0.35, face + 0.42), "body") for side in (-1, 1)]
            y += 1.6
        for side in (-1, 1):    # vents on its flanks
            parts.append(((centre + side * rt - 0.1, 6.7, z - rt / 3 + 0.35), (centre + side * rt + 0.1, 22.55, z + rt / 3 - 0.35), "grille"))
        # Its pipe to the pump comes up with it
        xa, xb = sorted((centre - sign * (rt - 3.5), cx + sign * (pump_r - 0.2)))
        parts += roller(xa, xb, pump_y, pump_z + 1.25, 1.56, dark)
        for a, b in ((xa + 0.1, xa + 1), (xb - 1, xb - 0.1), ((xa + xb) / 2 - 0.45, (xa + xb) / 2 + 0.45)):
            parts += roller(a, b, pump_y, pump_z + 1.25, 1.91, "copper")
        return parts

    return base, tank(24, 1), tank(-8, -1), tower, top, chimney, lamp


def coolant_mixer_parts():
    """A 2x1 bench: (the body, the opened parts), authored in x 0..32 and shifted so the machine block is x 0..16 and its
    Cyber Frame x -16..0. CoolantMixerRenderer draws the same boxes moving, and the hood and carriage always."""
    dark = {"up": "lid", "south": "body", "east": "body"}
    cap = {"up": "lid", "south": "trim", "east": "trim"}

    def shifted(parts):
        return [((a[0] - 16, a[1], a[2]), (b[0] - 16, b[1], b[2]), *rest) for a, b, *rest in parts]

    def tank(x0):
        x1 = x0 + 9
        return [*octagon(x0, 7.5, 2.5, x1, 15.5, 11.5, 2.25, dark),
                ((x0 + 1, 15.5, 3.5), (x1 - 1, 16, 10.5), cap),
                ((x0 + 0.6, 7.55, 3.1), (x1 - 0.6, 8.3, 11.75), "trim"),   # base ring, steel, like the cap
                ((x0 + 1.5, 8.3, 11.5), (x0 + 2, 15.5, 12.25), "trim"),    # two rails, not a bezel
                ((x1 - 2, 8.3, 11.5), (x1 - 1.5, 15.5, 12.25), "trim"),
                ((x0 + 2, 9.4, 11.5), (x1 - 2, 15, 12.1), "slot"),         # sight glass, 5 x 5.6 px
                ((x0 + 1, 3, 3.5), (x0 + 8, 7.6, 10.5), "body")]           # saddle, its foot under the deck

    body = [((0, 0, 0), (32, 1, 16), {"up": "lid", "down": "lid", "south": "plinth", "east": "plinth"}),
            *octagon(1, 1, 1, 31, 5, 15, 2, dark),
            *octagon(0.6, 2, 0.6, 31.4, 2.75, 15.4, 2, "copper"),
            ((11.5, 5, 5), (20.5, 5.2, 13.5), "slot"),                     # the bath, dry: the hood's front closes on it
            ((11, 5, 13.5), (21, 5.7, 14.5), "trim"),                      # its lip
            ((11.4, 5, 2.9), (20.6, 5.2, 5), "lid"),                       # sill under the rail wall
            ((14.5, 3, 15), (17.5, 4.2, 15.2), "light")]
    opened = [*tank(21), *tank(2),
              ((11.5, 5.1, 3), (20.5, 9.9, 4.9), {"up": "lid", "south": "beam", "east": "beam", "north": "body"}),   # rail wall
              ((12, 7.6, 4.9), (20, 8.4, 4.95), "slot")]
    return shifted(body), shifted(opened)


def coolant_mixer_loose():
    """The starting block and the item: half a bench, its water tank standing where it will stand, a flange open
    towards the frame (x 0 here, the right side as the player sees the front). Its lamp stays off: it doesn't work yet."""
    dark = {"up": "lid", "south": "body", "east": "body"}
    cap = {"up": "lid", "south": "trim", "east": "trim"}
    return [((0, 0, 0), (16, 1, 16), {"up": "lid", "down": "lid", "south": "plinth", "east": "plinth"}),
            ((1, 1, 1), (13, 5, 15), dark),                            # body, chamfered at the left end only
            ((1, 1 + LAYER, 3), (15, 5 - LAYER, 13), dark),
            ((1.5, 2, 0.6), (13.4, 2.75, 15.4), "copper"),
            ((1.5, 2 + LAYER, 2.6), (15.4, 2.75 - LAYER, 13.4), "copper"),
            ((2.5, 3, 15), (5.5, 4.2, 15.2), "light_off"),
            ((0.5, 1.1, 1.1), (1.5, 4.9, 14.9), "trim"),               # the flange: a recessed plate, a mint ring, a dark socket
            ((0, 1.9, 4), (0.5, 4.6, 5), "copper"),
            ((0, 1.9, 11), (0.5, 4.6, 12), "copper"),
            ((0, 1.9, 5), (0.5, 2.6, 11), "copper"),
            ((0, 3.9, 5), (0.5, 4.6, 11), "copper"),
            ((0.2, 2.6, 5), (0.5, 3.9, 11), "slot"),
            *octagon(5, 5, 2.5, 14, 15.5, 11.5, 2.25, dark),           # the water tank, with a dry window
            ((6, 15.5, 3.5), (13, 16, 10.5), cap),
            ((5.6, 5, 3.1), (13.4, 5.8, 11.75), "trim"),
            ((6.5, 5.8, 11.5), (7, 15.5, 12.25), "trim"),
            ((12, 5.8, 11.5), (12.5, 15.5, 12.25), "trim"),
            ((7, 8.5, 11.5), (12, 14.5, 12.1), "slot"),
            ((1.5, 5, 4), (4.5, 6.6, 12), "body"),                     # a shelf between flange and tank
            ((1.2, 6.6, 3.6), (4.8, 7.1, 12.4), "trim")]


def coolant_mixer_hood():
    """The panels that close the bench before it opens, sunk inside it afterwards: front, back, end, each a right and a left."""
    dark = {"up": "lid", "south": "body", "east": "body"}
    panels = [((3.1, 0.16, 12.3), (11.4, 4.95, 13.4)), ((20.6, 0.16, 12.3), (28.9, 4.95, 13.4)),
              ((3.1, 0.16, 1.1), (11.4, 4.95, 2.3)), ((20.6, 0.16, 1.1), (28.9, 4.95, 2.3)),
              ((1.1, 0.16, 3.1), (1.9, 4.95, 12.9)), ((30.1, 0.16, 3.1), (30.9, 4.95, 12.9))]
    return [[((a[0] - 16, a[1], a[2]), (b[0] - 16, b[1], b[2]), dark)] for a, b in panels]


def machine_parts():
    """The models of the parts that move while a small multiblock opens, in MachinePartBlock's order: the same boxes as the
    static model, so the same texture and light. Each is a state of the block machine_part, for each facing."""
    turbine = gas_turbine_parts()[1]
    mixer = coolant_mixer_parts()[1]
    groups = [("turbine_duct", turbine[:7]), ("turbine_nozzle", turbine[7:11]),
              ("turbine_door_left", turbine[11:12]), ("turbine_door_right", turbine[12:13]),
              ("mixer_water_tank", mixer[:7]), ("mixer_coolant_tank", mixer[8:15]),
              ("mixer_water_saddle", mixer[7:8]), ("mixer_coolant_saddle", mixer[15:16]), ("mixer_rail", mixer[16:18]),
              *[(f"mixer_hood_{i}", panel) for i, panel in enumerate(coolant_mixer_hood())],
              ("steam_piston", steam_engine_piston()), ("steam_wheel", steam_engine_wheel()),
              *steam_engine_cylinder_parts().items(), *steam_engine_dynamo_parts().items(),
              *zip(("acid_water", "acid_acid", "acid_tower", "acid_top", "acid_chimney"), acid_synthesizer_parts()[1:6]),
              *zip(("bio_top", "bio_ring_low", "bio_ring_high", "bio_tank"), bio_digester_parts()[1:5]),
              *zip(("fab_base", "fab_bed", "fab_idler_near", "fab_idler_far", "fab_magazine"), fabricator_parts()[:5]),
              *zip(("oven_base", "oven_tube", "oven_tank"), pyrolysis_oven_moving())]
    variants = {}
    for part, (name, parts) in enumerate(groups):
        build(f"part_{name}", parts, False)
        for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            variants[f"facing={facing},part={part}"] = {"model": f"cyberfarm:block/part_{name}", **({"y": y} if y else {})}
    (ASSETS / "blockstates/machine_part.json").write_text(json.dumps({"variants": variants}, indent=2) + "\n")


def coolant_mixer_port():
    """low_port() on a post that starts above the foot band."""
    return [((6, 2.8, 15), (10, 5, 16), "body"), *low_port()[1:]]


def small_multiblock_blockstate(name, frame_sides, up=False):
    """Its own small model until assembled; the body while it opens or folds, drawn over by the renderer; then the open
    machine. A port wherever a pipe plugs in, but never on a side with a frame (frame_sides: facing -> those sides)."""
    parts = []
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        turn = {"y": y} if y else {}
        parts.append({"when": {"facing": facing, "stage": "0"}, "apply": {"model": f"cyberfarm:block/{name}", **turn}})
        for lit, suffix in (("false", ""), ("true", "_on")):
            parts.append({"when": {"facing": facing, "stage": "1|3", "lit": lit},
                          "apply": {"model": f"cyberfarm:block/{name}_body{suffix}", **turn}})
            parts.append({"when": {"facing": facing, "stage": "2", "lit": lit},
                          "apply": {"model": f"cyberfarm:block/{name}_open{suffix}", **turn}})
    for facing, framed in frame_sides.items():
        for side, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            if side not in framed:
                parts.append({"when": {"facing": facing, side: "true"},
                              "apply": {"model": f"cyberfarm:block/{name}_port", **({"y": y} if y else {})}})
    if up:
        parts.append({"when": {"up": "true"}, "apply": {"model": f"cyberfarm:block/{name}_port_up"}})
    (ASSETS / f"blockstates/{name}.json").write_text(json.dumps({"multipart": parts}, indent=2) + "\n")


def hidden_frame():
    """Usually never drawn (HiddenFrameBlock), but its breaking particles need a model. A cell a machine's own model can't reach
    shows its piece, turned as the machine is."""
    (ASSETS / "models/block/hidden_frame.json").write_text(
        json.dumps({"textures": {"particle": "cyberfarm:block/cyber_frame"}, "elements": []}, indent=2) + "\n")
    variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for piece, model in enumerate(("hidden_frame", "steam_engine_cylinder", "steam_engine_dynamo")):
            variants[f"facing={facing},piece={piece}"] = {"model": f"cyberfarm:block/{model}", **({"y": y} if y and piece else {})}
    for facing in ("up", "down"):       # a frame stacked on another: never a piece
        for piece in range(3):
            variants[f"facing={facing},piece={piece}"] = {"model": "cyberfarm:block/hidden_frame"}
    (ASSETS / "blockstates/hidden_frame.json").write_text(json.dumps({"variants": variants}, indent=2) + "\n")


def gas_turbine_parts():
    """A 1x3 engine, the machine block in the middle cell: (the body, the opened parts). Alone it is the body, a drum with a
    round mouth at each end and sockets where pipes go. GasTurbineRenderer draws the opened parts moving, and the rotor always."""
    dark = {"up": "lid", "south": "body", "east": "body"}
    mint_front = {"up": "trim", "south": "copper", "east": "trim"}
    mint_rear = {"up": "trim", "south": "trim", "north": "copper", "east": "trim"}
    wall = {"up": "lid", "south": "copper", "east": "body"}
    nozzle = {"up": "lid", "south": "body", "north": "copper", "east": "body"}

    def mouth(z0, z1, r, mat):
        """An octagon along z, two crossed boxes."""
        c = 0.29 * r
        return [((8 - r + c, 9.5 - r, z0), (8 + r - c, 9.5 + r, z1), mat),
                ((8 - r, 9.5 - r + c, z0 + LAYER), (8 + r, 9.5 + r - c, z1 - LAYER), mat)]

    body = [((0, 0, 0), (16, 1, 16), {"up": "lid", "down": "lid", "south": "plinth", "east": "plinth"}),
            ((1, 1, 1), (15, 7, 15), dark),
            ((0.6, 1.2, 0.6), (15.4, 1.7, 15.4), "copper"),
            *zdisc(8, 9.5, 1.2, 14.8, 5.6, dark),
            ((4.2, 12, 4.5), (11.8, 15.2, 11.5), dark),                # the saddle the top port stands on
            *mouth(14.7, 15.8, 5.9, mint_front), *mouth(15.7, 16, 3.4, "slot"),
            *mouth(0.2, 1.3, 5.9, mint_rear), *mouth(0, 0.3, 3.4, "slot"),
            ((15, 2.2, 3), (15.12, 5.2, 13), "slot"),                  # the sight glasses, on the flanks
            ((0.88, 2.2, 3), (1, 5.2, 13), "slot"),
            ((1.9, 3, 15), (4.9, 4.2, 15.2), "light"),
            ((13, 5.6, 5.6), (15, 10.4, 10.4), "body"),                # sockets, where pipes really go: both flanks and the top
            ((15, 5.6, 5.6), (15.2, 10.4, 10.4), "copper"),
            ((15, 6.5, 6.5), (15.22, 9.5, 9.5), "slot"),
            ((1, 5.6, 5.6), (3, 10.4, 10.4), "body"),
            ((0.8, 5.6, 5.6), (1, 10.4, 10.4), "copper"),
            ((0.78, 6.5, 6.5), (1, 9.5, 9.5), "slot"),
            ((5.6, 15.2, 5.6), (10.4, 15.25, 10.4), "copper"),
            ((6.5, 15.2, 6.5), (9.5, 15.27, 9.5), "slot")]
    # Solid sleeves slide out of the drum with the duct and the nozzle, so the three cells read as one engine. A hair apart in size, as they overlap when closed
    opened = [*mouth(15.8, 24, 6, dark),                               # the front sleeve, then the duct, in the front frame's cell
              ((2, 3.5, 24), (14, 4.5, 30), wall), ((2, 14.5, 24), (14, 15.5, 30), wall),
              ((2, 4.5, 24), (3, 14.5, 30), wall), ((13, 4.5, 24), (14, 14.5, 30), wall),
              ((3, 4.5, 24), (13, 14.5, 24.2), "slot"),
              *mouth(-9, 0.2, 5.95, nozzle), *mouth(-9.2, -8.9, 3.4, "slot"),   # the back sleeve and nozzle, in the back frame's cell
              ((3.05, 4.55, 24.27), (3.35, 14.45, 29.2), "trim"),      # the doors, swung flat against the duct's walls
              ((12.65, 4.55, 24.27), (12.95, 14.45, 29.2), "trim")]
    return body, opened


def gas_turbine_loose():
    """The starting block and the item: the body with its lamp off, as it doesn't work yet."""
    return [(a, b, "light_off" if spec == "light" else spec) for a, b, spec in gas_turbine_parts()[0]]


def waste_reprocessor_parts():
    """A shielded back plate with a disc in front, and on the roof a plaque with the radiation mark, standing above the
    block because the disc leaves no room under it. WasteReprocessorRenderer draws the disc's pockets."""
    dark = {"up": "lid", "south": "body", "east": "body"}
    return [((0, 0, 0), (16, 1, 16), {"up": "lid", "down": "lid", "south": "plinth", "east": "plinth"}),
            *octagon(1, 1, 1, 15, 3, 15, 2, dark),
            *octagon(1.5, 3, 2, 14.5, 14.8, 9.6, 1.2, dark),
            *zdisc(8, 9, 9.6, 12.2, 5.7, "beam"),
            *zdisc(8, 9, 12.2, 12.4, 5.5, "trim"),
            *zdisc(8, 9, 12.4, 12.5, 1.6, "copper"),                   # the disc's hub
            *zdisc(8, 9, 11, 11.4, 5.8, "copper"),
            ((11.4, 2.4, 15), (14.2, 4, 15.08), "plinth"),
            ((11.6, 2.6, 15.08), (14, 3.8, 15.2), "light"),
            ((3.8, 14.8, 6.6), (12.2, 15.7, 9.6), "lid"),              # the plaque's seat and its stepped brace
            ((6.6, 15.7, 6.9), (9.4, 19.2, 8.2), "lid"),
            ((6.6, 15.7, 5.4), (9.4, 17.4, 6.9), "lid"),
            ((5, 15.7, 8.2), (11, 21.7, 9.4), "radiation_mark")]


def waste_reprocessor_item():
    """The item: the block's model with the disc's pockets at rest, which in the world the renderer draws."""
    def pocket(x, y, content):
        boxes = [((x - 1.6, y - 1.6, 12.46), (x + 1.6, y + 1.6, 12.61), "slot")]
        if content:
            boxes.append(((x - 1.3, y - 1.3, 12.61), (x + 1.3, y + 1.3, 12.91), content))
        return boxes

    body = [(a, b, "light_off" if spec == "light" else spec) for a, b, spec in waste_reprocessor_parts()]
    return [*body, *pocket(8, 12.6, "light_off"), *pocket(4.4, 9, "body"), *pocket(8, 5.4, None), *pocket(11.6, 9, "copper")]


def radiation_mark():
    """textures/block/radiation_mark.png: a steel frame, a dark plate, an emerald trefoil from its true geometry (hub radius
    1, blades 1.5 to 5, 60 degrees wide). A power of two, not to lower the block atlas's mipmaps; a whole-texel one can't draw it."""
    size, frame = 32, 3
    scale = (size / 2 - frame - 1) / 5
    samples = 6
    img = []
    for j in range(size):
        row = []
        for i in range(size):
            if min(i, j, size - 1 - i, size - 1 - j) < frame:
                row.append(rgba("a"))
                continue
            hits = 0
            for a in range(samples):
                for b in range(samples):
                    x, y = (i + (a + 0.5) / samples - size / 2) / scale, (size / 2 - j - (b + 0.5) / samples) / scale
                    r, angle = math.hypot(x, y), math.degrees(math.atan2(y, x)) % 360
                    blade = 1.5 <= r <= 5 and any(abs((angle - c + 180) % 360 - 180) <= 30 for c in (90, 210, 330))
                    hits += r <= 1 or blade
            row.append(rgba("H") if hits * 2 >= samples * samples else rgba("5"))
        img.append(row)
    save_png(ASSETS / "textures/block/radiation_mark.png", img)


def crystallizer_controller():
    """The Crystallizer's controller alone, and its item: a stepped pedestal with a seed of the crystal in a basin, the heart
    of the machine in miniature. Once the chamber is complete it hides, and CrystallizerRenderer draws the whole machine."""
    dark = {"up": "lid", "south": "body", "east": "body"}
    cap = {"up": "lid", "south": "trim", "east": "trim"}
    return [((0, 0, 0), (16, 1, 16), {"up": "lid", "down": "lid", "south": "plinth", "east": "plinth"}),
            *octagon(1, 1, 1, 15, 7, 15, 3, dark),
            *octagon(0.6, 2.4, 0.6, 15.4, 3.1, 15.4, 3, "copper"),
            *octagon(3, 7, 3, 13, 10, 13, 2.5, "beam"),
            ((3.5, 10, 3.5), (12.5, 11.2, 5), cap),                    # the basin's rim
            ((3.5, 10, 11), (12.5, 11.2, 12.5), cap),
            ((3.5, 10, 5), (5, 11.2, 11), cap),
            ((11, 10, 5), (12.5, 11.2, 11), cap),
            ((5, 10, 5), (11, 10.3, 11), "slot"),
            *octagon(6.5, 10.3, 6.5, 9.5, 12.1, 9.5, 0.9, "crystal"),  # the seed
            ((7.35, 12.1, 7.35), (8.65, 13.3, 8.65), "crystal_tip"),
            ((6, 1, 15), (10, 5, 15.2), "radiation_mark")]


def crystallizer_blockstate():
    """Its own model until the chamber is complete, then nothing: the renderer draws it all."""
    parts = []
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        parts.append({"when": {"facing": facing, "formed": "false"},
                      "apply": {"model": "cyberfarm:block/crystallizer", **({"y": y} if y else {})}})
        parts.append({"when": {facing: "true", "formed": "false"},
                      "apply": {"model": "cyberfarm:block/crystallizer_port_socket", **({"y": y} if y else {})}})
    parts.append({"when": {"formed": "true"}, "apply": {"model": "cyberfarm:block/crystallizer_hidden"}})
    (ASSETS / "blockstates/crystallizer.json").write_text(json.dumps({"multipart": parts}, indent=2) + "\n")


def supply_terminal_parts():
    """A notice board on a post: dim cards in its four places, the last set apart for events, under a lip the renderer's
    lit cards drop behind when their contract is done. SupplyTerminalRenderer draws what's lit."""
    dark = {"up": "lid", "south": "body", "east": "body"}
    board = {"up": "beam", "south": "body", "east": "body"}
    return [((0, 0, 0), (16, 1, 16), {"up": "lid", "down": "lid", "south": "plinth", "east": "plinth"}),
            *octagon(4.6, 1, 10.6, 11.4, 2.2, 14.8, 0.8, dark),        # footing
            *octagon(6.3, 2.2, 11.8, 9.7, 5.9, 14.2, 0.5, dark),       # post
            ((5.6, 5.3, 11.4), (10.4, 5.9, 14.6), "trim"),             # collar under the board
            *octagon(0.4, 5.9, 12, 15.6, 15.4, 15, 1, board),          # the board
            ((0.4, 5.9, 15), (0.9, 14.4, 15.6), "plinth"),             # its side posts, header and steel top edge
            ((15.1, 5.9, 15), (15.6, 14.4, 15.6), "plinth"),
            ((0.4, 14.4, 15), (15.6, 15.15, 15.6), "body"),
            ((0.4, 15.15, 15), (15.6, 15.4, 15.6), "trim"),
            ((0.9, 5.9, 15.42), (15.1, 10, 15.6), "body"),             # the lip the cards drop behind
            ((0.9, 9.75, 15.6), (15.1, 10, 15.66), "trim"),
            ((11.1, 10.1, 15), (11.5, 14.3, 15.2), "beam"),            # divider before the event's place
            ((1, 10, 15), (14.8, 14.4, 15.1), "slot"),                 # the window behind the cards
            ((1.4, 14.65, 15.6), (14.6, 15, 15.64), "light_off"),      # the header's light, off
            ((7.2, 2.4, 14.2), (8.8, 3, 14.3), "slot"),
            ((1.2, 10, 15.1), (3.7, 13.8, 15.16), "light_off"),        # the dim cards
            ((4.6, 10, 15.1), (7.1, 13.8, 15.16), "light_off"),
            ((8, 10, 15.1), (10.5, 13.8, 15.16), "light_off"),
            ((12.1, 13, 15.1), (14.6, 13.8, 15.16), "light_off")]


def steam_engine_plinth(z0, z1):
    """A base standing on four feet round the down socket, with its light lines."""
    return [((0.5, 0, z0), (15.5, 0.75, 5), "plinth"), ((0.5, 0, 11), (15.5, 0.75, z1), "plinth"),
            ((0.5, 0, 5), (5, 0.75, 11), "plinth"), ((11, 0, 5), (15.5, 0.75, 11), "plinth"),
            ((0.5, 0.75, z0), (15.5, 2.5, z1), "plinth"),
            ((0.4, 1.2, z0), (0.5, 2, z1), "light"), ((15.5, 1.2, z0), (15.6, 2, z1), "light")]


def steam_engine_boiler():
    """The Steam Engine's boiler: its coils, tagged "unlit", are drawn by SteamEngineRenderer once it is open."""
    dark = {"up": "lid", "south": "body", "east": "body"}
    rings = [(a, b, "copper", "unlit") for y0, y1 in ((5, 6), (8.6, 9.6)) for a, b, _ in octagon(1.2, y0, 1.2, 14.8, y1, 14.8, 3.6, "copper")]
    return [*steam_engine_plinth(0, 15.5), ((0.4, 1.2, 15.5), (15.6, 2, 15.6), "light"), ((0.5, 1.2, -0.1), (15.5, 2, 0), "light"),   # its back line goes under the row once assembled
            *octagon(1.5, 2.5, 1.5, 14.5, 13, 14.5, 3.5, dark), *rings,
            *octagon(3, 13, 3, 13, 14.7, 13, 2, "lid"), ((7, 11, 14.5), (9, 12, 14.6), "light"),
            ((5.8, 13.6, 1.2), (10.2, 15.85, 4.8), "trim"), ((5, 13.2, 0), (11, 16, 1.2), "trim")]    # the steam line's start


def steam_engine_piston():
    """Head and rod at rest, always the renderer's."""
    y = 7.4
    return [*octagon(5.2, y, 5.2, 10.8, y + 2.4, 10.8, 1, "trim"), *octagon(5.4, y + 2.4, 5.4, 10.6, y + 3, 10.6, 1, "light"),
            ((6.75, y - 5.6, 6.75), (9.25, y, 9.25), "trim")]    # always into the base, even at the top of its stroke


def steam_engine_cylinder_parts():
    """A cylinder as it composes on its rail plate: steel posts round a piston (steam_engine_piston, the renderer's), and a
    length of the steam line with a flange at each end, so the row joins up."""
    dark = {"up": "lid", "south": "body", "east": "body"}
    return {"steam_rail": [((0.5, 0, 0), (15.5, 2.5, 16), "plinth"),
                           ((0.4, 1.2, 0), (0.5, 2, 16), "light"), ((15.5, 1.2, 0), (15.6, 2, 16), "light")],
            "steam_base": octagon(2.5, 2.5, 2.5, 13.5, 4.5, 13.5, 0.8, dark),
            "steam_posts": [((x, 4.5, z), (x + 1.4, 13, z + 1.4), "trim") for x in (3.3, 11.3) for z in (3.3, 11.3)],
            "steam_lid": octagon(3, 13, 3, 13, 13.8, 13, 1, "lid"),
            "steam_line": [((5.8, 13.8, 1.2), (10.2, 15.85, 14.8), "trim"),
                           ((5, 13.4, 0), (11, 16, 1.2), "trim"), ((5, 13.4, 14.8), (11, 16, 16), "trim")]}


def steam_engine_wheel():
    """The flywheel's spokes, hub and lug, on the axle at x 8, y 9."""
    return [((3.8, 7.5, 2.9), (12.2, 10.5, 4.1), "trim"), ((6.5, 4.8, 2.9 + LAYER), (9.5, 13.2, 4.1 - LAYER), "trim"),
            ((6.3, 7.3, 2.7), (9.7, 10.7, 4.3), "lid"), ((9.9, 7.75, 2.7), (12.4, 10.25, 4.3), "light")]


def steam_engine_dynamo_parts():
    """A drum with two windings, the steam line's end on top towards the cylinders, and at the row's far end the rim of the
    flywheel (steam_engine_wheel, the renderer's) on a short axle: nothing that looks like a socket."""
    dark = {"up": "lid", "south": "body", "east": "body"}

    def drum(z0, z1, x0, y0, x1, y1, cut, mat):
        return [((x0 + cut, y0, z0), (x1 - cut, y1, z1), mat), ((x0, y0 + cut, z0 + LAYER), (x1, y1 - cut, z1 - LAYER), mat)]

    return {"steam_dynamo_rail": [*steam_engine_plinth(0.5, 16), ((0.5, 1.2, 0.4), (15.5, 2, 0.5), "light")],
            "steam_drum": [((3.5, 2.5, 5.5), (12.5, 4.25, 14.5), dark), *drum(5, 15.2, 1.5, 4.25, 14.5, 13.75, 3, dark),
                           *drum(12, 13, 1.2, 4.15, 14.8, 13.85, 3.2, "copper"), *drum(8, 9, 1.2, 4.15, 14.8, 13.85, 3.2, "copper")],
            "steam_rim": [((3.3, 13.7, 2.5), (12.7, 15.2, 4.5), "trim"), ((3.3, 2.8, 2.5), (12.7, 4.3, 4.5), "trim"),
                          ((1.8, 4.3, 2.5), (3.3, 13.7, 4.5), "trim"), ((12.7, 4.3, 2.5), (14.2, 13.7, 4.5), "trim")],
            "steam_shaft": [((6.8, 7.8, 4.3), (9.2, 10.2, 5.2), "trim"), ((5, 13.4, 14.4), (11, 16, 16), "trim")]}


def steam_engine_blockstate():
    """The boiler; its coils are the renderer's once open. A socket wherever a pipe plugs in, never on the side towards the row."""
    sides = (("north", 0), ("east", 90), ("south", 180), ("west", 270))
    opposite = {"north": "south", "east": "west", "south": "north", "west": "east"}
    parts = []
    for facing, y in sides:
        turn = {"y": y} if y else {}
        parts.append({"when": {"facing": facing, "stage": "0|1|3"}, "apply": {"model": "cyberfarm:block/steam_engine", **turn}})
        parts.append({"when": {"facing": facing, "stage": "2"}, "apply": {"model": "cyberfarm:block/steam_engine_on", **turn}})
        for side, turned in sides:
            if side != opposite[facing]:
                parts.append({"when": {"facing": facing, side: "true"},
                              "apply": {"model": "cyberfarm:block/steam_engine_port", **({"y": turned} if turned else {})}})
    parts.append({"when": {"up": "true"}, "apply": {"model": "cyberfarm:block/steam_engine_port_up"}})
    parts.append({"when": {"down": "true"}, "apply": {"model": "cyberfarm:block/steam_engine_port_down"}})
    (ASSETS / "blockstates/steam_engine.json").write_text(json.dumps({"multipart": parts}, indent=2) + "\n")


def methane_extractor():
    """Alone, a spray head: a ring docked under the body on four tubes, its nozzles for feet. No sockets: fluids reach the
    chamber only through its Acid Inlets and Methane Outlets."""
    dark = {"up": "lid", "south": "body", "east": "body"}
    cap = {"up": "lid", "south": "trim", "east": "trim"}
    mint = {"up": "copper", "south": "copper", "east": "copper"}
    return [((0.5, 0, 0.5), (3.1, 1, 3.1), "trim"), ((12.9, 0, 0.5), (15.5, 1, 3.1), "trim"),
            ((0.5, 0, 12.9), (3.1, 1, 15.5), "trim"), ((12.9, 0, 12.9), (15.5, 1, 15.5), "trim"),
            ((0.5, 1, 0.5), (15.5, 3.5, 2.5), "copper"), ((0.5, 1, 13.5), (15.5, 3.5, 15.5), "copper"),
            ((0.5, 1, 2.5), (2.5, 3.5, 13.5), "copper"), ((13.5, 1, 2.5), (15.5, 3.5, 13.5), "copper"),
            ((0.5, 3.5, 0.5), (4.3, 7, 4.3), cap), ((11.7, 3.5, 0.5), (15.5, 7, 4.3), cap),
            ((0.5, 3.5, 11.7), (4.3, 7, 15.5), cap), ((11.7, 3.5, 11.7), (15.5, 7, 15.5), cap),
            *octagon(1, 7, 1, 15, 11, 15, 4, dark), *octagon(0.6, 8.5, 0.6, 15.4, 9.6, 15.4, 4, mint),
            *round_tank(4, 11, 4, 12, 14.5, 12, cap), *round_tank(5.5, 14.5, 5.5, 10.5, 16, 10.5, dark),
            ((7.2, 8.8, 14.9), (8.8, 10.2, 15.15), "light")]


# A tank has a frame only around its outside, so its blocks can't be told apart: uprights where two shown sides meet, and on top and below
# a 3x3 grid of frame or plate, by which neighbours are part of the tank. The sides are open: FluidTankRenderer draws the glass and the fluid.
TANK_COLUMN = (0, 3, 13), (3, 13, 16)
TANK_COLUMN_END = (0, 13, 13), (3, 16, 16)
TANK_CENTRE = [((3, 13, 3), (13, 15, 13), "tank_plate")]
TANK_STRIP = (3, 13, 13), (13, 16, 16)
TANK_STRIP_PLATE = [((3, 13, 13), (13, 15, 16), "tank_plate")]
TANK_CORNER = (0, 13, 13), (3, 16, 16)
TANK_CORNER_B = (13, 13, 13), (16, 16, 16)
TANK_CORNER_PLATE = [((0, 13, 13), (3, 15, 16), "tank_plate")]


def tank_item(name):
    """A lone block, for the item."""
    def turned(box, times):
        (x0, y0, z0), (x1, y1, z1), *rest = box
        for _ in range(times):
            x0, z0, x1, z1 = z0, 16 - x1, z1, 16 - x0
        return ((min(x0, x1), y0, min(z0, z1)), (max(x0, x1), y1, max(z0, z1)), *rest)

    def upside_down(box):
        (x0, y0, z0), (x1, y1, z1), *rest = box
        return ((x0, 16 - y1, z0), (x1, 16 - y0, z1), *rest)

    lid = [*TANK_CENTRE]
    for times in range(4):
        lid += [turned((*TANK_STRIP, f"{name}_strip"), times), turned((*TANK_CORNER, f"{name}_corner"), times)]
    uprights = [turned((*TANK_COLUMN, f"{name}_column"), times) for times in range(4)]
    return [*lid, *[upside_down(box) for box in lid], *uprights]


def tank_blockstate(name):
    """Which of the shared pieces each block shows. Turned over with x: 180, a piece on the north edge lands on the south one, one in the north-west corner in the south-west."""
    shown = "face|socket"
    sides = (("north", 0), ("east", 90), ("south", 180), ("west", 270))
    parts = []

    def part(when, model, x=0, y=0):
        apply = {"model": f"cyberfarm:block/{model}"}
        if x:
            apply["x"] = x
        if y:
            apply["y"] = y
        parts.append({"when": when, "apply": apply})

    for i, (side, r) in enumerate(sides):
        before = sides[i - 1][0]
        part({side: shown, before: shown}, f"{name}_column", y=r)
        part({side: shown, before: shown, "up": "joined"}, f"{name}_column_end", y=r)
        part({side: shown, before: shown, "down": "joined"}, f"{name}_column_end", x=180, y=(r + 90) % 360)
        part({side: "socket"}, f"{name}_socket", y=r)
        for cap, x, edge, corner in (("up", 0, r, r), ("down", 180, (r + 180) % 360, (r + 90) % 360)):
            part({cap: shown, side: shown}, f"{name}_strip", x=x, y=edge)
            part({cap: shown, side: "joined"}, "tank_strip_plate", x=x, y=edge)
            part({cap: shown, side: shown, before: shown}, f"{name}_corner", x=x, y=corner)
            # One of the two sides goes on into the next block: the frame runs straight through this corner, along the other
            if cap == "up":
                part({cap: shown, side: shown, before: "joined"}, f"{name}_through", y=r)
                part({cap: shown, side: "joined", before: shown}, f"{name}_through_b", y=(r - 90) % 360)
            else:
                part({cap: shown, side: shown, before: "joined"}, f"{name}_through_b", x=x, y=(r + 180) % 360)
                part({cap: shown, side: "joined", before: shown}, f"{name}_through", x=x, y=(r + 90) % 360)
            part({cap: shown, side: "joined", before: "joined"}, "tank_corner_plate", x=x, y=corner)
    part({"up": shown}, "tank_centre")
    part({"down": shown}, "tank_centre", x=180)
    part({"up": "socket"}, f"{name}_socket_up")
    part({"down": "socket"}, f"{name}_socket_up", x=180)
    (ASSETS / f"blockstates/{name}.json").write_text(json.dumps({"multipart": parts}, indent=2) + "\n")


def tank_glass():
    """Mostly clear, with a faint streak, in the blocks atlas so the renderer can draw it with the fluid behind it."""
    pixels = [[(214, 240, 238, 26) for _ in range(16)] for _ in range(16)]
    for x in range(4, 9):
        pixels[x][x] = (214, 240, 238, 110)
    for x, y in ((3, 12), (9, 5)):
        pixels[15 - y][x] = (214, 240, 238, 70)
    save_png(ASSETS / "textures/block/tank_glass.png", pixels)


def tanks():
    for name, parts in (("tank_centre", TANK_CENTRE), ("tank_strip_plate", TANK_STRIP_PLATE), ("tank_corner_plate", TANK_CORNER_PLATE)):
        build(name, parts, False)
    tank_glass()
    for name, trim in (("liquid_tank", "trim"), ("gas_tank", "copper")):
        build(f"{name}_socket", [((4.5, 4.5, 13.5), (11.5, 11.5, 16.012), trim)], False)
        build(f"{name}_socket_up", [((4.5, 14.5, 4.5), (11.5, 16.012, 11.5), trim)], False)
        for piece, box, shape in (("column", TANK_COLUMN, "column"), ("column_end", TANK_COLUMN_END, "column"), ("strip", TANK_STRIP, "strip"),
                                  ("corner", TANK_CORNER, "corner"), ("through", TANK_CORNER, "strip"), ("through_b", TANK_CORNER_B, "strip")):
            build(f"{name}_{piece}", [(*box, f"{name}_{shape}")], False)
        build(name, tank_item(name), False)
        tank_blockstate(name)


def water_pump():
    """A pumpjack on a low body. The boxes tagged "unlit" are its moving parts at rest, the pose WaterPumpRenderer settles
    in (crank angle 0: beam level, connecting rod upright, counterweight down); that renderer draws them while it works."""
    dark = {"up": "lid", "south": "body", "east": "body"}
    cap = {"up": "lid", "south": "trim", "east": "trim"}
    rest = [((4.75, 11, 6.9), (14.25, 12.5, 9.1), "beam"),
            ((2.75, 10.15, 6.6), (5.25, 13.35, 9.4), "copper"),     # horse head
            ((2.3, 10.7, 6.65), (2.75, 12.8, 9.35), "copper"),
            ((13.25, 10.95, 6.95), (14.75, 12.55, 14.05), "beam"),  # tail, carrying the connecting rod's pin
            ((2.5, 7.56, 7.5), (3.5, 11.75, 8.5), "trim"),          # polished rod and its clamp
            ((2, 9.55, 7.25), (4, 9.95, 8.75), "trim"),
            ((13.5, 5.5, 13.4), (14.5, 11.75, 14), "trim"),         # connecting rod
            ((11, 4.75, 12.3), (14.4, 6.25, 13.3), "trim"),         # crank arm and counterweight
            ((10.75, 2, 12.3), (13.25, 4.75, 13.3), "copper")]
    # the plinth material's grey line (row 14) would show on this deck: its top and underside are plain lid
    return [((0, 0, 0), (16, 1, 16), {"up": "lid", "down": "lid", "south": "plinth", "east": "plinth"}),
            *octagon(1.5, 1, 1.5, 14.5, 5, 11.6, 2, dark),                     # body
            *octagon(1.1, 2.5, 1.1, 14.9, 3.25, 12, 2, "copper"),              # foot band, below the ports
            ((5, 1.5, 1.35), (11, 4.75, 1.5), {"up": "lid", "south": "grille", "east": "grille", "north": "grille"}),  # back vent
            ((3.6, 3.45, 11.6), (5.2, 5, 11.7), "plinth"),                     # status lamp, clear of a front pipe, on a dark
            ((3.85, 3.7, 11.7), (4.95, 4.7, 11.85), "light"),                  # plate so it outshines the body's pale top edge
            ((5, 5, 5), (11, 6.5, 11), dark),                                  # samson post, its pedestal
            ((6.75, 6.5, 5.5), (9.25, 13.5, 6.4), dark),                       # fork: the beam rocks between its cheeks
            ((6.75, 6.5, 9.6), (9.25, 13.5, 10.5), dark),
            *zdisc(8, 11.75, 5.25, 5.5, 1, "copper"),                          # pivot bosses, outside the cheeks
            *zdisc(8, 11.75, 10.5, 10.75, 1, "copper"),
            *round_tank(1.25, 5, 6.25, 4.75, 6.75, 9.75, dark),                # wellhead
            *round_tank(1.5, 6.75, 6.5, 4.5, 7.5, 9.5, cap),                   # stuffing box
            ((10.25, 1, 11), (13.75, 7.25, 12.05), dark),                      # crank pillow block
            *zdisc(12, 5.5, 12.05, 12.2, 1.25, "trim"),                        # crank hub
            *[(f, t, m, "unlit") for f, t, m in rest]]


def water_pump_intake():
    """Hangs below the block, into the water it pumps: flange, a real Liquid Pipe, a strainer between two rings.
    No face at y 0, so on solid ground it is simply hidden, with nothing to flicker."""
    return [*octagon(5, -1, 5, 11, 0.25, 11, 1, "copper"),
            ((6, -5.5, 6), (10, -0.75, 10), "liquid_pipe_up"),
            *round_tank(4.75, -5.75, 4.75, 11.25, -5, 11.25, "copper"),
            *round_tank(5, -9, 5, 11, -5.75, 11, {"up": "lid", "south": "strainer", "east": "strainer"}),
            *round_tank(4.75, -9.75, 4.75, 11.25, -9, 11.25, "copper")]


def water_pump_port_up():
    """The fork carries the collar over the beam."""
    return [((6.75, 13.5, 5.5), (9.25, 15.25, 6.4), "body"), ((6.75, 13.5, 9.6), (9.25, 15.25, 10.5), "body"),
            ((5, 15.25, 5), (11, 16, 11), "copper")]


# --------------------------------------------------------------------------- baking
FACES = ("down", "up", "north", "south", "west", "east")


def material_for(spec, face):
    """Face names here are in the design frame (front = south)."""
    if isinstance(spec, str):
        return spec
    if face == "up":
        return spec["up"]
    if face == "south":
        return spec["south"]
    return spec.get(face, spec["east"])


def flip(box):
    """Design front is south; Minecraft models face north, so mirror along z."""
    (x0, y0, z0), (x1, y1, z1) = box
    return (x0, y0, 16 - z1), (x1, y1, 16 - z0)


DESIGN_FACE = {"north": "south", "south": "north"}   # after the flip


def face_size(face, frm, to):
    w = {"up": to[0] - frm[0], "down": to[0] - frm[0], "north": to[0] - frm[0], "south": to[0] - frm[0]}.get(face, to[2] - frm[2])
    h = to[2] - frm[2] if face in ("up", "down") else to[1] - frm[1]
    return w, h


def bake_face(mat, face, frm, to):
    """Paint one face from its material, as the game's default UV would place it, plus bevel and gradient."""
    (x0, y0, z0), (x1, y1, z1) = frm, to
    w, h = face_size(face, frm, to)
    wp, hp = max(1, round(w)), max(1, round(h))
    src = MATERIALS[mat].g
    out = []
    for j in range(hp):
        row = []
        for i in range(wp):
            fi, fj = (i + 0.5) / wp, (j + 0.5) / hp
            if face == "north":
                u, v = 16 - (x1 - fi * w), 16 - (y1 - fj * h)
            elif face == "south":
                u, v = x0 + fi * w, 16 - (y1 - fj * h)
            elif face == "west":
                u, v = z0 + fi * w, 16 - (y1 - fj * h)
            elif face == "east":
                u, v = 16 - (z1 - fi * w), 16 - (y1 - fj * h)
            elif face == "up":
                u, v = x0 + fi * w, z0 + fj * h
            else:
                u, v = x0 + fi * w, 16 - (z1 - fj * h)
            key = src[math.floor(v) % 16 if face in ("up", "down") else min(15, max(0, int(v)))][math.floor(u) % 16]
            r, g, b, a = rgba(key)
            f = 1.0
            if key not in UNSHADED:
                if face == "down":
                    f = 0.8
                elif face == "up":
                    f = 1.04 - 0.08 * (fi + fj) / 2
                    f *= 1.2 if j == 0 and hp > 1 else 1.12 if i == 0 and wp > 1 else \
                        0.8 if j == hp - 1 and hp > 1 else 0.86 if i == wp - 1 and wp > 1 else 1.0
                else:
                    f = 1.08 - 0.18 * fj
                    if hp >= 2 and j == 0:
                        f *= 1.22
                    elif hp >= 2 and j == hp - 1:
                        f *= 0.72
                    elif wp >= 2 and i == 0:
                        f *= 1.1
                    elif wp >= 2 and i == wp - 1:
                        f *= 0.85
            row.append((min(255, int(r * f)), min(255, int(g * f)), min(255, int(b * f)), a))
        out.append(row)
    return out


def pack(patches):
    """Shelf-pack the face patches into the smallest square power-of-two atlas."""
    size = 16
    while True:
        spots, x, y, shelf = {}, 0, 0, 0
        for key, img in sorted(patches.items(), key=lambda kv: -len(kv[1])):
            w, h = len(img[0]), len(img)
            if x + w > size:
                x, y, shelf = 0, y + shelf, 0
            if y + h > size:
                break
            spots[key] = (x, y)
            x += w
            shelf = max(shelf, h)
        else:
            return size, spots
        size *= 2


def save_png(path, img):
    raw = bytearray()
    for row in img:
        raw.append(0)
        for px in row:
            raw.extend(px)

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", len(img[0]), len(img), 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def num(v):
    return int(v) if float(v).is_integer() else round(v, 4)


FOOT_LIGHT = {f"{name}{suffix}" for name in ("bio_digester", "fabricator", "acid_synthesizer", "pyrolysis_oven", "coolant_mixer", "gas_turbine") for suffix in ("", "_body", "_open")} | {
    "combustion_generator", "drone_dock", "swarm_dock", "drone_assembler", "water_pump", "electrolyzer", "waste_reprocessor", "waste_reprocessor_item",
    "supply_terminal", "methane_extractor", "crystallizer"}


def foot_light(name, parts):
    """The strip of light round a machine's base, as on the Steam Engine's boiler: all round its outline, however many blocks the base spans."""
    bases = [(frm, to) for frm, to, spec, *_ in parts if frm[1] == 0 and (spec == "plinth" or isinstance(spec, dict) and spec.get("south") == "plinth")]
    if not bases:
        print(f"{name}: no base for the foot light")
        return parts
    y0, y1, out = 0.15, 0.85, 0.1

    def inside(a, b):
        """A model can't reach past -16..32: a base that far gives up a sliver of itself to its strip."""
        return ((max(a[0], -16 + out), a[1], max(a[2], -16 + out)), (min(b[0], 32 - out), b[1], min(b[2], 32 - out)))

    parts = [(*inside(frm, to), *rest) if (frm, to) in bases else (frm, to, *rest) for frm, to, *rest in parts]
    bases = [inside(frm, to) for frm, to in bases]
    x0, z0 = min(frm[0] for frm, _ in bases), min(frm[2] for frm, _ in bases)
    x1, z1 = max(to[0] for _, to in bases), max(to[2] for _, to in bases)
    return [*parts, ((x0, y0, z1), (x1, y1, z1 + out), "light"), ((x0, y0, z0 - out), (x1, y1, z0), "light"),
            ((x0 - out, y0, z0), (x0, y1, z1), "light"), ((x1, y0, z0), (x1 + out, y1, z1), "light")]


def build(name, parts, lit_variant, translucent=False):
    """parts: (from, to, material spec), plus an optional "lit" or "unlit" for a box in one model only."""
    if name in FOOT_LIGHT:
        parts = foot_light(name, parts)
    elements = []
    for frm, to, spec, *only in parts:
        frm, to = flip((frm, to))
        elements.append((frm, to, spec, only[0] if only else None))
    patches = {}
    for n, (frm, to, spec, _) in enumerate(elements):
        if isinstance(spec, str) and (spec in EMISSIVE or spec in STILL_LIGHTS or spec in PIPES or spec in DECALS):
            continue
        for face in FACES:
            patches[(n, face)] = bake_face(material_for(spec, DESIGN_FACE.get(face, face)), face, frm, to)
    size, spots = pack(patches)
    atlas = [[(0, 0, 0, 0)] * size for _ in range(size)]
    for key, img in patches.items():
        ox, oy = spots[key]
        for j, row in enumerate(img):
            atlas[oy + j][ox:ox + len(row)] = row
    save_png(ASSETS / f"textures/block/models/{name}.png", atlas)

    def model(lit):
        out = []
        for n, (frm, to, spec, only) in enumerate(elements):
            if only and only != ("lit" if lit else "unlit"):
                continue
            faces = {}
            still = isinstance(spec, str) and spec in STILL_LIGHTS
            emissive = isinstance(spec, str) and spec in EMISSIVE or still
            for face in FACES:
                if isinstance(spec, str) and spec in DECALS:
                    face_json = {"uv": [0, 0, 16, 16] if face == "north" else [0, 0, 1, 1], "texture": "#" + spec}
                elif isinstance(spec, str) and spec in PIPES:
                    h = to[1] - frm[1]
                    if face in ("up", "down"):
                        face_json = {"uv": [6, 6, 10, 10], "texture": "#" + spec}
                    else:   # the pipe texture runs along u, so turn it to run up the stub
                        face_json = {"uv": [0, 6, num(h), 10], "rotation": 90, "texture": "#" + spec}
                elif emissive:
                    w, h = face_size(face, frm, to)
                    face_json = {"uv": [4, 4, num(4 + min(8, w)), num(4 + min(8, h))], "texture": "#" + spec}
                else:
                    x, y = spots[(n, face)]
                    img = patches[(n, face)]
                    s = 16 / size
                    face_json = {"uv": [num(x * s), num(y * s), num((x + len(img[0])) * s), num((y + len(img)) * s)],
                                 "texture": "#atlas"}
                inside = all(0 <= c <= 16 for c in (*frm, *to))
                edge = inside and {"down": frm[1] == 0, "up": to[1] == 16, "north": frm[2] == 0, "south": to[2] == 16,
                                   "west": frm[0] == 0, "east": to[0] == 16}[face]
                if edge:
                    face_json["cullface"] = face
                faces[face] = face_json
            element = {"from": [num(c) for c in frm], "to": [num(c) for c in to]}
            if emissive and lit and not still:
                element.update({"shade": False, "light_emission": 15})
            element["faces"] = faces
            out.append(element)
        textures = {"particle": f"cyberfarm:block/models/{name}", "atlas": f"cyberfarm:block/models/{name}"}
        for mat, tex in EMISSIVE.items():
            textures[mat] = f"cyberfarm:block/{tex}" + ("" if lit else "_off")
        for mat, tex in STILL_LIGHTS.items():
            if any(spec == mat for _, _, spec, _ in elements):
                textures[mat] = f"cyberfarm:block/{tex}_off"
        for mat, tex in {**PIPES, **DECALS}.items():
            if any(spec == mat for _, _, spec, _ in elements):
                textures[mat] = f"cyberfarm:block/{tex}"
        data = {"parent": "minecraft:block/block",
                "render_type": "minecraft:translucent" if translucent else "minecraft:cutout",
                "textures": textures, "elements": out}
        path = ASSETS / f"models/block/{name}{'_on' if lit and lit_variant else ''}.json"
        path.write_text(json.dumps(data, indent=2) + "\n")

    model(lit=not lit_variant)
    if lit_variant:
        model(lit=True)
    return size


def card_texture():
    """textures/entity/fabricator_card.png, sampled by FabricatorRenderer with fixed uvs: the card top (0..8 x 0..4,
    the card material's texels x 4..11, z 2..5), its edge (row 4), the chip top (8..11 x 0..2), the chip's pins
    (8..14, row 2) and a white texel (15, 15) for the vertex-coloured rollers."""
    img = [[(0, 0, 0, 0)] * N for _ in range(N)]
    for r in range(4):
        for c in range(8):
            img[r][c] = rgba(MATERIALS["card"].g[2 + r][4 + c])
    for c in range(8):
        img[4][c] = rgba("4")
    for r in range(2):
        for c in range(3):
            img[r][8 + c] = rgba(MATERIALS["chip_top"].g[r][c])
    for c in range(6):
        img[2][8 + c] = rgba(MATERIALS["chip_pins"].g[0][c])
    img[15][15] = (255, 255, 255, 255)
    save_png(ASSETS / "textures/entity/fabricator_card.png", img)


def to_rgba(tex):
    return [[rgba(k) for k in row] for row in tex.g]


# --------------------------------------------------------------------------- pipe ports
def side_port(depth, y0=5.5):
    """Socket where a pipe arm (6..10) meets the block edge: copper collar on the edge, housing back to the body."""
    return [((5.5, y0, 16 - depth), (10.5, 10.5, 15.25), "body"), ((5, 5, 15.25), (11, 11, 16), "copper")]


def low_port():
    """Socket that keeps an open chamber in view: a copper saddle under the pipe arm, fed by a post from the base,
    nothing above the pipe."""
    return [((6, 1, 15), (10, 5, 16), "body"), ((5, 5, 15), (11, 6, 16), "copper"),
            ((5, 6, 15), (6, 10, 16), "copper"), ((10, 6, 15), (11, 10, 16), "copper")]


def top_port(bottom):
    return [((5.5, bottom, 5.5), (10.5, 15.25, 10.5), "body"), ((5, 15.25, 5), (11, 16, 11), "copper")]


def blockstate(name, lit, ports, port_of=None, top_turns=False, below=None):
    """Multipart: the machine for each facing (and lit state), plus a port wherever a pipe is plugged in
    (port_of: reuse another machine's port models; top_turns: a top port that isn't square, so it follows the facing;
    below: a model shown unless a pipe is plugged in underneath)."""
    port_of = port_of or name
    parts = []
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for on in ((False, True) if lit else (None,)):
            when = {"facing": facing}
            model = f"cyberfarm:block/{name}"
            if on is not None:
                when["lit"] = str(on).lower()
                model += "_on" if on else ""
            apply = {"model": model}
            if y:
                apply["y"] = y
            parts.append({"when": when, "apply": apply})
    for side, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        apply = {"model": f"cyberfarm:block/{port_of}_port"}
        if y:
            apply["y"] = y
        parts.append({"when": {side: "true"}, "apply": apply})
    if "up" in ports and top_turns:
        for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            apply = {"model": f"cyberfarm:block/{port_of}_port_up"}
            if y:
                apply["y"] = y
            parts.append({"when": {"up": "true", "facing": facing}, "apply": apply})
    elif "up" in ports:
        parts.append({"when": {"up": "true"}, "apply": {"model": f"cyberfarm:block/{port_of}_port_up"}})
    if below:
        parts.append({"when": {"down": "false"}, "apply": {"model": f"cyberfarm:block/{below}"}})
    (ASSETS / f"blockstates/{name}.json").write_text(json.dumps({"multipart": parts}, indent=2) + "\n")


if __name__ == "__main__":
    for mat, tex in EMISSIVE.items():
        maker = fire if mat == "fire" else light
        save_png(ASSETS / f"textures/block/{tex}.png", to_rgba(maker(True)))
        save_png(ASSETS / f"textures/block/{tex}_off.png", to_rgba(maker(False)))
    for name, parts, lit_variant, translucent in (
            ("combustion_generator", combustion_generator(), True, False),
            ("bio_digester_body", bio_digester_parts()[0], True, False),
            ("bio_digester_open", sum(bio_digester_parts(), []), True, False),
            ("drone_dock", drone_dock(), False, False),
            ("swarm_dock", drone_dock("swarm_pad"), False, False),
            ("drone_assembler", drone_assembler(), True, False),
            ("fabricator_body", fabricator(), True, False),
            ("fabricator_open", [*fabricator(), *sum(fabricator_parts(), [])], True, False),
            ("acid_synthesizer_body", acid_synthesizer_parts()[0], True, False),
            ("acid_synthesizer_open", sum(acid_synthesizer_parts(), []), True, False),
            ("water_pump", water_pump(), True, False),
            ("electrolyzer", electrolyzer(), True, False),
            ("pyrolysis_oven_body", pyrolysis_oven(), True, False),
            ("pyrolysis_oven_open", pyrolysis_oven_open(), True, False),
            ("coolant_mixer_body", coolant_mixer_parts()[0], True, False),
            ("coolant_mixer_open", sum(coolant_mixer_parts(), []), True, False),
            ("gas_turbine_body", gas_turbine_parts()[0], True, False),
            ("gas_turbine_open", sum(gas_turbine_parts(), []), True, False),
            ("waste_reprocessor", waste_reprocessor_parts(), True, False),
            ("supply_terminal", supply_terminal_parts(), False, False),
            ("methane_extractor", methane_extractor(), True, False),
            ("steam_engine", steam_engine_boiler(), True, False),
            ("steam_engine_cylinder", [box for part in steam_engine_cylinder_parts().values() for box in part], False, False),
            ("steam_engine_dynamo", [box for part in steam_engine_dynamo_parts().values() for box in part], False, False)):
        size = build(name, parts, lit_variant, translucent)
        print(f"{name}: {len(parts)} boxes, atlas {size}x{size}")
    card_texture()
    # how far in each body sits from the block edge decides how deep its sockets go
    build("combustion_generator_port", side_port(3.25), False)
    build("combustion_generator_port_up", top_port(10), False)
    build("bio_digester", bio_digester_lone(), False)
    build("bio_digester_port", side_port(2.25), False)
    build("drone_dock_port", side_port(2.5, y0=1), False)
    build("drone_assembler_port", low_port(), False)
    build("fabricator", fabricator_lone(), False)
    build("fabricator_port", side_port(2), False)
    build("fabricator_port_up", top_port(12.75), False)
    build("acid_synthesizer", acid_synthesizer_lone(), False)
    build("acid_synthesizer_port", side_port(2, y0=3.5), False)
    build("water_pump_port", low_port(), False)
    build("water_pump_port_up", water_pump_port_up(), False)
    build("water_pump_intake", water_pump_intake(), False)
    build("electrolyzer_port", low_port(), False)
    build("pyrolysis_oven", pyrolysis_oven_lone(), False)
    build("pyrolysis_oven_port", low_port(), False)
    build("coolant_mixer", coolant_mixer_loose(), False)
    build("coolant_mixer_port", coolant_mixer_port(), False)
    build("crystallizer", crystallizer_controller(), False)
    build("crystallizer_port_socket", side_port(3), False)
    build("waste_reprocessor_port", low_port(), False)
    (ASSETS / "blockstates/methane_extractor.json").write_text(json.dumps({"variants": {
        f"facing={facing},formed={formed}": {"model": f"cyberfarm:block/{'hidden_frame' if formed == 'true' else 'methane_extractor'}",
                                             **({"y": y} if y and formed == "false" else {})}
        for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for formed in ("false", "true")}}, indent=2) + "\n")
    build("steam_engine_port", side_port(4), False)
    build("steam_engine_port_up", top_port(14.7), False)
    build("steam_engine_port_down", [((5, 0, 5), (11, 0.75, 11), "copper")], False)
    build("waste_reprocessor_item", waste_reprocessor_item(), False)
    radiation_mark()
    build("gas_turbine", gas_turbine_loose(), False)
    build("gas_turbine_port", side_port(4.5), False)
    build("gas_turbine_port_up", top_port(12), False)
    blockstate("combustion_generator", True, {"up"})
    small_multiblock_blockstate("bio_digester", {"north": {"east"}, "east": {"south"}, "south": {"west"}, "west": {"north"}})
    blockstate("drone_dock", False, set())
    blockstate("swarm_dock", False, set(), port_of="drone_dock")
    blockstate("drone_assembler", True, set())
    small_multiblock_blockstate("fabricator", {"north": {"south"}, "east": {"west"}, "south": {"north"}, "west": {"east"}}, up=True)
    small_multiblock_blockstate("acid_synthesizer", {"north": {"west", "east"}, "east": {"north", "south"}, "south": {"east", "west"}, "west": {"south", "north"}})
    blockstate("water_pump", True, {"up"}, top_turns=True, below="water_pump_intake")
    blockstate("electrolyzer", True, set())
    small_multiblock_blockstate("pyrolysis_oven", {"north": {"east"}, "east": {"south"}, "south": {"west"}, "west": {"north"}})
    blockstate("waste_reprocessor", True, set())
    crystallizer_blockstate()
    small_multiblock_blockstate("coolant_mixer", {"north": {"west"}, "east": {"north"}, "south": {"east"}, "west": {"south"}})
    small_multiblock_blockstate("gas_turbine", {"north": {"north", "south"}, "east": {"east", "west"},
                                                "south": {"north", "south"}, "west": {"east", "west"}}, up=True)
    hidden_frame()
    steam_engine_blockstate()
    tanks()
    machine_parts()
