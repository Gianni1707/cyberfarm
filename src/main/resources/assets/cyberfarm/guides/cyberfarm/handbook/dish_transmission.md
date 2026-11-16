---
navigation:
  title: Dish Transmission
  parent: cables.md
  position: 6
  icon: cyberfarm:transmitter_dish
item_ids:
  - cyberfarm:transmitter_dish
  - cyberfarm:receiver_dish
  - cyberfarm:range_module_i
  - cyberfarm:range_module_ii
  - cyberfarm:range_module_iii
  - cyberfarm:infinity_range_module
---

# Dish Transmission

<ItemGrid>
  <ItemIcon id="transmitter_dish" />
  <ItemIcon id="receiver_dish" />
  <ItemIcon id="range_module_i" />
  <ItemIcon id="range_module_ii" />
  <ItemIcon id="range_module_iii" />
  <ItemIcon id="infinity_range_module" />
</ItemGrid>

Dishes send **FE through the air**, to places a cable can't reach: across a valley, to an outpost, even
into another dimension. A **Transmitter Dish** sends, a **Receiver Dish** receives, and a **Range Module**
in the transmitter sets how far and how much.

There is no beam to see. A Transmitter Dish is working when it turns to a receiver and **holds still**
while it sends. A Receiver Dish is getting power when the rings of its dish **light up** from the rim
to the centre.

## Transmitter Dish

### Building

- It is a small multiblock. Place it with **three [Cyber Frames](cyber_frame.md)**: on its right as you face it, behind
  it, and in the corner between. The dish unfolds on its turret over a 2x2 base.
- Placed alone, a right-click sends out drones that show where the frames go.
- Feed it FE with [Energy Conduits](energy_conduit.md) on the dish block.

### Using it

Each Transmitter Dish runs **one network**. Its GUI has two tabs on the left of the network panel:

- **Network**: the network's **name**, its **colour** (which lights the indicator on both dishes) and
  its access.
- **Manage**: switches charging players' Energy Spheres on or off (owner only, see below).

Access decides who may link a receiver:

| Access | Who can link |
| --- | --- |
| Private (new networks) | only you |
| Team | you and the players on your team |
| Public | anyone |

A team is a vanilla `/team`, an FTB Teams team or an Open Parties and Claims party. Only you and your
team can change the network's name, colour and access.

### How it sends

- It serves up to **4 receivers in turn**: it turns to one (1 second), sends for 4 seconds, then moves
  to the next.
- The signal passes through blocks, but the chunks of both dishes must be loaded.
- A tenth of the FE is **lost on the way**.
- With a receiver in another dimension, both dishes point **straight up at the sky**.
- A receiver the transmitter no longer reaches (after a swap to a weaker Range Module, say) loses its
  seat on the network after **30 seconds**, so swapping a module is safe.

## Receiver Dish

- It is a small multiblock two blocks tall: put **a Cyber Frame on top** of it and its dish unfolds.
- Its GUI lists the networks it can reach, with their colour, distance (or dimension) and seats taken,
  for example "2/4". **Click** one to link. A full network is greyed out. **Disconnect** leaves it.
- The GUI also shows the FE/t arriving.
- It gives what it receives to the blocks and conduits around it.

## Range Modules

The Transmitter Dish has **one module slot**. The module sets how far it reaches and how much FE per
tick it sends in all, shared among its receivers.

| Module | Range | FE/t sent |
| --- | --- | --- |
| None | 32 blocks | 2,000 |
| Range Module I | 128 blocks | 8,000 |
| Range Module II | 512 blocks | 32,000 |
| Range Module III | 2,048 blocks | 128,000 |
| Infinity Range Module | Unlimited, into other dimensions | Unlimited |

## Charging your Energy Sphere

A Transmitter Dish also charges the [Energy Sphere](energy_sphere.md) on your [Utility Belt](utility_belt.md) from afar,
for players in its reach:

- on a Private network, only its owner;
- on a Team network, the team too;
- on a Public network, everyone.

It sends up to the module's power, losing 10% on the way. The players it charges take their turn with
the receivers, so now and then the dish turns to follow you. The owner can switch this off in the
**Manage** tab.

## Numbers

| Dish | Stores |
| --- | --- |
| Transmitter Dish | 10 times its module's FE/t (at least 20,000 FE; 1,000,000,000 FE with the Infinity Range Module) |
| Receiver Dish | 2,000,000 FE |

The Transmitter Dish takes in as much FE per tick as its module lets it send.

<RecipeFor id="transmitter_dish" />

<RecipeFor id="receiver_dish" />

<RecipeFor id="range_module_i" />

<RecipeFor id="range_module_ii" />

<RecipeFor id="range_module_iii" />

<RecipeFor id="infinity_range_module" />
