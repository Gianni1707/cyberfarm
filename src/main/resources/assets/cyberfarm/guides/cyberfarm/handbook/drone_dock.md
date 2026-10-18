---
navigation:
  title: Drone Dock
  parent: index.md
  position: 6
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
- remembers the drone's **work area**.

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

The area can be up to **33x33** blocks and must stay within 48 blocks of the dock.
Sneak and right-click in the air to clear a corner you set by mistake.

<RecipeFor id="survey_tool" />
