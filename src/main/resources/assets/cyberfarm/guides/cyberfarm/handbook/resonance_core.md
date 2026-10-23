---
navigation:
  title: Resonance Core
  parent: energy_storage.md
  position: 1
  icon: cyberfarm:resonance_core
item_ids:
  - cyberfarm:resonance_core
  - cyberfarm:resonance_cell
  - cyberfarm:stabilizer_pylon
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

## Building the shell

The shell is a hollow cube of <ItemLink id="resonance_cell" />s **right above the pedestal**,
centred on it: 3x3x3 for tier 2, 5x5x5 for tier 3, 7x7x7 for tier 4. Every block of the cube's
surface must be a cell and its **inside must stay empty**. When the last cell goes in, the cells
vanish and the crystal grows to fill the shell. The [Multiblock Projector](multiblock_projector.md)
shows every tier, pylons included: sneak + right-click to pick it, then click the pedestal.

## Stabilizer Pylons

Stack <ItemLink id="stabilizer_pylon" />s into **columns**: the bottom block gets a footing, the
top one the glowing **emitter ring**. The beam leaves through a pulsing lens on the side of the
ring that faces the crystal, at the crystal's height.

- Put the columns on the **same level** as the pedestal, **in line with it** on its sides
  (north, south, east, west), one column per side.
- Each tier needs **as many columns as its number**, each **one block taller** than the tier and
  within the distance in the table, counted from the pedestal.
- Nothing may stand between a column and the crystal at the crystal's height.

With enough columns the core is **Online**: the crystal spins and shines cyan and its heart grows as
it fills up. With too few it is **Unstable**: the crystal slows down and flickers violet and
**nothing goes in or out**. What it already stores is kept.

## Power in and out

The pylons are the core's **only way in and out**: connect cables to **any block** of a linked
column; an Energy Conduit plugs into a socket that appears on the pylon's side. Each column takes
and gives up to the amount in the table.

- Cables and generators fill the core through any linked pylon, CyberFarm's or any other mod's.
- The core never pushes power out by itself. To draw from it, **right-click the arm** of the
  [Energy Conduit](energy_conduit.md) that touches a pylon, so the cable **pulls** (see
  [Cables](cables.md)); other mods' cables that pull work too.

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

<RecipeFor id="resonance_cell" />

<RecipeFor id="stabilizer_pylon" />
