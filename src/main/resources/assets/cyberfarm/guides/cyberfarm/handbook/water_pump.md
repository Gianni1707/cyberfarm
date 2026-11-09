---
navigation:
  title: Water Pump
  parent: index.md
  position: 3
  icon: cyberfarm:water_pump
item_ids:
  - cyberfarm:water_pump
---

# Water Pump

<BlockImage id="cyberfarm:water_pump" scale="4" float="left" />

An endless supply of water for reactors and the Coolant Mixer. A small pumpjack: stand it **directly above a
water source block**, with its intake hanging into the water, and give it power.

<br clear="all" />

- Pumps **500 mB/t** with **80 FE/t** and pushes it into adjacent [Liquid Pipes](liquid_pipe.md) and machines
  (water is a liquid: a [Gas Pipe](gas_pipe.md) will not take it).
- One pump feeds 500 heat per tick: enough for a Small tower and a Medium one up to 250%. A Large tower
  at full power (2,000 heat) needs 4 pumps.
- It has no GUI: **right-click** it to see what it is doing. While it pumps the crank turns, the beam rocks and
  the rod goes up and down; it runs on for a few seconds after it last pumped.
- Only the block below counts: water beside it does nothing. A pump that stopped after an update, because it only
  had water at its sides, has to be moved over the water.
- Water can't be clicked: place the pump against a temporary block beside the spot, then remove that block. Keep it
  above the surface, as it won't keep water out of its own block.
- A pipe can plug in on any side, and from below it replaces the intake.

<RecipeFor id="water_pump" />
