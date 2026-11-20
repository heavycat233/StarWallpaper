"""Fetch and colorize Finkbeiner H-alpha cutouts for offline nebula sprites.

The survey values set the structure, while colors and edge fading are artistic.
SkyView FITS cutouts are north-up with RA increasing to the left; the packed
sprites are flipped horizontally for the app's east/north projection basis.
"""
import base64
import hashlib
import io
import json
import math
import struct
import time
import urllib.parse
import urllib.request
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'data'
CACHE=DATA/'halpha-raw'
CACHE.mkdir(exist_ok=True)
objects=json.loads((DATA/'celestials.json').read_text(encoding='utf-8'))['nebulae']
result={}

def fetch(url):
    for attempt in range(3):
        try:
            with urllib.request.urlopen(url,timeout=75) as response:
                data=response.read()
            if not data.startswith(b'SIMPLE  ='):
                raise ValueError('SkyView returned an error page instead of FITS')
            return data
        except Exception:
            if attempt==2:raise
            time.sleep(2*(attempt+1))

def fits_image(blob):
    cards=[blob[i:i+80].decode('ascii','replace') for i in range(0,len(blob),80)]
    end=next(i for i,card in enumerate(cards) if card.startswith('END'))
    header={card[:8].strip():card[10:30].strip().strip("'") for card in cards[:end] if card[8:10]=='= '}
    width,height=int(header['NAXIS1']),int(header['NAXIS2'])
    assert width==height==128 and int(header['BITPIX'])==-32
    assert header['CTYPE1'].startswith('RA') and header['CTYPE2'].startswith('DEC')
    assert float(header['CDELT1'])<0<float(header['CDELT2'])
    start=((end+1)*80+2879)//2880*2880
    values=[item[0] for item in struct.iter_unpack('>f',blob[start:start+width*height*4])]
    assert len(values)==width*height
    return header,values

def quantile(values,fraction):
    return values[round((len(values)-1)*fraction)]

for item in objects:
    size_arcmin=round(max(25,max(item['major'],item['minor'])*1.2),1)
    params=urllib.parse.urlencode({'Survey':'H-alpha','Position':f"{item['ra']*15:.7f},{item['dec']:.7f}",'Size':f'{size_arcmin/60:.5f}','Pixels':128,'Return':'FITS'})
    url='https://skyview.gsfc.nasa.gov/cgi-bin/images?'+params
    path=CACHE/(item['id']+'.fits')
    stamp=CACHE/(item['id']+'.url')
    if path.is_file() and stamp.is_file() and stamp.read_text(encoding='ascii')==url:
        blob=path.read_bytes()
    else:
        blob=fetch(url)
        path.write_bytes(blob)
        stamp.write_text(url,encoding='ascii')
    try:
        header,values=fits_image(blob)
    except (KeyError,ValueError,AssertionError):
        print(f"{item['id']}: no usable H-alpha coverage; keeping procedural texture",flush=True)
        continue
    assert abs(float(header['CRVAL1'])-item['ra']*15)<.001
    assert abs(float(header['CRVAL2'])-item['dec'])<.001
    finite=sorted(value for value in values if math.isfinite(value))
    low,high=quantile(finite,.25),quantile(finite,.99)
    assert high>low
    image=Image.new('RGBA',(128,128))
    pixels=image.load()
    for y in range(128):
        for x in range(128):
            value=values[y*128+(127-x)]
            signal=max(0,min(1,(value-low)/(high-low))) if math.isfinite(value) else 0
            # Both axes fade to transparent before the square cutout edge.
            edge_x=min(1,(min(x,127-x)/16)**2)
            edge_y=min(1,(min(y,127-y)/16)**2)
            strength=signal**.58*edge_x*edge_y
            color=[min(255,round(component*(.88+.25*math.sqrt(signal)))) for component in item['color']]
            pixels[x,y]=(*color,min(255,round(228*strength*item['intensity'])))
    output=io.BytesIO();image.save(output,format='PNG',optimize=True)
    result[item['id']]={'png':base64.b64encode(output.getvalue()).decode('ascii'),'sizeArcmin':size_arcmin,'fitsSha256':hashlib.sha256(blob).hexdigest(),'url':url,'backgroundRayleigh':round(low,3),'highRayleigh':round(high,3)}
    print(f"{item['id']}: {size_arcmin:g}' H-alpha, {low:.1f}–{high:.1f} R, {len(output.getvalue()):,} PNG bytes",flush=True)

atlas={'source':'Douglas P. Finkbeiner, Full-Sky H-alpha Composite v1.1 (WHAM/VTSS/SHASSA), accessed through NASA SkyView','sourceUrl':'https://skyview.gsfc.nasa.gov/current/help/faq.html','processing':'FITS H-alpha intensity (Rayleigh), per-cutout 25th–99th percentile normalization, horizontal WCS flip, artistic tint and edge fade','sprites':result}
(DATA/'halpha-nebula-sprites.json').write_text(json.dumps(atlas,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
print(f'Prepared {len(result)} survey-backed nebula sprites.',flush=True)
