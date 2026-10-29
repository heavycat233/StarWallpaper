"""Check that the extra APK truly targets Android 16 and uses the release key."""
import hashlib
import re
from pathlib import Path
from build_android import DIST,VERSION

apk=DIST/f'starwallpaper-{VERSION}-android16.apk'
meta=(DIST/'APK-METADATA-android16.txt').read_text(encoding='utf-8')
assert "platformBuildVersionCode='36'" in meta and "compileSdkVersion='36'" in meta
assert "minSdkVersion:'36'" in meta and "targetSdkVersion:'36'" in meta
assert "application-label:'starwallpaper'" in meta
assert "name='app.starfield.wallpaper'" in meta
expected=(DIST/'SHA256SUMS-android16.txt').read_text(encoding='ascii').split()[0]
assert hashlib.sha256(apk.read_bytes()).hexdigest()==expected
certificate=re.compile(r'certificate SHA-256 digest: ([0-9a-f]{64})')
android16=certificate.search((DIST/'APK-SIGNATURE-android16.txt').read_text(encoding='utf-8'))
android17=certificate.search((DIST/'APK-SIGNATURE.txt').read_text(encoding='utf-8'))
assert android16 and android17 and android16.group(1)==android17.group(1)
print('PASS Android 16 APK: API 36 minimum/target/compile, app name, checksum and same signing certificate as Android 17.')
