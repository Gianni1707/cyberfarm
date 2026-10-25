---
navigation:
  title: Drone Dock
  parent: drone_systems.md
  position: 5
  icon: cyberfarm:drone_dock
item_ids:
  - cyberfarm:drone_dock
  - cyberfarm:survey_tool
  - cyberfarm:swarm_dock
---

# Drone Dock

<BlockImage id="cyberfarm:drone_dock" scale="4" float="left" />

The home base of a drone. Each dock hosts **one drone** and:

- **charges** it (up to 500 FE/t) from its own 100,000 FE buffer;
- **stores** the harvest in 9 slots;
- remembers the drone's **work area**;
- holds an **item filter** for the Quarry and Transport modules.

<br clear="all" />

Power the dock with a <ItemLink id="combustion_generator" /> placed next to it, or with
cables from any mod. The harvest can be pulled out with hoppers or item pipes from any side.

<RecipeFor id="drone_dock" />

## The dock screen

Right-click the dock to open it:

- **Drone Work**: what the drone is doing right now (Harvesting, Quarrying, Transporting,
  Charging, Returning, On hold...).
- **Drone Position**: where the drone is, or **Docked** when it is on the pad.
- The **9 cargo slots**, the **Filter** button (see below) and the dock's energy bar.
- **Return / Resume**: **Return** calls the drone home and keeps it there, charging but taking
  no new jobs (a Taxi drone first finishes the trip it is on). **Resume** lets it work again.

Two **side tabs** on the left open a panel next to the screen; click a tab again to close it.
The screen remembers which one you left open.

- **Drone Status** (the "i" tab, coloured like the drone's state): its state, the modules it
  carries, the work area (not for Transport and Taxi drones, which never use it) and, for
  Transport and Taxi drones, whether a route or a destination is set.
- **Battery** (the battery tab, showing the charge): the charge level, the FE stored and whether
  the drone is charging or flying.

## The Survey Tool

The <ItemLink id="survey_tool" /> sets up a dock. It has **four modes**: hold it and **sneak +
scroll the mouse wheel** to switch, or **right-click the air** to go to the next one. The current
mode shows above the hotbar and in the tooltip,
and a click only does what that mode says. The tool never opens the blocks you click, so chests
and machines stay closed while you set things up.

| Mode | Right-click |
| --- | --- |
| **Dock** | a Drone Dock: the tool now configures that dock. Sneak + right-click resets it. |
| **Area** | two opposite corners: the dock's work area. |
| **Route** | two containers, source then destination: the transport route. |
| **Taxi** | the block where taxi passengers land. |

The tool **tells you each step** above the hotbar: when you switch mode and after every click it
says what to click next ("Click the source container", "Click the opposite corner"...). The
first click, in any mode, picks the dock you want to set up; after that a click does what the
mode says. Dock mode only picks the dock (use it to switch to another dock): to connect the dock
to a chest, use **Route** mode.

## Choosing the work area

Without any setup the drone works a **9x9** area centred on the dock. To change it:

1. Switch the tool to **Area** mode and **right-click the dock**.
2. **Right-click the first corner** of your field.
3. **Right-click the opposite corner**. The dock now uses that area.

The area can be up to **33x33** blocks, the two corners can be at most **8 blocks** apart in
height, and everything must stay within 48 blocks of the dock.

## Setting a transport route

For drones with a [Transport Module](modules.md):

1. Switch the tool to **Route** mode and **right-click the dock**.
2. **Right-click the source container** (chest, barrel, any modded inventory, or **another
   drone's dock**).
3. **Right-click the destination container**. The route is saved on the dock.

Another drone's Drone Dock counts as a container: a Transport drone can empty the dock of a
Harvester or a Quarry and bring the harvest to your storage.

## Setting a taxi destination

For drones with a [Taxi Module](modules.md): with the tool linked to the dock, switch to **Taxi**
mode and **right-click the block** where passengers should land. It can be up to 256 blocks away
and needs two blocks of air above it.

## Item filter

The **Filter** button under the dock's slots opens its filter:

- with a [Quarry Module](modules.md), the drone **keeps only the drops that pass** and throws the
  rest away (e.g. a whitelist of ores, or a blacklist of cobblestone, dirt and gravel);
- with a [Transport Module](modules.md), the drone **moves only the items that pass**.

Click a slot with an item to add it as a sample (nothing is used up), click it empty-handed to
clear it. The buttons switch between **whitelist** and **blacklist**, match items **exactly**
(enchantments, damage, names) or not, match **by mod**, and pick a **tag** of the first sample
(e.g. `#c:ores`). An empty blacklist, the default, lets everything through.

## Fixing mistakes

- **Sneak + right-click in the air** clears a corner or route source you set by mistake
  (switching mode clears it too).
- In **Dock** mode, **sneak + right-click the dock** resets it: default 9x9 area, no transport
  route, no taxi destination.

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
