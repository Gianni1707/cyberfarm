---
navigation:
  title: Energy Storage
  position: 3
  icon: cyberfarm:resonance_core
---

# Energy Storage

Generators make power only while they run, and reactors make far more than a base uses at once.
Energy storage keeps the surplus for later: charge it while the reactor runs at full power, draw
from it at night, while a turbine spins up or when a whole drone fleet comes home to charge.

CyberFarm's battery is a structure you **build bigger** over time. It has three parts:

- <ItemLink id="resonance_core" /> – the pedestal with the floating crystal: the battery itself and
  its screen.
- <ItemLink id="resonance_cell" /> – the blocks of the shell around the crystal: a bigger shell is a
  higher tier that stores more.
- <ItemLink id="stabilizer_pylon" /> – columns of pylons that hold the crystal steady and are its
  only way in and out for power.

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

Power goes in and out **through the pylons**, so it works with [Energy Conduits](energy_conduit.md)
and with any other mod's cables.

## Pages

<SubPages icons={true} />
