---
navigation:
  title: Coolant and Cryo Coolant
  parent: nuclear.md
  position: 5
  icon: cyberfarm:coolant_bucket
item_ids:
  - cyberfarm:coolant_bucket
  - cyberfarm:cryo_coolant_bucket
---

# Coolant and Cryo Coolant

<ItemImage id="cyberfarm:coolant_bucket" scale="2" float="left" />

**Coolant** and **Cryo Coolant** are freezing liquids for the [Fission Reactor](fission_reactor.md).
They carry heat out of the core much faster than plain water, so the reactor can run at higher power
without overheating. Both are made in the [Coolant Mixer](coolant_mixer.md).

<br clear="all" />

<ItemGrid>
  <ItemIcon id="coolant_bucket" />
  <ItemIcon id="cryo_coolant_bucket" />
</ItemGrid>

## In the reactor

Pipe the coolant into a **Reactor Port**, or pour a bucket into it. It fills the reactor's coolant
tank, its **primary loop**: the fluid that carries the core's heat to the water. With that tank
empty, the reactor cools with plain water.

| Primary loop | Heat moved | Small | Medium | Large |
| --- | --- | --- | --- | --- |
| Water | 1x | 150 | 250 | 400 |
| Coolant | **2.5x** | 375 | 625 | 1,000 |
| Cryo Coolant | **6x** | 900 | 1,500 | 2,400 |

Heat per tick each tower size can move.

The reactor's coolant tank holds **one kind** at a time. To switch, set the power to **Off** and
click the **Dump coolant** button under the coolant tank: the tank is poured away and lost. Then
pipe in the new one.

## How fast it wears out

Coolant is not boiled away like the water, but it wears out: **1 mB for every 400 heat** it moves.
At the safe power from the [Fission Reactor](fission_reactor.md) table:

| Tower and loop | Used per minute | Full tank (16,000 mB) lasts | Items per minute |
| --- | --- | --- | --- |
| Small, Coolant | 600 mB | 26 min 40 s | 1.2 Lapis |
| Small, Cryo | 600 mB | 26 min 40 s | 1.2 Lapis + 1.2 Blue Ice |
| Medium, Coolant | 1,800 mB | 8 min 53 s | 3.6 Lapis |
| Medium, Cryo | 2,100 mB | 7 min 37 s | 4.2 Lapis + 4.2 Blue Ice |
| Large, Coolant | 3,000 mB | 5 min 20 s | 6 Lapis |
| Large, Cryo | 6,000 mB | 2 min 40 s | 12 Lapis + 12 Blue Ice |

Keep a Coolant Mixer piped in. When the coolant runs out, cooling falls back to the water level and
a reactor running hard overheats.

## Buckets and pools

- **Fill a bucket**: right-click the Coolant Mixer, or any tank holding at least 1,000 mB of
  coolant, with an **empty bucket**.
- **Empty it**: right-click a Reactor Port, machine or tank to pour it in. Anywhere else it pours a
  **pool** of coolant, just like water. An empty bucket scoops it back up.

**Careful**: coolant is thick and freezing cold. Wading through a pool slows you down, and the
longer you stay, the more frost covers the screen. Once fully frozen you take damage. **Cryo
Coolant** slows you more and freezes you about **four times faster**. As in powder snow, **leather
armor** or a [Hazmat Suit](hazmat_suit.md) keeps the cold out, but not the slowdown.
