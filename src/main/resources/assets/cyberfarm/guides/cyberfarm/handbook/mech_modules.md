---
navigation:
  title: Mech Modules
  parent: mech.md
  position: 2
  icon: cyberfarm:mech_jet_module
item_ids:
  - cyberfarm:mech_jet_module
  - cyberfarm:mech_drill_module
  - cyberfarm:mech_blade_module
  - cyberfarm:mech_armor_module
---

# Mech Modules

A mech has four mounts, one module each. Modules are fitted in a [Mech Hangar](mech_hangar.md)'s
console while the mech is parked on the pad: the hangar's gantry arm carries each one to its mount.
Every module is built around an Advanced Frame.

Their parts stay stowed while the mech stands empty. **When you climb in**, the jet's wings and
the armour come out; the drill and the blade come out when you switch them on, and go back when
you switch them off or get out.

| Module | Mount | Comes out | What it does |
| --- | --- | --- | --- |
| <ItemLink id="mech_jet_module" /> | Back | Two wings unfold segment by segment, two nozzles light up | **Flight**: hold jump in the air to climb, sneak to dive, neither to glide. 60 FE/t in the air |
| <ItemLink id="mech_drill_module" /> | Right arm | The **R** key slides a drill out under the hand; it turns while on | Digs twice as fast, **3x3**, and gets the drops of any tier. 20 FE per extra block |
| <ItemLink id="mech_blade_module" /> | Left arm | The **G** key slides a glowing blade out of the forearm | **18** extra damage a blow and **8** to everything near the target. 80 FE a blow |
| <ItemLink id="mech_armor_module" /> | Trunk | Plates close onto the shoulders, chest and thighs | The mech takes **40%** less damage |

The keys are in the controls under *CyberFarm Mech*. The cockpit display lists every fitted
module with its state: the jet READY or THRUST, the drill and blade ON or OFF with their keys,
the armour's protection.

<Row>
  <RecipeFor id="mech_jet_module" />
  <RecipeFor id="mech_drill_module" />
</Row>

<Row>
  <RecipeFor id="mech_blade_module" />
  <RecipeFor id="mech_armor_module" />
</Row>
