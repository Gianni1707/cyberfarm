---
navigation:
  title: Muffler
  parent: index.md
  position: 2
  icon: cyberfarm:muffler
item_ids:
  - cyberfarm:muffler
---

# Muffler

<BlockImage id="cyberfarm:muffler" scale="4" float="left" />

A busy farm is a noisy farm: humming drones, bubbling extraction chambers, crackling generators
and the mobs locked inside them. Place a **Muffler** and the sounds you choose go quiet within
**10 blocks**.

<br clear="all" />

## Choosing what it silences

**Right-click** the Muffler and flip the switch of each kind of sound. A new Muffler silences everything.

| Row | Silences |
| --- | --- |
| Machines | CyberFarm machines: generators, the Drone Assembler, the Methane Extractor |
| Drones | the whine of their rotors (it fades out as a drone flies into range), takeoff and landing |
| Monsters | hostile mobs, like the zombie in your Methane Extractor |
| Animals | passive and neutral mobs: cows, chickens, villagers... |
| Other machines | the sounds listed in the client config: by default furnaces, blast furnaces and smokers |

Each Muffler keeps its own choices, so one can quiet the drone field while another keeps the
mob farm silent. Your own sounds are never touched: footsteps, doors, tools, blocks you place or
break, music and other players.

## Client config

In **Mods → CyberFarm → Config** (or `config/cyberfarm-client.toml`):

- **Range**: distance in blocks every Muffler covers, 1 to 64 (default 10);
- **Extra Sounds to Mute**: what the **Other machines** row silences. Add a mod id (`mekanism`) for all of a
  mod's sounds, or a single sound id (`minecraft:block.furnace.fire_crackle`).

## Crafting

<RecipeFor id="muffler" />

The Muffler needs no power. Use several to cover a large base.
