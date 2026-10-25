---
navigation:
  title: Getting Started
  parent: index.md
  position: 1
  icon: cyberfarm:drone_core
item_ids:
  - cyberfarm:drone_core
  - cyberfarm:rotor
  - cyberfarm:battery_cell
---

# Getting Started

Your first farming drone needs iron, copper, redstone and a little gold. Here is the whole path.

The **CyberFarm** tab of the advancements screen (press L) shows where to go next: drones,
power, and nuclear power up to the mech.

## 1. Craft the components

Every CyberFarm machine and drone is built around the <ItemLink id="drone_core" />.
Frames also need <ItemLink id="rotor" />s and a <ItemLink id="battery_cell" />.

<Row>
  <RecipeFor id="drone_core" />
  <RecipeFor id="rotor" />
  <RecipeFor id="battery_cell" />
</Row>

## 2. Make power

Place a <ItemLink id="combustion_generator" /> and feed it coal or any other furnace fuel.
It pushes FE into every machine touching it, or into <ItemLink id="energy_conduit" />s that carry
it further.

## 3. Build the drone

Place a <ItemLink id="drone_assembler" /> next to the generator. Put a
<ItemLink id="basic_frame" /> and a <ItemLink id="harvester_module" /> inside, then press **Assemble**.

## 4. Give it a home

Place a <ItemLink id="drone_dock" /> in the middle of your farm, also next to a power source
(or connect it with cables from another mod). Right-click the dock with the drone to deploy it.

By default the drone works a **9x9** area centred on the dock. Use the <ItemLink id="survey_tool" />
to choose a different area: **sneak + scroll** (or right-click the air) to its **Area** mode, click
the dock, then two opposite corners. The tool tells you each step above the hotbar.

## 5. Collect the harvest

The drone brings everything back to the dock. Open the dock to take the items, or pull them out
with a hopper or any item pipe.
