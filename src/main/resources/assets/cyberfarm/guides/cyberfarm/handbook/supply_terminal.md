---
navigation:
  title: Supply Terminal
  parent: drone_systems.md
  position: 10
  icon: cyberfarm:supply_terminal
item_ids:
  - cyberfarm:supply_terminal
---

# Supply Terminal

<BlockImage id="cyberfarm:supply_terminal" scale="4" float="left" />

The Supply Terminal lets your farm feed a village. The village posts **contracts** on it, asking
for food and materials, and pays you in emeralds and parts.

Place one in a village, or find one in a [Drone Mechanist's Outpost](tech_village.md). An
unemployed villager takes it as his job site and runs the contracts.

<br clear="all" />

## The Drone Mechanist

An unemployed villager takes the terminal as his job site, like a farmer takes a composter, and
becomes a **Drone Mechanist**: a villager in goggles and a white work coat. While he works it, the
terminal shows his level and the village's contracts.

The board shows the contracts from afar too:

- a lit card for each open contract, filling up as you deliver;
- the event's card in steel, blinking in a raid;
- a finished contract's card drops behind the board and a new one rises;
- the header is lit while the Mechanist works the terminal.

**Right-click** him to talk: he has a plan for a [tech village](tech_village.md). His trades are
behind the **Trade** button at the end of each talk.

## Contracts

The board holds **three contracts**. Every **two minutes** the mechanist fills the free places.
Each contract asks for a quantity of one item. As the mechanist levels up, new goods join the ones
he already asks for:

| Mechanist level | New goods |
| --- | --- |
| Novice | bread, carrots, potatoes, wheat, beetroot, apples, white wool |
| Apprentice | cooked beef, leather, oak logs, eggs, pumpkins, melon slices |
| Journeyman | iron ingots, cooked chicken, glass, cod, string |
| Expert | aluminum ingots, Basic Circuits, gold ingots |
| Master | carbon fiber, Advanced Circuits |

The higher his level, the bigger the contracts and the better they pay. No two are quite the same size.

## Delivering

Put the goods in the **Deliver** slot: by hand, with a hopper, or as the destination of a
[Transport drone](modules.md)'s route. The terminal only takes what is still wanted, so a drone
brings exactly what the contracts need and nothing more.

Some contracts pay a **bonus** besides their emeralds, shown on the row: battery cells, circuits,
addons, carbon fiber, even an Anchor Addon from an expert mechanist or an Elite Circuit from a master.

## Events

The fourth place on the board is for the village's events:

- **Market Day**: every morning the village holds a market, a contract **three times** the usual
  size and pay, always with a bonus. It closes at dusk: fill it in time.
- **Call to arms**: while a **raid** is on the village asks for iron, arrows or shields. Fill it and
  **two iron golems** join the fight.

## Prosperity

Every filled contract makes the village more **prosperous**: its emeralds count as points.

Once you have founded the [tech village](tech_village.md) with the Mechanist:

- the bar under the board shows the village's level and how far the next one is;
- the **Range** switch beside it outlines the village's bounds in the world.

When the bar is full and the last level's buildings stand, **talk to the Mechanist** to raise the
village a level. The village grows, a baby villager or an iron golem arrives, and it rings its
bell. It makes you its hero: **Hero of the Village** for twenty minutes, stronger at each level,
with big discounts and gifts from the villagers.

At the top level, **10**, every contract pays half again. The levels are listed on
[The Tech Village](tech_village.md) page.

## Rewards

A filled contract:

- pays **emeralds**, and any bonus, into the **Pay** slot (take them by hand, or with a hopper or a
  drone);
- gives the mechanist **experience**: he levels up, from Novice to Master, unlocking bigger
  contracts and new trades;
- makes the village **like you**: every villager around gives you better prices, the mechanist
  most of all. The goodwill goes to whoever founded the village (before that, to whoever placed
  the terminal).

## The mechanist's trades

The Drone Mechanist sells CyberFarm parts for emeralds, more as he levels up:

| Level | Trades |
| --- | --- |
| Novice | Basic Circuits; buys redstone |
| Apprentice | Battery Cells; buys copper |
| Journeyman | Advanced Circuits, Aluminum Ingots |
| Expert | Speed and Stack Addons, Carbon Fiber |
| Master | Elite Circuits, the Anchor Addon |

<RecipeFor id="supply_terminal" />
