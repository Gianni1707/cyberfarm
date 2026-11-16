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
---

# Aluminum

Aluminum is the light metal of the first drone frames. You cannot smelt it straight from its ore:
just as in real life, it takes electrolysis and a steady flow of power.

<ItemGrid>
  <ItemIcon id="bauxite_ore" />
  <ItemIcon id="raw_bauxite" />
  <ItemIcon id="alumina" />
  <ItemIcon id="aluminum_ingot" />
</ItemGrid>

## From ore to ingot

1. **Mine Bauxite Ore.** It shows as rusty clumps in stone and deepslate. It drops **Raw Bauxite**,
   and Fortune helps.
2. **Smelt or blast** the Raw Bauxite (or the ore itself) into **Alumina**, a white powder.
3. **Put the Alumina in an <ItemLink id="electrolyzer" />.** It makes 1 Aluminum Ingot every
   5 seconds at 80 FE/t. Two [Combustion Generators](combustion_generator.md) keep it running.

## Where to find bauxite

| What | Value |
| --- | --- |
| Found in | Stone and deepslate |
| Heights | y -32 to y 80 |
| Most common | Around y 24 |
| How common | Almost as common as iron |

## Other mods

Aluminum from other mods works in every CyberFarm recipe, and their crushers and furnaces can
process our ore. The tags are:

| Item | Tags |
| --- | --- |
| Aluminum Ingot | `c:ingots/aluminum` |
| Alumina | `c:dusts/alumina` |
| Bauxite Ore | `c:ores/bauxite`, `c:ores/aluminum` |
| Raw Bauxite | `c:raw_materials/aluminum`, `c:raw_materials/bauxite` |

Already have an aluminum ore from another mod? Set `bauxiteGeneration = false` under `[world]` in
`cyberfarm-common.toml`, and only theirs generates (in new chunks only).
