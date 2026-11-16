---
navigation:
  title: Gas Pipe
  parent: cables.md
  position: 2
  icon: cyberfarm:gas_pipe
item_ids:
  - cyberfarm:gas_pipe
  - cyberfarm:advanced_gas_pipe
  - cyberfarm:elite_gas_pipe
---

# Gas Pipe

<BlockImage id="cyberfarm:gas_pipe" p:east="true" p:west="true" scale="4" float="left" />

Gas Pipes carry **gas** (biogas, methane, Steam and the gases of other mods) from the machines that
make it to the machines and tanks that use or store it. **Liquids** such as water and coolant need a
[Liquid Pipe](liquid_pipe.md) instead.

<br clear="all" />

## Placing

- Pipes **connect by themselves** to other Gas Pipes and to any block that holds fluids, from
  CyberFarm or any other mod.
- All three tiers join into one network.
- A Gas Pipe never joins a Liquid Pipe, so a water line and a steam line can run side by side.
- To empty a [Gas Tank](fluid_tanks.md) into a machine, **right-click the pipe's arm** that touches
  the tank so it pulls (see [Cables](cables.md)).

## How it works

- Pipes **store nothing**: what a machine pushes in goes straight to the blocks on the network,
  which take turns.
- The sleeves have **green lights**.
- While a gas flows you can see it in the glass, see-through with puffs drifting along. When the
  flow stops, the pipe looks empty again.
- Where a line branches, bends or ends, the glass becomes a small framed cube.

## Numbers

The rate is how much gas can enter the network per tick from each machine pushing in, set by the
pipe that touches it.

| Tier | Sleeve band | Rate per source |
| --- | --- | --- |
| Gas Pipe | grey | 1,000 mB/t |
| Advanced Gas Pipe | gold | 4,000 mB/t |
| Elite Gas Pipe | violet | 16,000 mB/t |

The steam of a Large reactor needs Advanced pipes. To upgrade a line already in place, use the
[Cable Swapper](cable_swapper.md).

<RecipeFor id="gas_pipe" />

<RecipeFor id="advanced_gas_pipe" />

<RecipeFor id="elite_gas_pipe" />
