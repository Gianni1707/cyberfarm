---
navigation:
  title: Waste and Radiation
  parent: nuclear.md
  position: 5
  icon: cyberfarm:depleted_fuel_rod
item_ids:
  - cyberfarm:depleted_fuel_rod
---

# Waste and Radiation

<ItemImage id="cyberfarm:depleted_fuel_rod" scale="2" float="left" />

Every spent Fuel Rod becomes a **Depleted Fuel Rod**: nuclear waste. Left untreated it makes the
area around it **radioactive**. Treat it in a [Waste Reprocessor](waste_reprocessor.md).

<br clear="all" />

## What contaminates

Radiation builds up chunk by chunk (16x16 columns of the world):

- Depleted Fuel Rods **carried** by a player, **stored** in a chest or any container (storage
  blocks of other mods included), or **lying on the ground**: every rod adds a little every few seconds.
- A Fission Reactor whose **waste slots are full**: the spent rods wait in the core and it **leaks**.
- A **meltdown**: heavy contamination over a 5x5 chunk area, worst at the core.

Radiation fades away by itself over a few minutes once the source is gone; a meltdown site takes
much longer. Waste inside the reactor's waste slots or a Waste Reprocessor is shielded.

## What it does

| Radiation | Effect |
| --- | --- |
| below 20 Rad | harmless |
| 20 Rad | **Radiation Sickness I**: hunger and damage every 3 seconds |
| 100 Rad | **Radiation Sickness II**: also weakness, damage more often |
| 300 Rad | **Radiation Sickness III**: also nausea, damage almost every second |
| 50 Rad and more | the land **withers**: grass dries to dirt, plants wilt into dead bushes, leaves fall |

Radiation Sickness hits players and mobs alike and goes through normal armor. A
[Hazmat Suit](hazmat_suit.md) keeps it out, and a [Geiger Counter](hazmat_suit.md) tells you how
much there is. Withering land can be turned off in the common config (*Radiation Withers Land*).
