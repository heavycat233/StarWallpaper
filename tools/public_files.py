"""Explicit publication scope. Never traverse private build/signing/toolchain trees."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ROOT_FILES=['README.md','LICENSE','NOTICE.md','CONTRIBUTING.md','SECURITY.md','CHANGELOG.md','.gitignore','.gitattributes','.editorconfig']
FOLDERS=['android/app/src','android/qa','.github','docs','LICENSES','tests']
SINGLES=['android/README.md','android/build.gradle','android/settings.gradle','android/gradle.properties','android/app/build.gradle',
 'data/catalogue.b64','data/catalogue-meta.json','data/celestials.json','data/constellations.json','data/constellations.lines.upstream.json','data/constellations.upstream.json','data/HYG-LICENSE.txt','data/HYG-README.md','data/OpenNGC.csv','data/OpenNGC-LICENSE.txt','data/moon-lroc-2k.jpg',
 'vendor/astronomy/astronomy.browser.min.js','vendor/astronomy/astronomy.common.js']
def public_files():
    files=[ROOT/p for p in ROOT_FILES+SINGLES]
    for folder in FOLDERS:
        files.extend(p for p in (ROOT/folder).rglob('*') if p.is_file())
    files.extend((ROOT/'preview').glob('*.js'))
    files.extend(ROOT/'preview'/p for p in ['star-wallpaper.html','star-wallpaper.fragment.html'])
    files.extend((ROOT/'tools').glob('*.py'))
    files.extend((ROOT/'tools').glob('*.cjs'))
    return sorted(set(p for p in files if '__pycache__' not in p.parts))
