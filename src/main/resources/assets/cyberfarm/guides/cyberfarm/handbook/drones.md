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
**right-click a [Drone Dock](drone_dock.md)** with it.

<br clear="all" />

## What the drone does

1. It waits on the dock and **charges** until its battery is full
   (or until the dock runs out of power, as long as it has at least 30%).
2. It asks its [modules](modules.md) for a job (harvest, dig, carry) and does it, chaining
   jobs of the same kind without flying home in between.
3. It returns to the dock when its **cargo is full** (9 slots), its battery drops **below 10%**,
   or there is nothing left to do. With several modules, the next trip starts from the next module.
4. It unloads into the dock, charges and starts again.

Drones fly in a **straight line** to their target, slowing down as they arrive. If something
blocks the way they find a path around it. Flight speed comes from the [frame](drone_frames.md)
and the [Speed Addons](addons.md).

## Power use

The tooltip of a drone shows its **flight cost** in FE per tick. It is worked out like this:

**flight cost = (frame + modules) x (1 + addon percentages)**, rounded up.

- [Frame](drone_frames.md): 2, 3 or 4 FE/t by tier.
- [Modules](modules.md): +1 FE/t each (+2 for the Quarry).
- [Addons](addons.md): Speed +10%, Stack +5%, Battery +5% each.

Example: a Basic frame with a Harvester and 2 Speed Addons uses (2 + 1) x 1.2 = 3.6, so **4 FE/t**.

Jobs cost energy on top of flying (see each [module](modules.md)); Fortune adds +25% per level
and Silk Touch +50% to that. [Battery Addons](addons.md) make longer trips possible.

Drones cannot be hurt by mobs or by players.

## Changing a drone

Put the drone back in the **output slot** of the [Drone Assembler](drone_assembler.md) with the
other slots empty: the button becomes **Disassemble**. Press it and the frame, modules and
addons go back into their slots, ready to be changed and assembled again. The drone's charge
moves into the assembler (up to its 20,000 FE buffer).

## Recovering a drone

**Sneak + right-click** the drone with an empty hand. You get the drone item back with its
charge, and its cargo drops on the ground. If its dock is broken, the drone turns back
into an item by itself.
