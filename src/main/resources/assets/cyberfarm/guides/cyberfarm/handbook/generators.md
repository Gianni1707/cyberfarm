---
navigation:
  title: Generators
  position: 1
  icon: cyberfarm:combustion_generator
---

# Generators

Everything in CyberFarm runs on **FE** (Forge Energy): the Drone Assembler builds with it and the
Drone Dock charges your drones with it. These machines make that power, and since FE is the
standard, generators from other mods work just as well.

<ItemGrid>
  <ItemIcon id="combustion_generator" />
  <ItemIcon id="biomass_press" />
  <ItemIcon id="bio_digester" />
  <ItemIcon id="methane_extractor" />
  <ItemIcon id="gas_turbine" />
  <ItemIcon id="acid_synthesizer" />
  <ItemIcon id="steam_engine" />
  <ItemIcon id="reactor_controller" />
  <ItemIcon id="turbine_controller" />
</ItemGrid>

## Power tiers

| Tier | Machine | Fuel | Output |
| --- | --- | --- | --- |
| 1 | [Combustion Generator](combustion_generator.md) | any furnace fuel | 60 FE/t |
| 2 | [Gas Turbine](gas_turbine.md) | Biogas | 100 FE/t |
| 2 | [Gas Turbine](gas_turbine.md) | Methane | 240 FE/t |
| 2.5 | [Steam Engine](steam_engine.md) | Biogas | 405 FE/t |
| 2.5 | [Steam Engine](steam_engine.md) | Methane | 675 FE/t |
| 3 | [Fission Reactor](fission_reactor.md) + [Steam Turbine](steam_turbine.md) | Uranium Fuel Rods | up to 40,000 FE/t per turbine |

## Where the gas comes from

| Machine | Makes | Effort |
| --- | --- | --- |
| [Bio-Digester](bio_digester.md) | 10 mB/t of Biogas from compostables and rotten flesh | a farm to feed it |
| [Methane Extractor](methane_extractor.md) | 50 mB/t of Methane from a zombie (25 other monsters, 8 animals) | a 3x3x4 chamber, acid, a steady supply of mobs |

A zombie chamber is the big step: about **600 FE/t** in Gas Turbines (one burns 20 mB/t, so it takes two and a half), as much as twelve
Bio-Digesters. One chamber keeps a Steam Engine running, which turns its methane into
675 FE/t, more than a whole row of Combustion Generators, but still less than the smallest
reactor at full power.

Start with a **Combustion Generator** and coal. When you want power that does not need you to
shovel fuel, move on to [Gas Power](gas_power.md): a [Bio-Digester](bio_digester.md) turns farm
waste into Biogas and a [Methane Extractor](methane_extractor.md) draws Methane from a captured
mob dissolving in [Acid](acid_synthesizer.md), both burned by the Gas Turbine. For more power from
the same gases, build a [Steam Engine](steam_engine.md).

The most powerful option is in the [Nuclear](nuclear.md) section: a [Fission Reactor](fission_reactor.md)
burning uranium [Fuel Rods](uranium.md) makes steam for [Steam Turbines](steam_turbine.md), cooled
with the help of the [Coolant Mixer](coolant_mixer.md). Its waste must be treated in a
[Waste Reprocessor](waste_reprocessor.md), or the area turns [radioactive](radiation.md).

## How long fuels last

| Fuel | Burned in | Lasts | Gives |
| --- | --- | --- | --- |
| Coal or charcoal | [Combustion Generator](combustion_generator.md), 60 FE/t | 40 seconds | 48,000 FE |
| Block of coal | Combustion Generator | 400 seconds | 480,000 FE |
| A log or planks | Combustion Generator | 7.5 seconds | 9,000 FE |
| Blaze rod | Combustion Generator | 60 seconds | 72,000 FE |
| Lava bucket | Combustion Generator | 500 seconds | 600,000 FE |
| Biomass | [Bio-Digester](bio_digester.md), 2.5 seconds each | becomes 500 mB of Biogas | 2,500 FE in a Gas Turbine, 4,500 in a Steam Engine |
| 1,000 mB of Biogas | [Gas Turbine](gas_turbine.md), 100 FE/t | 2.5 seconds | 5,000 FE |
| 1,000 mB of Methane | Gas Turbine, 240 FE/t | 2.5 seconds | 12,000 FE |
| 1,000 mB of Biogas | [Steam Engine](steam_engine.md), 405 FE/t | 1.1 seconds | 9,000 FE |
| 1,000 mB of Methane | Steam Engine, 675 FE/t | 1.1 seconds | 15,000 FE |

The Combustion Generator burns any furnace fuel for half as long as a furnace would. Gases burn fast, so
a gas plant is sized by its **flow**: a Gas Turbine needs 20 mB/t (two Bio-Digesters), a Steam Engine 45 mB/t.

Generators push their FE into adjacent machines by themselves; to reach machines further away,
use [Energy Conduits](energy_conduit.md).

## Pages

<SubPages icons={true} />
