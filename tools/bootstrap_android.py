"""Download isolated official Android/JDK build tools, checking published digests."""
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
from pathlib import Path
import shutil
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / '.toolchains'
DOWNLOADS = BASE / 'downloads'
DOWNLOADS.mkdir(parents=True, exist_ok=True)

def get_json(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers={'User-Agent': 'starwallpaper-build/1.0'}), timeout=90) as response:
        return json.load(response)

def download(url, name, checksum=None, algorithm='sha256'):
    target = DOWNLOADS / name
    def valid():
        return target.is_file() and (not checksum or hashlib.file_digest(target.open('rb'), algorithm).hexdigest() == checksum)
    if not valid():
        print('Downloading ' + name, flush=True)
        partial = target.with_suffix(target.suffix + '.partial')
        with urllib.request.urlopen(urllib.request.Request(url, headers={'User-Agent': 'starwallpaper-build/1.0'}), timeout=120) as response, partial.open('wb') as out:
            shutil.copyfileobj(response, out, 1024 * 1024)
        partial.replace(target)
    if not valid():
        raise RuntimeError('Checksum mismatch: ' + name)
    return target

def extract_single(archive, destination):
    destination = Path(destination)
    if destination.is_dir():
        return
    temporary = destination.with_name(destination.name + '-extract')
    temporary.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(archive) as source:
        for item in source.infolist():
            resolved = (temporary / item.filename).resolve()
            if not resolved.is_relative_to(temporary.resolve()):
                raise RuntimeError('Unsafe archive path')
        source.extractall(temporary)
    children = list(temporary.iterdir())
    destination.parent.mkdir(parents=True, exist_ok=True)
    if len(children) == 1 and children[0].is_dir():
        try:
            children[0].rename(destination)
            temporary.rmdir()
        except PermissionError:
            shutil.copytree(children[0], destination, dirs_exist_ok=True)
    else:
        temporary.rename(destination)

def sdk_package(package, destination):
    repo = ET.parse(BASE / 'repository.xml').getroot()
    match = next(p for p in repo.findall('remotePackage') if p.attrib['path'] == package and p.find('channelRef').attrib['ref'] == 'channel-0')
    archive = next(a for a in match.findall('archives/archive') if a.findtext('host-os') in (None, 'windows'))
    complete = archive.find('complete')
    url = complete.findtext('url')
    digest = complete.find('checksum')
    path = download('https://dl.google.com/android/repository/' + url, url, digest.text, digest.attrib.get('type', 'sha1'))
    extract_single(path, destination)
    print('Ready ' + package, flush=True)

def jdk():
    package = get_json('https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse')[0]['binary']['package']
    archive = download(package['link'], package['name'], package['checksum'])
    extract_single(archive, BASE / 'jdk')
    print('Ready JDK 21', flush=True)

if __name__ == '__main__':
    if not (BASE / 'repository.xml').is_file():
        shutil.copy2(download('https://dl.google.com/android/repository/repository2-3.xml', 'repository2-3.xml'), BASE / 'repository.xml')
    with ThreadPoolExecutor(max_workers=4) as pool:
        tasks = [pool.submit(jdk), pool.submit(sdk_package, 'platforms;android-37.0', BASE / 'sdk/platforms/android-37'), pool.submit(sdk_package, 'build-tools;37.0.0', BASE / 'sdk/build-tools/37.0.0'), pool.submit(sdk_package, 'platform-tools', BASE / 'sdk/platform-tools')]
        for task in tasks:
            task.result()
    (BASE / 'toolchain.json').write_text(json.dumps({'jdk': str(BASE / 'jdk'), 'sdk': str(BASE / 'sdk'), 'platform': 'android-37', 'buildTools': '37.0.0', 'api': 37}, indent=2), encoding='utf-8')
    print('Android 17 build toolchain ready.', flush=True)
