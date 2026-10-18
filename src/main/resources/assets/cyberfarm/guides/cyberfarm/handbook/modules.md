---
navigation:
  title: Function Modules
  parent: index.md
  position: 6
  icon: cyberfarm:harvester_module
item_ids:
  - cyberfarm:harvester_module
  - cyberfarm:quarry_module
  - cyberfarm:transport_module
---

# Function Modules

Modules tell a drone **what job to do**. Install them in the
[Drone Assembler](drone_assembler.md), together with a [frame](drone_frames.md).

A drone with more than one module **takes turns**: after each trip back to the dock it tries
the next module first, so no job is forgotten. You can see the installed modules on the drone
itself: each one adds its own tool under the hull.

## Power use

Every module makes the drone use more power in flight, and some jobs cost energy too:

| Module | Flight cost | Per job |
| --- | --- | --- |
| Harvester Module | +1 FE/t | 40 FE per harvest |
| Quarry Module | +2 FE/t | 20 FE + 10 FE per point of hardness, per block |
| Transport Module | +1 FE/t | none |

## Harvester Module

<ItemImage id="cyberfarm:harvester_module" scale="2" float="left" />

The drone looks for **fully grown crops** in its work area, harvests them and **replants**
using one of the dropped seeds. It also harvests **melons and pumpkins**.

Works with wheat, carrots, potatoes, beetroot and any modded crop built like the vanilla ones.
A [Fortune Addon](addons.md) gives more crops and seeds.

<br clear="all" />

<RecipeFor id="harvester_module" />

## Quarry Module

<ItemImage id="cyberfarm:quarry_module" scale="2" float="left" />

The drone **digs out the work area layer by layer**, from the top of the area all the way down
to bedrock. The dock GUI shows the layer it is working on.

<br clear="all" />

- It digs **in order**: row by row along the layer, zig-zagging, and every other layer it walks
  back the other way, so it never has to fly across the pit.
- It flies back to unload after **192 items** (three stacks).
- It **only works with a surveyed area**: the default area around the dock is never dug,
  so your base is safe. Mark the area with the [Survey Tool](drone_dock.md).
- Blocks drop as if mined with a **diamond pickaxe**. Add Fortune or Silk Touch with [addons](addons.md).
- It skips **liquids**, **unbreakable blocks** and anything with an inventory or a machine
  inside (chests, furnaces, CyberFarm machines).
- The column right under the Drone Dock is never dug, so the dock stays on a pillar.
- Harder blocks take longer and cost more power: about 25 FE for dirt, 35 FE for stone, 520 FE for obsidian.

<RecipeFor id="quarry_module" />

## Transport Module

<ItemImage id="cyberfarm:transport_module" scale="2" float="left" />

The drone carries items **from one container to another**: it picks up a load at the
**source**, flies to the **destination** and empties it there. It keeps going while the source
has items and the destination has room.

A load is **16 items**; each [Stack Addon](addons.md) adds a full stack (64 items), up to 9 stacks.

<br clear="all" />

Set the route with the [Survey Tool](drone_dock.md): click the source container, then the
destination. Any inventory works, including ones from other mods. Items that do not fit in the
destination are brought back to the dock.

<RecipeFor id="transport_module" />
