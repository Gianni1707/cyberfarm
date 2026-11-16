---
navigation:
  title: Neural Chip
  parent: trinkets.md
  position: 6
  icon: cyberfarm:neural_chip
item_ids:
  - cyberfarm:neural_chip
  - cyberfarm:zoom_program
  - cyberfarm:night_vision_program
  - cyberfarm:machine_link_program
  - cyberfarm:geiger_hud_program
  - cyberfarm:taxi_call_program
  - cyberfarm:auto_feeder_program
---

# Neural Chip

<ItemImage id="cyberfarm:neural_chip" scale="2" float="left" />

An implant on the back of your neck, worn in the **Neck** slot. On its own it does nothing: it runs
**programs**, small cartridges that each add one ability. One chip can run **all six** programs.

<ItemGrid>
  <ItemIcon id="zoom_program" />
  <ItemIcon id="night_vision_program" />
  <ItemIcon id="machine_link_program" />
  <ItemIcon id="geiger_hud_program" />
  <ItemIcon id="taxi_call_program" />
  <ItemIcon id="auto_feeder_program" />
</ItemGrid>

## Installing programs

1. Make the chip and the programs at the [Trinket Table](trinket_table.md).
2. Put the chip and the programs you want to add in the table's grid together, and nothing else.
3. Take the chip: the programs are now installed.

A program the chip already has, or two copies of the same program, block the install. The chip's tooltip
lists its programs.

## The programs

| Program | What it does | Key |
| --- | --- | --- |
| Zoom | Hold the key to zoom. The mouse wheel sets **2x to 8x**, and the mouse slows down as with a spyglass | Z (hold) |
| Night Vision | See in the dark, switched on and off with its key, with no flicker | N |
| Machine Link | Look at a CyberFarm machine from up to **24 blocks** away to read its status | |
| Geiger HUD | Shows the radiation level above the hotbar wherever there is any, with no <ItemLink id="geiger_counter" /> in hand | |
| Taxi Call | Calls a Taxi drone to you (see below) | J |
| Auto-Feeder | Eats from your inventory when you are hungry (see below) | |

Programs with no key run on their own.

## Power

The programs run on the [Energy Sphere](energy_sphere.md) in your [Utility Belt](utility_belt.md). Only
Zoom works without it: it is optics only.

| Program | Uses |
| --- | --- |
| Zoom | nothing |
| Night Vision | 2 FE/t while it is on |
| Machine Link | 1 FE/t |
| Geiger HUD | 1 FE/t while it shows radiation |
| Taxi Call | nothing, but the sphere must have some charge |
| Auto-Feeder | 50 FE per meal |

## Taxi Call

Press **J** to call a Taxi drone (a drone with a [Taxi module](modules.md)).

- When it arrives, right-click it to ride to its dock's destination, or home to its dock if it has none.
- If you do not take it within **30 seconds**, it flies home alone.
- If no drone can come, a message tells you why.

**Linking a dock.** Right-click a [Drone Dock](drone_dock.md) while holding a chip that has the Taxi Call
program. The chip remembers that dock, and from then on the call goes to that dock's drone, however far.
Without the program, the dock just opens as usual.

- From more than **128 blocks** away, the drone **warps** to a little way off and flies the rest.
- Trips and the way home that long fly a little, then warp too, with a loading screen while you hang from
  the hook.
- Without a linked dock, the call goes to the nearest Taxi drone within **128 blocks**.

## Auto-Feeder

When you are hungry, it eats from your inventory: the food that fills you the most **without waste**.

- It never eats food with side effects, like rotten flesh or golden apples.
- It never eats food that leaves something behind, like stews or honey bottles.

## Tips

- You can change every key in Controls, under **CyberFarm Trinkets**.
