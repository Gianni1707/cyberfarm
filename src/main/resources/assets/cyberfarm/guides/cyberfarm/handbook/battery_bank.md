---
navigation:
  title: Battery Bank
  parent: energy_storage.md
  position: 4
  icon: cyberfarm:battery_bank
item_ids:
  - cyberfarm:battery_bank
---

# Battery Bank

<BlockImage id="cyberfarm:battery_bank" scale="3" float="left" />

A small FE store for an early base: **150,000 FE** a block. Use it to keep power for later, or to
feed a [Human Dock](human_dock.md) directly. For far more storage, build a
[Resonance Core](resonance_core.md).

<br clear="all" />

## Building a bank

- Blocks that fill a box join into **one bank**: a row, a column, a 2x2x2, up to **3x3x3**.
- A bank fills and empties as one: its blocks always share the charge evenly.
- Joined blocks share **one display** across the whole front, and the side gauges run its full height.

## Sides: In, Out and Off

Each side of the bank has a mode. Set it with the [Cable Swapper](cable_swapper.md): **right-click** a
side to switch it. While you hold the swapper, every side is framed in its mode's colour.

| Mode | Frame | What the side does |
| --- | --- | --- |
| **In** (default) | green | Takes FE from cables and machines. Gives none. |
| **Out** | amber | Pushes FE into the block or cable that touches it. Takes none. |
| **Off** | grey | Nothing connects there. |

A mode belongs to a whole side of the bank: setting one block's north face sets the north side of
every block in the bank.

Set the side facing your [Human Dock](human_dock.md) to **Out**.

## Display and GUI

- The **display** on the front shows the bank's charge and the FE/t coming in and going out. A gauge
  on each side fills as it charges.
- A bigger bank's display spells the numbers out and adds what the bank stores.
- **Right-click** the bank to open its GUI. It shows the whole bank's charge and the FE/t flowing in
  and out, and has a **slot that charges an item** from the bank.

## Numbers

| What | Amount |
| --- | --- |
| Stored per block | 150,000 FE |
| Full 3x3x3 bank (27 blocks) | 4,050,000 FE |
| In or out, through each block's face | 4,000 FE/t |
| Charging the item in the slot | 4,000 FE/t |

A bigger bank has more outer faces, so cables on several blocks move more power at once.

## Tips

- An **Out** side pushes by itself, so a cable there needs no pulling arm.
- A cable can't pull through an **In** side. Set the side to **Out** instead.

<RecipeFor id="battery_bank" />
