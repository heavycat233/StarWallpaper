"""Install the exact AT-HYG preview supplement as an Android asset."""
import base64
import gzip
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'android/app/src/main/assets'
source = (ROOT / 'preview/deep-catalog.js').read_text(encoding='ascii').strip()
prefix = 'window.STAR_DEEP_CATALOGUE='
assert source.startswith(prefix) and source.endswith(';')
catalogue = json.loads(source[len(prefix):-1])
upstream_meta = json.loads((ROOT / 'data/athyg-preview-meta.json').read_text(encoding='utf-8'))
count = catalogue['count']
assert count == upstream_meta['additionalCount'] and catalogue['counts'] == upstream_meta['counts']
compressed = base64.b64decode(catalogue['data'], validate=True)
records = gzip.decompress(compressed)
assert len(records) == count * 6

meta = {
    'source': 'AT-HYG v3.2',
    'count': count,
    'counts': catalogue['counts'],
    'recordBytes': 6,
    'layout': 'byte-planes',
    'sha256': hashlib.sha256(compressed).hexdigest(),
    'uncompressedSha256': hashlib.sha256(records).hexdigest(),
    'attribution': upstream_meta['license'],
    'photometry': upstream_meta['photometry'],
    'notice': '深星表加入无 HYG 交叉编号的 AT-HYG 恒星；与网页预览使用相同记录。',
}
(ASSETS / 'stars-deep.bin.gz').write_bytes(compressed)
(ASSETS / 'deep-catalogue.json').write_text(json.dumps(meta, ensure_ascii=False, separators=(',', ':')), encoding='utf-8')
print(f'Installed exact AT-HYG preview supplement: {count:,} stars, {len(compressed):,} compressed bytes')
