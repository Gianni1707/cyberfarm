---
navigation:
  title: Sulfur and Acid
  parent: generators.md
  position: 5
  icon: cyberfarm:acid_synthesizer
item_ids:
  - cyberfarm:acid_synthesizer
  - cyberfarm:nether_sulfur_ore
  - cyberfarm:sulfur
  - cyberfarm:acid_bucket
---

# Sulfur and Acid

The [Methane Extractor](methane_extractor.md) needs **Acid** to dissolve the mob in its chamber.
Acid is made from **Sulfur** and water.

## Sulfur

<BlockImage id="cyberfarm:nether_sulfur_ore" scale="3" float="left" />

**Nether Sulfur Ore** is found in netherrack all over the Nether. Mine it with any pickaxe: it
drops **2-4 Sulfur**, more with Fortune (Silk Touch gives the ore block).

Sulfur is tagged `c:dusts/sulfur` and the ore `c:ores/sulfur`, so sulfur from other mods works in
the Acid Synthesizer, and other mods' crushers and processors work on our ore.

<br clear="all" />

## Acid Synthesizer

<BlockImage id="cyberfarm:acid_synthesizer" scale="4" float="left" />

A machine that needs room: it takes **5 Cyber Frames**, one on each side and three on top, 3 blocks wide and 2 tall with the
machine at the bottom of the middle. Right-click it alone and drones show where they go. Once complete, the frames hide and the
tanks and the tower rise out of its base. It doesn't work until then; breaking a frame folds it back.

Uses **40 FE/t**. Each batch takes 2 seconds: **1 Sulfur + 500 mB Water → 500 mB Acid**. It takes Speed, Efficiency and Battery [addons](addons.md) on the tab beside its GUI. The gauges on
its two tanks show how full they are: water on the left, acid on the right. While it works the collar turns round
its tower, bubbles rise through the acid and white puffs leave the chimney. Pipes connect to the machine block, in front and behind.

- Pipe water in (from a [Water Pump](water_pump.md)) and put sulfur in the slot, by hand or with a hopper.
- The acid is pushed into adjacent pipes and tanks by itself: pipe it to an **Acid Inlet** of the
  [Methane Extractor](methane_extractor.md) chamber.
- Right-click it with an empty bucket for an **Acid Bucket**.

<br clear="all" />

<Row>
  <RecipeFor id="acid_synthesizer" />
</Row>
