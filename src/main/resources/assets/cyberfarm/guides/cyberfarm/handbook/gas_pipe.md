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

Pipes carry **gas** (biogas, methane, Steam, gases of other mods) from the machines that make it
to the machines and tanks that use or store it. **Liquids** such as water and coolant need a
[Liquid Pipe](liquid_pipe.md). Lines are easy to tell apart: a coloured band down every cable shows
what it carries, amber for gas, blue for liquids, cyan for power. Straight runs join into one
smooth tube; a coupling box appears only where a line branches, bends or ends.

<br clear="all" />

- Pipes **connect by themselves** to other Gas Pipes and to any block that holds fluids, from
  CyberFarm or from any other mod.
- They **store nothing**: what a machine pushes in goes straight to the blocks on the network,
  which take turns.
- From each machine pushing into it, up to **1,000 mB/t** enters the network through a basic
  pipe, **4,000** through an Advanced one and **16,000** through an Elite one. The steam of a Large
  reactor needs Advanced pipes.
- All tiers join into one network. A Gas Pipe never joins a Liquid Pipe, so a water line and a
  steam line can run side by side.

<RecipeFor id="gas_pipe" />

<RecipeFor id="advanced_gas_pipe" />

<RecipeFor id="elite_gas_pipe" />
