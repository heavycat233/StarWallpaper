"""Create a complete source-only archive using the reviewed publication scope."""
import hashlib,json,zipfile
import xml.etree.ElementTree as ET
from public_files import ROOT
from verify_open_source import verify
files=verify();version=ET.parse(ROOT/'android/app/src/main/AndroidManifest.xml').getroot().attrib['{http://schemas.android.com/apk/res/android}versionName'];out=ROOT/'dist'/f'starwallpaper-open-source-{version}.zip';out.parent.mkdir(exist_ok=True)
manifest={p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in files}
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
    for p in files:z.write(p,'starwallpaper/'+p.relative_to(ROOT).as_posix())
    z.writestr('starwallpaper/SOURCE-MANIFEST.json',json.dumps(manifest,ensure_ascii=False,indent=2))
with zipfile.ZipFile(out) as z:assert z.testzip() is None
digest=hashlib.sha256(out.read_bytes()).hexdigest()
out.with_suffix('.sha256').write_text(digest+'  '+out.name+'\n',encoding='ascii')
print(f'Built {out} ({out.stat().st_size:,} bytes); {len(files)} reviewed files; SHA256 {digest}')
