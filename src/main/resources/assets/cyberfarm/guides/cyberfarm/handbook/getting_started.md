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

Your first farming drone needs iron, copper, redstone, a little gold and **aluminum**. Here is
the whole path; [Materials and Progression](materials.md) shows where it leads.

The **CyberFarm** tab of the advancements screen (press L) shows where to go next: drones,
power, and nuclear power up to the mech.

## 1. Craft the components

Every drone is built around a <ItemLink id="drone_core" />, and every machine and module around
a <ItemLink id="basic_circuit" />: craft your first ones by hand. Frames also need
<ItemLink id="rotor" />s and a <ItemLink id="battery_cell" />.

<Row>
  <RecipeFor id="drone_core" />
  <RecipeFor id="basic_circuit" />
</Row>

<Row>
  <RecipeFor id="rotor" />
  <RecipeFor id="battery_cell" />
</Row>

## 2. Make power

Place a <ItemLink id="combustion_generator" /> and feed it coal or any other furnace fuel.
It pushes FE into every machine touching it, or into <ItemLink id="energy_conduit" />s that carry
it further.

**Look at any CyberFarm machine** to see a small panel beside the crosshair: whether it is
working, the FE it makes or uses each tick, how full its energy buffer is and how far its batch
has got. Working machines also show it: their front lights up and moves, their lights glow in the
dark, and they give off sparks, smoke, frost or bubbles depending on the machine.

## 3. Make aluminum

Drone frames are made of aluminum. Mine **Bauxite Ore**, smelt the Raw Bauxite into Alumina and
put it in an <ItemLink id="electrolyzer" /> next to the generator: it makes
<ItemLink id="aluminum_ingot" />s (see [Aluminum](aluminum.md)). A
<ItemLink id="fabricator" /> beside it makes Basic Circuits two at a time.

## 4. Build the drone

Place a <ItemLink id="drone_assembler" /> next to the generator. Put a
<ItemLink id="basic_frame" /> and a <ItemLink id="harvester_module" /> inside, then press **Assemble**.

<RecipeFor id="basic_frame" />

## 5. Give it a home

Place a <ItemLink id="drone_dock" /> in the middle of your farm, also next to a power source
(or connect it with cables from another mod). Right-click the dock with the drone to deploy it.

By default the drone works a **9x9** area centred on the dock. Use the <ItemLink id="survey_tool" />
to choose a different area: **sneak + scroll** (or right-click the air) to its **Area** mode, click
the dock, then two opposite corners. The tool tells you each step above the hotbar.

## 6. Collect the harvest

The drone brings everything back to the dock. Open the dock to take the items, or pull them out
with a hopper or any item pipe.
