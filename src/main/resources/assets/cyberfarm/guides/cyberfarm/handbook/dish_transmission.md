---
navigation:
  title: Dish Transmission
  parent: cables.md
  position: 5
  icon: cyberfarm:transmitter_dish
item_ids:
  - cyberfarm:transmitter_dish
  - cyberfarm:receiver_dish
  - cyberfarm:range_module_i
  - cyberfarm:range_module_ii
  - cyberfarm:range_module_iii
  - cyberfarm:infinity_range_module
---

# Dish Transmission

<ItemGrid>
  <ItemIcon id="transmitter_dish" />
  <ItemIcon id="receiver_dish" />
  <ItemIcon id="range_module_i" />
  <ItemIcon id="range_module_ii" />
  <ItemIcon id="range_module_iii" />
  <ItemIcon id="infinity_range_module" />
</ItemGrid>

Dishes send **FE through the air**, where a conduit can't go. There is no beam: you can tell a
**Transmitter Dish** is working because it turns to a receiver and **holds still** while it sends, and a
**Receiver Dish** is getting power because the rings of its dish **light up** from the rim to the centre.

## Transmitter Dish

- It is a small multiblock: place it with **three Cyber Frames**, to its right, behind it and in the
  corner between, and the dish unfolds on its turret over a 2x2 base.
- Feed it FE with [Energy Conduits](energy_conduit.md) on the dish block.
- Each Transmitter Dish has **one network**. In its GUI you give it a **name** and a **colour**, which
  lights the indicator on both dishes.
- A new network is **Private**: only you and the players on your team can see it. Switch it to
  **Public** and anyone can link a receiver.
- It serves up to **4 receivers in turn**: it turns to one, sends, then moves to the next. The signal
  passes through blocks, but the receiver's chunk must be loaded.
- A tenth of the FE is **lost on the way**.

## Receiver Dish

- A small multiblock two blocks tall: put **a Cyber Frame on top** of it and its dish unfolds.
- Its GUI lists the networks it can reach, with their colour, distance (or dimension) and
  seats taken, for example "2/4". Click one to link; a full network is greyed out.
- It gives the FE it receives to the blocks and conduits around it.

## Range Modules

The Transmitter Dish has one module slot. The module sets how far it reaches and how much FE per tick
it sends in all, shared among its receivers.

| Module | Range | FE/t |
| --- | --- | --- |
| None | 32 blocks | 2,000 |
| Range Module I | 128 blocks | 8,000 |
| Range Module II | 512 blocks | 32,000 |
| Range Module III | 2,048 blocks | 128,000 |
| Infinity Range Module | Unlimited, into other dimensions | Unlimited |

With a receiver in another dimension, both dishes point **straight up at the sky**.

<RecipeFor id="transmitter_dish" />

<RecipeFor id="receiver_dish" />

<RecipeFor id="range_module_i" />

<RecipeFor id="range_module_ii" />

<RecipeFor id="range_module_iii" />

<RecipeFor id="infinity_range_module" />
