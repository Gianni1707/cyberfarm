---
navigation:
  title: Survey Tool
  parent: drone_systems.md
  position: 6
  icon: cyberfarm:survey_tool
item_ids:
  - cyberfarm:survey_tool
---

# Survey Tool

<ItemImage id="cyberfarm:survey_tool" scale="2" float="left" />

The Survey Tool tells a drone **where to work**. Use it to mark a work area, a transport route or a
taxi landing spot on a [Drone Dock](drone_dock.md) or a [Builder Swarm Dock](swarm_dock.md).

<br clear="all" />

## How it works

1. **Right-click a dock** to link the tool to it.
2. The tool reads what the drone on that dock does and **asks for what it needs**, above the
   hotbar, for as long as you hold it.
3. Click the blocks it asks for. The dock saves each one.

The tool never opens the blocks you click, so chests and machines stay closed while you set
things up. With no drone on the dock, it asks you to put one on first.

| The drone has | The tool asks for |
| --- | --- |
| A module that works an area (Harvester, Quarry, Planter, Sponge...) | **two opposite corners**: the work area |
| A **Transport Module** | **two containers**, source then destination: the route |
| A **Taxi Module** | **the block** where passengers land |
| A **Builder Module** | nothing: link the structure with the [Multiblock Projector](multiblock_projector.md) |

A drone with several modules can need more than one thing. The tool moves on by itself once one
is set. **Sneak + mouse wheel** or a **right-click in the air** switches between them.

## Choosing the work area

Without any setup the drone works a **9x9** area centred on the dock. To change it:

1. **Right-click the dock** with the tool.
2. **Right-click the first corner** of your field.
3. **Right-click the opposite corner**. The dock now uses that area.

The drone works its area **block by block from one corner to the other**, row after row, so a
field is worked in order instead of here and there.

## Setting a transport route

For drones with a [Transport Module](modules.md):

1. **Right-click the dock** with the tool.
2. **Right-click the source container**: a chest, a barrel, any modded inventory, or **another
   drone's dock**.
3. **Right-click the destination container**. The route is saved on the dock.

Another drone's dock counts as a container: a Transport drone can empty the dock of a Harvester or
a Quarry and bring the harvest to your storage. With a [Tank Module](modules.md) on the drone, a
tank works as a source or destination too.

To carry things **out of the drone's own dock**, click that dock **twice in a row** as the source.
One click only repeats what the tool is asking for.

Both ends must be in loaded chunks. An [Anchor Addon](addons.md) keeps them loaded.

## Setting a taxi destination

For drones with a [Taxi Module](modules.md): with the tool linked to the dock, **right-click the
block** where passengers should land. It needs two blocks of air above it.

## Seeing what a dock is set up for

Press the **Area** button in the dock's screen: it sends out **mini drones**.

- For a work area, four take up its corners and light up its blocks, from the corners inwards.
- For a route or a landing spot, one hovers over each end and lights that block.

They stay out after you close the screen. They fly home when you click the button again or take
the drone off the dock.

## Limits

| Setting | Limit |
| --- | --- |
| Work area size | up to **33x33** blocks |
| Height between the two corners | at most **8** blocks |
| Work area and route ends | within **48** blocks of the dock |
| Taxi destination | within **256** blocks of the dock |

## Fixing mistakes

- **Sneak + right-click in the air** clears a corner or route source you set by mistake.
- **Sneak + right-click the linked dock** resets it: default 9x9 area, no transport route, no taxi
  destination.
- Clicking the linked dock again only repeats what the tool is asking for.
- To set up **another dock**, right-click it. While the tool asks for a route or a landing spot,
  or for the second corner, **sneak + right-click** it instead: a plain click would use it as a
  container, the landing block or the corner.

<RecipeFor id="survey_tool" />
