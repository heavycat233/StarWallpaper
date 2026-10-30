"""Create a complete source-only archive using the reviewed publication scope."""
import hashlib,json,zipfile
from public_files import ROOT
from verify_open_source import verify
files=verify();out=ROOT/'dist/starwallpaper-open-source-1.2.4.zip';out.parent.mkdir(exist_ok=True)
manifest={p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in files}
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
    for p in files:z.write(p,'starwallpaper/'+p.relative_to(ROOT).as_posix())
    z.writestr('starwallpaper/SOURCE-MANIFEST.json',json.dumps(manifest,ensure_ascii=False,indent=2))
with zipfile.ZipFile(out) as z:assert z.testzip() is None
digest=hashlib.sha256(out.read_bytes()).hexdigest()
out.with_suffix('.sha256').write_text(digest+'  '+out.name+'\n',encoding='ascii')
print(f'Built {out} ({out.stat().st_size:,} bytes); {len(files)} reviewed files; SHA256 {digest}')
