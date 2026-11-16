---
navigation:
  title: Mech Core
  parent: mech.md
  position: 0
  icon: cyberfarm:mech_core
item_ids:
  - cyberfarm:mech_core
  - cyberfarm:fusion_crystal
---

# Mech Core

<ItemImage id="cyberfarm:mech_core" scale="3" float="left" />

Every [mech](mech.md) is built around a **Mech Core**: a **Fusion Crystal** set in two Elite
Frames. The crystal is end-game work, grown at the end of a chain of machines.

<br clear="all" />

## Growing the Fusion Crystal

1. A running [Fission Reactor](fission_reactor.md) burns Fuel Rods into Depleted Fuel Rods.
2. The [Waste Reprocessor](waste_reprocessor.md) treats them. **One rod in four** gives up an
   <ItemLink id="isotope_shard" />.
3. The [Crystallizer](crystallizer.md) grows the shard into a <ItemLink id="fusion_crystal" />, in
   [Acid](acid_synthesizer.md) with **4 Amethyst Shards**, in 30 seconds at 4,000 FE/t.

<ItemGrid>
  <ItemIcon id="isotope_shard" />
  <ItemIcon id="minecraft:amethyst_shard" />
  <ItemIcon id="fusion_crystal" />
</ItemGrid>

## Making the core

Craft the Fusion Crystal with 2 Elite Frames, 2 Battery Cells and 4 Diamonds. The core then goes
into the <ItemLink id="mech_frame" />.

<RecipeFor id="mech_core" />
