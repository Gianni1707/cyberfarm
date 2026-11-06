---
navigation:
  title: Gas Turbine
  parent: generators.md
  position: 6
  icon: cyberfarm:gas_turbine
item_ids:
  - cyberfarm:gas_turbine
---

# Gas Turbine

<BlockImage id="cyberfarm:gas_turbine" scale="4" float="left" />

Burns **Biogas** or **Methane** into FE, 20 mB per tick.

It is a small multiblock: **place a Cyber Frame in front of it and one behind**. **Right-click** the machine
and sparks show the spots (hold sneak to place a frame against the machine itself). With both there it assembles: the
duct and the nozzle slide out of the drum and the rotor blooms. It does not work until then, so a turbine placed before this
change waits for its frames, with its gas kept; move any pipe that sits where a frame goes.

<br clear="all" />

| Fuel | Energy | Output |
| --- | --- | --- |
| Biogas | 3 FE per mB | 60 FE/t |
| Methane | 8 FE per mB | 160 FE/t |

- Holds **16,000 mB** of gas and **50,000 FE**. It only burns gas when its energy buffer has
  room, so no gas is wasted.
- Like the [Combustion Generator](combustion_generator.md), it **pushes power** into adjacent
  machines and [Energy Conduits](energy_conduit.md), up to 400 FE/t per side.
- Feed it with [Gas Pipes](gas_pipe.md) or any other mod's fluid pipes. Pipes and conduits connect at the three mint
  sockets of the machine block, on either side or on top, whichever you prefer.
- The rotor turns as fast as it makes power, so methane spins it faster than biogas. The slits on its flanks show
  the gas left. Breaking a frame folds it back and stops it; put the frame back and it opens again.

<RecipeFor id="gas_turbine" />
