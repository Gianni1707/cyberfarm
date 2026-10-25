---
navigation:
  title: Hologram Table
  parent: drone_systems.md
  position: 7
  icon: cyberfarm:hologram_table
item_ids:
  - cyberfarm:hologram_table
---

# Hologram Table

<BlockImage id="cyberfarm:hologram_table" scale="4" float="left" />

A table that projects a **3D map** of the land around it, with your docks and drones on it. Put
one next to your drone yard and watch the whole fleet at work at a glance.

It needs **no power** and no setup. Walk up to it and it **wakes**: a beam of light reaches up
from the table and the land rises from the middle outwards, like a wave. Step away and it folds
back down. While it is on, a **radar pulse** sweeps out from the table every few seconds,
lighting up the land and every drone it passes, and a **scan line** climbs through the relief
inside a frame of light, lighting up the land it crosses.

**Right-click** the table to change how far it shows: **16**, **24** or **32** blocks every way.
The wider the range, the wider the projection grows, so the land stays as easy to read.

<br clear="all" />

## What it shows

- The **land** around the table, in relief: hills, water, trees and buildings
  in the colours of a map. As on a vanilla map, slopes rising to the south are lighter and slopes
  falling away are darker, so the shape of the land reads at a glance.
- Every **Drone Dock** and **Builder Swarm Dock** as a pillar of cyan light.
- Every **drone** as a dot that moves **live**, with a short glowing trail behind it and a thin
  line down to the ground to show how high it flies.
- **Players** as white arrows pointing the way they look, and **hostile mobs** as red dots: you see
  at once if a creeper is heading for your farm.
- **N**, **E**, **S** and **W** on the edges of the map.
- A line floating above it all that counts your **docks**, your **drones** and how many are
  **working**, the **hostile mobs** in range and how far the map reaches.

## Reading the drones

| Colour | The drone is |
| --- | --- |
| Cyan | working |
| Amber | flying home |
| Pale | resting or charging on its dock |

## Good to know

- The land is read again every two seconds; the drones move every frame.
- The map's floor is the ground most of the land stands on: pits, ravines and riverbeds below it
  show flat, and land taller than 24 blocks above it is cut off at the top.
- It shows only what you could see yourself: chunks out of your view distance stay dark.

<RecipeFor id="hologram_table" />
