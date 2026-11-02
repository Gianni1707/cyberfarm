"""Turn the GuideME handbook (markdown pages) of the 26.x branches into the Patchouli book of 1.20.1.

    python tools/patchouli_book.py <folder of the GuideME .md pages>

for example the master checkout's src/main/resources/assets/cyberfarm/guides/cyberfarm/handbook.
Patchouli does not wrap text onto a new page, so each page's text is measured with the vanilla
font widths and split before it would overflow.
"""
import glob
import json
import os
import re
import shutil
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = ROOT + '/src/main/resources'
ASSETS = RES + '/assets/cyberfarm'
SRC = sys.argv[1]
BOOK_DATA = RES + '/data/cyberfarm/patchouli_books/handbook/book.json'
BOOK = ASSETS + '/patchouli_books/handbook/en_us'

LANG = json.load(open(ASSETS + '/lang/en_us.json', encoding='utf-8'))

# --- text measuring: vanilla font advances (glyph + 1 px), bold one more ---
NARROW = {**{c: 2 for c in "!',.:;i|"}, **{c: 3 for c in 'l`'}, **{c: 4 for c in 'It[] '},
          **{c: 5 for c in '"()*<>fk{}'}, **{c: 7 for c in '@~'}}
PAGE_WIDTH = 112  # 116 in Patchouli, minus a margin
LIST_INDENT = 10
LINE_LIMITS = {'first': 13, 'titled': 15, 'plain': 16, 'spotlight': 11}


def char_width(c, bold):
    return NARROW.get(c, 6) + (1 if bold else 0)


def word_width(w, bold):
    return sum(char_width(c, bold) for c in w)


# --- inline markdown -> tokens: (word, style) where style = (bold, italic, link, item) ---
def item_name(item_id):
    item_id = item_id if ':' in item_id else 'cyberfarm:' + item_id
    ns, path = item_id.split(':')
    for kind in ('item', 'block'):
        if f'{kind}.{ns}.{path}' in LANG:
            return LANG[f'{kind}.{ns}.{path}']
    return path.replace('_', ' ').title()


ITEM_PAGES = {}  # item id -> entry id of the page that lists it in item_ids
PAGE_ENTRY = {}  # 'foo.md' -> entry id


def link_target(href):
    href = href.split('#')[0]
    if href.startswith('http'):
        return href
    return PAGE_ENTRY[os.path.basename(href)]


INLINE = re.compile(r'<ItemLink id="([^"]+)" */>|\[([^\]]+)\]\(([^)]+)\)|\*\*(.+?)\*\*|\*(.+?)\*|`([^`]+)`')


def tokens(text, current_entry, bold=False, italic=False, link=None):
    """Split inline markdown into words, each with the style it is drawn in."""
    out = []
    pos = 0

    def plain(s, b, i, l, item=False):
        for part in re.split(r'(\s+)', s):
            if part.strip():
                out.append([part, (b, i, l, item), False])
            elif part and out:
                out[-1][2] = True  # a space follows this word

    for m in INLINE.finditer(text):
        plain(text[pos:m.start()], bold, italic, link)
        item_id, ltext, href, btext, itext, code = m.groups()
        if item_id:
            full = item_id if ':' in item_id else 'cyberfarm:' + item_id
            target = ITEM_PAGES.get(full)
            if target and target != current_entry:
                plain(item_name(item_id), False, False, target)
            else:
                plain(item_name(item_id), False, False, None, item=True)
        elif ltext:
            out.extend(tokens(ltext, current_entry, False, False, link_target(href)))
        elif btext:
            out.extend(tokens(btext, current_entry, link is None, italic, link))
        elif itext:
            out.extend(tokens(itext, current_entry, bold, True, link))
        else:
            plain(code, bold, italic, link)
        pos = m.end()
        if pos < len(text) and text[pos].isspace() and out:
            out[-1][2] = True
    plain(text[pos:], bold, italic, link)
    return out


def render(toks):
    """Patchouli text for tokens, opening and closing styles where they change."""
    s = ''
    cur = (False, False, None, False)
    space = False
    for word, style, next_space in toks:
        if style != cur:
            if cur[2] and cur[2] != style[2]:
                s += '$(/l)'
            if cur[0] or cur[1] or cur[3]:
                s += '$()'
            # The space goes after the closing codes, so a link or bold word does not take it
            s += ' ' if space else ''
            space = False
            if style[2] and style[2] != cur[2]:
                s += f'$(l:{style[2]})'
            if style[0]:
                s += '$(l)'
            if style[1]:
                s += '$(o)'
            if style[3]:
                s += '$(item)'
            cur = style
        s += (' ' if space else '') + word
        space = next_space
    if cur[2]:
        s += '$(/l)'
    if cur[0] or cur[1] or cur[3]:
        s += '$()'
    return s


# --- a page under construction: paragraphs of tokens, measured as they are added ---
class TextPage:
    def __init__(self, kind, title=None):
        self.kind = kind
        self.title = title
        self.lines = 0
        self.paras = []  # (prefix, tokens)

    def limit(self):
        return LINE_LIMITS[self.kind]

    def fit(self, toks, indent):
        """How many of toks fit on this page, and the lines they take."""
        width = PAGE_WIDTH - indent
        lines, x, n = 1, 0, 0
        for word, style, space in toks:
            w = word_width(word, style[0])
            if x and x + w > width:
                lines += 1
                x = 0
            if self.lines + lines > self.limit():
                return n, lines - 1
            x += w + (4 if space else 0)
            n += 1
        return n, lines


class Entry:
    def __init__(self, entry_id):
        self.entry_id = entry_id
        self.pages = []
        self.page = None
        self.pending_title = None

    def new_text_page(self, kind=None):
        self.close()
        if kind is None:
            kind = 'first' if not self.pages else ('titled' if self.pending_title else 'plain')
        title = self.pending_title if kind != 'first' else None
        self.page = TextPage(kind, title)
        if kind == 'first' and self.pending_title:
            self.add_para([[self.pending_title, (True, False, None, False), False]])
        self.pending_title = None

    def add_para(self, toks, bullet=False):
        if not toks:
            return
        while toks:
            if self.page is None:
                self.new_text_page()
            # A blank line between paragraphs, none next to a list item (see close)
            gap = 1 if self.page.paras and not bullet and not self.page.paras[-1][0] else 0
            self.page.lines += gap
            n, lines = self.page.fit(toks, LIST_INDENT if bullet else 0)
            # A paragraph that would fit whole on a new page is not split
            if 0 < n < len(toks) and (self.page.paras or self.page.kind == 'spotlight') and TextPage('plain').fit(toks, LIST_INDENT if bullet else 0)[0] == len(toks):
                n = 0
            if n == 0:
                self.page.lines -= gap
                if not self.page.paras and self.page.kind != 'spotlight':
                    raise ValueError(f'{self.entry_id}: a word wider than a page')
                self.new_text_page()
                continue
            self.page.paras.append(('$(li)' if bullet else '', toks[:n]))
            self.page.lines += lines
            toks = toks[n:]
            if toks:
                self.new_text_page()

    def close(self):
        p = self.page
        self.page = None
        if p is None or not p.paras:
            return
        text = ''
        for i, (prefix, toks) in enumerate(p.paras):
            if i:
                text += '$(br)' if prefix or p.paras[i - 1][0] else '$(br2)'
            text += prefix + render(toks)
        if p.kind == 'spotlight':
            self.pages[-1]['text'] = text
        else:
            page = {'type': 'patchouli:text', 'text': text}
            if p.title:
                page['title'] = p.title
            self.pages.append(page)

    def add_page(self, page):
        self.close()
        self.pages.append(page)

    def heading(self, text):
        self.close()
        self.pending_title = text


# --- recipes, by the item they make ---
RECIPES = {}
for p in sorted(glob.glob(RES + '/data/cyberfarm/recipes/*.json')):
    d = json.load(open(p, encoding='utf-8'))
    r = d['result']
    item = r if isinstance(r, str) else r['item']
    kind = {'minecraft:crafting_shaped': 'crafting', 'minecraft:crafting_shapeless': 'crafting',
            'minecraft:smelting': 'smelting', 'minecraft:blasting': 'blasting'}[d['type']]
    RECIPES.setdefault(item, []).append((kind, 'cyberfarm:' + os.path.basename(p)[:-5]))


# --- the pages ---
def front_matter(text):
    m = re.match(r'---\n(.*?)\n---\n', text, re.S)
    meta = {'item_ids': []}
    for line in m.group(1).splitlines():
        line = line.strip()
        if line.startswith('- '):
            meta['item_ids'].append(line[2:].strip())
        elif ':' in line and not line.endswith(':'):
            k, v = line.split(':', 1)
            meta[k.strip()] = v.strip()
    return meta, text[m.end():]


PAGES = {}
for p in glob.glob(SRC + '/*.md'):
    with open(p, encoding='utf-8') as f:
        text = f.read().replace('\r\n', '\n')
    text = re.sub(r'(?<![A-Za-z0-9_])(#?)c:([a-z_])', r'\1forge:\2', text)
    PAGES[os.path.basename(p)] = front_matter(text)

CATEGORY_PAGES = [n for n, (meta, _) in PAGES.items() if 'parent' not in meta and n != 'index.md']
CATEGORY_PAGES.sort(key=lambda n: int(PAGES[n][0]['position']))


def category_of(name):
    meta = PAGES[name][0]
    if name in CATEGORY_PAGES:
        return name[:-3]
    return 'basics' if meta['parent'] == 'index.md' else meta['parent'][:-3]


for name in PAGES:
    if name != 'index.md':
        PAGE_ENTRY[name] = f'{category_of(name)}/{name[:-3]}'
PAGE_ENTRY['index.md'] = 'basics/welcome'
for name, (meta, _) in PAGES.items():
    for item in meta['item_ids']:
        ITEM_PAGES.setdefault(item, PAGE_ENTRY[name])

ATTR = re.compile(r'([\w:]+)="([^"]*)"')


def multiblock(blocks):
    """A Patchouli multiblock page for the blocks of a GameScene."""
    xs = [b[0] for b in blocks]
    ys = [b[1] for b in blocks]
    zs = [b[2] for b in blocks]
    x0, y0, z0 = min(xs), min(ys), min(zs)
    w, h, d = max(xs) - x0 + 1, max(ys) - y0 + 1, max(zs) - z0 + 1
    at = {(x - x0, y - y0, z - z0): state for x, y, z, state in blocks}
    chars = {}
    alphabet = iter('ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz123456789')
    for state in at.values():
        if state not in chars:
            chars[state] = next(alphabet)
    # The centre: the block nearest the middle of the bottom layer
    centre = min(at, key=lambda k: (k[1], abs(k[0] - (w - 1) / 2) + abs(k[2] - (d - 1) / 2)))
    pattern = []
    for y in reversed(range(h)):
        layer = []
        for z in range(d):
            row = ''
            for x in range(w):
                if (x, y, z) == centre:
                    row += '0'
                else:
                    row += chars[at[(x, y, z)]] if (x, y, z) in at else ' '
            layer.append(row)
        pattern.append(layer)
    mapping = {c: s for s, c in chars.items()}
    mapping['0'] = at[centre]
    return {'pattern': pattern, 'mapping': mapping}


def entity_page(e):
    return {'type': 'patchouli:entity', 'entity': e, 'name': LANG.get('entity.cyberfarm.' + e.split(':')[1], e)}


def convert(name, meta, body, entry):
    lines = body.split('\n')
    i = 0
    para = []
    recipe_row = []

    def flush_para():
        if para:
            entry.add_para(tokens(' '.join(para), entry.entry_id))
            para.clear()

    def flush_recipes():
        # Two recipes of a kind share a page
        while recipe_row:
            kind, rid = recipe_row.pop(0)
            page = {'type': f'patchouli:{kind}', 'recipe': rid}
            if recipe_row and recipe_row[0][0] == kind:
                page['recipe2'] = recipe_row.pop(0)[1]
            entry.add_page(page)

    while i < len(lines):
        line = lines[i]
        s = line.strip()
        if not s or s.startswith('<br') or s in ('<Row>', '</Row>') or s.startswith('<SubPages'):
            flush_para()
            if s == '</Row>':
                flush_recipes()
            i += 1
            continue
        if not s.startswith('<RecipeFor'):
            flush_recipes()
        if s.startswith('#'):
            flush_para()
            level = len(s) - len(s.lstrip('#'))
            text = s.lstrip('#').strip()
            if level == 1:
                pass  # the entry's own name
            elif level == 2:
                entry.heading(text)
            else:
                toks = tokens(text, entry.entry_id)
                if all(t[1][2] is None for t in toks):
                    for t in toks:
                        t[1] = (True,) + t[1][1:]
                entry.add_para(toks)
        elif s.startswith('<RecipeFor'):
            flush_para()
            item = ATTR.search(s).group(2)
            item = item if ':' in item else 'cyberfarm:' + item
            found = RECIPES.get(item)
            if found:
                recipe_row.append(found[0])
            else:
                print(f'  {name}: no crafting recipe for {item}')
        elif s.startswith('<ItemImage') or s.startswith('<BlockImage'):
            flush_para()
            item = dict(ATTR.findall(s))['id']
            page = {'type': 'patchouli:spotlight', 'item': item}
            if entry.pending_title:
                page['title'] = entry.pending_title
                entry.pending_title = None
            entry.add_page(page)
            entry.page = TextPage('spotlight')
        elif s.startswith('<GameScene'):
            flush_para()
            blocks, entities = [], []
            i += 1
            while not lines[i].strip().startswith('</GameScene'):
                a = dict(ATTR.findall(lines[i]))
                if lines[i].strip().startswith('<Block'):
                    props = ','.join(f'{k[2:]}={v}' for k, v in a.items() if k.startswith('p:'))
                    state = a['id'] + (f'[{props}]' if props else '')
                    blocks.append((int(a.get('x', 0)), int(a.get('y', 0)), int(a.get('z', 0)), state))
                elif lines[i].strip().startswith('<Entity'):
                    entities.append(a['id'])
                i += 1
            if blocks:
                page = {'type': 'patchouli:multiblock', 'name': entry.pending_title or meta['title'],
                        'multiblock': multiblock(blocks), 'enable_visualize': False}
                entry.pending_title = None
                entry.add_page(page)
            for e in entities:
                if e.startswith('cyberfarm:'):
                    entry.add_page(entity_page(e))
        elif s.startswith('<Entity'):
            flush_para()
            e = dict(ATTR.findall(s))['id']
            entry.add_page(entity_page(e))
        elif s.startswith('<ItemGrid'):
            flush_para()
            i += 1
            while not lines[i].strip().startswith('</ItemGrid'):
                a = dict(ATTR.findall(lines[i]))
                if 'id' in a:
                    entry.add_para(tokens(f'<ItemLink id="{a["id"]}" />', entry.entry_id), bullet=True)
                i += 1
        elif s.startswith('|'):
            flush_para()
            rows = []
            while i < len(lines) and lines[i].strip().startswith('|'):
                rows.append([c.strip() for c in lines[i].strip().strip('|').split('|')])
                i += 1
            for row in rows[2:]:
                first = f'**{row[0]}**' if '](' not in row[0] and '<' not in row[0] else row[0]
                entry.add_para(tokens(first + ': ' + ', '.join(row[1:]), entry.entry_id), bullet=True)
            continue
        elif re.match(r'(- |\d+\. )', s):
            flush_para()
            item = [re.sub(r'^(- |\d+\. )', '', s)]
            i += 1
            while i < len(lines) and lines[i].startswith('  ') and lines[i].strip() and not re.match(r'(- |\d+\. )', lines[i].strip()):
                item.append(lines[i].strip())
                i += 1
            entry.add_para(tokens(' '.join(item), entry.entry_id), bullet=True)
            continue
        elif s.startswith('<') and not s.startswith('<ItemLink'):
            raise ValueError(f'{name}: unknown tag {s}')
        else:
            para.append(s)
        i += 1
    flush_para()
    flush_recipes()
    entry.close()


shutil.rmtree(ASSETS + '/patchouli_books', ignore_errors=True)


def save(p, d):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(d, f, indent=2, ensure_ascii=False)
        f.write('\n')


# The welcome entry is the index page; its first paragraph is the landing text
index_meta, index_body = PAGES['index.md']
landing = re.search(r'\n\n(Welcome.*?)\n\n', index_body, re.S).group(1)
landing_text = render(tokens(' '.join(landing.split('\n')), 'basics/welcome'))

save(BOOK_DATA, {
    'name': 'cyberfarm.guide.handbook',
    'landing_text': landing_text,
    'subtitle': 'Everything about drones, power and machines',
    'model': 'cyberfarm:handbook',
    'creative_tab': 'cyberfarm:main',
    'show_progress': False,
    'use_resource_pack': True,
    'i18n': False,
})

categories = [('basics', 'Basics', index_meta['icon'], 'Your first drone, and the machines every base wants early on.')]
for n in CATEGORY_PAGES:
    meta, body = PAGES[n]
    first = re.search(r'\n\n([^#<\n|-][^\n]*(?:\n[^\n]+)*)', body).group(1)
    desc = render(tokens(' '.join(first.split('\n')), ''))
    categories.append((n[:-3], meta['title'], meta['icon'], desc))
for sort, (cid, title, icon, desc) in enumerate(categories):
    save(f'{BOOK}/categories/{cid}.json', {'name': title, 'description': desc, 'icon': icon, 'sortnum': sort})

for name, (meta, body) in sorted(PAGES.items()):
    entry_id = PAGE_ENTRY[name]
    entry = Entry(entry_id)
    convert(name, meta, body, entry)
    if name == 'index.md':
        sort = -1
    elif name in CATEGORY_PAGES:
        sort = 0
    else:
        sort = int(meta.get('position', 50)) + 1
    data = {'name': 'Welcome' if name == 'index.md' else meta['title'], 'icon': meta['icon'],
            'category': 'cyberfarm:' + entry_id.split('/')[0], 'sortnum': sort, 'pages': entry.pages}
    if name in CATEGORY_PAGES or name == 'index.md':
        data['priority'] = True
    mappings = {}
    for item in meta['item_ids']:
        at = next((k for k, pg in enumerate(entry.pages) if item in (pg.get('item'), pg.get('recipe'), pg.get('recipe2'))), 0)
        if at == 0:
            recipe = RECIPES.get(item, [(None, None)])[0][1]
            at = next((k for k, pg in enumerate(entry.pages) if recipe and recipe in (pg.get('recipe'), pg.get('recipe2'))), 0)
        mappings[item] = at
    if mappings:
        data['extra_recipe_mappings'] = mappings
    save(f'{BOOK}/entries/{entry_id}.json', data)
    print(f'{entry_id}: {len(entry.pages)} pages')
