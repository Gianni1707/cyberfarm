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
  - cyberfarm:builder_module
  - cyberfarm:fisher_module
  - cyberfarm:breeder_module
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
| Builder Module | +2 FE/t | 20 FE per block |
| Fisher Module | +1 FE/t | 10 FE per catch |
| Breeder Module | +1 FE/t | 15 FE per animal fed |

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
- In a deep pit it climbs out over the rim and drops straight down the shaft, so the dock can sit
  beside the pit. A layer is finished only when every block in it is dug: a block the drone could
  not reach is tried again on the next trip, never skipped.
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

## Builder Module

<ItemImage id="cyberfarm:builder_module" scale="2" float="left" />

The drone **builds a multiblock or a schematic** for you: the one you lock with the
[Multiblock Projector](multiblock_projector.md) and link to its dock. It fits **only a
[Mini Drone Frame](drone_frames.md)**: building is work for a swarm of Mini Drones on a
[Builder Swarm Dock](drone_dock.md).

<br clear="all" />

- Pick the structure in the projector's list, **sneak + right-click** a block to lock it there,
  then **right-click the dock** with the projector to link it. Right-click the dock with an
  unlocked projector to unlink it.
- Put the blocks in the dock: the drone takes one kind per trip, carrying them in its claw.
- It builds **one layer at a time**, from the bottom: a layer is finished before the next one
  starts. If the dock runs out of a block the current layer needs, the drone **waits** for it.
  The dock's **Build** tab shows the layer and the blocks it still needs; the ones the dock has
  none of are marked in red.
- It flies above the layer it is building, straight from block to block, and goes over the
  whole structure when something is in the way, so it never gets walled in.
- The dock **takes in only the blocks the build still needs**. Point a Transport drone's route
  from your storage to the dock and it brings exactly what is missing, and nothing else.
- It **never breaks blocks**: the dock screen counts the blocks in the way, and you clear them.
- Doors and beds go in whole. Blocks it cannot place (water, a lantern hanging under a ceiling)
  are listed in the Build tab at the end, for you to place by hand. Chests go in empty.
- Several Builder drones on different docks can share one build: link the same projection to
  each dock and they split the blocks between them.

<RecipeFor id="builder_module" />

## Fisher Module

<ItemImage id="cyberfarm:fisher_module" scale="2" float="left" />

The drone **fishes** in the water of its work area and brings the catch to the dock.

<br clear="all" />

- It needs **open water**: a water source block with air above it, inside the work area.
- It hovers over the water, waits for a bite like a fishing rod and pulls out what a rod would,
  with the same odds: mostly fish, some junk and now and then **treasure** (enchanted books, saddles,
  name tags...).
- It flies home when its cargo is full.

<RecipeFor id="fisher_module" />

## Breeder Module

<ItemImage id="cyberfarm:breeder_module" scale="2" float="left" />

The drone **feeds the animals** of its work area so they breed, with their food from the dock.

<br clear="all" />

- Put the food in the dock: wheat for cows and sheep, seeds for chickens, carrots for pigs, or
  whatever a modded animal eats. The drone takes the right food for each animal.
- It feeds an animal only when **two of a kind** are ready, so none of the food is wasted.
- It stops once there are **16 of a kind** in the work area, babies included, so the farm does not
  grow without end. Pairs well with a Rancher, or a Harvester for the crops they eat.

<RecipeFor id="breeder_module" />
