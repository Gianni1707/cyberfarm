---
navigation:
  title: Supply Contracts
  parent: drone_systems.md
  position: 8
  icon: cyberfarm:supply_terminal
item_ids:
  - cyberfarm:supply_terminal
---

# Supply Contracts

Your farm can feed a village. Place a <ItemLink id="supply_terminal" /> in one, or find one in a
[Drone Mechanist's Outpost](tech_village.md), and the village starts asking you for food and
materials, and paying for them.

<BlockImage id="cyberfarm:supply_terminal" scale="4" float="left" />

## The Drone Mechanist

An unemployed villager takes the terminal as his job site, like a farmer takes a composter, and
becomes a **Drone Mechanist**: a villager in goggles and a white work coat. While he works it, the
terminal shows his level and the village's contracts.

**Right-click** him to talk: he has a plan for a [tech village](tech_village.md). His trades are
behind the **Trade** button at the end of each talk.

<br clear="all" />

## Contracts

The board holds **three contracts**; every two minutes the mechanist fills the free places. Each
asks for a quantity of one item: bread, carrots, wool and wheat at first; leather, logs, pumpkins
and beef as the mechanist levels up; then iron, glass and fish; and from the expert level on,
aluminum, circuits and carbon fiber. The higher his level, the bigger the contracts and the better
they pay, and no two are quite the same size.

Put the goods in the **Deliver** slot: by hand, with a hopper, or as the destination of a
[Transport drone](modules.md)'s route. The terminal only takes what is still wanted, so a drone
brings exactly what the contracts need and nothing more.

Some contracts pay a **bonus** besides their emeralds, shown on the row: battery cells, circuits,
addons, carbon fiber, even an Anchor Addon or an Elite Circuit from a master mechanist.

## Events

The fourth place on the board is for the village's events:

- **Market Day**: every morning the village holds a market, a contract **three times** the usual
  size and pay, always with a bonus. It closes at dusk: fill it in time.
- **Call to arms**: while a **raid** is on the village asks for iron, arrows or shields. Fill it and
  **two iron golems** join the fight.

## Prosperity

Every filled contract makes the village more **prosperous**: its emeralds count as points. Once
you have founded the [tech village](tech_village.md) with the Mechanist, the bar under the board
shows its level and how far the next one is; the **Range** switch beside it outlines the village's
bounds in the world. When the bar is full and the last level's buildings stand, **talk to the
Mechanist** to raise the village a level: it grows, a baby villager or an iron golem arriving, rings its bell, and
makes you its hero: **Hero of the Village** for twenty minutes, stronger at each level, with big
discounts and gifts from the villagers. At the top level, **10**, every contract pays half again.

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
