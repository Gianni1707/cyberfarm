---
navigation:
  title: Nuclear
  position: 2
  icon: cyberfarm:reactor_controller
---

# Nuclear

Nuclear power is the strongest energy in CyberFarm, and the most demanding. A **Fission Reactor**
shaped like a cooling tower burns uranium **Fuel Rods**. Its core boils water into **Steam**, and
**Steam Turbines** turn the steam into FE, just like a real power plant.

<ItemGrid>
  <ItemIcon id="uranium_ingot" />
  <ItemIcon id="fuel_rod" />
  <ItemIcon id="reactor_controller" />
  <ItemIcon id="reactor_casing" />
  <ItemIcon id="reactor_port" />
  <ItemIcon id="coolant_mixer" />
  <ItemIcon id="turbine_controller" />
  <ItemIcon id="water_pump" />
</ItemGrid>

## The power plant

1. Mine [uranium](uranium.md) deep underground and craft [Fuel Rods](fuel_rod.md).
2. Build the [Fission Reactor](fission_reactor.md) tower.
3. Feed it **water** with [Water Pumps](water_pump.md) and [Liquid Pipes](liquid_pipe.md).
4. Send the steam to [Steam Turbines](steam_turbine.md), and their FE into
   [Energy Conduits](energy_conduit.md).
5. To run the reactor harder, cool it with [Coolant](coolant.md) from a
   [Coolant Mixer](coolant_mixer.md).

The [Fission Reactor](fission_reactor.md) page has a table of what each tower size produces, and how
many turbines it needs.

## Safety first

A reactor keeps burning as long as its power is on, **whether or not it is cooled**. If it makes
more heat than it can move out, the core gets hotter. At **80%** an alarm sounds; at **100%** the
core **melts down** and explodes. Watch the temperature bar, and turn the power down (or off) when
something goes wrong.

Spent rods are **nuclear waste**: left untreated they make the area [radioactive](radiation.md).
Treat them in a [Waste Reprocessor](waste_reprocessor.md). Wear a [Hazmat Suit](hazmat_suit.md) and
carry a [Geiger Counter](geiger_counter.md) when things go wrong.

## Pages

<SubPages icons={true} />
