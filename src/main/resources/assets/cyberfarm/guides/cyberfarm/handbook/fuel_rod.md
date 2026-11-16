---
navigation:
  title: Fuel Rod
  parent: nuclear.md
  position: 2
  icon: cyberfarm:fuel_rod
item_ids:
  - cyberfarm:fuel_rod
---

# Fuel Rod

<ItemImage id="cyberfarm:fuel_rod" scale="2" float="left" />

A Fuel Rod is what the [Fission Reactor](fission_reactor.md) burns. Each burning rod heats the core,
and that heat becomes the steam that drives the turbines.

<br clear="all" />

## Crafting

3 [Uranium](uranium.md) Ingots and 6 Iron Ingots.

<RecipeFor id="fuel_rod" />

## Using it

- Put rods in the reactor's fuel slots, one per slot: by hand, or with a hopper or item pipe into a
  **Reactor Port**.
- A Small tower burns 2 rods, a Medium one 4, a Large one 8. The slots a tower cannot burn are
  dimmed.
- All the rods in the tower burn together, and only while the reactor's power is above Off.
- The durability bar shows the fuel left; the tooltip shows it in minutes and seconds.
- A spent rod becomes a **Depleted Fuel Rod** in the reactor's waste slots. That is nuclear waste:
  see [Waste and Radiation](radiation.md).

## How long a rod lasts

A rod lasts **40 minutes at 100% power**. Higher power burns it faster in the same proportion:
at 200% it lasts half as long.

| Power | Rod life |
| --- | --- |
| 50% | 80 min |
| 100% | 40 min |
| 150% | 26 min 40 s |
| 200% | 20 min |
| 250% | 16 min |
| 300% | 13 min 20 s |
| 350% | 11 min 26 s |
| 400% | 10 min |
| 450% | 8 min 53 s |
| 500% | 8 min |

Every rod gives the **same total steam**, whatever the power: **2,400,000 mB**, as long as the
reactor does not overheat. Higher power only gets it out faster. In
[Steam Turbines](steam_turbine.md) that is 24, 38.4 or **60 million FE** per rod (Small, Medium or
Large turbines).
