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
---

# Methane Extractor

The Methane Extractor is the "shower head" on top of an **extraction chamber**: a 3x3x4
structure where a mob is shut in, giving off **Methane**.

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
  <Block id="minecraft:glass" x="0" y="1" z="1" />
  <Block id="cyberfarm:cyber_frame" x="0" y="1" z="2" />
  <Block id="minecraft:glass" x="1" y="1" z="0" />
  <Block id="minecraft:glass" x="1" y="1" z="2" />
  <Block id="cyberfarm:cyber_frame" x="2" y="1" z="0" />
  <Block id="minecraft:glass" x="2" y="1" z="1" />
  <Block id="cyberfarm:cyber_frame" x="2" y="1" z="2" />
  <Block id="cyberfarm:cyber_frame" x="0" y="2" z="0" />
  <Block id="minecraft:glass" x="0" y="2" z="1" />
  <Block id="cyberfarm:cyber_frame" x="0" y="2" z="2" />
  <Block id="minecraft:glass" x="1" y="2" z="0" />
  <Block id="minecraft:glass" x="1" y="2" z="2" />
  <Block id="cyberfarm:cyber_frame" x="2" y="2" z="0" />
  <Block id="minecraft:glass" x="2" y="2" z="1" />
  <Block id="cyberfarm:cyber_frame" x="2" y="2" z="2" />
  <Block id="cyberfarm:cyber_frame" x="0" y="3" z="0" />
  <Block id="cyberfarm:cyber_frame" x="0" y="3" z="1" />
  <Block id="cyberfarm:cyber_frame" x="0" y="3" z="2" />
  <Block id="cyberfarm:cyber_frame" x="1" y="3" z="0" />
  <Block id="cyberfarm:methane_extractor" x="1" y="3" z="1" />
  <Block id="cyberfarm:cyber_frame" x="1" y="3" z="2" />
  <Block id="cyberfarm:cyber_frame" x="2" y="3" z="0" />
  <Block id="cyberfarm:cyber_frame" x="2" y="3" z="1" />
  <Block id="cyberfarm:cyber_frame" x="2" y="3" z="2" />
  <Block id="cyberfarm:gas_pipe" x="1" y="4" z="1" p:down="true" />
  <Entity id="minecraft:zombie" x="1.5" y="1" z="1.5" />
</GameScene>

## Building the chamber

- **Floor**: 3x3 <ItemLink id="cyber_frame" />s.
- **Walls** (two layers): a Cyber Frame Block in each corner and **glass** in the middle of each
  side. The centre stays empty: that is where the mob goes.
- **Roof**: 8 Cyber Frame Blocks around the **Methane Extractor** in the middle.

When the structure is right the chamber powers up with a chime: the extractor and every
Cyber Frame light up with cyan energy lines, and the GUI says **Chamber ready**. A finished chamber opens the GUI from any of its blocks
(sneak to place blocks against it instead).

## Putting a mob inside

Use a <ItemLink id="mob_capsule" />: right-click a mob to capture it, then **right-click the
Methane Extractor** with the full capsule. The mob appears inside the chamber. The chamber holds
one mob, and mobs released from a capsule never despawn.

| Mob | Methane |
| --- | --- |
| Zombies (any kind) | 10 mB/t |
| Other monsters | 5 mB/t |
| Animals | 2 mB/t |

The extractor holds **8,000 mB**. Connect a [Gas Pipe](gas_pipe.md) **on top** of it to carry the
methane to a [Gas Turbine](gas_turbine.md).

<Row>
  <RecipeFor id="methane_extractor" />
  <RecipeFor id="cyber_frame" />
  <RecipeFor id="mob_capsule" />
</Row>
