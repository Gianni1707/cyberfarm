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

The Steam Engine is a mid-game generator that burns **Biogas** or **Methane**. It gets more FE out of every mB than the
[Gas Turbine](gas_turbine.md): on methane it makes as much as about eleven [Combustion Generators](combustion_generator.md).

It is built in a row. An induction **boiler** (the Steam Engine block) burns the gas into steam,
**three cylinders** turn the steam into motion, and a **dynamo** at the end turns the flywheel into FE.

<GameScene zoom={4} interactive={true}>
  <Block id="cyberfarm:steam_engine" x="0" y="0" z="0" p:facing="north" />
  <Block id="cyberfarm:cyber_frame" x="0" y="0" z="1" />
  <Block id="cyberfarm:cyber_frame" x="0" y="0" z="2" />
  <Block id="cyberfarm:cyber_frame" x="0" y="0" z="3" />
  <Block id="cyberfarm:cyber_frame" x="0" y="0" z="4" />
</GameScene>

## Building it

1. Place the **Steam Engine**. Its front faces you.
2. Behind it, in a straight line on solid ground, place **4 [Cyber Frames](cyber_frame.md)**.

**Right-click** the engine while it is alone: mini drones fly out and project the frames where they go.

- A drone over a cell projects the frame it wants.
- An amber X means a block is in the way.
- A steel outline means that frame already belongs to another machine.
- A second click calls the drones back.

When the row is complete the frames are taken in and the engine opens. Rail plates run out from under the boiler,
and the three cylinders and the dynamo compose themselves on them. Breaking a frame folds it all back and gives the frames back.

## Using it

- Pipe Biogas or Methane into the **boiler**.
- Take the power from the boiler too, with [Energy Conduits](energy_conduit.md) or adjacent machines.
- Pipes and conduits connect on any side of the boiler but its back, where the row goes. A socket shows wherever one plugs in.
- The row itself takes no pipes or conduits.
- The GUI shows a live side view of the engine, the power made and the speed.
- While it runs, its pistons pump and the flywheel spins. The faster it runs, the brighter the boiler's coils glow.

## Numbers

| Fuel | FE per mB | Power |
| --- | --- | --- |
| Biogas | 9 | 405 FE/t |
| Methane | 15 | 675 FE/t |

| Stat | Value |
| --- | --- |
| Gas burned | 45 mB/t |
| Gas tank | 16,000 mB |
| Buffer | 200,000 FE |
| Time to full speed | about 5 seconds |

One zombie in a [Methane Extractor](methane_extractor.md) chamber (50 mB/t) keeps it at full power.

## Tips

- While it speeds up, it burns the full 45 mB/t but makes less power. It slows down when the fuel runs out.
- With its buffer nearly full, it burns only the gas for the power that still fits. It stops when the buffer is full.
- A [Fission Reactor](fission_reactor.md) still makes more power.

<RecipeFor id="steam_engine" />
