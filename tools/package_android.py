"""Package only native sources and public artifacts; never include tools, caches or signing keys."""
import hashlib
import json
from pathlib import Path
import zipfile
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
DIST=ROOT/'dist'
version=ET.parse(ROOT/'android/app/src/main/AndroidManifest.xml').getroot().attrib['{http://schemas.android.com/apk/res/android}versionName']
apk=DIST/f'Starfield-{version}-android17.apk'
checksum=hashlib.sha256(apk.read_bytes()).hexdigest()
expected=(DIST/'SHA256SUMS.txt').read_text().split()[0]
assert checksum==expected
sources=[]
for folder in [ROOT/'android/app/src',ROOT/'android/qa']:
    sources.extend(p for p in folder.rglob('*') if p.is_file())
sources.extend(ROOT/p for p in ['data/catalogue.b64','data/catalogue-meta.json','data/HYG-LICENSE.txt','tools/prepare_moon.cjs','tools/prepare_android_celestials.py','vendor/astronomy/astronomy.common.js','vendor/astronomy/astronomy.browser.min.js','data/celestials.json','data/OpenNGC-LICENSE.txt','android/README.md','docs/ANDROID-DESIGN.md','android/build.gradle','android/settings.gradle','android/gradle.properties','android/app/build.gradle','tests/SkyMathNativeTest.java','tools/bootstrap_android.py','tools/build_android.py','tools/build_android_qa.py','tools/download_deep_catalog.py','tools/prepare_android_catalog.py','tools/verify_android_catalog.py','tools/package_android.py'])
out=DIST/f'Starfield-{version}-source.zip'
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
    for source in sorted(sources):
        relative=source.relative_to(ROOT).as_posix()
        assert '.signing' not in relative and '.toolchains' not in relative
        archive.write(source,'Starfield/'+relative)
    archive.writestr('Starfield/.gitignore','.toolchains/\n.signing/\nbuild/\nandroid/.gradle/\nandroid/local.properties\nandroid/app/build/\n')
    archive.writestr('Starfield/README.md',(ROOT/'android/README.md').read_text(encoding='utf-8'))
with zipfile.ZipFile(out) as archive:
    assert archive.testzip() is None
report={'version':version,'applicationId':'app.starfield.wallpaper','minSdk':37,'targetSdk':37,'compileSdk':37,'apk':apk.name,'bytes':apk.stat().st_size,'sha256':checksum,'catalogue':115570,'catalogueSource':'HYG v4.1, exact preview records','sourceArchive':out.name,'signing':'local RSA-3072 APK Signature Scheme v3','tested':'Android 17 / API 37 emulator; signed in-place upgrade to versionCode 4; exact conversation preset button; native shaders; lunar altitude; GPU pixel differences for Moon, nebulae and 70% bright-star size; analog and digital night scenes; no lunar pixels below horizon; 1000 ephemeris direction samples (max 0.00038 degree); native coordinate tests','notYetValidated':'OEM physical devices, AOD, long-duration battery measurements'}
(DIST/'BUILD-INFO.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print('Verified APK: '+checksum)
print('Source archive: '+str(out)+f' ({out.stat().st_size:,} bytes)')
print('No signing keys or toolchain caches included.')
