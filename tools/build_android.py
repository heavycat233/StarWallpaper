"""Repeatable dependency-free Android 17 build using official aapt2/javac/D8/apksigner.

The local signing key is generated once, kept under ignored .signing/, and never printed.
"""
import hashlib
import json
import os
from pathlib import Path
import secrets
import subprocess
import xml.etree.ElementTree as ET
import zipfile

ROOT=Path(__file__).resolve().parents[1]
CONFIG=json.loads((ROOT/'.toolchains/toolchain.json').read_text(encoding='utf-8'))
JDK=Path(CONFIG['jdk']);SDK=Path(CONFIG['sdk']);TOOLS=SDK/'build-tools'/CONFIG['buildTools'];PLATFORM=SDK/'platforms'/CONFIG['platform']/'android.jar'
SRC=ROOT/'android/app/src/main';BUILD=ROOT/'build/android';DIST=ROOT/'dist';SIGN=ROOT/'.signing'
VERSION=ET.parse(SRC/'AndroidManifest.xml').getroot().attrib['{http://schemas.android.com/apk/res/android}versionName']
for folder in [BUILD,BUILD/'classes',BUILD/'generated',BUILD/'dex',DIST,SIGN]:folder.mkdir(parents=True,exist_ok=True)
ENV=os.environ.copy();ENV['JAVA_HOME']=str(JDK);ENV['PATH']=str(JDK/'bin')+os.pathsep+ENV.get('PATH','')

def run(args,**kwargs):
    result=subprocess.run([str(a) for a in args],cwd=ROOT,env=ENV,check=True,**kwargs)
    return result

def build():
    # AGP uses the Gradle namespace. aapt2's direct CLI instead needs the package
    # in its input manifest, so only the temporary build copy receives it.
    ET.register_namespace('android','http://schemas.android.com/apk/res/android')
    manifest=ET.parse(SRC/'AndroidManifest.xml')
    manifest.getroot().set('package','app.starfield.wallpaper')
    manifest.write(BUILD/'AndroidManifest.xml',encoding='utf-8',xml_declaration=True)
    print('Compiling Android resources',flush=True)
    run([TOOLS/'aapt2.exe','compile','--dir',SRC/'res','-o',BUILD/'resources.zip'])
    run([TOOLS/'aapt2.exe','link','-o',BUILD/'resources.apk','--manifest',BUILD/'AndroidManifest.xml','-I',PLATFORM,'--java',BUILD/'generated','--auto-add-overlay','--min-sdk-version','37','--target-sdk-version','37',BUILD/'resources.zip'])
    sources=sorted((SRC/'java').rglob('*.java'))+sorted((BUILD/'generated').rglob('*.java'))
    arguments=BUILD/'javac.args';arguments.write_text('\n'.join('"'+str(p).replace('\\','/')+'"' for p in sources),encoding='utf-8')
    print('Compiling native Android app',flush=True)
    run([JDK/'bin/javac.exe','-encoding','UTF-8','--release','17','-classpath',PLATFORM,'-d',BUILD/'classes','@'+str(arguments)])
    with zipfile.ZipFile(BUILD/'classes.jar','w',zipfile.ZIP_DEFLATED) as archive:
        for source in sorted((BUILD/'classes').rglob('*.class')):archive.write(source,source.relative_to(BUILD/'classes').as_posix())
    print('Producing Dalvik bytecode',flush=True)
    # Build Tools 37 bundles a D8 that warns about a 37 instruction floor. API 36
    # instructions are compatible with 37; installation remains min/target 37.
    run([JDK/'bin/java.exe','-cp',TOOLS/'lib/d8.jar','com.android.tools.r8.D8','--release','--min-api','36','--lib',PLATFORM,'--output',BUILD/'dex',BUILD/'classes.jar'])
    with zipfile.ZipFile(BUILD/'resources.apk') as resources,zipfile.ZipFile(BUILD/'unsigned.apk','w',zipfile.ZIP_DEFLATED) as archive:
        for item in resources.infolist():archive.writestr(item,resources.read(item.filename))
        for source in sorted((BUILD/'dex').glob('*.dex')):archive.write(source,source.name)
        for source in sorted((SRC/'assets').rglob('*')):
            if source.is_file():archive.write(source,'assets/'+source.relative_to(SRC/'assets').as_posix(),compress_type=zipfile.ZIP_STORED if source.suffix=='.gz' else zipfile.ZIP_DEFLATED)
    run([TOOLS/'zipalign.exe','-f','-p','4',BUILD/'unsigned.apk',BUILD/'aligned.apk'])
    passwordFile=SIGN/'signing.json';key=SIGN/'starfield-local.jks'
    if not passwordFile.is_file():passwordFile.write_text(json.dumps({'password':secrets.token_urlsafe(32)}),encoding='utf-8')
    ENV['STARFIELD_SIGN_PASSWORD']=json.loads(passwordFile.read_text())['password']
    if not key.is_file():
        run([JDK/'bin/keytool.exe','-genkeypair','-keystore',key,'-storetype','JKS','-alias','starfield','-keyalg','RSA','-keysize','3072','-validity','10000','-dname','CN=Starfield Local Build, OU=Development, O=Starfield','-storepass:env','STARFIELD_SIGN_PASSWORD','-keypass:env','STARFIELD_SIGN_PASSWORD','-noprompt'],capture_output=True)
    output=DIST/f'Starfield-{VERSION}-android17.apk'
    run([JDK/'bin/java.exe','-jar',TOOLS/'lib/apksigner.jar','sign','--ks',key,'--ks-key-alias','starfield','--ks-pass','env:STARFIELD_SIGN_PASSWORD','--key-pass','env:STARFIELD_SIGN_PASSWORD','--out',output,BUILD/'aligned.apk'])
    result=run([JDK/'bin/java.exe','-jar',TOOLS/'lib/apksigner.jar','verify','--verbose','--print-certs',output],capture_output=True,text=True)
    (DIST/'APK-SIGNATURE.txt').write_text(result.stdout,encoding='utf-8');print(result.stdout,flush=True)
    digest=hashlib.sha256(output.read_bytes()).hexdigest();(DIST/'SHA256SUMS.txt').write_text(digest+'  '+output.name+'\n',encoding='ascii')
    metadata=run([TOOLS/'aapt2.exe','dump','badging',output],capture_output=True,text=True,encoding='utf-8')
    (DIST/'APK-METADATA.txt').write_text(metadata.stdout,encoding='utf-8')
    print('Built '+str(output)+f' ({output.stat().st_size:,} bytes)',flush=True)
    print('SHA256 '+digest,flush=True)
if __name__=='__main__':build()
