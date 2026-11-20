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
deep_source=(ROOT/'preview/deep-catalog.js').read_text(encoding='ascii').strip()
prefix='window.STAR_DEEP_CATALOGUE='
assert deep_source.startswith(prefix) and deep_source.endswith(';')
deep=json.loads(deep_source[len(prefix):-1])
deep_meta=json.loads((ASSETS/'deep-catalogue.json').read_text(encoding='utf-8'))
deep_blob=(ASSETS/'stars-deep.bin.gz').read_bytes()
assert deep_blob==base64.b64decode(deep['data'],validate=True)
assert hashlib.sha256(deep_blob).hexdigest()==deep_meta['sha256']
planes=gzip.decompress(deep_blob)
assert len(planes)==deep_meta['count']*6 and deep_meta['count']==deep['count']==822917
assert hashlib.sha256(planes).hexdigest()==deep_meta['uncompressedSha256']
deep_counts={str(m):0 for m in range(3,12)}
count=deep_meta['count']
previous=-float('inf')
for i in range(count):
    dec=struct.unpack('<h',bytes((planes[count*2+i],planes[count*3+i])))[0]
    magnitude=planes[count*4+i]/16-2
    assert -32767<=dec<=32767 and previous<=magnitude<=11
    previous=magnitude
    for cutoff in deep_counts:
        if magnitude<=int(cutoff):deep_counts[cutoff]+=1
assert deep_counts==deep_meta['counts']==deep['counts']
assert meta['count']+deep_meta['count']==938487
print('PASS Android AT-HYG supplement equals the deep preview byte-for-byte; 938,487 combined stars and all 3–11 cutoffs verified.')
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
celestials=json.loads((ROOT/'data/celestials.json').read_text(encoding='utf-8'))
nebulae=celestials['nebulae']
native_nebulae=json.loads((ASSETS/'nebulae.json').read_text(encoding='utf-8'))
assert nebulae==native_nebulae and len(nebulae)==24
sprites=celestials['nebulaSprites']
assert len(sprites)==21 and set(sprites)<={item['id'] for item in nebulae}
for item in nebulae:
    asset=(ASSETS/(item['id']+'.png')).read_bytes()
    assert asset[:16]==b'\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR'
    assert struct.unpack('>II',asset[16:24])==(128,128)
    if item['id'] in sprites:
        assert asset==base64.b64decode(sprites[item['id']],validate=True)
        assert item['textureArcmin']>0
    else:
        assert 'textureArcmin' not in item
print('PASS Android and preview share 24 nebula coordinates and all 21 survey sprite PNG bytes.')
root=ET.parse(ROOT/'android/app/src/main/AndroidManifest.xml').getroot()
attr='{http://schemas.android.com/apk/res/android}'
assert root.find('uses-sdk').attrib[attr+'minSdkVersion']=='37'
assert root.find('uses-sdk').attrib[attr+'targetSdkVersion']=='37'
permissions={item.attrib[attr+'name'] for item in root.findall('uses-permission')}
assert permissions=={'android.permission.ACCESS_COARSE_LOCATION','android.permission.ACCESS_FINE_LOCATION','android.permission.ACCESS_BACKGROUND_LOCATION'}
print('PASS native catalogues: {:,} real records, SHA-256, coordinate ranges, ordered magnitudes and every 3–11 cutoff.'.format(meta['count']+deep_meta['count']))
print('PASS manifest: API 37 minimum/target; only location permissions; no Internet permission.')
