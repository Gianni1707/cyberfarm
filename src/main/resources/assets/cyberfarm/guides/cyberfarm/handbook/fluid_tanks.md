---
navigation:
  title: Liquid and Gas Tanks
  parent: cables.md
  position: 5
  icon: cyberfarm:liquid_tank
item_ids:
  - cyberfarm:liquid_tank
  - cyberfarm:gas_tank
---

# Liquid and Gas Tanks

<Row>
  <BlockImage id="cyberfarm:liquid_tank" scale="3" />
  <BlockImage id="cyberfarm:gas_tank" scale="3" />
</Row>

Tanks store fluids between the machines that make them and the machines that use them. A tank is a
handy buffer: it keeps the flow going when production dips.

There are two kinds. Each holds a single fluid:

- the **Liquid Tank** (a steel line around the glass, waves on its lid) takes liquids: water, lava,
  Coolant, Cryo Coolant, Acid and the liquids of other mods;
- the **Gas Tank** (a mint line around the glass, a cloud on its lid) takes gases: biogas, methane,
  Steam and the gases of other mods.

## Building bigger tanks

**Tanks grow.** Blocks of the same kind that fill a box join into one tank: a column, a row, a 2x2x2,
a 3x3x3, up to 16 blocks on a side. Each block adds **32 buckets**, and pipes and buckets see the
whole box as one tank.

- Tanks already holding different fluids don't join.
- A joined tank has one frame around its outside and one pane of glass on each side, however many
  blocks it has.

## Filling and emptying

- **Fill** a tank with the matching pipe: a [Liquid Pipe](liquid_pipe.md) or a [Gas Pipe](gas_pipe.md).
  It plugs into any free face of any block, top and bottom too. A collar (steel on the Liquid Tank,
  mint on the Gas Tank) shows where a pipe plugs in.
- Tanks are **passive**: they never push anything out. To **empty** one into a machine, right-click
  the arm of the pipe that touches it, so the pipe **pulls** from the tank. A lit band round the
  pipe's end shows it (see [Cables](cables.md)).
- **Buckets** work too: right-click a tank with a bucket to fill it or to take a bucket out. The Gas
  Tank takes gas buckets.

## Reading the level

- Through the glass you see the fluid standing up to its level, in its own colour. Gases look hazy.
- **Look at a tank** to see a panel over it with what is inside and how full it is, one panel for a
  whole joined tank.
- **Shift + right-click** a tank, with anything in hand, to read the exact amount.

## Numbers

| What | Amount |
| --- | --- |
| Per block | 32,000 mB (32 buckets) |
| Largest side of a joined tank | 16 blocks |

## Tips

- Put a tank between a [Methane Extractor](methane_extractor.md) and the Gas Turbines, or between a
  [Water Pump](water_pump.md) and a [Fission Reactor](fission_reactor.md).
- Breaking a block **loses what that block held**. The rest stays in the other blocks, which join up
  again where they can.
- A tank built across a chunk border gives and takes nothing while only part of it is loaded (far
  from every player, or at the edge of what a drone's Anchor Addon keeps loaded). It works again as
  soon as the rest is loaded.

<RecipeFor id="liquid_tank" />

<RecipeFor id="gas_tank" />
