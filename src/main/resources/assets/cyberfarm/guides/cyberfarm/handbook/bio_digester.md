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

Turns **organic waste** into **Biogas**. It needs no power: just feed it.

It takes **2 Cyber Frames**, one above it and one on its left as you face it. Right-click it alone and drones show where they go. Once complete, the upper drum rises and a tank slides out beside it, its sight glass showing the gas. It doesn't work until then; breaking a frame folds it back.

<br clear="all" />

- Accepts **anything a composter takes** (seeds, crops, leaves, saplings, flowers...) and
  **rotten flesh**.
- Each item takes 2 seconds. A sure-fire compost item (like a cake) gives **250 mB**, the others
  less, in proportion to their composting chance; rotten flesh gives **200 mB**.
- Holds **8,000 mB** and pushes the gas into adjacent [Gas Pipes](gas_pipe.md), tanks or a
  [Gas Turbine](gas_turbine.md). Pipes connect to the machine block, on its free sides.

A Harvester drone and a hopper feeding the digester make a fully automatic biogas farm.

<RecipeFor id="bio_digester" />
