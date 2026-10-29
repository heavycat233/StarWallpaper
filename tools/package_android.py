"""Package only native sources and public artifacts; never include tools, caches or signing keys."""
import hashlib
import json
from pathlib import Path
import zipfile
import xml.etree.ElementTree as ET
from verify_open_source import verify

ROOT=Path(__file__).resolve().parents[1]
DIST=ROOT/'dist'
version=ET.parse(ROOT/'android/app/src/main/AndroidManifest.xml').getroot().attrib['{http://schemas.android.com/apk/res/android}versionName']
apk=DIST/f'starwallpaper-{version}-android17.apk'
checksum=hashlib.sha256(apk.read_bytes()).hexdigest()
expected=(DIST/'SHA256SUMS.txt').read_text().split()[0]
assert checksum==expected
sources=verify()
out=DIST/f'starwallpaper-{version}-source.zip'
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
    for source in sorted(sources):
        relative=source.relative_to(ROOT).as_posix()
        assert '.signing' not in relative and '.toolchains' not in relative
        archive.write(source,'starwallpaper/'+relative)
with zipfile.ZipFile(out) as archive:
    assert archive.testzip() is None
report={'version':version,'applicationId':'app.starfield.wallpaper','minSdk':37,'targetSdk':37,'compileSdk':37,'apk':apk.name,'bytes':apk.stat().st_size,'sha256':checksum,'catalogue':115570,'catalogueSource':'HYG v4.1, exact preview records','sourceArchive':out.name,'signing':'local RSA-3072 APK Signature Scheme v3','tested':'Android 17 / API 37 emulator; star-name and constellation controls; GLES pixel changes for names, lines and twinkle strength; exact Android/preview HYG and constellation data; native coordinate tests','notYetValidated':'OEM physical devices, AOD, long-duration battery measurements'}
android16=DIST/f'starwallpaper-{version}-android16.apk'
if android16.is_file():
    expected16=(DIST/'SHA256SUMS-android16.txt').read_text(encoding='ascii').split()[0]
    actual16=hashlib.sha256(android16.read_bytes()).hexdigest()
    assert actual16==expected16
    report['android16']={'apk':android16.name,'bytes':android16.stat().st_size,'sha256':actual16,'minSdk':36,'targetSdk':36,'compileSdk':36}
    report['tested']+='; Android 16 / API 36 emulator: annotation and celestial GLES pixel regressions'
(DIST/'BUILD-INFO.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print('Verified APK: '+checksum)
print('Source archive: '+str(out)+f' ({out.stat().st_size:,} bytes)')
print('No signing keys or toolchain caches included.')
