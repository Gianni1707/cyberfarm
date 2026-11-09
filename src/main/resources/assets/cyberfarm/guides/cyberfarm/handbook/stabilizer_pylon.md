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

Columns of pylons hold a [Resonance Core](resonance_core.md)'s crystal steady: each sends out a drone that circles it,
and they are the **only way power goes in and out** of the core.

<br clear="all" />

| Tier | Columns | Column height | Distance | Each column carries |
| --- | --- | --- | --- | --- |
| 1 | 1 | 2 | 3-8 | 16,000 FE/t |
| 2 | 2 | 3 | 4-10 | 128,000 FE/t |
| 3 | 3 | 4 | 5-12 | 512,000 FE/t |
| 4 | 4 | 5 | 6-14 | 2,000,000 FE/t |

## Building the columns

Stack <ItemLink id="stabilizer_pylon" />s into **columns**: the bottom block gets a footing, the
top one a **cradle** with a landing pad. When a column links up, its drone lifts off the cradle, flies to the
crystal and circles it, rising and falling, and sends it waves that hold it steady. Unstable, the drones falter, tilt and their waves turn violet.

- Put the columns on the **same level** as the pedestal, **in line with it** on its sides
  (north, south, east, west), one column per side.
- Each tier needs **as many columns as its number**, each **one block taller** than the tier and
  within the distance in the table, counted from the pedestal.
- Nothing may stand between a column and the crystal at the crystal's height.

With enough columns the core is **Online**: the crystal spins, shines white and breathes, closing as the waves land,
and its heart grows as it fills up. With too few it is **Unstable**: the crystal slows down and flickers violet, its breath lurches and
**nothing goes in or out**. What it already stores is kept.

## Power in and out

The pylons are the core's **only way in and out**: connect cables to **any block** of a linked
column; an Energy Conduit plugs into a socket that appears on the pylon's side. Each column takes
and gives up to the amount in the table.

- Cables and generators fill the core through any linked pylon, CyberFarm's or any other mod's.
- The core never pushes power out by itself. To draw from it, **right-click the arm** of the
  [Energy Conduit](energy_conduit.md) that touches a pylon, so the cable **pulls** (see
  [Cables](cables.md)); other mods' cables that pull work too.

<RecipeFor id="stabilizer_pylon" />
