---
navigation:
  title: Drone Assembler
  parent: index.md
  position: 3
  icon: cyberfarm:drone_assembler
item_ids:
  - cyberfarm:drone_assembler
---

# Drone Assembler

<BlockImage id="cyberfarm:drone_assembler" scale="4" float="left" />

The workshop where drones are built. It needs power: each assembly costs **2,000 FE**
and takes 5 seconds.

<br clear="all" />

## How to build a drone

1. Put a [drone frame](drone_frames.md) in the **Frame** slot.
2. Put [function modules](modules.md) in the **Modules** slots. The frame tier decides how many
   slots are unlocked: locked slots are greyed out, so insert the frame first.
3. Press **Assemble**. When the bar is full, take the finished <ItemLink id="drone" /> from the output slot.

A drone needs **at least one module**, and the same module cannot be installed twice.

The assembler accepts FE from any side, so place it next to a
<ItemLink id="combustion_generator" /> or connect it with cables.

<RecipeFor id="drone_assembler" />
