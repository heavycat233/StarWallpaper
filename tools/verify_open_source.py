"""Check the publication allowlist, licenses, data and obvious secret material."""
import base64,gzip,json,re,subprocess
from pathlib import Path
from public_files import ROOT,public_files

def verify():
    files=public_files()
    patterns=[re.compile(rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'),re.compile(rb'gh[pousr]_[A-Za-z0-9]{30,}'),re.compile(rb'github_pat_[A-Za-z0-9_]{30,}')]
    for p in files:
        rel=p.relative_to(ROOT);assert p.is_file(),f'Missing public file: {rel}'
        assert not set(rel.parts)&{'.signing','.toolchains','.git','build','dist'},rel
        assert p.suffix.lower() not in {'.jks','.keystore','.p12','.pem','.apk','.log'},rel
        assert p.stat().st_size<50_000_000,f'Oversized public file: {rel}'
        if p.suffix.lower() not in {'.gz','.png','.jpg'}:
            content=p.read_bytes()
            assert not any(x.search(content) for x in patterns),f'Possible credential in {rel}; inspect locally, never print its contents'
    catalogue=gzip.decompress(base64.b64decode((ROOT/'data/catalogue.b64').read_text(encoding='ascii')))
    assert catalogue==gzip.decompress((ROOT/'android/app/src/main/assets/stars.bin.gz').read_bytes())
    assert len(catalogue)==115570*6
    assert 'MIT License' in (ROOT/'LICENSE').read_text(encoding='utf-8')
    for name in ['CC-BY-SA-4.0.txt','MIT-Astronomy-Engine.txt']:
        assert (ROOT/'LICENSES'/name).stat().st_size>500
    # When Git exists, ensure no unreviewed file bypasses the publication scope.
    if (ROOT/'.git').exists():
        tracked=subprocess.check_output(['git','ls-files','-z'],cwd=ROOT).decode().split('\0')
        allowed={p.relative_to(ROOT).as_posix() for p in files}
        assert set(filter(None,tracked))<=allowed,'Tracked files exceed publication scope'
    print(f'PASS open-source scope: {len(files)} files; licenses, size limits, obvious credential patterns and exact HYG payload checked.')
    return files
if __name__=='__main__':verify()
