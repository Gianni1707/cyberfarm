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

This page takes you from nothing to your first farming drone. You need iron, copper, redstone, a
little gold and **aluminum**. [Materials and Progression](materials.md) shows where it leads next.

The **CyberFarm** tab of the advancements screen (press **L**) also shows where to go next: drones,
power, and nuclear power up to the mech.

## 1. Craft the components

Every drone is built around a <ItemLink id="drone_core" />. Every machine and module is built around
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

Craft some [Cyber Frames](cyber_frame.md) too: most machines need one or more beside them before
they work.

## 2. Make power

Place a <ItemLink id="combustion_generator" /> and feed it coal or any other furnace fuel. It
pushes FE into every machine touching it, or into <ItemLink id="energy_conduit" />s that carry it
further.

**Look at any CyberFarm machine** to see a small panel floating just over it. It shows whether the
machine is working, the FE it makes or uses each tick, how full its energy buffer is and how far
its batch has got. With Jade or WTHIT installed, that mod's own panel takes over.

Working machines also show it: their front lights up and moves, their lights glow in the dark, and
some give off smoke, frost or bubbles.

## 3. Make aluminum

Drone frames are made of aluminum.

1. Mine **Bauxite Ore** and smelt the Raw Bauxite into **Alumina**.
2. Place an <ItemLink id="electrolyzer" /> with a Cyber Frame on its left, and power it.
3. Put the Alumina in: it makes <ItemLink id="aluminum_ingot" />s.

The Electrolyzer uses 80 FE/t and one Combustion Generator makes 60 FE/t, so use **two**
generators. See [Aluminum](aluminum.md) for the details.

A <ItemLink id="fabricator" /> beside it makes Basic Circuits two at a time, much cheaper than by hand.

## 4. Build the drone

Place a <ItemLink id="drone_assembler" /> next to the generator. Put a
<ItemLink id="basic_frame" /> and a <ItemLink id="harvester_module" /> inside, then press **Assemble**.

<RecipeFor id="basic_frame" />

## 5. Give it a home

Place a <ItemLink id="drone_dock" /> in the middle of your farm, also next to a power source (or
connect it with cables from another mod). Right-click the dock with the drone to deploy it.

By default the drone works a **9x9** area centred on the dock. To choose a different area, use the
<ItemLink id="survey_tool" />: click the dock, then two opposite corners. The tool reads what the
drone does and tells you each step above the hotbar.

**Look at a drone**, even from up to **32 blocks** away, to see a panel over it with what it is
doing, its charge and what it carries.

## 6. Collect the harvest

The drone brings everything back to the dock. Open the dock to take the items, or pull them out
with a hopper or any item pipe.

## Next steps

- Learn more about drones in [Using Drones](drones.md) and [Function Modules](modules.md).
- Build better power in [Generators](generators.md).
