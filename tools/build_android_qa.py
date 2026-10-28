from build_android import ROOT, JDK, TOOLS, PLATFORM, BUILD, ENV, SIGN, run
import json
import zipfile

QA=BUILD/'qa'
for path in [QA,QA/'classes',QA/'dex']:path.mkdir(parents=True,exist_ok=True)
run([JDK/'bin/javac.exe','-encoding','UTF-8','--release','17','-classpath',str(PLATFORM)+';'+str(BUILD/'classes'),'-d',QA/'classes',*sorted((ROOT/'android/qa').glob('*.java'))])
with zipfile.ZipFile(QA/'classes.jar','w',zipfile.ZIP_DEFLATED) as archive:
    for path in (QA/'classes').rglob('*.class'):archive.write(path,path.relative_to(QA/'classes').as_posix())
run([JDK/'bin/java.exe','-cp',TOOLS/'lib/d8.jar','com.android.tools.r8.D8','--release','--min-api','36','--lib',PLATFORM,'--classpath',BUILD/'classes.jar','--output',QA/'dex',QA/'classes.jar'])
run([TOOLS/'aapt2.exe','link','-o',QA/'resources.apk','--manifest',ROOT/'android/qa/AndroidManifest.xml','-I',PLATFORM,'--min-sdk-version','37','--target-sdk-version','37'])
with zipfile.ZipFile(QA/'resources.apk') as source,zipfile.ZipFile(QA/'unsigned.apk','w',zipfile.ZIP_DEFLATED) as archive:
    for item in source.infolist():archive.writestr(item,source.read(item.filename))
    for path in (QA/'dex').glob('*.dex'):archive.write(path,path.name)
run([TOOLS/'zipalign.exe','-f','4',QA/'unsigned.apk',QA/'aligned.apk'])
ENV['STARFIELD_SIGN_PASSWORD']=json.loads((SIGN/'signing.json').read_text())['password']
run([JDK/'bin/java.exe','-jar',TOOLS/'lib/apksigner.jar','sign','--ks',SIGN/'starfield-local.jks','--ks-key-alias','starfield','--ks-pass','env:STARFIELD_SIGN_PASSWORD','--key-pass','env:STARFIELD_SIGN_PASSWORD','--out',QA/'starfield-qa.apk',QA/'aligned.apk'])
run([JDK/'bin/javac.exe','-encoding','UTF-8','--release','17','-classpath',BUILD/'classes','-d',QA/'classes',ROOT/'tests/SkyMathNativeTest.java'])
run([JDK/'bin/java.exe','-cp',str(BUILD/'classes')+';'+str(QA/'classes'),'SkyMathNativeTest'])
print('Native QA APK ready: '+str(QA/'starfield-qa.apk'),flush=True)
