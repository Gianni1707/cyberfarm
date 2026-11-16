---
navigation:
  title: Electrolyzer
  parent: materials.md
  position: 2
  icon: cyberfarm:electrolyzer
item_ids:
  - cyberfarm:electrolyzer
---

# Electrolyzer

<BlockImage id="cyberfarm:electrolyzer" scale="4" float="left" />

The Electrolyzer turns **Alumina** into **Aluminum Ingots** with electricity. You need it early:
the first drone frames are made of aluminum, and aluminum cannot be smelted straight from its ore.

<br clear="all" />

## Building it

The Electrolyzer is a small multiblock. It needs one [Cyber Frame](cyber_frame.md) on its
**left**, as you face its front.

1. Place the Electrolyzer.
2. Right-click it: a drone shows where the frame goes.
3. Place the Cyber Frame there. The machine assembles itself.

It works only once assembled. Breaking the frame folds it back.

## Using it

- Put **Alumina** in the input slot. Ingots come out in the output slot.
- Give it FE. Plug [Energy Conduits](energy_conduit.md) into its free side, its front or its back:
  its top is full.
- A hopper underneath takes out only the ingots, never the Alumina still waiting.
- The tab beside its GUI takes Speed, Efficiency and Battery [addons](addons.md).

You can see it work: the feeder punch behind its window strikes four times per ingot, and alumina
grains drop into the bath. If the punch stops, so has the machine.

## Numbers

| What | Value |
| --- | --- |
| Recipe | 1 Alumina -> 1 Aluminum Ingot |
| Time | 5 seconds per ingot |
| Power | 80 FE/t (8,000 FE per ingot) |
| Energy buffer | 20,000 FE |

A [Combustion Generator](combustion_generator.md) makes 60 FE/t, so **two** of them keep the
Electrolyzer running. Alumina from other mods works too.

See [Aluminum](aluminum.md) for the whole chain, from bauxite ore to ingot.

<RecipeFor id="electrolyzer" />
