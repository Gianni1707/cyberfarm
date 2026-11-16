---
navigation:
  title: Laser Turret
  parent: index.md
  position: 4
  icon: cyberfarm:laser_turret
item_ids:
  - cyberfarm:laser_turret
---

# Laser Turret

<BlockImage id="cyberfarm:laser_turret" scale="4" float="left" />

Monsters at the door? A **Laser Turret** keeps them out. Give it power and it turns its head toward
the nearest hostile mob it can see and burns it down.

<br clear="all" />

## Placing and powering

Place it where it can see the ground you want to guard. It watches **16 blocks** all round: a
sphere 33 blocks across, as wide as the largest drone work area.

Power it with an [Energy Conduit](energy_conduit.md) or any other mod's cable, from any side.

**Right-click** it to see what it is doing. The red lens on the barrel lights up while it aims.

## Numbers

| | Laser Turret |
| --- | --- |
| Range | 16 blocks all round |
| Damage | 8 a shot (a zombie lasts three shots) |
| Fire rate | one shot a second |
| Energy per shot | 200 FE (25 FE per point of damage) |
| Power use | 10 FE/t while it fights, none while nothing is in sight |
| Energy buffer | 20,000 FE |

One [Combustion Generator](combustion_generator.md) keeps six turrets firing. The
[Guard Module](modules.md)'s laser, fired in flight, costs twice as much per point of damage.

## What it shoots

It shoots **hostile mobs only**. These are safe:

- players, animals, villagers and golems;
- **named mobs**: your mob farms are safe if you name the mobs you keep;
- mobs **riding** something (a minecart, a boat);
- **neutral mobs** (endermen, zombified piglins) until they get angry.

It needs a clear view: no block between its head and the mob's eyes. Glass stops it too.

## Defence drones

A turret stays where you put it. For an area that moves with your farm, give a drone a
[Guard Module](modules.md): it flies over its work area and shoots the same mobs from above.
For a big wave, a swarm of Mini Drones with the [Bomb Module](modules.md) dives at the monsters and
blows up on them.

## Crafting

<RecipeFor id="laser_turret" />
