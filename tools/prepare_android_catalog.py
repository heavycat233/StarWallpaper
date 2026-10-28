"""Use the preview's exact HYG payload; no re-selection or photometry conversion."""
import base64,gzip,hashlib,json,shutil
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];ASSETS=ROOT/'android/app/src/main/assets'
blob=base64.b64decode((ROOT/'data/catalogue.b64').read_text(encoding='ascii'))
meta=json.loads((ROOT/'data/catalogue-meta.json').read_text(encoding='utf-8'))
records=gzip.decompress(blob)
assert len(records)==meta['count']*6
meta.update(sha256=hashlib.sha256(blob).hexdigest(),uncompressedSha256=hashlib.sha256(records).hexdigest(),photometry='HYG v4.1 Johnson V, identical records and order to preview',notice='与对话预览相同的 HYG v4.1 星表，115,570 颗真实恒星。暗星端不完备，11 等为当前星表的显示上限。坐标为 J2000；银河及星云纹理为艺术增强。',attribution='David Nash / Astronomy Nexus, HYG v4.1, CC BY-SA 4.0',sourceUrl='https://github.com/astronexus/HYG-Database')
(ASSETS/'stars.bin.gz').write_bytes(blob)
(ASSETS/'catalogue.json').write_text(json.dumps(meta,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
shutil.copyfile(ROOT/'data/HYG-LICENSE.txt',ASSETS/'HYG-LICENSE.txt')
print('Copied exact preview catalogue: '+str(meta['count'])+' records, '+meta['sha256'])
