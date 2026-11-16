---
navigation:
  title: Trinkets
  position: 7
  icon: cyberfarm:gravity_boots
---

# Trinkets

Trinkets are small pieces of tech you wear. They let you hover over the ground, see in the dark, zoom in,
read your machines from afar, call your Taxi drone and have your drops picked up for you.

<ItemGrid>
  <ItemIcon id="gravity_boots" />
  <ItemIcon id="neural_chip" />
  <ItemIcon id="utility_belt" />
  <ItemIcon id="energy_sphere" />
  <ItemIcon id="pocket_drone" />
</ItemGrid>

## Getting started

1. Build a [Trinket Table](trinket_table.md). Every trinket and program is made there.
2. Make a [Utility Belt](utility_belt.md) and an [Energy Sphere](energy_sphere.md), and put the sphere in the belt.
   The sphere is the battery of all your trinkets.
3. Make the trinkets you want and put them on.
4. Recharge the sphere on a [Human Dock](human_dock.md).

## Where you wear them

Open your inventory and click the **Trinkets** tab above it (in the creative inventory, it is down the
right side). The tab has three slots of its own: **Neck**, **Belt** and **Feet**. Shift-click a trinket to
put it on. What you wear shows on your character.

These are not armor slots, so you keep your armor. Trinkets give no armor and never wear out.

With **Curios** installed, the trinkets go in three Curios slots of their own instead (CyberFarm's Neck,
Belt and Feet), never in the other mods' slots.

## When you die

Your trinkets are **kept** with keepInventory on. Otherwise they **drop** where you died, like the rest of
your inventory. With Curios installed, Curios' own rules decide.

## Every trinket

| Trinket | Slot | What it does | Key |
| --- | --- | --- | --- |
| [Utility Belt](utility_belt.md) | Belt | Holds the Energy Sphere, your tools and the Pocket Drone | B (open) |
| [Energy Sphere](energy_sphere.md) | in the belt | Powers every trinket; charges the FE items you carry | |
| [Pocket Drone](pocket_drone.md) | in the belt | Picks up drops around you | K |
| [Gravity Boots](gravity_boots.md) | Feet | Hover over the ground, no fall damage | H |
| [Neural Chip](neural_chip.md) | Neck | Runs up to six programs: zoom, night vision and more | Z, N, J |

You can change every key in Controls, under **CyberFarm Trinkets**.

## Power use

Every trinket runs on the Energy Sphere in your belt. With no sphere, or an empty one, they stop. Only the
Zoom program works without power.

| Trinket | Uses |
| --- | --- |
| Gravity Boots | 12 FE/t while you hover |
| Night Vision Program | 2 FE/t while it is on |
| Machine Link Program | 1 FE/t |
| Geiger HUD Program | 1 FE/t while it shows radiation |
| Auto-Feeder Program | 50 FE per meal |
| Pocket Drone | 1 FE/t in flight, from its own battery; the sphere refills it at up to 10 FE/t |

To store FE for your trinkets in bulk, see the [Battery Bank](battery_bank.md).

## All trinket pages

<SubPages icons={true} />
