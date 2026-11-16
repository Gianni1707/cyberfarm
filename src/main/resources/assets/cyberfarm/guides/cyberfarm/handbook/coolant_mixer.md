---
navigation:
  title: Coolant Mixer
  parent: nuclear.md
  position: 4
  icon: cyberfarm:coolant_mixer
item_ids:
  - cyberfarm:coolant_mixer
---

# Coolant Mixer

<BlockImage id="cyberfarm:coolant_mixer" scale="4" float="left" />

The Coolant Mixer makes [Coolant and Cryo Coolant](coolant.md). Piped into a
[Fission Reactor](fission_reactor.md), they move heat out of the core much faster than plain water,
so the reactor can run at higher power.

<br clear="all" />

## Building it

It is a small multiblock: it needs a **Cyber Frame on its right**, seen from its front, against the
open flange.

- **Right-click** the machine and a mini drone flies out and projects the frame where it goes.
- Hold sneak to place the frame against the machine itself.
- When the frame is there the machine assembles and opens, and both tanks show their levels.
- Until then it does nothing, but it keeps its contents. Breaking the frame folds the bench back
  and stops it; put a frame back and it opens again.

## Using it

| Fluid in (500 mB) | Item | Fluid out (500 mB) |
| --- | --- | --- |
| Water | Lapis Lazuli | **Coolant** |
| Coolant | Blue Ice | **Cryo Coolant** |

- Pipe the fluid in: water from a [Water Pump](water_pump.md), or Coolant to upgrade it to Cryo.
- Put the item in the slot by hand or with a hopper.
- Pipes and hoppers connect only to the machine's half of the bench: its left side, front, back or
  top.
- The left tank shows what goes in, the right one what comes out.
- The result is pushed into adjacent pipes and tanks by itself: pipe it to a **Reactor Port**.
- You can also fill an empty bucket from it: see [Coolant](coolant.md).
- It takes Speed, Efficiency and Battery [addons](addons.md) on the tab beside its GUI.

## Numbers

| What | Amount |
| --- | --- |
| Energy | 100 FE/t |
| Batch | 500 mB every 2 seconds |
| Tanks | 8,000 mB in, 8,000 mB out |

Without addons that is 15,000 mB a minute: more than even a Large reactor on Cryo Coolant uses.

## Switching coolants

Each tank holds one fluid at a time.

- **In the Mixer**: to switch from making Coolant to making Cryo Coolant, let the output tank empty
  first (pipe it away). Then feed it Coolant and Blue Ice.
- **In the reactor**: the coolant tank holds one kind at a time. Set the reactor's power to **Off**
  and click the small **Dump coolant** button under its coolant tank. The tank is poured away, then
  pipe in the new coolant. See [Fission Reactor](fission_reactor.md).

<RecipeFor id="coolant_mixer" />
