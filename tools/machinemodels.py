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

PALETTE = {  # ramps shift hue: highlights warm, shadows cool
    "1": "#5b7771", "2": "#395853", "3": "#233e3b", "4": "#132423", "5": "#071314",     # dark teal body
    "a": "#bdc6c5", "b": "#b1bebc", "c": "#7c8989", "s": "#030909",                     # steel trim (kept under the lights), recess
    "g": "#ffffff", "G": "#d1fee7", "h": "#055b57",                                      # near-white light, off
    "y": "#e8b830", "u": "#fff2a8", "v": "#ffc23d", "w": "#ff7a1f", "x": "#b8391a",      # hazard, fire
    "m": "#b6f08a", "o": "#5fcf6a", "p": "#2f8a52", "j": "#d6f3f7",                      # liquid, glint
    "A": "#a5ead4", "B": "#5ec2b8", "C": "#488d91", "D": "#496973",                      # mint enamel accent
    "H": "#00b894",                                                                      # emerald marking
    "K": "#3a1a12", "M": "#6e2c18",                                                      # cold firebox
}
UNSHADED = set("gG")


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
             "strainer": strainer(), "beam": plate("c", "1", "2", "3"), "copper": plate("A", "B", "C", "D")}
EMISSIVE = {"fire": "machine_fire", "light": "machine_light"}   # material -> texture name (+ "_off")
STILL_LIGHTS = {"light_off": "machine_light"}   # a lamp left off in both models: a block entity renderer lights it
PIPES = {"gas_pipe_up": "gas_pipe", "liquid_pipe_up": "liquid_pipe"}   # vertical stubs drawn with the real pipe texture, like the pipe blocks


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
            key = src[min(15, max(0, int(v)))][min(15, max(0, int(u)))]
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


def build(name, parts, lit_variant, translucent=False):
    """parts: (from, to, material spec), plus an optional "lit" or "unlit" for a box in one model only."""
    elements = []
    for frm, to, spec, *only in parts:
        frm, to = flip((frm, to))
        elements.append((frm, to, spec, only[0] if only else None))
    patches = {}
    for n, (frm, to, spec, _) in enumerate(elements):
        if isinstance(spec, str) and (spec in EMISSIVE or spec in STILL_LIGHTS or spec in PIPES):
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
                if isinstance(spec, str) and spec in PIPES:
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
                edge = {"down": frm[1] == 0, "up": to[1] == 16, "north": frm[2] == 0, "south": to[2] == 16,
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
        for mat, tex in PIPES.items():
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
            ("bio_digester", bio_digester(), True, False),
            ("drone_dock", drone_dock(), False, False),
            ("swarm_dock", drone_dock("swarm_pad"), False, False),
            ("drone_assembler", drone_assembler(), True, False),
            ("fabricator", fabricator(), True, False),
            ("acid_synthesizer", acid_synthesizer(), True, False),
            ("water_pump", water_pump(), True, False)):
        size = build(name, parts, lit_variant, translucent)
        print(f"{name}: {len(parts)} boxes, atlas {size}x{size}")
    card_texture()
    # how far in each body sits from the block edge decides how deep its sockets go
    build("combustion_generator_port", side_port(3.25), False)
    build("combustion_generator_port_up", top_port(10), False)
    build("bio_digester_port", side_port(2.25), False)
    build("drone_dock_port", side_port(2.5, y0=1), False)
    build("drone_assembler_port", low_port(), False)
    build("fabricator_port", side_port(2), False)
    build("fabricator_port_up", top_port(12.75), False)
    build("acid_synthesizer_port", side_port(2, y0=3.5), False)
    build("water_pump_port", low_port(), False)
    build("water_pump_port_up", water_pump_port_up(), False)
    build("water_pump_intake", water_pump_intake(), False)
    blockstate("combustion_generator", True, {"up"})
    blockstate("bio_digester", True, set())
    blockstate("drone_dock", False, set())
    blockstate("swarm_dock", False, set(), port_of="drone_dock")
    blockstate("drone_assembler", True, set())
    blockstate("fabricator", True, {"up"})
    blockstate("acid_synthesizer", True, set())
    blockstate("water_pump", True, {"up"}, top_turns=True, below="water_pump_intake")
