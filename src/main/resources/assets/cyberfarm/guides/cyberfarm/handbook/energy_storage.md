---
navigation:
  title: Energy Storage
  position: 3
  icon: cyberfarm:resonance_core
---

# Energy Storage

Generators make power only while they run, and reactors make far more than a base uses at once.
Energy storage keeps the surplus for later: charge it while the reactor runs at full power, then draw
from it at night, while a turbine spins up or when a whole drone fleet comes home to charge.

CyberFarm has two kinds of storage: a small one to start with and a big one you build up over time.

## Battery Bank: the small one

<ItemImage id="cyberfarm:battery_bank" scale="2" float="left" />

The [Battery Bank](battery_bank.md) is a block that stores **150,000 FE**. Blocks placed together join
into one bank of up to **3x3x3**, which stores 4,050,000 FE. It is enough for a small base, and it can
feed a [Human Dock](human_dock.md) directly.

<br clear="all" />

## Resonance Core: the big one

CyberFarm's big battery is a structure you **build bigger**, tier by tier. It has three parts:

- <ItemLink id="resonance_core" />: the pedestal with the floating crystal. It is the battery itself
  and has the screen.
- <ItemLink id="resonance_cell" />: the blocks of the shell around the crystal. A bigger shell is a
  higher tier that stores more.
- <ItemLink id="stabilizer_pylon" />: columns of pylons that hold the crystal steady. They are its
  **only way in and out** for power, so it works with [Energy Conduits](energy_conduit.md) and with
  any other mod's cables.

<ItemGrid>
  <ItemIcon id="resonance_core" />
  <ItemIcon id="resonance_cell" />
  <ItemIcon id="stabilizer_pylon" />
</ItemGrid>

| Tier | Shell of cells | Stores | Pylon columns | Each column carries |
| --- | --- | --- | --- | --- |
| 1 | none | 1,000,000 FE | 1 | 16,000 FE/t |
| 2 | 3x3x3 | 32,000,000 FE | 2 | 128,000 FE/t |
| 3 | 5x5x5 | 256,000,000 FE | 3 | 512,000 FE/t |
| 4 | 7x7x7 | 2,000,000,000 FE | 4 | 2,000,000 FE/t |

## Pages

<SubPages icons={true} />
