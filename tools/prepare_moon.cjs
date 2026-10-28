// Offline six-hour ephemeris from Astronomy Engine 2.1.19 (MIT).
const A=require('../vendor/astronomy/astronomy.common.js'),fs=require('fs'),zlib=require('zlib');
const start=Date.UTC(1900,0,1),end=Date.UTC(2101,0,2),step=21600000,N=Math.round((end-start)/step)+1;
const b=Buffer.alloc(20+N*44);b.writeDoubleLE(start,0);b.writeDoubleLE(step,8);b.writeUInt32LE(N,16);
function unit(v){const n=Math.hypot(v.x,v.y,v.z);return [v.x/n,v.y/n,v.z/n];}
for(let i=0;i<N;i++){
 const date=new Date(start+i*step),m=A.GeoMoon(date),s=A.GeoVector(A.Body.Sun,date,true),l=A.Libration(date),n=A.RotationAxis(A.Body.Moon,date).north;
 const values=[m.x*149597870.7,m.y*149597870.7,m.z*149597870.7,...unit({x:s.x-m.x,y:s.y-m.y,z:s.z-m.z}),...unit(n),l.elat,l.elon];
 values.forEach((v,k)=>b.writeFloatLE(v,20+i*44+k*4));
 if(i%50000===0)console.log(i+'/'+N);
}
fs.writeFileSync('android/app/src/main/assets/moon-ephemeris.bin.gz',zlib.gzipSync(b,{level:9}));console.log('Moon table complete: '+N);
