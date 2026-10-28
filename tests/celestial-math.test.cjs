const {test}=require('node:test');
const assert=require('node:assert/strict');
const M=global.StarSky=require('../preview/sky-math.js');
global.Astronomy=require('../vendor/astronomy/astronomy.common.js');
require('../preview/celestial-math.js');
const C=global.StarCelestial;
const at=date=>C.moonAt(Date.parse(date),29.652,91.172);

test('moon phase follows independently known 2024 eclipse dates',()=>{
  assert.ok(at('2024-04-08T18:21Z').fraction<.001,'total solar eclipse: new moon');
  assert.ok(at('2024-03-25T07:12Z').fraction>.999,'penumbral lunar eclipse: full moon');
});
test('topocentric moon positions, distance and angular diameter are physical',()=>{
  const m=at('2026-10-28T17:30Z');
  assert.ok(m.distanceKm>300000&&m.distanceKm<420000);
  assert.ok(m.angularRadius*2/M.D>.45&&m.angularRadius*2/M.D<.60);
  assert.ok(m.altitude>50&&m.altitude<65);
  const other=C.moonAt(Date.parse('2026-10-28T17:30Z'),-33.87,151.21);
  assert.ok(Math.acos(M.dot(m.ray,other.ray))/M.D>.1,'observer parallax changes lunar position');
});
test('moon orientation and illumination remain consistent through lunar phases',()=>{
  for(const date of ['2024-04-08T18:21Z','2024-03-25T07:12Z','2026-10-28T17:30Z']){
    const m=at(date),c=M.camera(new Date(date),29.652,91.172,m.azimuth,m.altitude,147,320,680),l=C.moonLighting(m,c);
    assert.ok(Math.abs(Math.hypot(...l.light)-1)<1e-8);
    assert.ok(Math.abs(Math.hypot(...l.north)-1)<1e-8);
    assert.ok(Math.abs((1+l.light[2])/2-m.fraction)<.015,'terminator matches illuminated fraction');
    const p=C.project(m.ray,c);
    assert.ok(Math.abs(p[0]-160)<1e-5&&Math.abs(p[1]-340)<1e-5);
    if(m.altitude>0)assert.deepEqual(p,M.project(m.ray,c));
  }
});
