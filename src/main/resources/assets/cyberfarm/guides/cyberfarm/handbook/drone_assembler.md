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

The workshop where drones are built. It needs power: each assembly costs **4,000 FE**
and takes 5 seconds. The assembler itself takes Speed, Efficiency and Battery [addons](addons.md) on the tab beside its GUI,
which change those numbers and its 20,000 FE buffer.

<br clear="all" />

## How to build a drone

1. Put a [drone frame](drone_frames.md) in the **frame** slot (on the left). Insert it first:
   the other slots stay greyed out until there is a frame.
2. Put [function modules](modules.md) in the **module** column. The frame tier decides how many
   module slots are unlocked.
3. Optionally put [addons](addons.md) in the **2x2 addon grid**. Each slot holds a stack of the
   same addon; the counter above the grid shows how many you used out of the frame's limit.
4. Press **Assemble**. The drone takes shape as a **hologram** on the build plate, under the
   assembler's print head, unfolding as it is made. When the welding head reaches the end of its rail it flashes into the output slot: take the finished
   <ItemLink id="drone" /> from there.

A drone needs **at least one module**, and the same module cannot be installed twice. The Builder
and Bomb Modules fit only a [Mini Drone Frame](drone_frames.md), which takes no other module, and
Fortune and Silk Touch Addons cannot go together.

To **change a finished drone**, put it back in the output slot with the other slots empty and
press **Disassemble**: its parts return to their slots. Disassembling is free; assembling again
costs the usual 4,000 FE.

Hover an empty slot to see what goes there.

The assembler accepts FE from any side, so place it next to a
<ItemLink id="combustion_generator" /> or connect it with cables.

<RecipeFor id="drone_assembler" />
