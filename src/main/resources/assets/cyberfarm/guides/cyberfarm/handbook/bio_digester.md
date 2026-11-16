---
navigation:
  title: Bio-Digester
  parent: generators.md
  position: 4
  icon: cyberfarm:bio_digester
item_ids:
  - cyberfarm:bio_digester
---

# Bio-Digester

<BlockImage id="cyberfarm:bio_digester" scale="4" float="left" />

The Bio-Digester digests **Biomass** into **Biogas**, a gas fuel for the [Gas Turbine](gas_turbine.md) and the
[Steam Engine](steam_engine.md). With a farm feeding it, it is power that runs without coal.

<br clear="all" />

## Building it

- Place the digester, then **2 [Cyber Frames](cyber_frame.md)**: one on top of it and one on its left as you face it.
- **Right-click** it while it is alone and drones show where the frames go.
- Once complete, the upper drum rises and a tank slides out beside it, its sight glass showing the gas.
- It doesn't work until then. Breaking a frame folds it back.
- Connect an [Energy Conduit](energy_conduit.md) to the machine block or to one of its frames.

## Using it

- It accepts only <ItemLink id="biomass" />, from a [Biomass Press](biomass_press.md).
- It pushes the gas into adjacent [Gas Pipes](gas_pipe.md), tanks or a Gas Turbine.
- Pipes connect to the machine block, on its free sides.
- An empty bucket on it fills a Biogas Bucket once it holds 1,000 mB (see [Gas Power](gas_power.md)).
- It takes Speed, Efficiency and Battery [addons](addons.md) on the tab beside its GUI.

## Numbers

| Stat | Value |
| --- | --- |
| Power | 8 FE/t |
| Time per Biomass | 2.5 seconds |
| Biogas per Biomass | 500 mB |
| Output while fed | 10 mB/t |
| Gas tank | 8,000 mB |
| One Biomass in a Gas Turbine | 2,500 FE |
| One Biomass in a Steam Engine | 4,500 FE |

One digester feeds 50 FE/t worth of gas to a Gas Turbine. **Two digesters** keep one Gas Turbine at full power.

## Tips

A Harvester drone, a hopper into a Biomass Press and the press's nozzle pointing at the digester make a fully automatic biogas farm.

<RecipeFor id="bio_digester" />
