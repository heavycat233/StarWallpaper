"""Fetch public AT-HYG source files without changing the inline-preview star table."""
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import shutil
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
(ROOT / 'data').mkdir(parents=True, exist_ok=True)
FILES = {
    'athyg_v32-1.csv.gz': 'https://raw.githubusercontent.com/astronexus/ATHYG-Database/main/data/athyg_v32-1.csv.gz',
    'athyg_v32-2.csv.gz': 'https://raw.githubusercontent.com/astronexus/ATHYG-Database/main/data/athyg_v32-2.csv.gz',
    'ATHYG-LICENSE.txt': 'https://raw.githubusercontent.com/astronexus/ATHYG-Database/main/LICENSE',
    'ATHYG-ACKNOWLEDGMENTS.md': 'https://raw.githubusercontent.com/astronexus/ATHYG-Database/main/ACKNOWLEDGMENTS.md',
    'ATHYG-VERSION.md': 'https://raw.githubusercontent.com/astronexus/ATHYG-Database/main/version-info.md',
}
def fetch(item):
    name, url = item
    path = ROOT / 'data' / name
    if not path.is_file():
        print('Downloading ' + name, flush=True)
        with urllib.request.urlopen(url, timeout=120) as response, path.with_suffix(path.suffix + '.partial').open('wb') as out:
            shutil.copyfileobj(response, out, 1024 * 1024)
        path.with_suffix(path.suffix + '.partial').replace(path)
    print('Ready ' + name, flush=True)
with ThreadPoolExecutor(max_workers=3) as pool:
    list(pool.map(fetch, FILES.items()))
