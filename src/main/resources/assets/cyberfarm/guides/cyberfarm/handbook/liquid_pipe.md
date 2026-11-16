---
navigation:
  title: Liquid Pipe
  parent: cables.md
  position: 3
  icon: cyberfarm:liquid_pipe
item_ids:
  - cyberfarm:liquid_pipe
  - cyberfarm:advanced_liquid_pipe
  - cyberfarm:elite_liquid_pipe
---

# Liquid Pipe

<BlockImage id="cyberfarm:liquid_pipe" p:east="true" p:west="true" scale="4" float="left" />

Liquid Pipes carry **liquids**: water, lava, Coolant, Cryo Coolant, Acid and the liquids of other mods.
**Gases** (biogas, methane, Steam) need a [Gas Pipe](gas_pipe.md) instead.

<br clear="all" />

Typical uses:

- water from a [Water Pump](water_pump.md) to a [Coolant Mixer](coolant_mixer.md), an
  [Acid Synthesizer](acid_synthesizer.md) or a [Fission Reactor](fission_reactor.md);
- acid to the [Methane Extractor](methane_extractor.md).

## Placing

- Pipes **connect by themselves** to other Liquid Pipes and to any block that holds fluids, from
  CyberFarm or any other mod.
- All three tiers join into one network.
- A Liquid Pipe **never joins a Gas Pipe**, so the water line into a reactor and the steam line out
  of it can run side by side.
- To empty a [Liquid Tank](fluid_tanks.md) into a machine, **right-click the pipe's arm** that
  touches the tank so it pulls (see [Cables](cables.md)).

## How it works

- Pipes **store nothing**: what a machine pushes in goes straight to the blocks on the network,
  which take turns.
- The sleeves have **blue lights**.
- While a liquid flows, it fills the glass in its own colour. When the flow stops, the pipe looks
  empty again.

## Numbers

The rate is how much liquid can enter the network per tick from each machine pushing in, set by the
pipe that touches it.

| Tier | Sleeve band | Rate per source |
| --- | --- | --- |
| Liquid Pipe | grey | 1,000 mB/t |
| Advanced Liquid Pipe | gold | 4,000 mB/t |
| Elite Liquid Pipe | violet | 16,000 mB/t |

To upgrade a line already in place, use the [Cable Swapper](cable_swapper.md).

<RecipeFor id="liquid_pipe" />

<RecipeFor id="advanced_liquid_pipe" />

<RecipeFor id="elite_liquid_pipe" />
