/* Shared annual meteor activity and approximate solar geometry for decorative sky effects. */
(function(scope){
  'use strict';
  const D=Math.PI/180,obliquity=23.439291*D;
  const sky=scope.StarSky||(typeof require==='function'?require('./sky-math.js'):null);
  const eclipticNormal=[0,-Math.sin(obliquity),Math.cos(obliquity)];
  const clamp=(v,a,b)=>Math.max(a,Math.min(b,v));
  const smoothstep=(a,b,v)=>{const t=clamp((v-a)/(b-a),0,1);return t*t*(3-2*t);};
  function solarLongitude(epoch){
    const days=epoch/86400000+2440587.5-2451545;
    const mean=(280.460+.9856474*days)*D,anomaly=(357.528+.9856003*days)*D;
    const longitude=(mean/D+1.915*Math.sin(anomaly)+.020*Math.sin(2*anomaly))%360;
    return (longitude+360)%360;
  }
  function sunRay(epoch){const l=solarLongitude(epoch)*D;return [Math.cos(l),Math.cos(obliquity)*Math.sin(l),Math.sin(obliquity)*Math.sin(l)];}
  function solarDistance(a,b){return (a-b+540)%360-180;}
  function zodiacalPhase(camera,epoch){
    const altitude=Math.asin(clamp(sky.mv(camera.local,sunRay(epoch))[2],-1,1))/D;
    return smoothstep(-28,-15,altitude)*(1-smoothstep(-9,-3,altitude));
  }
  function showerWeights(showers,epoch,camera){
    const date=new Date(epoch),day=(date.getUTCMonth()+1)*100+date.getUTCDate(),solar=solarLongitude(epoch);
    return showers.map(shower=>{
      const active=shower.start<=shower.end?day>=shower.start&&day<=shower.end:day>=shower.start||day<=shower.end;
      if(!active)return 0;
      const radiant=sky.equatorial(shower.ra/15,shower.dec);
      const altitude=sky.mv(camera.local,radiant)[2];
      if(altitude<=0)return 0;
      const delta=solarDistance(solar,shower.peakSolar)/.9856474;
      return shower.zhr*(.02+.98*Math.exp(-.5*(delta/shower.widthDays)**2))*altitude;
    });
  }
  function meteorMagnitude(r,random=Math.random){
    let total=0;for(let m=-3;m<=5;m++)total+=Math.pow(r,m);
    let value=random()*total;for(let m=-3;m<=5;m++){value-=Math.pow(r,m);if(value<=0)return m;}
    return 5;
  }
  const api={eclipticNormal,solarLongitude,sunRay,solarDistance,zodiacalPhase,showerWeights,meteorMagnitude};
  if(typeof module!=='undefined'&&module.exports)module.exports=api;else scope.StarEvents=api;
})(typeof window!=='undefined'?window:globalThis);
