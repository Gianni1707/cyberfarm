---
navigation:
  title: Bio-Digester
  parent: generators.md
  position: 4
  icon: cyberfarm:bio_digester
item_ids:
  - cyberfarm:bio_digester
---

# Bio-Digester

<BlockImage id="cyberfarm:bio_digester" scale="4" float="left" />

Digests **Biomass** into **Biogas**. It runs on **8 FE/t**: feed it from a [Biomass Press](biomass_press.md) and connect an [Energy Conduit](energy_conduit.md) to the machine block or to one of its frames. It takes Speed, Efficiency and Battery [addons](addons.md) on the tab beside its GUI.

It takes **2 Cyber Frames**, one above it and one on its left as you face it. Right-click it alone and drones show where they go. Once complete, the upper drum rises and a tank slides out beside it, its sight glass showing the gas. It doesn't work until then; breaking a frame folds it back.

<br clear="all" />

- Accepts only <ItemLink id="biomass" />.
- Each Biomass takes **2.5 seconds** and gives **500 mB** of Biogas: **10 mB/t** while it has Biomass.
- That is **2,500 FE** per Biomass in a [Gas Turbine](gas_turbine.md) (50 FE/t from one digester) and 4,500 FE in a
  [Steam Engine](steam_engine.md). Two digesters keep one Gas Turbine running at full power.
- Holds **8,000 mB** and pushes the gas into adjacent [Gas Pipes](gas_pipe.md), tanks or a
  [Gas Turbine](gas_turbine.md). Pipes connect to the machine block, on its free sides.

A Harvester drone, a hopper into a [Biomass Press](biomass_press.md) and the press's nozzle pointing at the digester make a fully automatic biogas farm.

<RecipeFor id="bio_digester" />
