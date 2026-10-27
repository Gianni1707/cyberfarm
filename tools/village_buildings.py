"""The tech village's buildings, each in three tiers, every tier only adding blocks to the one
before (a Builder drone never breaks a block), and each tier holding something that works. Used by
tools/villagegen.py, which writes them out; drawn on a plot each, with x running west to east, z
north to south and the north side (z = 0) the front.

The plots: the Outpost 26 x 14 x 21 (the Mechanist's cabin at its west end, as found in the
world), the Drone Workshop 29 x 17 x 19, the Power Station 25 x 13 x 19, the Hydroponic Farm
35 x 10 x 17, the Residence 36 x 15 x 15, the Market Hall 29 x 12 x 26 and the Watch Tower
19 x 21 x 19.
"""
import schemgen as sg
from schemgen import Schematic, state, stairs, slab, WHITE, LIGHT, GREY, DARK, QUARTZ, FRAME, LAMP, CYAN, GLASS, PANE, BARS

SPRUCE = state("spruce_planks")
LOG = state("stripped_spruce_log", axis="y")
STONE = state("stone_bricks")
MOSSY = state("mossy_stone_bricks")
# Coarse dirt for paths: a survival player cannot get dirt path as an item, so a drone could not place it
PATH = state("coarse_dirt")
GRAVEL = state("gravel")
GRASS = state("grass_block", snowy="false")
SOLAR = state("daylight_detector", inverted="false", power="0")
TERMINAL = state("cyberfarm:supply_terminal", facing="north", lit="false")
CHAIN_X = state("iron_chain", axis="x", waterlogged="false")
CHAIN_Y = state("iron_chain", axis="y", waterlogged="false")
LEAVES = state("oak_leaves", distance="1", persistent="true", waterlogged="false")
AZALEA = state("flowering_azalea_leaves", distance="1", persistent="true", waterlogged="false")
YELLOW = state("yellow_concrete")
BLACK = state("black_concrete")
ORANGE = state("orange_concrete")
LIME = state("lime_concrete")
CONDUIT = state("cyberfarm:energy_conduit")
GAS_PIPE = state("cyberfarm:gas_pipe")
ANDESITE = state("polished_andesite")
DEEP = state("polished_deepslate")
FENCE = state("spruce_fence")
POST = state("polished_deepslate_wall", up="true", north="none", south="none", east="none", west="none", waterlogged="false")
WALLPOST = POST

# Where each building sits on its plot
WX = 5   # the Drone Workshop's hangar, west wall
FX = 11  # the glasshouse on the farm's plot
RX = 11  # the house on the residence's plot
MX = 10  # the hall on the market's plot

sg.COLOURS.update({'daylight_detector': (38, 56, 92), 'white_bed': (230, 230, 235), 'light_blue_bed': (110, 170, 220), 'cyan_bed': (24, 130, 145), 'farmland': (95, 60, 35), 'water': (50, 90, 200), 'supply_terminal': (30, 140, 170), 'smithing_table': (60, 55, 60), 'chest': (150, 110, 60), 'bell': (230, 190, 60), 'lectern': (150, 110, 65), 'lantern': (240, 200, 120), 'flower_pot': (150, 80, 55), 'potted_fern': (70, 130, 60), 'potted_red_tulip': (200, 60, 50), 'potted_azure_bluet': (200, 210, 230), 'potted_cactus': (60, 130, 60), 'cyan_carpet': (24, 130, 145), 'white_carpet': (230, 230, 230), 'light_blue_carpet': (110, 170, 220), 'composter': (120, 90, 50), 'oak_leaves': (60, 120, 50), 'flowering_azalea_leaves': (90, 130, 70), 'spruce_planks': (115, 85, 52), 'stripped_spruce_log': (120, 90, 55), 'spruce_trapdoor': (105, 78, 48), 'spruce_fence': (105, 78, 48), 'spruce_stairs': (115, 85, 52), 'spruce_slab': (115, 85, 52), 'stone_bricks': (125, 125, 125), 'mossy_stone_bricks': (110, 120, 100), 'dirt_path': (150, 125, 75), 'gravel': (130, 125, 120), 'grass_block': (95, 150, 70), 'polished_deepslate_stairs': (55, 55, 62), 'polished_deepslate_slab': (55, 55, 62), 'iron_chain': (80, 85, 95), 'bookshelf': (120, 80, 50), 'anvil': (70, 70, 74), 'grindstone': (140, 140, 140), 'cauldron': (60, 60, 64), 'water_cauldron': (60, 70, 120), 'hopper': (70, 70, 74), 'redstone_lamp': (190, 120, 60), 'blast_furnace': (90, 90, 95), 'smoker': (90, 80, 70), 'loom': (180, 150, 110), 'cartography_table': (130, 100, 70), 'poppy': (200, 40, 40), 'dandelion': (240, 210, 50), 'azure_bluet': (220, 225, 235), 'short_grass': (90, 150, 60), 'fern': (70, 130, 60), 'white_wool': (235, 235, 235), 'cyan_wool': (24, 130, 145), 'light_blue_wool': (110, 170, 220), 'item_frame': (140, 100, 60), 'white_banner': (235, 235, 235), 'copper_bulb': (200, 120, 80), 'waxed_copper_bulb': (200, 120, 80), 'lightning_rod': (200, 120, 80), 'tinted_glass': (50, 45, 60), 'hay_block': (200, 170, 50), 'wheat': (210, 190, 90), 'carrots': (90, 160, 60), 'potatoes': (110, 160, 70), 'beetroots': (130, 60, 70), 'melon': (110, 160, 50), 'pumpkin': (210, 130, 40), 'barrel': (130, 95, 55), 'crafting_table': (140, 100, 60), 'polished_andesite': (140, 142, 140), 'andesite_wall': (140, 142, 140), 'stone_brick_wall': (125, 125, 125), 'cyber_frame': (40, 46, 54), 'drone_dock': (60, 70, 80), 'white_terracotta': (210, 180, 160), 'cyan_terracotta': (90, 100, 100), 'quartz_pillar': (236, 230, 222), 'smooth_stone_slab': (160, 160, 160), 'brewing_stand': (90, 80, 70), 'lily_pad': (60, 120, 50), 'glow_lichen': (120, 160, 130), 'end_rod': (245, 240, 230), 'sea_lantern': (190, 230, 225), 'yellow_concrete': (240, 175, 20), 'black_concrete': (20, 22, 26), 'spruce_door': (105, 78, 48), 'iron_door': (200, 200, 205), 'cyan_stained_glass_pane': (40, 140, 160), 'white_stained_glass': (230, 235, 240), 'light_blue_concrete': (60, 140, 200), 'orange_wool': (230, 120, 30), 'yellow_wool': (240, 200, 50), 'red_wool': (170, 40, 40), 'campfire': (200, 120, 40), 'stonecutter': (120, 120, 120), 'jukebox': (110, 75, 50), 'carved_pumpkin': (210, 130, 40), 'ladder': (150, 115, 70), 'drone_assembler': (40, 120, 150), 'energy_conduit': (60, 170, 220), 'mech_repair_bay': (230, 120, 40), 'iron_block': (210, 210, 215), 'orange_concrete': (220, 100, 20), 'coal_block': (25, 25, 28), 'smooth_stone': (160, 160, 160), 'combustion_generator': (90, 60, 40), 'gas_turbine': (70, 120, 130), 'steam_engine': (230, 230, 235), 'steam_engine_cylinder': (200, 205, 210), 'steam_engine_dynamo': (60, 140, 160), 'gas_pipe': (230, 170, 40), 'gas_tank': (200, 160, 60), 'lime_concrete': (110, 180, 40), 'hologram_table': (40, 160, 200), 'swarm_dock': (50, 80, 95), 'bio_digester': (90, 130, 60), 'fletching_table': (190, 170, 120), 'polished_deepslate': (55, 55, 62), 'stone_brick_slab': (125, 125, 125), 'stone_brick_stairs': (125, 125, 125), 'spruce_log': (70, 50, 30), 'light_gray_concrete': (140, 140, 135), 'azalea_leaves': (90, 130, 70), 'chiseled_stone_bricks': (130, 130, 130)})
sg.COLOURS["coarse_dirt"] = (110, 80, 55)
_base_shape = sg._shape


def walls(s, x0, z0, x1, z1, y0, y1, base=LIGHT, body=WHITE, band=None, band_block=CYAN, corner=FRAME):
    """Walls with a light base course, white body, an optional coloured band and frame corners."""
    for y in range(y0, y1 + 1):
        s.ring(x0, z0, x1, z1, y, base if y == y0 else band_block if y == band else body)
    for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        s.fill(x, y0, z, x, y1, z, corner)



def _shape(name, props):
    if name in ("lantern", "flower_pot") or name.startswith("potted_"):
        return (0.3, 0, 0.3, 0.7, 0.45, 0.7)
    if name in ("poppy", "dandelion", "azure_bluet", "short_grass", "fern", "wheat", "carrots", "potatoes", "beetroots"):
        return (0.25, 0, 0.25, 0.75, 0.55, 0.75)
    if name.endswith("_carpet") or name == "lily_pad":
        return (0, 0, 0, 1, 0.08, 1)
    if name == "daylight_detector":
        return (0, 0, 0, 1, 0.4, 1)
    if name == "iron_chain":
        axis = dict(props).get("axis", "y")
        return {"x": (0, 0.42, 0.42, 1, 0.58, 0.58), "z": (0.42, 0.42, 0, 0.58, 0.58, 1), "y": (0.42, 0, 0.42, 0.58, 1, 0.58)}[axis]
    if name.endswith("_bed"):
        return (0, 0, 0, 1, 0.56, 1)
    if name in ("anvil", "grindstone", "lectern", "brewing_stand"):
        return (0.15, 0, 0.15, 0.85, 0.9, 0.85)
    if name == "farmland":
        return (0, 0, 0, 1, 0.94, 1)
    return _base_shape(name, props)


# Previews draw small things (lanterns, flowers, carpets, beds...) at their size
sg._shape = _shape


def window(s, x, y, z, facing, width=1, height=2, sill=True):
    """A window in a wall facing `facing`: panes, a quartz sill outside and a frame lintel over it."""
    dx, dz = {"north": (1, 0), "south": (1, 0), "east": (0, 1), "west": (0, 1)}[facing]
    out = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[facing]
    for i in range(width):
        for j in range(height):
            s.set(x + dx * i, y + j, z + dz * i, PANE)
        if sill:
            ox, oz = x + dx * i + out[0], z + dz * i + out[1]
            if 0 <= ox < s.size[0] and 0 <= oz < s.size[2]:
                s.set(ox, y - 1, oz, stairs("smooth_quartz_stairs", opposite(facing), "top"))


def flower_box(s, x, y, z, facing, width, flower):
    out = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[facing]
    dx, dz = {"north": (1, 0), "south": (1, 0), "east": (0, 1), "west": (0, 1)}[facing]
    for i in range(width):
        ox, oz = x + dx * i + out[0], z + dz * i + out[1]
        if 0 <= ox < s.size[0] and 0 <= oz < s.size[2]:
            s.set(ox, y, oz, state("spruce_trapdoor", facing=facing, half="top", open="false", powered="false", waterlogged="false"))
            s.set(ox, y + 1, oz, state(flower))


def opposite(facing):
    return {"north": "south", "south": "north", "east": "west", "west": "east"}[facing]


def gable_roof(s, x0, x1, z0, z1, y, material="polished_deepslate", ridge="smooth_quartz_slab", gable=WHITE, trim=QUARTZ):
    """A pitched roof with its ridge along x, from z0 (front) to z1, overhanging x0..x1 by the given
    bounds; the gable ends filled with wall and a round window."""
    k = 0
    while z0 + k <= z1 - k:
        zf, zb = z0 + k, z1 - k
        for x in range(x0, x1 + 1):
            if zf == zb:
                s.set(x, y + k, zf, slab(ridge))
            else:
                s.set(x, y + k, zf, stairs(f"{material}_stairs", "south"))
                s.set(x, y + k, zb, stairs(f"{material}_stairs", "north"))
        # gable walls under the slope, one in from each end
        for x in (x0 + 1, x1 - 1):
            for z in range(zf + 1, zb):
                s.set(x, y + k, z, gable)
        k += 1
    return k


def lamp_post(s, x, y, z, height=2):
    s.fill(x, y, z, x, y + height - 1, z, state("polished_deepslate_wall", up="true", north="none", south="none", east="none", west="none",
                                                  waterlogged="false"))
    s.set(x, y + height, z, state("lantern", hanging="false", waterlogged="false"))


def bed(s, x, y, z, colour="white_bed", facing="south"):
    dz = {"south": 1, "north": -1}.get(facing, 0)
    dx = {"east": 1, "west": -1}.get(facing, 0)
    s.set(x, y, z, state(colour, facing=facing, part="foot", occupied="false"))
    s.set(x + dx, y, z + dz, state(colour, facing=facing, part="head", occupied="false"))


def hydroponic_farm(s):
    """Level 2, the Hydroponic Farm: a long glasshouse with a pitched glass roof on frame ribs,
    raised planter beds edged in quartz either side of a walkway, water channels with lily pads,
    an irrigation pipe under the ridge, hanging lamps, tanks and composters at the back, a
    vestibule with a door and flower boxes."""
    x0, x1, z0, z1 = 1, 13, 2, 15
    s.fill(x0, 0, z0, x1, 0, z1, STONE)
    for y in range(1, 5):
        s.ring(x0, z0, x1, z1, y, GLASS if y > 1 else LIGHT)
    for z in range(z0, z1 + 1, 3):
        for x in (x0, x1):
            s.fill(x, 1, z, x, 4, z, FRAME)
    for x in (x0, 7, x1):
        s.fill(x, 1, z1, x, 4, z1, FRAME)
    # Pitched glass roof: two blocks in each step up, frame ribs every third row, a frame ridge
    for k in range(4):
        y = 5 + k
        for z in range(z0, z1 + 1):
            rib = (z - z0) % 3 == 0
            for x in (x0 + 2 * k, x0 + 2 * k + 1, x1 - 2 * k, x1 - 2 * k - 1):
                s.set(x, y, z, FRAME if rib else GLASS)
    s.fill(6, 8, z0, 8, 8, z1, GLASS)
    s.fill(7, 9, z0, 7, 9, z1, FRAME)
    # Glass in the gable ends, under each step of the roof
    for z in (z0, z1):
        for k in range(3):
            s.fill(x0 + 2 * k + 2, 5 + k, z, x1 - 2 * k - 2, 5 + k, z, GLASS)
    # Vestibule with a door, flower boxes either side
    s.door(7, 1, z0, "iron_door", "north")
    s.set(7, 3, z0, FRAME)
    for x, f in ((5, "potted_red_tulip"), (9, "potted_azure_bluet")):
        flower_box(s, x, 1, z0, "north", 1, f)
    for x in (4, 10):
        lamp_post(s, x, 1, 0)
    s.fill(7, 0, 0, 7, 0, 1, PATH)
    # Planter beds edged in quartz, a walkway down the middle, water channels
    for bx0, bx1 in ((2, 5), (9, 12)):
        s.fill(bx0, 0, 4, bx1, 0, 13, state("farmland", moisture="7"))
        for z in range(4, 14):
            crop = ("wheat", "7") if z < 7 else ("carrots", "7") if z < 10 else ("potatoes", "7") if z < 12 else ("beetroots", "3")
            for x in range(bx0, bx1 + 1):
                if x not in (3, 11):
                    s.set(x, 1, z, state(crop[0], age=crop[1]))
        s.fill(bx0 + 1, 0, 4, bx0 + 1, 0, 13, state("water", level="0"))
        s.set(bx0 + 1, 1, 6, state("lily_pad"))
        s.set(bx0 + 1, 1, 11, state("lily_pad"))
        s.ring(bx0 - 1, 3, bx1 + 1, 14, 0, QUARTZ)
    s.fill(6, 0, 3, 8, 0, 14, state("polished_andesite"))
    # Irrigation pipe under the ridge, hanging lamps
    s.fill(7, 7, z0 + 1, 7, 7, z1 - 1, state("iron_chain", axis="z", waterlogged="false"))
    for z in (5, 9, 13):
        s.set(4, 4, z, state("lantern", hanging="true", waterlogged="false"))
        s.set(10, 4, z, state("lantern", hanging="true", waterlogged="false"))
    # Tanks and composters along the back
    for x, block in ((2, state("water_cauldron", level="3")), (3, state("water_cauldron", level="3")),
                     (11, state("composter", level="5")), (12, state("composter", level="2"))):
        s.set(x, 1, 14, block)
    s.set(7, 1, 14, state("barrel", facing="up", open="false"))
    s.set(7, 2, 14, state("hay_block", axis="y"))


def residence(s):
    """Level 3, the Residence: a two-storey house for colonists, a dark pitched roof with a dormer
    and solar panels on a rack, a balcony with plants over the porch, flower boxes, a front garden
    with a bench and a lamp; a kitchen and living room downstairs, three bedrooms upstairs."""
    x0, x1, z0, z1 = 2, 12, 4, 12
    s.fill(x0, 0, z0, x1, 0, z1, STONE)
    walls(s, x0, z0, x1, z1, 1, 8, band=4, band_block=GREY)
    s.set(x0, 4, z0, FRAME)
    for x in (7,):
        s.fill(x, 1, z1, x, 8, z1, FRAME)
    s.fill(x0 + 1, 4, z0 + 1, x1 - 1, 4, z1 - 1, SPRUCE)
    # Door, windows and flower boxes
    s.door(7, 1, z0, "spruce_door", "north")
    s.fill(6, 1, z0, 6, 3, z0, FRAME)
    s.fill(8, 1, z0, 8, 3, z0, FRAME)
    for x in (3, 10):
        window(s, x, 2, z0, "north", width=2, sill=False)
        flower_box(s, x, 1, z0, "north", 2, "potted_red_tulip" if x == 3 else "potted_azure_bluet")
    for x in (3, 10):
        window(s, x, 6, z0, "north", width=2)
    for z in (6, 9):
        window(s, x0, 2, z, "west", width=2)
        window(s, x1, 2, z, "east", width=2)
        window(s, x0, 6, z, "west", width=2)
        window(s, x1, 6, z, "east", width=2)
    # Balcony over the porch: quartz deck, railing, plants, a door out
    s.fill(5, 4, 1, 9, 4, 3, QUARTZ)
    for x in range(5, 10):
        s.set(x, 5, 1, BARS)
    for z in (2, 3):
        s.set(5, 5, z, BARS)
        s.set(9, 5, z, BARS)
    s.set(6, 5, 2, state("potted_fern"))
    s.set(8, 5, 2, AZALEA)
    s.door(7, 5, z0, "iron_door", "north")
    for x in (5, 9):
        s.fill(x, 1, 1, x, 3, 1, FENCE)
    # Roof: pitched along x, with a dormer on the front slope and a rack of solar panels
    gable_roof(s, x0 - 1, x1 + 1, z0 - 1, z1 + 1, 9)
    s.fill(6, 10, z0 + 1, 8, 11, z0 + 1, WHITE)
    s.set(7, 10, z0 + 1, PANE)
    s.fill(6, 12, z0, 8, 12, z0 + 1, slab("polished_deepslate_slab"))
    for x in range(9, 12):
        s.set(x, 12, 10, BARS)
        s.set(x, 13, 10, SOLAR)
    s.fill(4, 10, 11, 4, 13, 11, STONE)
    # Front garden: path, flowers, a bench, a lamp, a hedge
    s.fill(7, 0, 0, 7, 0, 3, PATH)
    s.set(3, 1, 2, stairs("spruce_stairs", "south"))
    s.set(4, 1, 2, stairs("spruce_stairs", "south"))
    lamp_post(s, 11, 1, 1)
    for x in range(0, 15):
        if x not in (6, 7, 8):
            s.set(x, 1, 0, LEAVES)
    for (x, z), f in (((2, 1), "poppy"), ((12, 2), "dandelion"), ((1, 3), "azure_bluet"), ((13, 3), "poppy")):
        s.set(x, 1, z, state(f))
    # Downstairs: kitchen and living room; ladder up
    s.set(x1 - 1, 1, z1 - 1, state("smoker", facing="north", lit="false"))
    s.set(x1 - 2, 1, z1 - 1, state("barrel", facing="up", open="false"))
    s.set(x1 - 3, 1, z1 - 1, state("crafting_table"))
    s.fill(x0 + 2, 1, 7, x0 + 4, 1, 9, state("light_blue_carpet"))
    s.set(x0 + 1, 1, 8, stairs("spruce_stairs", "east"))
    s.set(x0 + 1, 1, 9, stairs("spruce_stairs", "east"))
    for y in range(1, 5):
        s.set(x0 + 1, y, z1 - 1, state("ladder", facing="east", waterlogged="false"))
    s.remove(x0 + 1, 4, z1 - 1)
    s.set(x0 + 1, 4, z1 - 1, state("ladder", facing="east", waterlogged="false"))
    s.set(7, 3, 8, state("lantern", hanging="true", waterlogged="false"))
    # Upstairs: three bedrooms
    bed(s, 4, 5, 9, "light_blue_bed", "south")
    bed(s, 7, 5, 9, "cyan_bed", "south")
    bed(s, 10, 5, 9, "white_bed", "south")
    for x in (5, 8, 11):
        s.set(x, 5, 11, state("chest", facing="north", type="single", waterlogged="false"))
    s.set(7, 7, 8, state("lantern", hanging="true", waterlogged="false"))


def market_hall(s):
    """Level 4, the Market Hall: an open hall on frame columns under a striped canopy, stalls with
    their own awnings and goods (hay, melons, pumpkins, fish barrels, chests), the village bell on a
    quartz plinth over a little basin, lamp posts, benches and planters."""
    x0, x1, z0, z1 = 1, 17, 1, 13
    s.fill(x0, 0, z0, x1, 0, z1, QUARTZ)
    s.ring(x0, z0, x1, z1, 0, LIGHT)
    for x in range(x0 + 2, x1 - 1, 2):
        for z in range(z0 + 2, z1 - 1, 2):
            if (x + z) % 4 == 0:
                s.set(x, 0, z, state("polished_andesite"))
    for x in (x0, 6, 12, x1):
        for z in (z0, 7, z1):
            if not (z == 7 and x in (6, 12)):
                s.fill(x, 1, z, x, 4, z, FRAME)
    # Striped canopy: a low pitched roof of white and cyan wool, a quartz rim
    for k in range(0, 4):
        y = 5 + k
        for x in range(x0 - 1 + k, x1 + 2 - k):
            for z in range(z0 - 1, z1 + 2):
                if x in (x0 - 1 + k, x1 + 1 - k):
                    s.set(x, y, z, state("cyan_wool" if z % 2 else "white_wool"))
        if k == 3:
            for x in range(x0 + 3, x1 - 2):
                for z in range(z0 - 1, z1 + 2):
                    s.set(x, y, z, state("cyan_wool" if z % 2 else "white_wool"))
    s.ring(x0 - 1, z0 - 1, x1 + 1, z1 + 1, 5, slab("smooth_quartz_slab", "top"))
    # Stalls along the sides: counters, awnings, goods
    stalls = [(3, 3, "hay_block"), (3, 11, "melon"), (15, 3, "pumpkin"), (15, 11, "barrel")]
    for sx, sz, goods in stalls:
        for dx in (-1, 0, 1):
            s.set(sx + dx, 1, sz, slab("spruce_slab", "top"))
            s.set(sx + dx, 3, sz + (-1 if sz < 7 else 1), state("orange_wool" if dx == 0 else "white_wool"))
        good = state(goods, axis="y") if goods == "hay_block" else state("barrel", facing="up", open="false") if goods == "barrel" else \
            state("carved_pumpkin", facing="north") if goods == "pumpkin" else state(goods)
        s.set(sx, 2, sz, good)
        s.set(sx - 1, 2, sz, state("lantern", hanging="false", waterlogged="false"))
        s.set(sx + 1, 1, sz + (-1 if sz < 7 else 1), state("chest", facing="north", type="single", waterlogged="false"))
    # The bell on a plinth over a basin with lily pads
    s.fill(8, 0, 6, 10, 0, 8, state("water", level="0"))
    s.set(8, 1, 6, state("lily_pad"))
    s.ring(7, 5, 11, 9, 0, STONE)
    s.set(9, 0, 7, QUARTZ)
    s.set(9, 1, 7, state("quartz_pillar", axis="y"))
    s.set(9, 2, 7, state("bell", attachment="floor", facing="north", powered="false"))
    # Benches and planters, lamp posts at the entrances
    for z in (4, 10):
        s.set(7, 1, z, stairs("spruce_stairs", "east"))
        s.set(11, 1, z, stairs("spruce_stairs", "west"))
    for x, z in ((6, 4), (12, 10)):
        s.set(x, 1, z, AZALEA)
    for x, z in ((0, 0), (18, 0), (0, 14), (18, 14)):
        lamp_post(s, x, 1, z)
    s.set(9, 4, 3, state("lantern", hanging="true", waterlogged="false"))
    s.set(9, 4, 11, state("lantern", hanging="true", waterlogged="false"))


def hangar(s):
    """Tier I: the hangar under a stepped barrel roof, a wide portal with hazard stripes, a landing
    pad out front, two marked bays under a crane chain, the workbench wall and an office corner."""
    x0, x1, z0, z1 = WX + 1, WX + 15, 5, 13
    s.fill(x0, 0, z0, x1, 0, z1, state("polished_andesite"))
    walls(s, x0, z0, x1, z1, 1, 5, band=5, band_block=GREY)
    for x in (WX + 5, WX + 11):
        s.fill(x, 1, z1, x, 5, z1, FRAME)
    s.fill(x0, 1, 9, x0, 5, 9, FRAME)
    s.fill(x1, 1, 9, x1, 5, 9, FRAME)
    # Portal with hazard stripes
    for x in range(WX + 5, WX + 12):
        for y in range(1, 5):
            s.remove(x, y, z0)
    for y in range(1, 5):
        for x in (WX + 4, WX + 12):
            s.set(x, y, z0, YELLOW if y % 2 else BLACK)
    s.fill(WX + 4, 5, z0, WX + 12, 5, z0, FRAME)
    for x in range(WX + 5, WX + 12):
        s.set(x, 0, z0, YELLOW if x % 2 else BLACK)
    # A side door on the east wall, where the wing joins later
    s.door(x1, 1, 11, "iron_door", "east")
    # Landing pad out front
    s.fill(WX + 4, 0, 0, WX + 12, 0, 4, LIGHT)
    s.ring(WX + 5, 1, WX + 11, 4, 0, CYAN)
    s.fill(WX + 6, 0, 2, WX + 10, 0, 3, WHITE)
    for x in (WX + 4, WX + 12):
        s.set(x, 0, 0, LAMP)
    for x in (WX + 3, WX + 13):
        lamp_post(s, x, 1, 1)
    # Barrel roof with a glass ridge
    profile = [(0, 6, "block"), (1, 7, "slab"), (2, 7, "block"), (3, 8, "slab"), (4, 8, "block")]
    for x in range(x0, x1 + 1):
        for i, y, kind in profile:
            for z in (z0 - 1 + i, z1 + 1 - i):
                s.set(x, y, z, LIGHT if kind == "block" else slab("smooth_quartz_slab"))
        s.fill(x, 8, z0 + 4, x, 8, z1 - 4, GLASS if (x - WX) % 4 else FRAME)
    # The ends of the barrel closed with wall under the curve
    for x in (x0, x1):
        s.fill(x, 6, z0, x, 6, z1, WHITE)
        s.fill(x, 7, z0 + 2, x, 7, z1 - 2, WHITE)
    # Inside: bays, crane, workbench wall, office
    s.ring(WX + 3, 7, WX + 7, 11, 0, CYAN)
    s.ring(WX + 9, 7, WX + 13, 11, 0, YELLOW)
    s.set(WX + 5, 1, 9, state("cyberfarm:drone_assembler", facing="north", lit="false"))
    s.set(WX + 11, 1, 9, state("cyberfarm:drone_dock", facing="north"))
    # Power comes in on the west side and runs under both bays to the east wall
    s.fill(WX, 0, 9, WX + 15, 0, 9, CONDUIT)
    s.fill(WX + 5, 6, 9, WX + 5, 7, 9, CHAIN_Y)
    s.set(WX + 5, 5, 9, state("lightning_rod", facing="down", powered="false", waterlogged="false"))
    for dx, block in ((2, state("anvil", facing="north")), (3, state("grindstone", face="floor", facing="north")),
                      (4, state("smithing_table")), (6, state("crafting_table")), (7, state("barrel", facing="up", open="false"))):
        s.set(WX + dx, 1, z1 - 1, block)
    s.fill(WX + 2, 3, z1 - 1, WX + 7, 3, z1 - 1, slab("smooth_quartz_slab", "top"))
    for dx in (2, 4, 6):
        s.set(WX + dx, 4, z1 - 1, state("barrel", facing="north", open="false"))
    s.fill(WX + 12, 1, 10, WX + 14, 3, 10, PANE)
    s.remove(WX + 13, 1, 10)
    s.remove(WX + 13, 2, 10)
    s.set(WX + 14, 1, 12, state("lectern", facing="west", has_book="false", powered="false"))
    s.set(WX + 12, 1, 12, slab("smooth_quartz_slab", "top"))
    s.set(WX + 12, 2, 12, state("potted_fern"))
    window(s, x0, 2, 11, "west", width=2, sill=False)
    s.set(x0, 3, 7, state("iron_trapdoor", facing="west", half="top", open="true", powered="false", waterlogged="false"))
    for x, z in ((x0 + 1, 7), (x1 - 1, 7)):
        s.set(x, 4, z, state("end_rod", facing="up"))
    s.set(WX + 8, 5, 9, LAMP)
    window(s, x1, 2, 7, "east", width=1, sill=False)


def wing(s):
    """Tier II: a charging and storage wing on the east side with a roll-up door, charging posts,
    racks of crates, a flat roof with a railing; a second, yellow pad in front of it; a crane arm on
    the hangar roof."""
    x0, x1, z0, z1 = WX + 16, WX + 23, 5, 13
    s.fill(x0, 0, z0, x1, 0, z1, state("polished_andesite"))
    walls(s, x0, z0, x1, z1, 1, 4, band=4)
    # Roll-up door: stacked iron trapdoors in a frame
    for y in range(1, 4):
        for x in (WX + 19, WX + 20):
            s.set(x, y, z0, state("iron_trapdoor", facing="north", half="top", open="true", powered="false", waterlogged="false"))
    s.fill(WX + 18, 1, z0, WX + 18, 3, z0, FRAME)
    s.fill(WX + 21, 1, z0, WX + 21, 3, z0, FRAME)
    # Charging bays: three real Drone Docks along the back, wired to the line from the hangar
    for x in (WX + 18, WX + 20, WX + 22):
        s.set(x, 1, z1 - 1, state("cyberfarm:drone_dock", facing="north"))
        s.set(x, 4, z1 - 1, state("lantern", hanging="true", waterlogged="false"))
    s.fill(WX + 16, 0, 9, WX + 18, 0, 9, CONDUIT)
    s.fill(WX + 18, 0, 10, WX + 18, 0, 12, CONDUIT)
    s.fill(WX + 19, 0, 12, WX + 22, 0, 12, CONDUIT)
    # Crate racks along the east wall
    for z in range(z0 + 2, z1 - 1, 2):
        for y in (1, 2):
            s.set(x1 - 1, y, z, state("barrel", facing="west", open="false"))
        s.set(x1 - 1, 3, z, state("chest", facing="west", type="single", waterlogged="false"))
    window(s, x1, 2, 9, "east", width=1)
    # Flat roof with a railing and lamps
    s.fill(x0, 5, z0, x1, 5, z1, LIGHT)
    s.fill(x0 + 1, 5, z0 + 1, x1 - 1, 5, z1 - 1, GREY)
    s.ring(x0, z0, x1, z1, 6, BARS)
    for x, z in ((x0 + 2, z0 + 2), (x1 - 2, z1 - 2)):
        s.set(x, 5, z, LAMP)
    # Second pad, yellow, in front of the wing
    s.fill(WX + 16, 0, 0, WX + 23, 0, 4, LIGHT)
    s.ring(WX + 17, 1, WX + 22, 4, 0, YELLOW)
    s.set(WX + 19, 0, 2, WHITE)
    s.set(WX + 20, 0, 2, WHITE)
    lamp_post(s, WX + 23, 1, 0)
    # Crane arm on the hangar roof: a frame mast and boom with a hanging hook
    s.fill(WX + 3, 9, 9, WX + 3, 11, 9, FRAME)
    s.fill(WX + 3, 12, 9, WX + 9, 12, 9, FRAME)
    s.fill(WX + 9, 10, 9, WX + 9, 11, 9, CHAIN_Y)
    s.set(WX + 9, 9, 9, state("lightning_rod", facing="down", powered="false", waterlogged="false"))


def repair_hall(s):
    """Tier III: a mech repair hall on the west side, tall and wide enough for the mech to walk in
    through an open portal: a Mech Repair Bay in the floor under a gantry crane, wired to the line,
    a workbench and spare parts; solar arrays on the wing roof; fuel tanks at the back; the pads
    joined by a lit taxiway."""
    rx0, rx1, rz0, rz1 = 0, 5, 6, 13
    s.fill(rx0, 0, rz0, rx1, 0, rz1, state("polished_andesite"))
    walls(s, rx0, rz0, rx1, rz1, 1, 6, band=6, band_block=CYAN)
    # Open portal, two wide and five high, for the mech; hazard stripes on its jambs
    for x in (2, 3):
        for y in range(1, 6):
            s.remove(x, y, rz0)
    for y in range(1, 6):
        s.set(1, y, rz0, YELLOW if y % 2 else BLACK)
        s.set(4, y, rz0, YELLOW if y % 2 else BLACK)
    window(s, rx0, 3, 10, "west", width=2)
    # Lean-to roof falling to the west, a skylight strip
    for x in range(rx0, rx1 + 1):
        y = 7 + (x - rx0) // 2
        for z in range(rz0 - 1, rz1 + 2):
            s.set(x, y, z, stairs("polished_deepslate_stairs", "east"))
        # The front and back walls rise under the slope
        for z in (rz0, rz1):
            if y > 7:
                s.fill(x, 7, z, x, y - 1, z, WHITE)
    # The high east side closed up to the roof
    s.fill(rx1, 7, rz0, rx1, 8, rz1, WHITE)
    for z in range(rz0, rz1 + 1, 2):
        s.set(rx1, 9, z, GLASS)
    # The repair bay under a gantry crane
    # shortcut: polished andesite holds the Mech Repair Bay's place until that block exists; then it goes here
    s.set(2, 0, 10, ANDESITE)
    for x, z in ((1, 9), (3, 9), (1, 11), (3, 11), (2, 9), (2, 11), (1, 10), (3, 10)):
        s.set(x, 0, z, YELLOW)
    for z in (8, 12):
        s.fill(1, 1, z, 1, 6, z, FRAME)
        s.fill(4, 1, z, 4, 6, z, FRAME)
    s.fill(1, 6, 9, 1, 6, 11, FRAME)
    s.fill(4, 6, 9, 4, 6, 11, FRAME)
    s.fill(2, 6, 10, 3, 6, 10, FRAME)
    # Wired to the line coming in under the hangar's west wall
    s.fill(2, 0, 9, 5, 0, 9, CONDUIT)
    s.set(2, 0, 9, CONDUIT)
    # Workbench and spare parts along the back
    for x, block in ((1, state("anvil", facing="east")), (2, state("grindstone", face="floor", facing="north")),
                     (3, state("barrel", facing="up", open="false")), (4, state("iron_block"))):
        s.set(x, 1, rz1 - 1, block)
    s.set(3, 2, rz1 - 1, state("barrel", facing="up", open="false"))
    s.set(4, 2, rz1 - 1, state("lantern", hanging="false", waterlogged="false"))
    # Solar arrays on the wing roof
    for z in (7, 9, 11):
        for x in range(WX + 17, WX + 23):
            s.set(x, 6, z, SOLAR)
    # Fuel tanks at the back: white cylinders with cyan bands, a pipe into the hangar
    for tx in (WX + 3, WX + 7):
        for y in range(1, 5):
            for dx in (0, 1):
                for dz in (0, 1):
                    s.set(tx + dx, y, 15 + dz, state("cyan_concrete") if y == 3 else state("quartz_pillar", axis="y"))
        s.set(tx, 5, 15, slab("smooth_quartz_slab"))
        s.set(tx + 1, 5, 16, slab("smooth_quartz_slab"))
    s.fill(WX + 4, 3, 14, WX + 4, 3, 14, CHAIN_X)
    s.fill(WX + 5, 3, 14, WX + 7, 3, 14, CHAIN_X)
    # A lit taxiway between the pads and the tower door
    for x in range(2, WX + 4):
        s.set(x, 0, 3, LAMP if x % 3 == 0 else LIGHT)
    s.fill(2, 0, 4, 3, 0, 5, LIGHT)


def machine(name, facing="north"):
    return state(f"cyberfarm:{name}", facing=facing, lit="false")


def pad(s, x0, z0, x1, z1, colour):
    s.ring(x0, z0, x1, z1, 0, colour)


def chimney(s, x, z, y0, y1):
    s.fill(x, y0, z, x, y1, z, STONE)
    s.set(x, y1 - 1, z, state("cyan_concrete"))
    s.set(x, y1 + 1, z, state("iron_bars"))


def generator_hall(s):
    """Tier I: the generator hall. Two Combustion Generators, each fed by a hopper from a fuel chest
    above it (a Transport drone can keep the chests full); the energy line runs under the floor the
    whole width of the hall, so the later wings plug into it, and out to the transformer."""
    x0, x1, z0, z1 = 8, 16, 6, 13
    s.fill(x0, 0, z0, x1, 0, z1, state("smooth_stone"))
    walls(s, x0, z0, x1, z1, 1, 4, base=STONE, band=4, band_block=ORANGE)
    s.door(12, 1, z0, "iron_door", "north")
    s.fill(11, 1, z0, 11, 3, z0, FRAME)
    s.fill(13, 1, z0, 13, 3, z0, FRAME)
    window(s, 9, 2, z0, "north", width=2)
    window(s, 14, 2, z0, "north", width=2)
    s.door(x0, 1, 11, "iron_door", "west")
    s.door(x1, 1, 11, "iron_door", "east")
    gable_roof(s, x0, x1, z0 - 1, z1 + 1, 5)
    # The generators, fuel hoppers and chests
    for gx in (10, 14):
        pad(s, gx - 1, 8, gx + 1, 10, CYAN)
        s.set(gx, 1, 9, machine("combustion_generator"))
        s.set(gx, 2, 9, state("hopper", facing="down", enabled="true"))
        s.set(gx, 3, 9, state("chest", facing="north", type="single", waterlogged="false"))
    s.set(12, 4, 10, state("lantern", hanging="true", waterlogged="false"))
    s.fill(9, 1, 12, 10, 1, 12, state("coal_block"))
    chimney(s, 14, 12, 5, 11)
    # The energy line: under the floor across the hall, then out the front to the transformer
    s.fill(8, 0, 9, 16, 0, 9, CONDUIT)
    s.fill(12, 0, 3, 12, 0, 8, CONDUIT)
    s.fill(13, 0, 3, 17, 0, 3, CONDUIT)
    for x in range(8, 17):
        for z in range(0, 6):
            if (x, 0, z) not in s.blocks:
                s.set(x, 0, z, GRAVEL)
    # Transformer and the power line along the front, east to the town
    s.set(17, 1, 4, state("iron_block"))
    s.set(17, 2, 4, state("cyan_concrete"))
    s.set(17, 3, 4, state("lightning_rod", facing="up", powered="false", waterlogged="false"))
    for x in (18, 21, 24):
        s.fill(x, 1, 2, x, 4, 2, POST)
        s.set(x, 5, 2, state("end_rod", facing="up"))
    for x in range(17, 25):
        if x not in (18, 21, 24):
            s.set(x, 4, 2, CHAIN_X)


def gas_wing(s):
    """Tier II: the gas wing on the west side. Two Gas Turbines fed by gas pipes from two gas
    tanks in white drums out front (fill them with a pipe from your methane or biogas), wired into
    the energy line; a vent and a second chimney."""
    x0, x1, z0, z1 = 0, 7, 6, 13
    s.fill(x0, 0, z0, x1, 0, z1, state("smooth_stone"))
    walls(s, x0, z0, x1, z1, 1, 4, base=STONE, band=4, band_block=YELLOW)
    window(s, x0, 2, 11, "west", width=2)
    s.fill(x0, 5, z0, x1, 5, z1, LIGHT)
    s.fill(x0 + 1, 5, z0 + 1, x1 - 1, 5, z1 - 1, GREY)
    s.ring(x0, z0, x1, z1, 6, slab("smooth_quartz_slab"))
    s.set(2, 6, 8, state("iron_trapdoor", facing="north", half="bottom", open="false", powered="false", waterlogged="false"))
    for tx in (2, 5):
        pad(s, tx - 1, 9, tx + 1, 11, YELLOW)
        s.set(tx, 1, 10, machine("gas_turbine"))
    chimney(s, 5, 12, 6, 11)
    # The energy line on from the hall, under the turbines
    s.fill(1, 0, 9, 7, 0, 9, CONDUIT)
    s.set(2, 0, 10, CONDUIT)
    s.set(5, 0, 10, CONDUIT)
    # Drums out front, each with a real gas tank in its heart and a pipe into the wing
    for tx in (0, 4):
        for y in range(1, 4):
            for dx in range(3):
                for dz in range(3):
                    if (dx, dz) != (1, 1):
                        s.set(tx + dx, y, 1 + dz, state("cyan_concrete") if y == 2 else WHITE)
        s.set(tx + 1, 1, 2, state("cyberfarm:gas_tank", facing="north"))
        s.set(tx + 1, 4, 2, QUARTZ)
        for dx, dz in ((1, 0), (0, 1), (2, 1), (1, 2)):
            s.set(tx + dx, 4, 1 + dz, slab("smooth_quartz_slab"))
        s.fill(tx + 1, 1, 3, tx + 1, 1, 8, GAS_PIPE)
    s.set(1, 1, 9, GAS_PIPE)
    s.set(2, 1, 9, GAS_PIPE)
    s.set(5, 1, 9, GAS_PIPE)
    for x in range(0, 8):
        for z in range(0, 6):
            if (x, 0, z) not in s.blocks:
                s.set(x, 0, z, GRAVEL)


def steam_hall(s):
    """Tier III: the steam hall on the east side. A full Steam Engine (boiler, four cylinders,
    dynamo) along the hall, its own gas tank piped to the boiler, the dynamo wired into the energy
    line; clerestory windows and a cooling tower behind."""
    x0, x1, z0, z1 = 17, 24, 6, 13
    s.fill(x0, 0, z0, x1, 0, z1, state("smooth_stone"))
    walls(s, x0, z0, x1, z1, 1, 6, base=STONE, band=6, band_block=ORANGE)
    for x in (19, 22):
        window(s, x, 4, z0, "north", width=1)
    window(s, x1, 2, 9, "east", width=2)
    gable_roof(s, x0, x1, z0 - 1, z1 + 1, 7)
    s.set(20, 1, 7, state("cyberfarm:steam_engine", facing="north", formed="false"))
    for z in range(8, 12):
        s.set(20, 1, z, state("cyberfarm:steam_engine_cylinder", formed="false"))
    s.set(20, 1, 12, state("cyberfarm:steam_engine_dynamo", formed="false"))
    s.ring(19, 7, 21, 12, 0, CYAN)
    s.set(23, 1, 7, state("cyberfarm:gas_tank", facing="west"))
    s.fill(21, 1, 7, 22, 1, 7, GAS_PIPE)
    s.fill(20, 0, 9, 20, 0, 12, CONDUIT)
    s.fill(17, 0, 9, 19, 0, 9, CONDUIT)
    s.set(20, 5, 10, state("lantern", hanging="true", waterlogged="false"))
    for y in range(1, 10):
        inset = 0 if y < 3 or y > 7 else 1
        s.ring(18 + inset, 14 + inset, 23 - inset, 18 - inset, y, state("light_gray_concrete") if y % 3 else LIGHT)
    s.ring(18, 14, 23, 18, 9, state("cyan_concrete"))


class Shifted(Schematic):
    """A view of a schematic moved by (dx, dz), so a building drawn at the origin lands on its plot."""

    def __init__(self, base, dx, dz):
        self.base, self.dx, self.dz = base, dx, dz
        self.size = (base.size[0] - dx, base.size[1], base.size[2] - dz)
        self.blocks = base.blocks

    def set(self, x, y, z, block):
        self.base.set(x + self.dx, y, z + self.dz, block)

    def remove(self, x, y, z):
        self.base.remove(x + self.dx, y, z + self.dz)


def dock(s, x, y, z, facing="north", pad=CYAN):
    s.ring(x - 1, z - 1, x + 1, z + 1, y - 1, pad)
    s.set(x, y, z, state("cyberfarm:drone_dock", facing=facing))


def flat_roof(s, x0, z0, x1, z1, y, rim=True):
    s.fill(x0, y, z0, x1, y, z1, LIGHT)
    s.fill(x0 + 1, y, z0 + 1, x1 - 1, y, z1 - 1, GREY)
    if rim:
        s.ring(x0, z0, x1, z1, y + 1, slab("smooth_quartz_slab"))


def hip_roof(s, x0, z0, x1, z1, y, material="polished_deepslate"):
    """A pyramid roof: rings of stairs facing in, stepping up to a point."""
    k = 0
    while x0 + k <= x1 - k and z0 + k <= z1 - k:
        a, b, c, d = x0 + k, z0 + k, x1 - k, z1 - k
        if a == c or b == d:
            s.fill(a, y + k, b, c, y + k, d, slab(f"{material}_slab"))
            break
        for x in range(a, c + 1):
            s.set(x, y + k, b, stairs(f"{material}_stairs", "south"))
            s.set(x, y + k, d, stairs(f"{material}_stairs", "north"))
        for z in range(b + 1, d):
            s.set(a, y + k, z, stairs(f"{material}_stairs", "east"))
            s.set(c, y + k, z, stairs(f"{material}_stairs", "west"))
        k += 1
    return y + k


def supply_depot(s):
    """Tier II: a covered loading bay east of the cabin. A Drone Dock for a Transport drone that
    carries the goods from the depot chests to the Supply Terminal, so contracts fill themselves;
    behind it the Builder Swarm Dock, whose Mini Drones raise the next buildings."""
    x0, x1, z0, z1 = 14, 23, 3, 10
    s.fill(x0, 0, z0, x1, 0, z1, ANDESITE)
    s.fill(x0, 0, z0, x1, 0, z0, YELLOW)
    for x in range(10, 15):
        s.set(x, 0, 1, GRAVEL)
    # Back and east walls, the front and west open on frame posts
    for y in range(1, 5):
        block = CYAN if y == 4 else WHITE
        s.fill(x0, y, z1, x1, y, z1, block)
        s.fill(x1, y, z0, x1, y, z1, block)
    for x, z in ((x0, z0), (18, z0), (x1, z0), (x0, z1), (x1, z1), (x0, 6)):
        s.fill(x, 1, z, x, 4, z, FRAME)
    window(s, x1, 2, 6, "east", width=2)
    flat_roof(s, x0, z0, x1, z1, 5)
    s.set(16, 6, 8, state("iron_trapdoor", facing="north", half="bottom", open="false", powered="false", waterlogged="false"))
    # The dock under a hoist, wired to the town grid along the back
    dock(s, 17, 1, 6)
    s.fill(17, 4, 6, 17, 4, 6, CHAIN_Y)
    s.fill(17, 0, 7, 17, 0, z1 - 1, CONDUIT)
    s.fill(14, 0, z1 - 1, x1 + 2, 0, z1 - 1, CONDUIT)
    # The stock: chests along the back wall, crates in the corner
    for x in (15, 17, 19):
        s.set(x, 1, z1 - 1, state("chest", facing="north", type="single", waterlogged="false"))
    for y in (1, 2):
        s.set(21, y, z1 - 1, state("barrel", facing="up", open="false"))
        s.set(22, y, z1 - 1, state("barrel", facing="up", open="false"))
    s.set(22, 1, z1 - 2, state("barrel", facing="up", open="false"))
    s.set(20, 4, 6, state("lantern", hanging="true", waterlogged="false"))
    # Crates waiting in the yard out front
    for x, y in ((20, 1), (21, 1), (20, 2)):
        s.set(x, y, 1, state("barrel", facing="north", open="false"))
    lamp_post(s, 23, 1, 1)
    # The Builder Swarm Dock on its pad behind the depot, wired from the depot line
    s.fill(12, 0, 14, 16, 0, 18, LIGHT)
    s.ring(12, 14, 16, 18, 0, YELLOW)
    s.ring(13, 15, 15, 17, 0, CYAN)
    s.set(14, 1, 16, state("cyberfarm:swarm_dock", facing="north"))
    s.fill(14, 0, 10, 14, 0, 13, CONDUIT)
    for x, z in ((12, 14), (16, 14), (12, 18), (16, 18)):
        s.set(x, 1, z, state("end_rod", facing="up"))


def command_post(s):
    """Tier III: a command post behind the cabin. The Hologram Table maps the village and the
    drones at work; the bell by the door makes the outpost the village's meeting point."""
    x0, x1, z0, z1 = 2, 10, 13, 19
    s.fill(x0, 0, z0, x1, 0, z1, STONE)
    walls(s, x0, z0, x1, z1, 1, 4, band=4)
    s.door(x0, 1, 16, "iron_door", "west")
    s.fill(x0, 3, 16, x0, 3, 16, FRAME)
    window(s, 4, 2, z1, "south", width=5)
    window(s, x1, 2, 15, "east", width=3)
    window(s, 4, 2, z0, "north", width=2)
    flat_roof(s, x0, z0, x1, z1, 5)
    # Inside: the map table on a ring of carpet, consoles along the walls
    s.set(6, 1, 16, state("cyberfarm:hologram_table"))
    s.ring(5, 15, 7, 17, 1, state("cyan_carpet"))
    for x in (4, 5, 7, 8):
        s.set(x, 1, z1 - 1, slab("smooth_quartz_slab", "top"))
    s.set(4, 2, z1 - 1, state("potted_fern"))
    s.set(8, 2, z1 - 1, state("lantern", hanging="false", waterlogged="false"))
    s.set(x1 - 1, 1, z0 + 1, state("chest", facing="west", type="single", waterlogged="false"))
    s.set(6, 4, 16, LAMP)
    # A tall antenna with a dish
    s.fill(9, 6, 18, 9, 12, 18, POST)
    s.set(9, 13, 18, state("end_rod", facing="up"))
    s.set(8, 9, 18, QUARTZ)
    s.set(8, 10, 18, slab("smooth_quartz_slab"))
    s.set(9, 11, 17, state("redstone_lamp", lit="false"))
    # The bell by the door, a path round to the depot
    s.set(1, 1, 18, state("quartz_pillar", axis="y"))
    s.set(1, 2, 18, state("bell", attachment="floor", facing="west", powered="false"))
    for z in range(13, 20):
        s.set(1, 0, z, PATH)


def glasshouse(s):
    """Tier I: the glasshouse with crops in watered beds and a Drone Dock at the back for a
    Harvester or a Planter; a chest beside it for the harvest."""
    hydroponic_farm(Shifted(s, FX, 0))
    f = Shifted(s, FX, 0)
    f.set(7, 1, 13, state("cyberfarm:drone_dock", facing="north"))
    f.set(6, 1, 14, state("chest", facing="north", type="single", waterlogged="false"))
    f.fill(7, 0, 13, 7, 0, 16, CONDUIT)


def biogas_shed(s):
    """Tier II: the biogas shed east of the glasshouse. A Transport drone drops the harvest in the
    chest, a hopper feeds the Bio-Digester and the biogas is piped to a Gas Tank and on, out of the
    plot, to the Power Station's turbines."""
    x0, x1, z0, z1 = 27, 33, 5, 12
    s.fill(x0, 0, z0, x1, 0, z1, state("smooth_stone"))
    walls(s, x0, z0, x1, z1, 1, 4, base=STONE, band=4, band_block=LIME)
    s.door(x0, 1, 8, "iron_door", "west")
    window(s, 29, 2, z1, "south", width=3)
    window(s, 29, 2, z0, "north", width=3)
    gable_roof(s, x0 - 1, x1 + 1, z0 - 1, z1 + 1, 5)
    for x in range(22, 27):
        s.set(x, 0, 1, GRAVEL)
    for z in range(1, 9):
        s.set(26, 0, z, GRAVEL)
    # The digester line: chest, hopper, digester, pipe, tank, pipe out through the wall
    s.ring(29, 8, 31, 10, 0, LIME)
    s.set(30, 1, 9, state("cyberfarm:bio_digester", facing="west", lit="false"))
    s.set(30, 2, 9, state("hopper", facing="down", enabled="true"))
    s.set(30, 3, 9, state("chest", facing="west", type="single", waterlogged="false"))
    s.set(31, 1, 9, GAS_PIPE)
    s.set(32, 1, 9, state("cyberfarm:gas_tank", facing="west"))
    s.set(32, 1, 10, GAS_PIPE)
    s.set(33, 1, 10, GAS_PIPE)
    s.set(34, 1, 10, GAS_PIPE)
    # Composters and spare sacks by the door
    s.set(28, 1, 6, state("composter", level="4"))
    s.set(28, 1, 11, state("hay_block", axis="y"))
    s.set(29, 1, 11, state("barrel", facing="up", open="false"))
    s.set(30, 4, 7, state("lantern", hanging="true", waterlogged="false"))
    # A vent through the roof
    s.fill(32, 6, 11, 32, 9, 11, DARK)


def ranch(s):
    """Tier III: the ranch west of the glasshouse. Two pens under a half barn with hay and a
    trough, and a Drone Dock for a Rancher or a Breeder."""
    x0, x1, z0, z1 = 0, 9, 3, 15
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                s.set(x, 1, z, FENCE)
    s.fill(5, 1, z0 + 1, 5, 1, 9, FENCE)
    s.door(3, 1, z0, "spruce_door", "north")
    s.door(7, 1, z0, "spruce_door", "north")
    s.fill(3, 0, 0, 3, 0, 2, GRAVEL)
    s.fill(7, 0, 0, 7, 0, 2, GRAVEL)
    s.fill(4, 0, 1, 6, 0, 1, GRAVEL)
    for x, z in ((x0, z0), (x1, z0)):
        s.set(x, 2, z, state("lantern", hanging="false", waterlogged="false"))
    # The half barn over the back of the pens
    for x in (x0, x1):
        for z in (10, z1):
            s.fill(x, 1, z, x, 3, z, state("spruce_log", axis="y"))
    gable_roof(s, x0, x1, 9, z1 + 1, 4, material="spruce", ridge="spruce_slab", gable=SPRUCE, trim=SPRUCE)
    # Hay, a trough, the dock and its chest
    for x, y in ((1, 1), (2, 1), (1, 2)):
        s.set(x, y, 14, state("hay_block", axis="y"))
    s.fill(2, 0, 11, 3, 0, 11, state("water", level="0"))
    s.set(4, 1, 14, state("water_cauldron", level="3"))
    s.set(7, 1, 12, state("cyberfarm:drone_dock", facing="north"))
    s.set(8, 1, 14, state("chest", facing="north", type="single", waterlogged="false"))
    s.fill(7, 0, 13, 7, 0, 16, CONDUIT)
    s.set(5, 3, 12, state("lantern", hanging="true", waterlogged="false"))


def house(s):
    """Tier I: the house, three beds for new villagers and a smoker (a butcher's job site)."""
    residence(Shifted(s, RX, 0))


def lodge(s):
    """Tier II: the lodge east of the house, two more beds and two job sites: a lectern for a
    librarian and a cartography table for a cartographer."""
    x0, x1, z0, z1 = 27, 34, 4, 12
    s.fill(x0, 0, z0, x1, 0, z1, STONE)
    walls(s, x0, z0, x1, z1, 1, 4, band=4, band_block=GREY)
    s.door(31, 1, z0, "spruce_door", "north")
    s.fill(30, 1, z0, 30, 3, z0, FRAME)
    s.fill(32, 1, z0, 32, 3, z0, FRAME)
    flower_box(s, 28, 1, z0, "north", 1, "potted_red_tulip")
    window(s, 28, 2, z0, "north", width=1, sill=False)
    flower_box(s, 33, 1, z0, "north", 1, "potted_azure_bluet")
    window(s, 33, 2, z0, "north", width=1, sill=False)
    window(s, x1, 2, 7, "east", width=3)
    window(s, x0, 2, 7, "west", width=3)
    gable_roof(s, x0 - 1, x1 + 1, z0 - 1, z1 + 1, 5)
    s.fill(31, 0, 0, 31, 0, 3, PATH)
    lamp_post(s, 33, 1, 2)
    # Beds at the back, the job sites at the front
    bed(s, 28, 1, 10, "light_blue_bed", "south")
    bed(s, 30, 1, 10, "cyan_bed", "south")
    s.set(33, 1, 6, state("lectern", facing="west", has_book="false", powered="false"))
    s.set(33, 1, 8, state("cartography_table"))
    s.set(33, 1, 11, state("bookshelf"))
    s.set(33, 2, 11, state("bookshelf"))
    s.set(32, 1, 11, state("chest", facing="north", type="single", waterlogged="false"))
    s.fill(28, 1, 6, 29, 1, 7, state("light_blue_carpet"))
    s.set(31, 4, 8, state("lantern", hanging="true", waterlogged="false"))


def clinic(s):
    """Tier III: the clinic and canteen west of the house, two more beds, a brewing stand (a
    cleric), a cauldron (a leatherworker) and a stonecutter (a mason); a roof garden on top."""
    x0, x1, z0, z1 = 1, 9, 4, 12
    s.fill(x0, 0, z0, x1, 0, z1, STONE)
    walls(s, x0, z0, x1, z1, 1, 4, band=4, band_block=LIME)
    s.door(5, 1, z0, "iron_door", "north")
    s.fill(4, 1, z0, 4, 3, z0, FRAME)
    s.fill(6, 1, z0, 6, 3, z0, FRAME)
    window(s, 2, 2, z0, "north", width=2)
    window(s, 7, 2, z0, "north", width=2)
    window(s, x0, 2, 7, "west", width=3)
    s.fill(5, 0, 0, 5, 0, 3, PATH)
    # Roof garden: flat roof, quartz railing, planters and a bench
    flat_roof(s, x0, z0, x1, z1, 5, rim=False)
    s.ring(x0, z0, x1, z1, 6, BARS)
    for x in (2, 3, 7, 8):
        s.set(x, 6, 11, AZALEA)
    for x in (3, 7):
        s.set(x, 6, 6, state("potted_red_tulip"))
    s.set(5, 6, 10, stairs("spruce_stairs", "north"))
    # The clinic corner, the canteen table, the beds
    s.set(2, 1, 11, state("brewing_stand", has_bottle_0="false", has_bottle_1="false", has_bottle_2="false"))
    s.set(3, 1, 11, state("water_cauldron", level="3"))
    s.set(2, 1, 9, state("stonecutter", facing="east"))
    s.fill(4, 1, 7, 6, 1, 7, slab("spruce_slab", "top"))
    for x in (4, 6):
        s.set(x, 1, 6, stairs("spruce_stairs", "south"))
        s.set(x, 1, 8, stairs("spruce_stairs", "north"))
    bed(s, 7, 1, 10, "white_bed", "south")
    bed(s, 8, 1, 10, "white_bed", "south")
    s.set(5, 4, 8, state("lantern", hanging="true", waterlogged="false"))
    s.set(1, 1, 2, stairs("spruce_stairs", "south"))
    lamp_post(s, 8, 1, 2)


def market(s):
    """Tier I: the hall round the bell, its stalls built on job sites (a fletching table, a loom, a
    cartography table and a barrel) so villagers trade there."""
    market_hall(Shifted(s, MX, 0))
    m = Shifted(s, MX, 0)
    for (x, z), block in (((3, 3), state("fletching_table")), ((3, 11), state("loom", facing="north")),
                          ((15, 3), state("cartography_table")), ((15, 11), state("barrel", facing="up", open="false"))):
        m.set(x, 2, z, block)
    # The goods move out from the counters to the floor beside them
    m.set(2, 1, 4, state("hay_block", axis="y"))
    m.set(4, 1, 10, state("melon"))
    m.set(14, 1, 4, state("carved_pumpkin", facing="north"))


def warehouse(s):
    """Tier II: the warehouse behind the hall, rows of barrels and a Drone Dock for a Transport
    drone that moves the stock between the depot, the farm and the market."""
    x0, x1, z0, z1 = 12, 26, 16, 24
    s.fill(x0, 0, z0, x1, 0, z1, ANDESITE)
    walls(s, x0, z0, x1, z1, 1, 5, band=5, band_block=ORANGE)
    for x in range(18, 21):
        for y in range(1, 4):
            s.remove(x, y, z0)
    for y in range(1, 4):
        for x in (17, 21):
            s.set(x, y, z0, YELLOW if y % 2 else state("black_concrete"))
    s.fill(17, 4, z0, 21, 4, z0, FRAME)
    s.fill(18, 0, z0, 20, 0, z0, YELLOW)
    for x in (15, 23):
        window(s, x, 3, z1, "south", width=2, height=1)
    for z in (19, 22):
        window(s, x0, 3, z, "west", width=1, height=2)
        window(s, x1, 3, z, "east", width=1, height=2)
    gable_roof(s, x0 - 1, x1 + 1, z0 - 1, z1 + 1, 6)
    # Racks of barrels either side of the aisle
    for x in list(range(13, 17)) + list(range(22, 26)):
        for z in (18, 21):
            for y in (1, 2, 3):
                s.set(x, y, z, state("barrel", facing="up", open="false"))
    dock(s, 19, 1, 22)
    s.fill(19, 0, 23, 19, 0, z1 + 1, CONDUIT)
    for x in (14, 24):
        s.set(x, 1, 23, state("chest", facing="north", type="single", waterlogged="false"))
    for x in (15, 23):
        s.set(x, 5, 20, state("lantern", hanging="true", waterlogged="false"))
    s.fill(19, 0, 15, 19, 0, 15, GRAVEL)


def taxi_port(s):
    """Tier III: the taxi port west of the hall, a pad with a Drone Dock for a Taxi drone, a
    shelter with a bench, a windsock and pad lights."""
    s.fill(1, 0, 3, 7, 0, 9, LIGHT)
    s.ring(1, 3, 7, 9, 0, YELLOW)
    s.ring(2, 4, 6, 8, 0, CYAN)
    s.fill(3, 0, 6, 5, 0, 6, WHITE)
    s.fill(4, 0, 5, 4, 0, 7, WHITE)
    s.set(4, 1, 6, state("cyberfarm:drone_dock", facing="east"))
    s.fill(4, 0, 7, 4, 0, 10, CONDUIT)
    for x, z in ((1, 3), (7, 3), (1, 9), (7, 9)):
        s.set(x, 1, z, state("end_rod", facing="up"))
    # Windsock on a pole
    s.fill(0, 1, 1, 0, 5, 1, POST)
    s.set(1, 5, 1, ORANGE)
    s.set(2, 5, 1, state("white_wool"))
    s.set(3, 5, 1, ORANGE)
    # Shelter: a quartz canopy on posts over a bench, a lamp
    for x in (1, 6):
        s.fill(x, 1, 11, x, 3, 11, FRAME)
        s.fill(x, 1, 13, x, 3, 13, FRAME)
    s.fill(1, 4, 11, 6, 4, 13, slab("smooth_quartz_slab"))
    s.ring(1, 11, 6, 13, 4, CYAN)
    s.fill(2, 1, 13, 5, 1, 13, PANE)
    for x in (3, 4):
        s.set(x, 1, 12, stairs("spruce_stairs", "south"))
    s.set(2, 1, 12, state("potted_fern"))
    s.set(5, 1, 12, state("barrel", facing="up", open="false"))
    s.set(4, 3, 12, state("lantern", hanging="true", waterlogged="false"))
    s.fill(8, 0, 6, 9, 0, 6, GRAVEL)


def watch_tower(s):
    """Tier I: a stone watch tower with a lookout platform and the alarm bell (rung, it sends the
    villagers indoors and lights up raiders), lanterns all round."""
    x0, x1, z0, z1 = 7, 11, 7, 11
    s.fill(x0, 0, z0, x1, 0, z1, STONE)
    walls(s, x0, z0, x1, z1, 1, 10, base=MOSSY, body=STONE, band=6, band_block=state("chiseled_stone_bricks"), corner=DEEP)
    s.door(9, 1, z0, "spruce_door", "north")
    s.set(9, 3, z0, state("chiseled_stone_bricks"))
    for y in (5, 8):
        s.set(9, y, z0, BARS)
        s.set(x0, y, 9, BARS)
        s.set(x1, y, 9, BARS)
    s.set(9, 7, z1, BARS)
    for y in range(1, 12):
        s.set(9, y, 10, state("ladder", facing="north", waterlogged="false"))
    # The lookout: a deck over the walls, a railing, log posts and a pyramid roof
    s.fill(6, 11, 6, 12, 11, 12, SPRUCE)
    s.remove(9, 11, 10)
    s.set(9, 11, 10, state("ladder", facing="north", waterlogged="false"))
    for x in range(6, 13):
        for z in range(6, 13):
            if x in (6, 12) or z in (6, 12):
                s.set(x, 12, z, FENCE)
    for x, z in ((6, 6), (12, 6), (6, 12), (12, 12)):
        s.fill(x, 12, z, x, 14, z, state("spruce_log", axis="y"))
        s.set(x, 10, z, stairs("stone_brick_stairs", "south" if z == 6 else "north", "top"))
    top = hip_roof(s, 5, 5, 13, 13, 15)
    s.set(9, top, 9, state("lightning_rod", facing="up", powered="false", waterlogged="false"))
    s.set(9, 12, 8, state("bell", attachment="floor", facing="north", powered="false"))
    for x, z in ((8, 7), (10, 7), (8, 11), (10, 11)):
        pass
    for x, z in ((9, 6), (6, 9), (12, 9), (9, 12)):
        s.set(x, 13, z, state("lantern", hanging="false", waterlogged="false"))
    s.set(9, 9, 9, state("lantern", hanging="true", waterlogged="false"))
    # A path to the door, lanterns either side
    for z in range(0, 7):
        s.set(9, 0, z, GRAVEL)
    for x in (8, 10):
        s.set(x, 1, 6, state("lantern", hanging="false", waterlogged="false"))


def palisade(s):
    """Tier II: a stone palisade round the tower with a double gate and floodlit corner turrets,
    lamp posts inside: no dark corner for mobs to spawn in."""
    x0, x1, z0, z1 = 2, 16, 2, 16
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if (x in (x0, x1) or z in (z0, z1)) and not (z == z0 and 8 <= x <= 10):
                s.set(x, 1, z, MOSSY if (x + z) % 5 == 0 else STONE)
                s.set(x, 2, z, STONE)
                s.set(x, 3, z, slab("stone_brick_slab"))
    # The gate: posts, a lintel, a pair of doors and a lamp each side
    for x in (7, 11):
        s.fill(x, 1, z0, x, 4, z0, DEEP)
        s.set(x, 5, z0, state("lantern", hanging="false", waterlogged="false"))
    s.fill(8, 4, z0, 10, 4, z0, STONE)
    s.door(8, 1, z0, "spruce_door", "north", hinge="left")
    s.door(10, 1, z0, "spruce_door", "north", hinge="right")
    # Corner turrets with floodlights
    for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        s.fill(x, 1, z, x, 4, z, DEEP)
        s.set(x, 5, z, state("sea_lantern"))
    for x, z in ((4, 4), (14, 4), (4, 14), (14, 14)):
        lamp_post(s, x, 1, z)


def gatehouse(s):
    """Tier III: a guard deck over the gate and lookouts on the corner turrets. Two Drone Docks on
    the gate deck for Guard drones (the Guard Module comes with the defence update)."""
    # The deck round the gate lanterns, a hole for the ladder up the west post
    for x in range(6, 13):
        for z in (1, 2, 3):
            if (x, z) not in ((7, 2), (11, 2), (7, 3)):
                s.set(x, 5, z, SPRUCE)
    for x in range(6, 13):
        for z in (1, 3):
            if not (z == 3 and x == 7):
                s.set(x, 6, z, FENCE)
    s.set(6, 6, 2, FENCE)
    s.set(12, 6, 2, FENCE)
    s.set(9, 6, 2, state("sea_lantern"))
    s.set(8, 6, 2, state("cyberfarm:drone_dock", facing="north"))
    s.set(10, 6, 2, state("cyberfarm:drone_dock", facing="north"))
    for y in range(1, 5):
        s.set(7, y, 3, state("ladder", facing="south", waterlogged="false"))
    for x, z in ((2, 2), (16, 2), (2, 16), (16, 16)):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if (dx, dz) != (0, 0) and 0 <= x + dx < 19 and 0 <= z + dz < 19:
                    s.set(x + dx, 5, z + dz, slab("stone_brick_slab", "top"))
        s.set(x, 6, z, state("end_rod", facing="up"))


def cabin(s):
    """The Mechanist's cabin: white walls on a light base under a cyan band, a dark pitched roof with
    a quartz ridge, a spruce porch with a bench, flower boxes, lamp posts on the path, a vent, a
    radio mast, solar panels and a dish; a vegetable garden at the side; inside the Supply Terminal
    (his job site), a bed, a desk and shelves."""
    x0, x1, z0, z1 = 2, 10, 3, 10
    # Floor, porch and path
    s.fill(x0, 0, z0, x1, 0, z1, STONE)
    s.fill(x0 + 1, 0, 1, x1 - 1, 0, 2, SPRUCE)
    s.set(6, 0, 0, PATH)
    s.set(5, 0, 0, GRAVEL)
    s.set(7, 0, 0, GRAVEL)
    lamp_post(s, 4, 1, 0)
    lamp_post(s, 8, 1, 0)
    # Walls with frame posts, a door in a frame portal
    walls(s, x0, z0, x1, z1, 1, 4, band=4)
    s.fill(6, 1, z1, 6, 4, z1, FRAME)
    s.door(6, 1, z0, "spruce_door", "north")
    s.set(6, 3, z0, FRAME)
    s.fill(5, 1, z0, 5, 3, z0, FRAME)
    s.fill(7, 1, z0, 7, 3, z0, FRAME)
    # Windows: front ones with flower boxes, sides and back with sills
    for x in (3, 8):
        window(s, x, 2, z0, "north", sill=False)
    flower_box(s, 3, 1, z0, "north", 1, "potted_red_tulip")
    flower_box(s, 8, 1, z0, "north", 1, "potted_azure_bluet")
    window(s, x0, 2, z0 + 2, "west", width=2)
    window(s, x1, 2, z0 + 2, "east", width=2)
    window(s, 5, 2, z1, "south", width=3)
    # Porch: canopy on spruce posts, a hanging lantern, a bench and pots. The canopy is of bottom
    # slabs, so the posts and the lantern's chain meet it
    for x in range(x0 + 1, x1):
        s.set(x, 4, 1, slab("spruce_slab"))
    for x in (x0 + 1, x1 - 1):
        s.fill(x, 1, 1, x, 3, 1, FENCE)
    s.set(6, 3, 1, state("lantern", hanging="true", waterlogged="false"))
    s.set(4, 1, 2, stairs("spruce_stairs", "south"))
    s.set(8, 1, 2, state("barrel", facing="up", open="false"))
    s.set(8, 2, 2, state("potted_fern"))
    # Roof: pitched, dark, with a quartz ridge; round windows in the gables
    gable_roof(s, x0 - 1, x1 + 1, z0 - 1, z1 + 1, 5)
    for x in (x0, x1):
        s.set(x, 6, 6, PANE)
        s.set(x, 6, 7, PANE)
    # A vent stack through the back slope, a dish on the front one
    s.fill(9, 6, 9, 9, 9, 9, DARK)
    s.set(9, 10, 9, state("iron_trapdoor", facing="north", half="bottom", open="false", powered="false", waterlogged="false"))
    s.set(4, 7, 4, QUARTZ)
    s.set(4, 8, 4, state("end_rod", facing="up"))
    # Radio mast at the back corner
    mx, mz = x1 + 1, z1 + 1
    s.set(mx, 1, mz, FRAME)
    s.fill(mx, 2, mz, mx, 9, mz, POST)
    s.fill(mx, 10, mz, mx, 11, mz, state("end_rod", facing="up"))
    s.set(mx, 8, mz - 1, state("redstone_lamp", lit="false"))
    # Solar panels on a rack by the east wall
    for z in range(z0 + 1, z1 - 1):
        s.set(x1 + 2, 1, z, BARS)
        s.set(x1 + 2, 2, z, SOLAR)
    # A garden at the west side: a vegetable bed, water, a composter, flowers
    s.fill(0, 0, 5, 1, 0, 9, state("farmland", moisture="7"))
    s.set(0, 0, 10, state("water", level="0"))
    for z in range(5, 10):
        s.set(0, 1, z, state("carrots", age="7"))
        s.set(1, 1, z, state("potatoes", age="7"))
    s.set(1, 1, 10, state("composter", level="3"))
    s.set(0, 1, 11, state("water_cauldron", level="3"))
    s.set(1, 1, 11, state("barrel", facing="up", open="false"))
    s.set(1, 2, 11, state("barrel", facing="up", open="false"))
    for (x, z), f in (((0, 2), "poppy"), ((1, 1), "dandelion"), ((0, 1), "azure_bluet")):
        s.set(x, 1, z, state(f))
    # Inside: the terminal on the back wall, a bed, a desk, shelves, a rug, a lamp
    s.set(6, 1, z1 - 1, TERMINAL)
    bed(s, x0 + 1, 1, z0 + 2, "cyan_bed", "south")
    s.set(x0 + 1, 1, z1 - 1, slab("smooth_quartz_slab", "top"))
    s.set(x0 + 2, 1, z1 - 1, slab("smooth_quartz_slab", "top"))
    s.set(x0 + 1, 2, z1 - 1, state("potted_cactus"))
    s.set(x1 - 1, 1, z1 - 1, state("bookshelf"))
    s.set(x1 - 1, 2, z1 - 1, state("bookshelf"))
    s.set(x1 - 1, 1, z1 - 2, state("lectern", facing="west", has_book="false", powered="false"))
    s.set(x1 - 1, 1, z0 + 1, state("chest", facing="west", type="single", waterlogged="false"))
    s.fill(5, 1, 5, 7, 1, 7, state("cyan_carpet"))
    s.set(6, 4, 6, state("lantern", hanging="true", waterlogged="false"))


def outpost_cabin(s):
    """Outpost tier I, found in the world: the cabin with the Supply Terminal (the Mechanist's job site)."""
    cabin(s)


# Each building: its plot and its three tiers, each drawn over the one before
BUILDINGS = {
    "outpost": ((26, 14, 21), [outpost_cabin, supply_depot, command_post]),
    "drone_workshop": ((29, 17, 19), [hangar, wing, repair_hall]),
    "power_station": ((25, 13, 19), [generator_hall, gas_wing, steam_hall]),
    "hydroponic_farm": ((35, 10, 17), [glasshouse, biogas_shed, ranch]),
    "residence": ((36, 15, 15), [house, lodge, clinic]),
    "market_hall": ((29, 12, 26), [market, warehouse, taxi_port]),
    "watch_tower": ((19, 21, 19), [watch_tower, palisade, gatehouse]),
}
