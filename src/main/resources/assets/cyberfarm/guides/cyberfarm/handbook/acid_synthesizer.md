---
navigation:
  title: Acid Synthesizer
  parent: generators.md
  position: 6
  icon: cyberfarm:acid_synthesizer
item_ids:
  - cyberfarm:acid_synthesizer
  - cyberfarm:nether_sulfur_ore
  - cyberfarm:sulfur
  - cyberfarm:acid_bucket
---

# Acid Synthesizer

<BlockImage id="cyberfarm:acid_synthesizer" scale="4" float="left" />

The Acid Synthesizer makes **Acid** from **Sulfur** and **water**. The [Methane Extractor](methane_extractor.md) needs
Acid to dissolve the mob in its chamber, and the [Fabricator](circuits.md) etches the Advanced and Elite Circuits with it.
This page also covers where to find Sulfur and how to carry Acid in a bucket.

<br clear="all" />

## Building it

- It takes **5 [Cyber Frames](cyber_frame.md)**: one on each side of it and three on top.
- The finished machine is 3 blocks wide and 2 tall, with the Acid Synthesizer at the bottom of the middle.
- **Right-click** it while it is alone and drones show where the frames go.
- Once complete, the frames hide and the tanks and the tower rise out of its base.
- It doesn't work until then. Breaking a frame folds it back.

## Using it

- Pipe water in with a [Liquid Pipe](liquid_pipe.md): from a [Water Pump](water_pump.md), or from a
  [Liquid Tank](fluid_tanks.md) filled with water buckets.
- Put Sulfur in its slot, by hand or with a hopper.
- It pushes the Acid into adjacent pipes and tanks by itself. Pipe it to an **Acid Inlet** of the
  Methane Extractor's chamber.
- Pipes connect to the machine block: in front, behind or below.
- Right-click it with an empty bucket for an **Acid Bucket** once it holds 1,000 mB.
- It takes Speed, Efficiency and Battery [addons](addons.md) on the tab beside its GUI.
- The gauges on its two tanks show how full they are: water on the left, acid on the right.
- While it works, the collar turns round its tower, bubbles rise through the acid and white puffs leave the chimney.

## Numbers

| Stat | Value |
| --- | --- |
| One batch | 1 Sulfur + 500 mB Water → 500 mB Acid |
| Time per batch | 2 seconds |
| Power | 40 FE/t |
| Water tank | 8,000 mB |
| Acid tank | 8,000 mB |

A Methane Extractor uses 1 mB of Acid per tick, so one batch lasts it 25 seconds.

## Sulfur

<ItemImage id="cyberfarm:sulfur" scale="2" float="left" />

<ItemLink id="nether_sulfur_ore" /> is found in netherrack all over the Nether. Mine it with a pickaxe: it
drops **2-4 Sulfur**, more with Fortune. Silk Touch gives the ore block.

<br clear="all" />

Sulfur is tagged `c:dusts/sulfur` and the ore `c:ores/sulfur`. Sulfur from other mods works in
the Acid Synthesizer, and other mods' crushers and processors work on this ore.

## Acid Bucket

An <ItemLink id="acid_bucket" /> holds 1,000 mB of Acid. Fill it by right-clicking the Acid Synthesizer with an empty bucket.
Pour it into an Acid Inlet or any tank by right-clicking it. Acid cannot be poured into the world.

<Row>
  <RecipeFor id="acid_synthesizer" />
</Row>
