---
navigation:
  title: Waste and Radiation
  parent: nuclear.md
  position: 7
  icon: cyberfarm:depleted_fuel_rod
item_ids:
  - cyberfarm:depleted_fuel_rod
---

# Waste and Radiation

<ItemImage id="cyberfarm:depleted_fuel_rod" scale="2" float="left" />

Every spent [Fuel Rod](fuel_rod.md) becomes a **Depleted Fuel Rod**: nuclear waste. Left untreated
it makes the area around it **radioactive**, and radiation makes people and animals sick. Treat it
in a [Waste Reprocessor](waste_reprocessor.md).

<br clear="all" />

Depleted Fuel Rods stack to 16.

## What contaminates

Radiation is measured in **Rad** and builds up chunk by chunk (16x16 columns of the world).

- Depleted Fuel Rods **carried** by a player, **stored** in a chest or any container (storage
  blocks of other mods included), or **lying on the ground**. Every rod adds **2.5 Rad every
  5 seconds** to its chunk. A single rod in your inventory makes your chunk unsafe in about a minute.
- A [Fission Reactor](fission_reactor.md) whose **waste slots are full**: the spent rods wait in the
  core and it **leaks**.
- A **meltdown**: up to **400 Rad** over a 5x5 chunk area, worst at the core.

Waste is **shielded** inside the reactor's waste slots, a Reactor Port and any CyberFarm machine,
such as the Waste Reprocessor. Stored waste only counts while a player is within 2 chunks.

Radiation fades by itself once the source is gone. A small leak is gone in a few minutes; a meltdown
site takes much longer.

## What it does

| Radiation | Effect |
| --- | --- |
| below 20 Rad | harmless |
| 20 Rad | **Radiation Sickness I**: hunger, and damage every 3 seconds |
| 50 Rad | the land **withers**: grass dries to dirt, plants wilt into dead bushes, leaves fall |
| 100 Rad | **Radiation Sickness II**: also weakness, damage every 1.5 seconds |
| 300 Rad | **Radiation Sickness III**: also nausea, damage more than once a second |

Radiation Sickness hits players and mobs alike and goes through normal armor.

## Staying safe

- A [Hazmat Suit](hazmat_suit.md) keeps it out: each piece a quarter, the full suit all of it.
- A [Geiger Counter](geiger_counter.md) tells you how much there is.
- Move waste with hoppers instead of carrying it, and treat it in a
  [Waste Reprocessor](waste_reprocessor.md).
- Withering land can be turned off in the common config (*Radiation Withers Land*).
