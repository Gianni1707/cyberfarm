---
navigation:
  title: Addons
  parent: index.md
  position: 7
  icon: cyberfarm:speed_addon
item_ids:
  - cyberfarm:speed_addon
  - cyberfarm:stack_addon
  - cyberfarm:battery_addon
  - cyberfarm:fortune_addon
  - cyberfarm:silk_touch_addon
---

# Addons

Addons are chips that **improve a drone**. Put them in the four **addon slots** of the
[Drone Assembler](drone_assembler.md): each slot holds a stack of the same addon.

How many addons fit depends on the [frame](drone_frames.md):

| Frame | Max addons |
| --- | --- |
| Basic Drone Frame | 4 |
| Advanced Drone Frame | 8 |
| Elite Drone Frame | 16 |

<ItemGrid>
  <ItemIcon id="speed_addon" />
  <ItemIcon id="stack_addon" />
  <ItemIcon id="battery_addon" />
  <ItemIcon id="fortune_addon" />
  <ItemIcon id="silk_touch_addon" />
</ItemGrid>

| Addon | Effect | Limit | Power use |
| --- | --- | --- | --- |
| Speed Addon | +10% flight speed each | frame limit only | +10% flight cost |
| Stack Addon | +1 stack (64 items) per [Transport](modules.md) trip each | up to 9 stacks per trip | +5% flight cost |
| Battery Addon | +25% of the frame's battery capacity each | frame limit only | +5% flight cost |
| Fortune Addon | +1 Fortune level for Quarry and Harvester drops | max 3 | +25% energy per job |
| Silk Touch Addon | Silk Touch for Quarry and Harvester drops | max 1 | +50% energy per job |

**Fortune and Silk Touch cannot be combined**, just like the enchantments.

Without Stack Addons a Transport drone carries only **16 items** per trip.

The percentages add up: see how the total is worked out on the [Drones](drones.md) page.

## Recipes

<Row>
  <RecipeFor id="speed_addon" />
  <RecipeFor id="stack_addon" />
</Row>

<RecipeFor id="battery_addon" />

<Row>
  <RecipeFor id="fortune_addon" />
  <RecipeFor id="silk_touch_addon" />
</Row>
