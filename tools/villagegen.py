"""Writes the tech village's structures (gzipped NBT, like tools/schemgen.py), drawn in
tools/village_buildings.py:

- data/cyberfarm/structure/village/outpost.nbt: the Drone Mechanist's Outpost as found in the
  world (worldgen data in tools/assetgen.py), his cabin with the Mechanist inside;
- data/cyberfarm/structure/schematic/village/<building>_<tier>.nbt: every building at each of its
  three tiers, for the Multiblock Projector once its blueprint disc is learned. A tier holds every
  block of the tiers before it, so projected over a built tier it asks only for what is new.

Run from the project root:  python tools/villagegen.py
With --preview <dir> it also draws each structure seen from the front-right, as <name>.png.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import schemgen as sg
from schemgen import Schematic, state, Int, Byte
import village_buildings as vb

DATA = sg.ROOT / "src/main/resources/data/cyberfarm/structure"
AIR = state("air")
CROPS = ("minecraft:wheat", "minecraft:carrots", "minecraft:potatoes", "minecraft:beetroots")

# The Mechanist waits by his rug: already in his profession (one point of experience keeps him in
# it until he finds the terminal) and never despawning
MECHANIST = {"id": "minecraft:villager", "PersistenceRequired": Byte(1), "Xp": Int(1),
             "VillagerData": {"type": "minecraft:plains", "profession": "cyberfarm:drone_mechanist", "level": Int(1)}}


def clear_above(s):
    """Air in every empty place above the ground, so the land and its trees make way."""
    w, h, d = s.size
    for x in range(w):
        for y in range(1, h):
            for z in range(d):
                if (x, y, z) not in s.blocks:
                    s.set(x, y, z, AIR)


def for_survival(s):
    """What a Builder drone may place from items a player can have: no grown crops (the farm plants
    its own), empty composters."""
    for pos, block in list(s.blocks.items()):
        if block[0] in CROPS:
            del s.blocks[pos]
        elif block[0] == "minecraft:composter":
            s.blocks[pos] = state("composter", level="0")


def outpost():
    s = Schematic(14, 12, 13)
    vb.cabin(s)
    clear_above(s)
    s.save("outpost", out=DATA / "village", entities=[(6.5, 1.0, 6.5, MECHANIST)])
    s.blocks = {pos: block for pos, block in s.blocks.items() if block != AIR}
    return "outpost", s


def buildings():
    """Every tier of every building; a tier that does not keep every block of the one before stops the run."""
    built = []
    for name, (size, tiers) in vb.BUILDINGS.items():
        before = {}
        for n in range(1, len(tiers) + 1):
            s = Schematic(*size)
            for tier in tiers[:n]:
                tier(s)
            for_survival(s)
            lost = [pos for pos in before if s.blocks.get(pos) != before[pos]]
            assert not lost, f"{name} tier {n} replaces blocks of tier {n - 1}: {lost[:5]}"
            before = dict(s.blocks)
            if name == "outpost" and n == 1:
                continue  # found in the world, never projected
            s.save(f"{name}_{n}", out=DATA / "schematic/village")
            built.append((f"{name}_{n}", s))
    return built


if __name__ == "__main__":
    built = [outpost()] + buildings()
    if "--preview" in sys.argv:
        folder = Path(sys.argv[sys.argv.index("--preview") + 1])
        folder.mkdir(parents=True, exist_ok=True)
        for name, s in built:
            s.resolved = {pos: block for pos, block in s.resolved.items() if block != AIR}
            sg.preview(s, folder / f"{name}.png")
