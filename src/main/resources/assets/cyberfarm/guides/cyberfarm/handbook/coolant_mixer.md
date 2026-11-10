---
navigation:
  title: Coolant Mixer
  parent: nuclear.md
  position: 3
  icon: cyberfarm:coolant_mixer
item_ids:
  - cyberfarm:coolant_mixer
  - cyberfarm:coolant_bucket
  - cyberfarm:cryo_coolant_bucket
---

# Coolant Mixer

<BlockImage id="cyberfarm:coolant_mixer" scale="4" float="left" />

Makes the coolants that let a [Fission Reactor](fission_reactor.md) run at higher power. Uses
**100 FE/t**; each batch takes 2 seconds and makes **500 mB**. It takes Speed, Efficiency and Battery [addons](addons.md) on the tab beside its GUI.

It is a small multiblock: **place a Cyber Frame on its right**, seen from its front, against the open flange.
**Right-click** the machine and a mini drone flies out and projects the frame where it goes (hold sneak to place the frame against the machine itself). When the frame is
there the machine assembles and opens, and both tanks show their levels. It does not work until then, so a Mixer
placed before this change waits for its frame, with its contents kept.

<br clear="all" />

| Fluid in (500 mB) | Item | Fluid out (500 mB) |
| --- | --- | --- |
| Water | Lapis Lazuli | **Coolant** |
| Coolant | Blue Ice | **Cryo Coolant** |

- Pipe the fluid in (water from a [Water Pump](water_pump.md), or Coolant to upgrade it), put the
  items in the slot by hand or with a hopper. Pipes and hoppers connect to the machine's half of the bench only,
  on its left side, front, back or top.
- The left tank shows what goes in, the right one what comes out. Breaking the frame folds the bench back and
  stops it; put a frame back and it opens again.
- The result is pushed into adjacent pipes and tanks by itself: pipe it to a **Reactor Port**.

## Buckets and pools

- **Fill a bucket**: right-click the Coolant Mixer (or any tank holding at least 1,000 mB of coolant)
  with an **empty bucket**.
- **Empty it**: right-click a Reactor Port or tank to pour it in, or anywhere else to pour a
  **pool** of coolant, just like water. An empty bucket scoops it back up.
- **Careful**: coolant is thick and freezing cold. Wading through a pool slows you down, and the
  longer you stay, the more frost covers the screen; once fully frozen you take damage. **Cryo
  Coolant** slows you more and freezes you about four times faster. As in powder snow, **leather
  armor** keeps the cold out (but not the slowdown).

<ItemGrid>
  <ItemIcon id="coolant_bucket" />
  <ItemIcon id="cryo_coolant_bucket" />
</ItemGrid>

## Why better coolant

The coolant fills the reactor's **primary loop** and decides how much heat can leave the core
each tick: plain water moves the tower's base amount (150, 250 or 400 by size), Coolant **2.5 times** as much,
Cryo Coolant **6 times** (up to **2,400** in a Large tower). It is not used
up like the water, but wears out slowly: 1 mB for every 400 heat it moves.

<RecipeFor id="coolant_mixer" />
