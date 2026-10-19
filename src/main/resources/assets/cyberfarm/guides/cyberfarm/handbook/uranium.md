---
navigation:
  title: Uranium and Fuel Rods
  parent: generators.md
  position: 7
  icon: cyberfarm:uranium_ingot
item_ids:
  - cyberfarm:uranium_ore
  - cyberfarm:deepslate_uranium_ore
  - cyberfarm:raw_uranium
  - cyberfarm:uranium_ingot
  - cyberfarm:fuel_rod
  - cyberfarm:depleted_fuel_rod
---

# Uranium and Fuel Rods

<BlockImage id="cyberfarm:deepslate_uranium_ore" scale="3" float="left" />

**Uranium Ore** is rare and found deep in the Overworld, from Y 16 down to the bottom of the world,
more often the deeper you go. Its green specks glow faintly in the dark. You need at least an
**iron pickaxe**; Fortune gives more Raw Uranium, Silk Touch the ore block itself.

<br clear="all" />

## From ore to fuel

<ItemGrid>
  <ItemIcon id="raw_uranium" />
  <ItemIcon id="uranium_ingot" />
  <ItemIcon id="fuel_rod" />
  <ItemIcon id="depleted_fuel_rod" />
</ItemGrid>

- Smelt **Raw Uranium** (or the ore) in a furnace or blast furnace into an **Uranium Ingot**.
- A **Fuel Rod** is 3 Uranium Ingots in an iron casing.

<RecipeFor id="fuel_rod" />

## Burning fuel

A Fuel Rod lasts **40 minutes** in a [Fission Reactor](fission_reactor.md) at 100% power; its
durability bar shows the fuel left. At higher power it burns proportionally faster. A spent rod
becomes a **Depleted Fuel Rod** in the reactor's waste slots.

Depleted Fuel Rods are **nuclear waste**: take them out of the reactor (by hand or with a hopper
under a Reactor Port) and keep them stored, away from where you live.

Uranium uses the common `c:` tags, so ore processors and machines from other mods handle it too.
