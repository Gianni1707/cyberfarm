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

The Water Pump is a small pumpjack that gives an **endless supply of water**. Use it for a
[Fission Reactor](fission_reactor.md), the [Coolant Mixer](coolant_mixer.md) or the [Acid Synthesizer](acid_synthesizer.md).

<br clear="all" />

## Placing it

- Stand it **directly above a water source block**, with its intake hanging into the water.
- Only the block below counts: water beside it does nothing.
- Water can't be clicked. Place the pump against a temporary block beside the spot, then remove that block.
- Keep it above the surface: it won't keep water out of its own block.

## Using it

- Give it power, for example with an [Energy Conduit](energy_conduit.md).
- It pushes the water into adjacent [Liquid Pipes](liquid_pipe.md) and machines.
- Water is a liquid: a [Gas Pipe](gas_pipe.md) will not take it.
- A pipe can plug in on any side. From below, it replaces the intake.
- It has no GUI: **right-click** it to see what it is doing.
- While it pumps, the crank turns, the beam rocks and the rod goes up and down. It runs on for a few seconds after it last pumped.

## Numbers

| Stat | Value |
| --- | --- |
| Water | 500 mB/t |
| Power | 80 FE/t |

In a Fission Reactor, each mB of water carries away one unit of heat. So one pump covers 500 heat per tick:

| Reactor tower | Pumps needed |
| --- | --- |
| Small, any power | 1 |
| Medium, up to 250% | 1 |
| Medium, above 250% | 2 |
| Large at full power (2,000 heat) | 4 |

## Troubleshooting

- A pump that stopped after an update, because it only had water at its sides, has to be moved over the water.

<RecipeFor id="water_pump" />
