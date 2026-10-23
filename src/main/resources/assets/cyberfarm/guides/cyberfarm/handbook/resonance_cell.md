---
navigation:
  title: Resonance Cell
  parent: energy_storage.md
  position: 2
  icon: cyberfarm:resonance_cell
item_ids:
  - cyberfarm:resonance_cell
---

# Resonance Cell

<BlockImage id="cyberfarm:resonance_cell" scale="4" float="left" />

The blocks of a [Resonance Core](resonance_core.md)'s **shell**: the more of them around the
crystal, the higher the tier and the more it stores. They are crafted four at a time.

<br clear="all" />

| Tier | Shell | Cells |
| --- | --- | --- |
| 2 | 3x3x3 | 26 |
| 3 | 5x5x5 | 98 |
| 4 | 7x7x7 | 218 |

## Building the shell

The shell is a hollow cube of <ItemLink id="resonance_cell" />s **right above the pedestal**,
centred on it: 3x3x3 for tier 2, 5x5x5 for tier 3, 7x7x7 for tier 4. Every block of the cube's
surface must be a cell and its **inside must stay empty**. When the last cell goes in, the cells
vanish and the crystal grows to fill the shell. The [Multiblock Projector](multiblock_projector.md)
shows every tier, pylons included: sneak + right-click to pick it, then click the pedestal.

- Breaking a cell of the shell drops the core back a tier, and **the energy that no longer fits is
  lost**: empty the core before rebuilding it.

<RecipeFor id="resonance_cell" />
