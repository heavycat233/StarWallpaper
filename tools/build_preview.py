"""Build the self-contained, under-1MB inline preview from editable sources."""
import json
import base64
import gzip
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
fragment = (ROOT / 'preview/star-wallpaper.fragment.html').read_text(encoding='utf-8')
meta = json.loads((ROOT / 'data/catalogue-meta.json').read_text(encoding='utf-8'))
packed = gzip.decompress(base64.b64decode((ROOT / 'data/catalogue.b64').read_text(encoding='ascii')))
# Byte planes compress the ordered magnitudes much better, leaving room for
# the full offline ephemeris without removing any real catalogue stars.
planes = b''.join(packed[j::6] for j in range(6))
assert b''.join(bytes(planes[j*meta['count']+i] for j in range(6)) for i in range(meta['count'])) == packed
data = base64.b64encode(gzip.compress(planes,9,mtime=0)).decode('ascii')
catalogue = {'count': meta['count'], 'named': meta['named'], 'layout': 'byte-planes', 'data': data}
fragment = fragment.replace('<!-- SKY_MATH -->', '<script>\n' + (ROOT / 'preview/sky-math.js').read_text(encoding='utf-8') + '\n</script>')
fragment = fragment.replace('<!-- CATALOGUE -->', '<script>window.STAR_CATALOGUE=' + json.dumps(catalogue, ensure_ascii=False, separators=(',', ':')) + ';</script>')
fragment = fragment.replace('<!-- CONSTELLATIONS -->', '<script>window.STAR_CONSTELLATIONS=' + (ROOT/'data/constellations.json').read_text(encoding='utf-8') + ';</script>')
fragment = fragment.replace('<!-- ASTRONOMY -->', '<script>\n' + (ROOT/'vendor/astronomy/astronomy.browser.min.js').read_text(encoding='utf-8') + '\n</script>')
fragment = fragment.replace('<!-- CELESTIAL_DATA -->','<script>window.STAR_CELESTIAL_DATA='+(ROOT/'data/celestials.json').read_text(encoding='utf-8')+';</script>')
fragment = fragment.replace('<!-- CELESTIAL_MATH -->','<script>\n'+(ROOT/'preview/celestial-math.js').read_text(encoding='utf-8')+'\n</script>')
fragment = fragment.replace('<!-- PREVIEW_SCRIPT -->', '<script>\n' + (ROOT / 'preview/preview.js').read_text(encoding='utf-8') + '\n</script>')
out = ROOT / 'preview/star-wallpaper.html'
out.write_text(fragment, encoding='utf-8')
assert out.stat().st_size < 1_000_000, out.stat().st_size
print(f'Built {out} ({out.stat().st_size:,} bytes)')
