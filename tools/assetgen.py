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
    "harvester_module", "quarry_module", "transport_module",
    "speed_addon", "stack_addon", "battery_addon", "fortune_addon", "silk_touch_addon",
    "drone", "survey_tool",
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

    names = ["combustion_generator", "drone_assembler", "drone_dock"]
    for name in names:
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/{name}"}})
        block_loot(name)
    write(RES / "data/minecraft/tags/block/mineable/pickaxe.json", {"values": [f"{NS}:{n}" for n in names]})


# --------------------------------------------------------------------------- items

def items():
    for name in FLAT_ITEMS:
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}})
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
    shaped("survey_tool", [" G ", "IRI", " I "], {"G": "minecraft:glass_pane", "I": iron, "R": redstone}, category="equipment")

    # The handbook is a GuideME guide: only craftable when GuideME is installed.
    write(DATA / "recipe/handbook.json", {
        "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "guideme"}],
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": ["minecraft:book", copper],
        "result": {"id": "guideme:guide", "components": {"guideme:guide_id": f"{NS}:handbook"}},
    })


if __name__ == "__main__":
    blocks()
    items()
    recipes()
    print("assets and data written")
