---
navigation:
  title: Drone Assembler
  parent: drone_systems.md
  position: 1
  icon: cyberfarm:drone_assembler
item_ids:
  - cyberfarm:drone_assembler
---

# Drone Assembler

<BlockImage id="cyberfarm:drone_assembler" scale="4" float="left" />

The workshop where drones are built. Put in a frame, modules and addons, press **Assemble**, and
take out a finished <ItemLink id="drone" />. It also takes drones apart, so you can change them.

<br clear="all" />

## Placing and powering

The assembler needs power and accepts FE from any side. Place it next to a
[Combustion Generator](combustion_generator.md) or connect it with cables.

<RecipeFor id="drone_assembler" />

## How to build a drone

1. Put a [drone frame](drone_frames.md) in the **frame** slot (on the left). Insert it first:
   the other slots stay greyed out until there is a frame.
2. Put [function modules](modules.md) in the **module** column. The frame tier decides how many
   module slots are unlocked.
3. Optionally put [addons](addons.md) in the **2x2 addon grid**. Each slot holds a stack of the
   same addon. The counter above the grid shows how many you used out of the frame's limit.
4. Press **Assemble**.
5. The drone takes shape as a **hologram** on the build plate, under the print head, unfolding as
   it is made. When the welding head reaches the end of its rail, the drone flashes into the
   output slot. Take it from there.

Hover an empty slot to see what goes there.

## What fits together

- A drone needs **at least one module**.
- The same module cannot be installed twice.
- The Builder and Bomb Modules fit only a [Mini Drone Frame](drone_frames.md), and that frame
  takes no other module.
- Fortune and Silk Touch Addons cannot go together.

## Changing a drone

1. Put the finished drone in the **output slot**, with the other slots empty.
2. Press **Disassemble**: its parts return to their slots.
3. Change what you want and press **Assemble** again.

Disassembling is free; assembling again costs the usual energy. The drone's charge goes into the
assembler's buffer. What does not fit is kept and goes back into the next drone built there. So
does its **name**, as long as you build it from the same frame.

## Numbers

| | Drone Assembler |
| --- | --- |
| Energy per assembly | 4,000 FE |
| Time per assembly | 5 seconds |
| Energy buffer | 20,000 FE |
| Disassembly | free |

## Machine addons

The assembler itself takes **Speed**, **Efficiency** and **Battery** [addons](addons.md) on the tab
beside its GUI. They change its speed, its energy per assembly and its buffer. See the
[Addons](addons.md) page for the numbers.
