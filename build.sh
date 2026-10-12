#!/bin/sh
set -eu
cd "$(dirname "$0")"
rm -rf build/classes build/preverified
mkdir -p build/classes dist
java -jar ../lib/ecj.jar -source 1.3 -target 1.3 -bootclasspath ../lib/cldcapi11.jar -classpath ../lib/midpapi20.jar -d build/classes ../nitro2d/src/n2d/*.java src/ndash/*.java
java -jar ../lib/proguard.jar -injars build/classes -outjars build/preverified -libraryjars ../lib/cldcapi11.jar -libraryjars ../lib/midpapi20.jar -dontshrink -dontoptimize -dontobfuscate -microedition
python3 - <<'PY'
from pathlib import Path
import zipfile
fields=['MIDlet-Name: Nokodash','MIDlet-Version: 0.1.0','MIDlet-Vendor: Atlas','MIDlet-1: Nokodash,/icon.png,ndash.Dash','MicroEdition-Configuration: CLDC-1.1','MicroEdition-Profile: MIDP-2.0']
jar=Path('dist/Nokodash.jar')
with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED) as z:
 z.writestr('META-INF/MANIFEST.MF','\r\n'.join(['Manifest-Version: 1.0']+fields+['','']))
 for f in ['icon.png','atlas.png','atlas.tbl','bg0.png','bg1.png','bg2.png','logo.png']:
  z.write('assets/'+f,f)
 for f in sorted(Path('build/preverified').rglob('*.class')):z.write(f,f.relative_to('build/preverified').as_posix())
with zipfile.ZipFile(jar) as z2:
 assert z2.testzip() is None
 for name in z2.namelist():
  if name.endswith('.class'):assert int.from_bytes(z2.read(name)[6:8],'big')==47
Path('dist/Nokodash.jad').write_bytes(('\r\n'.join(fields+['MIDlet-Jar-URL: Nokodash.jar','MIDlet-Jar-Size: '+str(jar.stat().st_size),''])).encode('ascii'))
print('Built',jar,jar.stat().st_size,'bytes; class version 47 and ZIP verified')
PY
