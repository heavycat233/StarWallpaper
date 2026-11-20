"""Build an optional AT-HYG v3.2 supplement for the comparison preview.

The large upstream CSVs are local build inputs, excluded from the repository.
The generated, CC BY-SA 4.0 derivative contains only stars without HYG links.
"""
import base64
import csv
import gzip
import hashlib
import json
import math
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCES = [ROOT / 'data' / f'athyg_v32-{number}.csv.gz' for number in (1, 2)]
for source in SOURCES:
    if not source.is_file():
        raise SystemExit(f'Missing {source.name}; run tools/download_deep_catalog.py first')

stars = []
raw_rows = 0
counts_by_source = {}
fieldnames = None
for index, path in enumerate(SOURCES):
    with gzip.open(path, 'rt', encoding='utf-8-sig', newline='') as handle:
        reader = csv.DictReader(handle, fieldnames=fieldnames)
        if index == 0:
            fieldnames = reader.fieldnames
        for row in reader:
            raw_rows += 1
            if row['hyg'] or not row['ra'] or not row['dec'] or not row['mag']:
                continue
            mag = float(row['mag'])
            source_name = row['mag_src']
            ci = float(row['ci']) if row['ci'] else None
            if source_name == 'T' and ci is not None:
                # ESA SP-1200: approximate Tycho BT/VT -> Johnson B/V.
                mag -= .09 * ci
                bv = .85 * ci
            else:
                bv = ci if ci is not None else .65
            if not math.isfinite(mag) or mag > 11:
                continue
            ra, dec = float(row['ra']), float(row['dec'])
            if not 0 <= ra < 24 or not -90 <= dec <= 90:
                continue
            stars.append((mag, ra, dec, bv))
            counts_by_source[source_name] = counts_by_source.get(source_name, 0) + 1

stars.sort()
packed = bytearray(len(stars) * 6)
for i, (mag, ra, dec, bv) in enumerate(stars):
    struct.pack_into('<HhBB', packed, i * 6,
                     round(ra / 24 * 65535), round(dec / 90 * 32767),
                     max(0, min(255, math.ceil((mag + 2) * 16))),
                     max(0, min(255, round((bv + .5) * 64))))

n = len(stars)
planes = b''.join(packed[offset::6] for offset in range(6))
encoded = base64.b64encode(gzip.compress(planes, compresslevel=9, mtime=0)).decode('ascii')
base = json.loads((ROOT / 'data/catalogue-meta.json').read_text(encoding='utf-8'))
meta = {
    'source': 'AT-HYG v3.2 (Tycho-2, Gaia DR3 and HYG); HYG v4.1 base',
    'rawRows': raw_rows,
    'additionalCount': n,
    'baseCount': base['count'],
    'combinedCount': n + base['count'],
    'counts': {str(m): sum(star[0] <= m for star in stars) for m in range(3, 12)},
    'recordBytes': 6,
    'packedLayout': 'byte-planes',
    'magSources': counts_by_source,
    'selection': 'AT-HYG rows without HYG cross-reference, approximate Johnson V <= 11',
    'photometry': 'For Tycho mag_src=T, V=VT-0.090*(BT-VT), B-V=0.850*(BT-VT); ESA SP-1200 approximation',
    'license': 'CC BY-SA 4.0; David Nash / Astronomy Nexus, AT-HYG contributors',
    'sourceSha256': {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in SOURCES},
    'encodedBytes': len(encoded),
}
(ROOT / 'data/athyg-preview-meta.json').write_text(json.dumps(meta, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
(ROOT / 'preview/deep-catalog.js').write_text('window.STAR_DEEP_CATALOGUE=' + json.dumps({
    'count': n, 'counts': meta['counts'], 'data': encoded,
}, separators=(',', ':')) + ';\n', encoding='ascii')
print(f'Built AT-HYG supplement: {n:,} stars, {n + base["count"]:,} combined, {len(encoded):,} encoded bytes')
