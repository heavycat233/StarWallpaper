"""Prepare an isolated official Android 16/API 36 emulator for compatibility QA."""
from bootstrap_android import BASE,download,extract_single,sdk_package
import shutil
import xml.etree.ElementTree as ET

sdk_package('emulator',BASE/'sdk/emulator')
if not (BASE/'system-images.xml').is_file():
    shutil.copy2(download('https://dl.google.com/android/repository/sys-img/android/sys-img2-1.xml','sys-img2-1.xml'),BASE/'system-images.xml')
repo=ET.parse(BASE/'system-images.xml').getroot()
package=next(p for p in repo.findall('remotePackage') if p.attrib['path']=='system-images;android-36;default;x86_64' and p.find('channelRef').attrib['ref']=='channel-0')
archive=package.find('archives/archive/complete')
url=archive.findtext('url')
path=download('https://dl.google.com/android/repository/sys-img/android/'+url,url,archive.findtext('checksum'),'sha1')
extract_single(path,BASE/'sdk/system-images/android-36/default/x86_64')
avd=BASE/'avd/StarWallpaperApi36.avd';avd.mkdir(parents=True,exist_ok=True)
(avd.parent/'StarWallpaperApi36.ini').write_text('avd.ini.encoding=UTF-8\npath='+str(avd)+'\ntarget=android-36\n',encoding='utf-8')
config={'avd.ini.encoding':'UTF-8','AvdId':'StarWallpaperApi36','abi.type':'x86_64','hw.cpu.arch':'x86_64','hw.cpu.ncore':'4','hw.ramSize':'2048','hw.gpu.enabled':'yes','hw.gpu.mode':'swiftshader','hw.lcd.width':'1080','hw.lcd.height':'2400','hw.lcd.density':'420','hw.keyboard':'yes','hw.audioInput':'no','hw.camera.back':'none','hw.camera.front':'none','hw.mainKeys':'no','disk.dataPartition.size':'1024M','image.sysdir.1':str(BASE/'sdk/system-images/android-36/default/x86_64')+'\\','tag.id':'default','tag.display':'Default','target':'android-36','showDeviceFrame':'no','fastboot.forceColdBoot':'yes'}
(avd/'config.ini').write_text('\n'.join(k+'='+v for k,v in config.items())+'\n',encoding='utf-8')
print('Android 16 emulator image ready.',flush=True)
