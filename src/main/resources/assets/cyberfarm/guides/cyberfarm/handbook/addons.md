---
navigation:
  title: Addons
  parent: drone_systems.md
  position: 4
  icon: cyberfarm:speed_addon
item_ids:
  - cyberfarm:speed_addon
  - cyberfarm:stack_addon
  - cyberfarm:battery_addon
  - cyberfarm:fortune_addon
  - cyberfarm:silk_touch_addon
  - cyberfarm:anchor_addon
  - cyberfarm:efficiency_addon
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
| Mini Drone Frame | 2 |

<ItemGrid>
  <ItemIcon id="speed_addon" />
  <ItemIcon id="stack_addon" />
  <ItemIcon id="battery_addon" />
  <ItemIcon id="fortune_addon" />
  <ItemIcon id="silk_touch_addon" />
  <ItemIcon id="anchor_addon" />
  <ItemIcon id="efficiency_addon" />
</ItemGrid>

| Addon | Effect | Limit | Power use |
| --- | --- | --- | --- |
| Speed Addon | +10% flight speed each | frame limit only | +10% flight cost |
| Stack Addon | +1 stack (64 items) per [Transport](modules.md) trip each | up to 9 stacks per trip | +5% flight cost |
| Battery Addon | +25% of the frame's battery capacity each | frame limit only | +5% flight cost |
| Fortune Addon | +1 Fortune level for Quarry and Harvester drops | max 3 | +25% energy per job |
| Silk Touch Addon | Silk Touch for Quarry and Harvester drops | max 1 | +50% energy per job |
| Anchor Addon | Its dock keeps the drone's chunks loaded | max 1 | 4 FE/t per chunk, from the dock |
| Efficiency Addon | Saves power in flight and on every job | max 3 | -10% flight cost and -10% energy per job each |

**Fortune and Silk Touch cannot be combined**, just like the enchantments.

Without Stack Addons a Transport drone carries only **16 items** per trip.

The percentages add up: see how the total is worked out on the [Using Drones](drones.md) page.

## Anchor Addon

A drone only works while its chunks are loaded: walk away from a far dock and it stops. With an
**Anchor Addon** its [Drone Dock](drone_dock.md) keeps loaded:

- the dock's own chunk;
- the chunks around the drone, following it as it flies;
- the two ends of its transport route;
- its work area.

That is up to **25 chunks** per dock. The dock pays **4 FE/t for each chunk** out of its battery:
when the battery runs dry, or the dock is paused, it lets the chunks go. It also works on a Builder
Swarm Dock, for its anchored Mini Drones. Server owners can change the cost and the number of chunks,
or switch anchoring off, in the `drones` section of the CyberFarm config.

## In machines

The Speed, Efficiency and Battery Addons also fit ten machines: the [Biomass Press](biomass_press.md),
[Bio-Digester](bio_digester.md), [Acid Synthesizer](acid_synthesizer.md), [Coolant Mixer](coolant_mixer.md),
[Electrolyzer](aluminum.md), [Pyrolysis Oven](carbon_fiber.md), [Fabricator](circuits.md),
[Crystallizer](mech_core.md), [Waste Reprocessor](waste_reprocessor.md) and [Drone Assembler](drone_assembler.md).
Each has **3 addon slots** on a tab on the right edge of its GUI. A slot holds a stack of one addon
type, and pipes and hoppers never reach them. The Water Pump has no GUI and takes none.

| Addon | In a machine | Limit |
| --- | --- | --- |
| Speed Addon | +50% speed and +80% FE/t each | max 4 |
| Efficiency Addon | -20% FE per operation each | max 3 |
| Battery Addon | +100% energy buffer each | max 4 |

They stack: 4 Speed Addons make a machine **3 times as fast** for about 40% more FE per operation,
and two Efficiency Addons more than win that back. A faster machine also takes proportionally
more FE per tick from its cables.

## Recipes

<Row>
  <RecipeFor id="speed_addon" />
  <RecipeFor id="stack_addon" />
</Row>

<Row>
  <RecipeFor id="battery_addon" />
  <RecipeFor id="efficiency_addon" />
</Row>

<Row>
  <RecipeFor id="fortune_addon" />
  <RecipeFor id="silk_touch_addon" />
</Row>

<RecipeFor id="anchor_addon" />
