---
navigation:
  title: Function Modules
  parent: drone_systems.md
  position: 3
  icon: cyberfarm:harvester_module
item_ids:
  - cyberfarm:harvester_module
  - cyberfarm:quarry_module
  - cyberfarm:transport_module
  - cyberfarm:planter_module
  - cyberfarm:logger_module
  - cyberfarm:taxi_module
  - cyberfarm:rancher_module
  - cyberfarm:fertilizer_module
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
| Planter Module | +1 FE/t | 10 FE per seed |
| Logger Module | +2 FE/t | 20 FE per log, 5 FE per leaf block |
| Taxi Module | +2 FE/t | 50 FE per trip |
| Rancher Module | +1 FE/t | 15 FE per shearing or milking |
| Fertilizer Module | +1 FE/t | 10 FE per use |

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
- The [dock filter](drone_dock.md) decides which drops are **kept**; the others are thrown away.
- Harder blocks take longer and cost more power: about 25 FE for dirt, 35 FE for stone, 520 FE for obsidian.

<RecipeFor id="quarry_module" />

## Transport Module

<ItemImage id="cyberfarm:transport_module" scale="2" float="left" />

The drone carries items **from one container to another**: it picks up a load at the
**source**, flies to the **destination** and empties it there. It keeps going while the source
has items and the destination has room.

A load is **16 items**; each [Stack Addon](addons.md) adds a full stack (64 items), up to 9 stacks.
While loaded, the drone carries the item **in its claw**, so you can see what it is moving.

<br clear="all" />

Set the route with the [Survey Tool](drone_dock.md) in **Route** mode: click the source
container, then the destination. Any inventory works, including ones from other mods and **other drones' docks**: a
Transport drone can collect what a Harvester or a Quarry brings home. Items that do not fit in the
destination are brought back to the dock. The [dock filter](drone_dock.md) decides **which items**
are moved.

<RecipeFor id="transport_module" />

## Planter Module

<ItemImage id="cyberfarm:planter_module" scale="2" float="left" />

The drone plants seeds on **empty farmland** in its work area. It loads up to a stack of seeds
**from its dock** before each trip, so it works perfectly with a Harvester: harvested seeds go to
the dock and the Planter puts them back in the ground. You can also fill the dock with a hopper.

<br clear="all" />

- Plants anything that grows on farmland: wheat seeds, carrots, potatoes, beetroot seeds,
  melon and pumpkin seeds, and modded crops.
- It **never tills soil**: hoe the field once and it will keep it planted.
- Seeds it did not use go back to the dock.

<RecipeFor id="planter_module" />

## Logger Module

<ItemImage id="cyberfarm:logger_module" scale="2" float="left" />

The drone finds the **trees** in its work area (a log standing on soil), parks next to the trunk
and fells the whole tree **from the top down**, leaves included, so it also collects saplings
and apples. Then it **replants a sapling** where the trunk stood.

<br clear="all" />

- Works with every vanilla tree and with modded trees using the standard log and leaf tags.
- Leaves you placed yourself are never touched.
- Fortune gives more saplings and apples; Silk Touch collects the leaf blocks themselves.
- A big tree can drop more than the 9 cargo slots hold: the rest falls on the ground.

<RecipeFor id="logger_module" />

## Taxi Module

<ItemImage id="cyberfarm:taxi_module" scale="2" float="left" />

A personal air taxi. The drone **waits above its dock** with a cable and a hook. **Right-click
it** to grab the hook: you hang below it with one hand while it carries you to the dock's
**taxi destination**, then it flies home by itself.

<br clear="all" />

1. With the [Survey Tool](drone_dock.md) linked to the dock and in **Taxi** mode, **right-click
   the block** you want to land on (up to 256 blocks from the dock, with two blocks of air above it).
2. Right-click the waiting drone.
3. It climbs above the terrain, flies straight to the destination and puts you down gently.

- Trips are **one way**: put a second dock with its own Taxi drone at the destination to fly back.
- Before taking off the drone checks it has enough charge for the trip and the way back.
- **Sneak** to let go mid-flight: you get **Slow Falling**, so you land safely.
- Speed Addons make the trip faster.

<RecipeFor id="taxi_module" />

## Rancher Module

<ItemImage id="cyberfarm:rancher_module" scale="2" float="left" />

The drone looks after the animals in its work area: it **shears** every sheep (and mooshroom,
and any shearable mob from other mods) and **milks** cows and goats. Wool, milk and the rest go
back to the dock.

<br clear="all" />

- Milking needs **empty buckets**: put them in the dock, the drone takes up to 16 per trip and
  brings back the milk buckets.
- It chases the animal and hovers just above it, so fence the herd inside the work area.
- Sheep are sheared again once their wool grows back.

<RecipeFor id="rancher_module" />

## Fertilizer Module

<ItemImage id="cyberfarm:fertilizer_module" scale="2" float="left" />

The drone **grows the crops, stems and saplings** of its work area with fertilizer from the dock,
until they are fully grown. It works with **any fertilizer**: bone meal, or fertilizers from
other mods (everything in the `c:fertilizers` tag).

<br clear="all" />

- Put the fertilizer in the dock: the drone takes up to 64 per trip.
- Grass and moss are left alone, so your lawn does not fill with flowers.
- A fertilizer that does not work on a plant is not wasted: the drone skips that plant.
- Pairs well with a Harvester and a Planter on the same field.

<RecipeFor id="fertilizer_module" />
