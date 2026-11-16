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

The frame is the base of every drone. Its **tier** decides how big the battery is, how fast the
drone flies, how many [function modules](modules.md) it carries and how many
[addons](addons.md) fit in it.

<ItemGrid>
  <ItemIcon id="basic_frame" />
  <ItemIcon id="advanced_frame" />
  <ItemIcon id="elite_frame" />
  <ItemIcon id="mini_frame" />
</ItemGrid>

## Comparing the frames

| Frame | Tier | Battery | Speed | Module slots | Addons | Flight cost |
| --- | --- | --- | --- | --- | --- | --- |
| Basic Drone Frame | 1 | 40,000 FE | 80% (2.4 blocks/s) | 1 | 4 | 4 FE/t |
| Advanced Drone Frame | 2 | 120,000 FE | 130% (3.9 blocks/s) | 2 | 8 | 6 FE/t |
| Elite Drone Frame | 3 | 300,000 FE | 160% (4.8 blocks/s) | 3 | 16 | 8 FE/t |
| Mini Drone Frame | 1 | 20,000 FE | 100% (3 blocks/s) | 1 | 2 | 2 FE/t |

- **Speed**: each Speed Addon adds 10% on top of the frame speed. Above 100%, the same speed also
  shortens the work itself: digging, clearing a site, cutting trees, soaking up fluid and waiting
  for a catch. Guard and Bomb drones fire and dive at their own pace.
- **Flight cost**: the frame's share of the drone's [power use](drones.md).

## The Mini Drone Frame

<ItemImage id="cyberfarm:mini_frame" scale="2" float="left" />

A drone built on the Mini Drone Frame is a **Mini Drone**: half the size of the others. It is made
for two jobs only: it takes the [Builder Module](modules.md) or the [Bomb Module](modules.md) and
nothing else.

A Mini Drone lives only on a [Builder Swarm Dock](swarm_dock.md), which takes up to four, each on
its own corner of the pad.

<br clear="all" />

<RecipeFor id="mini_frame" />

## Recipes

The Basic and Mini frames are made of [aluminum](aluminum.md). The Advanced and Elite frames are
made of [carbon fiber](carbon_fiber.md), with [Advanced and Elite Circuits](circuits.md).

Higher tiers are crafted from the tier below, so you never lose your progress.

<RecipeFor id="basic_frame" />

<Row>
  <RecipeFor id="advanced_frame" />
  <RecipeFor id="elite_frame" />
</Row>
