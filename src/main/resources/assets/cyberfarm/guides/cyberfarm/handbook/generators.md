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
  <ItemIcon id="bio_digester" />
  <ItemIcon id="methane_extractor" />
  <ItemIcon id="gas_turbine" />
  <ItemIcon id="reactor_controller" />
  <ItemIcon id="steam_turbine" />
</ItemGrid>

## Power tiers

| Tier | Machine | Fuel | Output |
| --- | --- | --- | --- |
| 1 | [Combustion Generator](combustion_generator.md) | any furnace fuel | 40 FE/t |
| 2 | [Gas Turbine](gas_turbine.md) | Biogas | 30 FE/t |
| 2 | [Gas Turbine](gas_turbine.md) | Methane | 80 FE/t |
| 3 | [Fission Reactor](fission_reactor.md) + [Steam Turbine](steam_turbine.md) | Uranium Fuel Rods | up to 800 FE/t per turbine |

Start with a **Combustion Generator** and coal. When you want power that does not need you to
shovel fuel, move on to [Gas Power](gas_power.md): a [Bio-Digester](bio_digester.md) turns farm
waste into Biogas and a [Methane Extractor](methane_extractor.md) draws Methane from a captured
mob, both burned by the Gas Turbine.

The most powerful option is in the [Nuclear](nuclear.md) section: a [Fission Reactor](fission_reactor.md)
burning uranium [Fuel Rods](uranium.md) makes steam for [Steam Turbines](steam_turbine.md), cooled
with the help of the [Coolant Mixer](coolant_mixer.md). Its waste must be treated in a
[Waste Reprocessor](waste_reprocessor.md), or the area turns [radioactive](radiation.md).

Generators push their FE into adjacent machines by themselves; to reach machines further away,
use [Energy Conduits](energy_conduit.md).

## Pages

<SubPages icons={true} />
