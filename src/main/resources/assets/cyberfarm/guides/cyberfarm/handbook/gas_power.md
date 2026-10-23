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

| Gas | Made by | Gas Turbine | Steam Engine |
| --- | --- | --- | --- |
| Biogas | [Bio-Digester](bio_digester.md), about 6 mB/t | 3 FE per mB | 6 FE per mB |
| Methane | [Methane Extractor](methane_extractor.md), up to 25 mB/t | 8 FE per mB | 10 FE per mB |

The Gas Turbine is small and cheap; the [Steam Engine](steam_engine.md) is a bigger build that gets
more out of every mB.

## Gas buckets

Gases can be carried in buckets, held **upside down** since they are lighter than air:

- **Fill one**: right-click a machine or tank holding at least 1,000 mB of gas with an **empty
  bucket** (a Bio-Digester for Biogas, a Methane Outlet of the extraction chamber for Methane, a
  reactor port or turbine for Steam).
- **Empty it**: right-click a machine, a Reactor Port or a tank of any mod to pour the 1,000 mB in.
- A gas cannot be poured into the world: it would just float away.

<ItemGrid>
  <ItemIcon id="biogas_bucket" />
  <ItemIcon id="methane_bucket" />
  <ItemIcon id="steam_bucket" />
</ItemGrid>

Both gases are **standard fluids**: tanks, pipes and machines of other mods that handle fluids can
store and move them too. They are tagged `c:gaseous`, `c:biogas` and `c:methane`.

A typical setup: a Bio-Digester and the Methane Outlet of an extraction chamber push their gas
into [Gas Pipes](gas_pipe.md), which carry it to a [Gas Turbine](gas_turbine.md) or a
[Steam Engine](steam_engine.md); they push FE into [Energy Conduits](energy_conduit.md) that power
your drones' docks.

