---
navigation:
  title: Cables
  position: 3
  icon: cyberfarm:energy_conduit
---

# Cables

Cables carry power and fluids between your machines (to move items, use a drone with a
[Transport Module](modules.md)). Every kind **connects by itself** to
cables of the same kind and to any block that accepts what it carries, from CyberFarm or any other
mod. Each kind comes in **three tiers**, told apart by the rims of the sleeve: plain, **gold** and
**violet**.

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

| Cable | Carries | Basic | Advanced | Elite |
| --- | --- | --- | --- | --- |
| [Energy Conduit](energy_conduit.md) | FE | 2,000 FE/t | 16,000 FE/t | 128,000 FE/t |
| [Gas Pipe](gas_pipe.md) | gases: biogas, methane, Steam | 1,000 mB/t | 4,000 mB/t | 16,000 mB/t |
| [Liquid Pipe](liquid_pipe.md) | liquids: water, lava, coolants | 1,000 mB/t | 4,000 mB/t | 16,000 mB/t |

The rate is how much can enter the network **from each source**: a generator or tank pushing in. It is set by the pipe the source touches, so
tiers can be mixed in one network: put a better pipe right where a big producer connects.

As a guide, the numbers sit between Immersive Engineering's wires and Mekanism's cables:

- **Basic** carries all the gas power and a Small Steam Turbine (800 FE/t).
- **Advanced** carries a Large Steam Turbine (9,600 FE/t) or the steam of a Large reactor (2,400 mB/t).
- **Elite** joins several turbines into one line.

Cables **store nothing**: whatever comes in goes straight to the blocks on the other end. A
network can be up to 1,024 cables long. Different kinds never connect to each other, so they can
run side by side.

## Pulling out of a block

Generators and machines push what they make into the cables by themselves. Batteries, tanks and
other storage do not: to take something **out** of them, **right-click the cable's arm** that
touches the block. An orange collar appears and the cable **pulls** from that block every tick,
up to its tier's rate. Right-click it again to stop.

The network never sends anything back into a block it pulls from, so a pulling arm is a one-way
exit. Use it to empty a [Liquid Tank or Gas Tank](fluid_tanks.md), or a battery of another mod.

**Upgrading**: 8 cables around gold and redstone make 8 Advanced ones; 8 Advanced around diamonds
and a uranium ingot make 8 Elite ones.

## Pages

<SubPages icons={true} />
