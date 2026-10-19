---
navigation:
  title: Gas Turbine
  parent: generators.md
  position: 5
  icon: cyberfarm:gas_turbine
item_ids:
  - cyberfarm:gas_turbine
---

# Gas Turbine

<BlockImage id="cyberfarm:gas_turbine" scale="4" float="left" />

Burns **Biogas** or **Methane** into FE, 10 mB per tick.

<br clear="all" />

| Fuel | Energy | Output |
| --- | --- | --- |
| Biogas | 3 FE per mB | 30 FE/t |
| Methane | 8 FE per mB | 80 FE/t |

- Holds **8,000 mB** of gas and **50,000 FE**. It only burns gas when its energy buffer has
  room, so no gas is wasted.
- Like the [Combustion Generator](combustion_generator.md), it **pushes power** into adjacent
  machines and [Energy Conduits](energy_conduit.md), up to 400 FE/t per side.
- Feed it with [Gas Pipes](gas_pipe.md) or any other mod's fluid pipes.

<RecipeFor id="gas_turbine" />
