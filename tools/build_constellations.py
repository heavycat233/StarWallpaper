"""Convert d3-celestial J2000 GeoJSON into the shared compact sky overlay."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'data'


def build():
    lines = json.loads((DATA / 'constellations.lines.upstream.json').read_text(encoding='utf-8'))
    names = json.loads((DATA / 'constellations.upstream.json').read_text(encoding='utf-8'))
    by_id = {feature['id']: feature for feature in names['features']}
    figures = []
    for feature in lines['features']:
        code = feature['id']
        name = by_id[code]
        geometry = feature['geometry']
        assert geometry['type'] == 'MultiLineString', code
        assert name['geometry']['type'] == 'Point', code
        paths = geometry['coordinates']
        assert paths and all(len(path) >= 2 for path in paths), code
        lon, dec = name['geometry']['coordinates']
        figures.append([code, name['properties']['zh'], lon, dec, paths])
    # Serpens has two separate line figures under one IAU constellation name.
    assert len(figures) == 89 and len(by_id) == 88
    output = {'source': 'd3-celestial / Olaf Frohn / BSD-3-Clause / J2000', 'figures': figures}
    encoded = json.dumps(output, ensure_ascii=False, separators=(',', ':')) + '\n'
    (DATA / 'constellations.json').write_text(encoded, encoding='utf-8')
    (ROOT / 'android/app/src/main/assets/constellations.json').write_text(encoded, encoding='utf-8')
    print(f'Built 89 constellation figures ({len(encoded.encode("utf-8")):,} bytes)')


if __name__ == '__main__':
    build()
