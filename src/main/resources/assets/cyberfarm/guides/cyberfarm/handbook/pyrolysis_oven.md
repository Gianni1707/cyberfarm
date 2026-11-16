---
navigation:
  title: Pyrolysis Oven
  parent: materials.md
  position: 6
  icon: cyberfarm:pyrolysis_oven
item_ids:
  - cyberfarm:pyrolysis_oven
---

# Pyrolysis Oven

<BlockImage id="cyberfarm:pyrolysis_oven" scale="4" float="left" />

The Pyrolysis Oven makes **Carbon Fiber**, the material of the Advanced and Elite Drone Frames.
It splits **Methane** into carbon and hydrogen (CH4 -> C + 2H2) and draws the carbon onto **String**.

<br clear="all" />

## Building it

The oven is a small multiblock. It needs one [Cyber Frame](cyber_frame.md) on its **left**, as
you face its front.

1. Place the Pyrolysis Oven.
2. Right-click it: a drone shows where the frame goes.
3. Place the Cyber Frame there. The machine opens into one long oven.

It works only once assembled. Breaking the frame folds it back.

## Using it

- Put **String** in its slot. Carbon Fiber comes out in the output slot.
- Pipe **Methane** in with a [Gas Pipe](gas_pipe.md), from a [Methane Extractor](methane_extractor.md)
  or a [gas tank](fluid_tanks.md). Pipes plug into its front, its back or its free side: its top is full.
- Give it FE through the same sides.
- A hopper underneath takes out only the Carbon Fiber, never the String.
- The tab beside its GUI takes Speed, Efficiency and Battery [addons](addons.md).

You can see it work: a shuttle runs along the glowing slit once per fiber, drawing the fiber
behind it. If the shuttle stops, so has the machine.

## Numbers

| What | Value |
| --- | --- |
| Recipe | 250 mB Methane + 1 String -> 1 Carbon Fiber |
| Time | 4 seconds per fiber |
| Power | 120 FE/t (9,600 FE per fiber) |
| Methane tank | 8,000 mB |
| Energy buffer | 20,000 FE |

See [Carbon Fiber](carbon_fiber.md) for where the fiber goes.

<RecipeFor id="pyrolysis_oven" />
