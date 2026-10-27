---
navigation:
  title: Multiblock Projector
  parent: index.md
  position: 4
  icon: cyberfarm:multiblock_projector
item_ids:
  - cyberfarm:multiblock_projector
---

# Multiblock Projector

<ItemImage id="multiblock_projector" scale="4" float="left" />

Shows a multiblock **in the world** as ghost blocks: what to place and where, before you place a
single block.

<br clear="all" />

- **Right-click** to open the list of structures and see the blocks each one takes: the [Fission Reactor](fission_reactor.md) and the
  [Steam Turbine](steam_turbine.md) in all three sizes, the
  [Methane Extractor](methane_extractor.md) chamber, the four tiers of the
  [Resonance Core](resonance_core.md), the [Mech Hangar](mech_hangar.md) and the
  [Crystallizer](mech_core.md) chamber.
- Below the multiblocks the list has **schematics**, marked in cyan: ready-made buildings such as
  a Watch Tower, a Storage Shed (a small depot) and a Drone Pad. Any data pack can add more: save a building with a
  **Structure Block** and put the file in `data/<namespace>/structure/schematic/`. Its north side is
  its front, which turns towards you. Schematics up to 40 blocks a side are shown.
- The [tech village](tech_village.md)'s buildings join the list once you **learn** their
  **Blueprint Disc**. Pick an upgrade near its village and the projection locks itself over the
  building it upgrades, so it shows only what the upgrade adds.
- While you hold it, the projection **follows the block you look at**: the structure **stands on**
  the face you aim at, the controller on the side towards you, facing you, and the rest stretches
  away from you.
- **Sneak + right-click a block** to lock it there. Lock it on a controller you already placed and
  the projection lines up with it.
- **Sneak + right-click the air** to unlock it.
- **Right-click a [Drone Dock](drone_dock.md)** with a locked projection and a drone with a
  [Builder Module](modules.md) builds it for you.
- In **creative** mode, right-clicking a block **clears the site and builds** the structure right
  away (where it is locked, or where it is projected), with plain casings; swap in the ports you
  need.

Blocks still to place glow softly; **red** blocks are wrong, or in the way of a space that must
stay empty. Correct blocks are not drawn. Above the hotbar the projector counts what is left, and
says when the structure is complete. When any CyberFarm multiblock forms, a ring of light rises
through it from the bottom to the top.

<RecipeFor id="multiblock_projector" />
