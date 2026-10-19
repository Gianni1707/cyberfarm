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
    "basic_frame", "advanced_frame", "elite_frame",
    "harvester_module", "quarry_module", "transport_module", "planter_module", "logger_module", "taxi_module",
    "speed_addon", "stack_addon", "battery_addon", "fortune_addon", "silk_touch_addon",
    "drone", "survey_tool", "mob_capsule_filled",
    "raw_uranium", "uranium_ingot", "fuel_rod", "depleted_fuel_rod",
    "biogas_bucket", "methane_bucket", "steam_bucket", "coolant_bucket", "cryo_coolant_bucket",
    "hazmat_helmet", "hazmat_suit", "hazmat_pants", "hazmat_boots", "geiger_counter",
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


def pipe_models(name):
    """Core, arm and inventory models plus a multipart blockstate for a NetworkPipeBlock."""
    texture = {"particle": tex(name), "conduit": tex(name)}
    all_faces = lambda: {d: {"texture": "#conduit"} for d in ("north", "east", "south", "west", "up", "down")}
    core = {"from": [5, 5, 5], "to": [11, 11, 11], "faces": all_faces()}
    # Arm pointing north; the blockstate rotates it to every other side
    arm = {"from": [6, 6, 0], "to": [10, 10, 5], "faces": {
        "north": {"texture": "#conduit", "cullface": "north"},
        "east": {"texture": "#conduit"}, "west": {"texture": "#conduit"},
        "up": {"texture": "#conduit"}, "down": {"texture": "#conduit"},
    }}
    write(ASSETS / f"models/block/{name}_core.json", {"parent": "minecraft:block/block", "textures": texture, "elements": [core]})
    write(ASSETS / f"models/block/{name}_side.json", {"textures": texture, "elements": [arm]})
    # Item: core with arms east and west
    write(ASSETS / f"models/block/{name}_inventory.json", {
        "parent": "minecraft:block/block", "textures": texture,
        "elements": [core,
                     {"from": [0, 6, 6], "to": [5, 10, 10], "faces": all_faces()},
                     {"from": [11, 6, 6], "to": [16, 10, 10], "faces": all_faces()}],
    })
    rotations = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
    parts = [{"apply": {"model": f"{NS}:block/{name}_core"}}]
    for direction, rotation in rotations.items():
        parts.append({"when": {direction: "true"}, "apply": {"model": f"{NS}:block/{name}_side", **rotation}})
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
    orientable("combustion_generator", "combustion_generator_front", "combustion_generator_top")
    orientable("combustion_generator_on", "combustion_generator_front_on", "combustion_generator_top")
    facing_blockstate("combustion_generator", {"lit=false": "combustion_generator", "lit=true": "combustion_generator_on"})

    orientable("drone_assembler", "drone_assembler_front", "drone_assembler_top", side="drone_assembler_side")
    facing_blockstate("drone_assembler")

    dock_model()
    facing_blockstate("drone_dock")

    pipe_models("energy_conduit")
    pipe_models("gas_pipe")

    orientable("bio_digester", "bio_digester_front", "bio_digester_top")
    facing_blockstate("bio_digester")
    orientable("gas_turbine", "gas_turbine_front", "gas_turbine_top")
    facing_blockstate("gas_turbine")

    # The extractor is the chamber's "shower head": its bottom face looks into the chamber
    for model, suffix in (("methane_extractor", ""), ("methane_extractor_formed", "_formed")):
        side = tex("methane_extractor_side" + suffix)
        write(ASSETS / f"models/block/{model}.json", {
            "parent": "minecraft:block/cube",
            "textures": {"particle": side, "up": tex("methane_extractor_top" + suffix),
                         "down": tex("methane_extractor_bottom" + suffix),
                         "north": side, "east": side, "south": side, "west": side},
        })
    write(ASSETS / "blockstates/methane_extractor.json", {"variants": {
        f"facing={f},formed={formed}": {"model": f"{NS}:block/methane_extractor{'_formed' if formed == 'true' else ''}"}
        for f in FACING_ROTATION for formed in ("false", "true")}})

    for model in ("cyber_frame", "cyber_frame_formed"):
        write(ASSETS / f"models/block/{model}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": tex(model)}})
    write(ASSETS / "blockstates/cyber_frame.json", {"variants": {
        "formed=false": {"model": f"{NS}:block/cyber_frame"},
        "formed=true": {"model": f"{NS}:block/cyber_frame_formed"}}})

    # Soft block, not a machine: no pickaxe needed
    write(ASSETS / "models/block/muffler.json", {"parent": "minecraft:block/cube_all", "textures": {"all": tex("muffler")}})
    facing_blockstate("muffler")
    write(ASSETS / "items/muffler.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/muffler"}})
    block_loot("muffler")

    nuclear_blocks()
    coolant_blocks()

    names = ["combustion_generator", "drone_assembler", "drone_dock", "energy_conduit", "gas_pipe",
             "bio_digester", "gas_turbine", "methane_extractor", "cyber_frame",
             "reactor_controller", "reactor_casing", "reactor_port", "steam_turbine", "water_pump", "coolant_mixer",
             "waste_reprocessor", "vitrified_waste"]
    for name in names:
        if not name.endswith(("_conduit", "_pipe")):
            write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/{name}"}})
        block_loot(name)

    # Common fluid tags, so other mods recognise our gases
    write(RES / "data/c/tags/fluid/biogas.json", {"values": [f"{NS}:biogas"]})
    write(RES / "data/c/tags/fluid/methane.json", {"values": [f"{NS}:methane"]})
    ores = ["uranium_ore", "deepslate_uranium_ore"]
    write(RES / "data/minecraft/tags/block/mineable/pickaxe.json", {"values": [f"{NS}:{n}" for n in names + ores]})
    write(RES / "data/minecraft/tags/block/needs_iron_tool.json", {"values": [f"{NS}:{n}" for n in ores]})
    write(RES / "data/c/tags/fluid/steam.json", {"values": [f"{NS}:steam"]})
    write(RES / "data/c/tags/fluid/gaseous.json", {"values": [f"{NS}:biogas", f"{NS}:methane", f"{NS}:steam"]})
    # Common material tags, so other mods' machines process our uranium and ours take theirs
    for kind in ("block", "item"):
        write(RES / f"data/c/tags/{kind}/ores/uranium.json", {"values": [f"{NS}:{n}" for n in ores]})
        write(RES / f"data/c/tags/{kind}/ores.json", {"replace": False, "values": [f"#c:ores/uranium"]})
    write(RES / "data/c/tags/item/raw_materials/uranium.json", {"values": [f"{NS}:raw_uranium"]})
    write(RES / "data/c/tags/item/raw_materials.json", {"replace": False, "values": ["#c:raw_materials/uranium"]})
    write(RES / "data/c/tags/item/ingots/uranium.json", {"values": [f"{NS}:uranium_ingot"]})
    write(RES / "data/c/tags/item/ingots.json", {"replace": False, "values": ["#c:ingots/uranium"]})
    uranium_worldgen()


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
                variants[f"facing={facing},formed={formed},lit={lit}"] = {"model": f"{NS}:block/{model}", **({"y": rot} if rot else {})}
    write(ASSETS / "blockstates/reactor_controller.json", {"variants": variants})

    orientable("steam_turbine", "steam_turbine_front", "machine_top")
    facing_blockstate("steam_turbine")
    orientable("water_pump", "water_pump_front", "water_pump_top")
    facing_blockstate("water_pump")
    orientable("coolant_mixer", "coolant_mixer_front", "machine_top")
    facing_blockstate("coolant_mixer")
    orientable("waste_reprocessor", "waste_reprocessor_front", "machine_top")
    facing_blockstate("waste_reprocessor")
    write(ASSETS / "models/block/vitrified_waste.json", {"parent": "minecraft:block/cube_all", "textures": {"all": tex("vitrified_waste")}})
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
    write(RES / "data/minecraft/tags/damage_type/no_knockback.json", {"replace": False, "values": [f"{NS}:radiation"]})
    write(ASSETS / "equipment/hazmat.json", {"layers": {
        "humanoid": [{"texture": f"{NS}:hazmat"}],
        "humanoid_leggings": [{"texture": f"{NS}:hazmat"}],
    }})
    # The suit keeps the cold of coolant pools out, like leather in powder snow
    write(RES / "data/minecraft/tags/item/freeze_immune_wearables.json", {"replace": False, "values": [
        f"{NS}:hazmat_helmet", f"{NS}:hazmat_suit", f"{NS}:hazmat_pants", f"{NS}:hazmat_boots"]})


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

    shaped("drone_core", ["IRI", "RGR", "IRI"], {"I": iron, "R": redstone, "G": gold}, count=2)
    shaped("rotor", ["N N", " I ", "N N"], {"N": "minecraft:iron_nugget", "I": iron}, count=2)
    shaped("battery_cell", [" N ", "CRC", "CRC"], {"N": "minecraft:iron_nugget", "C": copper, "R": redstone})

    shaped("basic_frame", ["R R", " D ", "RBR"], {"R": rotor, "D": core, "B": cell})
    shaped("advanced_frame", ["GBG", "DFD", "GBG"], {"G": gold, "B": cell, "D": core, "F": f"{NS}:basic_frame"})
    shaped("elite_frame", ["XBX", "EFE", "XBX"], {"X": "minecraft:diamond", "B": cell, "E": "minecraft:ender_eye", "F": f"{NS}:advanced_frame"})

    shaped("harvester_module", ["IHI", "CDC"], {"I": iron, "H": "minecraft:iron_hoe", "C": copper, "D": core})
    shaped("quarry_module", ["IPI", "CDC"], {"I": iron, "P": "minecraft:diamond_pickaxe", "C": copper, "D": core})
    shaped("transport_module", ["IHI", "CDC"], {"I": iron, "H": "minecraft:hopper", "C": copper, "D": core})
    shaped("planter_module", ["IXI", "CDC"], {"I": iron, "X": "minecraft:dispenser", "C": copper, "D": core})
    shaped("logger_module", ["IXI", "CDC"], {"I": iron, "X": "minecraft:iron_axe", "C": copper, "D": core})
    shaped("taxi_module", ["IXI", "CDC"], {"I": iron, "X": "minecraft:lead", "C": copper, "D": core})

    # Addons share a chip base (gold nuggets, redstone, copper) with one themed ingredient
    def addon(name, special):
        shaped(name, ["NXN", "RCR"], {"N": "minecraft:gold_nugget", "X": special, "R": redstone, "C": copper})

    addon("speed_addon", "minecraft:sugar")
    addon("stack_addon", "minecraft:chest")
    addon("battery_addon", cell)
    addon("fortune_addon", "minecraft:emerald")
    addon("silk_touch_addon", "minecraft:diamond")

    shaped("combustion_generator", ["III", "IRI", "CFC"], {"I": iron, "R": redstone, "C": copper, "F": "minecraft:furnace"})
    shaped("drone_assembler", ["IDI", "CTC", "III"], {"I": iron, "D": core, "C": copper, "T": "minecraft:crafting_table"})
    shaped("drone_dock", ["CDC", "III"], {"C": copper, "D": core, "I": iron})
    shaped("energy_conduit", ["CCC", "RRR", "CCC"], {"C": copper, "R": redstone}, count=8)
    shaped("gas_pipe", ["CCC", "GGG", "CCC"], {"C": copper, "G": "minecraft:glass"}, count=8)
    shaped("cyber_frame", ["ICI", "C C", "ICI"], {"I": iron, "C": copper}, count=8)
    shaped("bio_digester", ["ICI", "GXG", "ICI"], {"I": iron, "C": copper, "G": "minecraft:glass", "X": "minecraft:composter"})
    shaped("gas_turbine", ["ICI", "RFR", "IDI"], {"I": iron, "C": copper, "R": redstone, "F": "minecraft:furnace", "D": core})
    shaped("methane_extractor", ["IHI", "CDC", "IGI"], {"I": iron, "H": "minecraft:hopper", "C": copper, "D": core, "G": "minecraft:glass"})
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
           {"I": iron, "U": uranium, "D": "minecraft:diamond", "X": casing, "R": "minecraft:redstone_block"})
    shaped("steam_turbine", ["ICI", "CTC", "ICI"], {"I": iron, "C": "minecraft:copper_block", "T": f"{NS}:gas_turbine"})
    shaped("water_pump", ["IBI", "CPC", "IDI"], {"I": iron, "B": "minecraft:bucket", "C": copper, "P": "minecraft:piston", "D": core})
    wool, lead_lining = "minecraft:yellow_wool", iron
    shaped("hazmat_helmet", ["WWW", "WGW"], {"W": wool, "G": "minecraft:glass_pane"}, category="equipment")
    shaped("hazmat_suit", ["W W", "WIW", "WWW"], {"W": wool, "I": lead_lining}, category="equipment")
    shaped("hazmat_pants", ["WWW", "W W", "W W"], {"W": wool}, category="equipment")
    shaped("hazmat_boots", ["W W", "L L"], {"W": wool, "L": "minecraft:leather"}, category="equipment")
    shaped("geiger_counter", [" C ", "IGI", "IRI"], {"C": copper, "I": iron, "G": "minecraft:glass_pane", "R": redstone}, category="equipment")
    shaped("waste_reprocessor", ["ICI", "BXB", "IDI"],
           {"I": iron, "C": copper, "B": "minecraft:blast_furnace", "X": "minecraft:glass", "D": core})
    shaped("coolant_mixer", ["IBI", "GXG", "IDI"],
           {"I": iron, "B": "minecraft:bucket", "G": "minecraft:glass", "X": "minecraft:cauldron", "D": core})

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
        "drone.fly": files("mob/bee/loop1", pitch=1.5, volume=0.5),
        "drone.takeoff": files("block/beacon/power1", pitch=1.8, volume=0.4),
        "drone.land": files("block/iron_trapdoor/close", 4, pitch=1.5, volume=0.5),
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
    }
    write(ASSETS / "sounds.json", {name: {"sounds": entries, "subtitle": f"subtitles.{NS}.{name}"}
                                   for name, entries in events.items()})


if __name__ == "__main__":
    blocks()
    items()
    recipes()
    sounds()
    print("assets and data written")
