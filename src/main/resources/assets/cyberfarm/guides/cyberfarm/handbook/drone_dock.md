---
navigation:
  title: Drone Dock
  parent: drone_systems.md
  position: 5
  icon: cyberfarm:drone_dock
item_ids:
  - cyberfarm:drone_dock
  - cyberfarm:dock_tank
---

# Drone Dock

<BlockImage id="cyberfarm:drone_dock" scale="4" float="left" />

The home base of a drone. A drone needs a dock to work: it charges there, unloads its harvest
there and works the area the dock is set up for.

Each dock hosts **one drone**. It:

- **charges** the drone from its own battery;
- **stores** what the drone brings home in 9 slots;
- remembers the drone's **work area**, transport route or taxi destination;
- holds an **item filter** for the Quarry and Transport modules.

A **Dock Tank** placed beside it stores the liquids a drone brings home (see below).

<br clear="all" />

## Placing and powering

Place the dock where the drone should live. The side that faces you is its **front**: a drone
always parks facing it.

Power it with a [Combustion Generator](combustion_generator.md) placed next to it, or with cables
from any mod, from any side. Pull the harvest out with hoppers or item pipes, also from any side.

To put a drone on it, **right-click the dock** with the drone. See [Using Drones](drones.md).

To tell the drone where to work, use the [Survey Tool](survey_tool.md). Without any setup the
drone works a **9x9** area centred on the dock.

Mini Drones do not fit a Drone Dock: they live on a [Builder Swarm Dock](swarm_dock.md).

<RecipeFor id="drone_dock" />

## The dock screen

Right-click the dock to open it:

- **Drone Work**: what the drone is doing right now (Harvesting, Quarrying, Transporting,
  Charging, Returning, On hold...).
- **Drone Position**: where the drone is, or **Docked** when it is on the pad.
- The **9 cargo slots**, the **Filter** button (see below) and the dock's energy bar.
- **Return / Resume**: **Return** calls the drone home and keeps it there, charging but taking
  no new jobs (a Taxi drone first finishes the trip it is on). **Resume** lets it work again.
- **Area**: shows in the world what the dock is set up for (see the [Survey Tool](survey_tool.md)).
- With a **Dock Tank** beside the dock, a bar for its liquid under the cargo.

**Side tabs** on the left open a panel next to the screen. Click a tab again to close it.
The screen remembers which one you left open.

| Tab | Shows |
| --- | --- |
| **Drone Status** (the "i" tab, coloured like the drone's state) | its state, its modules, the work area (not for Transport, Taxi and Builder drones, which never use it), whether a route, a taxi destination or a build is set, and the layer a Quarry has reached |
| **Battery** (shows the charge) | the charge level, the FE stored and whether the drone is charging or flying |
| **Build** (only with a Builder drone or a linked build) | the layer being built and the blocks it still needs; the ones the dock has none of are in red |

## Item filter

The **Filter** button under the dock's slots opens its filter:

- with a [Quarry Module](modules.md), the drone **keeps only the drops that pass** and throws the
  rest away (e.g. a whitelist of ores, or a blacklist of cobblestone, dirt and gravel);
- with a [Transport Module](modules.md), the drone **moves only the items that pass**.

To set it up:

- Click a slot with an item to add it as a sample. Nothing is used up.
- Click a slot empty-handed to clear it.
- The buttons switch between **whitelist** and **blacklist**, match items **exactly**
  (enchantments, damage, names) or not, match **by mod**, and pick a **tag** of the first sample
  (e.g. `#c:ores`).

An empty blacklist, the default, lets everything through.

The bottom row sets a **limit** for the Transport Module: **Keep at destination: N**. The drone
stops bringing an item once the destination holds N of it, and carries only what is missing. So a
chest stays stocked with 64 bread instead of filling up with 2,000. It counts each item
separately.

The arrows step through round numbers (8, 16, 32 and on up to 4,096). Hold **Shift** to step by one.

## Dock Tank

<BlockImage id="cyberfarm:dock_tank" scale="3" float="left" />

The <ItemLink id="dock_tank" /> stores the liquid that drones bring home. Place it against a side
of the dock: a bar for it appears in the dock's screen, under the cargo.

It only **stores**: it holds **64 buckets** of one liquid at a time. Drones with a
[Tank Module](modules.md) empty into it when they come home. A drone without that module brings
it nothing.

<br clear="all" />

- Watch the level and the float in its side windows, or right-click it to read what is in it.
- **Pipes** plug in behind it and on top, so you can drain it or use it as a plain tank.

<RecipeFor id="dock_tank" />

## Numbers

| | Drone Dock |
| --- | --- |
| Drones | 1 |
| Energy buffer | 100,000 FE |
| Charging speed | up to 1,000 FE/t |
| Cargo slots | 9 |
| Default work area | 9x9, centred on the dock |
| Dock Tank | 64 buckets of one liquid |

## Good to know

- You can read a dock at a glance: lights **chase round its landing ring** while a drone flies
  home, and **arcs of energy** jump from its corner beacons to a drone charging on it.
- A dock cannot see a drone that is far off, in a part of the world that is not loaded, and lets
  you place another one. When the first drone comes home and finds no place left, it lands on the
  dock as an item.
- An [Anchor Addon](addons.md) on the drone makes its dock keep the drone's chunks loaded.
- The [Hologram Table](hologram_table.md) shows all your docks and drones on a 3D map.
