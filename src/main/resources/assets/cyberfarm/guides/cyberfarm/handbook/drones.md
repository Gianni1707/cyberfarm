---
navigation:
  title: Using Drones
  parent: drone_systems.md
  position: 8
  icon: cyberfarm:drone
item_ids:
  - cyberfarm:drone
---

# Using Drones

<ItemImage id="cyberfarm:drone" scale="2" float="left" />

A drone is a flying worker built in the [Drone Assembler](drone_assembler.md). Put it on a dock and
it does its [modules](modules.md)' jobs by itself, flying home to unload and charge. Its tooltip
shows its parts, its battery and its flight cost.

<br clear="all" />

## Putting a drone to work

**Right-click a [Drone Dock](drone_dock.md)** with the drone. Mini Drones go on a
[Builder Swarm Dock](swarm_dock.md) instead.

The drone lands folded on the pad, facing the dock's front (the side that faced you when you
placed the dock). It opens its arms, spins its rotors up and lifts off for a moment, then gets to
work, or settles back on the dock.

Tell it where to work with the [Survey Tool](survey_tool.md).

## What the drone does

1. It waits on the dock and **charges** until its battery is full, or until the dock runs out of
   power, as long as it has at least **30%**.
2. It asks its [modules](modules.md) for a job (harvest, dig, carry) and does it, chaining jobs of
   the same kind without flying home in between.
3. It returns to the dock when:
   - its **cargo is full** (9 slots), or its [Tank Module](modules.md) is;
   - its battery drops **below 10%**;
   - there is nothing left to do.
4. It unloads into the dock, charges and starts again. With several modules, the next trip starts
   from the next module.

Drones fly in a **straight line** to their target, slowing down as they arrive. If something
blocks the way they find a path around it. Flight speed comes from the [frame](drone_frames.md)
and the [Speed Addons](addons.md).

While they work, drones hit their target with a short **beam of light** (a steady one while they
dig). Flying low, their rotors kick up **dust**, or spray over water.

## Recovering a drone

**Sneak + right-click** the drone with an empty hand.

- It lands on its dock if it is there, otherwise on the ground below.
- Its rotors spin down, it folds its arms and then jumps into your inventory with its charge.
- Its cargo goes to the dock, or drops on the ground if the dock is full.

If its dock is broken, the drone turns back into an item by itself.

Drones are **indestructible**: weapons, mobs, players, explosions, fire and lava cannot hurt them.
A left-click goes right through them, so you cannot knock one down by mistake.

## Changing a drone

Put the drone in the **output slot** of the [Drone Assembler](drone_assembler.md) with the other
slots empty: the button becomes **Disassemble**. Press it and the frame, modules and addons go
back into their slots, ready to be changed and assembled again.

The drone's charge moves into the assembler, as much as its buffer holds (20,000 FE, more with
Battery Addons).

## Power use

A drone uses power in two ways: a **flight cost** every tick it flies, and energy for each job.

The **flight cost** is shown in the tooltip, in FE per tick:

**flight cost = (frame + modules) x (1 + addon percentages)**, rounded up.

| Part | Adds |
| --- | --- |
| [Frame](drone_frames.md) | 4, 6 or 8 FE/t by tier (2 FE/t for the Mini) |
| Each [module](modules.md) | +2 FE/t (+4 for the Quarry, Logger, Taxi, Builder, Guard and Sponge) |
| Each Speed Addon | +10% |
| Each Stack Addon | +5% |
| Each Battery Addon | +5% |
| Each Efficiency Addon | -10% |

Example: a Basic frame with a Harvester and 2 Speed Addons uses (4 + 2) x 1.2 = 7.2, so **8 FE/t**.

**Jobs** cost energy on top of flying: see each [module](modules.md). These [addons](addons.md)
change the cost of every job:

| Addon | Job cost |
| --- | --- |
| Fortune Addon | +25% per level |
| Silk Touch Addon | +50% |
| Efficiency Addon | -10% each |

[Battery Addons](addons.md) make longer trips possible.
