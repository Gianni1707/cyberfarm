---
navigation:
  title: Steam Turbine
  parent: nuclear.md
  position: 6
  icon: cyberfarm:turbine_controller
item_ids:
  - cyberfarm:turbine_controller
  - cyberfarm:turbine_casing
  - cyberfarm:turbine_port
---

# Steam Turbine

The Steam Turbine turns **Steam** into FE. It is how a [Fission Reactor](fission_reactor.md) makes
power: steam piped in spins a big rotor, and the rotor drives the generator.

It is a horizontal hall in **three sizes**, built from <ItemLink id="turbine_casing" />s, a few
<ItemLink id="turbine_port" />s and one <ItemLink id="turbine_controller" />. The scene shows the
Small turbine.

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

| Size | Cross-section x length |
| --- | --- |
| Small | 3x3 x 4 |
| Medium | 5x5 x 6 |
| Large | 7x7 x 8 |

1. Build the **front and back** as full squares of <ItemLink id="turbine_casing" />s.
2. In between, build only the **outer ring**. The inside must be **empty**: the rotor spins there.
3. Put the <ItemLink id="turbine_controller" /> **in the middle of the front**, facing out. The
   hall runs straight back from it.
4. Swap shell blocks for <ItemLink id="turbine_port" />s: steam goes in, FE comes out. One port for
   a steam pipe and one for an [Energy Conduit](energy_conduit.md) are enough. The controller itself
   also gives power to cables touching it.
   On the outer ring, ports go only **in the middle of each side**: the hall is drawn with diagonal
   faces towards its edges, and a pipe could not reach a port there. On a 3x3 or 5x5 cross-section that is
   the centre block of each side, on 7x7 the centre block and the one either side of it. A port
   nearer an edge leaves the turbine **incomplete**. Inside the end caps, ports go anywhere.

Bigger turbines need bigger cables and pipes:

- Medium: **Advanced** conduits (16,000 FE/t).
- Large: **Elite** conduits (128,000 FE/t), and an **Advanced** [Gas Pipe](gas_pipe.md) for the
  steam (a basic one carries 1,000 mB/t).

**Help with the layout**: a [Multiblock Projector](multiblock_projector.md) shows any size in the
world. You can also **right-click the unfinished controller**: drones fly out, each projecting a
group of the blocks still to place, and an amber X marks a block in the way. They start from the
size you have built the most of (Small if none). Each further click shows the next size; after
Large they come back to Small.

Complete, the blocks become one faceted **enamel and mint octagonal hull**. The controller becomes a
panel on the front, every port a glowing hatch, and at the back you can see the **rotor**.
**Right-click any block** of the formed hall to open it.

## Using it

The GUI shows the steam tank (32,000 mB), the FE output, the rotor speed and the stored energy.
Hover the readout for the size's ratings.

The rotor does not start at full speed. It **spins up** while there is steam and **spins down**
when the steam runs out. The faster it turns, the more steam it uses and the more FE it makes. It
settles at the speed your steam supply can keep up.

The rotor also stops when its FE has nowhere to go, so no steam is wasted. Steam from other mods
works too (fluids tagged `c:steam`).

## Numbers

| Size | Steam at full speed | FE per mB | Max output | Spin-up time |
| --- | --- | --- | --- | --- |
| Small | 200 mB/t | 10 | 2,000 FE/t | 5 s |
| Medium | 600 mB/t | 16 | 9,600 FE/t | 7 s |
| Large | 1,600 mB/t | 25 | 40,000 FE/t | 10 s |

Bigger turbines get more FE out of every mB of steam.

## How many turbines?

A reactor makes 1 mB of steam for each unit of heat. Add turbines until together they take
**all** of its steam. For example, a Medium tower with 4 rods at 300%, cooled with Coolant, makes
600 mB/t: one Medium turbine (9,600 FE/t) or three Small ones (6,000 FE/t).

The [Fission Reactor](fission_reactor.md) page lists every tower's steam, with the turbines it needs
and the FE/t they give.

If the turbines cannot keep up, the reactor's steam tank fills up and its core **overheats**. The
reactor's **Dump excess steam** toggle prevents that by venting the extra steam, but vented steam
makes no FE.

<RecipeFor id="turbine_casing" />

<RecipeFor id="turbine_port" />

<RecipeFor id="turbine_controller" />
