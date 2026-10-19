---
navigation:
  title: Drone Dock
  parent: drone_systems.md
  position: 5
  icon: cyberfarm:drone_dock
item_ids:
  - cyberfarm:drone_dock
  - cyberfarm:survey_tool
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

The dock GUI shows what the drone is doing, its battery level and the size of the work area.

<RecipeFor id="drone_dock" />

## Choosing the work area

Without any setup the drone works a **9x9** area centred on the dock. To change it, use the
<ItemLink id="survey_tool" />:

1. **Right-click the dock** to link the tool to it.
2. **Right-click the first corner** of your field.
3. **Right-click the opposite corner**. The dock now uses that area.

The area can be up to **33x33** blocks, the two corners can be at most **8 blocks** apart in
height, and everything must stay within 48 blocks of the dock.

## Setting a transport route

For drones with a [Transport Module](modules.md):

1. **Right-click the dock** to link the tool.
2. **Right-click the source container** (chest, barrel, any modded inventory).
3. **Right-click the destination container**. The route is saved on the dock.

Clicking a container sets the route instead of an area corner.

## Setting a taxi destination

For drones with a [Taxi Module](modules.md): with the tool linked to the dock, **sneak +
right-click the block** where passengers should land. It can be up to 256 blocks away and needs
two blocks of air above it.

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

- **Sneak + right-click in the air** clears a corner or route source you set by mistake.
- **Sneak + right-click the dock** resets it: default 9x9 area, no transport route, no taxi destination.

<RecipeFor id="survey_tool" />
