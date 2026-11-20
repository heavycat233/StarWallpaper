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
 'NGC0281':('吃豆人星云',[160,69,103],1.0),
 'NGC1952':('蟹状星云 M1',[126,111,159],1.15),
 'NGC1977':('奔跑人星云',[101,129,175],1.0),
 'NGC2024':('火焰星云',[180,82,89],1.15),
 'NGC2174':('猴头星云',[159,73,114],1.0),
 'NGC2359':('雷神头盔星云',[118,132,176],1.0),
 'NGC6611':('鹰状星云 M16',[172,84,107],1.1),
 'NGC6618':('欧米茄星云 M17',[179,83,105],1.15),
 'NGC6888':('新月星云',[121,137,171],1.0),
 'NGC7293':('螺旋星云',[120,144,166],1.05),
 'NGC7635':('气泡星云',[151,98,153],1.05),
 'IC5146':('茧星云',[158,87,123],1.0),
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
atlas_path=ROOT/'data/halpha-nebula-sprites.json'
if atlas_path.is_file():
    atlas=json.loads(atlas_path.read_text(encoding='utf-8'))
    assert set(atlas['sprites'])<={item['id'] for item in nebulae}
    data['nebulaSprites']={key:value['png'] for key,value in atlas['sprites'].items()}
    for item in nebulae:
        if item['id'] in atlas['sprites']:item['textureArcmin']=atlas['sprites'][item['id']]['sizeArcmin']
    data['nebulaSource']='Mattia Verga / OpenNGC, CC BY-SA 4.0; H-alpha sprite structure from Douglas Finkbeiner full-sky H-alpha composite via NASA SkyView; false color and opacity processing'
(ROOT/'data/celestials.json').write_text(json.dumps(data,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
print(f'Prepared {len(nebulae)} real nebula positions and {len(texture):,} bytes of lunar albedo.')
