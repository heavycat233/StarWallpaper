"""Check the actual native payload, rather than a reconstructed rendering sample."""
import gzip
import base64
import hashlib
import json
from pathlib import Path
import struct
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'android/app/src/main/assets'
meta=json.loads((ASSETS/'catalogue.json').read_text(encoding='utf-8'))
blob=(ASSETS/'stars.bin.gz').read_bytes()
assert hashlib.sha256(blob).hexdigest()==meta['sha256']
records=gzip.decompress(blob)
assert len(records)==meta['count']*6
counts={str(m):0 for m in range(3,12)}
previous=-float('inf')
for ra,dec,mag,bv in struct.iter_unpack('<HhBB',records):
    magnitude=mag/16-2
    assert -32767<=dec<=32767 and previous<=magnitude<=11
    previous=magnitude
    for cutoff in counts:
        if magnitude<=int(cutoff):counts[cutoff]+=1
assert counts==meta['counts']
assert records==gzip.decompress(base64.b64decode((ROOT/'data/catalogue.b64').read_text(encoding='ascii')))
assert meta['count']==115570 and meta['source']=='HYG v4.1'
print('PASS every native record equals the preview, including order and quantization.')
figures_bytes=(ROOT/'data/constellations.json').read_bytes()
assert figures_bytes==(ASSETS/'constellations.json').read_bytes()
figures=json.loads(figures_bytes)['figures']
assert len(figures)==89 and len({item[0] for item in figures})==88
for code,name,lon,dec,paths in figures:
    assert code and name and -180<=lon<=180 and -90<=dec<=90
    assert paths and all(len(path)>=2 for path in paths)
    for path in paths:
        for ra,latitude in path:assert -180<=ra<=180 and -90<=latitude<=90
print('PASS Android and preview use the same 88 constellation figures, names and J2000 lines.')
root=ET.parse(ROOT/'android/app/src/main/AndroidManifest.xml').getroot()
attr='{http://schemas.android.com/apk/res/android}'
assert root.find('uses-sdk').attrib[attr+'minSdkVersion']=='37'
assert root.find('uses-sdk').attrib[attr+'targetSdkVersion']=='37'
permissions={item.attrib[attr+'name'] for item in root.findall('uses-permission')}
assert permissions=={'android.permission.ACCESS_COARSE_LOCATION','android.permission.ACCESS_FINE_LOCATION','android.permission.ACCESS_BACKGROUND_LOCATION'}
print('PASS native catalogue: {:,} real records, SHA-256, coordinate ranges, ordered magnitudes and every 3–11 cutoff.'.format(meta['count']))
print('PASS manifest: API 37 minimum/target; only location permissions; no Internet permission.')
