---
navigation:
  title: Steam Engine
  parent: generators.md
  position: 8
  icon: cyberfarm:steam_engine
item_ids:
  - cyberfarm:steam_engine
---

# Steam Engine

A mid-game generator built in a row: an induction **boiler** burns Biogas or Methane into steam,
**three cylinders** turn it into motion and a **dynamo** at the end turns the flywheel into FE.

<GameScene zoom={4} interactive={true}>
  <Block id="cyberfarm:steam_engine" x="0" y="0" z="0" p:facing="north" />
  <Block id="cyberfarm:cyber_frame" x="0" y="0" z="1" />
  <Block id="cyberfarm:cyber_frame" x="0" y="0" z="2" />
  <Block id="cyberfarm:cyber_frame" x="0" y="0" z="3" />
  <Block id="cyberfarm:cyber_frame" x="0" y="0" z="4" />
</GameScene>

## Building it

- Place the **Steam Engine** (the boiler). Its front faces you. Alone, a right click sends out mini drones that project the frames where they go.
- Behind it, in a straight line on solid ground, place **4 Cyber Frames**.

A drone over a cell projects the frame it wants. An amber X means a block is in the way; a steel outline means that
frame already belongs to another machine. A second click calls the drones back.

The frames are taken in and the engine opens: rail plates run out from under the boiler one after another,
and on them the three cylinders and the dynamo compose themselves. Then its pistons pump between steel posts,
the steam line runs over them and the flywheel spins. The faster it runs, the brighter the boiler's coils glow.
Breaking a frame folds it all back and gives the frames back.

The GUI shows a live side view of the engine, the power made and the speed.

## Fuel and power

Pipe Biogas or Methane into the boiler, which holds 16,000 mB. It burns **45 mB/t**:

| Fuel | FE per mB | Power |
| --- | --- | --- |
| Biogas | 9 | 405 FE/t |
| Methane | 15 | 675 FE/t |

On methane it makes as much as about eleven Combustion Generators; a [Fission Reactor](fission_reactor.md)
still makes more.

The engine needs about **5 seconds** to reach full speed, burning the full 45 mB/t for less power meanwhile, and slows down when the fuel runs out.
It stops burning fuel when its buffer (200,000 FE) is full.

Both the gas and the power go through the **boiler**: connect pipes, [Energy Conduits](energy_conduit.md) or
machines to it, on any side but its back, where the row goes. A socket shows wherever one plugs in. The row
itself takes none.

<RecipeFor id="steam_engine" />
