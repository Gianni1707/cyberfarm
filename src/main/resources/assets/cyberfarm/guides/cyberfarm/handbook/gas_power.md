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

The second step of CyberFarm power is **gas**: fuel that your farm and your mob traps make for you. This page shows how the
gas machines fit together, and how to carry gases in **buckets**: Biogas, Methane and Steam Buckets.

<ItemGrid>
  <ItemIcon id="biogas_bucket" />
  <ItemIcon id="methane_bucket" />
  <ItemIcon id="steam_bucket" />
</ItemGrid>

## The two gases

- **Biogas**: a [Biomass Press](biomass_press.md) presses farm waste into Biomass, and a [Bio-Digester](bio_digester.md) turns it into Biogas.
- **Methane**: a mob shut in the chamber of a [Methane Extractor](methane_extractor.md) dissolves in Acid and gives off Methane.

Two generators burn both gases: the [Gas Turbine](gas_turbine.md), small and cheap, and the
[Steam Engine](steam_engine.md), a bigger build that gets more out of every mB.

<ItemGrid>
  <ItemIcon id="biomass_press" />
  <ItemIcon id="bio_digester" />
  <ItemIcon id="methane_extractor" />
  <ItemIcon id="gas_turbine" />
  <ItemIcon id="steam_engine" />
  <ItemIcon id="gas_pipe" />
</ItemGrid>

A typical setup: a Bio-Digester and the Methane Outlet of an extraction chamber push their gas
into [Gas Pipes](gas_pipe.md). The pipes carry it to a Gas Turbine or a Steam Engine. The generator pushes FE into
[Energy Conduits](energy_conduit.md) that power your drones' docks.

## Numbers

| Gas | Made by | Gas Turbine | Steam Engine |
| --- | --- | --- | --- |
| Biogas | Bio-Digester, 10 mB/t | 5 FE per mB | 9 FE per mB |
| Methane | Methane Extractor, up to 50 mB/t | 12 FE per mB | 15 FE per mB |

How long the gas lasts and what it gives (a bucket is 1,000 mB):

| | Gas Turbine (burns 20 mB/t) | Steam Engine (burns 45 mB/t) |
| --- | --- | --- |
| 1,000 mB of Biogas | 5,000 FE, 2.5 seconds at 100 FE/t | 9,000 FE, about 1.1 seconds at 405 FE/t |
| 1,000 mB of Methane | 12,000 FE, 2.5 seconds at 240 FE/t | 15,000 FE, about 1.1 seconds at 675 FE/t |
| One Biomass (500 mB of Biogas) | 2,500 FE | 4,500 FE |
| One zombie in the chamber (300,000 mB of Methane) | 3,600,000 FE | 4,500,000 FE |

Gas is made slowly and burned fast, so what counts is the **flow**:

| Generator at full power | Needs | That is |
| --- | --- | --- |
| Gas Turbine | 20 mB/t | two Bio-Digesters, or two fifths of a zombie chamber |
| Steam Engine | 45 mB/t | a little less than one zombie chamber |

The Biomass Press and the Bio-Digester draw **8 FE/t** each, so the first power has to come from elsewhere,
such as a [Combustion Generator](combustion_generator.md). At full speed a digester and one press cost 16 FE/t together.
Their gas gives 50 FE/t in a Gas Turbine, or 90 FE/t in a Steam Engine.

## Gas buckets

Gases can be carried in buckets. The buckets are held **upside down**, since gas is lighter than air.

- **Fill one**: right-click a machine or tank holding at least 1,000 mB of gas with an **empty bucket**.
  Biogas comes from a Bio-Digester, Methane from a Methane Outlet of the extraction chamber, Steam from a
  [Fission Reactor](fission_reactor.md) port or a [Steam Turbine](steam_turbine.md).
- **Empty it**: right-click a machine, a Reactor Port or a tank of any mod to pour the 1,000 mB in.
- A gas cannot be poured into the world: it would just float away.

## Tips

- Both gases are **standard fluids**. Tanks, pipes and machines of other mods that handle fluids can store and move them too.
- They are tagged `c:gaseous`, `c:biogas` and `c:methane`.
