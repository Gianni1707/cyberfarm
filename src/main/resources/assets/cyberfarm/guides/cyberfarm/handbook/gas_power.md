---
navigation:
  title: Gas Power
  parent: generators.md
  position: 2
  icon: cyberfarm:gas_turbine
---

# Gas Power

The second step of CyberFarm power: **gases**. Organic waste becomes **Biogas**, mobs shut in an
extraction chamber give off **Methane**, and a **Gas Turbine** burns both into FE.

<ItemGrid>
  <ItemIcon id="bio_digester" />
  <ItemIcon id="methane_extractor" />
  <ItemIcon id="gas_turbine" />
  <ItemIcon id="gas_pipe" />
</ItemGrid>

| Gas | Made by | Energy in a Gas Turbine |
| --- | --- | --- |
| Biogas | [Bio-Digester](bio_digester.md) | 3 FE per mB |
| Methane | [Methane Extractor](methane_extractor.md) | 8 FE per mB |

Both gases are **standard fluids**: tanks, pipes and machines of other mods that handle fluids can
store and move them too. They are tagged `c:gaseous`, `c:biogas` and `c:methane`.

A typical setup: a Bio-Digester and a Methane Extractor push their gas into
[Gas Pipes](gas_pipe.md), which carry it to a [Gas Turbine](gas_turbine.md); the turbine pushes
FE into [Energy Conduits](energy_conduit.md) that power your drones' docks.

