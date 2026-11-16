---
navigation:
  title: Waste Reprocessor
  parent: nuclear.md
  position: 8
  icon: cyberfarm:waste_reprocessor
item_ids:
  - cyberfarm:waste_reprocessor
---

# Waste Reprocessor

<BlockImage id="cyberfarm:waste_reprocessor" scale="4" float="left" />

The Waste Reprocessor makes nuclear waste safe. It turns each Depleted Fuel Rod into a harmless
block of [Vitrified Waste](vitrified_waste.md), and sometimes recovers uranium and a rare
[Isotope Shard](isotope_shard.md).

<br clear="all" />

## Building it

It needs a **Cyber Frame on top**. Right-click the machine alone and a drone shows where. Once
complete, its plate grows up a block with a plaque on it. It does not work until then; breaking the
frame folds it back.

The plaque marks it as a machine that handles radioactive material. It stands a little above the
block: leave the space over it free to see it.

## Using it

<ItemGrid>
  <ItemIcon id="depleted_fuel_rod" />
  <ItemIcon id="vitrified_waste" />
  <ItemIcon id="uranium_ingot" />
  <ItemIcon id="isotope_shard" />
</ItemGrid>

- Put [Depleted Fuel Rods](radiation.md) in. Waste waiting inside the Reprocessor is **shielded**:
  it does not contaminate.
- **Automate it**: a hopper under a **Reactor Port** pulls Depleted Fuel Rods out of the reactor,
  and a hopper into the Reprocessor feeds them in. Hoppers below take the products out.
- [Energy Conduits](energy_conduit.md) plug in on its front, sides or back.
- It takes Speed, Efficiency and Battery [addons](addons.md) on the tab beside its GUI.

While it works its disc turns a quarter at each quarter of the batch: the rod waits on the left,
melts at the top, leaves as glass on the right, and the pocket comes round empty at the bottom.

## Numbers

| Per Depleted Fuel Rod | Amount |
| --- | --- |
| Time | 10 seconds |
| Energy | 160 FE/t |
| Always | 1 [Vitrified Waste](vitrified_waste.md) |
| 1 time in 3 | 1 Uranium Ingot |
| 1 time in 4 | 1 [Isotope Shard](isotope_shard.md) |

<RecipeFor id="waste_reprocessor" />
