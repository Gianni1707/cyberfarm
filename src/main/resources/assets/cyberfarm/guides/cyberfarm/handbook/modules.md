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
  - cyberfarm:guard_module
  - cyberfarm:bomb_module
  - cyberfarm:sponge_module
  - cyberfarm:tank_module
---

# Function Modules

Modules tell a drone **what job to do**. Install them in the
[Drone Assembler](drone_assembler.md), together with a [frame](drone_frames.md). The frame decides
how many modules fit.

## All modules at a glance

Every module is crafted with a **Basic Circuit**. **Flight** is what the module adds to the drone's
power use while it flies; **per job** is the energy each piece of work costs on top.

| Module | Job | Flight | Per job | Frames |
| --- | --- | --- | --- | --- |
| Harvester | harvests grown crops | +2 FE/t | 80 FE per harvest | Basic, Advanced, Elite |
| Quarry | digs out the area layer by layer | +4 FE/t | 40 FE + 20 FE per point of hardness, per block | Basic, Advanced, Elite |
| Transport | carries items between two containers | +2 FE/t | none | Basic, Advanced, Elite |
| Planter | plants seeds on farmland | +2 FE/t | 20 FE per seed | Basic, Advanced, Elite |
| Logger | fells trees and replants them | +4 FE/t | 40 FE per log, 10 FE per leaf block | Basic, Advanced, Elite |
| Taxi | flies you to a set place | +4 FE/t | 100 FE per trip | Basic, Advanced, Elite |
| Rancher | shears and milks animals | +2 FE/t | 30 FE per shearing or milking | Basic, Advanced, Elite |
| Fertilizer | grows plants with fertilizer | +2 FE/t | 20 FE per use | Basic, Advanced, Elite |
| Builder | builds a multiblock or schematic | +4 FE/t | 40 FE per block | **Mini only** |
| Fisher | fishes in open water | +2 FE/t | 20 FE per catch | Basic, Advanced, Elite |
| Breeder | feeds animals so they breed | +2 FE/t | 30 FE per animal fed | Basic, Advanced, Elite |
| Guard | shoots hostile mobs | +4 FE/t | 300 FE per shot | Basic, Advanced, Elite |
| Bomb | dives at a mob and explodes | +2 FE/t | the Mini Drone itself | **Mini only** |
| Sponge | sucks up liquids | +4 FE/t | 40 FE per 3x3 | Basic, Advanced, Elite |
| Tank | holds 32 buckets of liquid for the other modules | +2 FE/t | none | Basic, Advanced, Elite |

A [Mini Drone Frame](drone_frames.md) takes only the Builder or the Bomb Module, and those two fit
no other frame. A drone cannot carry the same module twice. How the drone's total power use is
worked out is on the [Using Drones](drones.md) page.

## Several modules on one drone

A drone with more than one module **takes turns**: after each trip back to the dock it tries the
next module first, so no job is forgotten.

You can see the installed modules on the drone itself: each one adds its own tool on it.

Good pairs:

- **Harvester + Planter**: harvested seeds go to the dock and the Planter puts them back.
- **Harvester + Planter + Fertilizer** on the same field.
- **Breeder + Rancher**: the herd grows, and its wool and milk come home.
- **Sponge + Tank** to keep the liquid it sucks up.

## Harvester Module

<ItemImage id="cyberfarm:harvester_module" scale="2" float="left" />

The drone looks for **fully grown crops** in its work area and harvests them, bringing crops and
seeds to the dock.

<br clear="all" />

- It also harvests **melons, pumpkins and ripe cocoa pods**.
- It **picks** ripe plants that give their fruit to a right-click, like sweet berry bushes,
  leaving the plant in place. Plants a click does nothing to, like bamboo or kelp, are left alone.
- Works with wheat, carrots, potatoes, beetroot and modded crops and bushes built like the vanilla ones.
- It **does not replant**: the field stays bare. For a farm that runs by itself add a
  **Planter Module**, on a frame with room for both or on a second drone sharing the seeds.
- A [Fortune Addon](addons.md) gives more crops and seeds.

<RecipeFor id="harvester_module" />

## Quarry Module

<ItemImage id="cyberfarm:quarry_module" scale="2" float="left" />

The drone **digs out the work area layer by layer**, from the top of the area all the way down
to bedrock. The dock screen shows the layer it is working on.

<br clear="all" />

Setting it up:

- It **only works with a surveyed area**: the default area around the dock is never dug, so your
  base is safe. Mark the area with the [Survey Tool](survey_tool.md).
- Mark the area high enough to take in anything solid that rises above it (a tree, a hillside):
  the ground under it is left alone.
- The [dock filter](drone_dock.md) decides which drops are **kept**; the others are thrown away.

How it digs:

- It digs **in order**: row by row along the layer, zig-zagging, and every other layer back the
  other way, so it never has to fly across the pit.
- It flies back to unload after **192 items** (three stacks).
- Blocks drop as if mined with a **diamond pickaxe**. Add Fortune or Silk Touch with [addons](addons.md).
- In a deep pit it climbs out over the rim and drops straight down the shaft, so the dock can sit
  beside the pit.
- A layer is finished only when every block in it is dug: a block the drone could not reach is
  tried again on the next trip, never skipped.

What it leaves:

- **Liquids**, **unbreakable blocks** and anything with an inventory or a machine inside
  (chests, furnaces, CyberFarm machines). What lies **under** one of those stays too.
- The column right under the Drone Dock, so the dock stays on a pillar.

Harder blocks take longer and cost more power:

| Block | Energy per block |
| --- | --- |
| Dirt | about 50 FE |
| Stone | about 70 FE |
| Obsidian | about 1,040 FE |

<RecipeFor id="quarry_module" />

## Transport Module

<ItemImage id="cyberfarm:transport_module" scale="2" float="left" />

The drone carries items **from one container to another**. It picks up a load at the **source**,
flies to the **destination** and empties it there. It keeps going while the source has items and
the destination has room.

<br clear="all" />

- Set the route with the [Survey Tool](survey_tool.md): click the dock, the source container, then
  the destination.
- Any inventory works, including ones from other mods and **other drones' docks**: a Transport
  drone can collect what a Harvester or a Quarry brings home.
- A load is **16 items**. Each [Stack Addon](addons.md) adds a full stack (64 items), up to 9 stacks.
- While loaded, the drone carries the item **in its claw**, so you can see what it is moving.
- Both containers must be within **48 blocks of the dock**, in loaded chunks.
- If something stands in the way, the drone climbs over it. If it stays stuck for 5 seconds it
  gives that container up and goes home until its next trip.
- Items that do not fit in the destination are brought back to the dock.
- The [dock filter](drone_dock.md) decides **which items** are moved, and can keep a set number
  of each at the destination.

<RecipeFor id="transport_module" />

## Planter Module

<ItemImage id="cyberfarm:planter_module" scale="2" float="left" />

The drone plants seeds on **empty farmland** in its work area. It loads up to a stack of seeds
**from its dock** before each trip.

<br clear="all" />

- It works perfectly with a Harvester: harvested seeds go to the dock and the Planter puts them
  back in the ground. You can also fill the dock with a hopper.
- Plants anything that grows on farmland: wheat seeds, carrots, potatoes, beetroot seeds,
  melon and pumpkin seeds, and modded crops.
- It **never tills soil**: hoe the field once and it will keep it planted.
- Seeds it did not use go back to the dock.
- Its tank feeds a pump under the hull, which sprays green on each block it sows.

<RecipeFor id="planter_module" />

## Logger Module

<ItemImage id="cyberfarm:logger_module" scale="2" float="left" />

The drone finds the **trees** in its work area (a log standing on soil), parks next to the trunk
and fells the whole tree **from the top down**, leaves included. Then it **replants a sapling**
where the trunk stood.

<br clear="all" />

- Felling the leaves also collects saplings and apples.
- Works with every vanilla tree and with modded trees using the standard log and leaf tags.
- Leaves you placed yourself are never touched.
- Fortune gives more saplings and apples; Silk Touch collects the leaf blocks themselves.
- A big tree can drop more than the 9 cargo slots hold: the rest falls on the ground.

<RecipeFor id="logger_module" />

## Taxi Module

<ItemImage id="cyberfarm:taxi_module" scale="2" float="left" />

A personal air taxi. The drone **waits above its dock** with a cable and a hook, and carries you to
the dock's **taxi destination**.

<br clear="all" />

1. With the [Survey Tool](survey_tool.md) linked to the dock, **right-click the block** you want
   to land on: up to **256 blocks** from the dock, with two blocks of air above it.
2. **Right-click** the waiting drone to grab the hook. You hang below it with one hand.
3. It climbs above the terrain, flies straight to the destination and puts you down gently.
   Then it flies home by itself.

- Trips are **one way**: put a second dock with its own Taxi drone at the destination to fly back.
- Before taking off the drone checks it has enough charge for the trip and the way back.
- **Sneak** to let go mid-flight: you get **Slow Falling**, so you land safely.
- On a long trip the dock can fall out of the loaded world behind you. The drone still takes you
  all the way, then waits where it left you and flies home once its dock is loaded again.
- Speed Addons make the trip faster.

<RecipeFor id="taxi_module" />

## Rancher Module

<ItemImage id="cyberfarm:rancher_module" scale="2" float="left" />

The drone looks after the animals in its work area: it **shears** every sheep (and mooshroom,
and any shearable mob from other mods) and **milks** cows and goats. Wool, milk and the rest go
back to the dock.

<br clear="all" />

- Milking needs **empty buckets**: put them in the dock. The drone takes up to 16 per trip and
  brings back the milk buckets.
- It chases the animal and hovers just above it, so fence the herd inside the work area.
- Sheep are sheared again once their wool grows back.

<RecipeFor id="rancher_module" />

## Fertilizer Module

<ItemImage id="cyberfarm:fertilizer_module" scale="2" float="left" />

The drone **grows the crops, stems and saplings** of its work area with fertilizer from the dock,
until they are fully grown.

<br clear="all" />

- It works with **any fertilizer**: bone meal, Biomass (weak: it grows the plant one time in
  three), or fertilizers from other mods (everything in the `c:fertilizers` tag).
- Put the fertilizer in the dock: the drone takes up to 64 per trip.
- Grass and moss are left alone, so your lawn does not fill with flowers.
- A fertilizer that does not work on a plant is not wasted: the drone skips that plant.
- The white canister on its shoulder feeds a pump under the hull, which sprays each plant.

<RecipeFor id="fertilizer_module" />

## Builder Module

<ItemImage id="cyberfarm:builder_module" scale="2" float="left" />

The drone **builds a multiblock or a schematic** for you: the one you lock with the
[Multiblock Projector](multiblock_projector.md) and link to its dock. It fits **only a
[Mini Drone Frame](drone_frames.md)**: building is work for a swarm of Mini Drones on a
[Builder Swarm Dock](swarm_dock.md).

<br clear="all" />

Setting it up:

1. Pick the structure in the projector's list and **sneak + right-click** a block to lock it there.
2. **Right-click the dock** with the projector to link it. Right-click the dock with an unlocked
   projector to unlink it.
3. Put the blocks in the dock. The drone takes one kind per trip, carrying them in its claw.

How it builds:

- First it **clears the site**, from the top down. It cuts away wrong blocks where the structure
  goes and anything in a space that must stay empty. In a schematic's box (above its bottom
  layer) it also clears the ground, rocks, trees and plants. What it cuts drops where the block was.
- It leaves chests, machines and anything else holding things, fluids and unbreakable blocks:
  the dock screen counts them, and you move them.
- Then it builds **one layer at a time**, from the bottom. A layer is finished before the next one
  starts. If the dock runs out of a block the current layer needs, the drone **waits** for it.
- Each block is **printed** into place: a shell of light closes in on it and a scan line runs down it.
- It flies above the layer it is building, straight from block to block, and goes over the whole
  structure when something is in the way, so it never gets walled in.

Supplying the blocks:

- The dock's **Build** tab shows the layer and the blocks it still needs. The ones the dock has
  none of are marked in red.
- The dock **takes in only the blocks the build still needs**. Point a Transport drone's route
  from your storage to the dock and it brings exactly what is missing, and nothing else.
- Or put **chests next to the dock**: every second it takes from them what the layer being built
  still needs, and puts back there what that layer does not use. Its nine slots never fill up
  with blocks for later layers.

Good to know:

- Doors and beds go in whole. Chests go in empty.
- Blocks it cannot place (water, a lantern hanging under a ceiling) are listed in the Build tab at
  the end, for you to place by hand.
- Several Builder drones on different docks can share one build: link the same projection to
  each dock and they split the blocks between them.

<RecipeFor id="builder_module" />

## Fisher Module

<ItemImage id="cyberfarm:fisher_module" scale="2" float="left" />

The drone **fishes** in the water of its work area and brings the catch to the dock.

<br clear="all" />

- It needs **open water**: a water source block with air above it, inside the work area.
- It hovers over the water and waits for a bite, like a fishing rod.
- It pulls out what a rod would, with the same odds: mostly fish, some junk and now and then
  **treasure** (enchanted books, saddles, name tags...).
- It flies home when its cargo is full.

<RecipeFor id="fisher_module" />

## Breeder Module

<ItemImage id="cyberfarm:breeder_module" scale="2" float="left" />

The drone **feeds the animals** of its work area so they breed, with their food from the dock.
A hay rack hangs under its hull, and feed falls from it on each animal it feeds.

<br clear="all" />

- Put the food in the dock: wheat for cows and sheep, seeds for chickens, carrots for pigs, or
  whatever a modded animal eats. The drone takes the right food for each animal, up to 16 per trip.
- It feeds an animal only when **two of a kind** are ready, so none of the food is wasted.
- It stops once there are **16 of a kind** in the work area, babies included, so the farm does not
  grow without end.
- Pairs well with a Rancher, or a Harvester for the crops they eat.

<RecipeFor id="breeder_module" />

## Guard Module

<ItemImage id="cyberfarm:guard_module" scale="2" float="left" />

The drone **defends its work area**: it hunts every hostile mob there and shoots it with the laser
on its shoulder, then goes after the next one.

<br clear="all" />

- It looks over the work area and **8 blocks above and below** it, so it also catches flying mobs
  and what comes out of a cave. Without a surveyed area that is the default 9x9 square round the
  dock; mark a bigger one (up to 33x33) with the [Survey Tool](survey_tool.md).
- It hovers **3 blocks over the mob's head**, out of reach of swords and claws. Nothing can hurt
  the drone, creepers included.
- It fires once a second: **6 damage a shot** for **300 FE**. That is 50 FE per point of damage,
  twice what the [Laser Turret](laser_turret.md) pays.
- It shoots **hostile mobs only**, like the Laser Turret: never players, animals, villagers,
  golems, named mobs, mobs riding something or neutral mobs that are not angry.
- With nothing left to shoot it flies home to charge, and checks again every two seconds.
- Pairs well with any farm module on the same drone, or on its own on a dock by the village gate.

<RecipeFor id="guard_module" />

## Bomb Module

<ItemImage id="cyberfarm:bomb_module" scale="2" float="left" />

A kamikaze drone. It fits **only a [Mini Drone Frame](drone_frames.md)**. The Mini Drone waits on
its [Builder Swarm Dock](swarm_dock.md) until a hostile mob comes into the work area, then **dives
at it and explodes**.

<br clear="all" />

- It looks over the work area and **8 blocks above and below** it. It goes for the same mobs as
  the Guard Module: never animals, villagers, golems, named mobs or calm neutral mobs.
- With the fuse lit (you hear it hiss) it flies straight at the mob and goes off once it is close.
- The blast is **a small TNT**, half as strong, going off at the mob's feet: about **24 damage**,
  enough for a zombie, a skeleton or a creeper.
- It **breaks a few blocks** and hurts anything near it, **players included**. Keep the work area
  away from what you built.
- **The drone is used up**: assemble a new Mini Drone and put it back on the dock. Its place on
  the pad is free as soon as it goes off.
- Up to four Mini Drones share a Builder Swarm Dock, and **each goes for a different mob**, so a
  swarm takes out a whole group.

<RecipeFor id="bomb_module" />

## Sponge Module

<ItemImage id="cyberfarm:sponge_module" scale="2" float="left" />

The drone **sucks up any liquid** from its work area (water, lava or a modded one), lowering a
hood over the surface. Its rings and gauge lamp take the colour of what it holds.

<br clear="all" />

- It **only works with a surveyed area**: the default area around the dock is never touched. Mark
  it with the [Survey Tool](survey_tool.md).
- It flies to the highest source in the area and takes the **3x3** around it, sources and
  currents alike, then the next, until the area is dry.
- A pool deeper than the area is followed down to its floor, up to **32 blocks** under it.
- On its own it **throws the liquid away** and carries on, without going home.
- To keep the liquid, put a **Tank Module** on the same drone. It then holds one kind of liquid at
  a time and brings it to the dock.
- Kelp and seagrass break and drop; waterlogged blocks just dry out.
- Water running in from outside the area is left alone. It dries up once its source is gone, so
  to drain a river, mark the stretch with its sources.

<RecipeFor id="sponge_module" />

## Tank Module

<ItemImage id="cyberfarm:tank_module" scale="2" float="left" />

A windowed **canister** on the drone's flank, holding **32 buckets** of one liquid. It has no job
of its own: it works with the modules beside it.

<br clear="all" />

- **With a Sponge Module** the drone keeps what it sucks up, one kind at a time, and brings it to
  the dock. There it goes into a [Dock Tank](drone_dock.md) if there is one. With none, or with
  that one full or holding another liquid, it stays on board.
- When the canister is full and nothing can empty it, **the drone stops on its dock** until there
  is room.
- **With a Transport Module** a route between two tanks carries liquid from one to the other. Set
  it with the [Survey Tool](survey_tool.md) as usual: click the source tank, then the destination.
  If the route also has items to move, the drone carries those first.
- You can read its colour and level from outside, and its pump works while it does.

<RecipeFor id="tank_module" />
