---
navigation:
  title: Steam Turbine
  parent: nuclear.md
  position: 4
  icon: cyberfarm:turbine_controller
item_ids:
  - cyberfarm:turbine_controller
  - cyberfarm:turbine_casing
  - cyberfarm:turbine_port
---

# Steam Turbine

A horizontal hall that turns **Steam** into FE, in **three sizes**. Steam piped into its ports spins
up a big rotor; the rotor drives the generator. The scene shows the Small turbine.

<GameScene zoom={4} interactive={true}>
  <Block id="cyberfarm:turbine_casing" x="0" y="0" z="0" />
  <Block id="cyberfarm:turbine_casing" x="1" y="0" z="0" />
  <Block id="cyberfarm:turbine_casing" x="2" y="0" z="0" />
  <Block id="cyberfarm:turbine_casing" x="0" y="1" z="0" />
  <Block id="cyberfarm:turbine_controller" x="1" y="1" z="0" p:facing="north" />
  <Block id="cyberfarm:turbine_casing" x="2" y="1" z="0" />
  <Block id="cyberfarm:turbine_casing" x="0" y="2" z="0" />
  <Block id="cyberfarm:turbine_casing" x="1" y="2" z="0" />
  <Block id="cyberfarm:turbine_casing" x="2" y="2" z="0" />
  <Block id="cyberfarm:turbine_casing" x="0" y="0" z="1" />
  <Block id="cyberfarm:turbine_casing" x="1" y="0" z="1" />
  <Block id="cyberfarm:turbine_casing" x="2" y="0" z="1" />
  <Block id="cyberfarm:turbine_casing" x="0" y="1" z="1" />
  <Block id="cyberfarm:turbine_casing" x="2" y="1" z="1" />
  <Block id="cyberfarm:turbine_casing" x="0" y="2" z="1" />
  <Block id="cyberfarm:turbine_port" x="1" y="2" z="1" />
  <Block id="cyberfarm:turbine_casing" x="2" y="2" z="1" />
  <Block id="cyberfarm:turbine_casing" x="0" y="0" z="2" />
  <Block id="cyberfarm:turbine_casing" x="1" y="0" z="2" />
  <Block id="cyberfarm:turbine_casing" x="2" y="0" z="2" />
  <Block id="cyberfarm:turbine_casing" x="0" y="1" z="2" />
  <Block id="cyberfarm:turbine_port" x="2" y="1" z="2" />
  <Block id="cyberfarm:turbine_casing" x="0" y="2" z="2" />
  <Block id="cyberfarm:turbine_casing" x="1" y="2" z="2" />
  <Block id="cyberfarm:turbine_casing" x="2" y="2" z="2" />
  <Block id="cyberfarm:turbine_casing" x="0" y="0" z="3" />
  <Block id="cyberfarm:turbine_casing" x="1" y="0" z="3" />
  <Block id="cyberfarm:turbine_casing" x="2" y="0" z="3" />
  <Block id="cyberfarm:turbine_casing" x="0" y="1" z="3" />
  <Block id="cyberfarm:turbine_casing" x="1" y="1" z="3" />
  <Block id="cyberfarm:turbine_casing" x="2" y="1" z="3" />
  <Block id="cyberfarm:turbine_casing" x="0" y="2" z="3" />
  <Block id="cyberfarm:turbine_casing" x="1" y="2" z="3" />
  <Block id="cyberfarm:turbine_casing" x="2" y="2" z="3" />
</GameScene>

## Building the hall

| Size | Cross-section x length | Steam at full speed | FE per mB | Max output |
| --- | --- | --- | --- | --- |
| Small | 3x3 x 4 | 200 mB/t | 10 | 2,000 FE/t |
| Medium | 5x5 x 6 | 600 mB/t | 16 | 9,600 FE/t |
| Large | 7x7 x 8 | 1,600 mB/t | 25 | 40,000 FE/t |

- The **front and back** are full squares; in between only the **outer ring** is shell, and the
  inside must be **empty**: that is where the rotor spins.
- Use <ItemLink id="turbine_casing" />s for the shell and put the <ItemLink id="turbine_controller" />
  **in the middle of the front**, facing out. The hall runs straight back from it.
- Swap any shell blocks for <ItemLink id="turbine_port" />s: steam goes in, FE comes out. One port
  for a steam pipe and one for an [Energy Conduit](energy_conduit.md) is enough; the controller
  itself also gives power to cables touching it. Medium and Large turbines make more than a basic
  conduit carries: use **Advanced** conduits (16,000 FE/t) at a Medium one's ports and **Elite** ones (128,000 FE/t) at a Large one's.
  A Large one also takes more steam than a basic [Gas Pipe](gas_pipe.md) carries (1,000 mB/t): use an **Advanced** one.

Complete, the blocks disappear into one faceted **enamel and mint octagonal hull**: the controller becomes a
panel on the front, every port a glowing hatch, and at the back you can see the **rotor**. A
[Multiblock Projector](multiblock_projector.md) shows the layout of any size in the world.
Right-click the controller while it is unfinished and bigger drones fly out, each projecting a group of the blocks still to place; an amber X marks a block in the way. They start from the size you have built the most of (the Small one if none); each further click shows the next size, and after the Large one they come back.

## Spinning up

The rotor does not start at full speed: it **spins up** as long as there is steam, faster for small
turbines than for big ones (5, 7 or 10 seconds by size), and **spins down** the same way when the steam
runs out. The faster it turns, the more steam it uses and the more FE it makes. The rotor also stops
when the energy has nowhere to go, so no steam is wasted.

The GUI shows the steam tank (32,000 mB), the output, the rotor speed and the stored energy; hover the readout for the size's ratings. Right-
click any block of the formed hall to open it.

## How many turbines?

A reactor makes 1 mB of steam for each unit of heat: a Medium tower with 4 rods at 300%, cooled with
Coolant, makes 600 mB/t, just what a Medium turbine uses. If the turbines cannot keep up, the reactor's steam tank
fills up and its core overheats.

Steam from other mods works too (fluids tagged `c:steam`).

<RecipeFor id="turbine_casing" />

<RecipeFor id="turbine_port" />

<RecipeFor id="turbine_controller" />
