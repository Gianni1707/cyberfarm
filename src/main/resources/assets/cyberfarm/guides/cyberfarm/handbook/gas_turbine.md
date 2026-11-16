---
navigation:
  title: Gas Turbine
  parent: generators.md
  position: 7
  icon: cyberfarm:gas_turbine
item_ids:
  - cyberfarm:gas_turbine
---

# Gas Turbine

<BlockImage id="cyberfarm:gas_turbine" scale="4" float="left" />

The Gas Turbine burns **Biogas** or **Methane** into FE. It is small and cheap, and it is the first generator that
runs without you shovelling fuel. Methane gives more than twice the power of Biogas.

<br clear="all" />

## Building it

- It is a small multiblock: place a [Cyber Frame](cyber_frame.md) **in front of it** and one **behind it**.
- **Right-click** the machine and mini drones fly out and project the frames where they go.
- Hold sneak to place a frame against the machine itself.
- With both frames there it assembles: the duct and the nozzle slide out of the drum and the rotor blooms.
- It does not work until then. Breaking a frame folds it back and stops it; put the frame back and it opens again.

## Using it

- Feed it with [Gas Pipes](gas_pipe.md) or any other mod's fluid pipes.
- Pipes and conduits connect at the three mint sockets of the machine block: on either side or on top.
- It **pushes power by itself** into adjacent machines and [Energy Conduits](energy_conduit.md).
- A conduit on one of its frames can also pull the power with its arm (see [Energy Conduit](energy_conduit.md)).
- The rotor turns as fast as it makes power, so methane spins it faster than biogas.
- The slits on its flanks show how much gas is left.

## Numbers

| Fuel | Energy | Output |
| --- | --- | --- |
| Biogas | 5 FE per mB | 100 FE/t |
| Methane | 12 FE per mB | 240 FE/t |

| Stat | Value |
| --- | --- |
| Gas burned | 20 mB/t |
| Gas tank | 16,000 mB |
| Buffer | 50,000 FE |
| Output | up to 400 FE/t per side |

**Two [Bio-Digesters](bio_digester.md)** keep it at full power on Biogas.

## Tips

- It only burns gas when its energy buffer has room, so no gas is wasted.
- A turbine from an older world waits for its frames, with its gas kept. Move any pipe that sits where a frame goes.
- For more FE out of every mB, build a [Steam Engine](steam_engine.md).

<RecipeFor id="gas_turbine" />
