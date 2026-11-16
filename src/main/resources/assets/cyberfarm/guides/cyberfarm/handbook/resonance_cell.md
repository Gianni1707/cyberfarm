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

Resonance Cells make the **shell** of a [Resonance Core](resonance_core.md). The bigger the shell around
the crystal, the higher the core's tier and the more it stores.

<br clear="all" />

## Building the shell

- The shell is a **hollow cube** of cells **right above the pedestal**, centred on it.
- Every block of the cube's surface must be a cell.
- Its **inside must stay empty**.
- When the last cell goes in, the cells vanish and the crystal grows to fill the shell.

The [Multiblock Projector](multiblock_projector.md) shows every tier, pylons included: sneak +
right-click to pick the tier, then click the pedestal. The core also needs its
[Stabilizer Pylon](stabilizer_pylon.md) columns to work.

## Numbers

| Tier | Shell | Cells |
| --- | --- | --- |
| 2 | 3x3x3 | 26 |
| 3 | 5x5x5 | 98 |
| 4 | 7x7x7 | 218 |

One craft makes **4 cells**.

## Tips

- Right-clicking any cell of the shell, finished or not, opens the core's screen.
- Breaking a cell of the shell drops the core back a tier, and **the energy that no longer fits is
  lost**. Empty the core before you rebuild it.

<RecipeFor id="resonance_cell" />
