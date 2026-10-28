const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const zlib = require('node:zlib');
const path = require('node:path');
const M = require('../preview/sky-math.js');
const epoch = new Date('2000-01-01T12:00:00Z');
const close = (a, b, tolerance = 1e-8) => assert.ok(Math.abs(a - b) < tolerance, `${a} != ${b}`);

test('Greenwich sidereal time agrees with J2000 reference', () => {
  close(M.sidereal(epoch, 0) / M.D, 280.46061837);
  close(M.sidereal(new Date(+epoch + 86400000), 0) / M.D, 281.44626573629);
});
test('celestial pole altitude equals observer latitude in both hemispheres', () => {
  for (const latitude of [-67, -33.8688, 0, 22.3193, 65]) {
    const c = M.camera(epoch, latitude, 114, 0, 60, 150, 320, 680);
    const northPole = M.mv(c.local, [0, 0, 1]);
    close(Math.asin(northPole[2]) / M.D, latitude);
  }
});
test('a star on the meridian at observer declination is at the zenith', () => {
  for (const latitude of [-33.8688, 22.3193, 89.99]) {
    const c = M.camera(epoch, latitude, 151, 0, 90, 150, 320, 680);
    const star = M.equatorial(M.sidereal(epoch, 151) / M.D / 15, latitude);
    const p = M.project(star, c);
    close(p[0], 160, 1e-5); close(p[1], 340, 1e-5); close(p[2], 1);
    assert.equal(M.project(star.map(v => -v), c), null);
  }
});
test('fisheye projection and inverse remain consistent across dates and view fields', () => {
  for (const date of [epoch, new Date('2026-10-28T14:00Z'), new Date('2050-06-01T02:00Z')]) {
    for (const fov of [120, 150, 180]) {
      const c = M.camera(date, -33.8688, 151.2093, 237, 65, fov, 320, 680);
      for (const x of [0, 40, 160, 280, 320]) for (const y of [0, 80, 340, 600, 680]) {
        const v = M.unproject(x, y, c);
        close(M.dot(v.equatorial, v.equatorial), 1, 1e-8);
        if (v.local[2] > 0) {
          const p = M.project(v.equatorial, c);
          assert.ok(p); close(p[0], x, 1e-5); close(p[1], y, 1e-5);
        }
      }
      const corner = M.unproject(0, 0, c);
      close(Math.acos(M.dot(corner.local, c.center)) / M.D, fov / 2);
    }
  }
});
test('catalogue is complete relative to HYG source selection and preserves integer magnitude cuts', () => {
  const meta = JSON.parse(fs.readFileSync(path.join(__dirname, '../data/catalogue-meta.json')));
  const data = zlib.gunzipSync(Buffer.from(fs.readFileSync(path.join(__dirname, '../data/catalogue.b64'), 'utf8'), 'base64'));
  assert.equal(data.length, meta.count * 6);
  let previous = -Infinity;
  const counts = Object.fromEntries(Array.from({ length: 9 }, (_, i) => [i + 3, 0]));
  for (let i = 0; i < data.length; i += 6) {
    const magnitude = data[i + 4] / 16 - 2;
    assert.ok(magnitude >= previous); previous = magnitude;
    for (const cutoff in counts) if (magnitude <= +cutoff) counts[cutoff]++;
  }
  assert.deepEqual(counts, meta.counts);
});
