---
navigation:
  title: Multiblock Projector
  parent: index.md
  position: 5
  icon: cyberfarm:multiblock_projector
item_ids:
  - cyberfarm:multiblock_projector
---

# Multiblock Projector

<ItemImage id="multiblock_projector" scale="4" float="left" />

Shows a multiblock **in the world**: what to place and where, before you place a single block.
Lock the projection in place to build it by hand, or let Builder drones build it for you.

<br clear="all" />

## Picking a structure

**Right-click** to open the list of structures and see the blocks each one takes:

- **Multiblocks**: the [Fission Reactor](fission_reactor.md) and the
  [Steam Turbine](steam_turbine.md) in all three sizes, the
  [Methane Extractor](methane_extractor.md) chamber, tiers 2 to 4 of the
  [Resonance Core](resonance_core.md) (tier 1 is the core alone), the [Mech Hangar](mech_hangar.md)
  and the [Crystallizer](mech_core.md) chamber.
- **Schematics**, marked in emerald, below the multiblocks: buildings saved with a
  **Structure Block**. Schematics up to 40 blocks a side are shown.
- The [tech village](tech_village.md)'s buildings, once you **learn** their **Blueprint Disc**.

Small machines that assemble from Cyber Frames are not in the list: click one and its own drones
show you where the frames go.

## Placing the projection

- While you hold it, the projection **follows the block you look at**. The structure **stands on**
  the face you aim at, the controller on the side towards you, facing you, and the rest stretches
  away from you.
- **Sneak + right-click a block** to lock it there. **Mini drones** fly out to hold the projection,
  each over a group of blocks.
- Lock it on a controller you already placed and the projection lines up with it.
- Pick a village upgrade near its village and the projection locks itself over the building it
  upgrades, so it shows only what the upgrade adds.
- **Sneak + right-click the air** to unlock it, and the drones fly home.

## Reading the projection

| You see | It means |
| --- | --- |
| Soft glow (following your gaze) | a block still to place |
| **Red** block (following your gaze) | a wrong block, or one in a space that must stay empty |
| Blocks in a cone of light (locked) | blocks still to place |
| **Amber X** (locked) | something in the way |
| Nothing drawn | the block is already correct |

Above the hotbar the projector counts what is left, and says when the structure is complete.
When any CyberFarm multiblock forms, a ring of light rises through it from the bottom to the top.

## Building it with drones

**Right-click a [Builder Swarm Dock](swarm_dock.md)** with a locked projection, and its Mini Drones
with a [Builder Module](modules.md) build it for you.

## Creative mode

In creative, right-clicking a block **clears the site and builds** the structure right away
(where it is locked, or where it is projected), with plain casings. Swap in the ports you need.

## Adding schematics

Any data pack can add schematics: put the Structure Block file in
`data/<namespace>/structure/schematic/`. Its north side is its front, which turns towards you.

<RecipeFor id="multiblock_projector" />
