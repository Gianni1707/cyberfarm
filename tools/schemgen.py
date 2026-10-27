"""Writes CyberFarm's ready-made schematics: structure files in the vanilla Structure Block format
(gzipped NBT) under data/cyberfarm/structure/schematic/, shown in the Multiblock Projector's list.

Each schematic is drawn layer by layer: x runs west to east, z north to south, and the north side
(z = 0) is the front, which the projector turns towards the player. The style follows the mod:
clean white and grey panels, dark Cyber Frame trim with cyan accents, light blue glass.

Run from the project root:  python tools/schemgen.py
With --preview <dir> it also draws each schematic seen from the front-right, as <name>.png.
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


def watch_tower():
    """A white tower on a dark plinth: frame corners, tall slit windows, a cantilevered deck with a
    railing, a canopy on frame posts and a lamp on top."""
    s = Schematic(7, 14, 7)
    # Plinth: dark rim, grey floor, a lamp in each corner
    s.fill(0, 0, 0, 6, 0, 6, LIGHT)
    s.ring(0, 0, 6, 6, 0, DARK)
    for x, z in ((0, 0), (6, 0), (0, 6), (6, 6)):
        s.set(x, 0, z, LAMP)
    # Shaft: 5x5 of white panels between frame corners, a grey band every few layers
    for y in range(1, 8):
        s.ring(1, 1, 5, 5, y, GREY if y == 4 else WHITE)
        for x, z in ((1, 1), (5, 1), (1, 5), (5, 5)):
            s.set(x, y, z, FRAME)
    # Tall slit windows in the middle of the sides, either side of the ladder at the back, over the door
    for y in range(2, 7):
        if y != 4:
            s.set(1, y, 3, PANE)
            s.set(5, y, 3, PANE)
            s.set(2, y, 5, PANE)
            s.set(4, y, 5, PANE)
    for y in (5, 6):
        s.set(3, y, 1, PANE)
    # Entrance: an iron door with a button beside it, under a small quartz canopy
    s.door(3, 1, 1, "iron_door", "north")
    s.set(4, 2, 0, state("polished_blackstone_button", face="wall", facing="north", powered="false"))
    for x in (2, 3, 4):
        s.set(x, 3, 0, slab("smooth_quartz_slab", "top"))
    # Ladder up the inside of the back wall, through the deck
    for y in range(1, 9):
        s.set(3, y, 4, state("ladder", facing="north", waterlogged="false"))
    # Deck: overhanging 7x7, quartz with a light rim, held by stairs under the edge
    s.fill(0, 8, 0, 6, 8, 6, QUARTZ)
    s.ring(0, 0, 6, 6, 8, LIGHT)
    s.remove(3, 8, 4)
    s.set(3, 8, 4, state("ladder", facing="north", waterlogged="false"))
    for x in range(1, 6):
        s.set(x, 7, 0, stairs("smooth_quartz_stairs", "south", "top"))
        s.set(x, 7, 6, stairs("smooth_quartz_stairs", "north", "top"))
    for z in range(1, 6):
        s.set(0, 7, z, stairs("smooth_quartz_stairs", "east", "top"))
        s.set(6, 7, z, stairs("smooth_quartz_stairs", "west", "top"))
    # Railing round the deck, frame posts holding a canopy
    s.ring(0, 0, 6, 6, 9, BARS)
    for x, z in ((1, 1), (5, 1), (1, 5), (5, 5)):
        s.set(x, 9, z, FRAME)
        s.set(x, 10, z, FRAME)
    s.fill(1, 11, 1, 5, 11, 5, QUARTZ)
    s.ring(1, 1, 5, 5, 11, LIGHT)
    for x in range(1, 6):
        s.set(x, 11, 0, slab("smooth_quartz_slab"))
        s.set(x, 11, 6, slab("smooth_quartz_slab"))
    for z in range(1, 6):
        s.set(0, 11, z, slab("smooth_quartz_slab"))
        s.set(6, 11, z, slab("smooth_quartz_slab"))
    # Beacon: a frame block with a lamp and a rod on top
    s.set(3, 12, 3, LAMP)
    s.set(3, 13, 3, state("lightning_rod", facing="up", powered="false", waterlogged="false"))
    s.save("watch_tower")
    return s


def storage_shed():
    """A prefab depot: white panels between grey columns on a dark footing, a band of light blue
    windows under a cyan stripe, an iron door in a frame portal off a covered porch, a parapet roof
    with lamps set in; barrels and a crafting table inside."""
    s = Schematic(9, 7, 8)
    s.fill(0, 0, 0, 8, 0, 7, DARK)
    s.fill(1, 0, 2, 7, 0, 6, LIGHT)
    # Walls on z 1-7; the front row (z 0) is the porch
    for y in range(1, 5):
        s.ring(0, 1, 8, 7, y, WHITE)
    for x, z in ((0, 1), (8, 1), (0, 7), (8, 7), (4, 7), (0, 4), (8, 4)):
        s.fill(x, 1, z, x, 4, z, GREY)
    # Windows between the columns, a cyan stripe over them
    for x in (1, 2, 6, 7):
        s.set(x, 2, 1, PANE)
    for x in (1, 2, 3, 5, 6, 7):
        s.set(x, 2, 7, PANE)
    for z in (2, 3, 5, 6):
        s.set(0, 2, z, PANE)
        s.set(8, 2, z, PANE)
    for x in range(1, 8):
        s.set(x, 4, 1, CYAN)
        s.set(x, 4, 7, CYAN)
    for z in range(2, 7):
        s.set(0, 4, z, CYAN)
        s.set(8, 4, z, CYAN)
    # Entrance: frame portal, iron door, a button on the wall beside it
    s.fill(3, 1, 1, 3, 3, 1, FRAME)
    s.fill(5, 1, 1, 5, 3, 1, FRAME)
    s.set(4, 3, 1, FRAME)
    s.door(4, 1, 1, "iron_door", "north")
    s.set(6, 2, 0, state("polished_blackstone_button", face="wall", facing="north", powered="false"))
    s.set(6, 2, 1, WHITE)
    # Porch canopy: quartz slabs out over the front, on two frame posts
    for x in range(2, 7):
        s.set(x, 4, 0, slab("smooth_quartz_slab", "top"))
    for x in (2, 6):
        s.fill(x, 1, 0, x, 3, 0, BARS)
    # Roof: a light parapet round grey slabs with lamps set in, a vent and a rod
    s.fill(0, 5, 1, 8, 5, 7, LIGHT)
    s.fill(1, 5, 2, 7, 5, 6, GREY)
    for x, z in ((2, 3), (6, 3), (2, 5), (6, 5)):
        s.set(x, 5, z, LAMP)
    s.ring(0, 1, 8, 7, 6, slab("smooth_quartz_slab"))
    s.set(4, 6, 4, state("iron_trapdoor", facing="north", half="bottom", open="false", powered="false", waterlogged="false"))
    s.set(7, 6, 6, state("lightning_rod", facing="up", powered="false", waterlogged="false"))
    # Inside: barrels stacked along the back, a crafting table in the middle
    for x in (1, 2, 6, 7):
        s.set(x, 1, 6, state("barrel", facing="up", open="false"))
    for x in (1, 7):
        s.set(x, 2, 6, state("barrel", facing="up", open="false"))
    s.set(4, 1, 6, state("crafting_table"))
    s.save("storage_shed")
    return s


def drone_pad():
    """A raised 9x9 landing pad with beveled quartz edges, cyan markings round the middle (where the
    Drone Dock goes), lamps set in the deck and four frame light posts."""
    s = Schematic(9, 4, 9)
    # Raised deck with stair bevels all round and slabs at the corners
    s.fill(1, 0, 1, 7, 0, 7, LIGHT)
    for i in range(1, 8):
        s.set(i, 0, 0, stairs("smooth_quartz_stairs", "south"))
        s.set(i, 0, 8, stairs("smooth_quartz_stairs", "north"))
        s.set(0, 0, i, stairs("smooth_quartz_stairs", "east"))
        s.set(8, 0, i, stairs("smooth_quartz_stairs", "west"))
    for x, z in ((0, 0), (8, 0), (0, 8), (8, 8)):
        s.set(x, 0, z, slab("smooth_quartz_slab"))
    # Markings: a cyan ring round the white middle, lamps at the ends of the cross
    s.ring(2, 2, 6, 6, 0, CYAN)
    s.fill(3, 0, 3, 5, 0, 5, WHITE)
    for x, z in ((4, 1), (4, 7), (1, 4), (7, 4)):
        s.set(x, 0, z, LAMP)
    # Light posts on the corners of the deck
    for x, z in ((1, 1), (7, 1), (1, 7), (7, 7)):
        s.set(x, 1, z, FRAME)
        s.set(x, 2, z, state("polished_deepslate_wall", up="true", north="none", south="none", east="none", west="none",
                             waterlogged="false"))
        s.set(x, 3, z, state("end_rod", facing="up"))
    s.save("drone_pad")
    return s


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


if __name__ == "__main__":
    built = [watch_tower(), storage_shed(), drone_pad()]
    if "--preview" in sys.argv:
        out = Path(sys.argv[sys.argv.index("--preview") + 1])
        out.mkdir(parents=True, exist_ok=True)
        for schematic, name in zip(built, ("watch_tower", "storage_shed", "drone_pad")):
            preview(schematic, out / f"{name}.png")
