"""Writes CyberFarm's JSON assets and data: blockstates, models, item definitions,
loot tables, tags and recipes. Translations (lang/en_us.json) are written by hand.

Run from the project root:  python tools/assetgen.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets/cyberfarm"
DATA = RES / "data/cyberfarm"
NS = "cyberfarm"

FACING_ROTATION = {"north": 0, "east": 90, "south": 180, "west": 270}

# Items drawn as flat sprites (textures/item/<name>.png)
FLAT_ITEMS = [
    "drone_core", "rotor", "battery_cell",
    "basic_frame", "advanced_frame", "elite_frame", "mini_frame",
    "harvester_module", "quarry_module", "transport_module", "planter_module", "logger_module", "taxi_module", "rancher_module", "fertilizer_module", "builder_module", "fisher_module", "breeder_module", "guard_module", "bomb_module",
    "speed_addon", "stack_addon", "battery_addon", "fortune_addon", "silk_touch_addon", "anchor_addon",
    "raw_bauxite", "alumina", "aluminum_ingot", "carbon_fiber", "basic_circuit", "advanced_circuit", "elite_circuit",
    "drone", "mini_drone", "survey_tool", "multiblock_projector", "blueprint_disc", "mob_capsule_filled",
    "raw_uranium", "uranium_ingot", "fuel_rod", "depleted_fuel_rod",
    "biogas_bucket", "methane_bucket", "steam_bucket", "coolant_bucket", "cryo_coolant_bucket",
    "hazmat_helmet", "hazmat_suit", "hazmat_pants", "hazmat_boots", "geiger_counter",
    "sulfur", "acid_bucket", "mech_frame", "isotope_shard", "fusion_crystal", "mech_core",
    "mech_jet_module", "mech_drill_module", "mech_blade_module", "mech_armor_module",
]


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def tex(name):
    return f"{NS}:block/{name}"


# --------------------------------------------------------------------------- blocks

def orientable(name, front, top, side="machine_side", bottom="machine_bottom"):
    write(ASSETS / f"models/block/{name}.json", {
        "parent": "minecraft:block/orientable_with_bottom",
        "textures": {"front": tex(front), "top": tex(top), "side": tex(side), "bottom": tex(bottom)},
    })


def lit_machine(name, top, side="machine_side", bottom="machine_bottom"):
    """A machine that lights up while it works: its model off, and on with the animated front and,
    over it and its sides, a skin of the same lights that glows in the dark."""
    orientable(name, f"{name}_front", top, side, bottom)
    # Explicit UVs: worked out from the overlay's coordinates they would run off the texture
    glow = {d: {"uv": [0, 0, 16, 16], "texture": "#side_glow", "cullface": d} for d in ("east", "south", "west")}
    write(ASSETS / f"models/block/{name}_on.json", {
        "parent": "minecraft:block/orientable_with_bottom",
        "render_type": "minecraft:cutout",
        "textures": {"front": tex(f"{name}_front_on"), "top": tex(top), "side": tex(side), "bottom": tex(bottom),
                     "front_glow": tex(f"{name}_front_glow"), "side_glow": tex(f"{side}_glow")},
        "elements": [
            {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
                "down": {"texture": "#bottom", "cullface": "down"}, "up": {"texture": "#top", "cullface": "up"},
                "north": {"texture": "#front", "cullface": "north"}, "south": {"texture": "#side", "cullface": "south"},
                "west": {"texture": "#side", "cullface": "west"}, "east": {"texture": "#side", "cullface": "east"}}},
            # A hair outside the cube so it never fights with the faces under it
            {"from": [-0.01, -0.01, -0.01], "to": [16.01, 16.01, 16.01], "shade": False, "light_emission": 15,
             "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#front_glow", "cullface": "north"}, **glow}},
        ],
    })
    facing_blockstate(name, {"lit=false": name, "lit=true": f"{name}_on"})


def assembler_models():
    """The Drone Assembler as an open chamber, so the drone it makes shows inside: a low base with
    the control panel and the assembly pad, a steel pillar on each corner and a cap with the emitter
    looking down. Working, the pillars' light lines, the emitter and the panel glow in the dark."""
    sides = ("north", "east", "south", "west")
    pillars = [(x, z) for x in (0, 14) for z in (0, 14)]

    def elements(on):
        base = {"from": [0, 0, 0], "to": [16, 4, 16], "faces": {
            "north": {"uv": [0, 12, 16, 16], "texture": "#front", "cullface": "north"},
            **{d: {"uv": [0, 12, 16, 16], "texture": "#side", "cullface": d} for d in sides[1:]},
            "up": {"texture": "#pad"}, "down": {"texture": "#bottom", "cullface": "down"}}}
        posts = [{"from": [x, 4, z], "to": [x + 2, 14, z + 2],
                  "faces": {d: {"uv": [7, 2, 9, 12], "texture": "#pillar"} for d in sides}} for x, z in pillars]
        cap = {"from": [0, 14, 0], "to": [16, 16, 16], "faces": {
            **{d: {"uv": [0, 0, 16, 2], "texture": "#cap", "cullface": d} for d in sides},
            "up": {"texture": "#cap_top", "cullface": "up"}, "down": {"texture": "#emitter"}}}
        parts = [base, *posts, cap]
        if on:
            glow = {"shade": False, "light_emission": 15}
            # Each a hair outside the part it lights, so it never fights with the faces under it
            parts.append({"from": [-0.01, -0.01, -0.01], "to": [16.01, 4, 16.01], **glow, "faces": {
                "north": {"uv": [0, 12, 16, 16], "texture": "#front_glow", "cullface": "north"},
                **{d: {"uv": [0, 12, 16, 16], "texture": "#side_glow", "cullface": d} for d in sides[1:]}}})
            parts += [{"from": [x - 0.01, 4, z - 0.01], "to": [x + 2.01, 14, z + 2.01], **glow,
                       "faces": {d: {"uv": [7, 2, 9, 12], "texture": "#pillar_glow"} for d in sides}} for x, z in pillars]
            parts.append({"from": [0, 13.99, 0], "to": [16, 13.99, 16], **glow,
                          "faces": {"down": {"uv": [0, 0, 16, 16], "texture": "#emitter_glow"}}})
        return parts

    for on in (False, True):
        lit = "_on" if on else ""
        textures = {"particle": tex("drone_assembler_side"), "front": tex(f"drone_assembler_front{lit}"),
                    "side": tex("drone_assembler_side"), "pad": tex("drone_assembler_top"), "bottom": tex("machine_bottom"),
                    "pillar": tex(f"drone_assembler_pillar{lit}"), "cap": tex("drone_assembler_cap"), "cap_top": tex("machine_top"),
                    "emitter": tex(f"drone_assembler_emitter{lit}")}
        if on:
            textures.update({"front_glow": tex("drone_assembler_front_glow"), "side_glow": tex("drone_assembler_side_glow"),
                             "pillar_glow": tex("drone_assembler_pillar_glow"), "emitter_glow": tex("drone_assembler_emitter_glow")})
        write(ASSETS / f"models/block/drone_assembler{lit}.json", {
            "parent": "minecraft:block/block", **({"render_type": "minecraft:cutout"} if on else {}),
            "textures": textures, "elements": elements(on)})
    facing_blockstate("drone_assembler", {"lit=false": "drone_assembler", "lit=true": "drone_assembler_on"})


def facing_blockstate(name, extra=None):
    """Variants for every horizontal facing; `extra` maps a property value suffix to a model."""
    variants = {}
    for facing, rot in FACING_ROTATION.items():
        if extra:
            for prop, model in extra.items():
                variants[f"facing={facing},{prop}"] = {"model": f"{NS}:block/{model}", **({"y": rot} if rot else {})}
        else:
            variants[f"facing={facing}"] = {"model": f"{NS}:block/{name}", **({"y": rot} if rot else {})}
    write(ASSETS / f"blockstates/{name}.json", {"variants": variants})


def dock_model():
    sides = {d: {"texture": "#side", "cullface": d} for d in ("north", "east", "south", "west")}
    elements = [{
        "from": [0, 0, 0], "to": [16, 6, 16],
        "faces": {"up": {"texture": "#top"}, "down": {"texture": "#bottom", "cullface": "down"}, **sides},
    }]
    beacon_side = {"uv": [0, 0, 3, 3], "texture": "#beacon"}
    for x in (0, 13):
        for z in (0, 13):
            elements.append({
                "from": [x, 6, z], "to": [x + 3, 9, z + 3],
                "faces": {
                    "up": {"uv": [4, 0, 7, 3], "texture": "#beacon"},
                    "north": beacon_side, "east": beacon_side, "south": beacon_side, "west": beacon_side,
                },
            })
    write(ASSETS / "models/block/drone_dock.json", {
        "parent": "minecraft:block/block",
        "textures": {
            "particle": tex("drone_dock_side"),
            "top": tex("drone_dock_top"),
            "side": tex("drone_dock_side"),
            "bottom": tex("machine_bottom"),
            "beacon": tex("drone_dock_beacon"),
        },
        "elements": elements,
    })
    # Hologram Table: a low projector plate with an emitter post on each corner, like the dock's
    # beacons; the projection is drawn by its renderer
    rim = {d: {"texture": "#side", "uv": [0, 11, 16, 16], "cullface": d} for d in ("north", "east", "south", "west")}
    post_side = {"uv": [0, 0, 2, 3], "texture": "#beacon"}
    posts = [{
        "from": [x, 5, z], "to": [x + 2, 8, z + 2],
        "faces": {"up": {"uv": [4, 0, 6, 2], "texture": "#beacon"},
                  "north": post_side, "east": post_side, "south": post_side, "west": post_side},
    } for x in (0, 14) for z in (0, 14)]
    write(ASSETS / "models/block/hologram_table.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": tex("hologram_table_side"), "side": tex("hologram_table_side"),
                     "top": tex("hologram_table_top"), "bottom": tex("machine_bottom"), "beacon": tex("drone_dock_beacon")},
        "elements": [
            {"from": [0, 0, 0], "to": [16, 5, 16],
             "faces": {**rim, "up": {"texture": "#top"}, "down": {"texture": "#bottom", "cullface": "down"}}},
            *posts,
        ],
    })
    write(ASSETS / "blockstates/hologram_table.json", {"variants": {"": {"model": f"{NS}:block/hologram_table"}}})
    # The Builder Swarm Dock: the same pad with four landing rings on top
    write(ASSETS / "models/block/swarm_dock.json", {"parent": f"{NS}:block/drone_dock", "textures": {"top": tex("swarm_dock_top")}})


def laser_turret_models():
    """Laser Turret pedestal with the bearing neck; the head is drawn by its renderer. The item shows
    the head too, barrel to the north."""
    sides = ("north", "east", "south", "west")
    base = [
        {"from": [1, 0, 1], "to": [15, 6, 15], "faces": {
            **{d: {"uv": [1, 10, 15, 16], "texture": "#side"} for d in sides},
            "up": {"uv": [1, 1, 15, 15], "texture": "#top"}, "down": {"texture": "#bottom", "cullface": "down"}}},
        {"from": [5, 6, 5], "to": [11, 8, 11], "faces": {d: {"uv": [5, 0, 11, 2], "texture": "#ring"} for d in sides}},
    ]
    head = [
        {"from": [4.5, 8, 4.5], "to": [11.5, 13, 11.5], "faces": {
            **{d: {"uv": [4, 4, 11, 9], "texture": "#ring"} for d in sides},
            "up": {"uv": [4, 4, 11, 11], "texture": "#ring"}, "down": {"uv": [4, 4, 11, 11], "texture": "#ring"}}},
        {"from": [7, 9.5, 0], "to": [9, 11.5, 4.5], "faces": {
            "north": {"uv": [7, 11, 9, 12], "texture": "#side"},
            **{d: {"uv": [0, 6, 4.5, 8], "texture": "#ring"} for d in ("east", "west", "up", "down")}}},
    ]
    textures = {"particle": tex("laser_turret_side"), "side": tex("laser_turret_side"), "top": tex("laser_turret_top"),
                "ring": tex("laser_turret_ring"), "bottom": tex("machine_bottom")}
    write(ASSETS / "models/block/laser_turret.json", {"parent": "minecraft:block/block", "textures": textures, "elements": base})
    write(ASSETS / "models/block/laser_turret_inventory.json", {"parent": "minecraft:block/block", "textures": textures, "elements": base + head})
    facing_blockstate("laser_turret")


def energy_storage_models():
    """Resonance Core pedestal (its crystal is drawn by the renderer) and the Stabilizer Pylon."""
    sides = {d: {"texture": "#side", "cullface": d} for d in ("east", "south", "west")}
    write(ASSETS / "models/block/resonance_core.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": tex("resonance_core_side"), "front": tex("resonance_core_front"), "side": tex("resonance_core_side"),
                     "top": tex("resonance_core_top"), "bottom": tex("machine_bottom"), "ring": tex("resonance_core_ring")},
        "elements": [
            {"from": [0, 0, 0], "to": [16, 6, 16], "faces": {
                "north": {"texture": "#front", "cullface": "north"}, **sides,
                "up": {"texture": "#top"}, "down": {"texture": "#bottom", "cullface": "down"}}},
            {"from": [3, 6, 3], "to": [13, 8, 13], "faces": {
                "up": {"uv": [3, 3, 13, 13], "texture": "#ring"},
                **{d: {"uv": [3, 0, 13, 2], "texture": "#ring"} for d in ("north", "east", "south", "west")}}},
        ],
    })
    facing_blockstate("resonance_core")

    def box(start, end, side="#side", side_uv=None, glow=False):
        element = {"from": start, "to": end, "faces": {
            **{d: {"uv": side_uv or [start[0], 16 - end[1], end[0], 16 - start[1]], "texture": side} for d in ("north", "east", "south", "west")},
            "up": {"uv": [start[0], start[2], end[0], end[2]], "texture": "#top"},
            "down": {"uv": [start[0], start[2], end[0], end[2]], "texture": "#top"}}}
        if glow:
            element["light_emission"] = 15
        return element

    # A pylon column: footing at the bottom, shaft segments, and on top the glowing emitter ring and tip
    footing = box([2, 0, 2], [14, 3, 14])
    crown = [box([3, 6, 3], [13, 11, 13], "#emitter", [0, 0, 16, 5], glow=True), box([5, 11, 5], [11, 14, 11]),
             box([6, 14, 6], [10, 16, 10], glow=True)]
    parts = {
        "single": [footing, box([4, 3, 4], [12, 6, 12])] + crown,
        "bottom": [footing, box([4, 3, 4], [12, 16, 12])],
        "middle": [box([4, 0, 4], [12, 16, 12])],
        "top": [box([4, 0, 4], [12, 6, 12])] + crown,
    }
    for part, elements in parts.items():
        write(ASSETS / f"models/block/stabilizer_pylon_{part}.json", {
            "parent": "minecraft:block/block",
            "textures": {"particle": tex("stabilizer_pylon_side"), "side": tex("stabilizer_pylon_side"),
                         "top": tex("stabilizer_pylon_top"), "emitter": tex("stabilizer_pylon_emitter")},
            "elements": elements,
        })
    # Socket toward a cable plugged into the side (modelled facing north, turned for the others)
    write(ASSETS / "models/block/stabilizer_pylon_socket.json", {
        "textures": {"particle": tex("stabilizer_pylon_side"), "side": tex("stabilizer_pylon_side"), "port": tex("stabilizer_pylon_port")},
        "elements": [{"from": [5, 5, 0], "to": [11, 11, 4], "faces": {
            "north": {"texture": "#port", "cullface": "north"},
            **{d: {"uv": [5, 5, 9, 11], "texture": "#side"} for d in ("east", "west")},
            **{d: {"uv": [5, 0, 11, 4], "texture": "#side"} for d in ("up", "down")}}}],
    })
    multipart = [{"when": {"part": part}, "apply": {"model": f"{NS}:block/stabilizer_pylon_{part}"}} for part in parts]
    for side, rot in FACING_ROTATION.items():
        multipart.append({"when": {side: "true"}, "apply": {"model": f"{NS}:block/stabilizer_pylon_socket", **({"y": rot} if rot else {})}})
    write(ASSETS / "blockstates/stabilizer_pylon.json", {"multipart": multipart})
    # The item (and anything else asking for the plain model) shows a whole single pylon
    write(ASSETS / "models/block/stabilizer_pylon.json", {"parent": f"{NS}:block/stabilizer_pylon_single"})

    # Shell cells vanish inside the crystal once the shell is complete
    write(ASSETS / "models/block/resonance_cell.json", {"parent": "minecraft:block/cube_all", "textures": {"all": tex("resonance_cell")}})
    write(ASSETS / "models/block/resonance_cell_hidden.json", {"textures": {"particle": tex("resonance_cell")}})
    write(ASSETS / "blockstates/resonance_cell.json", {"variants": {
        "formed=false": {"model": f"{NS}:block/resonance_cell"}, "formed=true": {"model": f"{NS}:block/resonance_cell_hidden"}}})


PIPES = [f"{tier}{kind}" for kind in ("energy_conduit", "gas_pipe", "liquid_pipe") for tier in ("", "advanced_", "elite_")]


def pipe_models(name):
    """Core, arm and inventory models plus a multipart blockstate for a NetworkPipeBlock."""
    texture = {"particle": tex(name), "conduit": tex(name)}
    all_faces = lambda: {d: {"texture": "#conduit"} for d in ("north", "east", "south", "west", "up", "down")}
    core = {"from": [5, 5, 5], "to": [11, 11, 11], "faces": all_faces()}
    # Arm pointing north; the blockstate rotates it to every other side. The coloured band (rows 6..9,
    # ring at column 0) goes round all four sides along the tube, so a pipe looks the same whichever
    # way it runs: up and down get it turned a quarter, the ring at the block's edge.
    arm = {"from": [6, 6, 0], "to": [10, 10, 5], "faces": {
        "north": {"texture": "#conduit", "cullface": "north"},
        "east": {"texture": "#conduit"}, "west": {"texture": "#conduit"},
        "up": {"texture": "#conduit", "uv": [0, 6, 5, 10], "rotation": 90},
        "down": {"texture": "#conduit", "uv": [0, 6, 5, 10], "rotation": 270},
    }}
    write(ASSETS / f"models/block/{name}_core.json", {"parent": "minecraft:block/block", "textures": texture, "elements": [core]})
    write(ASSETS / f"models/block/{name}_side.json", {"textures": texture, "elements": [arm]})
    # Middle of a straight run, joining the two arms into one tube (along north-south, rotated for the other axes).
    # Columns 1..4 are plain tube, stretched over the 6 pixels: 5..10 belong to the coupling box.
    body = [1, 6, 5, 10]
    middle = {"from": [6, 6, 5], "to": [10, 10, 11], "faces": {
        "east": {"texture": "#conduit", "uv": body}, "west": {"texture": "#conduit", "uv": body},
        "up": {"texture": "#conduit", "uv": body, "rotation": 90}, "down": {"texture": "#conduit", "uv": body, "rotation": 270},
    }}
    write(ASSETS / f"models/block/{name}_middle.json", {"textures": texture, "elements": [middle]})
    # Item: core with arms east and west
    write(ASSETS / f"models/block/{name}_inventory.json", {
        "parent": "minecraft:block/block", "textures": texture,
        "elements": [core,
                     {"from": [0, 6, 6], "to": [5, 10, 10], "faces": all_faces()},
                     {"from": [11, 6, 6], "to": [16, 10, 10], "faces": all_faces()}],
    })
    rotations = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
    # The coupling box only where the pipe is not a straight run: at junctions, bends and ends
    sides = list(rotations)
    opposite = {"north": "south", "east": "west", "up": "down"}
    not_straight = [{a: "true", b: "false"} for a, b in opposite.items()] + [{a: "false", b: "true"} for a, b in opposite.items()]
    not_straight += [{"north": "true", "east": "true"}, {"north": "true", "up": "true"}, {"east": "true", "up": "true"}]
    not_straight.append({side: "false" for side in sides})
    parts = [{"when": {"OR": not_straight}, "apply": {"model": f"{NS}:block/{name}_core"}}]
    for axis, (a, b), rotation in (("z", ("north", "south"), {}), ("x", ("east", "west"), {"y": 90}), ("y", ("up", "down"), {"x": 90})):
        straight = {side: "true" if side in (a, b) else "false" for side in sides}
        parts.append({"when": straight, "apply": {"model": f"{NS}:block/{name}_middle", **rotation}})
    for direction, rotation in rotations.items():
        parts.append({"when": {direction: "true"}, "apply": {"model": f"{NS}:block/{name}_side", **rotation}})
    # Pulling collar on the arms set to extract (shared by every pipe)
    for direction, rotation in rotations.items():
        parts.append({"when": {direction: "true", f"extract_{direction}": "true"}, "apply": {"model": f"{NS}:block/pipe_extract", **rotation}})
    write(ASSETS / f"blockstates/{name}.json", {"multipart": parts})
    write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/{name}_inventory"}})


def block_loot(name):
    write(DATA / f"loot_table/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1.0,
            "bonus_rolls": 0.0,
            "conditions": [{"condition": "minecraft:survives_explosion"}],
            "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}],
        }],
        "random_sequence": f"{NS}:blocks/{name}",
    })


def blocks():
    lit_machine("combustion_generator", "combustion_generator_top")

    assembler_models()

    dock_model()
    facing_blockstate("drone_dock")
    facing_blockstate("swarm_dock")

    for pipe in PIPES:
        pipe_models(pipe)
    write(ASSETS / "models/block/pipe_extract.json", {"textures": {"particle": tex("pipe_extract"), "collar": tex("pipe_extract")},
        "elements": [{"from": [5, 5, 0], "to": [11, 11, 2], "faces": {d: {"texture": "#collar"} for d in ("north", "east", "south", "west", "up", "down")}}]})

    lit_machine("bio_digester", "bio_digester_top")
    lit_machine("gas_turbine", "gas_turbine_top")

    # The extractor is the chamber's "shower head": its bottom face looks into the chamber
    for model, suffix in (("methane_extractor", ""), ("methane_extractor_formed", "_formed")):
        side = tex("methane_extractor_side" + suffix)
        write(ASSETS / f"models/block/{model}.json", {
            "parent": "minecraft:block/cube",
            "textures": {"particle": side, "up": tex("methane_extractor_top" + suffix),
                         "down": tex("methane_extractor_bottom" + suffix),
                         "north": side, "east": side, "south": side, "west": side},
        })
    # A formed chamber is drawn whole by the extractor's renderer: its blocks show no faces
    write(ASSETS / "models/block/methane_extractor_hidden.json", {"textures": {"particle": tex("methane_extractor_side_formed")}})
    write(ASSETS / "blockstates/methane_extractor.json", {"variants": {
        f"facing={f},formed={formed}": {"model": f"{NS}:block/methane_extractor{'_hidden' if formed == 'true' else ''}"}
        for f in FACING_ROTATION for formed in ("false", "true")}})

    for model in ("cyber_frame", "cyber_frame_formed"):
        write(ASSETS / f"models/block/{model}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": tex(model)}})
    for part in ("cyber_frame", "acid_inlet", "methane_outlet", "intake_gate", "containment_glass"):
        if part not in ("cyber_frame", "containment_glass"):
            write(ASSETS / f"models/block/{part}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": tex(part)}})
        write(ASSETS / f"models/block/{part}_hidden.json", {"textures": {"particle": tex(part)}})
        variants = {"formed=false": {"model": f"{NS}:block/{part}"}, "formed=true": {"model": f"{NS}:block/{part}_hidden"}}
        if part == "intake_gate":
            variants = {f"{key},open={o}": model for key, model in variants.items() for o in ("false", "true")}
        write(ASSETS / f"blockstates/{part}.json", {"variants": variants})

    # Soft block, not a machine: no pickaxe needed
    write(ASSETS / "models/block/muffler.json", {"parent": "minecraft:block/cube_all", "textures": {"all": tex("muffler")}})
    facing_blockstate("muffler")
    write(ASSETS / "items/muffler.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/muffler"}})
    block_loot("muffler")

    nuclear_blocks()
    coolant_blocks()
    energy_storage_models()
    laser_turret_models()

    names = PIPES + ["combustion_generator", "drone_assembler", "drone_dock", "swarm_dock", "hologram_table",
             "bio_digester", "gas_turbine", "methane_extractor", "cyber_frame",
             "reactor_controller", "reactor_casing", "reactor_port", "turbine_controller", "turbine_casing", "turbine_port", "water_pump", "coolant_mixer",
             "waste_reprocessor", "vitrified_waste", "liquid_tank", "gas_tank", "resonance_core", "resonance_cell", "stabilizer_pylon",
             "acid_synthesizer", "containment_glass", "acid_inlet", "methane_outlet", "intake_gate", "steam_engine", "steam_engine_cylinder", "steam_engine_dynamo",
             "mech_hangar", "hangar_frame", "hangar_pad", "crystallizer", "crystallizer_port", "electrolyzer", "pyrolysis_oven", "fabricator",
             "supply_terminal", "laser_turret"]
    for name in names:
        if not name.endswith(("_conduit", "_pipe")):
            model = f"{name}_inventory" if name == "laser_turret" else name
            write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/{model}"}})
        block_loot(name)

    # Common fluid tags, so other mods recognise our gases
    write(RES / "data/c/tags/fluid/biogas.json", {"values": [f"{NS}:biogas"]})
    write(RES / "data/c/tags/fluid/methane.json", {"values": [f"{NS}:methane"]})
    ores = ["uranium_ore", "deepslate_uranium_ore"]
    bauxite = ["bauxite_ore", "deepslate_bauxite_ore"]
    write(RES / "data/minecraft/tags/block/mineable/pickaxe.json", {"values": [f"{NS}:{n}" for n in names + ores + bauxite + ["nether_sulfur_ore"]]})
    write(RES / "data/minecraft/tags/block/needs_iron_tool.json", {"values": [f"{NS}:{n}" for n in ores]})
    write(RES / "data/minecraft/tags/block/needs_stone_tool.json", {"values": [f"{NS}:{n}" for n in bauxite]})
    write(RES / "data/c/tags/fluid/steam.json", {"values": [f"{NS}:steam"]})
    write(RES / "data/c/tags/fluid/gaseous.json", {"values": [f"{NS}:biogas", f"{NS}:methane", f"{NS}:steam"]})
    # Common material tags, so other mods' machines process our uranium and ours take theirs
    for kind in ("block", "item"):
        write(RES / f"data/c/tags/{kind}/ores/uranium.json", {"values": [f"{NS}:{n}" for n in ores]})
        write(RES / f"data/c/tags/{kind}/ores/sulfur.json", {"values": [f"{NS}:nether_sulfur_ore"]})
        write(RES / f"data/c/tags/{kind}/ores/bauxite.json", {"values": [f"{NS}:{n}" for n in bauxite]})
        # Bauxite is aluminum ore to other mods' crushers; our furnace recipes stay on our own ore (no smelting conflict)
        write(RES / f"data/c/tags/{kind}/ores/aluminum.json", {"replace": False, "values": [f"{NS}:{n}" for n in bauxite]})
        write(RES / f"data/c/tags/{kind}/ores.json", {"replace": False, "values": ["#c:ores/uranium", "#c:ores/sulfur", "#c:ores/bauxite", "#c:ores/aluminum"]})
        write(RES / f"data/c/tags/{kind}/ores_in_ground/netherrack.json", {"replace": False, "values": [f"{NS}:nether_sulfur_ore"]})
    write(RES / "data/c/tags/item/dusts/sulfur.json", {"values": [f"{NS}:sulfur"]})
    write(RES / "data/c/tags/item/dusts/alumina.json", {"values": [f"{NS}:alumina"]})
    write(RES / "data/c/tags/item/dusts.json", {"replace": False, "values": ["#c:dusts/sulfur", "#c:dusts/alumina"]})
    write(RES / "data/c/tags/fluid/acid.json", {"values": [f"{NS}:acid"]})
    write(RES / "data/c/tags/item/raw_materials/uranium.json", {"values": [f"{NS}:raw_uranium"]})
    write(RES / "data/c/tags/item/raw_materials/bauxite.json", {"values": [f"{NS}:raw_bauxite"]})
    write(RES / "data/c/tags/item/raw_materials/aluminum.json", {"replace": False, "values": [f"{NS}:raw_bauxite"]})
    write(RES / "data/c/tags/item/raw_materials.json", {"replace": False, "values": ["#c:raw_materials/uranium", "#c:raw_materials/bauxite", "#c:raw_materials/aluminum"]})
    write(RES / "data/c/tags/item/ingots/uranium.json", {"values": [f"{NS}:uranium_ingot"]})
    write(RES / "data/c/tags/item/ingots/aluminum.json", {"values": [f"{NS}:aluminum_ingot"]})
    write(RES / "data/c/tags/item/ingots.json", {"replace": False, "values": ["#c:ingots/uranium", "#c:ingots/aluminum"]})
    uranium_worldgen()
    sulfur_ore()
    bauxite_ores()


def nuclear_blocks():
    # Ores: vanilla stone or deepslate under a transparent overlay of uranium specks
    for name, base in (("uranium_ore", "minecraft:block/stone"), ("deepslate_uranium_ore", "minecraft:block/deepslate")):
        faces = {d: {"uv": [0, 0, 16, 16], "texture": "#base", "cullface": d} for d in ("down", "up", "north", "south", "west", "east")}
        over = {d: {"uv": [0, 0, 16, 16], "texture": "#overlay", "cullface": d} for d in faces}
        write(ASSETS / f"models/block/{name}.json", {
            "parent": "minecraft:block/block",
            "textures": {"particle": base, "base": base, "overlay": tex("uranium_ore_overlay")},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces},
                         {"from": [0, 0, 0], "to": [16, 16, 16], "faces": over}],
        })
        write(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": f"{NS}:block/{name}"}}})
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/{name}"}})
        write(DATA / f"loot_table/blocks/{name}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": f"{NS}:{name}", "conditions": [{"condition": "minecraft:match_tool", "predicate": {
                    "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
                {"type": "minecraft:item", "name": f"{NS}:raw_uranium", "functions": [
                    {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                    {"function": "minecraft:explosion_decay"}]},
            ]}]}],
            "random_sequence": f"{NS}:blocks/{name}",
        })

    # Formed casings are hidden inside the tower drawn by the controller: a model with no faces
    write(ASSETS / "models/block/reactor_casing_hidden.json", {"textures": {"particle": tex("reactor_casing_formed")}})
    write(ASSETS / "blockstates/reactor_casing.json", {"variants": {
        "formed=false": {"model": f"{NS}:block/reactor_casing"}, "formed=true": {"model": f"{NS}:block/reactor_casing_hidden"}}})
    write(ASSETS / "models/block/reactor_casing.json", {"parent": "minecraft:block/cube_all", "textures": {"all": tex("reactor_casing")}})

    # Port: a plain cube; once formed the tower draws it as a hatch on its surface
    write(ASSETS / "models/block/reactor_port.json", {"parent": "minecraft:block/cube_all", "textures": {"all": tex("reactor_port")}})
    write(ASSETS / "models/block/reactor_port_hidden.json", {"textures": {"particle": tex("reactor_port_formed")}})
    write(ASSETS / "blockstates/reactor_port.json", {"variants": {
        "formed=false": {"model": f"{NS}:block/reactor_port"}, "formed=true": {"model": f"{NS}:block/reactor_port_hidden"}}})

    # Controller: incomplete, formed (ready) and burning
    orientable("reactor_controller", "reactor_controller", "reactor_casing", side="reactor_casing", bottom="reactor_casing")
    write(ASSETS / "models/block/reactor_controller_hidden.json", {"textures": {"particle": tex("reactor_controller_formed")}})
    variants = {}
    for facing, rot in FACING_ROTATION.items():
        for formed in ("false", "true"):
            for lit in ("false", "true"):
                # Formed, the tower draws the controller as a panel on its surface
                model = "reactor_controller" if formed == "false" else "reactor_controller_hidden"
                for size in range(3):
                    variants[f"facing={facing},formed={formed},lit={lit},size={size}"] = {"model": f"{NS}:block/{model}", **({"y": rot} if rot else {})}
    write(ASSETS / "blockstates/reactor_controller.json", {"variants": variants})

    # Steam Turbine: the same scheme, its formed hall drawn by the controller as a cylinder
    for part in ("casing", "port"):
        write(ASSETS / f"models/block/turbine_{part}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": tex(f"turbine_{part}")}})
        write(ASSETS / f"models/block/turbine_{part}_hidden.json", {"textures": {"particle": tex(f"turbine_{part}_formed")}})
        write(ASSETS / f"blockstates/turbine_{part}.json", {"variants": {
            "formed=false": {"model": f"{NS}:block/turbine_{part}"}, "formed=true": {"model": f"{NS}:block/turbine_{part}_hidden"}}})
    orientable("turbine_controller", "turbine_controller", "turbine_casing", side="turbine_casing", bottom="turbine_casing")
    write(ASSETS / "models/block/turbine_controller_hidden.json", {"textures": {"particle": tex("turbine_controller_formed")}})
    variants = {}
    for facing, rot in FACING_ROTATION.items():
        for formed in ("false", "true"):
            for lit in ("false", "true"):
                for size in range(3):
                    model = "turbine_controller" if formed == "false" else "turbine_controller_hidden"
                    variants[f"facing={facing},formed={formed},lit={lit},size={size}"] = {"model": f"{NS}:block/{model}", **({"y": rot} if rot else {})}
    write(ASSETS / "blockstates/turbine_controller.json", {"variants": variants})
    lit_machine("water_pump", "water_pump_top")
    for tank in ("liquid_tank", "gas_tank"):
        write(ASSETS / f"models/block/{tank}.json", {"parent": "minecraft:block/cube_column",
            "textures": {"side": tex(f"{tank}_side"), "end": tex("tank_top")}})
        facing_blockstate(tank)
    lit_machine("coolant_mixer", "machine_top")
    lit_machine("waste_reprocessor", "machine_top")
    write(ASSETS / "models/block/vitrified_waste.json", {"parent": "minecraft:block/cube_all", "textures": {"all": tex("vitrified_waste")}})
    lit_machine("acid_synthesizer", "machine_top")
    lit_machine("electrolyzer", "machine_top")
    lit_machine("pyrolysis_oven", "machine_top")
    lit_machine("fabricator", "machine_top")
    orientable("supply_terminal", "supply_terminal_front", "machine_top")
    facing_blockstate("supply_terminal")
    # A villager job site, for mods that look for them
    write(RES / "data/c/tags/block/villager_job_sites.json", {"replace": False, "values": [f"{NS}:supply_terminal"]})
    write(RES / "data/c/tags/item/villager_job_sites.json", {"replace": False, "values": [f"{NS}:supply_terminal"]})
    orientable("crystallizer", "crystallizer_front", "crystallizer_top")
    # A complete chamber is drawn whole by the Crystallizer's renderer, the controller with it
    write(ASSETS / "models/block/crystallizer_hidden.json", {"textures": {"particle": tex("crystallizer_front")}})
    facing_blockstate("crystallizer", {"formed=false": "crystallizer", "formed=true": "crystallizer_hidden"})
    # Its ports hide too: the renderer draws a socket on the machine where each one is
    write(ASSETS / "models/block/crystallizer_port.json", {"parent": "minecraft:block/cube_all", "textures": {"all": tex("crystallizer_port")}})
    write(ASSETS / "models/block/crystallizer_port_hidden.json", {"textures": {"particle": tex("crystallizer_port")}})
    write(ASSETS / "blockstates/crystallizer_port.json", {"variants": {
        "formed=false": {"model": f"{NS}:block/crystallizer_port"}, "formed=true": {"model": f"{NS}:block/crystallizer_port_hidden"}}})
    # A complete Steam Engine is drawn whole by the boiler's renderer: its blocks show no faces
    orientable("steam_engine", "steam_engine_front", "machine_top")
    write(ASSETS / "models/block/steam_engine_hidden.json", {"textures": {"particle": tex("steam_engine_front")}})
    write(ASSETS / "blockstates/steam_engine.json", {"variants": {
        f"facing={f},formed={formed}": {"model": f"{NS}:block/steam_engine" + ("_hidden" if formed == "true" else ""),
                                        **({"y": rot} if rot and formed == "false" else {})}
        for f, rot in FACING_ROTATION.items() for formed in ("false", "true")}})
    for part in ("steam_engine_cylinder", "steam_engine_dynamo"):
        write(ASSETS / f"models/block/{part}.json", {"parent": "minecraft:block/cube_column",
            "textures": {"side": tex(part), "end": tex("machine_top")}})
        write(ASSETS / f"models/block/{part}_hidden.json", {"textures": {"particle": tex(part)}})
        write(ASSETS / f"blockstates/{part}.json", {"variants": {
            "formed=false": {"model": f"{NS}:block/{part}"}, "formed=true": {"model": f"{NS}:block/{part}_hidden"}}})
    write(ASSETS / "models/block/containment_glass.json", {"parent": "minecraft:block/cube_all",
        "render_type": "minecraft:translucent", "textures": {"all": tex("containment_glass")}})
    # Mech Hangar: console, frame and pad light up once the hangar is complete
    orientable("mech_hangar", "mech_hangar_front", "machine_top")
    orientable("mech_hangar_on", "mech_hangar_front_on", "machine_top")
    facing_blockstate("mech_hangar", {"formed=false": "mech_hangar", "formed=true": "mech_hangar_on"})
    for model in ("hangar_frame", "hangar_frame_formed"):
        write(ASSETS / f"models/block/{model}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": tex(model)}})
    # Formed, the rack's frames hide: the console's renderer draws the whole rack
    write(ASSETS / "models/block/hangar_frame_hidden.json", {"textures": {"particle": tex("hangar_frame_formed")}})
    write(ASSETS / "blockstates/hangar_frame.json", {"variants": {
        "formed=false": {"model": f"{NS}:block/hangar_frame"}, "formed=true": {"model": f"{NS}:block/hangar_frame_hidden"}}})
    for model, top in (("hangar_pad", "hangar_pad"), ("hangar_pad_formed", "hangar_pad_formed")):
        write(ASSETS / f"models/block/{model}.json", {"parent": "minecraft:block/cube_bottom_top",
            "textures": {"top": tex(top), "side": tex("hangar_pad_side"), "bottom": tex("machine_bottom")}})
    write(ASSETS / "blockstates/hangar_pad.json", {"variants": {
        "formed=false": {"model": f"{NS}:block/hangar_pad"}, "formed=true": {"model": f"{NS}:block/hangar_pad_formed"}}})
    write(ASSETS / "blockstates/vitrified_waste.json", {"variants": {"": {"model": f"{NS}:block/vitrified_waste"}}})

    radiation_data()


def coolant_blocks():
    """Coolant pools in the world: the fluid renderer draws them, the model only gives the particle texture."""
    for name in ("coolant", "cryo_coolant"):
        write(ASSETS / f"models/block/{name}.json", {"textures": {"particle": tex(f"{name}_still")}})
        write(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": f"{NS}:block/{name}"}}})


def radiation_data():
    """Radiation damage type, Hazmat Suit equipment model and the tags they need."""
    write(DATA / "damage_type/radiation.json", {"message_id": f"{NS}.radiation", "exhaustion": 0.0, "scaling": "never"})
    write(RES / "data/minecraft/tags/damage_type/bypasses_armor.json", {"replace": False, "values": [f"{NS}:radiation"]})
    # The laser keeps a mob where the gun aims instead of pushing it out of range
    write(DATA / "damage_type/laser.json", {"message_id": f"{NS}.laser", "exhaustion": 0.1, "scaling": "never"})
    write(RES / "data/minecraft/tags/damage_type/no_knockback.json", {"replace": False, "values": [f"{NS}:radiation", f"{NS}:laser"]})
    write(ASSETS / "equipment/hazmat.json", {"layers": {
        "humanoid": [{"texture": f"{NS}:hazmat"}],
        "humanoid_leggings": [{"texture": f"{NS}:hazmat"}],
    }})
    # The suit keeps the cold of coolant pools out, like leather in powder snow
    write(RES / "data/minecraft/tags/item/freeze_immune_wearables.json", {"replace": False, "values": [
        f"{NS}:hazmat_helmet", f"{NS}:hazmat_suit", f"{NS}:hazmat_pants", f"{NS}:hazmat_boots"]})


def bauxite_ores():
    """Bauxite, the ore of aluminum: rusty clumps in stone and deepslate, dropping Raw Bauxite, in
    veins about as common as iron, mostly near the surface where it weathers out."""
    for name, base in (("bauxite_ore", "minecraft:block/stone"), ("deepslate_bauxite_ore", "minecraft:block/deepslate")):
        faces = {d: {"uv": [0, 0, 16, 16], "texture": "#base", "cullface": d} for d in ("down", "up", "north", "south", "west", "east")}
        over = {d: {"uv": [0, 0, 16, 16], "texture": "#overlay", "cullface": d} for d in faces}
        write(ASSETS / f"models/block/{name}.json", {
            "parent": "minecraft:block/block",
            "textures": {"particle": base, "base": base, "overlay": tex("bauxite_ore_overlay")},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces},
                         {"from": [0, 0, 0], "to": [16, 16, 16], "faces": over}],
        })
        write(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": f"{NS}:block/{name}"}}})
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/{name}"}})
        write(DATA / f"loot_table/blocks/{name}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": f"{NS}:{name}", "conditions": [{"condition": "minecraft:match_tool", "predicate": {
                    "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
                {"type": "minecraft:item", "name": f"{NS}:raw_bauxite", "functions": [
                    {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                    {"function": "minecraft:explosion_decay"}]},
            ]}]}],
            "random_sequence": f"{NS}:blocks/{name}",
        })
    write(DATA / "worldgen/configured_feature/bauxite_ore.json", {"type": "minecraft:ore", "config": {
        "discard_chance_on_air_exposure": 0.0, "size": 9, "targets": [
            {"state": {"Name": f"{NS}:bauxite_ore"}, "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}},
            {"state": {"Name": f"{NS}:deepslate_bauxite_ore"}, "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"}},
        ]}})
    write(DATA / "worldgen/placed_feature/bauxite_ore.json", {"feature": f"{NS}:bauxite_ore", "placement": [
        {"type": "minecraft:count", "count": 10},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:trapezoid",
                                                      "min_inclusive": {"absolute": -16}, "max_inclusive": {"absolute": 112}}},
        {"type": "minecraft:biome"},
    ]})
    write(DATA / "neoforge/biome_modifier/bauxite_ore.json", {
        "neoforge:conditions": [{"type": f"{NS}:bauxite_generation"}],  # config: world.bauxiteGeneration
        "type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
        "features": f"{NS}:bauxite_ore", "step": "underground_ores"})


def sulfur_ore():
    """Sulfur in the Nether: netherrack with yellow crystals, dropping sulfur dust like redstone ore."""
    name, base = "nether_sulfur_ore", "minecraft:block/netherrack"
    faces = {d: {"uv": [0, 0, 16, 16], "texture": "#base", "cullface": d} for d in ("down", "up", "north", "south", "west", "east")}
    over = {d: {"uv": [0, 0, 16, 16], "texture": "#overlay", "cullface": d} for d in faces}
    write(ASSETS / f"models/block/{name}.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": base, "base": base, "overlay": tex("nether_sulfur_ore_overlay")},
        "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces},
                     {"from": [0, 0, 0], "to": [16, 16, 16], "faces": over}],
    })
    write(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": f"{NS}:block/{name}"}}})
    write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/{name}"}})
    write(DATA / f"loot_table/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": f"{NS}:{name}", "conditions": [{"condition": "minecraft:match_tool", "predicate": {
                "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
            {"type": "minecraft:item", "name": f"{NS}:sulfur", "functions": [
                {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2.0, "max": 4.0}},
                {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:uniform_bonus_count",
                 "parameters": {"bonusMultiplier": 1}},
                {"function": "minecraft:explosion_decay"}]},
        ]}]}],
        "random_sequence": f"{NS}:blocks/{name}",
    })
    write(DATA / f"worldgen/configured_feature/{name}.json", {"type": "minecraft:ore", "config": {
        "discard_chance_on_air_exposure": 0.0, "size": 10, "targets": [
            {"state": {"Name": f"{NS}:{name}"}, "target": {"predicate_type": "minecraft:block_match", "block": "minecraft:netherrack"}}]}})
    write(DATA / f"worldgen/placed_feature/{name}.json", {"feature": f"{NS}:{name}", "placement": [
        {"type": "minecraft:count", "count": 12},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform",
                                                      "min_inclusive": {"above_bottom": 10}, "max_inclusive": {"below_top": 10}}},
        {"type": "minecraft:biome"},
    ]})
    write(DATA / f"neoforge/biome_modifier/{name}.json", {
        "type": "neoforge:add_features", "biomes": "#minecraft:is_nether",
        "features": f"{NS}:{name}", "step": "underground_decoration"})


def uranium_worldgen():
    """Rare uranium veins deep in the Overworld, more common towards the bottom of the world."""
    write(DATA / "worldgen/configured_feature/uranium_ore.json", {"type": "minecraft:ore", "config": {
        "discard_chance_on_air_exposure": 0.3, "size": 5, "targets": [
            {"state": {"Name": f"{NS}:uranium_ore"}, "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}},
            {"state": {"Name": f"{NS}:deepslate_uranium_ore"}, "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"}},
        ]}})
    write(DATA / "worldgen/placed_feature/uranium_ore.json", {"feature": f"{NS}:uranium_ore", "placement": [
        {"type": "minecraft:count", "count": 4},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:trapezoid",
                                                      "min_inclusive": {"absolute": -64}, "max_inclusive": {"absolute": 16}}},
        {"type": "minecraft:biome"},
    ]})
    write(DATA / "neoforge/biome_modifier/uranium_ore.json", {
        "type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
        "features": f"{NS}:uranium_ore", "step": "underground_ores"})


# --------------------------------------------------------------------------- items

def items():
    for name in FLAT_ITEMS:
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}})
    # One drone item: built on a Mini Drone Frame it shows the mini sprite
    write(ASSETS / "items/drone.json", {"model": {
        "type": "minecraft:condition", "property": f"{NS}:mini_drone",
        "on_true": {"type": "minecraft:model", "model": f"{NS}:item/mini_drone"},
        "on_false": {"type": "minecraft:model", "model": f"{NS}:item/drone"},
    }})
    write(ASSETS / "models/item/mob_capsule.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/mob_capsule"}})
    write(ASSETS / "items/mob_capsule.json", {"model": {
        "type": "minecraft:condition", "property": "minecraft:has_component", "component": f"{NS}:captured_mob",
        "on_true": {"type": "minecraft:model", "model": f"{NS}:item/mob_capsule_filled"},
        "on_false": {"type": "minecraft:model", "model": f"{NS}:item/mob_capsule"},
    }})
    # Model used by GuideME for the handbook (see guideme_guides/handbook.json). GuideME bakes it on
    # demand, so it only exists if something references it: the items/ definition makes the game load it.
    write(ASSETS / "models/item/handbook.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/handbook"}})
    write(ASSETS / "items/handbook.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/handbook"}})


# --------------------------------------------------------------------------- recipes

def shaped(name, pattern, key, count=1, category="misc"):
    result = {"id": f"{NS}:{name}"}
    if count > 1:
        result["count"] = count
    write(DATA / f"recipe/{name}.json", {
        "type": "minecraft:crafting_shaped", "category": category,
        "key": key, "pattern": pattern, "result": result,
    })


def recipes():
    iron, copper, gold, redstone = "minecraft:iron_ingot", "minecraft:copper_ingot", "minecraft:gold_ingot", "minecraft:redstone"
    core, rotor, cell = f"{NS}:drone_core", f"{NS}:rotor", f"{NS}:battery_cell"
    # Progression: aluminum and Basic Circuits, then carbon fiber and Advanced Circuits (acid), then Elite Circuits (uranium)
    aluminum, fiber = "#c:ingots/aluminum", f"{NS}:carbon_fiber"
    circuit, advanced_circuit, elite_circuit = f"{NS}:basic_circuit", f"{NS}:advanced_circuit", f"{NS}:elite_circuit"
    for source, suffix in ((f"{NS}:raw_bauxite", ""), (f"{NS}:bauxite_ore", "_from_ore"), (f"{NS}:deepslate_bauxite_ore", "_from_deepslate_ore")):
        for kind, time in (("smelting", 200), ("blasting", 100)):
            write(DATA / f"recipe/alumina{suffix}_{kind}.json", {
                "type": f"minecraft:{kind}", "category": "misc", "cookingtime": time, "experience": 0.5,
                "group": "alumina", "ingredient": source, "result": {"id": f"{NS}:alumina"}})
    # By hand a circuit is slow and dear; the Fabricator makes two from the same
    shaped("basic_circuit", [" R ", "CNC", " R "], {"R": redstone, "C": copper, "N": "minecraft:gold_nugget"})
    shaped("electrolyzer", ["ICI", "GXG", "IBI"], {"I": iron, "C": circuit, "G": "minecraft:glass", "X": "minecraft:cauldron", "B": cell})
    shaped("fabricator", ["IPI", "CTC", "IBI"], {"I": iron, "P": "minecraft:piston", "C": circuit, "T": "minecraft:crafting_table", "B": cell})
    shaped("supply_terminal", ["GGG", "CXC", "IBI"], {"G": "minecraft:glass_pane", "C": circuit, "X": "minecraft:emerald",
                                                      "I": iron, "B": "minecraft:barrel"})
    shaped("pyrolysis_oven", ["ICI", "PFP", "III"], {"I": iron, "C": circuit, "P": f"{NS}:gas_pipe", "F": "minecraft:blast_furnace"})

    shaped("drone_core", ["IRI", "RGR", "IRI"], {"I": iron, "R": redstone, "G": gold}, count=2)
    shaped("rotor", ["N N", " I ", "N N"], {"N": "minecraft:iron_nugget", "I": iron}, count=2)
    shaped("battery_cell", [" N ", "CRC", "CRC"], {"N": "minecraft:iron_nugget", "C": copper, "R": redstone})

    # Frames like real drones': aluminum first, carbon fiber for the advanced and elite ones
    shaped("basic_frame", ["RAR", "CDB", "RAR"], {"R": rotor, "A": aluminum, "C": circuit, "D": core, "B": cell})
    shaped("mini_frame", ["RAR", " D "], {"R": rotor, "A": aluminum, "D": core})
    shaped("advanced_frame", ["KCK", "BFB", "KCK"], {"K": fiber, "C": advanced_circuit, "B": cell, "F": f"{NS}:basic_frame"})
    shaped("elite_frame", ["KDK", "CFC", "KEK"], {"K": fiber, "D": "minecraft:diamond", "C": elite_circuit,
                                                 "F": f"{NS}:advanced_frame", "E": "minecraft:ender_eye"})

    shaped("harvester_module", ["IHI", "CDC"], {"I": iron, "H": "minecraft:iron_hoe", "C": copper, "D": circuit})
    shaped("quarry_module", ["IPI", "CDC"], {"I": iron, "P": "minecraft:diamond_pickaxe", "C": copper, "D": circuit})
    shaped("transport_module", ["IHI", "CDC"], {"I": iron, "H": "minecraft:hopper", "C": copper, "D": circuit})
    shaped("planter_module", ["IXI", "CDC"], {"I": iron, "X": "minecraft:dispenser", "C": copper, "D": circuit})
    shaped("logger_module", ["IXI", "CDC"], {"I": iron, "X": "minecraft:iron_axe", "C": copper, "D": circuit})
    shaped("taxi_module", ["IXI", "CDC"], {"I": iron, "X": "minecraft:lead", "C": copper, "D": circuit})
    shaped("rancher_module", ["IXI", "CDC"], {"I": iron, "X": "minecraft:shears", "C": copper, "D": circuit})
    shaped("fertilizer_module", ["IXI", "CDC"], {"I": iron, "X": "minecraft:bone_block", "C": copper, "D": circuit})
    shaped("builder_module", ["IXI", "CDC"], {"I": iron, "X": "minecraft:piston", "C": copper, "D": circuit})
    shaped("fisher_module", ["IXI", "CDC"], {"I": iron, "X": "minecraft:fishing_rod", "C": copper, "D": circuit})
    shaped("breeder_module", ["IXI", "CDC"], {"I": iron, "X": "minecraft:hay_block", "C": copper, "D": circuit})
    # Defence: an amethyst lens focuses the laser
    shaped("guard_module", ["IXI", "CDC"], {"I": iron, "X": "minecraft:amethyst_shard", "C": copper, "D": circuit})
    shaped("bomb_module", ["IXI", "CDC"], {"I": iron, "X": "minecraft:tnt", "C": copper, "D": circuit})

    # Addons share a chip base (gold nuggets, redstone, copper) with one themed ingredient
    def addon(name, special):
        shaped(name, ["NXN", "RCR"], {"N": "minecraft:gold_nugget", "X": special, "R": redstone, "C": circuit})

    addon("speed_addon", "minecraft:sugar")
    addon("stack_addon", "minecraft:chest")
    addon("battery_addon", cell)
    addon("fortune_addon", "minecraft:emerald")
    addon("silk_touch_addon", "minecraft:diamond")
    addon("anchor_addon", "minecraft:ender_eye")

    shaped("combustion_generator", ["III", "IRI", "CFC"], {"I": iron, "R": redstone, "C": copper, "F": "minecraft:furnace"})
    shaped("drone_assembler", ["IDI", "CTC", "III"], {"I": iron, "D": circuit, "C": copper, "T": "minecraft:crafting_table"})
    shaped("drone_dock", ["CDC", "III"], {"C": copper, "D": circuit, "I": iron})
    shaped("swarm_dock", ["CFC", "FDF", "CFC"], {"C": copper, "F": f"{NS}:cyber_frame", "D": f"{NS}:drone_dock"})
    shaped("hologram_table", ["GGG", "IDI", " I "], {"G": "minecraft:light_blue_stained_glass_pane", "I": iron, "D": circuit})
    shaped("energy_conduit", ["CCC", "RRR", "CCC"], {"C": copper, "R": redstone}, count=8)
    shaped("gas_pipe", ["CCC", "GGG", "CCC"], {"C": copper, "G": "minecraft:glass"}, count=8)
    shaped("liquid_pipe", ["III", "GGG", "III"], {"I": iron, "G": "minecraft:glass"}, count=8)
    # Higher tiers: 8 pipes around an upgrade core
    for kind in ("energy_conduit", "gas_pipe", "liquid_pipe"):
        shaped(f"advanced_{kind}", ["PPP", "GRG", "PPP"], {"P": f"{NS}:{kind}", "G": "minecraft:gold_ingot", "R": advanced_circuit}, count=8)
        shaped(f"elite_{kind}", ["PPP", "DUD", "PPP"], {"P": f"{NS}:advanced_{kind}", "D": "minecraft:diamond", "U": elite_circuit}, count=8)
    shaped("cyber_frame", ["ICI", "C C", "ICI"], {"I": iron, "C": copper}, count=8)
    shaped("bio_digester", ["IUI", "GXG", "ICI"], {"I": iron, "U": circuit, "C": copper, "G": "minecraft:glass", "X": "minecraft:composter"})
    shaped("gas_turbine", ["ICI", "RFR", "IDI"], {"I": iron, "C": copper, "R": redstone, "F": "minecraft:furnace", "D": circuit})
    shaped("acid_inlet", ["F", "B"], {"F": f"{NS}:cyber_frame", "B": "minecraft:bucket"})
    shaped("methane_outlet", ["F", "H"], {"F": f"{NS}:cyber_frame", "H": "minecraft:hopper"})
    shaped("intake_gate", ["IFI", "PDP"], {"I": iron, "F": f"{NS}:cyber_frame", "P": "minecraft:piston", "D": "minecraft:iron_door"})
    shaped("methane_extractor", ["IHI", "CDC", "IGI"], {"I": iron, "H": "minecraft:hopper", "C": copper, "D": circuit, "G": "minecraft:glass"})
    # Nuclear
    uranium = f"{NS}:uranium_ingot"
    casing = f"{NS}:reactor_casing"
    for source, suffix in ((f"{NS}:raw_uranium", ""), (f"{NS}:uranium_ore", "_from_ore"), (f"{NS}:deepslate_uranium_ore", "_from_deepslate_ore")):
        for kind, time in (("smelting", 200), ("blasting", 100)):
            write(DATA / f"recipe/uranium_ingot{suffix}_{kind}.json", {
                "type": f"minecraft:{kind}", "category": "misc", "cookingtime": time, "experience": 1.0,
                "group": "uranium_ingot", "ingredient": source, "result": {"id": uranium}})
    shaped("fuel_rod", ["IUI", "IUI", "IUI"], {"I": iron, "U": uranium})
    shaped("reactor_casing", ["CIC", "I I", "CIC"], {"C": "minecraft:light_gray_concrete", "I": iron}, count=8)
    shaped("reactor_port", ["H", "X", "B"], {"H": "minecraft:hopper", "X": casing, "B": "minecraft:bucket"})
    shaped("reactor_controller", ["IUI", "DXD", "IRI"],
           {"I": iron, "U": uranium, "D": "minecraft:diamond", "X": casing, "R": elite_circuit})
    turbine_casing = f"{NS}:turbine_casing"
    shaped("turbine_casing", ["CIC", "I I", "CIC"], {"C": copper, "I": iron}, count=8)
    shaped("turbine_port", ["E", "X", "B"], {"E": f"{NS}:energy_conduit", "X": turbine_casing, "B": "minecraft:bucket"})
    shaped("turbine_controller", ["ICI", "RTR", "IXI"],
           {"I": iron, "C": elite_circuit, "R": redstone, "T": f"{NS}:gas_turbine", "X": turbine_casing})
    shaped("multiblock_projector", ["GLG", "IRI", " I "],
           {"G": "minecraft:glass_pane", "L": "minecraft:glowstone_dust", "I": iron, "R": redstone}, category="equipment")
    shaped("liquid_tank", ["IGI", "GBG", "IGI"], {"I": iron, "G": "minecraft:glass", "B": "minecraft:bucket"})
    shaped("gas_tank", ["CGC", "GBG", "CGC"], {"C": copper, "G": "minecraft:glass", "B": "minecraft:bucket"})
    shaped("laser_turret", ["IAI", "RDR", "IBI"], {"I": iron, "A": "minecraft:amethyst_shard", "R": redstone, "D": advanced_circuit, "B": cell})
    shaped("water_pump", ["IBI", "CPC", "IDI"], {"I": iron, "B": "minecraft:bucket", "C": copper, "P": "minecraft:piston", "D": advanced_circuit})
    wool, lead_lining = "minecraft:yellow_wool", iron
    shaped("hazmat_helmet", ["WWW", "WGW"], {"W": wool, "G": "minecraft:glass_pane"}, category="equipment")
    shaped("hazmat_suit", ["W W", "WIW", "WWW"], {"W": wool, "I": lead_lining}, category="equipment")
    shaped("hazmat_pants", ["WWW", "W W", "W W"], {"W": wool}, category="equipment")
    shaped("hazmat_boots", ["W W", "L L"], {"W": wool, "L": "minecraft:leather"}, category="equipment")
    shaped("geiger_counter", [" C ", "IGI", "IRI"], {"C": copper, "I": iron, "G": "minecraft:glass_pane", "R": redstone}, category="equipment")
    shaped("waste_reprocessor", ["ICI", "BXB", "IDI"],
           {"I": iron, "C": copper, "B": "minecraft:blast_furnace", "X": "minecraft:glass", "D": advanced_circuit})
    shaped("coolant_mixer", ["IBI", "GXG", "IDI"],
           {"I": iron, "B": "minecraft:bucket", "G": "minecraft:glass", "X": "minecraft:cauldron", "D": advanced_circuit})

    # Acid: sulfur into water; the chamber walls are framed glass
    shaped("acid_synthesizer", ["ICI", "GXG", "IDI"],
           {"I": iron, "C": copper, "G": "minecraft:glass", "X": "minecraft:cauldron", "D": circuit})
    shaped("containment_glass", ["IGI", "GOG", "IGI"], {"I": "minecraft:iron_nugget", "G": "minecraft:glass", "O": "minecraft:obsidian"}, count=4)
    # Steam Engine: boiler, cylinders and the dynamo closing the row
    shaped("steam_engine", ["ICI", "BFB", "IDI"],
           {"I": iron, "C": copper, "B": "minecraft:blast_furnace", "F": f"{NS}:cyber_frame", "D": advanced_circuit})
    shaped("steam_engine_cylinder", ["IPI", "I I", "ICI"], {"I": iron, "P": "minecraft:piston", "C": copper}, count=2)
    shaped("steam_engine_dynamo", ["ICI", "CRC", "IBI"], {"I": iron, "C": copper, "R": "minecraft:redstone_block", "B": cell})

    # Mech: end game. Its core is a Fusion Crystal (reactor waste -> Isotope Shard -> Crystallizer) in two elite frames
    advanced, elite = f"{NS}:advanced_frame", f"{NS}:elite_frame"
    shaped("crystallizer", ["DAD", "GXG", "DCD"], {"D": "minecraft:diamond", "A": "minecraft:amethyst_block",
                                                   "G": f"{NS}:containment_glass", "X": f"{NS}:acid_synthesizer", "C": elite_circuit})
    shaped("crystallizer_port", ["IHI", "EXL", "III"], {"I": iron, "H": "minecraft:hopper", "E": f"{NS}:energy_conduit",
                                                       "X": f"{NS}:cyber_frame", "L": f"{NS}:liquid_pipe"})
    shaped("mech_core", ["DBD", "EXE", "DBD"], {"D": "minecraft:diamond", "B": cell, "E": elite, "X": f"{NS}:fusion_crystal"})
    shaped("mech_frame", ["IEI", "AMA", "IRI"], {"I": "minecraft:iron_block", "E": "minecraft:ender_eye", "A": advanced,
                                                 "M": f"{NS}:mech_core", "R": "minecraft:redstone_block"})

    # Mech Hangar and modules: mid to late game, an advanced frame in each
    shaped("mech_hangar", ["IGI", "CDC", "IFI"], {"I": "minecraft:iron_block", "G": "minecraft:glass_pane", "C": copper,
                                                  "D": advanced_circuit, "F": advanced})
    shaped("hangar_frame", ["IWI", "W W", "IWI"], {"I": iron, "W": "minecraft:white_concrete"}, count=8)
    shaped("hangar_pad", ["CRC", "RBR", "CRC"], {"C": copper, "R": redstone, "B": cell}, count=3)
    shaped("mech_jet_module", ["M M", "RAR", "B B"], {"M": "minecraft:phantom_membrane", "R": rotor, "A": advanced, "B": "minecraft:blaze_rod"})
    shaped("mech_drill_module", [" D ", "DAD", "ICI"], {"D": "minecraft:diamond", "A": advanced, "I": "minecraft:iron_block", "C": advanced_circuit})
    shaped("mech_blade_module", ["  S", " S ", "DA "], {"S": "minecraft:amethyst_shard", "D": "minecraft:diamond", "A": advanced})
    shaped("mech_armor_module", ["IOI", "OAO", "IOI"], {"I": "minecraft:iron_block", "O": "minecraft:obsidian", "A": advanced})

    # Energy storage: the shell is many cheap cells, the pylons come in pairs
    shaped("resonance_cell", ["GCG", "CBC", "GCG"], {"G": "minecraft:glass", "C": copper, "B": cell}, count=4)
    shaped("resonance_core", ["GAG", "IRI", "IFI"], {"G": "minecraft:glass", "A": "minecraft:amethyst_block", "I": iron,
                                                     "R": advanced_circuit, "F": f"{NS}:cyber_frame"})
    shaped("stabilizer_pylon", [" A ", "IFI", "IRI"], {"A": "minecraft:amethyst_shard", "I": iron, "F": f"{NS}:cyber_frame", "R": redstone}, count=2)

    shaped("muffler", ["WIW", "INI", "WIW"], {"W": "#minecraft:wool", "I": iron, "N": "minecraft:note_block"})
    shaped("mob_capsule",["IGI", "GEG", "IGI"], {"I": "minecraft:iron_nugget", "G": "minecraft:glass", "E": "minecraft:ender_pearl"})
    shaped("survey_tool", [" G ", "IRI", " I "], {"G": "minecraft:glass_pane", "I": iron, "R": redstone}, category="equipment")

    # The handbook is a GuideME guide: only craftable when GuideME is installed.
    write(DATA / "recipe/handbook.json", {
        "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "guideme"}],
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": ["minecraft:book", copper],
        "result": {"id": "guideme:guide", "components": {"guideme:guide_id": f"{NS}:handbook"}},
    })


# --------------------------------------------------------------------------- sounds

def sounds():
    """CyberFarm sound events, built from vanilla audio files with their own pitch and volume."""
    def files(path, count=1, **attrs):
        names = [path] if count == 1 else [f"{path}{i}" for i in range(1, count + 1)]
        return [{"name": f"minecraft:{n}", **attrs} for n in names]

    events = {
        "drone.fly": [{"name": f"{NS}:drone_fly", "volume": 0.6}],  # synthesized by soundgen.py
        "drone.takeoff": files("block/beacon/power1", pitch=1.8, volume=0.4),
        "drone.land": files("block/iron_trapdoor/close", 4, pitch=1.5, volume=0.5),
        "drone.unfold": files("tile/piston/out", pitch=1.7, volume=0.4) + files("block/iron_trapdoor/open", 4, pitch=1.8, volume=0.4),
        "methane_extractor.work": files("block/bubble_column/upwards_ambient", 5, pitch=0.7, volume=0.5)
                                  + files("random/fizz", pitch=0.6, volume=0.15),
        "methane_extractor.activate": files("block/beacon/activate", pitch=1.2),
        "methane_extractor.deactivate": files("block/beacon/deactivate", pitch=1.2),
        "combustion_generator.crackle": files("block/furnace/fire_crackle", 5, pitch=1.2, volume=0.6),
        "drone_assembler.work": files("block/smithing_table/smithing_table", 3),
        "reactor.activate": files("block/beacon/activate", pitch=0.6),
        "reactor.deactivate": files("block/beacon/deactivate", pitch=0.6),
        "reactor.hum": files("block/beacon/ambient", pitch=0.5, volume=0.9),
        "reactor.alarm": files("note/pling", pitch=1.6, volume=1.0) + files("note/bell", pitch=1.4, volume=0.8),
        "coolant_mixer.work": files("block/brewing_stand/brew", 2, pitch=1.3, volume=0.4),
        "waste_reprocessor.work": files("block/beacon/power1", pitch=0.7, volume=0.5),
        "geiger.click": files("random/click", pitch=2.0, volume=0.3),
        "steam_turbine.work": files("random/fizz", pitch=0.5, volume=0.25),
        "crystallizer.work": files("block/amethyst/resonate", 4, pitch=0.8, volume=0.5) + files("block/amethyst/shimmer", pitch=0.9, volume=0.6),
        "crystallizer.done": files("block/amethyst_cluster/place", 4, pitch=0.7) + files("block/beacon/power1", pitch=1.6, volume=0.5),
        # Mech: synthesized servos, jet, drill, blade and boot chime (soundgen.py), vanilla metal for the rest
        "mech_hangar.arm": [{"name": f"{NS}:mech_servo", "pitch": 1.3, "volume": 0.6}],
        "mech_hangar.fit": files("block/vault/insert", pitch=0.8) + files("block/iron_door/close", 4, pitch=1.2, volume=0.6),
        "mech.servo": [{"name": f"{NS}:mech_servo", "volume": 0.8}],
        "mech.hiss": files("random/fizz", pitch=0.5, volume=0.6),
        "mech.clunk": files("block/iron_door/close", 4, pitch=0.6),
        "mech.step": files("mob/irongolem/walk", 4, pitch=0.6, volume=0.9) + files("block/heavy_core/step", 4, pitch=0.5, volume=0.7),
        "mech.jet": [{"name": f"{NS}:mech_jet", "volume": 0.8}],
        "mech.drill": [{"name": f"{NS}:mech_drill", "volume": 0.6}],
        "mech.blade_on": [{"name": f"{NS}:mech_blade_on", "volume": 0.8}],
        "mech.blade_off": [{"name": f"{NS}:mech_blade_off", "volume": 0.8}],
        "mech.punch": files("mob/irongolem/hit", 4, pitch=0.7) + files("entity/player/attack/strong", 6, pitch=0.6, volume=0.8),
        "mech.slash": files("entity/player/attack/sweep", 7, pitch=0.7) + files("item/trident/riptide", 3, pitch=1.6, volume=0.4),
        "mech.boot": [{"name": f"{NS}:mech_boot", "volume": 0.6}],
        "mech.shutdown": files("block/beacon/deactivate", pitch=0.5),
    }
    write(ASSETS / "sounds.json", {name: {"sounds": entries, "subtitle": f"subtitles.{NS}.{name}"}
                                   for name, entries in events.items()})


# --------------------------------------------------------------------------- advancements

def _advancement(name, parent, icon, criteria, frame="task"):
    display = {
        "icon": {"id": f"{NS}:{icon}"},
        "title": {"translate": f"advancements.{NS}.{name}.title"},
        "description": {"translate": f"advancements.{NS}.{name}.description"},
        "frame": frame,
        "show_toast": parent is not None,
        "announce_to_chat": parent is not None,
    }
    data = {"display": display, "criteria": criteria, "requirements": [[key] for key in criteria]}
    if parent:
        data["parent"] = f"{NS}:{parent}"
    else:
        # The tab's background: the mod's machine plating, tiled
        display["background"] = f"{NS}:block/machine_side"
    write(DATA / f"advancement/{name}.json", data)


def _has(*items):
    """All these items in the inventory at once."""
    return {"get": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": i if i.startswith("#") else f"{NS}:{i}"} for i in items]}}}


def _placed(block):
    return {"place": {"trigger": "minecraft:placed_block", "conditions": {"location": [
        {"condition": "minecraft:location_check", "predicate": {"block": {"blocks": f"{NS}:{block}"}}}]}}}


def _used_on(item, block):
    return {"use": {"trigger": "minecraft:item_used_on_block", "conditions": {"location": [
        {"condition": "minecraft:location_check", "predicate": {"block": {"blocks": f"{NS}:{block}"}}},
        {"condition": "minecraft:match_tool", "predicate": {"items": f"{NS}:{item}"}}]}}}


def _rides(entity):
    return {"ride": {"trigger": "minecraft:started_riding", "conditions": {"player": [
        {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"vehicle": {"entity_type": f"{NS}:{entity}"}}}]}}}


def advancements():
    """The CyberFarm tab: three branches from the Drone Core, drones, power, and nuclear up to the mech."""
    _advancement("root", None, "drone_core", _has("drone_core"))
    # Materials: aluminum and circuits, carbon fiber, then the elite circuits
    _advancement("aluminum", "root", "aluminum_ingot", _has("#c:ingots/aluminum"))
    _advancement("circuits", "aluminum", "basic_circuit", _placed("fabricator"))
    _advancement("advanced_circuit", "circuits", "advanced_circuit", _has("advanced_circuit"))
    _advancement("carbon_fiber", "advanced_circuit", "carbon_fiber", _has("carbon_fiber"))
    _advancement("elite_circuit", "carbon_fiber", "elite_circuit", _has("elite_circuit"), "goal")
    _advancement("supply_contracts", "circuits", "supply_terminal", _placed("supply_terminal"))
    # Drones
    _advancement("assemble_drone", "root", "drone", _has("drone"))
    _advancement("deploy_drone", "assemble_drone", "drone_dock", _used_on("drone", "drone_dock"))
    _advancement("survey_tool", "deploy_drone", "survey_tool", _has("survey_tool"))
    _advancement("taxi", "deploy_drone", "taxi_module", _rides("drone"))
    _advancement("elite_frame", "assemble_drone", "elite_frame", _has("elite_frame"), "goal")
    _advancement("guard", "deploy_drone", "guard_module", _has("guard_module"))
    _advancement("laser_turret", "guard", "laser_turret", _placed("laser_turret"))
    _advancement("bomb", "guard", "bomb_module", _has("bomb_module"))
    _advancement("swarm", "deploy_drone", "swarm_dock", _used_on("drone", "swarm_dock"))
    _advancement("construction", "swarm", "multiblock_projector", _used_on("multiblock_projector", "swarm_dock"), "goal")
    # Power
    _advancement("power_on", "root", "combustion_generator", _placed("combustion_generator"))
    _advancement("gas_power", "power_on", "gas_turbine", _placed("gas_turbine"))
    _advancement("steam_engine", "gas_power", "steam_engine", _placed("steam_engine"))
    _advancement("energy_storage", "power_on", "resonance_core", _placed("resonance_core"), "goal")
    # Nuclear, then the mech
    _advancement("uranium", "power_on", "raw_uranium", _has("raw_uranium"))
    _advancement("reactor", "uranium", "reactor_controller", _placed("reactor_controller"))
    _advancement("spent_fuel", "reactor", "depleted_fuel_rod", _has("depleted_fuel_rod"), "goal")
    _advancement("hazmat", "reactor", "hazmat_suit", _has("hazmat_helmet", "hazmat_suit", "hazmat_pants", "hazmat_boots"))
    _advancement("fusion_crystal", "spent_fuel", "fusion_crystal", _has("fusion_crystal"))
    _advancement("mech_core", "fusion_crystal", "mech_core", _has("mech_core"))
    _advancement("pilot_mech", "mech_core", "mech_frame", _rides("mech"), "challenge")


# --------------------------------------------------------------------------- villagers

# The Drone Mechanist's trades by level: (name, wants, wanted count, gives, given count, max uses, xp)
MECHANIST_TRADES = {
    1: [("basic_circuit", "minecraft:emerald", 1, f"{NS}:basic_circuit", 2, 12, 2),
        ("redstone_emerald", "minecraft:redstone", 20, "minecraft:emerald", 1, 16, 2)],
    2: [("battery_cell", "minecraft:emerald", 3, f"{NS}:battery_cell", 2, 12, 10),
        ("copper_emerald", "minecraft:copper_ingot", 10, "minecraft:emerald", 1, 16, 10)],
    3: [("advanced_circuit", "minecraft:emerald", 6, f"{NS}:advanced_circuit", 1, 8, 20),
        ("aluminum_ingot", "minecraft:emerald", 4, f"{NS}:aluminum_ingot", 4, 12, 15)],
    4: [("speed_addon", "minecraft:emerald", 10, f"{NS}:speed_addon", 1, 4, 25),
        ("stack_addon", "minecraft:emerald", 12, f"{NS}:stack_addon", 1, 4, 25),
        ("carbon_fiber", "minecraft:emerald", 8, f"{NS}:carbon_fiber", 4, 8, 20)],
    5: [("elite_circuit", "minecraft:emerald", 24, f"{NS}:elite_circuit", 1, 4, 30),
        ("anchor_addon", "minecraft:emerald", 16, f"{NS}:anchor_addon", 1, 4, 30)],
}


def villagers():
    """The Drone Mechanist's trades: one file a trade, a tag a level, and the trade set that picks two of them."""
    # Unemployed villagers only look for job sites in this tag
    write(RES / "data/minecraft/tags/point_of_interest_type/acquirable_job_site.json", {"replace": False, "values": [f"{NS}:supply_terminal"]})
    for level, trades in MECHANIST_TRADES.items():
        ids = []
        for name, wants, wants_count, gives, gives_count, max_uses, xp in trades:
            trade = {"wants": {"id": wants, **({"count": wants_count} if wants_count > 1 else {})},
                     "gives": {"id": gives, **({"count": gives_count} if gives_count > 1 else {})},
                     "max_uses": float(max_uses), "reputation_discount": 0.05, "xp": float(xp)}
            write(DATA / f"villager_trade/drone_mechanist/{level}/{name}.json", trade)
            ids.append(f"{NS}:drone_mechanist/{level}/{name}")
        write(DATA / f"tags/villager_trade/drone_mechanist/level_{level}.json", {"values": ids})
        write(DATA / f"trade_set/drone_mechanist/level_{level}.json", {
            "amount": 2.0, "random_sequence": f"{NS}:trade_set/drone_mechanist/level_{level}",
            "trades": f"#{NS}:drone_mechanist/level_{level}"})


def outpost_worldgen():
    """The Drone Mechanist's Outpost: his cabin (tools/villagegen.py), alone in the grasslands and
    woods of the Overworld. Rare, a thousand blocks and more apart: a whole village grows from one."""
    write(DATA / "worldgen/template_pool/village/outpost.json", {"fallback": "minecraft:empty", "elements": [{"weight": 1, "element": {
        "element_type": "minecraft:single_pool_element", "location": f"{NS}:village/outpost",
        "processors": {"processors": []}, "projection": "rigid"}}]})
    write(DATA / "worldgen/structure/mechanist_outpost.json", {
        "type": "minecraft:jigsaw", "biomes": f"#{NS}:has_structure/mechanist_outpost", "step": "surface_structures",
        "start_pool": f"{NS}:village/outpost", "size": 1, "start_height": {"absolute": -1},
        "project_start_to_heightmap": "WORLD_SURFACE_WG", "max_distance_from_center": 80, "use_expansion_hack": False,
        "terrain_adaptation": "beard_thin", "spawn_overrides": {}})
    write(DATA / "worldgen/structure_set/mechanist_outposts.json", {
        "structures": [{"structure": f"{NS}:mechanist_outpost", "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": 80, "separation": 32, "salt": 1717_0414}})
    write(DATA / "tags/worldgen/biome/has_structure/mechanist_outpost.json", {"values": [
        "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:savanna", "minecraft:forest",
        "minecraft:birch_forest", "minecraft:flower_forest", "minecraft:taiga", "minecraft:snowy_plains"]})


if __name__ == "__main__":
    villagers()
    outpost_worldgen()
    blocks()
    items()
    recipes()
    sounds()
    advancements()
    print("assets and data written")
