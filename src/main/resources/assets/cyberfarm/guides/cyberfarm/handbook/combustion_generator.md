---
navigation:
  title: Combustion Generator
  parent: generators.md
  position: 1
  icon: cyberfarm:combustion_generator
item_ids:
  - cyberfarm:combustion_generator
---

# Combustion Generator

<BlockImage id="cyberfarm:combustion_generator" p:lit="true" scale="4" float="left" />

The first power source of CyberFarm. It burns **any furnace fuel** (coal, charcoal, wood,
blaze rods, lava buckets...) and turns it into FE. While it burns, the vent on its chimney spins, and it coasts to a stop when the fuel is gone.

<br clear="all" />

| Stat | Value |
| --- | --- |
| Generation | 60 FE/t while burning |
| Buffer | 50,000 FE |
| Output | up to 400 FE/t per side |
| One coal | 48,000 FE |

The generator **pushes power automatically** into every adjacent block that accepts FE:
CyberFarm machines, [Energy Conduits](energy_conduit.md), and machines and cables from other mods. Its fire holds while its
buffer is full and goes on when there is room again, so no fuel is wasted.

Lava buckets leave an empty bucket in the slot. Take it out to refuel.

<RecipeFor id="combustion_generator" />
