---
navigation:
  title: Bio-Digester
  parent: generators.md
  position: 3
  icon: cyberfarm:bio_digester
item_ids:
  - cyberfarm:bio_digester
---

# Bio-Digester

<BlockImage id="cyberfarm:bio_digester" scale="4" float="left" />

Digests **Biomass** into **Biogas**. It runs on **8 FE/t**: feed it from a [Biomass Press](biomass_press.md) and connect an [Energy Conduit](energy_conduit.md) to the machine block. It takes Speed, Efficiency and Battery [addons](addons.md) on the tab beside its GUI.

It takes **2 Cyber Frames**, one above it and one on its left as you face it. Right-click it alone and drones show where they go. Once complete, the upper drum rises and a tank slides out beside it, its sight glass showing the gas. It doesn't work until then; breaking a frame folds it back.

<br clear="all" />

- Accepts only <ItemLink id="biomass" />.
- Each Biomass takes 2 seconds and gives **250 mB** of Biogas.
- Holds **8,000 mB** and pushes the gas into adjacent [Gas Pipes](gas_pipe.md), tanks or a
  [Gas Turbine](gas_turbine.md). Pipes connect to the machine block, on its free sides.

A Harvester drone, a hopper into a [Biomass Press](biomass_press.md) and the press's nozzle pointing at the digester make a fully automatic biogas farm.

<RecipeFor id="bio_digester" />
