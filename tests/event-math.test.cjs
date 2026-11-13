const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const M=global.StarSky=require('../preview/sky-math.js');
const E=require('../preview/event-math.js');
const showers=JSON.parse(fs.readFileSync(path.join(__dirname,'../data/meteor-showers.json'),'utf8')).showers;
const galaxies=JSON.parse(fs.readFileSync(path.join(__dirname,'../data/galaxies.json'),'utf8'));
const camera=date=>M.camera(new Date(date),90,0,0,90,150,320,680);

test('annual shower peak and activity windows follow the solar calendar',()=>{
  const per=showers.find(s=>s.id==='PER');
  const at=date=>E.showerWeights([per],Date.parse(date),camera(date))[0];
  assert.ok(at('2026-08-13T00:00:00Z')>at('2026-07-20T00:00:00Z')*4);
  assert.equal(at('2026-09-01T00:00:00Z'),0);
  assert.ok(E.solarDistance(E.solarLongitude(Date.parse('2026-08-13T00:00:00Z')),per.peakSolar)<2);
  const qua=showers.find(s=>s.id==='QUA');
  assert.ok(E.showerWeights([qua],Date.parse('2026-01-03T00:00:00Z'),camera('2026-01-03T00:00:00Z'))[0]>0);
  assert.equal(E.showerWeights([qua],Date.parse('2026-02-01T00:00:00Z'),camera('2026-02-01T00:00:00Z'))[0],0);
});

test('population index makes bright apparent meteors rare',()=>{
  const r=2.6,weights=Array.from({length:9},(_,i)=>r**(i-3));
  const bright=weights.slice(0,4).reduce((a,b)=>a+b,0)/weights.reduce((a,b)=>a+b,0);
  assert.ok(bright<.02);
  assert.equal(E.meteorMagnitude(r,()=>0),-3);
  assert.equal(E.meteorMagnitude(r,()=>.99999),5);
});

test('galaxy source coordinates and packaged copies agree',()=>{
  assert.equal(galaxies.length,2);
  assert.ok(Math.abs(galaxies[0].ra-.7123194)<1e-7);
  assert.ok(Math.abs(galaxies[1].ra-1.5641361)<1e-7);
  for(const name of ['galaxies.json','meteor-showers.json']){
    assert.equal(fs.readFileSync(path.join(__dirname,'../data',name),'utf8'),fs.readFileSync(path.join(__dirname,'../android/app/src/main/assets',name),'utf8'));
  }
});

test('zodiacal glow is confined to sun-below-horizon twilight',()=>{
  const date='2026-03-20';
  const values=Array.from({length:24},(_,hour)=>{
    const epoch=Date.parse(`${date}T${String(hour).padStart(2,'0')}:00:00Z`);
    return E.zodiacalPhase(M.camera(new Date(epoch),0,0,90,10,150,320,680),epoch);
  });
  assert.ok(values.some(value=>value>.1));
  assert.ok(values.some(value=>value===0));
});
