---
navigation:
  title: Energy Conduit
  parent: cables.md
  position: 1
  icon: cyberfarm:energy_conduit
item_ids:
  - cyberfarm:energy_conduit
---

# Energy Conduit

<BlockImage id="cyberfarm:energy_conduit" p:east="true" p:west="true" scale="4" float="left" />

Conduits carry **FE** from generators to machines, so they no longer need to touch each other.

<br clear="all" />

- Conduits **connect by themselves** to other conduits and to any block that uses FE, from
  CyberFarm or from any other mod.
- They **store nothing**: whatever a generator pushes in goes straight to the machines on the
  network, which **take turns** so none is left without power.
- Up to **1,000 FE/t** enters the network from each generator or battery pushing into it.
- Networks can be up to 1,024 conduits long.

<GameScene zoom={4} interactive={true}>
  <Block id="cyberfarm:combustion_generator" p:facing="south" />
  <Block id="cyberfarm:energy_conduit" x="1" p:west="true" p:east="true" />
  <Block id="cyberfarm:energy_conduit" x="2" p:west="true" p:east="true" />
  <Block id="cyberfarm:drone_assembler" x="3" p:facing="south" />
</GameScene>

<RecipeFor id="energy_conduit" />
