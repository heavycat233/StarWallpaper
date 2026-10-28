/* Celestial math uses Astronomy Engine 2.1.19 (Don Cross, MIT).
 * Positions are EQJ/J2000, compatible with the star catalogue; no screen-space moon placement.
 */
(function(scope){
  'use strict';
  const M=scope.StarSky,A=scope.Astronomy;
  const unit=v=>{const r=Math.hypot(...v);return v.map(n=>n/r);};
  const xyz=v=>[v.x,v.y,v.z];
  const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
  function project(eq,c){
    const local=M.mv(c.local,eq),x=M.dot(local,c.right),y=M.dot(local,c.up),z=M.dot(local,c.center);
    if(z<-.001)return null;
    const r=Math.hypot(x,y),factor=r<1e-10?c.scale:c.scale*Math.acos(M.clamp(z,-1,1))/r;
    return [c.width/2+x*factor,c.height/2-y*factor,local[2]];
  }
  function basis(ray){const east=unit([-ray[1],ray[0],0]);return {east,north:unit(cross(ray,east))};}
  function offset(ray,tangent,angle){return unit(ray.map((x,i)=>x*Math.cos(angle)+tangent[i]*Math.sin(angle)));}
  function screenVector(ray,tangent,c){const center=project(ray,c),end=project(offset(ray,tangent,.0001),c);return center&&end?[end[0]-center[0],end[1]-center[1]]:[0,-1];}
  function moonAt(epoch,latitude,longitude){
    const date=new Date(epoch),observer=new A.Observer(latitude,longitude,0),equ=A.Equator(A.Body.Moon,date,observer,false,true),ray=unit(xyz(equ.vec));
    const c=M.camera(date,latitude,longitude,0,90,150,320,680),local=M.mv(c.local,ray),geo=xyz(A.GeoMoon(date)),sun=xyz(A.GeoVector(A.Body.Sun,date,true));
    const light=unit(sun.map((x,i)=>x-geo[i])),illum=A.Illumination(A.Body.Moon,date),phase=A.MoonPhase(date),lib=A.Libration(date),axis=A.RotationAxis(A.Body.Moon,date);
    return {ray,light,altitude:Math.asin(M.clamp(local[2],-1,1))/M.D,azimuth:(Math.atan2(local[0],local[1])/M.D+360)%360,distanceKm:equ.dist*149597870.7,angularRadius:Math.asin(1737.4/(equ.dist*149597870.7)),fraction:illum.phase_fraction,phase,phaseName:phase<10||phase>350?'新月':phase<80?'娥眉月':phase<100?'上弦月':phase<170?'盈凸月':phase<190?'满月':phase<260?'亏凸月':phase<280?'下弦月':'残月',libration:{latitude:lib.elat,longitude:lib.elon},north:unit(xyz(axis.north))};
  }
  function moonLighting(moon,c){
    const {east,north}=basis(moon.ray),e=screenVector(moon.ray,east,c),n=screenVector(moon.ray,north,c);
    const se=M.dot(moon.light,east),sn=M.dot(moon.light,north),sx=e[0]*se+n[0]*sn,sy=e[1]*se+n[1]*sn,len=Math.hypot(sx,sy)||1;
    const z=M.clamp(-M.dot(moon.light,moon.ray),-1,1),xy=Math.sqrt(Math.max(0,1-z*z));
    const pe=M.dot(moon.north,east),pn=M.dot(moon.north,north),px=e[0]*pe+n[0]*pn,py=e[1]*pe+n[1]*pn,pl=Math.hypot(px,py)||1;
    return {light:[sx/len*xy,sy/len*xy,z],north:[px/pl,py/pl]};
  }
  scope.StarCelestial={unit,cross,project,basis,offset,screenVector,moonAt,moonLighting};
})(typeof window!=='undefined'?window:globalThis);
