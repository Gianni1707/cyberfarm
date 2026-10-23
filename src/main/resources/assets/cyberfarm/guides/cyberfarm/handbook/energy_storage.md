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

<ItemGrid>
  <ItemIcon id="resonance_core" />
  <ItemIcon id="resonance_cell" />
  <ItemIcon id="stabilizer_pylon" />
</ItemGrid>

## The Resonance Core

CyberFarm's battery is the [Resonance Core](resonance_core.md): a crystal floating over its
pedestal, held steady by beams from columns of **Stabilizer Pylons**. You **build it bigger**: a
shell of **Resonance Cells** around the crystal raises it by a tier and the crystal grows to fill
it, and every tier asks for more pylons, taller and farther away.

| Tier | Shell | Stores | Per pylon |
| --- | --- | --- | --- |
| 1 | none | 1,000,000 FE | 16,000 FE/t |
| 2 | 3x3x3 | 32,000,000 FE | 128,000 FE/t |
| 3 | 5x5x5 | 256,000,000 FE | 512,000 FE/t |
| 4 | 7x7x7 | 2,000,000,000 FE | 2,000,000 FE/t |

Power goes in and out **through the pylons**, so it works with [Energy Conduits](energy_conduit.md)
and with any other mod's cables.
