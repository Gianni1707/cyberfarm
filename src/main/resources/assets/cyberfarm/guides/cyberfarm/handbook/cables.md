---
navigation:
  title: Cables
  position: 4
  icon: cyberfarm:energy_conduit
---

# Cables

Cables carry power and fluids from one machine to another, so your machines no longer need to touch.
To move items, use a drone with a [Transport Module](modules.md) instead.

<ItemGrid>
  <ItemIcon id="energy_conduit" />
  <ItemIcon id="advanced_energy_conduit" />
  <ItemIcon id="elite_energy_conduit" />
  <ItemIcon id="gas_pipe" />
  <ItemIcon id="advanced_gas_pipe" />
  <ItemIcon id="elite_gas_pipe" />
  <ItemIcon id="liquid_pipe" />
  <ItemIcon id="advanced_liquid_pipe" />
  <ItemIcon id="elite_liquid_pipe" />
</ItemGrid>

## Kinds and tiers

There are three kinds of cable. All are glass tubes, and the lit sleeves near their ends tell them apart:

| Cable | Carries | Sleeve lights |
| --- | --- | --- |
| [Energy Conduit](energy_conduit.md) | FE | cyan |
| [Gas Pipe](gas_pipe.md) | gases: biogas, methane, Steam | green |
| [Liquid Pipe](liquid_pipe.md) | liquids: water, lava, coolants, acid | blue |

Each kind comes in **three tiers**: Basic, Advanced and Elite. The band on the sleeve shows the tier:
grey, **gold** or **violet**. You can see when something goes through a cable.

## Placing cables

- A cable **connects by itself** to cables of the same kind.
- It also connects to any block that takes what it carries, from CyberFarm or any other mod.
- Machines built with [Cyber Frames](cyber_frame.md) take cables, pipes and hoppers on **any of their frames**,
  not only on the machine block.
- Different kinds never connect to each other, so they can run side by side.
- All tiers of one kind join into one network.
- A network can be up to **1,024** cables long.
- Cables **store nothing**: what comes in goes straight to the blocks at the other end.

## Pulling out of a block

Generators and machines push what they make into the cables by themselves. Batteries, tanks and
other storage do not.

To take something **out** of a storage block, **right-click the cable's arm** that touches it.
A lit band wraps that end, and the cable now **pulls** from that block every tick, up to its tier's rate.
Right-click the arm again to stop.

The network never sends anything back into a block it pulls from, so a pulling arm is a one-way exit.
Use it to empty a [Liquid Tank or Gas Tank](fluid_tanks.md) or a
battery of another mod.

## Numbers

The **rate** is the most that can enter the network per tick **from each source**: each generator or
machine pushing in, or each block a cable pulls from. The cable touching the source sets it.

| Cable | Basic | Advanced | Elite |
| --- | --- | --- | --- |
| Energy Conduit | 2,000 FE/t | 16,000 FE/t | 128,000 FE/t |
| Gas Pipe | 1,000 mB/t | 4,000 mB/t | 16,000 mB/t |
| Liquid Pipe | 1,000 mB/t | 4,000 mB/t | 16,000 mB/t |

Because the rate depends only on the cable at the source, you can mix tiers in one network: put a
better cable right where a big producer connects. As a guide:

- **Basic** carries all the gas power and a Small Steam Turbine (2,000 FE/t).
- **Advanced** carries a Medium Steam Turbine (9,600 FE/t) or the steam of a Large reactor (up to 2,000 mB/t).
- **Elite** carries a Large Steam Turbine (40,000 FE/t) and joins several turbines into one line.

These numbers sit between Immersive Engineering's wires and Mekanism's cables.

## Upgrading

- 6 cables around two gold ingots and a Basic Circuit make **8 Advanced** cables.
- 6 Advanced cables around two diamonds and an Advanced Circuit make **8 Elite** cables.

To upgrade a line already in place, use the [Cable Swapper](cable_swapper.md): one right-click swaps
the whole line for the next tier.

Where a cable can't go, [Dish Transmission](dish_transmission.md) sends FE through the air.

## Pages

<SubPages icons={true} />
