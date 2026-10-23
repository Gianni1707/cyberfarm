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

Pipes for **liquids**: water, lava, Coolant, Cryo Coolant, Acid and the liquids of other mods. Use
them to bring water from a [Water Pump](water_pump.md) to a [Coolant Mixer](coolant_mixer.md), an
[Acid Synthesizer](acid_synthesizer.md) or a [Fission Reactor](fission_reactor.md), and acid to the
[Methane Extractor](methane_extractor.md). Gases (biogas, methane, Steam) need a
[Gas Pipe](gas_pipe.md). Their band is **blue**.

<br clear="all" />

- Pipes **connect by themselves** to other Liquid Pipes and to any block that holds fluids, from
  CyberFarm or from any other mod.
- They **store nothing**: what a machine pushes in goes straight to the blocks on the network,
  which take turns.
- From each machine pushing into it, up to **1,000 mB/t** enters the network through a basic
  pipe, **4,000** through an Advanced one and **16,000** through an Elite one.
- All tiers join into one network, but a Liquid Pipe **never joins a Gas Pipe**: the water line
  into a reactor and the steam line out of it can run side by side.

<RecipeFor id="liquid_pipe" />

<RecipeFor id="advanced_liquid_pipe" />

<RecipeFor id="elite_liquid_pipe" />
