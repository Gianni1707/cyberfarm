---
navigation:
  title: Liquid and Gas Tanks
  parent: cables.md
  position: 4
  icon: cyberfarm:liquid_tank
item_ids:
  - cyberfarm:liquid_tank
  - cyberfarm:gas_tank
---

# Liquid and Gas Tanks

<Row>
  <BlockImage id="cyberfarm:liquid_tank" scale="3" />
  <BlockImage id="cyberfarm:gas_tank" scale="3" />
</Row>

A tank block stores **32,000 mB** (32 buckets) of a single fluid:

- the **Liquid Tank** (a steel line around the glass, waves on its lid) takes liquids: water, lava, Coolant, Cryo Coolant, Acid, liquids
  of other mods;
- the **Gas Tank** (a mint line around the glass, a cloud on its lid) takes gases: biogas, methane, Steam, gases of other mods.

**Tanks grow.** Blocks of the same kind that fill a box, of any size up to 16 on a side, join into one tank: a column, a row,
a 2x2x2, a 3x3x3. Each block adds 32 buckets, and pipes and buckets see them as one. Tanks already holding different fluids don't join.

Fill them with the matching [pipe](cables.md): a [Liquid Pipe](liquid_pipe.md) or a
[Gas Pipe](gas_pipe.md), on any free face of any block, top and bottom too: a collar, steel on the Liquid Tank and mint on the Gas Tank, shows where one plugs in. A Liquid Tank also takes and gives **buckets**: just right-click it with one.

Tanks are **passive**: they never push anything out. To empty one into a machine, right-click
the arm of the pipe that touches it, so the pipe **pulls** from the tank (an orange collar shows
it). A joined tank has one frame around its outside and one pane of glass on each side, however many blocks it has. Through it you see into the tank,
right through to the other side, and the fluid inside stands up to its level with its own colour (gases look hazy); shift + right-click a tank, with anything in hand, to read the exact amount.

A tank is a handy buffer: put one between a Methane Extractor and the Gas Turbines, or between a
Water Pump and a Fission Reactor, and it keeps the flow going when production dips.

Breaking a block **loses what that block held**; the rest stays in the other blocks, which join up again where they can.

<RecipeFor id="liquid_tank" />

<RecipeFor id="gas_tank" />
