"""Validate that every declared app content destination has an independent bucket key."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'apps/hamyar-app/src/main/assets/content'
manifest = json.loads((ASSETS / 'bucket-route-manifest.json').read_text(encoding='utf-8'))
menu = json.loads((ASSETS / 'books-menu.json').read_text(encoding='utf-8'))
server = json.loads((ASSETS / 'server-map.json').read_text(encoding='utf-8'))
fallback = 'Bucket/Html-files/spaceholder.html'
assert manifest['fallback'] == fallback
records = manifest['records']
keys = {r['key'] for r in records}
assert all(isinstance(k, str) and k and '..' not in k.split('/') for k in keys)
assert fallback in keys
ids = [r['id'] for r in server['entries']]
assert len(ids) == len(set(ids)), 'Conflicting file identifiers'
assert all(r['key'] in keys for r in server['entries'])
tabs = 0
leaves = 0
def walk(nodes):
    global tabs, leaves
    for node in nodes:
        children = node.get('children', [])
        node_tabs = node.get('tabs', [])
        for tab in node_tabs:
            assert tab.get('key') and tab['key'] != fallback, tab
            assert tab['key'] in keys
            tabs += 1
        if not children and not node_tabs:
            assert node.get('key') and node['key'] != fallback, node
            assert node['key'] in keys
            leaves += 1
        walk(children)
for book in menu['books']:
    walk(book['items'])
assert not list((ROOT / 'apps/hamyar-app/src/main/assets').rglob('*.html')), 'Bundled HTML payload remains'
assert not list((ROOT / 'shared/feature-study/src/main/java').rglob('books/*.kt')), 'Bundled lesson payload remains'
print(json.dumps({'version': manifest['version'], 'references': len(records), 'uniqueKeys': len(keys),
                  'menuTabs': tabs, 'leafDestinations': leaves, 'books': len(menu['books'])}))
