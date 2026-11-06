---
navigation:
  title: Aluminum
  parent: materials.md
  position: 1
  icon: cyberfarm:aluminum_ingot
item_ids:
  - cyberfarm:bauxite_ore
  - cyberfarm:deepslate_bauxite_ore
  - cyberfarm:raw_bauxite
  - cyberfarm:alumina
  - cyberfarm:aluminum_ingot
  - cyberfarm:electrolyzer
---

# Aluminum

Aluminum is the light metal of the first drone frames. You cannot smelt it straight out of its
ore: just as in real life, it takes electrolysis and a steady flow of power.

<ItemGrid>
  <ItemIcon id="bauxite_ore" />
  <ItemIcon id="raw_bauxite" />
  <ItemIcon id="alumina" />
  <ItemIcon id="aluminum_ingot" />
</ItemGrid>

1. Mine **Bauxite Ore**: rusty clumps in stone and deepslate, almost as common as iron, from y -32
   to y 80 and most common around y 24. It drops **Raw Bauxite** (Fortune helps).
2. Smelt or blast it into **Alumina**, a white powder.
3. Put the Alumina in an **Electrolyzer**: **1 Aluminum Ingot every 5 seconds at 40 FE/t**. One
   [Combustion Generator](combustion_generator.md) keeps it running. While it works, the feeder punch behind its
   window strikes four times per ingot and alumina grains drop into the bath; if the punch stops, so has the machine.
   Plug conduits into its sides, front or back: its top is full.

Aluminum Ingots are tagged `c:ingots/aluminum`, Alumina `c:dusts/alumina`, the ore
`c:ores/bauxite` and `c:ores/aluminum`, Raw Bauxite `c:raw_materials/aluminum`: aluminum from other
mods works in every recipe, and their crushers and furnaces can process our ore.

Already have an aluminum ore from another mod? Set `bauxiteGeneration = false` under `[world]` in
`cyberfarm-common.toml` and only theirs generates (new chunks only).

<Row>
  <RecipeFor id="electrolyzer" />
</Row>
