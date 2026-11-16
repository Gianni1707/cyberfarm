---
navigation:
  title: Generators
  position: 1
  icon: cyberfarm:combustion_generator
---

# Generators

Everything in CyberFarm runs on **FE** (Forge Energy): the Drone Assembler builds with it and the
Drone Dock charges your drones with it. The machines in this section make that power. FE is a standard,
so generators from other mods work just as well.

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

## Which generator?

| Tier | Generator | Fuel | Output | What it takes |
| --- | --- | --- | --- | --- |
| 1 | [Combustion Generator](combustion_generator.md) | any furnace fuel | 60 FE/t | a furnace, iron, copper, redstone |
| 2 | [Gas Turbine](gas_turbine.md) | Biogas, 20 mB/t | 100 FE/t | a furnace, a Basic Circuit, 2 [Cyber Frames](cyber_frame.md) |
| 2 | Gas Turbine | Methane, 20 mB/t | 240 FE/t | same |
| 2.5 | [Steam Engine](steam_engine.md) | Biogas, 45 mB/t | 405 FE/t | 2 blast furnaces, an Advanced Circuit, 5 Cyber Frames |
| 2.5 | Steam Engine | Methane, 45 mB/t | 675 FE/t | same |
| 3 | [Fission Reactor](fission_reactor.md) + [Steam Turbine](steam_turbine.md) | Uranium Fuel Rods | 2,000 to 40,000 FE/t per turbine | Elite Circuits, uranium, a big multiblock |

The Steam Engine's 5 Cyber Frames are one in its recipe and 4 in the finished row.

## Where the gas comes from

Gas generators need a steady flow of gas. Two machines make it:

| Machine | Makes | Needs |
| --- | --- | --- |
| [Bio-Digester](bio_digester.md) | 10 mB/t of Biogas | Biomass from a [Biomass Press](biomass_press.md), which presses farm waste |
| [Methane Extractor](methane_extractor.md) | 50 mB/t of Methane from a zombie, 25 from other monsters, 8 from animals | a 3x3x4 chamber, a mob, and Acid from an [Acid Synthesizer](acid_synthesizer.md) |

Some of these machines draw power themselves, so the first FE has to come from somewhere else:

| Machine | Uses |
| --- | --- |
| [Biomass Press](biomass_press.md) | 8 FE/t |
| [Bio-Digester](bio_digester.md) | 8 FE/t |
| [Acid Synthesizer](acid_synthesizer.md) | 40 FE/t |
| [Water Pump](water_pump.md) | 80 FE/t |

## How to progress

1. Start with a **Combustion Generator** and coal.
2. When you are tired of shovelling fuel, move on to [Gas Power](gas_power.md). A Bio-Digester turns farm waste into Biogas. A Methane Extractor draws Methane from a mob dissolving in Acid. A Gas Turbine burns both.
3. For more power from the same gas, build a **Steam Engine**.
4. The most power comes from the [Nuclear](nuclear.md) section. A Fission Reactor burns uranium [Fuel Rods](fuel_rod.md) into steam for Steam Turbines. The [Coolant Mixer](coolant_mixer.md) helps cool it. Its waste must go to a [Waste Reprocessor](waste_reprocessor.md), or the area turns [radioactive](radiation.md).

A zombie chamber is the big step in gas power. Its methane makes about **600 FE/t** in Gas Turbines: as much as twelve
Bio-Digesters. One Gas Turbine burns 20 mB/t, so it takes two and a half turbines. The same chamber keeps a Steam Engine
running at 675 FE/t. That is still less than the smallest reactor at full power.

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

The Combustion Generator burns any furnace fuel for half as long as a furnace would.

Gases burn fast, so size a gas plant by its **flow**: a Gas Turbine needs 20 mB/t (two Bio-Digesters), a Steam Engine 45 mB/t.

## Moving the power

The Combustion Generator, the Gas Turbine and the Steam Engine push their FE into adjacent machines by themselves. To reach machines further away,
use [Energy Conduits](energy_conduit.md).

## Pages

<SubPages icons={true} />
