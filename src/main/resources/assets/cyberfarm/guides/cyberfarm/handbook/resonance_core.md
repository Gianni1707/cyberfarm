---
navigation:
  title: Resonance Core
  parent: energy_storage.md
  position: 1
  icon: cyberfarm:resonance_core
item_ids:
  - cyberfarm:resonance_core
---

# Resonance Core

<ItemImage id="cyberfarm:resonance_core" scale="4" float="left" />

A buildable battery: a polyhedral crystal floats over the pedestal, columns of **Stabilizer
Pylons** on its sides hold it steady with their beams, and a shell of **Resonance Cells** around
the crystal makes it bigger, tier by tier.

<br clear="all" />

## Tiers

| Tier | Shell of cells | Stores | Pylon columns | Column height | Distance | Per pylon |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | none | 1,000,000 FE | 1 | 2 | 3-8 | 16,000 FE/t |
| 2 | 3x3x3 (26 cells) | 32,000,000 FE | 2 | 3 | 4-10 | 128,000 FE/t |
| 3 | 5x5x5 (98 cells) | 256,000,000 FE | 3 | 4 | 5-12 | 512,000 FE/t |
| 4 | 7x7x7 (218 cells) | 2,000,000,000 FE | 4 | 5 | 6-14 | 2,000,000 FE/t |

## Building it

1. Place the **Resonance Core** (the pedestal). Alone it is tier 1.
2. Build a shell of [Resonance Cells](resonance_cell.md) around the space above it for tiers 2-4.
3. Put up the columns of [Stabilizer Pylons](stabilizer_pylon.md) the tier asks for, on its sides.

The [Multiblock Projector](multiblock_projector.md) shows every tier, pylons included: sneak +
right-click to pick it, then click the pedestal.

## What each tier looks like

Every tier at its smallest, with the pylons at their closest. Drag to turn the scene.

### Tier 1

The core alone: one pylon column, two blocks tall, at least three blocks away.

<GameScene zoom={4} interactive={true}>
  <Block id="cyberfarm:resonance_core" x="3" y="0" z="3" p:facing="south" />
  <Block id="cyberfarm:stabilizer_pylon" x="6" y="0" z="3" p:part="bottom" />
  <Block id="cyberfarm:stabilizer_pylon" x="6" y="1" z="3" p:part="top" />
</GameScene>

### Tier 2

A 3x3x3 shell of 26 cells and two columns, three blocks tall, four blocks away.

<GameScene zoom={3} interactive={true}>
  <Block id="cyberfarm:resonance_core" x="4" y="0" z="4" p:facing="south" />
  <Block id="cyberfarm:resonance_cell" x="3" y="1" z="3" />
  <Block id="cyberfarm:resonance_cell" x="3" y="1" z="4" />
  <Block id="cyberfarm:resonance_cell" x="3" y="1" z="5" />
  <Block id="cyberfarm:resonance_cell" x="4" y="1" z="3" />
  <Block id="cyberfarm:resonance_cell" x="4" y="1" z="4" />
  <Block id="cyberfarm:resonance_cell" x="4" y="1" z="5" />
  <Block id="cyberfarm:resonance_cell" x="5" y="1" z="3" />
  <Block id="cyberfarm:resonance_cell" x="5" y="1" z="4" />
  <Block id="cyberfarm:resonance_cell" x="5" y="1" z="5" />
  <Block id="cyberfarm:resonance_cell" x="3" y="2" z="3" />
  <Block id="cyberfarm:resonance_cell" x="3" y="2" z="4" />
  <Block id="cyberfarm:resonance_cell" x="3" y="2" z="5" />
  <Block id="cyberfarm:resonance_cell" x="4" y="2" z="3" />
  <Block id="cyberfarm:resonance_cell" x="4" y="2" z="5" />
  <Block id="cyberfarm:resonance_cell" x="5" y="2" z="3" />
  <Block id="cyberfarm:resonance_cell" x="5" y="2" z="4" />
  <Block id="cyberfarm:resonance_cell" x="5" y="2" z="5" />
  <Block id="cyberfarm:resonance_cell" x="3" y="3" z="3" />
  <Block id="cyberfarm:resonance_cell" x="3" y="3" z="4" />
  <Block id="cyberfarm:resonance_cell" x="3" y="3" z="5" />
  <Block id="cyberfarm:resonance_cell" x="4" y="3" z="3" />
  <Block id="cyberfarm:resonance_cell" x="4" y="3" z="4" />
  <Block id="cyberfarm:resonance_cell" x="4" y="3" z="5" />
  <Block id="cyberfarm:resonance_cell" x="5" y="3" z="3" />
  <Block id="cyberfarm:resonance_cell" x="5" y="3" z="4" />
  <Block id="cyberfarm:resonance_cell" x="5" y="3" z="5" />
  <Block id="cyberfarm:stabilizer_pylon" x="8" y="0" z="4" p:part="bottom" />
  <Block id="cyberfarm:stabilizer_pylon" x="8" y="1" z="4" p:part="middle" />
  <Block id="cyberfarm:stabilizer_pylon" x="8" y="2" z="4" p:part="top" />
  <Block id="cyberfarm:stabilizer_pylon" x="0" y="0" z="4" p:part="bottom" />
  <Block id="cyberfarm:stabilizer_pylon" x="0" y="1" z="4" p:part="middle" />
  <Block id="cyberfarm:stabilizer_pylon" x="0" y="2" z="4" p:part="top" />
</GameScene>

### Tier 3

A 5x5x5 shell of 98 cells and three columns, four blocks tall, five blocks away.

<GameScene zoom={2} interactive={true}>
  <Block id="cyberfarm:resonance_core" x="5" y="0" z="5" p:facing="south" />
  <Block id="cyberfarm:resonance_cell" x="3" y="1" z="3" />
  <Block id="cyberfarm:resonance_cell" x="3" y="1" z="4" />
  <Block id="cyberfarm:resonance_cell" x="3" y="1" z="5" />
  <Block id="cyberfarm:resonance_cell" x="3" y="1" z="6" />
  <Block id="cyberfarm:resonance_cell" x="3" y="1" z="7" />
  <Block id="cyberfarm:resonance_cell" x="4" y="1" z="3" />
  <Block id="cyberfarm:resonance_cell" x="4" y="1" z="4" />
  <Block id="cyberfarm:resonance_cell" x="4" y="1" z="5" />
  <Block id="cyberfarm:resonance_cell" x="4" y="1" z="6" />
  <Block id="cyberfarm:resonance_cell" x="4" y="1" z="7" />
  <Block id="cyberfarm:resonance_cell" x="5" y="1" z="3" />
  <Block id="cyberfarm:resonance_cell" x="5" y="1" z="4" />
  <Block id="cyberfarm:resonance_cell" x="5" y="1" z="5" />
  <Block id="cyberfarm:resonance_cell" x="5" y="1" z="6" />
  <Block id="cyberfarm:resonance_cell" x="5" y="1" z="7" />
  <Block id="cyberfarm:resonance_cell" x="6" y="1" z="3" />
  <Block id="cyberfarm:resonance_cell" x="6" y="1" z="4" />
  <Block id="cyberfarm:resonance_cell" x="6" y="1" z="5" />
  <Block id="cyberfarm:resonance_cell" x="6" y="1" z="6" />
  <Block id="cyberfarm:resonance_cell" x="6" y="1" z="7" />
  <Block id="cyberfarm:resonance_cell" x="7" y="1" z="3" />
  <Block id="cyberfarm:resonance_cell" x="7" y="1" z="4" />
  <Block id="cyberfarm:resonance_cell" x="7" y="1" z="5" />
  <Block id="cyberfarm:resonance_cell" x="7" y="1" z="6" />
  <Block id="cyberfarm:resonance_cell" x="7" y="1" z="7" />
  <Block id="cyberfarm:resonance_cell" x="3" y="2" z="3" />
  <Block id="cyberfarm:resonance_cell" x="3" y="2" z="4" />
  <Block id="cyberfarm:resonance_cell" x="3" y="2" z="5" />
  <Block id="cyberfarm:resonance_cell" x="3" y="2" z="6" />
  <Block id="cyberfarm:resonance_cell" x="3" y="2" z="7" />
  <Block id="cyberfarm:resonance_cell" x="4" y="2" z="3" />
  <Block id="cyberfarm:resonance_cell" x="4" y="2" z="7" />
  <Block id="cyberfarm:resonance_cell" x="5" y="2" z="3" />
  <Block id="cyberfarm:resonance_cell" x="5" y="2" z="7" />
  <Block id="cyberfarm:resonance_cell" x="6" y="2" z="3" />
  <Block id="cyberfarm:resonance_cell" x="6" y="2" z="7" />
  <Block id="cyberfarm:resonance_cell" x="7" y="2" z="3" />
  <Block id="cyberfarm:resonance_cell" x="7" y="2" z="4" />
  <Block id="cyberfarm:resonance_cell" x="7" y="2" z="5" />
  <Block id="cyberfarm:resonance_cell" x="7" y="2" z="6" />
  <Block id="cyberfarm:resonance_cell" x="7" y="2" z="7" />
  <Block id="cyberfarm:resonance_cell" x="3" y="3" z="3" />
  <Block id="cyberfarm:resonance_cell" x="3" y="3" z="4" />
  <Block id="cyberfarm:resonance_cell" x="3" y="3" z="5" />
  <Block id="cyberfarm:resonance_cell" x="3" y="3" z="6" />
  <Block id="cyberfarm:resonance_cell" x="3" y="3" z="7" />
  <Block id="cyberfarm:resonance_cell" x="4" y="3" z="3" />
  <Block id="cyberfarm:resonance_cell" x="4" y="3" z="7" />
  <Block id="cyberfarm:resonance_cell" x="5" y="3" z="3" />
  <Block id="cyberfarm:resonance_cell" x="5" y="3" z="7" />
  <Block id="cyberfarm:resonance_cell" x="6" y="3" z="3" />
  <Block id="cyberfarm:resonance_cell" x="6" y="3" z="7" />
  <Block id="cyberfarm:resonance_cell" x="7" y="3" z="3" />
  <Block id="cyberfarm:resonance_cell" x="7" y="3" z="4" />
  <Block id="cyberfarm:resonance_cell" x="7" y="3" z="5" />
  <Block id="cyberfarm:resonance_cell" x="7" y="3" z="6" />
  <Block id="cyberfarm:resonance_cell" x="7" y="3" z="7" />
  <Block id="cyberfarm:resonance_cell" x="3" y="4" z="3" />
  <Block id="cyberfarm:resonance_cell" x="3" y="4" z="4" />
  <Block id="cyberfarm:resonance_cell" x="3" y="4" z="5" />
  <Block id="cyberfarm:resonance_cell" x="3" y="4" z="6" />
  <Block id="cyberfarm:resonance_cell" x="3" y="4" z="7" />
  <Block id="cyberfarm:resonance_cell" x="4" y="4" z="3" />
  <Block id="cyberfarm:resonance_cell" x="4" y="4" z="7" />
  <Block id="cyberfarm:resonance_cell" x="5" y="4" z="3" />
  <Block id="cyberfarm:resonance_cell" x="5" y="4" z="7" />
  <Block id="cyberfarm:resonance_cell" x="6" y="4" z="3" />
  <Block id="cyberfarm:resonance_cell" x="6" y="4" z="7" />
  <Block id="cyberfarm:resonance_cell" x="7" y="4" z="3" />
  <Block id="cyberfarm:resonance_cell" x="7" y="4" z="4" />
  <Block id="cyberfarm:resonance_cell" x="7" y="4" z="5" />
  <Block id="cyberfarm:resonance_cell" x="7" y="4" z="6" />
  <Block id="cyberfarm:resonance_cell" x="7" y="4" z="7" />
  <Block id="cyberfarm:resonance_cell" x="3" y="5" z="3" />
  <Block id="cyberfarm:resonance_cell" x="3" y="5" z="4" />
  <Block id="cyberfarm:resonance_cell" x="3" y="5" z="5" />
  <Block id="cyberfarm:resonance_cell" x="3" y="5" z="6" />
  <Block id="cyberfarm:resonance_cell" x="3" y="5" z="7" />
  <Block id="cyberfarm:resonance_cell" x="4" y="5" z="3" />
  <Block id="cyberfarm:resonance_cell" x="4" y="5" z="4" />
  <Block id="cyberfarm:resonance_cell" x="4" y="5" z="5" />
  <Block id="cyberfarm:resonance_cell" x="4" y="5" z="6" />
  <Block id="cyberfarm:resonance_cell" x="4" y="5" z="7" />
  <Block id="cyberfarm:resonance_cell" x="5" y="5" z="3" />
  <Block id="cyberfarm:resonance_cell" x="5" y="5" z="4" />
  <Block id="cyberfarm:resonance_cell" x="5" y="5" z="5" />
  <Block id="cyberfarm:resonance_cell" x="5" y="5" z="6" />
  <Block id="cyberfarm:resonance_cell" x="5" y="5" z="7" />
  <Block id="cyberfarm:resonance_cell" x="6" y="5" z="3" />
  <Block id="cyberfarm:resonance_cell" x="6" y="5" z="4" />
  <Block id="cyberfarm:resonance_cell" x="6" y="5" z="5" />
  <Block id="cyberfarm:resonance_cell" x="6" y="5" z="6" />
  <Block id="cyberfarm:resonance_cell" x="6" y="5" z="7" />
  <Block id="cyberfarm:resonance_cell" x="7" y="5" z="3" />
  <Block id="cyberfarm:resonance_cell" x="7" y="5" z="4" />
  <Block id="cyberfarm:resonance_cell" x="7" y="5" z="5" />
  <Block id="cyberfarm:resonance_cell" x="7" y="5" z="6" />
  <Block id="cyberfarm:resonance_cell" x="7" y="5" z="7" />
  <Block id="cyberfarm:stabilizer_pylon" x="10" y="0" z="5" p:part="bottom" />
  <Block id="cyberfarm:stabilizer_pylon" x="10" y="1" z="5" p:part="middle" />
  <Block id="cyberfarm:stabilizer_pylon" x="10" y="2" z="5" p:part="middle" />
  <Block id="cyberfarm:stabilizer_pylon" x="10" y="3" z="5" p:part="top" />
  <Block id="cyberfarm:stabilizer_pylon" x="0" y="0" z="5" p:part="bottom" />
  <Block id="cyberfarm:stabilizer_pylon" x="0" y="1" z="5" p:part="middle" />
  <Block id="cyberfarm:stabilizer_pylon" x="0" y="2" z="5" p:part="middle" />
  <Block id="cyberfarm:stabilizer_pylon" x="0" y="3" z="5" p:part="top" />
  <Block id="cyberfarm:stabilizer_pylon" x="5" y="0" z="10" p:part="bottom" />
  <Block id="cyberfarm:stabilizer_pylon" x="5" y="1" z="10" p:part="middle" />
  <Block id="cyberfarm:stabilizer_pylon" x="5" y="2" z="10" p:part="middle" />
  <Block id="cyberfarm:stabilizer_pylon" x="5" y="3" z="10" p:part="top" />
</GameScene>

### Tier 4

A 7x7x7 shell of 218 cells and four columns, five blocks tall, six blocks away.

<GameScene zoom={2} interactive={true}>
  <Block id="cyberfarm:resonance_core" x="6" y="0" z="6" p:facing="south" />
  <Block id="cyberfarm:resonance_cell" x="3" y="1" z="3" />
  <Block id="cyberfarm:resonance_cell" x="3" y="1" z="4" />
  <Block id="cyberfarm:resonance_cell" x="3" y="1" z="5" />
  <Block id="cyberfarm:resonance_cell" x="3" y="1" z="6" />
  <Block id="cyberfarm:resonance_cell" x="3" y="1" z="7" />
  <Block id="cyberfarm:resonance_cell" x="3" y="1" z="8" />
  <Block id="cyberfarm:resonance_cell" x="3" y="1" z="9" />
  <Block id="cyberfarm:resonance_cell" x="4" y="1" z="3" />
  <Block id="cyberfarm:resonance_cell" x="4" y="1" z="4" />
  <Block id="cyberfarm:resonance_cell" x="4" y="1" z="5" />
  <Block id="cyberfarm:resonance_cell" x="4" y="1" z="6" />
  <Block id="cyberfarm:resonance_cell" x="4" y="1" z="7" />
  <Block id="cyberfarm:resonance_cell" x="4" y="1" z="8" />
  <Block id="cyberfarm:resonance_cell" x="4" y="1" z="9" />
  <Block id="cyberfarm:resonance_cell" x="5" y="1" z="3" />
  <Block id="cyberfarm:resonance_cell" x="5" y="1" z="4" />
  <Block id="cyberfarm:resonance_cell" x="5" y="1" z="5" />
  <Block id="cyberfarm:resonance_cell" x="5" y="1" z="6" />
  <Block id="cyberfarm:resonance_cell" x="5" y="1" z="7" />
  <Block id="cyberfarm:resonance_cell" x="5" y="1" z="8" />
  <Block id="cyberfarm:resonance_cell" x="5" y="1" z="9" />
  <Block id="cyberfarm:resonance_cell" x="6" y="1" z="3" />
  <Block id="cyberfarm:resonance_cell" x="6" y="1" z="4" />
  <Block id="cyberfarm:resonance_cell" x="6" y="1" z="5" />
  <Block id="cyberfarm:resonance_cell" x="6" y="1" z="6" />
  <Block id="cyberfarm:resonance_cell" x="6" y="1" z="7" />
  <Block id="cyberfarm:resonance_cell" x="6" y="1" z="8" />
  <Block id="cyberfarm:resonance_cell" x="6" y="1" z="9" />
  <Block id="cyberfarm:resonance_cell" x="7" y="1" z="3" />
  <Block id="cyberfarm:resonance_cell" x="7" y="1" z="4" />
  <Block id="cyberfarm:resonance_cell" x="7" y="1" z="5" />
  <Block id="cyberfarm:resonance_cell" x="7" y="1" z="6" />
  <Block id="cyberfarm:resonance_cell" x="7" y="1" z="7" />
  <Block id="cyberfarm:resonance_cell" x="7" y="1" z="8" />
  <Block id="cyberfarm:resonance_cell" x="7" y="1" z="9" />
  <Block id="cyberfarm:resonance_cell" x="8" y="1" z="3" />
  <Block id="cyberfarm:resonance_cell" x="8" y="1" z="4" />
  <Block id="cyberfarm:resonance_cell" x="8" y="1" z="5" />
  <Block id="cyberfarm:resonance_cell" x="8" y="1" z="6" />
  <Block id="cyberfarm:resonance_cell" x="8" y="1" z="7" />
  <Block id="cyberfarm:resonance_cell" x="8" y="1" z="8" />
  <Block id="cyberfarm:resonance_cell" x="8" y="1" z="9" />
  <Block id="cyberfarm:resonance_cell" x="9" y="1" z="3" />
  <Block id="cyberfarm:resonance_cell" x="9" y="1" z="4" />
  <Block id="cyberfarm:resonance_cell" x="9" y="1" z="5" />
  <Block id="cyberfarm:resonance_cell" x="9" y="1" z="6" />
  <Block id="cyberfarm:resonance_cell" x="9" y="1" z="7" />
  <Block id="cyberfarm:resonance_cell" x="9" y="1" z="8" />
  <Block id="cyberfarm:resonance_cell" x="9" y="1" z="9" />
  <Block id="cyberfarm:resonance_cell" x="3" y="2" z="3" />
  <Block id="cyberfarm:resonance_cell" x="3" y="2" z="4" />
  <Block id="cyberfarm:resonance_cell" x="3" y="2" z="5" />
  <Block id="cyberfarm:resonance_cell" x="3" y="2" z="6" />
  <Block id="cyberfarm:resonance_cell" x="3" y="2" z="7" />
  <Block id="cyberfarm:resonance_cell" x="3" y="2" z="8" />
  <Block id="cyberfarm:resonance_cell" x="3" y="2" z="9" />
  <Block id="cyberfarm:resonance_cell" x="4" y="2" z="3" />
  <Block id="cyberfarm:resonance_cell" x="4" y="2" z="9" />
  <Block id="cyberfarm:resonance_cell" x="5" y="2" z="3" />
  <Block id="cyberfarm:resonance_cell" x="5" y="2" z="9" />
  <Block id="cyberfarm:resonance_cell" x="6" y="2" z="3" />
  <Block id="cyberfarm:resonance_cell" x="6" y="2" z="9" />
  <Block id="cyberfarm:resonance_cell" x="7" y="2" z="3" />
  <Block id="cyberfarm:resonance_cell" x="7" y="2" z="9" />
  <Block id="cyberfarm:resonance_cell" x="8" y="2" z="3" />
  <Block id="cyberfarm:resonance_cell" x="8" y="2" z="9" />
  <Block id="cyberfarm:resonance_cell" x="9" y="2" z="3" />
  <Block id="cyberfarm:resonance_cell" x="9" y="2" z="4" />
  <Block id="cyberfarm:resonance_cell" x="9" y="2" z="5" />
  <Block id="cyberfarm:resonance_cell" x="9" y="2" z="6" />
  <Block id="cyberfarm:resonance_cell" x="9" y="2" z="7" />
  <Block id="cyberfarm:resonance_cell" x="9" y="2" z="8" />
  <Block id="cyberfarm:resonance_cell" x="9" y="2" z="9" />
  <Block id="cyberfarm:resonance_cell" x="3" y="3" z="3" />
  <Block id="cyberfarm:resonance_cell" x="3" y="3" z="4" />
  <Block id="cyberfarm:resonance_cell" x="3" y="3" z="5" />
  <Block id="cyberfarm:resonance_cell" x="3" y="3" z="6" />
  <Block id="cyberfarm:resonance_cell" x="3" y="3" z="7" />
  <Block id="cyberfarm:resonance_cell" x="3" y="3" z="8" />
  <Block id="cyberfarm:resonance_cell" x="3" y="3" z="9" />
  <Block id="cyberfarm:resonance_cell" x="4" y="3" z="3" />
  <Block id="cyberfarm:resonance_cell" x="4" y="3" z="9" />
  <Block id="cyberfarm:resonance_cell" x="5" y="3" z="3" />
  <Block id="cyberfarm:resonance_cell" x="5" y="3" z="9" />
  <Block id="cyberfarm:resonance_cell" x="6" y="3" z="3" />
  <Block id="cyberfarm:resonance_cell" x="6" y="3" z="9" />
  <Block id="cyberfarm:resonance_cell" x="7" y="3" z="3" />
  <Block id="cyberfarm:resonance_cell" x="7" y="3" z="9" />
  <Block id="cyberfarm:resonance_cell" x="8" y="3" z="3" />
  <Block id="cyberfarm:resonance_cell" x="8" y="3" z="9" />
  <Block id="cyberfarm:resonance_cell" x="9" y="3" z="3" />
  <Block id="cyberfarm:resonance_cell" x="9" y="3" z="4" />
  <Block id="cyberfarm:resonance_cell" x="9" y="3" z="5" />
  <Block id="cyberfarm:resonance_cell" x="9" y="3" z="6" />
  <Block id="cyberfarm:resonance_cell" x="9" y="3" z="7" />
  <Block id="cyberfarm:resonance_cell" x="9" y="3" z="8" />
  <Block id="cyberfarm:resonance_cell" x="9" y="3" z="9" />
  <Block id="cyberfarm:resonance_cell" x="3" y="4" z="3" />
  <Block id="cyberfarm:resonance_cell" x="3" y="4" z="4" />
  <Block id="cyberfarm:resonance_cell" x="3" y="4" z="5" />
  <Block id="cyberfarm:resonance_cell" x="3" y="4" z="6" />
  <Block id="cyberfarm:resonance_cell" x="3" y="4" z="7" />
  <Block id="cyberfarm:resonance_cell" x="3" y="4" z="8" />
  <Block id="cyberfarm:resonance_cell" x="3" y="4" z="9" />
  <Block id="cyberfarm:resonance_cell" x="4" y="4" z="3" />
  <Block id="cyberfarm:resonance_cell" x="4" y="4" z="9" />
  <Block id="cyberfarm:resonance_cell" x="5" y="4" z="3" />
  <Block id="cyberfarm:resonance_cell" x="5" y="4" z="9" />
  <Block id="cyberfarm:resonance_cell" x="6" y="4" z="3" />
  <Block id="cyberfarm:resonance_cell" x="6" y="4" z="9" />
  <Block id="cyberfarm:resonance_cell" x="7" y="4" z="3" />
  <Block id="cyberfarm:resonance_cell" x="7" y="4" z="9" />
  <Block id="cyberfarm:resonance_cell" x="8" y="4" z="3" />
  <Block id="cyberfarm:resonance_cell" x="8" y="4" z="9" />
  <Block id="cyberfarm:resonance_cell" x="9" y="4" z="3" />
  <Block id="cyberfarm:resonance_cell" x="9" y="4" z="4" />
  <Block id="cyberfarm:resonance_cell" x="9" y="4" z="5" />
  <Block id="cyberfarm:resonance_cell" x="9" y="4" z="6" />
  <Block id="cyberfarm:resonance_cell" x="9" y="4" z="7" />
  <Block id="cyberfarm:resonance_cell" x="9" y="4" z="8" />
  <Block id="cyberfarm:resonance_cell" x="9" y="4" z="9" />
  <Block id="cyberfarm:resonance_cell" x="3" y="5" z="3" />
  <Block id="cyberfarm:resonance_cell" x="3" y="5" z="4" />
  <Block id="cyberfarm:resonance_cell" x="3" y="5" z="5" />
  <Block id="cyberfarm:resonance_cell" x="3" y="5" z="6" />
  <Block id="cyberfarm:resonance_cell" x="3" y="5" z="7" />
  <Block id="cyberfarm:resonance_cell" x="3" y="5" z="8" />
  <Block id="cyberfarm:resonance_cell" x="3" y="5" z="9" />
  <Block id="cyberfarm:resonance_cell" x="4" y="5" z="3" />
  <Block id="cyberfarm:resonance_cell" x="4" y="5" z="9" />
  <Block id="cyberfarm:resonance_cell" x="5" y="5" z="3" />
  <Block id="cyberfarm:resonance_cell" x="5" y="5" z="9" />
  <Block id="cyberfarm:resonance_cell" x="6" y="5" z="3" />
  <Block id="cyberfarm:resonance_cell" x="6" y="5" z="9" />
  <Block id="cyberfarm:resonance_cell" x="7" y="5" z="3" />
  <Block id="cyberfarm:resonance_cell" x="7" y="5" z="9" />
  <Block id="cyberfarm:resonance_cell" x="8" y="5" z="3" />
  <Block id="cyberfarm:resonance_cell" x="8" y="5" z="9" />
  <Block id="cyberfarm:resonance_cell" x="9" y="5" z="3" />
  <Block id="cyberfarm:resonance_cell" x="9" y="5" z="4" />
  <Block id="cyberfarm:resonance_cell" x="9" y="5" z="5" />
  <Block id="cyberfarm:resonance_cell" x="9" y="5" z="6" />
  <Block id="cyberfarm:resonance_cell" x="9" y="5" z="7" />
  <Block id="cyberfarm:resonance_cell" x="9" y="5" z="8" />
  <Block id="cyberfarm:resonance_cell" x="9" y="5" z="9" />
  <Block id="cyberfarm:resonance_cell" x="3" y="6" z="3" />
  <Block id="cyberfarm:resonance_cell" x="3" y="6" z="4" />
  <Block id="cyberfarm:resonance_cell" x="3" y="6" z="5" />
  <Block id="cyberfarm:resonance_cell" x="3" y="6" z="6" />
  <Block id="cyberfarm:resonance_cell" x="3" y="6" z="7" />
  <Block id="cyberfarm:resonance_cell" x="3" y="6" z="8" />
  <Block id="cyberfarm:resonance_cell" x="3" y="6" z="9" />
  <Block id="cyberfarm:resonance_cell" x="4" y="6" z="3" />
  <Block id="cyberfarm:resonance_cell" x="4" y="6" z="9" />
  <Block id="cyberfarm:resonance_cell" x="5" y="6" z="3" />
  <Block id="cyberfarm:resonance_cell" x="5" y="6" z="9" />
  <Block id="cyberfarm:resonance_cell" x="6" y="6" z="3" />
  <Block id="cyberfarm:resonance_cell" x="6" y="6" z="9" />
  <Block id="cyberfarm:resonance_cell" x="7" y="6" z="3" />
  <Block id="cyberfarm:resonance_cell" x="7" y="6" z="9" />
  <Block id="cyberfarm:resonance_cell" x="8" y="6" z="3" />
  <Block id="cyberfarm:resonance_cell" x="8" y="6" z="9" />
  <Block id="cyberfarm:resonance_cell" x="9" y="6" z="3" />
  <Block id="cyberfarm:resonance_cell" x="9" y="6" z="4" />
  <Block id="cyberfarm:resonance_cell" x="9" y="6" z="5" />
  <Block id="cyberfarm:resonance_cell" x="9" y="6" z="6" />
  <Block id="cyberfarm:resonance_cell" x="9" y="6" z="7" />
  <Block id="cyberfarm:resonance_cell" x="9" y="6" z="8" />
  <Block id="cyberfarm:resonance_cell" x="9" y="6" z="9" />
  <Block id="cyberfarm:resonance_cell" x="3" y="7" z="3" />
  <Block id="cyberfarm:resonance_cell" x="3" y="7" z="4" />
  <Block id="cyberfarm:resonance_cell" x="3" y="7" z="5" />
  <Block id="cyberfarm:resonance_cell" x="3" y="7" z="6" />
  <Block id="cyberfarm:resonance_cell" x="3" y="7" z="7" />
  <Block id="cyberfarm:resonance_cell" x="3" y="7" z="8" />
  <Block id="cyberfarm:resonance_cell" x="3" y="7" z="9" />
  <Block id="cyberfarm:resonance_cell" x="4" y="7" z="3" />
  <Block id="cyberfarm:resonance_cell" x="4" y="7" z="4" />
  <Block id="cyberfarm:resonance_cell" x="4" y="7" z="5" />
  <Block id="cyberfarm:resonance_cell" x="4" y="7" z="6" />
  <Block id="cyberfarm:resonance_cell" x="4" y="7" z="7" />
  <Block id="cyberfarm:resonance_cell" x="4" y="7" z="8" />
  <Block id="cyberfarm:resonance_cell" x="4" y="7" z="9" />
  <Block id="cyberfarm:resonance_cell" x="5" y="7" z="3" />
  <Block id="cyberfarm:resonance_cell" x="5" y="7" z="4" />
  <Block id="cyberfarm:resonance_cell" x="5" y="7" z="5" />
  <Block id="cyberfarm:resonance_cell" x="5" y="7" z="6" />
  <Block id="cyberfarm:resonance_cell" x="5" y="7" z="7" />
  <Block id="cyberfarm:resonance_cell" x="5" y="7" z="8" />
  <Block id="cyberfarm:resonance_cell" x="5" y="7" z="9" />
  <Block id="cyberfarm:resonance_cell" x="6" y="7" z="3" />
  <Block id="cyberfarm:resonance_cell" x="6" y="7" z="4" />
  <Block id="cyberfarm:resonance_cell" x="6" y="7" z="5" />
  <Block id="cyberfarm:resonance_cell" x="6" y="7" z="6" />
  <Block id="cyberfarm:resonance_cell" x="6" y="7" z="7" />
  <Block id="cyberfarm:resonance_cell" x="6" y="7" z="8" />
  <Block id="cyberfarm:resonance_cell" x="6" y="7" z="9" />
  <Block id="cyberfarm:resonance_cell" x="7" y="7" z="3" />
  <Block id="cyberfarm:resonance_cell" x="7" y="7" z="4" />
  <Block id="cyberfarm:resonance_cell" x="7" y="7" z="5" />
  <Block id="cyberfarm:resonance_cell" x="7" y="7" z="6" />
  <Block id="cyberfarm:resonance_cell" x="7" y="7" z="7" />
  <Block id="cyberfarm:resonance_cell" x="7" y="7" z="8" />
  <Block id="cyberfarm:resonance_cell" x="7" y="7" z="9" />
  <Block id="cyberfarm:resonance_cell" x="8" y="7" z="3" />
  <Block id="cyberfarm:resonance_cell" x="8" y="7" z="4" />
  <Block id="cyberfarm:resonance_cell" x="8" y="7" z="5" />
  <Block id="cyberfarm:resonance_cell" x="8" y="7" z="6" />
  <Block id="cyberfarm:resonance_cell" x="8" y="7" z="7" />
  <Block id="cyberfarm:resonance_cell" x="8" y="7" z="8" />
  <Block id="cyberfarm:resonance_cell" x="8" y="7" z="9" />
  <Block id="cyberfarm:resonance_cell" x="9" y="7" z="3" />
  <Block id="cyberfarm:resonance_cell" x="9" y="7" z="4" />
  <Block id="cyberfarm:resonance_cell" x="9" y="7" z="5" />
  <Block id="cyberfarm:resonance_cell" x="9" y="7" z="6" />
  <Block id="cyberfarm:resonance_cell" x="9" y="7" z="7" />
  <Block id="cyberfarm:resonance_cell" x="9" y="7" z="8" />
  <Block id="cyberfarm:resonance_cell" x="9" y="7" z="9" />
  <Block id="cyberfarm:stabilizer_pylon" x="12" y="0" z="6" p:part="bottom" />
  <Block id="cyberfarm:stabilizer_pylon" x="12" y="1" z="6" p:part="middle" />
  <Block id="cyberfarm:stabilizer_pylon" x="12" y="2" z="6" p:part="middle" />
  <Block id="cyberfarm:stabilizer_pylon" x="12" y="3" z="6" p:part="middle" />
  <Block id="cyberfarm:stabilizer_pylon" x="12" y="4" z="6" p:part="top" />
  <Block id="cyberfarm:stabilizer_pylon" x="0" y="0" z="6" p:part="bottom" />
  <Block id="cyberfarm:stabilizer_pylon" x="0" y="1" z="6" p:part="middle" />
  <Block id="cyberfarm:stabilizer_pylon" x="0" y="2" z="6" p:part="middle" />
  <Block id="cyberfarm:stabilizer_pylon" x="0" y="3" z="6" p:part="middle" />
  <Block id="cyberfarm:stabilizer_pylon" x="0" y="4" z="6" p:part="top" />
  <Block id="cyberfarm:stabilizer_pylon" x="6" y="0" z="12" p:part="bottom" />
  <Block id="cyberfarm:stabilizer_pylon" x="6" y="1" z="12" p:part="middle" />
  <Block id="cyberfarm:stabilizer_pylon" x="6" y="2" z="12" p:part="middle" />
  <Block id="cyberfarm:stabilizer_pylon" x="6" y="3" z="12" p:part="middle" />
  <Block id="cyberfarm:stabilizer_pylon" x="6" y="4" z="12" p:part="top" />
  <Block id="cyberfarm:stabilizer_pylon" x="6" y="0" z="0" p:part="bottom" />
  <Block id="cyberfarm:stabilizer_pylon" x="6" y="1" z="0" p:part="middle" />
  <Block id="cyberfarm:stabilizer_pylon" x="6" y="2" z="0" p:part="middle" />
  <Block id="cyberfarm:stabilizer_pylon" x="6" y="3" z="0" p:part="middle" />
  <Block id="cyberfarm:stabilizer_pylon" x="6" y="4" z="0" p:part="top" />
</GameScene>

## The screen

Right-click **any part** of the core to open it: the pedestal, any cell of the shell (finished or
not) or any block of a pylon column lined up with it. The screen shows:

- its **tier and shell**, and whether it is **Online** or **Unstable**;
- the pylon columns it has and needs (point at the line for what this tier asks for);
- **what it found on each side**: a linked pylon, or why one does not count (no pylon, too close,
  too far, too short, beam blocked); point at a side for the column's height and distance;
- how much each pylon carries and the FE stored.

- Breaking a cell of the shell drops the core back a tier, and **the energy that no longer fits is
  lost**: empty the core before rebuilding it.
- Breaking the pedestal leaves the shell's cells where they are, visible again, but **the energy
  stored is lost**.

<RecipeFor id="resonance_core" />
