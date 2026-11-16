---
navigation:
  title: Fabricator
  parent: materials.md
  position: 4
  icon: cyberfarm:fabricator
item_ids:
  - cyberfarm:fabricator
---

# Fabricator

<BlockImage id="cyberfarm:fabricator" scale="4" float="left" />

The Fabricator makes **circuits**. It makes Basic Circuits two at a time, far cheaper than by hand,
and it is the only way to make Advanced and Elite Circuits.

<br clear="all" />

## Building it

The Fabricator is a small multiblock. It needs one [Cyber Frame](cyber_frame.md) **behind** it.

1. Place the Fabricator.
2. Right-click it: a drone shows where the frame goes.
3. Place the Cyber Frame there. The line grows a block back, to a card magazine.

It works only once assembled. Breaking the frame folds it back.

## Using it

- Put the ingredients in its **three slots**, in any order and any slot: it finds the recipe.
- Pipe **Acid** in for the Advanced and Elite Circuits. [Liquid Pipes](liquid_pipe.md) plug into its
  front and sides, or into the top, where they clamp onto its acid drum.
- Give it FE.
- A hopper underneath takes out only the finished circuits, never the ingredients.
- The tab beside its GUI takes Speed, Efficiency and Battery [addons](addons.md).

Feed it with hoppers, pipes or a [Transport drone](modules.md), and keep your circuits stocked with
the dock filter's **limit**.

You can see it work: the card slides out from under the drum in short steps, like paper from a
printer. The print head sweeps the drum's lip before each step, the rollers turn and the chip comes
out between them. The light bar on the tray shows how far the circuit is. When it is done the card
flashes away and the next one starts.

## Numbers

It uses **50 FE/t** for every recipe.

| Makes | Ingredients | Acid | Time |
| --- | --- | --- | --- |
| 2 Basic Circuits | 1 Copper Ingot, 2 Redstone, 1 Gold Nugget | none | 3 s |
| 1 Advanced Circuit | 2 Basic Circuits, 1 Gold Ingot | 250 mB | 5 s |
| 1 Elite Circuit | 1 Advanced Circuit, 1 Diamond, 1 Uranium Ingot | 500 mB | 8 s |

| What | Value |
| --- | --- |
| Acid tank | 8,000 mB |
| Energy buffer | 20,000 FE |

The Acid comes from an [Acid Synthesizer](acid_synthesizer.md), the uranium from
[uranium ore](uranium.md). See [Circuits](circuits.md) for what each circuit is for.

<RecipeFor id="fabricator" />
