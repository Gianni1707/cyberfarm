---
navigation:
  title: Using Drones
  parent: drone_systems.md
  position: 6
  icon: cyberfarm:drone
item_ids:
  - cyberfarm:drone
---

# Using Drones

<ItemImage id="cyberfarm:drone" scale="2" float="left" />

An assembled drone shows its parts and battery in the tooltip. To put it to work,
**right-click a [Drone Dock](drone_dock.md)** with it. It lands folded on the pad, facing the dock's
front (the side that faced you when you placed it, the same way it parks whenever it comes home), opens its arms,
spins its rotors up and lifts off for a moment, then gets to work (or settles back on the dock).

<br clear="all" />

## What the drone does

1. It waits on the dock and **charges** until its battery is full
   (or until the dock runs out of power, as long as it has at least 30%).
2. It asks its [modules](modules.md) for a job (harvest, dig, carry) and does it, chaining
   jobs of the same kind without flying home in between.
3. It returns to the dock when its **cargo is full** (9 slots) or its [Tank Module](modules.md) is, its battery drops **below 10%**,
   or there is nothing left to do. With several modules, the next trip starts from the next module.
4. It unloads into the dock, charges and starts again.

While they work, drones hit their target with a short **beam of light** (a steady one while they
dig), and flying low their rotors kick up **dust**, or spray over water.

Drones fly in a **straight line** to their target, slowing down as they arrive. If something
blocks the way they find a path around it. Flight speed comes from the [frame](drone_frames.md)
and the [Speed Addons](addons.md).

## Power use

The tooltip of a drone shows its **flight cost** in FE per tick. It is worked out like this:

**flight cost = (frame + modules) x (1 + addon percentages)**, rounded up.

- [Frame](drone_frames.md): 4, 6 or 8 FE/t by tier.
- [Modules](modules.md): +2 FE/t each (+4 for the Quarry, Logger, Taxi, Builder, Guard and Sponge).
- [Addons](addons.md): Speed +10%, Stack +5%, Battery +5%, Efficiency -10% each.

Example: a Basic frame with a Harvester and 2 Speed Addons uses (4 + 2) x 1.2 = 7.2, so **8 FE/t**.

Jobs cost energy on top of flying (see each [module](modules.md)); Fortune adds +25% per level,
Silk Touch +50% and each Efficiency Addon -10% to that. [Battery Addons](addons.md) make longer trips possible.

Drones cannot be hurt by mobs or by players.

## Changing a drone

Put the drone back in the **output slot** of the [Drone Assembler](drone_assembler.md) with the
other slots empty: the button becomes **Disassemble**. Press it and the frame, modules and
addons go back into their slots, ready to be changed and assembled again. The drone's charge
moves into the assembler, as much as its buffer holds (20,000 FE, more with Battery Addons).

## Recovering a drone

Drones are **indestructible**: weapons, mobs, explosions, fire and lava cannot hurt them, and a
left-click goes right through them, so you cannot knock one down by mistake.

**Sneak + right-click** the drone with an empty hand. It lands (on its dock if it is there,
otherwise on the ground below), its rotors spin down, it folds its arms and then jumps into your
inventory with its charge; its cargo goes to the dock, or drops on the ground if the dock is full. If its dock is broken, the drone turns back
into an item by itself.
