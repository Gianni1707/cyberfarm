---
navigation:
  title: Mech Core
  parent: mech.md
  position: 0
  icon: cyberfarm:mech_core
item_ids:
  - cyberfarm:mech_core
  - cyberfarm:fusion_crystal
  - cyberfarm:crystallizer
  - cyberfarm:crystallizer_port
---

# Mech Core

<ItemImage id="cyberfarm:mech_core" scale="3" float="left" />

Every [mech](mech.md) is built around a **Mech Core**: a **Fusion Crystal** set in two elite
frames. The crystal is end-game work, grown at the end of a chain of machines:

<br clear="all" />

1. A running [Fission Reactor](fission_reactor.md) burns Fuel Rods into Depleted Fuel Rods.
2. The [Waste Reprocessor](waste_reprocessor.md) treats them, and one rod in four gives up an
   <ItemLink id="isotope_shard" />.
3. The **Crystallizer** grows the shard into a <ItemLink id="fusion_crystal" />, in
   [Acid](acid_synthesizer.md) with Amethyst Shards and a lot of power.

## Crystallizer

The crystal grows in a **chamber** 3 blocks wide and deep and 4 high:

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

- a floor of <ItemLink id="cyber_frame" />, with the <ItemLink id="crystallizer" /> in the middle
  of its front edge, facing out;
- Cyber Frames up the four corners, walls of <ItemLink id="containment_glass" /> between them,
  and the two blocks in the middle left empty;
- a roof of Cyber Frames.

Any of the Cyber Frames, in the floor, the corners or the roof, can be a
<ItemLink id="crystallizer_port" /> instead: the projector suggests one at the back of the floor.

The [Multiblock Projector](multiblock_projector.md) shows it: pick *Crystallizer*. Complete, the
chamber turns into one machine: a stepped octagonal plinth with the acid in a basin sunk in its
top, four white pylons holding up a crown with an emitter in its hub, a violet energy field
between them and a console on the plinth's front.

Put an Isotope Shard and **4 Amethyst Shards** in the Crystallizer and pipe **2,000 mB of Acid**
into it: in **30 seconds** at **1,000 FE/t** (600,000 FE in all) it grows one Fusion Crystal.
That much power wants a reactor or several Steam Engines behind it.

## Ports

Acid, power and items go in through the Crystallizer itself or through any **Crystallizer Port**
of the chamber: Liquid Pipes, Energy Conduits (or any mod's cables), hoppers and item pipes all
connect to them. The Isotope Shards and Amethyst Shards find their own slots, and automation can
take only the **Fusion Crystal** out: never the ingredients, never the acid. Once the chamber is
complete each port becomes a node of your pipes set on the machine: a coupling like the pipes'
own, with a violet window, in the plinth, against a pylon or under the crown (a port in the floor
also shows a violet socket on the plinth). A pipe or cable you connect runs straight into it.

You watch it grow: beams from the basin and the crown feed a violet speck in the middle of the
field, arcs crackle to it from the pylons, motes of light spiral in and it swells to full size
while the console's bar fills. A
finished crystal hangs there, turning slowly, until you take it out.

<ItemGrid>
  <ItemIcon id="isotope_shard" />
  <ItemIcon id="minecraft:amethyst_shard" />
  <ItemIcon id="fusion_crystal" />
</ItemGrid>

<Row>
  <RecipeFor id="crystallizer" />
  <RecipeFor id="crystallizer_port" />
  <RecipeFor id="mech_core" />
</Row>
