---
navigation:
  title: Waste Reprocessor
  parent: nuclear.md
  position: 6
  icon: cyberfarm:waste_reprocessor
item_ids:
  - cyberfarm:waste_reprocessor
  - cyberfarm:vitrified_waste
  - cyberfarm:isotope_shard
---

# Waste Reprocessor

<BlockImage id="cyberfarm:waste_reprocessor" scale="4" float="left" />

It takes a **Cyber Frame** on top: right-click it alone and a drone shows where. Once complete, its plate grows up a block with the plaque on it. It doesn't work until then; breaking the frame folds it back.

Makes nuclear waste safe. Each **Depleted Fuel Rod** takes 10 seconds and **160 FE/t** and becomes
a block of **Vitrified Waste**; one time in three it also recovers an **Uranium Ingot**, and one
time in four an **Isotope Shard**, the seed of the [Mech Core](mech_core.md)'s crystal. It takes Speed, Efficiency and Battery [addons](addons.md) on the tab beside its GUI.

<br clear="all" />

<ItemGrid>
  <ItemIcon id="depleted_fuel_rod" />
  <ItemIcon id="vitrified_waste" />
  <ItemIcon id="uranium_ingot" />
  <ItemIcon id="isotope_shard" />
</ItemGrid>

- Waste waiting inside the Reprocessor is **shielded**: it does not contaminate.
- Automate it: a hopper under a **Reactor Port** pulls Depleted Fuel Rods out of the reactor, and a
  hopper into the Reprocessor feeds them in. Hoppers below take the products out.
- **Vitrified Waste** is inert glass: store it anywhere, or build with it.

While it works its disc turns a quarter at each quarter of the batch: the rod waits on the left, melts at the
top, leaves as glass on the right and the pocket comes round empty at the bottom. The plaque on its roof marks it as a
machine that handles radioactive material, and stands a little above the block: leave the space over it free to see it.
[Energy Conduits](energy_conduit.md) plug in on its front, sides or back.

<RecipeFor id="waste_reprocessor" />
