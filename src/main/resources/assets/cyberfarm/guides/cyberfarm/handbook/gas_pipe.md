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

Pipes carry **gas** (and any other fluid) from the machines that make it to the machines and
tanks that use or store it. Power and gas lines are easy to tell apart: pipes have an amber
stripe, conduits a cyan one.

<br clear="all" />

- Pipes **connect by themselves** to other Gas Pipes and to any block that holds fluids, from
  CyberFarm or from any other mod.
- They **store nothing**: what a machine pushes in goes straight to the blocks on the network,
  which take turns.
- From each machine pushing into it, up to **1,000 mB/t** enters the network through a basic
  pipe, **4,000** through an Advanced one and **16,000** through an Elite one. The steam of a Large
  reactor needs Advanced pipes.
- All tiers join into one network.

<RecipeFor id="gas_pipe" />

<RecipeFor id="advanced_gas_pipe" />

<RecipeFor id="elite_gas_pipe" />
