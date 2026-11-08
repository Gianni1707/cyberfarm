"""Writes the test setup, for trying the drones in game: /function cyberfarm:test_setup builds a row of ready stations on a
platform at x 2000, y 100, z 2000 of the overworld and takes you there. Every station has its dock powered, its area or route
set and its drone already on it; run it again to rebuild them all. Run from tools/, like the other generators."""
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/data/cyberfarm/function"
X0, Y, Z0 = 2000, 100, 2000
TASKS = ["harvester", "quarry", "transport", "planter", "logger", "taxi", "rancher", "fertilizer", "builder", "fisher", "breeder", "guard", "bomb", "sponge", "tank"]
LENGTH, DEPTH = 150, 44
next_drone = [1]


def dock(lines, x, z, frame, modules, area=None, route=None, paused=False, tank=False):
    """A powered dock with a drone already on it; (x, z) from the platform's corner. area: (x0, z0, x1, z1) of the clicked corners."""
    ax, az = X0 + x, Z0 + z
    nbt = ['energy:{energy:100000}', f'drone_modules:{sum(1 << TASKS.index(m) for m in modules)}']
    uuid = f"[I;1000,{next_drone[0]},0,0]"
    next_drone[0] += 1
    nbt.append(f"drone:{uuid}")
    if area:
        # As the Survey Tool does it: a block under and two over the corners clicked, which are at the platform's first layer
        x0, z0, x1, z1 = area
        nbt.append(f"work_area:[I;{X0 + x0},{Y - 1},{Z0 + z0},{X0 + x1},{Y + 2},{Z0 + z1}]")
    if route:
        (sx, sz), (dx, dz) = route
        nbt.append(f"transport_source:[I;{X0 + sx},{Y},{Z0 + sz}]")
        nbt.append(f"transport_destination:[I;{X0 + dx},{Y},{Z0 + dz}]")
    if paused:
        nbt.append("paused:1b")
    lines.append(f"setblock {ax} {Y} {az} cyberfarm:drone_dock{{{','.join(nbt)}}}")
    if tank:
        lines.append(f"setblock {ax + 1} {Y} {az} cyberfarm:dock_tank[facing=west]")
    parts = ",".join(f'"cyberfarm:{part}"' for part in [frame, *[f"{m}_module" for m in modules]])
    energy = {"basic_frame": 20000, "advanced_frame": 60000, "elite_frame": 150000}[frame]
    lines.append(f"summon cyberfarm:drone {ax}.5 {Y}.4 {az}.5 {{UUID:{uuid},modules:[{parts}],energy:{energy},dock:[I;{ax},{Y},{az}]}}")


def sign(lines, x, z, *text):
    messages = ",".join(f'"{t}"' for t in (list(text) + ["", "", "", ""])[:4])
    lines.append(f"setblock {X0 + x} {Y} {Z0 + z} minecraft:oak_sign[rotation=8]{{front_text:{{messages:[{messages}]}}}}")


def pool(lines, x0, z0, x1, z1, liquid):
    """A liquid held in the platform's first layer, with a low wall round it."""
    lines.append(f"fill {X0 + x0 - 1} {Y} {Z0 + z0 - 1} {X0 + x1 + 1} {Y} {Z0 + z1 + 1} minecraft:stone_bricks")
    lines.append(f"fill {X0 + x0} {Y} {Z0 + z0} {X0 + x1} {Y} {Z0 + z1} {liquid}")


def sponge_alone():
    lines = []
    sign(lines, 0, 0, "1 Sponge alone:", "liquid thrown", "away on the spot", "")
    pool(lines, 2, 4, 6, 8, "minecraft:water")
    dock(lines, 2, 1, "basic_frame", ["sponge"], area=(2, 4, 6, 8))
    return lines


def sponge_dock_tank():
    lines = []
    sign(lines, 14, 0, "2 Sponge + Tank", "+ Dock Tank: lava", "stored in it, bar", "in dock screen")
    pool(lines, 16, 4, 20, 8, "minecraft:lava")
    dock(lines, 16, 1, "advanced_frame", ["sponge", "tank"], area=(16, 4, 20, 8), tank=True)
    return lines


def sponge_tank_full():
    lines = []
    sign(lines, 28, 0, "3 Sponge + Tank", "no Dock Tank:", "fills up to 32 B", "then it stops")
    pool(lines, 30, 4, 36, 10, "minecraft:water")
    dock(lines, 30, 1, "advanced_frame", ["sponge", "tank"], area=(30, 4, 36, 10))
    return lines


def elite_two_liquids():
    lines = []
    sign(lines, 42, 0, "4 Elite: Sponge", "Tank, Transport", "water + lava:", "one kind a trip")
    pool(lines, 44, 4, 46, 6, "minecraft:water")
    pool(lines, 48, 4, 50, 6, "minecraft:lava")
    dock(lines, 44, 1, "elite_frame", ["sponge", "tank", "transport"], area=(44, 4, 50, 6), tank=True)
    return lines


def liquid_route():
    lines = []
    sign(lines, 56, 0, "5 Transport +", "Tank: water from", "left tank to", "right tank")
    lines.append(f'setblock {X0 + 58} {Y} {Z0 + 5} cyberfarm:liquid_tank{{tank:{{stacks:[{{id:"minecraft:water",amount:32000}}]}}}}')
    lines.append(f"setblock {X0 + 62} {Y} {Z0 + 5} cyberfarm:liquid_tank")
    dock(lines, 60, 1, "advanced_frame", ["transport", "tank"], route=((58, 5), (62, 5)))
    return lines


def item_route():
    lines = []
    sign(lines, 70, 0, "6 Transport:", "left chest to", "right chest", "")
    lines.append(f'setblock {X0 + 72} {Y} {Z0 + 5} minecraft:chest{{Items:[{{Slot:0b,id:"minecraft:wheat",count:64}},{{Slot:1b,id:"minecraft:iron_ingot",count:64}},{{Slot:2b,id:"minecraft:oak_log",count:64}}]}}')
    lines.append(f"setblock {X0 + 76} {Y} {Z0 + 5} minecraft:chest")
    dock(lines, 74, 1, "basic_frame", ["transport"], route=((72, 5), (76, 5)))
    return lines


def dock_route():
    lines = []
    sign(lines, 84, 0, "7 Transport:", "from its own dock", "to the chest: put", "items in the dock")
    lines.append(f"setblock {X0 + 88} {Y} {Z0 + 5} minecraft:chest")
    dock(lines, 86, 1, "basic_frame", ["transport"], route=((86, 1), (88, 5)))
    return lines


def harvest():
    lines = []
    sign(lines, 98, 0, "8 Elite: Harvester", "Planter, Tank:", "cuts, then plants", "the seeds it got")
    lines.append(f"fill {X0 + 100} {Y} {Z0 + 4} {X0 + 106} {Y} {Z0 + 10} minecraft:farmland[moisture=7]")
    lines.append(f"fill {X0 + 100} {Y + 1} {Z0 + 4} {X0 + 106} {Y + 1} {Z0 + 10} minecraft:wheat[age=7]")
    dock(lines, 100, 1, "elite_frame", ["harvester", "planter", "tank"], area=(100, 4, 106, 10))
    return lines


def parked_combinations():
    lines = []
    sign(lines, 112, 0, "9 Elite on hold:", "every module with", "the Tank, to look", "for overlaps")
    for i, modules in enumerate((["fertilizer", "guard"], ["logger", "rancher"], ["fisher", "breeder"], ["planter", "quarry"], ["taxi", "sponge"])):
        dock(lines, 114 + i * 6, 1, "elite_frame", [*modules, "tank"], paused=True)
    return lines


def holograms():
    lines = []
    sign(lines, 40, 18, "Hologram Table", "3x3, 2x2 and 1x1", "right-click: range", "")
    for dx in range(3):
        for dz in range(3):
            lines.append(f"setblock {X0 + 42 + dx} {Y} {Z0 + 20 + dz} cyberfarm:hologram_table")
    for dx in range(2):
        for dz in range(2):
            lines.append(f"setblock {X0 + 52 + dx} {Y} {Z0 + 20 + dz} cyberfarm:hologram_table")
    lines.append(f"setblock {X0 + 60} {Y} {Z0 + 20} cyberfarm:hologram_table")
    return lines


def machines():
    lines = []
    sign(lines, 0, 18, "Look at each", "machine: its panel", "shows over it", "")
    x = 2
    for block in ("combustion_generator", "drone_assembler", "crystallizer", "muffler", "liquid_tank", "water_pump", "laser_turret"):
        lines.append(f"setblock {X0 + x} {Y} {Z0 + 20} cyberfarm:{block}")
        x += 3
    # Frames on top: these fold out together
    for block in ("biomass_press", "waste_reprocessor"):
        lines.append(f"setblock {X0 + x} {Y} {Z0 + 20} cyberfarm:{block}")
        lines.append(f"setblock {X0 + x} {Y + 1} {Z0 + 20} cyberfarm:cyber_frame")
        x += 3
    return lines


STATIONS = [sponge_alone, sponge_dock_tank, sponge_tank_full, elite_two_liquids, liquid_route, item_route, dock_route, harvest,
            parked_combinations, holograms, machines]


def main():
    area = f"x={X0 - 2},y={Y - 5},z={Z0 - 12},dx={LENGTH + 4},dy=40,dz={DEPTH + 14}"
    start = [
        "# The stations need their chunks loaded, so the building waits for that",
        f"execute in minecraft:overworld run forceload add {X0 - 2} {Z0 - 12} {X0 + LENGTH} {Z0 + DEPTH}",
        f"execute in minecraft:overworld run kill @e[type=cyberfarm:drone,{area}]",
        f"execute in minecraft:overworld run tp @s {X0 + 8}.5 {Y + 2} {Z0 - 8}.5 0 15",
        "gamemode creative @s",
        "give @s cyberfarm:survey_tool",
        "give @s cyberfarm:dock_tank",
        "give @s cyberfarm:hologram_table 9",
        "give @s cyberfarm:sponge_module 4",
        "give @s cyberfarm:tank_module 4",
        "schedule function cyberfarm:test_setup/build 3s",
    ]
    build = [f"kill @e[type=item,{area}]"]
    # Cleared and floored in strips: a single fill may not change more than 32768 blocks
    for x in range(X0 - 2, X0 + LENGTH, 16):
        build.append(f"fill {x} {Y} {Z0 - 12} {min(x + 15, X0 + LENGTH)} {Y + 12} {Z0 + DEPTH} minecraft:air")
        build.append(f"fill {x} {Y - 1} {Z0 - 12} {min(x + 15, X0 + LENGTH)} {Y - 1} {Z0 + DEPTH} minecraft:smooth_stone")
    # One function per station: one that fails to load leaves the others working
    for station in STATIONS:
        build.append(f"function cyberfarm:test_setup/{station.__name__}")
    build.append(f"forceload remove {X0 - 2} {Z0 - 12} {X0 + LENGTH} {Z0 + DEPTH}")
    build.append('tellraw @a {"text":"CyberFarm test setup ready: the signs say what each station shows","color":"green"}')

    folder = OUT / "test_setup"
    folder.mkdir(parents=True, exist_ok=True)
    (OUT / "test_setup.mcfunction").write_text("\n".join(start) + "\n")
    (folder / "build.mcfunction").write_text("\n".join(build) + "\n")
    for station in STATIONS:
        (folder / f"{station.__name__}.mcfunction").write_text("\n".join(station()) + "\n")
    print(f"wrote the test setup, {len(STATIONS)} stations")


if __name__ == "__main__":
    main()
