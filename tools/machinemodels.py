"""Builds the 3D machine models (prototype): Combustion Generator, Bio-Digester and Drone Dock.

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
    "1": "#6c7a88", "2": "#4a5664", "3": "#333c47", "4": "#232a33", "5": "#12161c",     # body
    "a": "#e4ecf0", "b": "#b4c0ca", "c": "#7c8894", "s": "#0b1014",                     # trim, recess
    "g": "#d4fbff", "G": "#2fe0ff", "h": "#145c6c",                                      # cyan light
    "y": "#e8b830", "u": "#fff2a8", "v": "#ffc23d", "w": "#ff7a1f", "x": "#b8391a",      # hazard, fire
    "m": "#b6f08a", "o": "#5fcf6a", "p": "#2f8a52", "j": "#d6f3f7",                      # liquid, glint
    "A": "#ffc98a", "B": "#ef8f3e", "C": "#b65a26", "D": "#6e3420",                      # copper
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


MATERIALS = {"body": body(), "plinth": plinth(), "trim": trim(), "lid": lid(), "grille": grille(), "pipe": pipe(),
             "pad": pad(), "liquid": liquid(), "copper": plate("A", "B", "C", "D")}
EMISSIVE = {"fire": "machine_fire", "light": "machine_light"}   # material -> texture name (+ "_off")
PIPES = {"gas_pipe_up": "gas_pipe"}   # vertical stubs drawn with the real pipe texture, like the pipe blocks


# --------------------------------------------------------------------------- models, front facing +z (south)
def octagon(x0, y0, z0, x1, y1, z1, cut, mat):
    return [((x0 + cut, y0, z0), (x1 - cut, y1, z1), mat), ((x0, y0, z0 + cut), (x1, y1, z1 - cut), mat)]


def round_tank(x0, y0, z0, x1, y1, z1, mat):
    """Twelve-sided column from three crossed boxes, close to a cylinder at block scale."""
    w = (x1 - x0) / 6
    return [((x0 + 2 * w, y0, z0), (x1 - 2 * w, y1, z1), mat), ((x0, y0, z0 + 2 * w), (x1, y1, z1 - 2 * w), mat),
            ((x0 + w, y0, z0 + w), (x1 - w, y1, z1 - w), mat)]


def combustion_generator():
    cap = {"up": "lid", "south": "trim", "east": "trim"}
    return [
        ((1, 0, 1), (15, 1, 15), "plinth"),
        *octagon(2, 1, 2, 14, 9, 13, 2, {"up": "lid", "south": "body", "east": "body"}),
        *octagon(1.6, 6.5, 1.6, 14.4, 7.5, 13.4, 2, "copper"),     # copper band round the body
        ((14, 2, 5), (14.5, 6, 10), "grille"),                      # intake grille
        ((4, 2, 12.5), (12, 8, 13.5), "copper"),                    # firebox door frame
        ((5, 3, 13), (11, 6.25, 13.75), "fire"),
        ((6.5, 3, 13.75), (7.25, 6.25, 14.25), "pipe"),             # grille bars
        ((8.75, 3, 13.75), (9.5, 6.25, 14.25), "pipe"),
        *octagon(4, 9, 4, 11, 10, 11, 1, cap),                      # dome
        ((3, 9, 4), (6, 15, 7), "pipe"),                            # chimney
        ((2.5, 12, 3.5), (6.5, 13, 7.5), "copper"),
        ((2.5, 15, 3.5), (6.5, 16, 7.5), {"up": "plinth", "south": "trim", "east": "trim"}),
        ((8, 10, 3), (12, 12.5, 7), {"up": "grille", "south": "copper", "east": "copper"}),   # fuel hopper
        ((13.5, 3, 6), (15, 6, 10), "trim"),                        # FE terminal
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
        ((4.5, 5.5, 13.5), (11.5, 8.5, 14.5), "copper"),
        ((6.5, 5.5, 14), (9.5, 8.5, 14.75), "liquid"),              # sludge behind it
        ((5.5, 6.5, 14), (10.5, 7.5, 14.75), "liquid"),
        ((7, 11.25, 13.75), (9, 12, 14.25), "light"),               # running light
        *round_tank(4, 13, 4, 12, 14.5, 12, lid),                   # domed top
        ((6, 14.5, 6), (10, 16, 10), "gas_pipe_up"),
    ]


def drone_dock():
    # pad top stays at 6 px and the beacons sit on the corners, where the drone and DockRenderer expect them
    pad = {"up": "pad", "south": "trim", "east": "trim"}
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
    elements = []
    for frm, to, spec in parts:
        frm, to = flip((frm, to))
        elements.append((frm, to, spec))
    patches = {}
    for n, (frm, to, spec) in enumerate(elements):
        if isinstance(spec, str) and (spec in EMISSIVE or spec in PIPES):
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
        for n, (frm, to, spec) in enumerate(elements):
            faces = {}
            emissive = isinstance(spec, str) and spec in EMISSIVE
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
            if emissive and lit:
                element.update({"shade": False, "light_emission": 15})
            element["faces"] = faces
            out.append(element)
        textures = {"particle": f"cyberfarm:block/models/{name}", "atlas": f"cyberfarm:block/models/{name}"}
        for mat, tex in EMISSIVE.items():
            textures[mat] = f"cyberfarm:block/{tex}" + ("" if lit else "_off")
        for mat, tex in PIPES.items():
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


def to_rgba(tex):
    return [[rgba(k) for k in row] for row in tex.g]


# --------------------------------------------------------------------------- pipe ports
def side_port(depth, y0=5.5):
    """Socket where a pipe arm (6..10) meets the block edge: copper collar on the edge, housing back to the body."""
    return [((5.5, y0, 16 - depth), (10.5, 10.5, 15.25), "body"), ((5, 5, 15.25), (11, 11, 16), "copper")]


def top_port(bottom):
    return [((5.5, bottom, 5.5), (10.5, 15.25, 10.5), "body"), ((5, 15.25, 5), (11, 16, 11), "copper")]


def blockstate(name, lit, ports):
    """Multipart: the machine for each facing (and lit state), plus a port wherever a pipe is plugged in."""
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
        apply = {"model": f"cyberfarm:block/{name}_port"}
        if y:
            apply["y"] = y
        parts.append({"when": {side: "true"}, "apply": apply})
    if "up" in ports:
        parts.append({"when": {"up": "true"}, "apply": {"model": f"cyberfarm:block/{name}_port_up"}})
    (ASSETS / f"blockstates/{name}.json").write_text(json.dumps({"multipart": parts}, indent=2) + "\n")


if __name__ == "__main__":
    for mat, tex in EMISSIVE.items():
        maker = fire if mat == "fire" else light
        save_png(ASSETS / f"textures/block/{tex}.png", to_rgba(maker(True)))
        save_png(ASSETS / f"textures/block/{tex}_off.png", to_rgba(maker(False)))
    for name, parts, lit_variant, translucent in (
            ("combustion_generator", combustion_generator(), True, False),
            ("bio_digester", bio_digester(), True, False),
            ("drone_dock", drone_dock(), False, False)):
        size = build(name, parts, lit_variant, translucent)
        print(f"{name}: {len(parts)} boxes, atlas {size}x{size}")
    # how far in each body sits from the block edge decides how deep its sockets go
    build("combustion_generator_port", side_port(3.25), False)
    build("combustion_generator_port_up", top_port(10), False)
    build("bio_digester_port", side_port(2.25), False)
    build("drone_dock_port", side_port(2.5, y0=1), False)
    blockstate("combustion_generator", True, {"up"})
    blockstate("bio_digester", True, set())
    blockstate("drone_dock", False, set())
