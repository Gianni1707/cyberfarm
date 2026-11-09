---
navigation:
  title: Drone Dock
  parent: drone_systems.md
  position: 5
  icon: cyberfarm:drone_dock
item_ids:
  - cyberfarm:drone_dock
  - cyberfarm:survey_tool
  - cyberfarm:dock_tank
  - cyberfarm:swarm_dock
---

# Drone Dock

<BlockImage id="cyberfarm:drone_dock" scale="4" float="left" />

The home base of a drone. Each dock hosts **one drone** and:

- **charges** it (up to 1,000 FE/t) from its own 100,000 FE buffer;
- **stores** the harvest in 9 slots;
- remembers the drone's **work area**;
- holds an **item filter** for the Quarry and Transport modules.

<br clear="all" />

Power the dock with a <ItemLink id="combustion_generator" /> placed next to it, or with
cables from any mod. The harvest can be pulled out with hoppers or item pipes from any side.

You can read a dock at a glance: lights **chase round its landing ring** while a drone flies
home, and **arcs of energy** jump from its corner beacons to a drone charging on it. On a Builder
Swarm Dock each of the four landing rings **lights up** while a Mini Drone stands on it.

<RecipeFor id="drone_dock" />

## The dock screen

Right-click the dock to open it:

- **Drone Work**: what the drone is doing right now (Harvesting, Quarrying, Transporting,
  Charging, Returning, On hold...).
- **Drone Position**: where the drone is, or **Docked** when it is on the pad.
- The **9 cargo slots**, the **Filter** button (see below) and the dock's energy bar.
- **Return / Resume**: **Return** calls the drone home and keeps it there, charging but taking
  no new jobs (a Taxi drone first finishes the trip it is on). **Resume** lets it work again.
- **Area**: shows in the world what the dock is set up for (see **The Survey Tool** below).
- With a **Dock Tank** beside the dock (see below), a bar for its liquid under the cargo.

**Side tabs** on the left open a panel next to the screen; click a tab again to close it.
The screen remembers which one you left open.

- **Drone Status** (the "i" tab, coloured like the drone's state): its state, the modules it
  carries, the work area (not for Transport, Taxi and Builder drones, which never use it),
  whether a route, a taxi destination or a build is set, and the layer a Quarry has reached.
- **Battery** (the battery tab, showing the charge): the charge level, the FE stored and whether
  the drone is charging or flying.
- **Build** (only with a Builder drone or a linked build): the layer being built and the blocks it
  still needs; the ones the dock has none of are marked in red.

## The Survey Tool

The <ItemLink id="survey_tool" /> sets up a dock. **Right-click a dock** to link it: the tool reads
what the drone on that dock does and **asks for what it needs**, above the hotbar, for as long as
you hold it. The tool never opens the blocks you click, so chests and machines stay closed while
you set things up.

| The drone has | The tool asks for |
| --- | --- |
| A module that works an area (Harvester, Quarry, Planter, Sponge...) | **two opposite corners**: the work area |
| A **Transport Module** | **two containers**, source then destination: the route |
| A **Taxi Module** | **the block** where passengers land |
| A **Builder Module** | nothing: link the structure with the [Multiblock Projector](multiblock_projector.md) |

With no drone on the dock it asks you to put one on first. A drone with several modules can need
more than one thing: the tool moves on by itself once one is set, and **sneak + mouse wheel** or a
**right-click in the air** switches between them.

To see what a dock is set up for, press the **Area** button in its screen: it sends out **mini
drones**. Four take up the corners of the work area and light up its blocks, from the corners
inwards; for a route or a landing spot, one hovers over each and lights that block. They stay out
after you close the screen, and fly home when you click the button again or take the drone off the dock.

Clicking the linked dock again only repeats what the tool is asking for. To set up another dock,
**right-click it** (while the tool asks for a route or a landing spot, or for the second corner,
**sneak + right-click** it, as a plain click would use it as a container, the landing block or the corner).

## Choosing the work area

Without any setup the drone works a **9x9** area centred on the dock. To change it:

1. **Right-click the dock** with the tool.
2. **Right-click the first corner** of your field.
3. **Right-click the opposite corner**. The dock now uses that area.

The area can be up to **33x33** blocks, the two corners can be at most **8 blocks** apart in
height, and everything must stay within 48 blocks of the dock.

The drone works its area **block by block from one corner to the other**, row after row like a
Quarry, so a field is worked in order instead of here and there.

## Setting a transport route

For drones with a [Transport Module](modules.md):

1. **Right-click the dock** with the tool.
2. **Right-click the source container** (chest, barrel, any modded inventory, or **another
   drone's dock**).
3. **Right-click the destination container**. The route is saved on the dock.

To carry things **out of the drone's own dock**, click that dock **twice in a row** as the source: one
click only repeats what the tool is asking for. Both must be within **48 blocks of the dock**, and their chunks loaded: an [Anchor Addon](addons.md) keeps them so.

Another drone's Drone Dock counts as a container: a Transport drone can empty the dock of a
Harvester or a Quarry and bring the harvest to your storage.

## Setting a taxi destination

For drones with a [Taxi Module](modules.md): with the tool linked to the dock, **right-click the
block** where passengers should land. It can be up to 256 blocks away and needs two blocks of air
above it.

## Dock Tank

Place a <ItemLink id="dock_tank" /> against a side of the dock: a bar for it appears in the dock's
screen, under the cargo. It only **stores**: it holds **64 buckets** of one liquid at a time, and drones
with a [Tank Module](modules.md) empty into it when they come home. A drone without that module brings
it nothing.

Watch the level and the float in its side windows, or right-click it to read what is in it. **Pipes**
plug in behind it and on top, so you can drain it or use it as a plain tank.

<RecipeFor id="dock_tank" />

## Item filter

The **Filter** button under the dock's slots opens its filter:

- with a [Quarry Module](modules.md), the drone **keeps only the drops that pass** and throws the
  rest away (e.g. a whitelist of ores, or a blacklist of cobblestone, dirt and gravel);
- with a [Transport Module](modules.md), the drone **moves only the items that pass**.

Click a slot with an item to add it as a sample (nothing is used up), click it empty-handed to
clear it. The buttons switch between **whitelist** and **blacklist**, match items **exactly**
(enchantments, damage, names) or not, match **by mod**, and pick a **tag** of the first sample
(e.g. `#c:ores`). An empty blacklist, the default, lets everything through.

The bottom row sets a **limit** for the Transport Module: **Keep at destination: N**. The drone
then stops bringing an item once the destination holds N of it, and carries only what is missing,
so a chest stays stocked with 64 bread instead of filling up with 2,000. The arrows step through
round numbers (8, 16, 32 and on up to 4,096); hold **Shift** to step by one. It counts each item
separately.

## Fixing mistakes

- **Sneak + right-click in the air** clears a corner or route source you set by mistake.
- **Sneak + right-click the linked dock** resets it: default 9x9 area, no transport route, no taxi
  destination.

<RecipeFor id="survey_tool" />

## Builder Swarm Dock

<BlockImage id="cyberfarm:swarm_dock" scale="3" float="left" />

A dock for up to **four Mini Drones** (drones built on a [Mini Drone Frame](drone_frames.md)). They
share its work area, transport route, build and stock, and each parks on its own corner of the pad.
Four [Builder](modules.md) Mini Drones raise a structure together: link it to the swarm dock with
the Multiblock Projector, and they split its blocks between them.

<br clear="all" />

- Right-click the swarm dock with a Mini Drone to deploy it; it takes up to four. Bigger drones
  do not fit, and Mini Drones do not fit an ordinary Drone Dock.
- The screen shows the swarm as one: the busiest drone's job, their batteries added up, and how
  many drones are on the dock.

<RecipeFor id="swarm_dock" />

The [Hologram Table](hologram_table.md) shows all your docks and drones on a 3D map.
