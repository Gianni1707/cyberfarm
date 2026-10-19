---
navigation:
  title: Liquid and Gas Tanks
  parent: cables.md
  position: 4
  icon: cyberfarm:liquid_tank
item_ids:
  - cyberfarm:liquid_tank
  - cyberfarm:gas_tank
---

# Liquid and Gas Tanks

<Row>
  <BlockImage id="cyberfarm:liquid_tank" scale="3" />
  <BlockImage id="cyberfarm:gas_tank" scale="3" />
</Row>

Tanks store **32,000 mB** (32 buckets) of a single fluid:

- the **Liquid Tank** (blue window) takes liquids: water, lava, Coolant, Cryo Coolant, liquids
  of other mods;
- the **Gas Tank** (amber window) takes gases: biogas, methane, Steam, gases of other mods.

Fill them with the matching [pipe](cables.md): a [Liquid Pipe](liquid_pipe.md) or a
[Gas Pipe](gas_pipe.md). A Liquid Tank also takes and gives **buckets**.

Tanks are **passive**: they never push anything out. To empty one into a machine, right-click
the arm of the pipe that touches it, so the pipe **pulls** from the tank (an orange collar shows
it). Right-click a tank with an empty hand to see what is inside.

A tank is a handy buffer: put one between a Methane Extractor and the Gas Turbines, or between a
Water Pump and a Fission Reactor, and it keeps the flow going when production dips.

Breaking a tank **empties it**.

<RecipeFor id="liquid_tank" />

<RecipeFor id="gas_tank" />
