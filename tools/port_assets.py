"""Bring resources written in the 26.x formats over to the 1.20.1 ones, in place.

    python tools/port_assets.py [folder of the GuideME .md pages]

The asset generators in tools/ are shared with the 26.x branches and write their formats; run this
after them (or after carrying files over from a 26.x branch). Safe to run more than once:
- data folders take their 1.20.1 names (recipe -> recipes, loot_table -> loot_tables, tags/item ->
  tags/items, structure -> structures, c: tags -> forge: tags...);
- recipes take the 1.20.1 ingredient and result objects;
- block models' "light_emission" becomes Forge's "forge_data" block and sky light, and one with see-through
  textures and no render_type gets cutout (or translucent), which 26.x picks by itself;
- item model definitions (assets/<ns>/items/*.json) become item models in models/item;
- structure files take the 1.20.1 data version and blocks;
- texts naming c: tags name the forge: ones;
- a GuideME handbook (the folder given, or one left under assets/cyberfarm/guides) becomes the
  Patchouli book through tools/patchouli_book.py.
"""
import gzip
import json
import os
import re
import shutil
import subprocess
import struct
import sys
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'src', 'main', 'resources')
DATA = os.path.join(RES, 'data')
ASSETS = os.path.join(RES, 'assets')

DATA_VERSION = 3465  # SharedConstants.WORLD_VERSION of Minecraft 1.20.1
FOLDERS = {'recipe': 'recipes', 'loot_table': 'loot_tables', 'structure': 'structures', 'advancement': 'advancements'}
TAG_FOLDERS = {'block': 'blocks', 'item': 'items', 'fluid': 'fluids', 'entity_type': 'entity_types', 'game_event': 'game_events'}
# Blocks renamed after 1.20.1 -> their 1.20.1 names
BLOCKS = {'minecraft:iron_chain': 'minecraft:chain', 'minecraft:short_grass': 'minecraft:grass'}


def load(path):
    with open(path, encoding='utf-8') as f:
        return json.load(f)


def save(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')


def move_tree(src, dst):
    """Move every file of src into dst, replacing what is there, and remove src."""
    for base, _, files in os.walk(src):
        for name in files:
            target = os.path.join(dst, os.path.relpath(os.path.join(base, name), src))
            os.makedirs(os.path.dirname(target), exist_ok=True)
            os.replace(os.path.join(base, name), target)
    shutil.rmtree(src)


def rename_folders():
    for ns in os.listdir(DATA):
        nsdir = os.path.join(DATA, ns)
        for old, new in FOLDERS.items():
            if os.path.isdir(os.path.join(nsdir, old)):
                move_tree(os.path.join(nsdir, old), os.path.join(nsdir, new))
        tags = os.path.join(nsdir, 'tags')
        if os.path.isdir(tags):
            for old, new in TAG_FOLDERS.items():
                if os.path.isdir(os.path.join(tags, old)):
                    move_tree(os.path.join(tags, old), os.path.join(tags, new))
        if os.path.isdir(os.path.join(nsdir, 'neoforge')):
            move_tree(os.path.join(nsdir, 'neoforge'), os.path.join(nsdir, 'forge'))
    if os.path.isdir(os.path.join(DATA, 'c')):
        move_tree(os.path.join(DATA, 'c'), os.path.join(DATA, 'forge'))


def ingredient(value):
    if isinstance(value, str):
        return {'tag': forge_tag(value[1:])} if value.startswith('#') else {'item': value}
    if isinstance(value, list):
        return [ingredient(v) for v in value]
    if isinstance(value, dict) and 'tag' in value:
        return {**value, 'tag': forge_tag(value['tag'])}
    return value


def forge_tag(tag):
    return 'forge:' + tag[2:] if tag.startswith('c:') else tag


def port_recipe(path):
    recipe = load(path)
    before = json.dumps(recipe)
    kind = recipe.get('type', '')
    if 'key' in recipe:
        recipe['key'] = {k: ingredient(v) for k, v in recipe['key'].items()}
    if 'ingredients' in recipe:
        recipe['ingredients'] = [ingredient(v) for v in recipe['ingredients']]
    if 'ingredient' in recipe:
        recipe['ingredient'] = ingredient(recipe['ingredient'])
    result = recipe.get('result')
    if isinstance(result, dict) and 'id' in result:
        if kind in ('minecraft:smelting', 'minecraft:blasting', 'minecraft:smoking', 'minecraft:campfire_cooking'):
            recipe['result'] = result['id']  # 1.20.1 cooking results are a bare item id
        else:
            recipe['result'] = {'item': result['id'], **({'count': result['count']} if 'count' in result else {})}
    if json.dumps(recipe) != before:
        save(path, recipe)


def port_tag(path):
    tag = load(path)
    values = ['#' + forge_tag(v[1:]) if isinstance(v, str) and v.startswith('#') else v for v in tag.get('values', [])]
    if values != tag.get('values', []):
        tag['values'] = values
        save(path, tag)


def png_alpha(path):
    """'none', 'clear' (every pixel 0 or 255 alpha) or 'partial', read with no image library."""
    data = open(path, 'rb').read()
    pos, chunks, idat = 8, {}, b''
    while pos < len(data):
        size, kind = struct.unpack('>I4s', data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + size]
        if kind == b'IDAT':
            idat += body
        else:
            chunks.setdefault(kind, body)
        pos += 12 + size
    width, height, depth, color = struct.unpack('>IIBB', chunks[b'IHDR'][:10])
    if color == 3:  # palette: transparency only through tRNS
        values = set(chunks.get(b'tRNS', b''))
        return 'partial' if values - {0, 255} else 'clear' if 0 in values else 'none'
    if color != 6 or depth != 8:
        return 'clear' if b'tRNS' in chunks else 'none'
    raw, stride = zlib.decompress(idat), width * 4
    prev, i, seen = bytearray(stride), 0, set()
    for _ in range(height):
        kind, line = raw[i], bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - 4] if x >= 4 else 0
            b = prev[x]
            c = prev[x - 4] if x >= 4 else 0
            if kind == 1:
                line[x] = (line[x] + a) & 255
            elif kind == 2:
                line[x] = (line[x] + b) & 255
            elif kind == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif kind == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        seen.update(line[3::4])
        prev = line
    return 'partial' if seen - {0, 255} else 'clear' if 0 in seen else 'none'


def render_type(model, ns):
    """26.x draws a block model cutout or translucent by itself from its textures; 1.20.1 draws it solid unless told."""
    kinds = set()
    for texture in model.get('textures', {}).values():
        if isinstance(texture, str) and ':' in texture and texture.split(':', 1)[0] == ns:
            png = os.path.join(ASSETS, ns, 'textures', texture.split(':', 1)[1] + '.png')
            if os.path.exists(png):
                kinds.add(png_alpha(png))
    return 'minecraft:translucent' if 'partial' in kinds else 'minecraft:cutout' if 'clear' in kinds else None


def port_model(path, ns):
    model = load(path)
    changed = False
    for element in model.get('elements', []):
        if 'light_emission' in element:
            light = element.pop('light_emission')
            element['forge_data'] = {'block_light': light, 'sky_light': light}
            changed = True
    if 'render_type' not in model and os.sep + 'block' + os.sep in path:
        kind = render_type(model, ns)
        if kind:
            model['render_type'] = kind
            changed = True
    if changed:
        save(path, model)


def port_item_definitions(ns):
    items = os.path.join(ASSETS, ns, 'items')
    if not os.path.isdir(items):
        return
    for name in sorted(os.listdir(items)):
        definition = load(os.path.join(items, name))['model']
        target = os.path.join(ASSETS, ns, 'models', 'item', name)
        if definition.get('type') != 'minecraft:model':
            # A condition or select: 1.20.1 does it with item properties, in an item model kept by hand (the drone, the mob capsule)
            if not os.path.exists(target):
                sys.exit(f'{ns}:items/{name} is a {definition.get("type")}: give it an item model with overrides by hand')
            continue
        if definition['model'] != f'{ns}:item/{name[:-5]}':
            save(target, {'parent': definition['model']})
        elif not os.path.exists(target):
            sys.exit(f'{ns}:items/{name} points at a missing model')
    shutil.rmtree(items)


def port_lang(path):
    """Tags named in the texts are forge: ones on 1.20.1, and 1.20.1 names a villager profession by its own key."""
    with open(path, encoding='utf-8') as f:
        text = f.read()
    fixed = re.sub(r'(?<!\w)c:(?=[a-z])', 'forge:', text)
    fixed = re.sub(r'"entity\.(\w+)\.villager\.(\w+)"', r'"entity.minecraft.villager.\1.\2"', fixed)
    if fixed != text:
        with open(path, 'w', encoding='utf-8', newline='\n') as f:
            f.write(fixed)


def port_structure(path):
    with open(path, 'rb') as f:
        raw = gzip.decompress(f.read())
    fixed = raw
    for old, new in BLOCKS.items():
        o, n = old.encode(), new.encode()
        fixed = fixed.replace(len(o).to_bytes(2, 'big') + o, len(n).to_bytes(2, 'big') + n)
    key = b'\x03\x00\x0bDataVersion'
    at = fixed.find(key)
    if at >= 0:
        at += len(key)
        fixed = fixed[:at] + DATA_VERSION.to_bytes(4, 'big', signed=True) + fixed[at + 4:]
    if fixed != raw:
        with open(path, 'wb') as f:
            f.write(gzip.compress(fixed, mtime=0))


def port_handbook(pages):
    subprocess.run([sys.executable, os.path.join(ROOT, 'tools', 'patchouli_book.py'), pages], check=True)


def main():
    rename_folders()
    for base, _, files in os.walk(DATA):
        for name in files:
            path = os.path.join(base, name)
            parts = os.path.relpath(path, DATA).replace(os.sep, '/').split('/')
            if name.endswith('.json') and len(parts) > 2 and parts[1] == 'recipes':
                port_recipe(path)
            elif name.endswith('.json') and len(parts) > 2 and parts[1] == 'tags':
                port_tag(path)
            elif name.endswith('.nbt') and len(parts) > 2 and parts[1] == 'structures':
                port_structure(path)
    for ns in os.listdir(ASSETS):
        port_item_definitions(ns)
        lang = os.path.join(ASSETS, ns, 'lang')
        if os.path.isdir(lang):
            for name in os.listdir(lang):
                port_lang(os.path.join(lang, name))
        models = os.path.join(ASSETS, ns, 'models')
        for base, _, files in os.walk(models):
            for name in files:
                if name.endswith('.json'):
                    port_model(os.path.join(base, name), ns)
    guides = os.path.join(ASSETS, 'cyberfarm', 'guides')
    if len(sys.argv) > 1:
        port_handbook(sys.argv[1])
    elif os.path.isdir(guides):
        port_handbook(os.path.join(guides, 'cyberfarm', 'handbook'))
    if os.path.isdir(guides):
        shutil.rmtree(guides)


if __name__ == '__main__':
    main()
