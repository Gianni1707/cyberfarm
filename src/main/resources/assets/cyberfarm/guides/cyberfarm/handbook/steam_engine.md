---
navigation:
  title: Steam Engine
  parent: generators.md
  position: 7
  icon: cyberfarm:steam_engine
item_ids:
  - cyberfarm:steam_engine
  - cyberfarm:steam_engine_cylinder
  - cyberfarm:steam_engine_dynamo
---

# Steam Engine

A mid-game generator built in a row: an induction **boiler** burns Biogas or Methane into steam,
**1 to 4 cylinders** turn it into motion and a **dynamo** at the end turns the flywheel into FE.

<GameScene zoom={4} interactive={true}>
  <Block id="cyberfarm:steam_engine" x="0" y="0" z="0" p:facing="north" />
  <Block id="cyberfarm:steam_engine_cylinder" x="0" y="0" z="1" />
  <Block id="cyberfarm:steam_engine_cylinder" x="0" y="0" z="2" />
  <Block id="cyberfarm:steam_engine_dynamo" x="0" y="0" z="3" />
</GameScene>

## Building it

- Place the **Steam Engine** (the boiler). Its front faces you.
- Behind it, in a straight line, place **1 to 4 Steam Engine Cylinders**.
- Close the row with a **Steam Engine Dynamo**.

When the row is complete its blocks turn into **one machine**: a white induction boiler with glowing
orange coils and steam puffing from its top, a cylinder for every block with its piston pumping in a
glass sleeve, the steam line running over them, and the dynamo with copper windings and a spinning
flywheel. The faster the engine runs, the brighter the coils and windings glow.

The GUI shows a live side view of the engine, the cylinders found, the power made and the speed.

## Fuel and power

Pipe Biogas or Methane into the boiler. **Each cylinder** burns **15 mB/t**:

| Fuel | FE per mB | 1 cylinder | 4 cylinders |
| --- | --- | --- | --- |
| Biogas | 6 | 90 FE/t | 360 FE/t |
| Methane | 10 | 150 FE/t | 600 FE/t |

A full engine on methane burns 60 mB/t, the gas of two or three zombie chambers, and makes as much
as fifteen Combustion Generators; a [Fission Reactor](fission_reactor.md) still makes more.

The engine needs about **5 seconds** to reach full speed and slows down when the fuel runs out.
It stops burning fuel when its buffer (200,000 FE) is full.

The power comes out of the **dynamo**: connect [Energy Conduits](energy_conduit.md) or machines to it.
Pipes and cables joining the boiler or the dynamo get a collar with a cyan ring.

<Row>
  <RecipeFor id="steam_engine" />
  <RecipeFor id="steam_engine_cylinder" />
  <RecipeFor id="steam_engine_dynamo" />
</Row>
