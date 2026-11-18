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

CyberFarm's big battery, from 1,000,000 FE up to 2,000,000,000 FE: a pedestal with a crystal floating
over it, built bigger tier by tier from three blocks.

<br clear="all" />

<ItemGrid>
  <ItemIcon id="resonance_core" />
  <ItemIcon id="resonance_cell" />
  <ItemIcon id="stabilizer_pylon" />
</ItemGrid>

## Building it

1. **Core**: place the <ItemLink id="resonance_core" />, the pedestal. Alone it is tier 1.
2. **Shell** (tiers 2 to 4): a **hollow cube** of <ItemLink id="resonance_cell" />s right above the
   pedestal, centred on it. Every block of its surface is a cell and **the middle block stays empty**:
   the crystal grows there, so a 3x3x3 takes 26 cells and does not form with a 27th in the middle. To go
   up a tier, build the next shell round the old one: its cells can stay inside. When the last cell goes
   in, the cells vanish into the bigger crystal. One craft makes 4 cells.
3. **Pylon columns**: stacks of <ItemLink id="stabilizer_pylon" />s on the **same level** as the
   pedestal, **in line with it** on its sides, one per side. A tier needs as many columns as its
   number, each at least one block taller than the tier, within the distance in the table, with
   nothing between it and the crystal. A linked column's drone flies up and circles the crystal.

The [Multiblock Projector](multiblock_projector.md) shows tiers 2 to 4, pylons included: sneak +
right-click to pick the tier, then click the pedestal.

## Every tier

At its smallest, with the pylons at their closest. Drag to turn the scene.

### Tier 1

The core alone: one pylon column, two blocks tall, at least three blocks away.

<GameScene zoom={4} interactive={true}>
  <Block id="cyberfarm:resonance_core" x="3" y="0" z="3" p:facing="south" />
  <Block id="cyberfarm:stabilizer_pylon" x="6" y="0" z="3" p:part="bottom" />
  <Block id="cyberfarm:stabilizer_pylon" x="6" y="1" z="3" p:part="top" />
</GameScene>

### Tier 2

A 3x3x3 shell of 26 cells (the middle stays empty) and two columns, three blocks tall, four blocks away.

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

## Online, Unstable and the screen

- With enough linked columns the core is **Online**: the crystal spins, shines white and its heart
  grows as it fills.
- With too few it is **Unstable**: the crystal flickers violet and **nothing goes in or out**, but
  what it stores is kept.

Right-click **any part** to open the screen: the pedestal, any cell (finished shell or not) or any block
of a lined-up column. It shows the tier, the columns it has and needs, what it found on each side (a
linked pylon, or no pylon, too close, too far, too short, beam blocked) and the FE stored.

## Power in and out

Power goes in and out **only through linked pylons**: connect cables to **any block** of a column. An
[Energy Conduit](energy_conduit.md) plugs into a socket on the pylon's side.

- **To fill it**: cables and generators push into a pylon, CyberFarm's or any other mod's.
- **To draw from it**: the core never pushes by itself. **Right-click the arm** of the conduit that
  touches a pylon so it **pulls** (see [Cables](cables.md)).

## Numbers

| Tier | Shell (cells) | Stores | Columns | Column height | Distance | Each column carries |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | none | 1,000,000 FE | 1 | 2 | 3-8 | 16,000 FE/t |
| 2 | 3x3x3 (26) | 32,000,000 FE | 2 | 3 | 4-10 | 128,000 FE/t |
| 3 | 5x5x5 (98) | 256,000,000 FE | 3 | 4 | 5-12 | 512,000 FE/t |
| 4 | 7x7x7 (218) | 2,000,000,000 FE | 4 | 5 | 6-14 | 2,000,000 FE/t |

Column height is the least a column needs; distance is in blocks from the pedestal.

## Tips

- Breaking a cell drops the core back a tier, and **the energy that no longer fits is lost**. Empty
  the core before you rebuild it.
- Breaking the pedestal leaves the cells visible again, but **all the energy stored is lost**.

<RecipeFor id="resonance_core" />

<RecipeFor id="resonance_cell" />

<RecipeFor id="stabilizer_pylon" />
