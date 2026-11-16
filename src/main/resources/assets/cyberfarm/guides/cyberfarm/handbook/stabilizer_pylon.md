---
navigation:
  title: Stabilizer Pylon
  parent: energy_storage.md
  position: 3
  icon: cyberfarm:stabilizer_pylon
item_ids:
  - cyberfarm:stabilizer_pylon
---

# Stabilizer Pylon

<BlockImage id="cyberfarm:stabilizer_pylon" scale="4" float="left" />

Columns of Stabilizer Pylons hold a [Resonance Core](resonance_core.md)'s crystal steady. They are also
the **only way power goes in and out** of the core, so every core needs them.

<br clear="all" />

## Building the columns

Stack pylons into **columns**. The bottom block gets a footing, the top one a **cradle** with a landing pad.

- Put the columns on the **same level** as the pedestal, **in line with it** on its sides (north,
  south, east, west), one column per side.
- Each tier needs **as many columns as its number**.
- Each column must be at least **one block taller** than the tier, and within the distance in the
  table, counted from the pedestal.
- Nothing may stand between a column and the crystal at the crystal's height.

When a column links up, its drone lifts off the cradle, flies to the crystal and circles it, rising and
falling. It sends the crystal waves that hold it steady.

## Online and Unstable

- With enough linked columns the core is **Online**: the crystal spins, shines white and breathes,
  closing as the waves land. Its heart grows as it fills up.
- With too few it is **Unstable**: the crystal slows down and flickers violet, its breath lurches, and
  the drones falter, tilt and send violet waves. **Nothing goes in or out**, but what it already stores
  is kept.

The core's screen tells you what is wrong with each side: no pylon, too close, too far, too short or
beam blocked. Right-click any block of a lined-up column to open it.

## Power in and out

Connect cables to **any block** of a linked column. An [Energy Conduit](energy_conduit.md) plugs into a
socket that appears on the pylon's side.

- **To fill the core**: cables and generators push into any linked pylon, CyberFarm's or any other mod's.
- **To draw from it**: the core never pushes power out by itself. **Right-click the arm** of the Energy
  Conduit that touches a pylon, so the cable **pulls** (see [Cables](cables.md)). Other mods' cables
  that pull work too.

## Numbers

| Tier | Columns | Column height | Distance | Each column carries |
| --- | --- | --- | --- | --- |
| 1 | 1 | 2 | 3-8 | 16,000 FE/t |
| 2 | 2 | 3 | 4-10 | 128,000 FE/t |
| 3 | 3 | 4 | 5-12 | 512,000 FE/t |
| 4 | 4 | 5 | 6-14 | 2,000,000 FE/t |

Each linked column takes in and gives out up to its tier's FE/t.

<RecipeFor id="stabilizer_pylon" />
