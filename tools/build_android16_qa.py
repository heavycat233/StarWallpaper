"""Build the same native QA suite for the separate Android 16 APK."""
import json
import zipfile
import xml.etree.ElementTree as ET
from build_android import ROOT,JDK,SDK,ENV,SIGN,run

TOOLS=SDK/'build-tools/36.0.0';PLATFORM=SDK/'platforms/android-36/android.jar'
BUILD=ROOT/'build/android16';QA=BUILD/'qa'
for folder in [QA,QA/'classes',QA/'dex']:folder.mkdir(parents=True,exist_ok=True)
ET.register_namespace('android','http://schemas.android.com/apk/res/android')
manifest=ET.parse(ROOT/'android/qa/AndroidManifest.xml')
uses=manifest.getroot().find('uses-sdk');android='{http://schemas.android.com/apk/res/android}'
uses.set(android+'minSdkVersion','36');uses.set(android+'targetSdkVersion','36')
manifest.write(QA/'AndroidManifest.xml',encoding='utf-8',xml_declaration=True)
run([JDK/'bin/javac.exe','-encoding','UTF-8','--release','17','-classpath',str(PLATFORM)+';'+str(BUILD/'classes'),'-d',QA/'classes',*sorted((ROOT/'android/qa').glob('*.java'))])
with zipfile.ZipFile(QA/'classes.jar','w',zipfile.ZIP_DEFLATED) as archive:
    for path in (QA/'classes').rglob('*.class'):archive.write(path,path.relative_to(QA/'classes').as_posix())
run([JDK/'bin/java.exe','-cp',TOOLS/'lib/d8.jar','com.android.tools.r8.D8','--release','--min-api','36','--lib',PLATFORM,'--classpath',BUILD/'classes.jar','--output',QA/'dex',QA/'classes.jar'])
run([TOOLS/'aapt2.exe','link','-o',QA/'resources.apk','--manifest',QA/'AndroidManifest.xml','-I',PLATFORM,'--min-sdk-version','36','--target-sdk-version','36'])
with zipfile.ZipFile(QA/'resources.apk') as source,zipfile.ZipFile(QA/'unsigned.apk','w',zipfile.ZIP_DEFLATED) as archive:
    for item in source.infolist():archive.writestr(item,source.read(item.filename))
    for path in (QA/'dex').glob('*.dex'):archive.write(path,path.name)
run([TOOLS/'zipalign.exe','-f','4',QA/'unsigned.apk',QA/'aligned.apk'])
ENV['STARFIELD_SIGN_PASSWORD']=json.loads((SIGN/'signing.json').read_text())['password']
run([JDK/'bin/java.exe','-jar',TOOLS/'lib/apksigner.jar','sign','--ks',SIGN/'starfield-local.jks','--ks-key-alias','starfield','--ks-pass','env:STARFIELD_SIGN_PASSWORD','--key-pass','env:STARFIELD_SIGN_PASSWORD','--out',QA/'starfield-qa.apk',QA/'aligned.apk'])
print('Android 16 native QA APK ready: '+str(QA/'starfield-qa.apk'),flush=True)
