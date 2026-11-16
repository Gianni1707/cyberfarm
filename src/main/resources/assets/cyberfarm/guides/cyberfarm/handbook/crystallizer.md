---
navigation:
  title: Crystallizer
  parent: mech.md
  position: 1
  icon: cyberfarm:crystallizer
item_ids:
  - cyberfarm:crystallizer
  - cyberfarm:crystallizer_port
---

# Crystallizer

<BlockImage id="cyberfarm:crystallizer" scale="3" float="left" />

The Crystallizer grows a <ItemLink id="fusion_crystal" /> from an <ItemLink id="isotope_shard" />,
in Acid, with Amethyst Shards and a lot of power. The Fusion Crystal is the heart of a
[Mech Core](mech_core.md), so you need a Crystallizer to build a [mech](mech.md).

<br clear="all" />

## Building it

The crystal grows in a **chamber** 3 blocks wide, 3 deep and 4 high:

<GameScene zoom={3} interactive={true}>
  <Block id="cyberfarm:cyber_frame" x="0" y="0" z="0" />
  <Block id="cyberfarm:cyber_frame" x="0" y="1" z="0" />
  <Block id="cyberfarm:cyber_frame" x="0" y="2" z="0" />
  <Block id="cyberfarm:cyber_frame" x="0" y="3" z="0" />
  <Block id="cyberfarm:cyber_frame" x="0" y="0" z="1" />
  <Block id="cyberfarm:containment_glass" x="0" y="1" z="1" />
  <Block id="cyberfarm:containment_glass" x="0" y="2" z="1" />
  <Block id="cyberfarm:cyber_frame" x="0" y="3" z="1" />
  <Block id="cyberfarm:cyber_frame" x="0" y="0" z="2" />
  <Block id="cyberfarm:cyber_frame" x="0" y="1" z="2" />
  <Block id="cyberfarm:cyber_frame" x="0" y="2" z="2" />
  <Block id="cyberfarm:cyber_frame" x="0" y="3" z="2" />
  <Block id="cyberfarm:crystallizer" x="1" y="0" z="0" p:facing="north" />
  <Block id="cyberfarm:containment_glass" x="1" y="1" z="0" />
  <Block id="cyberfarm:containment_glass" x="1" y="2" z="0" />
  <Block id="cyberfarm:cyber_frame" x="1" y="3" z="0" />
  <Block id="cyberfarm:cyber_frame" x="1" y="0" z="1" />
  <Block id="cyberfarm:cyber_frame" x="1" y="3" z="1" />
  <Block id="cyberfarm:crystallizer_port" x="1" y="0" z="2" />
  <Block id="cyberfarm:containment_glass" x="1" y="1" z="2" />
  <Block id="cyberfarm:containment_glass" x="1" y="2" z="2" />
  <Block id="cyberfarm:cyber_frame" x="1" y="3" z="2" />
  <Block id="cyberfarm:cyber_frame" x="2" y="0" z="0" />
  <Block id="cyberfarm:cyber_frame" x="2" y="1" z="0" />
  <Block id="cyberfarm:cyber_frame" x="2" y="2" z="0" />
  <Block id="cyberfarm:cyber_frame" x="2" y="3" z="0" />
  <Block id="cyberfarm:cyber_frame" x="2" y="0" z="1" />
  <Block id="cyberfarm:containment_glass" x="2" y="1" z="1" />
  <Block id="cyberfarm:containment_glass" x="2" y="2" z="1" />
  <Block id="cyberfarm:cyber_frame" x="2" y="3" z="1" />
  <Block id="cyberfarm:cyber_frame" x="2" y="0" z="2" />
  <Block id="cyberfarm:cyber_frame" x="2" y="1" z="2" />
  <Block id="cyberfarm:cyber_frame" x="2" y="2" z="2" />
  <Block id="cyberfarm:cyber_frame" x="2" y="3" z="2" />
</GameScene>

- **Floor**: <ItemLink id="cyber_frame" />s, with the <ItemLink id="crystallizer" /> in the middle
  of its front edge, facing out.
- **Walls**: Cyber Frames up the four corners, <ItemLink id="containment_glass" /> between them.
  Leave the two blocks in the middle empty.
- **Roof**: Cyber Frames.

Any of the Cyber Frames, in the floor, the corners or the roof, can be a
<ItemLink id="crystallizer_port" /> instead. The projector suggests one at the back of the floor.

Need help placing it? Right-click the Crystallizer while the chamber is unfinished: bigger drones
fly out, each projecting a group of the blocks still to place. An amber X marks a block in the
way. A second click calls them back. The [Multiblock Projector](multiblock_projector.md) shows it
too: pick *Crystallizer*.

When the chamber is complete it turns into one machine: a stepped octagonal plinth with the acid in
a basin sunk in its top, four pylons holding up a crown with an emitter in its hub, a violet energy
field between them, and a console on the plinth's front under the radiation mark. Alone, the
Crystallizer is a small pedestal with a seed of the crystal in its basin.

## Using it

1. Put an **Isotope Shard** and **4 Amethyst Shards** in the Crystallizer.
2. Pipe in **2,000 mB of Acid** from an [Acid Synthesizer](acid_synthesizer.md).
3. Give it power: it uses **4,000 FE/t**. That much power wants a [reactor](fission_reactor.md) or
   several [Steam Engines](steam_engine.md) behind it.
4. After **30 seconds** a Fusion Crystal is ready. Take it out.

The tab beside its GUI takes Speed, Efficiency and Battery [addons](addons.md).

You can watch it grow: beams from the basin and the crown feed a violet speck in the middle of the
field, arcs crackle to it from the pylons, motes of light spiral in, and it swells to full size
while the console's bar fills. A finished crystal hangs there, turning slowly, until you take it out.

<ItemGrid>
  <ItemIcon id="isotope_shard" />
  <ItemIcon id="minecraft:amethyst_shard" />
  <ItemIcon id="fusion_crystal" />
</ItemGrid>

## Ports and automation

Acid, power and items can go in through:

- the Crystallizer itself;
- any **Crystallizer Port** of the chamber;
- the formed chamber's **plinth**, at the middle of each side, and the **middle of its roof**,
  from outside.

[Liquid Pipes](liquid_pipe.md), [Energy Conduits](energy_conduit.md) (or any mod's cables), hoppers
and item pipes all connect to them. The Isotope Shards and Amethyst Shards find their own slots.
Automation can take out only the **Fusion Crystal**: never the ingredients, never the acid.

Once the chamber is complete, each port becomes a node of your pipes set on the machine: a coupling
like the pipes' own, with a violet window, in the plinth, against a pylon or under the crown. A port
in the floor also shows a violet socket on the plinth. A pipe or cable you connect runs straight into it.

## Numbers

| What | Value |
| --- | --- |
| Recipe | 1 Isotope Shard + 4 Amethyst Shards + 2,000 mB Acid -> 1 Fusion Crystal |
| Time | 30 seconds |
| Power | 4,000 FE/t (2,400,000 FE per crystal) |
| Acid tank | 8,000 mB |
| Energy buffer | 1,000,000 FE |

<Row>
  <RecipeFor id="crystallizer" />
  <RecipeFor id="crystallizer_port" />
</Row>
