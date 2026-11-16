---
navigation:
  title: Trinkets
  position: 7
  icon: cyberfarm:gravity_boots
item_ids:
  - cyberfarm:trinket_table
  - cyberfarm:gravity_boots
  - cyberfarm:neural_chip
  - cyberfarm:utility_belt
  - cyberfarm:energy_sphere
  - cyberfarm:advanced_energy_sphere
  - cyberfarm:elite_energy_sphere
  - cyberfarm:pocket_drone
  - cyberfarm:zoom_program
  - cyberfarm:night_vision_program
  - cyberfarm:machine_link_program
  - cyberfarm:geiger_hud_program
  - cyberfarm:taxi_call_program
  - cyberfarm:auto_feeder_program
  - cyberfarm:human_dock
  - cyberfarm:battery_bank
---

# Trinkets

Small pieces of tech you wear to make life easier: hover over the ground, see in the dark, zoom in,
read your machines from afar, call your Taxi drone, have your drops picked up for you.

<ItemGrid>
  <ItemIcon id="gravity_boots" />
  <ItemIcon id="neural_chip" />
  <ItemIcon id="utility_belt" />
  <ItemIcon id="energy_sphere" />
  <ItemIcon id="pocket_drone" />
</ItemGrid>

## Where you wear them

Open your inventory and click the **Trinkets** tab above it (on the creative inventory, down its right
side). It has three slots of their own: **Neck**, **Belt** and **Feet**. They are not armor slots, so
you keep your armor; trinkets give no armor and never wear out. Shift-click a trinket to put it on.
What you wear shows on your character.

With **Curios** installed, the trinkets go in three Curios slots of their own instead (CyberFarm's
Neck, Belt and Feet), never in the other mods' slots.

Trinkets are kept with keepInventory and dropped on death otherwise.

## Utility Belt

<ItemImage id="cyberfarm:utility_belt" scale="2" float="left" />

A leather belt with a mint enamel stripe, for the Belt slot. Press its key (**B**) to open its four
holders (with the sphere's and the drone's charge under them), and everything in them hangs on your belt:

- the **Energy Sphere**, the battery of all your trinkets;
- the [Multiblock Projector](multiblock_projector.md): **Y** takes it in hand, and puts it back;
- the [Survey Tool](drone_dock.md): **U** takes it in hand, and puts it back;
- the **Pocket Drone**: **K** sends it out and calls it back.
- the **Cable Swapper** (see [Cables](cables.md)): **I** brings it to hand and puts it back.

If your hand is full, what you hold moves to a free slot of your inventory.

### Energy Sphere

An orb of plasma holding **500,000 FE**. Every trinket runs on it; with no sphere, or an empty one,
they rest. Charge it on a **Human Dock** (below), in a Battery Bank's slot, in another mod's charger, or
**from afar**: a [Transmitter Dish](dish_transmission.md) tops up the sphere of the players in its reach: only the
network owner's if it is Private, the team's too if it is Team, everyone's if it is Public, at the module's power, losing 10% on the way.

Two more tiers, made at the Trinket Table from the tier below, which keeps its charge:

| Sphere | Holds | Takes up to |
| --- | --- | --- |
| Energy Sphere | 500,000 FE | 4,000 FE/t |
| Advanced Energy Sphere | 2,000,000 FE | 16,000 FE/t |
| Elite Energy Sphere | 8,000,000 FE | 64,000 FE/t |

Its plasma shows the tier on your belt: mint, emerald, violet.

| Trinket | Uses |
| --- | --- |
| Gravity Boots | 12 FE/t while you hover |
| Night Vision Program | 2 FE/t while it is on |
| Machine Link Program | 1 FE/t |
| Geiger HUD Program | 1 FE/t while it shows radiation |
| Auto-Feeder Program | 50 FE per meal |

A full sphere keeps the Gravity Boots up for about 40 minutes.

### Pocket Drone

Rides folded on your belt. Sent out, it opens its arms on the belt and flies up to wait over your shoulder, a small cousin of the Builder's mini
drones. When something lies on the ground within **8 blocks**, it flies over, picks it up and brings it
back into your inventory, one drop at a time. Items you threw away yourself are left alone for 5
seconds, and anything you have no room for stays where it is. Call it back mid-errand and what it holds comes straight to you. It has a
small battery of its own, using 1 FE/t in flight, which the sphere keeps topped up whether it flies
or not.

## Gravity Boots

<ItemImage id="cyberfarm:gravity_boots" scale="2" float="left" />

You hover over the ground, following the terrain like a hovercraft: steps and slabs disappear, and
you glide over water and lava without touching them. Press the **Gravity Boots height** key (H) to
pick **0.5, 1 or 2 blocks**, or off. You move **40% faster** while hovering; walking, sprinting and
jumping work as on foot, with a touch of drift, as on a little ice, when you stop or turn; **sneak to land**. Rings of light leave your soles
and sink to the ground under you.
While they work you take **no fall damage**: off a cliff, the field slows you down and catches you
at your height above the ground below.

## Neural Chip

<ItemImage id="cyberfarm:neural_chip" scale="2" float="left" />

An implant on the back of your neck that runs **all six programs** if you give it them. Make a program
at the Trinket Table, then put the chip and the programs in its grid together to install them.

<ItemGrid>
  <ItemIcon id="zoom_program" />
  <ItemIcon id="night_vision_program" />
  <ItemIcon id="machine_link_program" />
  <ItemIcon id="geiger_hud_program" />
  <ItemIcon id="taxi_call_program" />
  <ItemIcon id="auto_feeder_program" />
</ItemGrid>

| Program | What it does | Key |
| --- | --- | --- |
| Zoom | Hold the key to zoom; the mouse wheel sets 2x to 8x, and the mouse slows down as with a spyglass. Optics only: no FE needed | Z |
| Night Vision | See in the dark, on and off, with no flicker | N |
| Machine Link | Look at a CyberFarm machine from up to 24 blocks away to read its status | |
| Geiger HUD | The radiation level above the hotbar wherever there is any, no [Geiger Counter](hazmat_suit.md) in hand | |
| Taxi Call | Calls a [Taxi drone](modules.md): right-click it to ride to its dock's destination, or home to its dock if it has none. Untaken for 30 seconds, it flies home alone | J |
| Auto-Feeder | Eats from your inventory when you are hungry, the food that fills you most without waste; never food with side effects, nor stews | |

**Taxi Call and your dock.** Right-click a Drone Dock with a Neural Chip that has the Taxi Call program,
and the chip remembers it (without the program the dock just opens): the Taxi Call then calls that
dock's drone, however far. From more than **128 blocks** away it **warps** to a little way off and flies
the rest; trips and the way home that long fly a little, then warp too, with a loading screen while
you hang from the hook.
Without a remembered dock, the call goes to the nearest Taxi drone within 128 blocks. If none can come,
the message says why.

All the keys can be changed in Controls, under CyberFarm Trinkets.

## Human Dock

<BlockImage id="cyberfarm:human_dock" scale="3" float="left" />

The [Drone Dock](drone_dock.md)'s sibling, for you. Place it with a **Cyber Frame on top**: the frame
hides and the arch rises behind the pad like a tripod's legs, a clamp snapping shut at each joint; a
display slides out of its top showing the dock's charge. Then **step onto the pad** and it charges everything at once,
up to **4,000 FE/t**: the Energy Sphere and the Pocket Drone on your belt, and every item in your
inventory that holds FE, from any mod. It takes power from cables, or from a Battery Bank beside it
(with that side of the bank set to Out). It has no screen: its lights show it working.

<RecipeFor id="human_dock" />

## Battery Bank

<BlockImage id="cyberfarm:battery_bank" scale="3" float="left" />

A small FE store: **150,000 FE** a block. Blocks that fill a box, up to **3x3x3**, join into one bank
that fills and empties as one, up to 4,050,000 FE (a [Resonance Core](resonance_core.md) holds far
more). Its **display** shows at a glance the bank's charge and the FE/t coming in and going out, and a
gauge on each side fills as it charges. Joined blocks share **one display** across the whole front, and a
bigger one spells the numbers out and adds what the bank stores; the side gauges run its full height. Its GUI shows the whole bank's charge and the FE/t flowing in and out, and has a slot that charges an
item. Each side of the bank is set with the [Cable Swapper](cables.md): hold it to see every side
framed in its mode's colour, and right-click a side to switch it:

- **In** (green, the default): the side takes FE from cables and machines;
- **Out** (amber): the side gives FE to what touches it, such as a Human Dock, or to a cable;
- **Off** (grey): nothing connects there.

Set the side facing your Human Dock to **Out**.

<RecipeFor id="battery_bank" />

## Trinket Table

<BlockImage id="cyberfarm:trinket_table" scale="3" float="left" />

A **2x2** workstation standing upright: place the table, then **Cyber Frames** on its right (seen from
the front), above it and above that one. It opens, and its two robotic arms build what you ask.
It works like a **crafting table**, with no power: put one item in each slot of its 3x3 grid, in any
order, and take what it makes; its arms go to work for a moment. Trinkets and programs are made only
here. Put a Neural Chip and the programs it lacks in the grid together to install them.

<RecipeFor id="trinket_table" />
