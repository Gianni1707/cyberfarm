---
navigation:
  title: Drone Frames
  parent: drone_systems.md
  position: 2
  icon: cyberfarm:basic_frame
item_ids:
  - cyberfarm:basic_frame
  - cyberfarm:advanced_frame
  - cyberfarm:elite_frame
  - cyberfarm:mini_frame
---

# Drone Frames

The frame is the base of every drone. Its **tier** decides how big the battery is,
how fast the drone flies, how many function modules it can carry and how many
[addons](addons.md) fit in it.

<ItemGrid>
  <ItemIcon id="basic_frame" />
  <ItemIcon id="advanced_frame" />
  <ItemIcon id="elite_frame" />
</ItemGrid>

| Frame | Tier | Battery | Speed | Module slots | Addons | Flight cost |
| --- | --- | --- | --- | --- | --- | --- |
| Basic Drone Frame | 1 | 20,000 FE | 80% (2.4 blocks/s) | 1 | 4 | 2 FE/t |
| Advanced Drone Frame | 2 | 60,000 FE | 130% (3.9 blocks/s) | 2 | 8 | 3 FE/t |
| Elite Drone Frame | 3 | 150,000 FE | 160% (4.8 blocks/s) | 3 | 16 | 4 FE/t |
| Mini Drone Frame | 1 | 10,000 FE | 100% (3 blocks/s) | 1 | 2 | 1 FE/t |

A drone built on the **Mini Drone Frame** is a **Mini Drone**: half the size of the others, and a
builder only: the frame takes the [Builder Module](modules.md) and nothing else. It lives
only on a [Builder Swarm Dock](drone_dock.md), which takes up to four, each on its own corner of the pad.

<RecipeFor id="mini_frame" />

Speed Addons add 10% each on top of the frame speed. The flight cost is the frame's share of
the drone's [power use](drones.md). Higher tiers are crafted from the tier
below, so you never lose your progress.

## Recipes

<RecipeFor id="basic_frame" />

<Row>
  <RecipeFor id="advanced_frame" />
  <RecipeFor id="elite_frame" />
</Row>
