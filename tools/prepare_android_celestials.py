import base64,gzip,json,math,random,shutil
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'android/app/src/main/assets'; data=json.loads((ROOT/'data/celestials.json').read_text(encoding='utf-8'))
tex=data['moonTexture'];Image.frombytes('L',(tex['width'],tex['height']),gzip.decompress(base64.b64decode(tex['data']))).save(OUT/'moon.png')
(OUT/'nebulae.json').write_text(json.dumps(data['nebulae'],ensure_ascii=False),encoding='utf-8')
def hash3(x,y,z):
    h=(x*374761393+y*668265263+z*2147483647)&0xffffffff
    h=((h^(h>>13))*1274126177)&0xffffffff
    return (h^(h>>16))/4294967295

def noise(x,y,z):
    ix,iy,iz=math.floor(x),math.floor(y),math.floor(z)
    fx,fy,fz=x-ix,y-iy,z-iz;fx=fx*fx*(3-2*fx);fy=fy*fy*(3-2*fy);fz=fz*fz*(3-2*fz)
    mix=lambda a,b,t:a+(b-a)*t
    return mix(mix(mix(hash3(ix,iy,iz),hash3(ix+1,iy,iz),fx),mix(hash3(ix,iy+1,iz),hash3(ix+1,iy+1,iz),fx),fy),mix(mix(hash3(ix,iy,iz+1),hash3(ix+1,iy,iz+1),fx),mix(hash3(ix,iy+1,iz+1),hash3(ix+1,iy+1,iz+1),fx),fy),fz)

for idx,n in enumerate(data['nebulae']):
    img=Image.new('RGBA',(128,128));p=img.load()
    for y in range(128):
        for x in range(128):
            nx=(x-63.5)/64;ny=(y-63.5)/64;r=math.hypot(nx,ny)
            a=noise(nx*4+idx*19,ny*4+13,7);b=noise(nx*13+8,ny*13+idx*9,19)
            density=(.22+.9*a+.35*b)*math.exp(-r*r*4.5)*max(0,min(1,(1-r)*6))
            if n['id']=='NGC2237':density*=.35+.8*math.exp(-(r-.42)**2/.025)
            density*=1-.7*math.exp(-(nx+.18*math.sin(ny*8))**2/.022)*(.25+.75*b)
            p[x,y]=(*(min(255,round(v*(.85+.3*b))) for v in n['color']),round(255*min(1,max(0,density*.82*n['intensity']))))
    img.save(OUT/(n['id']+'.png'))
shutil.copyfile(ROOT/'data/OpenNGC-LICENSE.txt',OUT/'OpenNGC-LICENSE.txt')
license=(ROOT/'vendor/astronomy/astronomy.browser.min.js').read_text(encoding='utf-8').split('*/',1)[0]+'*/'
(OUT/'CELESTIAL-LICENSE.txt').write_text('Moon ephemeris derived from Astronomy Engine 2.1.19, Don Cross. MIT.\n'+license+'\nNebula positions/sizes: Mattia Verga / OpenNGC, CC BY-SA 4.0. Procedural artistic textures.\nMoon map: NASA SVS CGI Moon Kit https://svs.gsfc.nasa.gov/4720/ Ernie Wright / LROC / LOLA. Resampled grayscale.\nEphemeris: 1900-2101, six-hour J2000 samples with linear interpolation; visual use only.\n',encoding='utf-8')
