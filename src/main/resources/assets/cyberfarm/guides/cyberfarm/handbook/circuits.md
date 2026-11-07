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

It takes a **Cyber Frame** behind it: right-click it alone and a drone shows where. Once complete, the line grows a block back, to a card magazine; it doesn't work until then, and breaking the frame folds it back.

The Fabricator makes circuits from the ingredients in its **three slots**, at **40 FE/t**. Pipe
**Acid** into it for the etched circuits: Liquid Pipes plug into any side, or into the top where they clamp onto
its acid drum. Any order, any slot: it finds the recipe. While it works, the card slides out from under the
drum in short steps, like paper from a printer: the print head sweeps the drum's lip before each step, the rollers
turn and the chip comes out between them; the light bar on the tray shows how far the circuit is. When it is done
the card flashes away and the next one starts.

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
