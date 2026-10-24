---
navigation:
  title: Mech Hangar
  parent: mech.md
  position: 1
  icon: cyberfarm:mech_hangar
item_ids:
  - cyberfarm:mech_hangar
  - cyberfarm:hangar_frame
  - cyberfarm:hangar_pad
---

# Mech Hangar

Where your [mech](mech.md) lives: a frame it parks in, on a pad that **charges** its battery,
**repairs** it and lets you **fit its modules**.

<GameScene zoom={2} interactive={true}>
  <Block id="cyberfarm:mech_hangar" x="4" y="0" z="0" p:facing="north" />
  <Block id="cyberfarm:hangar_frame" x="4" y="0" z="1" />
  <Block id="cyberfarm:hangar_frame" x="4" y="0" z="2" />
  <Block id="cyberfarm:hangar_frame" x="4" y="0" z="3" />
  <Block id="cyberfarm:hangar_frame" x="4" y="0" z="4" />
  <Block id="cyberfarm:hangar_frame" x="4" y="1" z="4" />
  <Block id="cyberfarm:hangar_frame" x="4" y="2" z="4" />
  <Block id="cyberfarm:hangar_frame" x="4" y="3" z="4" />
  <Block id="cyberfarm:hangar_frame" x="4" y="4" z="4" />
  <Block id="cyberfarm:hangar_frame" x="4" y="5" z="4" />
  <Block id="cyberfarm:hangar_pad" x="3" y="0" z="1" />
  <Block id="cyberfarm:hangar_pad" x="3" y="0" z="2" />
  <Block id="cyberfarm:hangar_frame" x="3" y="5" z="2" />
  <Block id="cyberfarm:hangar_pad" x="3" y="0" z="3" />
  <Block id="cyberfarm:hangar_frame" x="3" y="5" z="3" />
  <Block id="cyberfarm:hangar_frame" x="3" y="5" z="4" />
  <Block id="cyberfarm:hangar_pad" x="2" y="0" z="1" />
  <Block id="cyberfarm:hangar_pad" x="2" y="0" z="2" />
  <Block id="cyberfarm:hangar_pad" x="2" y="0" z="3" />
  <Block id="cyberfarm:hangar_frame" x="2" y="5" z="4" />
  <Block id="cyberfarm:hangar_pad" x="1" y="0" z="1" />
  <Block id="cyberfarm:hangar_pad" x="1" y="0" z="2" />
  <Block id="cyberfarm:hangar_frame" x="1" y="5" z="2" />
  <Block id="cyberfarm:hangar_pad" x="1" y="0" z="3" />
  <Block id="cyberfarm:hangar_frame" x="1" y="5" z="3" />
  <Block id="cyberfarm:hangar_frame" x="1" y="5" z="4" />
  <Block id="cyberfarm:hangar_frame" x="0" y="0" z="0" />
  <Block id="cyberfarm:hangar_frame" x="0" y="0" z="1" />
  <Block id="cyberfarm:hangar_frame" x="0" y="0" z="2" />
  <Block id="cyberfarm:hangar_frame" x="0" y="0" z="3" />
  <Block id="cyberfarm:hangar_frame" x="0" y="0" z="4" />
  <Block id="cyberfarm:hangar_frame" x="0" y="1" z="4" />
  <Block id="cyberfarm:hangar_frame" x="0" y="2" z="4" />
  <Block id="cyberfarm:hangar_frame" x="0" y="3" z="4" />
  <Block id="cyberfarm:hangar_frame" x="0" y="4" z="4" />
  <Block id="cyberfarm:hangar_frame" x="0" y="5" z="4" />
  <Entity id="cyberfarm:mech" x="2.5" y="1" z="2.5" rotationY="180" />
</GameScene>

## Building it

The hangar is a **rack** the mech stands in, 5 blocks wide and deep and 6 high, open in front and
on the sides so the mech walks in and out:

- the <ItemLink id="mech_hangar" /> at the **front of the left floor rail**, its screen facing out
  (place it facing you as you stand in front of the hangar);
- two **floor rails** of <ItemLink id="hangar_frame" />, 5 blocks long, along the sides;
- a **post** of Hangar Frames at the back end of each rail, 5 blocks up, a **beam** across their
  tops and two **arms**, two blocks long, reaching forward from the beam over the pad;
- a 3x3 <ItemLink id="hangar_pad" /> between the rails, with the space over it left free.

The [Multiblock Projector](multiblock_projector.md) shows it: sneak + right-click to pick *Mech
Hangar*, then look where the console goes. When it is complete the frames give way to the rack
itself: white rails with treads and a cyan light line, posts with blue bands braced to the rails,
the beam and its arms, hydraulic struts, two **clamps** that come down onto a parked mech's
shoulders and a **gantry arm** riding on the arms, which fits the modules.

## Parking

A <ItemLink id="mech_frame" /> only unfolds here: right-click the pad and the mech unfolds in the
middle of it, facing out of the hangar. Walk it back onto the pad and get out to park it: it
straightens up on the stand by itself, gliding to the middle of the pad and turning to face out,
and the clamps come down onto its shoulders. While it stands there, empty:

- it is **charged** at up to **2,000 FE/t** from the console's own battery (1,000,000 FE), which
  you fill with [Energy Conduits](energy_conduit.md) or any mod's cables;
- it is **repaired**, 1 point of integrity every half second for 200 FE. A mech that **shut down**
  comes back on once it is fully repaired.

You can climb in and walk out at any time: in the hangar the mech crouches and opens to let you
in just as it does outside.

## The console

Right-click the console, or with an empty hand any part or the pad of the complete hangar, for its
screen. On the left, a card with the parked mech's **battery** and **integrity**, the hangar's own
battery, the status (charging, repairing, ready) and the **Fold** button. On the right, the mech's
outline with its four **module slots** at their mounts (see [Modules](mech_modules.md)): the jet
behind the shoulder, the armour on the trunk, the blade and the drill by the arms.

The slots show the modules the parked mech carries. Put a module in its slot to fit it, take it
out to remove it, and the **gantry arm** does the work: the bridge rolls along the rack's arms,
the gripper drops onto the mount (the back, a forearm or the chest) and the module locks in with a
flash, or comes away held in the gripper; then the arm goes home. The module is on the mech once
the gripper reaches it, and the modules always travel with the mech.
With no mech on the pad, the slots are empty and take nothing.

The **Fold** button packs the parked mech back up: it crouches, crosses its arms, draws its head and
legs in and folds down into its frame, which drops on the pad with the battery's charge and the
modules in it.

<Row>
  <RecipeFor id="mech_hangar" />
  <RecipeFor id="hangar_frame" />
  <RecipeFor id="hangar_pad" />
</Row>
