---
navigation:
  title: Steam Turbine
  parent: generators.md
  position: 10
  icon: cyberfarm:steam_turbine
item_ids:
  - cyberfarm:steam_turbine
---

# Steam Turbine

<BlockImage id="cyberfarm:steam_turbine" scale="4" float="left" />

Turns **Steam** into FE: **4 FE per mB**, up to **200 mB per tick**, so up to **800 FE/t**.

<br clear="all" />

- Holds **16,000 mB** of steam and **200,000 FE**, and pushes up to 1,600 FE/t into adjacent
  machines and [Energy Conduits](energy_conduit.md).
- One turbine handles the steam of 4 Fuel Rods at 100% power. Running the reactor harder makes more
  steam: add a turbine for every 200 mB/t, or the reactor's steam tank fills up and the core
  overheats.
- Steam from other mods works too (fluids tagged `c:steam`).

It is built from a [Gas Turbine](gas_turbine.md):

<RecipeFor id="steam_turbine" />
