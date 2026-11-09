---
navigation:
  title: Biomass Press
  parent: generators.md
  position: 3
  icon: cyberfarm:biomass_press
item_ids:
  - cyberfarm:biomass_press
  - cyberfarm:biomass
---

# Biomass Press

<BlockImage id="cyberfarm:biomass_press" scale="4" float="left" />

Presses **organic waste** into **Biomass**, the solid mash a [Bio-Digester](bio_digester.md) turns into Biogas. It runs on **8 FE/t** and needs a <ItemLink id="cyber_frame" /> above it: with one there it assembles into a press two blocks tall, and only then works. It takes Speed, Efficiency and Battery [addons](addons.md) on the tab beside its GUI.

<br clear="all" />

- Accepts **anything a composter takes** (seeds, crops, leaves, saplings, flowers...) and **rotten flesh**. A hopper feeds it through the funnel on top, or from the sides of its lower block.
- The mash in its chamber rises with what is inside, up to a full stack.
- [Energy Conduits](energy_conduit.md) connect to the machine block. Without power it waits and keeps its progress.
- Each item takes 2 seconds and gives a <ItemLink id="biomass" /> with the chance a composter would take it: a cake always gives one,
  a seed only sometimes, and rotten flesh 4 times out of 5.
- The **nozzle on its right**, as you face its open front, pushes the Biomass into the container there: a hopper, a chest or a Bio-Digester.
  A hopper under the press or a [Transport](modules.md) drone can take it too; they take only the Biomass, never the waste still to press.

Biomass is also a **weak fertilizer**: use it on a plant by hand or load it in a dock for a [Fertilizer](modules.md) drone. It is always used up, but grows the plant only one time in three, so bone meal goes further and Biogas stays a real alternative.

<RecipeFor id="biomass_press" />
