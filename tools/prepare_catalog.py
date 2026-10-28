"""Prepare the HYG v4.1 preview catalogue; preserve every real star through V=11."""
import base64
import csv
import gzip
import json
import math
from pathlib import Path
import struct

ROOT = Path(__file__).resolve().parents[1]
stars = []
named = []
with (ROOT / 'data/hygdata_v41.csv').open(encoding='utf-8') as source:
    for row in csv.DictReader(source):
        if row['id'] == '0' or not row['mag'] or not row['ra'] or not row['dec']:
            continue
        mag = float(row['mag'])
        if mag > 11:
            continue
        ra, dec = float(row['ra']), float(row['dec'])
        bv = float(row['ci']) if row['ci'] else 0.65
        stars.append((ra, dec, mag, bv))
        if row['proper'] and mag < 2.6:
            named.append([row['proper'], round(ra, 6), round(dec, 6), mag])
stars.sort(key=lambda item: item[2])
packed = bytearray()
for ra, dec, mag, bv in stars:
    packed.extend(struct.pack('<HhBB', round(ra / 24 * 65535), round(dec / 90 * 32767), math.ceil((mag + 2) * 16), max(0, min(255, round((bv + .5) * 64)))))
encoded = base64.b64encode(gzip.compress(packed, compresslevel=9, mtime=0)).decode('ascii')
meta = {'source': 'HYG v4.1', 'count': len(stars), 'recordBytes': 6, 'counts': {str(m): sum(s[2] <= m for s in stars) for m in range(3, 12)}, 'named': named, 'base64Bytes': len(encoded)}
(ROOT / 'data/catalogue.b64').write_text(encoded, encoding='ascii')
(ROOT / 'data/catalogue-meta.json').write_text(json.dumps(meta, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps({k: v for k, v in meta.items() if k != 'named'}))
