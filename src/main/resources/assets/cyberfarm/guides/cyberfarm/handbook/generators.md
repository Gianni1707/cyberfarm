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
  <ItemIcon id="acid_synthesizer" />
  <ItemIcon id="steam_engine" />
  <ItemIcon id="reactor_controller" />
  <ItemIcon id="turbine_controller" />
</ItemGrid>

## Power tiers

| Tier | Machine | Fuel | Output |
| --- | --- | --- | --- |
| 1 | [Combustion Generator](combustion_generator.md) | any furnace fuel | 40 FE/t |
| 2 | [Gas Turbine](gas_turbine.md) | Biogas | 60 FE/t |
| 2 | [Gas Turbine](gas_turbine.md) | Methane | 160 FE/t |
| 2.5 | [Steam Engine](steam_engine.md) (1-4 cylinders) | Biogas | 90-360 FE/t |
| 2.5 | [Steam Engine](steam_engine.md) (1-4 cylinders) | Methane | 150-600 FE/t |
| 3 | [Fission Reactor](fission_reactor.md) + [Steam Turbine](steam_turbine.md) | Uranium Fuel Rods | up to 9,600 FE/t per turbine |

## Where the gas comes from

| Machine | Makes | Effort |
| --- | --- | --- |
| [Bio-Digester](bio_digester.md) | about 6 mB/t of Biogas from compostables and rotten flesh | a farm to feed it |
| [Methane Extractor](methane_extractor.md) | 25 mB/t of Methane from a zombie (12 other monsters, 4 animals) | a 3x3x4 chamber, acid, a steady supply of mobs |

A zombie chamber is the big step: about **200 FE/t** in a Gas Turbine, as much as ten
Bio-Digesters. A full four-cylinder Steam Engine burns the methane of two or three chambers into
600 FE/t, more than a whole row of Combustion Generators, but still less than the smallest
reactor at full power.

Start with a **Combustion Generator** and coal. When you want power that does not need you to
shovel fuel, move on to [Gas Power](gas_power.md): a [Bio-Digester](bio_digester.md) turns farm
waste into Biogas and a [Methane Extractor](methane_extractor.md) draws Methane from a captured
mob dissolving in [Acid](acid_synthesizer.md), both burned by the Gas Turbine. For more power from
the same gases, build a [Steam Engine](steam_engine.md) with up to four cylinders.

The most powerful option is in the [Nuclear](nuclear.md) section: a [Fission Reactor](fission_reactor.md)
burning uranium [Fuel Rods](uranium.md) makes steam for [Steam Turbines](steam_turbine.md), cooled
with the help of the [Coolant Mixer](coolant_mixer.md). Its waste must be treated in a
[Waste Reprocessor](waste_reprocessor.md), or the area turns [radioactive](radiation.md).

Generators push their FE into adjacent machines by themselves; to reach machines further away,
use [Energy Conduits](energy_conduit.md).

## Pages

<SubPages icons={true} />
