---
navigation:
  title: Cable Swapper
  parent: cables.md
  position: 4
  icon: cyberfarm:cable_swapper
item_ids:
  - cyberfarm:cable_swapper
---

# Cable Swapper

<ItemImage id="cyberfarm:cable_swapper" scale="2" float="left" />

A hand tool that upgrades a whole line of [cables](cables.md) to the next tier in one click, so you don't
have to break and replace them one by one. It also sets the sides of a [Battery Bank](battery_bank.md).

<br clear="all" />

## Upgrading a line

1. Carry cables of the next tier: Advanced ones to upgrade Basic ones, Elite ones to upgrade Advanced ones.
2. **Right-click** any cable of the line with the Cable Swapper.

Every cable of the same kind and tier joined to the one you click becomes the next tier. Each swap uses one
new cable from your inventory and gives the old one back. If your inventory is full, the old cable drops on
the ground.

- **Sneak + right-click** to swap only the cable you click.
- Swapped cables keep their connections and which arms **pull**.
- If you run out of new cables, the ones nearest the cable you clicked are swapped first.
- In Creative mode it needs no new cables and gives none back.

## Other mods' cables

It works on other mods' cables and pipes too, within their own mod: a Mekanism Basic Universal Cable
becomes an Advanced Universal Cable.

- It counts as a cable any block whose id has the word cable, pipe, conduit, duct, tube, wire,
  transporter or conductor in it.
- It finds the next tier by the word in the id, in this order: basic, advanced, improved, reinforced,
  hardened, elite, ultimate.
- Tiers named another way (such as "mk2") are not recognised.

## Battery Bank sides

**Right-click** a face of a [Battery Bank](battery_bank.md) to switch that face of that block: **In**, **Out**,
**Off**, then In again. While you hold the swapper, every outer face of a bank is framed in its mode's colour.

## On the Utility Belt

The [Utility Belt](utility_belt.md) has a holder for the Cable Swapper. Press **I** to bring it to your hand,
and press it again to put it back.

## Numbers

| What | Amount |
| --- | --- |
| Cables swapped per click | up to **1,024** |
| Stack size | 1 |

## Troubleshooting

| Message | What it means |
| --- | --- |
| There is no higher tier of this cable | It is already the top tier (Elite for CyberFarm cables). |
| You need ... in your inventory | Carry cables of the next tier. |
| Swapped ... of ... cables: you ran out | Only part of the line was swapped. Bring more cables and click again. |
| The Cable Swapper works on cables and pipes | You clicked a block that is not a cable. |

<RecipeFor id="cable_swapper" />
