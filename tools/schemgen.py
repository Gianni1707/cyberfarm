"""Shared tools for CyberFarm's schematics: structure files in the vanilla Structure Block format
(gzipped NBT) under data/cyberfarm/structure/schematic/, shown in the Multiblock Projector's list.
The tech village's buildings are drawn with it (tools/village_buildings.py, tools/villagegen.py).

Each schematic is drawn layer by layer: x runs west to east, z north to south, and the north side
(z = 0) is the front, which the projector turns towards the player. The style follows the mod:
clean white and grey panels, dark Cyber Frame trim with cyan accents, light blue glass.

preview() draws a schematic seen from the front-right, as a PNG.
"""
import gzip
import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/data/cyberfarm/structure/schematic"
DATA_VERSION = 4790  # SharedConstants.WORLD_VERSION of Minecraft 26.1.2

# ---------------------------------------------------------------------------- NBT writer

END, BYTE, INT, DOUBLE, STRING, LIST, COMPOUND = 0, 1, 3, 6, 8, 9, 10


class Int(int):
    pass


class Byte(int):
    pass


class Double(float):
    pass


def _name(out, name):
    data = name.encode("utf-8")
    out += struct.pack(">H", len(data)) + data


def _payload(out, value):
    if isinstance(value, Int):
        out += struct.pack(">i", value)
    elif isinstance(value, Byte):
        out += struct.pack(">b", value)
    elif isinstance(value, Double):
        out += struct.pack(">d", value)
    elif isinstance(value, str):
        _name(out, value)
    elif isinstance(value, list):
        kind = _kind(value[0]) if value else END
        out += struct.pack(">bi", kind, len(value))
        for item in value:
            _payload(out, item)
    elif isinstance(value, dict):
        for key, item in value.items():
            out += struct.pack(">b", _kind(item))
            _name(out, key)
            _payload(out, item)
        out += struct.pack(">b", END)
    else:
        raise TypeError(value)


def _kind(value):
    kinds = ((Int, INT), (Byte, BYTE), (Double, DOUBLE), (str, STRING), (list, LIST))
    return next((kind for cls, kind in kinds if isinstance(value, cls)), COMPOUND)


def write_nbt(path, root):
    out = bytearray(struct.pack(">b", COMPOUND))
    _name(out, "")
    _payload(out, root)
    path.parent.mkdir(parents=True, exist_ok=True)
    # mtime=0 keeps the file identical between runs
    path.write_bytes(gzip.compress(bytes(out), mtime=0))


# ---------------------------------------------------------------------------- blocks

def state(name, **props):
    return (name if ":" in name else "minecraft:" + name, tuple(sorted((k, str(v)) for k, v in props.items())))


def stairs(name, facing, half="bottom", shape="straight"):
    return state(name, facing=facing, half=half, shape=shape, waterlogged="false")


def slab(name, kind="bottom"):
    return state(name, type=kind, waterlogged="false")


# Blocks that reach out to their neighbours (panes, bars, fences): their sides are worked out on save
CONNECTING = ("_pane", "iron_bars", "_fence")
SIDES = {"north": (0, 0, -1), "south": (0, 0, 1), "west": (-1, 0, 0), "east": (1, 0, 0)}
# Blocks a connecting block does not join: thin or decorative ones
NOT_SOLID = ("_slab", "_stairs", "lantern", "torch", "end_rod", "lightning_rod", "_door", "ladder", "button",
             "_trapdoor", "chain", "_carpet", "flower_pot", "air")


def connector(name):
    return any(part in name for part in CONNECTING)


class Schematic:
    def __init__(self, width, height, depth):
        self.size = (width, height, depth)
        self.blocks = {}

    def set(self, x, y, z, block):
        assert 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2], (x, y, z)
        self.blocks[(x, y, z)] = block

    def fill(self, x0, y0, z0, x1, y1, z1, block):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    self.set(x, y, z, block)

    def ring(self, x0, z0, x1, z1, y, block):
        """The outline of a rectangle on layer y."""
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    self.set(x, y, z, block)

    def remove(self, x, y, z):
        self.blocks.pop((x, y, z), None)

    def door(self, x, y, z, name, facing, hinge="left"):
        for half, dy in (("lower", 0), ("upper", 1)):
            self.set(x, y + dy, z, state(name, facing=facing, half=half, hinge=hinge, open="false", powered="false"))

    def _connected(self, pos, block):
        """A pane, bar or fence with its sides set towards solid blocks and other connecting ones."""
        props = dict(block[1])
        for side, (dx, dy, dz) in SIDES.items():
            other = self.blocks.get((pos[0] + dx, pos[1] + dy, pos[2] + dz))
            joins = other is not None and (connector(other[0]) or not any(part in other[0] for part in NOT_SOLID))
            props[side] = "true" if joins else "false"
        props.setdefault("waterlogged", "false")
        return block[0], tuple(sorted(props.items()))

    def save(self, name, out=OUT, entities=()):
        """Writes the schematic as out/name.nbt; entities are (x, y, z, nbt) placed with it."""
        resolved = {pos: self._connected(pos, block) if connector(block[0]) else block for pos, block in self.blocks.items()}
        palette = []
        index = {}
        blocks = []
        for (x, y, z), block in sorted(resolved.items(), key=lambda item: (item[0][1], item[0][2], item[0][0])):
            if block not in index:
                index[block] = len(palette)
                entry = {"Name": block[0]}
                if block[1]:
                    entry["Properties"] = dict(block[1])
                palette.append(entry)
            blocks.append({"pos": [Int(x), Int(y), Int(z)], "state": Int(index[block])})
        write_nbt(out / f"{name}.nbt", {
            "DataVersion": Int(DATA_VERSION),
            "size": [Int(v) for v in self.size],
            "palette": palette,
            "blocks": blocks,
            "entities": [{"pos": [Double(x), Double(y), Double(z)], "blockPos": [Int(int(x)), Int(int(y)), Int(int(z))], "nbt": nbt}
                         for x, y, z, nbt in entities],
        })
        print(f"{name}: {len(blocks)} blocks, {len(palette)} block states")
        self.resolved = resolved


# ---------------------------------------------------------------------------- the schematics

WHITE = state("white_concrete")
LIGHT = state("light_gray_concrete")
GREY = state("gray_concrete")
DARK = state("polished_deepslate")
QUARTZ = state("smooth_quartz")
FRAME = state("cyberfarm:cyber_frame")
LAMP = state("sea_lantern")
CYAN = state("cyan_concrete")
GLASS = state("light_blue_stained_glass")
PANE = state("light_blue_stained_glass_pane")
BARS = state("iron_bars")


# ---------------------------------------------------------------------------- preview

COLOURS = {
    "white_concrete": (220, 224, 228), "light_gray_concrete": (150, 152, 150), "gray_concrete": (70, 74, 78),
    "polished_deepslate": (60, 60, 66), "smooth_quartz": (236, 230, 222), "cyber_frame": (40, 46, 54),
    "sea_lantern": (190, 230, 225), "cyan_concrete": (24, 130, 145), "light_blue_stained_glass": (110, 170, 220),
    "light_blue_stained_glass_pane": (120, 180, 225), "iron_bars": (150, 155, 160), "iron_door": (200, 200, 205),
    "polished_blackstone_button": (50, 45, 50), "smooth_quartz_slab": (236, 230, 222), "smooth_quartz_stairs": (225, 219, 211),
    "ladder": (150, 115, 70), "lightning_rod": (200, 120, 80), "iron_trapdoor": (190, 190, 195), "barrel": (130, 95, 55),
    "crafting_table": (140, 100, 60), "polished_deepslate_wall": (60, 60, 66), "end_rod": (240, 235, 220),
}


def _shape(name, props):
    """(x0, y0, z0, x1, y1, z1) of the block's main box, roughly, for the preview."""
    props = dict(props)
    if name.endswith("_slab"):
        return (0, 0.5, 0, 1, 1, 1) if props.get("type") == "top" else (0, 0, 0, 1, 0.5, 1)
    if any(part in name for part in ("_pane", "iron_bars", "_fence", "_wall")):
        return (0.35, 0, 0.35, 0.65, 1, 0.65)
    if any(part in name for part in ("end_rod", "lightning_rod")):
        return (0.4, 0, 0.4, 0.6, 1, 0.6)
    if "button" in name:
        return (0.35, 0.4, 0.85, 0.65, 0.6, 1)
    if "trapdoor" in name:
        return (0, 0, 0, 1, 0.2, 1)
    if name.endswith("ladder") or name.endswith("_door"):
        facing = props.get("facing", "north")
        return {"north": (0, 0, 0.8, 1, 1, 1), "south": (0, 0, 0, 1, 1, 0.2),
                "east": (0, 0, 0, 0.2, 1, 1), "west": (0.8, 0, 0, 1, 1, 1)}[facing]
    return (0, 0, 0, 1, 1, 1)


def preview(s, path):
    """The schematic seen from the front-right and above, each block a shaded box."""
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    from texgen import Canvas
    w, h, d = s.size
    tile = 14  # pixels per block along an edge, on screen
    def project(x, y, z):
        # Looking at the north (front) side from the north-east: the front and east faces show
        return (x + z) * tile, (x - z) * tile * 0.5 - y * tile * 1.15
    corners = [project(x, y, z) for x in (0, w) for y in (0, h) for z in (0, d)]
    min_x = min(c[0] for c in corners) - 4
    min_y = min(c[1] for c in corners) - 4
    canvas = Canvas(int(max(c[0] for c in corners) - min_x) + 8, int(max(c[1] for c in corners) - min_y) + 8)
    for row in canvas.pixels:
        for i in range(len(row)):
            row[i] = (34, 38, 44, 255)

    def polygon(points, colour):
        xs = [p[0] - min_x for p in points]
        ys = [p[1] - min_y for p in points]
        for py in range(int(min(ys)), int(max(ys)) + 1):
            for px in range(int(min(xs)), int(max(xs)) + 1):
                inside = False
                j = len(xs) - 1
                for i in range(len(xs)):
                    if (ys[i] > py) != (ys[j] > py) and px < (xs[j] - xs[i]) * (py - ys[i]) / (ys[j] - ys[i]) + xs[i]:
                        inside = not inside
                    j = i
                if inside and 0 <= py < canvas.height and 0 <= px < canvas.width:
                    canvas.pixels[py][px] = colour + (255,)

    def shade(colour, factor):
        return tuple(min(255, int(c * factor)) for c in colour)

    # Far blocks first: the view looks from high x, low z (the front) downwards
    for (x, y, z), block in sorted(s.resolved.items(), key=lambda item: (item[0][0] - item[0][2], item[0][1])):
        name = block[0].split(":")[1]
        colour = COLOURS.get(name, (255, 0, 255))
        x0, y0, z0, x1, y1, z1 = _shape(name, block[1])
        X0, X1, Y0, Y1, Z0, Z1 = x + x0, x + x1, y + y0, y + y1, z + z0, z + z1
        top = [project(X0, Y1, Z0), project(X1, Y1, Z0), project(X1, Y1, Z1), project(X0, Y1, Z1)]
        front = [project(X0, Y0, Z0), project(X1, Y0, Z0), project(X1, Y1, Z0), project(X0, Y1, Z0)]
        side = [project(X1, Y0, Z0), project(X1, Y0, Z1), project(X1, Y1, Z1), project(X1, Y1, Z0)]
        polygon(side, shade(colour, 0.62))
        polygon(front, shade(colour, 0.82))
        polygon(top, shade(colour, 1.0))
    canvas.save(Path(path))
