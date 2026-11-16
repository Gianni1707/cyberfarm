---
navigation:
  title: Energy Conduit
  parent: cables.md
  position: 1
  icon: cyberfarm:energy_conduit
item_ids:
  - cyberfarm:energy_conduit
  - cyberfarm:advanced_energy_conduit
  - cyberfarm:elite_energy_conduit
---

# Energy Conduit

<BlockImage id="cyberfarm:energy_conduit" p:east="true" p:west="true" scale="4" float="left" />

Energy Conduits carry **FE** (power) from generators and batteries to the machines that use it.
With them, a machine no longer has to touch its generator.

<br clear="all" />

## Placing

- Conduits **connect by themselves** to other conduits and to any block that uses or stores FE,
  from CyberFarm or any other mod.
- All three tiers join into one network. A network can be up to **1,024** conduits long.
- Machines built with [Cyber Frames](cyber_frame.md) (Bio-Digester, Biomass Press, Gas Turbine, Electrolyzer and the
  others) take a conduit on **any of their frames**, not only on the machine block. Pipes and hoppers too.
- A generator pushes power only from its own block. If your conduit touches one of its frames instead,
  **right-click the conduit's arm** so it pulls (see [Cables](cables.md)).

<GameScene zoom={4} interactive={true}>
  <Block id="cyberfarm:combustion_generator" p:facing="south" />
  <Block id="cyberfarm:energy_conduit" x="1" p:west="true" p:east="true" />
  <Block id="cyberfarm:energy_conduit" x="2" p:west="true" p:east="true" />
  <Block id="cyberfarm:drone_assembler" x="3" p:facing="south" />
</GameScene>

## How it works

- Conduits **store nothing**: what a generator pushes in goes straight to the machines on the network.
- The machines **take turns**, so none is left without power.
- Most batteries don't push. To draw from one, **right-click the arm** of the conduit touching it so it
  pulls. A [Battery Bank](battery_bank.md) side set to **Out** pushes by itself.
- The sleeves have **cyan lights**. While energy flows, a **cyan arc** runs through the glass.

## Numbers

The rate is how much FE can enter the network per tick from each generator or battery, set by the
conduit that touches it.

| Tier | Sleeve band | Rate per source |
| --- | --- | --- |
| Energy Conduit | grey | 2,000 FE/t |
| Advanced Energy Conduit | gold | 16,000 FE/t |
| Elite Energy Conduit | violet | 128,000 FE/t |

A Medium Steam Turbine needs Advanced conduits at its ports, a Large one Elite conduits. To upgrade a
line already in place, use the [Cable Swapper](cable_swapper.md).

<RecipeFor id="energy_conduit" />

<RecipeFor id="advanced_energy_conduit" />

<RecipeFor id="elite_energy_conduit" />
