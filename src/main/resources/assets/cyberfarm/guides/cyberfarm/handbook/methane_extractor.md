---
navigation:
  title: Methane Extractor
  parent: generators.md
  position: 5
  icon: cyberfarm:methane_extractor
item_ids:
  - cyberfarm:methane_extractor
  - cyberfarm:mob_capsule
  - cyberfarm:containment_glass
  - cyberfarm:acid_inlet
  - cyberfarm:methane_outlet
  - cyberfarm:intake_gate
---

# Methane Extractor

The Methane Extractor is the head of an **extraction chamber**: a 3x3x4 structure where a mob is shut in and
dissolved in **Acid**, giving off **Methane**. One zombie gives enough gas to run a [Steam Engine](steam_engine.md)
at full power. This page also covers the chamber's other parts and the Mob Capsule.

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

## The chamber's parts

| Part | Where it goes | What it does |
| --- | --- | --- |
| **Methane Extractor** | middle of the roof | the head: opens the GUI and takes a Mob Capsule |
| [Cyber Frame](cyber_frame.md) | floor, wall corners, roof | the structure |
| <ItemLink id="containment_glass" /> | middle of each wall side | keeps the mob in view |
| <ItemLink id="acid_inlet" /> (green ring) | in place of any Cyber Frame | the only way acid goes in |
| <ItemLink id="methane_outlet" /> (pale green ring) | in place of any Cyber Frame | the only way methane comes out |
| <ItemLink id="intake_gate" /> | in place of any glass of the **lower** wall | pulls a passing monster in |
| <ItemLink id="mob_capsule" /> | an item, not a block | carries a mob to the chamber |

## Building the chamber

- **Floor**: 3x3 Cyber Frames.
- **Walls** (two layers): a Cyber Frame in each corner and Containment Glass in the middle of
  each side. The centre stays empty: that is where the mob goes.
- **Roof**: 8 Cyber Frames around the **Methane Extractor** in the middle.
- **Ports**: any Cyber Frame can be an Acid Inlet or a Methane Outlet instead. Fluids only pass through them, so build at least one of each.
- **Gate**: optionally swap a glass of the lower wall for an Intake Gate.

**Right-click** the extractor while the chamber is unfinished. Bigger drones fly out, each projecting a group of the
blocks still to place. An amber X marks a block in the way. A second click calls them back.

When the structure is right the chamber powers up with a chime and becomes **one machine**, a sibling of the
[Crystallizer](mech_core.md). Ports show as couplings where a pipe joins them, and gates as roll-up doors.
A finished chamber opens the GUI from any of its blocks. Sneak to place blocks against it instead.

## Putting a mob inside

The chamber holds **one mob**.

- **Intake Gate**: while the chamber is empty, the first **monster** that comes up to a gate is
  pulled inside. The shutter rolls up and closes behind it. Let zombies chase you past the gate, or
  lead them there with a mob farm. Bosses are never taken.
- **Mob Capsule**: right-click a mob with an empty capsule to capture it. Then **right-click the Methane Extractor**
  with the full capsule. This works for animals too, but not for bosses.
- A full capsule right-clicked on any other block lets the mob out there.
- Mobs pulled in through a gate or let out of a capsule never despawn.

## Acid and methane

The mob gives off methane only while it is dissolving, so the chamber needs **Acid**.

- Pipe Acid from an [Acid Synthesizer](acid_synthesizer.md) into an **Acid Inlet** with a [Liquid Pipe](liquid_pipe.md),
  or pour an Acid Bucket into it.
- Take methane from a **Methane Outlet** with a [Gas Pipe](gas_pipe.md). Every outlet pushes it into adjacent pipes and tanks.
- Carry the methane to a [Gas Turbine](gas_turbine.md) or a Steam Engine.
- An empty bucket on an outlet fills a Methane Bucket.

Every 15 seconds the acid eats one point of the mob's health. Holes open in its skin, what is left turns green,
and scraps drop off it among green fumes. In the end it is consumed completely and the chamber is ready for the next mob.

## The GUI

A live view into the tank: the mob inside, the acid rising around it, and how much of the mob is **left**.
The acid tank is on the left, the methane on the right. Without acid the GUI says **NO ACID**.

## Numbers

| Mob | Methane | In Gas Turbines |
| --- | --- | --- |
| Zombies (any kind) | 50 mB/t | 600 FE/t |
| Other monsters | 25 mB/t | 300 FE/t |
| Animals | 8 mB/t | 96 FE/t |

| Stat | Value |
| --- | --- |
| Acid used | 1 mB/t while dissolving |
| Acid tank | 8,000 mB |
| Methane tank | 8,000 mB |
| Damage | 1 health every 15 seconds |
| A zombie (20 health) lasts | about 5 minutes |
| Methane from one zombie | about 300,000 mB (3,600,000 FE in Gas Turbines) |

## Tips

- Mobs that are neither monsters nor animals, such as villagers, give no methane. They are never dissolved either, so they block the chamber.
- Without acid, nothing is produced and the mob is not harmed.
- When the methane tank is full, the chamber stops and uses no acid. Keep the outlets piped.
- The chamber is a lot of work (acid, mobs, the structure), but it pays back far more than a
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
