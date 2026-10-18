---
navigation:
  title: Function Modules
  parent: index.md
  position: 5
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

## Harvester Module

<ItemImage id="cyberfarm:harvester_module" scale="2" float="left" />

The drone looks for **fully grown crops** in its work area, harvests them and **replants**
using one of the dropped seeds. It also harvests **melons and pumpkins**.

Works with wheat, carrots, potatoes, beetroot and any modded crop built like the vanilla ones.
Each harvest costs **40 FE**.

<br clear="all" />

<RecipeFor id="harvester_module" />

## Quarry Module

<ItemImage id="cyberfarm:quarry_module" scale="2" float="left" />

The drone **digs out the work area layer by layer**, from the top of the area all the way down
to bedrock. The dock GUI shows the layer it is working on.

<br clear="all" />

- It **only works with a surveyed area**: the default area around the dock is never dug,
  so your base is safe. Mark the area with the [Survey Tool](drone_dock.md).
- Blocks drop as if mined with a **diamond pickaxe**.
- It skips **liquids**, **unbreakable blocks** and anything with an inventory or a machine
  inside (chests, furnaces, CyberFarm machines).
- The column right under the Drone Dock is never dug, so the dock stays on a pillar.
- Harder blocks take longer and cost more power: from about 35 FE for dirt up to 520 FE for obsidian.

<RecipeFor id="quarry_module" />

## Transport Module

<ItemImage id="cyberfarm:transport_module" scale="2" float="left" />

The drone carries items **from one container to another**: it fills its 9 cargo slots at the
**source**, flies to the **destination** and empties them there. It keeps going while the source
has items and the destination has room.

<br clear="all" />

Set the route with the [Survey Tool](drone_dock.md): click the source container, then the
destination. Any inventory works, including ones from other mods. Items that do not fit in the
destination are brought back to the dock.

<RecipeFor id="transport_module" />
