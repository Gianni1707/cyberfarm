---
navigation:
  title: Drones
  parent: index.md
  position: 7
  icon: cyberfarm:drone
item_ids:
  - cyberfarm:drone
---

# Drones

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

Flying costs 2 FE per tick. Drones cannot be hurt by mobs or by players.

## Recovering a drone

**Sneak + right-click** the drone with an empty hand. You get the drone item back with its
charge, and its cargo drops on the ground. If its dock is broken, the drone turns back
into an item by itself.
