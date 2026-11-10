"""Writes the showcase, a plaza with everything in CyberFarm, as a data pack of one dev world (nothing of it ships in the mod).

    python showcase.py [world folder under run/saves; default: the last one played]

Then in game: /reload the first time, and /function showcase:go, which also rebuilds it. It stands on a platform at
x 3000, y 200, z 3000 of the overworld. The list of what must be there comes from the registries: this script stops if a block,
item or entity is neither shown nor explained in EXPLAINED. In game, showcase:check says in chat what did not assemble.
Machines stand facing south, towards the aisle in front of them; x runs west to east, z north to south.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
REGISTRY = ROOT / "src/main/java/com/cyberfarm/registry"
LANG = json.loads((ROOT / "src/main/resources/assets/cyberfarm/lang/en_us.json").read_text(encoding="utf-8"))
X0, Y, Z0 = 3000, 200, 3000
WIDTH, DEPTH = 96, 92
AISLE = 6
CLEAR = 20  # Blocks of air over the floor: the tallest structure is the large reactor, 15

EXPLAINED = {
    "machine_part": "never placed: its states only name the models of the parts that move in an opening machine",
    "hidden_frame": "invisible: what a Cyber Frame becomes inside an assembled machine, so it is in every one of them",
}

TASKS = ["harvester", "quarry", "transport", "planter", "logger", "taxi", "rancher", "fertilizer", "builder", "fisher", "breeder", "guard", "bomb", "sponge", "tank"]
FRAME_ENERGY = {"basic_frame": 20000, "advanced_frame": 60000, "elite_frame": 150000, "mini_frame": 10000}
SMALL_MACHINES = {  # Where the Cyber Frames go around each machine: (dx, dy, dz), see the machines' frameCells
    "steam_engine": [(0, 0, -1), (0, 0, -2), (0, 0, -3), (0, 0, -4)],
    "gas_turbine": [(0, 0, 1), (0, 0, -1)],
    "coolant_mixer": [(1, 0, 0)],
    "acid_synthesizer": [(-1, 0, 0), (1, 0, 0), (-1, 1, 0), (1, 1, 0), (0, 1, 0)],
    "biomass_press": [(0, 1, 0)],
    "bio_digester": [(0, 1, 0), (-1, 0, 0)],
    "fabricator": [(0, 0, -1)],
    "pyrolysis_oven": [(-1, 0, 0)],
    "electrolyzer": [(-1, 0, 0)],
    "waste_reprocessor": [(0, 1, 0)],
    "transmitter_dish": [(1, 0, 0), (0, 0, -1), (1, 0, -1)],
    "receiver_dish": [(0, 1, 0)],
}
WALL = {
    "Raw materials": ["raw_uranium", "uranium_ingot", "raw_bauxite", "alumina", "aluminum_ingot", "sulfur", "biomass", "carbon_fiber"],
    "Circuits and parts": ["basic_circuit", "advanced_circuit", "elite_circuit", "drone_core", "rotor", "battery_cell", "isotope_shard", "fusion_crystal", "mech_core"],
    "Nuclear": ["fuel_rod", "depleted_fuel_rod", "hazmat_helmet", "hazmat_suit", "hazmat_pants", "hazmat_boots", "geiger_counter"],
    "Buckets": ["biogas_bucket", "methane_bucket", "steam_bucket", "coolant_bucket", "cryo_coolant_bucket", "acid_bucket"],
    "Tools": ["survey_tool", "multiblock_projector", "blueprint_disc", "mob_capsule", "drone"],
    "Drone frames": ["basic_frame", "advanced_frame", "elite_frame", "mini_frame"],
    "Drone modules": [f"{task}_module" for task in TASKS],
    "Drone addons": ["speed_addon", "stack_addon", "battery_addon", "fortune_addon", "silk_touch_addon", "anchor_addon", "efficiency_addon"],
    "Range modules": ["range_module_i", "range_module_ii", "range_module_iii", "infinity_range_module"],
    "Mech": ["mech_frame", "mech_jet_module", "mech_drill_module", "mech_blade_module", "mech_armor_module"],
}

commands = []
checks = []
placed = {}
shown = set()
drones = [0]


def block(x, y, z, name, state="", nbt=""):
    assert 0 <= x < WIDTH and 0 <= z < DEPTH, (x, z, name)
    assert (x, y, z) not in placed, f"{name} on {placed.get((x, y, z))} at {x} {y} {z}"
    placed[(x, y, z)] = name
    shown.add(name)
    commands.append(f"setblock {X0 + x} {Y + y} {Z0 + z} cyberfarm:{name}{state}{nbt}")


def machine(x, y, z, name, nbt=""):
    block(x, y, z, name, "[facing=south]", nbt)


def fill(x0, y0, z0, x1, y1, z1, what):
    commands.append(f"fill {X0 + x0} {Y + y0} {Z0 + z0} {X0 + x1} {Y + y1} {Z0 + z1} {what}")


def summon(entity, x, y, z, nbt):
    commands.append(f"summon {entity} {X0 + x} {Y + y} {Z0 + z} {nbt}")


def check(x, y, z, expected, what):
    checks.append(f'execute unless block {X0 + x} {Y + y} {Z0 + z} cyberfarm:{expected} run tellraw @a {{text:"Showcase: {what} did not assemble",color:"red"}}')


# ------------------------------------------------------------------ labels

def tr(key, **style):
    assert key in LANG, f"no translation for {key}"
    return {"translate": key, **style}


def block_name(name):
    return tr(f"block.cyberfarm.{name}")


def item_name(name):
    return tr(f"item.cyberfarm.{name}")


def snbt(value):
    """A text component as SNBT; only strings, dicts and lists of them."""
    if isinstance(value, dict):
        return "{" + ",".join(f"{key}:{snbt(item)}" for key, item in value.items()) + "}"
    if isinstance(value, list):
        return "[" + ",".join(snbt(item) for item in value) + "]"
    return json.dumps(value)


def text(x, y, z, component, scale=0.6, facing="vertical", width=130, background=0xB0101418 - 2**32):
    """Billboard text by default; "north" stands it on a wall facing north, "floor" lays it on the ground, read from the south."""
    rotation = {"north": "[180f,0f]", "floor": "[0f,-90f]"}.get(facing, "[0f,0f]")
    billboard = "vertical" if facing == "vertical" else "fixed"
    summon("text_display", x, y, z, "{" + f'text:{snbt(component)},billboard:"{billboard}",Rotation:{rotation},line_width:{width},background:{background},'
           f"see_through:0b,brightness:{{sky:15,block:15}},transformation:{{translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],"
           f"right_rotation:[0f,0f,0f,1f],scale:[{scale}f,{scale}f,{scale}f]}}" + "}")


# ------------------------------------------------------------------ the big structures, each from its left edge and front z
# shortcut: the layouts are copied from Blueprint, TowerSize and TurbineSize; if those change, showcase:check says which fell apart.

def tower(x, front, rings, size):
    base = rings[0]
    cx, floor = x + base, front - base
    for y, ring in enumerate(rings):
        for dx in range(-base, base + 1):
            for dz in range(-base, base + 1):
                if y == 0 or max(abs(dx), abs(dz)) == ring:
                    is_controller = (y, dx, dz) == (1, 0, base)
                    if is_controller:
                        machine(cx, 1, front, "reactor_controller")
                    else:
                        block(cx + dx, y, floor + dz, "reactor_port" if (y, dx, dz) == (1, 1, base) else "reactor_casing")
    check(cx, 1, front, f"reactor_controller[formed=true,size={size}]", f"Reactor size {size}")
    text(cx + 0.5, 0.1, front + 1.5, {**tr("projector.cyberfarm.reactor"), "extra": [{"text": " "}, tr(f"gui.cyberfarm.reactor.size.{['small', 'medium', 'large'][size]}")]}, 1.0)


def hall(x, front, half, length, size):
    cx = x + half
    for along in range(length):
        for right in range(-half, half + 1):
            for up in range(-half, half + 1):
                if along in (0, length - 1) or max(abs(right), abs(up)) == half:
                    if (along, right, up) == (0, 0, 0):
                        machine(cx, half, front, "turbine_controller")
                    else:
                        block(cx + right, half + up, front - along, "turbine_port" if (along, right, up) == (0, 1, 0) else "turbine_casing")
    check(cx, half, front, f"turbine_controller[formed=true,size={size}]", f"Turbine size {size}")
    text(cx + 0.5, 0.1, front + 1.5, {**tr("projector.cyberfarm.turbine"), "extra": [{"text": " "}, tr(f"gui.cyberfarm.reactor.size.{['small', 'medium', 'large'][size]}")]}, 1.0)


def chamber(x, front):
    """The Methane Extractor on top of its chamber; the Intake Gate on the front, where it can be seen."""
    cx, cz = x + 1, front - 1
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            centre, corner = dx == dz == 0, dx != 0 and dz != 0
            block(cx + dx, 0, cz + dz, "cyber_frame")
            for y in (1, 2):
                if corner:
                    block(cx + dx, y, cz + dz, "cyber_frame")
                elif not centre:
                    block(cx + dx, y, cz + dz, "intake_gate" if (y, dx, dz) == (1, 0, 1) else "containment_glass")
            if centre:
                machine(cx, 3, cz, "methane_extractor")
            else:
                block(cx + dx, 3, cz + dz, "acid_inlet" if (dx, dz) == (-1, 0) else "methane_outlet" if (dx, dz) == (1, 0) else "cyber_frame")
    check(cx, 3, cz, "methane_extractor[formed=true]", "Methane Extractor")
    text(cx + 0.5, 0.1, front + 1.5, tr("projector.cyberfarm.methane_chamber"), 1.0)


def crystallizer(x, front):
    cx, cz = x + 1, front - 1
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            centre, corner = dx == dz == 0, dx != 0 and dz != 0
            if (dx, dz) == (0, 1):
                machine(cx, 0, front, "crystallizer")
            else:
                block(cx + dx, 0, cz + dz, "crystallizer_port" if (dx, dz) == (0, -1) else "cyber_frame")
            for y in (1, 2):
                if corner:
                    block(cx + dx, y, cz + dz, "cyber_frame")
                elif not centre:
                    block(cx + dx, y, cz + dz, "containment_glass")
            block(cx + dx, 3, cz + dz, "cyber_frame")
    check(cx, 0, front, "crystallizer[formed=true]", "Crystallizer")
    text(cx + 0.5, 0.1, front + 1.5, tr("projector.cyberfarm.crystallizer"), 1.0)


def hangar(x, front):
    """Rails, posts, beam and arms round the pad, with a mech wearing all its modules on it."""
    for w in range(5):
        for d in range(5):
            rail = w in (0, 4)
            if (w, d) == (0, 0):
                machine(x, 0, front, "mech_hangar")
            elif rail:
                block(x + w, 0, front - d, "hangar_frame")
            elif 1 <= d <= 3:
                block(x + w, 0, front - d, "hangar_pad")
            if rail and d == 4:
                for y in range(1, 6):
                    block(x + w, y, front - d, "hangar_frame")
            elif (not rail and d == 4) or (w in (1, 3) and d in (2, 3)):
                block(x + w, 5, front - d, "hangar_frame")
    check(x, 0, front, "mech_hangar[formed=true]", "Mech Hangar")
    summon("cyberfarm:mech", x + 2.5, 1, front - 1.5, "{modules:15,energy:100000,Rotation:[0f,0f]}")
    # A mech never dies, not even to /kill, so a rebuild must not summon a second one
    commands[-1] = f"execute unless entity @e[type=cyberfarm:mech,x={X0},y={Y - 2},z={Z0},dx={WIDTH},dy={CLEAR + 2},dz={DEPTH}] run {commands[-1]}"
    shown.add("mech")
    text(x + 2.5, 0.1, front + 1.5, tr("projector.cyberfarm.mech_hangar"), 1.0)


def core(x, front, tier):
    """A Resonance Core with its shell (none at tier 1) and its pylons at their closest, as many and as tall as the tier wants."""
    radius, distance = tier - 1, tier + 2
    cx = x + distance
    cz = front - distance if tier >= 3 else front - radius
    machine(cx, 0, cz, "resonance_core")
    for dx in range(-radius, radius + 1):
        for dy in range(-radius, radius + 1):
            for dz in range(-radius, radius + 1):
                if radius > 0 and max(abs(dx), abs(dy), abs(dz)) == radius:
                    block(cx + dx, 1 + radius + dy, cz + dz, "resonance_cell")
    for (dx, dz) in [(1, 0), (-1, 0), (0, 1), (0, -1)][:tier]:
        for y in range(tier + 1):
            block(cx + dx * distance, y, cz + dz * distance, "stabilizer_pylon")
    if radius:
        check(cx, 1, cz, "resonance_cell[formed=true]", f"Resonance Core tier {tier}")
    size = [{"text": f" tier {tier}"}] if tier == 1 else [{"text": " "}, tr(f"projector.cyberfarm.size.tier_{tier}")]
    text(cx + 0.5, 0.1, front + 1.5, {**tr("projector.cyberfarm.resonance_core"), "extra": size}, 1.0)


# ------------------------------------------------------------------ the rows of the plaza

def row(title, front, depth, cells, gap=0):
    """A dark bay with its title on the floor in front, and the cells (width, function of the left x) centred on it."""
    fill(1, -1, front - depth + 1, WIDTH - 2, -1, front + 1, "minecraft:polished_deepslate")
    fill(1, -1, front + 2, WIDTH - 2, -1, front + 2, "minecraft:cyan_concrete")
    text(WIDTH / 2, 0.02, front + 4, {"text": title, "color": "aqua", "bold": True}, 1.6, "floor", 600, 0)
    x = (WIDTH - sum(width for width, _ in cells) - gap * (len(cells) - 1)) // 2
    assert x >= 1, f"{title} is too wide"
    for width, draw in cells:
        draw(x)
        x += width + gap
    return front + AISLE


def assembled(name):
    """A small multiblock with its frames: it opens by itself when they are there."""
    def draw(x):
        cx, z = x + 1, ROW_FRONT[0] - 1
        machine(cx, 0, z, name)
        for dx, dy, dz in SMALL_MACHINES[name]:
            block(cx + dx, dy, z + dz, "cyber_frame")
        check(cx, 0, z, f"{name}[stage=2]", name)
        text(cx + 0.5, 0.1, z + 2.5, block_name(name))
    return 3, draw


def single(name, *, facing=True, width=3):
    def draw(x):
        z = ROW_FRONT[0]
        (machine if facing else block)(x + 1, 0, z, name)
        text(x + 1.5, 0.1, z + 1.4, block_name(name), 0.5)
    return width, draw


def docked(frame, modules, mini=False, tank=False):
    """A parked drone on its dock, so it stays there. A mini drone on a Swarm Dock: four of them."""
    def draw(x):
        z = ROW_FRONT[0]
        cx = x + 1
        bits = sum(1 << TASKS.index(module) for module in modules)
        uuids = []
        for _ in range(4 if mini else 1):
            drones[0] += 1
            uuids.append(f"[I;5000,{drones[0]},0,0]")
        held = f"drones:[{','.join(uuids)}]" if mini else f"drone:{uuids[0]}"
        machine(cx, 0, z, "swarm_dock" if mini else "drone_dock", f"{{energy:{{energy:100000}},drone_modules:{bits},{held},paused:1b}}")
        if tank:
            block(cx + 1, 0, z, "dock_tank", "[facing=west]")
        parts = ",".join(f'"cyberfarm:{part}"' for part in [frame, *[f"{module}_module" for module in modules]])
        for i, uuid in enumerate(uuids):
            summon("cyberfarm:drone", cx + 0.5, 0.4, z + 0.5, f"{{UUID:{uuid},modules:[{parts}],energy:{FRAME_ENERGY[frame]},dock:[I;{X0 + cx},{Y},{Z0 + z}]}}")
            checks.append(f'execute unless entity @e[type=cyberfarm:drone,x={X0 + cx}.5,y={Y},z={Z0 + z}.5,distance=..3] run tellraw @a {{text:"Showcase: the drone of {frame} {" ".join(modules)} left its dock",color:"red"}}')
        shown.add("drone")
        label = item_name(f"{modules[0]}_module") if frame == "basic_frame" else item_name(frame)
        text(cx + 0.5, 0.1, z + 1.4, label, 0.45)
    return 4 if tank else 3, draw


def tank(kind, fluid):
    def draw(x):
        z = ROW_FRONT[0]
        block(x + 1, 0, z, kind, nbt=f'{{tank:{{stacks:[{{id:"{fluid}",amount:24000}}]}}}}')
        path = fluid.split(":")[1]
        name = tr(f"fluid.cyberfarm.{path}") if fluid.startswith("cyberfarm") else {"text": path.capitalize()}
        text(x + 1.5, 0.1, z + 1.4, {**block_name(kind), "extra": [{"text": " "}, name]}, 0.45)
    return 3, draw


def pipe(name):
    def draw(x):
        z = ROW_FRONT[0]
        for dx in range(3):
            block(x + dx, 0, z, name)
        text(x + 1.5, 0.1, z + 1.4, block_name(name), 0.45)
    return 4, draw


def pool(name):
    """A source block in each cell of a basin sunk in the floor, which holds it."""
    def draw(x):
        z = ROW_FRONT[0]
        fill(x, -1, z - 2, x + 2, -1, z, f"cyberfarm:{name}")
        shown.add(name)
        text(x + 1.5, 0.1, z + 1.4, block_name(name), 0.45)
    return 4, draw


def villager(draw_machine):
    """The Supply Terminal with the Drone Mechanist working at it."""
    width, draw = draw_machine

    def with_villager(x):
        draw(x)
        summon("minecraft:villager", x + 2.5, 0, ROW_FRONT[0] + 0.5, '{VillagerData:{profession:"cyberfarm:drone_mechanist",level:1,type:"minecraft:plains"},NoAI:1b,Invulnerable:1b,PersistenceRequired:1b,Rotation:[0f,0f]}')
    return width, with_villager


def hologram(x):
    z = ROW_FRONT[0]
    for dx in range(3):
        for dz in range(3):
            block(x + dx, 0, z - dz, "hologram_table")
    text(x + 1.5, 0.1, z + 1.4, block_name("hologram_table"))


def wall():
    """The back of the plaza: every item in a glow frame that cannot be taken, by subject, with its name under it."""
    z = DEPTH - 1
    fill(0, 0, z, WIDTH - 1, 5, z, "minecraft:polished_deepslate")
    fill(0, 5, z, WIDTH - 1, 5, z, "minecraft:cyan_concrete")
    columns = sum((len(items) + 1) // 2 for items in WALL.values())
    x = (WIDTH - (columns * 2 + (len(WALL) - 1) * 2)) // 2
    assert x >= 0, "the wall is too short"
    # Seen from the plaza the wall runs from east to west
    x = WIDTH - 1 - x
    for title, items in WALL.items():
        columns = (len(items) + 1) // 2
        text(x - columns + 1.5, 4.05, z - 0.05, {"text": title, "color": "aqua", "bold": True}, 0.9, "north", 600, 0)
        for i, item in enumerate(items):
            fx, fy = x - (i // 2) * 2, 3 if i % 2 == 0 else 1
            summon("minecraft:glow_item_frame", fx + 0.5, fy + 0.5, z - 0.5, f'{{Facing:2b,block_pos:[I;{X0 + fx},{Y + fy},{Z0 + z - 1}],Fixed:1b,Invulnerable:1b,Item:{{id:"cyberfarm:{item}",count:1}}}}')
            text(fx + 0.5, fy - 0.32, z - 0.05, item_name(item), 0.32, "north", 110)
            shown.add(item)
        x -= columns * 2 + 2
    summon("minecraft:armor_stand", WIDTH / 2, 0, z - 4.5, '{Rotation:[180f,0f],ShowArms:1b,Invulnerable:1b,equipment:{head:{id:"cyberfarm:hazmat_helmet",count:1},chest:{id:"cyberfarm:hazmat_suit",count:1},legs:{id:"cyberfarm:hazmat_pants",count:1},feet:{id:"cyberfarm:hazmat_boots",count:1}}}')


ROW_FRONT = [0]


def plaza():
    fill(0, -2, 0, WIDTH - 1, -1, DEPTH - 1, "minecraft:light_gray_concrete")
    for x0, z0, x1, z1 in ((0, 0, WIDTH - 1, 0), (0, DEPTH - 1, WIDTH - 1, DEPTH - 1), (0, 0, 0, DEPTH - 1), (WIDTH - 1, 0, WIDTH - 1, DEPTH - 1)):
        fill(x0, -1, z0, x1, -1, z1, "minecraft:cyan_concrete")

    def next_row(title, depth, cells, gap=0):
        ROW_FRONT[0] += depth
        row(title, ROW_FRONT[0], depth, cells, gap)
        ROW_FRONT[0] += AISLE

    ROW_FRONT[0] = 1
    next_row("TOWERS AND ENERGY STORAGE", 13, [
        (9, lambda x: core(x, ROW_FRONT[0], 2)), (11, lambda x: core(x, ROW_FRONT[0], 3)), (13, lambda x: core(x, ROW_FRONT[0], 4)), (4, lambda x: core(x - 3, ROW_FRONT[0], 1)),
        (5, lambda x: tower(x, ROW_FRONT[0], [2, 2, 2, 1, 1, 1, 2], 0)),
        (7, lambda x: tower(x, ROW_FRONT[0], [3, 3, 3, 3, 2, 2, 2, 2, 2, 3, 3], 1)),
        (9, lambda x: tower(x, ROW_FRONT[0], [4, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4], 2)),
    ], 4)
    next_row("TURBINES, CHAMBERS AND THE HANGAR", 8, [
        (3, lambda x: hall(x, ROW_FRONT[0], 1, 4, 0)), (5, lambda x: hall(x, ROW_FRONT[0], 2, 6, 1)), (7, lambda x: hall(x, ROW_FRONT[0], 3, 8, 2)),
        (3, lambda x: chamber(x, ROW_FRONT[0])), (3, lambda x: crystallizer(x, ROW_FRONT[0])), (5, lambda x: hangar(x, ROW_FRONT[0])),
    ], 7)
    next_row("ASSEMBLED MACHINES", 6, [assembled(name) for name in SMALL_MACHINES], 3)
    next_row("MACHINES AND DEFENCE", 3, [
        single("combustion_generator"), single("water_pump"), single("muffler"), single("drone_assembler"),
        villager(single("supply_terminal", width=4)), single("laser_turret"), (3, hologram), single("hologram_table", facing=False),
    ], 2)
    next_row("DRONES AND MODULES", 1, [docked("basic_frame", [task]) for task in TASKS] + [
        docked("advanced_frame", ["harvester", "planter"]), docked("elite_frame", ["sponge", "tank", "transport"], tank=True),
        docked("mini_frame", ["builder"], mini=True),
    ])
    next_row("TANKS, PIPES AND CABLES", 3, [
        tank("liquid_tank", "minecraft:water"), tank("liquid_tank", "minecraft:lava"), tank("liquid_tank", "cyberfarm:coolant"),
        tank("liquid_tank", "cyberfarm:cryo_coolant"), tank("liquid_tank", "cyberfarm:acid"),
        tank("gas_tank", "cyberfarm:biogas"), tank("gas_tank", "cyberfarm:methane"), tank("gas_tank", "cyberfarm:steam"),
        pool("coolant"), pool("cryo_coolant"),
        *[pipe(f"{tier}{kind}") for kind in ("energy_conduit", "gas_pipe", "liquid_pipe") for tier in ("", "advanced_", "elite_")],
    ])
    next_row("BEFORE THEY ARE ASSEMBLED", 1, [single(name) for name in SMALL_MACHINES] + [
        single(name) for name in ("reactor_controller", "turbine_controller", "methane_extractor", "crystallizer", "mech_hangar", "resonance_core")
    ], 1)
    next_row("PARTS AND ORES", 1, [
        single(name, facing=False) for name in (
            "cyber_frame", "reactor_casing", "reactor_port", "turbine_casing", "turbine_port", "containment_glass", "acid_inlet", "methane_outlet",
            "intake_gate", "crystallizer_port", "hangar_frame", "hangar_pad", "resonance_cell", "stabilizer_pylon",
            "uranium_ore", "deepslate_uranium_ore", "bauxite_ore", "deepslate_bauxite_ore", "nether_sulfur_ore", "vitrified_waste")
    ], 1)
    assert ROW_FRONT[0] + 6 <= DEPTH, ROW_FRONT[0]
    wall()
    # Light at the crossings of a grid, under what stands on the floor too, so no mob spawns
    for x in range(4, WIDTH - 1, 8):
        for z in range(4, DEPTH - 1, 8):
            commands.append(f"execute unless block {X0 + x} {Y - 1} {Z0 + z} cyberfarm:coolant unless block {X0 + x} {Y - 1} {Z0 + z} cyberfarm:cryo_coolant run setblock {X0 + x} {Y - 1} {Z0 + z} minecraft:sea_lantern")


# ------------------------------------------------------------------ coverage and output

def registered():
    def read(name):
        return (REGISTRY / name).read_text(encoding="utf-8")
    blocks = re.findall(r'register(?:Simple)?Block\(\s*"(\w+)"', read("ModBlocks.java"))
    items = re.findall(r'ITEMS\.register(?:Simple)?Item\(\s*"(\w+)"', read("ModItems.java"))
    block_items = [name.lower() for name in re.findall(r"registerSimpleBlockItem\(ModBlocks\.(\w+)", read("ModItems.java"))]
    assert set(block_items) <= set(blocks), set(block_items) - set(blocks)
    entities = re.findall(r'registerEntityType\(\s*"(\w+)"', read("ModEntities.java"))
    assert len(blocks) > 50 and len(items) > 50 and entities, "the registries have changed shape"
    return blocks, items, entities


def covered():
    """Stops with the list of what is neither in the plaza nor explained."""
    blocks, items, entities = registered()
    everything = set(blocks) | set(items) | set(entities)
    assert set(EXPLAINED) <= everything, set(EXPLAINED) - everything
    missing = sorted(everything - shown - set(EXPLAINED))
    if missing:
        sys.exit("Not in the plaza and not explained: " + ", ".join(missing))
    print(f"All {len(blocks)} blocks, {len(items)} items that are not blocks and {len(entities)} entities are there. Left out on purpose:")
    for name, why in EXPLAINED.items():
        print(f"  {name}: {why}")


def write(world):
    pack = ROOT / "run/saves" / world / "datapacks/cyberfarm_showcase"
    (pack / "data/showcase/function").mkdir(parents=True, exist_ok=True)
    (pack / "pack.mcmeta").write_text(json.dumps({"pack": {"description": "CyberFarm showcase", "min_format": 101, "max_format": 101}}), encoding="utf-8")
    x0, z0, x1, z1 = X0, Z0, X0 + WIDTH - 1, Z0 + DEPTH - 1
    box = f"x={x0},y={Y - 2},z={z0},dx={WIDTH},dy={CLEAR + 2},dz={DEPTH}"
    functions = {
        "go": [
            f"execute in minecraft:overworld run forceload add {x0} {z0} {x1} {z1}",
            f"execute in minecraft:overworld run tp @s {X0 + WIDTH / 2} {Y} {z1 - 6.5} 180 5",
            "gamemode creative @s",
            'tellraw @a {text:"Building the CyberFarm showcase...",color:"aqua"}',
            "schedule function showcase:clear 3s",
        ],
        # Drones keep their UUIDs from a build to the next, so the old ones must be gone a tick before
        "clear": [f"kill @e[type=!player,{box}]",
                  *[f"fill {x0} {Y} {z} {x1} {Y + CLEAR} {min(z + 7, z1)} minecraft:air" for z in range(z0, z1 + 1, 8)],
                  "schedule function showcase:build 1s"],
        "build": [*commands, 'tellraw @a {text:"CyberFarm showcase ready: machines ahead, items on the wall behind you",color:"green"}', "schedule function showcase:check 20s"],
        "check": [*checks, f'execute in minecraft:overworld run forceload remove {x0} {z0} {x1} {z1}', f'tellraw @a {{text:"Showcase checked: {len(checks)} checks, a line in red for each that failed",color:"aqua"}}'],
    }
    for name, lines in functions.items():
        (pack / f"data/showcase/function/{name}.mcfunction").write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")
    print(f"{sum(len(lines) for lines in functions.values())} commands written to {pack}")


def main():
    if len(sys.argv) > 1:
        world = sys.argv[1]
    else:
        world = max((ROOT / "run/saves").iterdir(), key=lambda path: (path / "session.lock").stat().st_mtime).name
    plaza()
    covered()
    write(world)


main()
