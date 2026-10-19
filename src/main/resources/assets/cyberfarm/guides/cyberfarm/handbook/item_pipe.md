---
navigation:
  title: Item Pipe
  parent: cables.md
  position: 3
  icon: cyberfarm:item_pipe
item_ids:
  - cyberfarm:item_pipe
  - cyberfarm:advanced_item_pipe
  - cyberfarm:elite_item_pipe
---

# Item Pipe

<BlockImage id="cyberfarm:item_pipe" p:east="true" p:west="true" scale="4" float="left" />

Pipes that move **items** between chests, machines and inventories of any mod. Item Pipes have a
green stripe.

<br clear="all" />

- Pipes **connect by themselves** to other Item Pipes and to any block with an inventory.
- **Pulling**: right-click the arm of a pipe that touches an inventory to make it **pull** items
  out of it; an orange collar shows it. **Sneak + right-click** the arm to stop.
- **Pushing**: hoppers and machines that push items out can push them straight into a pipe.
- Items go to the **nearest** inventory on the network that has room, never back where they came
  from. They arrive at once.
- Rate per source: **8 items/s** for basic pipes, **32** for Advanced ones (gold rims) and **128**
  for Elite ones (violet rims). All tiers join into one network.

## Filter

The pulling pipe, the first of the line, has a **filter**: right-click it again to open it. It
decides which items may enter the network through that pipe, both the ones it pulls and the ones
hoppers push into it.

- **Samples**: click a filter slot with an item to add it, empty-handed to clear it. Shift-click
  in your inventory works too. Nothing is used up: they are only copies.
- **Whitelist / Blacklist**: only the matching items pass, or everything except them. A new pipe
  has an empty blacklist, so everything passes.
- **NBT: exact / any**: whether enchantments, damage, names and other data must match too.
- **By item / By mod**: match the sample items, or any item from the same mod as a sample.
- **Tag**: the arrows step through the tags of the first sample (for example `#c:ingots`); every
  item with that tag matches, together with the samples.

To send different items to different places, use a pulling pipe per item kind, each with its own
whitelist.

<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:chest" />
  <Block id="cyberfarm:item_pipe" x="1" p:west="true" p:east="true" p:extract_west="true" />
  <Block id="cyberfarm:item_pipe" x="2" p:west="true" p:east="true" />
  <Block id="minecraft:barrel" x="3" />
</GameScene>

<RecipeFor id="item_pipe" />

<RecipeFor id="advanced_item_pipe" />

<RecipeFor id="elite_item_pipe" />
