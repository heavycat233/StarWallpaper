"""Prepare compact, sourced celestial data for the offline conversation preview."""
import base64
import csv
import gzip
import json
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
selected={
 'NGC1976':('猎户座大星云 M42',[145,92,178],1.35),
 'NGC6523':('礁湖星云 M8',[177,71,105],1.1),
 'NGC6514':('三叶星云 M20',[146,84,161],1.1),
 'NGC7000':('北美洲星云',[149,62,94],.95),
 'NGC1499':('加州星云',[157,61,88],.9),
 'NGC2237':('玫瑰星云',[166,68,105],1.2),
 'NGC3372':('船底座星云',[145,105,145],1.2),
 'NGC2070':('蜘蛛星云',[128,116,163],1.0),
 'IC1805':('心脏星云',[153,67,103],1.1),
 'IC1848':('灵魂星云',[147,70,109],1.0),
 'NGC6960':('西面纱星云',[85,138,162],.8),
 'NGC6992':('东面纱星云',[96,133,170],.9),
}
def angle(text,ra=False):
    sign=-1 if text.startswith('-') else 1
    values=[float(p) for p in text.lstrip('+-').split(':')]
    value=values[0]+values[1]/60+values[2]/3600
    return value if ra else sign*value
nebulae=[]
with (ROOT/'data/OpenNGC.csv').open(encoding='utf-8') as stream:
    for row in csv.DictReader(stream,delimiter=';'):
        if row['Name'] not in selected:continue
        name,color,intensity=selected[row['Name']]
        major=float(row['MajAx'] or 60);minor=float(row['MinAx'] or major)
        nebulae.append({'id':row['Name'],'name':name,'ra':round(angle(row['RA'],True),7),'dec':round(angle(row['Dec']),7),'major':major,'minor':minor,'pa':float(row['PosAng'] or 0),'color':color,'intensity':intensity})
assert len(nebulae)==len(selected)
moon=Image.open(ROOT/'data/moon-lroc-2k.jpg').convert('L').resize((256,128),Image.Resampling.LANCZOS)
texture=base64.b64encode(gzip.compress(moon.tobytes(),9,mtime=0)).decode('ascii')
data={'nebulae':nebulae,'moonTexture':{'width':256,'height':128,'data':texture},'nebulaSource':'Mattia Verga / OpenNGC, CC BY-SA 4.0; selected coordinates and angular extents; artistic colors and procedural structure','moonTextureSource':'NASA SVS CGI Moon Kit / LROC WAC, grayscale downsample of lroc_color_2k.jpg, https://svs.gsfc.nasa.gov/4720/'}
(ROOT/'data/celestials.json').write_text(json.dumps(data,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
print(f'Prepared {len(nebulae)} real nebula positions and {len(texture):,} bytes of lunar albedo.')
