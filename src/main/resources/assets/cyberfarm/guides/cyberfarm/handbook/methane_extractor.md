---
navigation:
  title: Methane Extractor
  parent: generators.md
  position: 4
  icon: cyberfarm:methane_extractor
item_ids:
  - cyberfarm:methane_extractor
  - cyberfarm:cyber_frame
  - cyberfarm:mob_capsule
  - cyberfarm:containment_glass
  - cyberfarm:acid_inlet
  - cyberfarm:methane_outlet
  - cyberfarm:intake_gate
---

# Methane Extractor

The Methane Extractor is the "shower head" on top of an **extraction chamber**: a 3x3x4
structure where a mob is shut in and dissolved in **Acid**, giving off **Methane**.

<GameScene zoom={4} interactive={true}>
  <Block id="cyberfarm:cyber_frame" x="0" y="0" z="0" />
  <Block id="cyberfarm:cyber_frame" x="0" y="0" z="1" />
  <Block id="cyberfarm:cyber_frame" x="0" y="0" z="2" />
  <Block id="cyberfarm:cyber_frame" x="1" y="0" z="0" />
  <Block id="cyberfarm:cyber_frame" x="1" y="0" z="1" />
  <Block id="cyberfarm:cyber_frame" x="1" y="0" z="2" />
  <Block id="cyberfarm:cyber_frame" x="2" y="0" z="0" />
  <Block id="cyberfarm:cyber_frame" x="2" y="0" z="1" />
  <Block id="cyberfarm:cyber_frame" x="2" y="0" z="2" />
  <Block id="cyberfarm:cyber_frame" x="0" y="1" z="0" />
  <Block id="cyberfarm:containment_glass" x="0" y="1" z="1" />
  <Block id="cyberfarm:cyber_frame" x="0" y="1" z="2" />
  <Block id="cyberfarm:intake_gate" x="1" y="1" z="0" />
  <Block id="cyberfarm:containment_glass" x="1" y="1" z="2" />
  <Block id="cyberfarm:cyber_frame" x="2" y="1" z="0" />
  <Block id="cyberfarm:containment_glass" x="2" y="1" z="1" />
  <Block id="cyberfarm:cyber_frame" x="2" y="1" z="2" />
  <Block id="cyberfarm:cyber_frame" x="0" y="2" z="0" />
  <Block id="cyberfarm:containment_glass" x="0" y="2" z="1" />
  <Block id="cyberfarm:cyber_frame" x="0" y="2" z="2" />
  <Block id="cyberfarm:containment_glass" x="1" y="2" z="0" />
  <Block id="cyberfarm:containment_glass" x="1" y="2" z="2" />
  <Block id="cyberfarm:cyber_frame" x="2" y="2" z="0" />
  <Block id="cyberfarm:containment_glass" x="2" y="2" z="1" />
  <Block id="cyberfarm:cyber_frame" x="2" y="2" z="2" />
  <Block id="cyberfarm:cyber_frame" x="0" y="3" z="0" />
  <Block id="cyberfarm:acid_inlet" x="0" y="3" z="1" />
  <Block id="cyberfarm:cyber_frame" x="0" y="3" z="2" />
  <Block id="cyberfarm:cyber_frame" x="1" y="3" z="0" />
  <Block id="cyberfarm:methane_extractor" x="1" y="3" z="1" />
  <Block id="cyberfarm:cyber_frame" x="1" y="3" z="2" />
  <Block id="cyberfarm:cyber_frame" x="2" y="3" z="0" />
  <Block id="cyberfarm:methane_outlet" x="2" y="3" z="1" />
  <Block id="cyberfarm:cyber_frame" x="2" y="3" z="2" />
  <Block id="cyberfarm:gas_pipe" x="3" y="3" z="1" p:west="true" />
  <Entity id="minecraft:zombie" x="1.5" y="1" z="1.5" />
</GameScene>

## Building the chamber

- **Floor**: 3x3 <ItemLink id="cyber_frame" />s.
- **Walls** (two layers): a Cyber Frame Block in each corner and <ItemLink id="containment_glass" /> in the middle of
  each side. The centre stays empty: that is where the mob goes.
- **Roof**: 8 Cyber Frame Blocks around the **Methane Extractor** in the middle.
- **Ports**: any Cyber Frame can be an <ItemLink id="acid_inlet" /> or a <ItemLink id="methane_outlet" />
  instead. Fluids only go in and out of the chamber through them: the **Acid Inlet** (green ring)
  only takes acid in, the **Methane Outlet** (pale green ring) only gives methane out. Pipe acid to the
  inlet with a [Liquid Pipe](liquid_pipe.md) and take methane from the outlet with a Gas Pipe.
- **Intake Gate**: any glass of the **lower** wall can be an <ItemLink id="intake_gate" />.

Right-click the extractor while it is unfinished and bigger drones fly out, each projecting a group of the blocks still to place; an amber X marks a block in the way. A second click calls them back. 

When the structure is right the chamber powers up with a chime and becomes **one machine**, a sibling of the
[Crystallizer](mech_core.md): a stepped octagonal plinth, four pylons and a crown with the extractor's head in its hub, and between
them a stepped glass bell with the mob in plain view. Ports show as couplings where a pipe joins them, gates as roll-up doors
between two pylons. The acid pools in the bell's lowest step, and while a mob dissolves a spray ring strokes up and down
around its head on four rods. A finished chamber opens the GUI from any of its blocks (sneak to
place blocks against it instead).

## The GUI

A live view into the tank: the mob inside, the acid rising around it, bubbles and arcs while it
dissolves, and how much of it is **left**. The acid tank is on the left, the methane on the right.

## Acid and methane

The mob gives off methane only while it is dissolving: pipe **Acid** from an
[Acid Synthesizer](acid_synthesizer.md) into an **Acid Inlet** (or pour an Acid Bucket into it). It
uses **1 mB of acid per tick** and holds 8,000 mB. While acid is pumped in you see it pour from the
extractor's head into the pool, splashing as the level rises. Every 15 seconds the acid eats into the mob: holes open in its
skin patch by patch, what is left turns a sickly green, and scraps of flesh and goo drop off it
among more and more green fumes. In
the end it is consumed completely and the chamber is ready for the next one. Without acid the GUI
says **NO ACID** and nothing is produced.

The methane (up to **8,000 mB**) is pushed out of every **Methane Outlet** into adjacent
[Gas Pipes](gas_pipe.md) and tanks (when it is full, the chamber stops and no acid is used): carry it to a [Gas Turbine](gas_turbine.md) or a
[Steam Engine](steam_engine.md). An empty bucket on an outlet fills a Methane Bucket.

## Putting a mob inside

- **Intake Gate**: while the chamber is empty, the first **monster** that comes up to a gate is
  pulled inside; the shutter rolls up and closes behind it. Let zombies chase you past the gate, or
  lead them there with a mob farm. Bosses are never taken, and mobs pulled in never despawn.
- **Mob Capsule**: right-click a mob with a <ItemLink id="mob_capsule" /> to capture it, then
  **right-click the Methane Extractor** with the full capsule. Works for animals too.

The chamber holds one mob.

| Mob | Methane | In a Gas Turbine |
| --- | --- | --- |
| Zombies (any kind) | 25 mB/t | 200 FE/t |
| Other monsters | 12 mB/t | 96 FE/t |
| Animals | 4 mB/t | 32 FE/t |

A zombie lasts about 5 minutes and gives about 150,000 mB of methane in all. The chamber is a lot
of work (acid, mobs, the structure), so it pays back far more than a
[Bio-Digester](bio_digester.md).

<Row>
  <RecipeFor id="methane_extractor" />
  <RecipeFor id="cyber_frame" />
  <RecipeFor id="mob_capsule" />
  <RecipeFor id="containment_glass" />
</Row>

<Row>
  <RecipeFor id="acid_inlet" />
  <RecipeFor id="methane_outlet" />
  <RecipeFor id="intake_gate" />
</Row>
