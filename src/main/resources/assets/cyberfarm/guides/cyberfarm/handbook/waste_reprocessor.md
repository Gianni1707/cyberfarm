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

Makes nuclear waste safe. Each **Depleted Fuel Rod** takes 10 seconds and **40 FE/t** and becomes
a block of **Vitrified Waste**; one time in three it also recovers an **Uranium Ingot**, and one
time in four an **Isotope Shard**, the seed of the [Mech Core](mech_core.md)'s crystal.

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

<RecipeFor id="waste_reprocessor" />
