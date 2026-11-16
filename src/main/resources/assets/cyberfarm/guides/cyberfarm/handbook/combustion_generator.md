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
blaze rods, lava buckets...) and turns it into FE. It needs no multiblock, so it is the way to power your first machines.

<br clear="all" />

## Using it

- Put fuel in its slot.
- It **pushes power by itself** into every adjacent block that accepts FE: CyberFarm machines,
  [Energy Conduits](energy_conduit.md), and machines and cables from other mods.
- While it burns, the vent on its chimney spins. It coasts to a stop when the fuel is gone.

## Numbers

| Stat | Value |
| --- | --- |
| Generation | 60 FE/t while burning |
| Buffer | 50,000 FE |
| Output | up to 400 FE/t per side |
| Burn time | half of a furnace's |
| One coal | 48,000 FE (40 seconds) |

See [Generators](generators.md) for how long each fuel lasts.

## Tips

- When its buffer is full, the fire holds until there is room again. No fuel is wasted.
- A lava bucket leaves an empty bucket in the slot. Take it out to refuel.

<RecipeFor id="combustion_generator" />
