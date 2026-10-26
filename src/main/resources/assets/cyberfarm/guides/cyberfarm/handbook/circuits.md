---
navigation:
  title: Circuits and the Fabricator
  parent: materials.md
  position: 2
  icon: cyberfarm:fabricator
item_ids:
  - cyberfarm:basic_circuit
  - cyberfarm:advanced_circuit
  - cyberfarm:elite_circuit
  - cyberfarm:fabricator
---

# Circuits and the Fabricator

Every machine, module and frame runs on a circuit, and each tier has its own.

<ItemGrid>
  <ItemIcon id="basic_circuit" />
  <ItemIcon id="advanced_circuit" />
  <ItemIcon id="elite_circuit" />
</ItemGrid>

## By hand

Your first **Basic Circuits** come from a crafting table: copper, redstone and a gold nugget,
**one** at a time. It is slow and dear on purpose: build a Fabricator as soon as you can.

<RecipeFor id="basic_circuit" />

## The Fabricator

<BlockImage id="cyberfarm:fabricator" scale="4" float="left" />

The Fabricator makes circuits from the ingredients in its **three slots**, at **40 FE/t**. Pipe
**Acid** into it for the etched circuits. Any order, any slot: it finds the recipe.

<br clear="all" />

| Circuit | Ingredients | Acid | Time |
| --- | --- | --- | --- |
| 2 Basic Circuits | 1 Copper Ingot, 2 Redstone, 1 Gold Nugget | none | 3 s |
| Advanced Circuit | 2 Basic Circuits, 1 Gold Ingot | 250 mB | 5 s |
| Elite Circuit | 1 Advanced Circuit, 1 Diamond, 1 Uranium Ingot | 500 mB | 8 s |

The Acid comes from an [Acid Synthesizer](acid_synthesizer.md); the uranium from
[uranium ore](uranium.md). Feed the Fabricator with hoppers, pipes or a
[Transport drone](modules.md) and keep your circuits stocked with the dock filter's **limit**.

<RecipeFor id="fabricator" />
