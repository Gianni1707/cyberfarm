---
navigation:
  title: Coolant Mixer
  parent: generators.md
  position: 9
  icon: cyberfarm:coolant_mixer
item_ids:
  - cyberfarm:coolant_mixer
---

# Coolant Mixer

<BlockImage id="cyberfarm:coolant_mixer" scale="4" float="left" />

Makes the coolants that let a [Fission Reactor](fission_reactor.md) run at higher power. Uses
**20 FE/t**; each batch takes 2 seconds and makes **500 mB**.

<br clear="all" />

| Fluid in (500 mB) | Item | Fluid out (500 mB) |
| --- | --- | --- |
| Water | Lapis Lazuli | **Coolant** |
| Coolant | Blue Ice | **Cryo Coolant** |

- Pipe the fluid in (water from a [Water Pump](water_pump.md), or Coolant to upgrade it), put the
  items in the slot by hand or with a hopper.
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
each tick: 200 with plain water, **500** with Coolant, **1,200** with Cryo Coolant. It is not used
up like the water, but wears out slowly: 1 mB for every 400 heat it moves.

<RecipeFor id="coolant_mixer" />
