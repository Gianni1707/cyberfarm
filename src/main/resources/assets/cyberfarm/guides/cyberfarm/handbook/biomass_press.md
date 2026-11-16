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

The Biomass Press squeezes **organic waste** into <ItemLink id="biomass" />. Biomass is the solid mash a
[Bio-Digester](bio_digester.md) turns into Biogas, the first gas fuel. Biomass is also a weak fertilizer.

<br clear="all" />

## Building it

- Place the press, then a [Cyber Frame](cyber_frame.md) **on top of it**.
- With the frame there it assembles into a press two blocks tall. It only works once assembled.
- Connect power with [Energy Conduits](energy_conduit.md), to the machine block or to its frame.

## Using it

- It accepts **anything a composter takes** (seeds, crops, leaves, saplings, flowers...) and **rotten flesh**.
- Feed it by hand, or with a hopper: through the funnel on top, or from the sides of its lower block.
- The **nozzle on its right**, as you face its open front, pushes the Biomass into the container there: a hopper, a chest or a Bio-Digester.
- A hopper under the press, or a [Transport](modules.md) drone, can also take the Biomass. They never take the waste still to press.
- The mash in its chamber rises with what is inside, up to a full stack.
- It takes Speed, Efficiency and Battery [addons](addons.md) on the tab beside its GUI.

## Numbers

| Stat | Value |
| --- | --- |
| Power | 8 FE/t |
| Time per item | 2 seconds |
| Yield | twice the item's composter chance, on average |

**Richer waste gives more Biomass:**

| What goes in | Biomass per item, on average |
| --- | --- |
| Seeds, leaves, saplings, grass | 0.6 |
| Cactus, vines, melon slices, sugar cane | 1 |
| Wheat, carrots, potatoes, apples, flowers, pumpkins | 1.3 |
| Rotten flesh | 1.6 |
| Hay bales, baked potatoes, bread | 1.7 |
| Cake, pumpkin pie | 2 |

The recipe viewer (JEI or REI) lists every item with its yield. A press fed with wheat keeps about one and a half
Bio-Digesters busy.

## Biomass as fertilizer

Use Biomass on a plant by hand, or load it in a dock for a [Fertilizer](modules.md) drone.
It is always used up, but grows the plant only **one time in three**. Bone meal goes further.

## Tips

- Without power the press waits and keeps its progress.
- Point the nozzle straight at a Bio-Digester for a fully automatic biogas line.

<RecipeFor id="biomass_press" />
