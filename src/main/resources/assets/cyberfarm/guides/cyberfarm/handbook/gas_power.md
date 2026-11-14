---
navigation:
  title: Gas Power
  parent: generators.md
  position: 2
  icon: cyberfarm:gas_turbine
item_ids:
  - cyberfarm:biogas_bucket
  - cyberfarm:methane_bucket
  - cyberfarm:steam_bucket
---

# Gas Power

The second step of CyberFarm power: **gases**. Organic waste, pressed into Biomass, becomes **Biogas**, mobs shut in an
extraction chamber give off **Methane**, and a **Gas Turbine** burns both into FE.

<ItemGrid>
  <ItemIcon id="biomass_press" />
  <ItemIcon id="bio_digester" />
  <ItemIcon id="methane_extractor" />
  <ItemIcon id="gas_turbine" />
  <ItemIcon id="gas_pipe" />
</ItemGrid>

| Gas | Made by | Gas Turbine | Steam Engine |
| --- | --- | --- | --- |
| Biogas | [Bio-Digester](bio_digester.md), 10 mB/t | 5 FE per mB | 9 FE per mB |
| Methane | [Methane Extractor](methane_extractor.md), up to 30 mB/t | 12 FE per mB | 15 FE per mB |

How long the gas lasts and what it gives (a bucket is 1,000 mB):

| | Gas Turbine (burns 20 mB/t) | Steam Engine (burns 45 mB/t) |
| --- | --- | --- |
| 1,000 mB of Biogas | 5,000 FE, 2.5 seconds at 100 FE/t | 9,000 FE, about 1.1 seconds at 405 FE/t |
| 1,000 mB of Methane | 12,000 FE, 2.5 seconds at 240 FE/t | 15,000 FE, about 1.1 seconds at 675 FE/t |
| One Biomass (500 mB of Biogas) | 2,500 FE | 4,500 FE |
| One zombie in the chamber (180,000 mB) | 2,160,000 FE | 2,700,000 FE |

Gas is made slowly and burned fast: what counts is the flow. One Gas Turbine at full power needs 20 mB/t,
that is **two Bio-Digesters** or two thirds of a zombie chamber; a Steam Engine needs 45 mB/t.

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

The Biomass Press and the Bio-Digester draw 8 FE/t each, so the first power has to come from elsewhere, such as a
[Combustion Generator](combustion_generator.md). At full speed a digester and one press cost 16 FE/t together, while their gas gives 50 FE/t in a Gas Turbine, or 90 in a Steam Engine.

A typical setup: a Bio-Digester and the Methane Outlet of an extraction chamber push their gas
into [Gas Pipes](gas_pipe.md), which carry it to a [Gas Turbine](gas_turbine.md) or a
[Steam Engine](steam_engine.md); they push FE into [Energy Conduits](energy_conduit.md) that power
your drones' docks.

