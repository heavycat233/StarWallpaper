/* Astronomical geometry for a wide-field visual preview. Angles are radians unless noted. */
(function (scope) {
  'use strict';
  const D = Math.PI / 180, TAU = Math.PI * 2;
  const clamp = (x, lo, hi) => Math.max(lo, Math.min(hi, x));
  const dot = (a, b) => a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
  const mv = (m, v) => m.map(row => dot(row, v));
  const transpose = m => m[0].map((_, i) => m.map(row => row[i]));
  const mm = (a, b) => a.map(row => transpose(b).map(col => dot(row, col)));
  const rz = a => [[Math.cos(a), -Math.sin(a), 0], [Math.sin(a), Math.cos(a), 0], [0, 0, 1]];
  function equatorial(raHours, decDegrees) {
    const ra = raHours * 15 * D, dec = decDegrees * D;
    return [Math.cos(dec) * Math.cos(ra), Math.cos(dec) * Math.sin(ra), Math.sin(dec)];
  }
  function sidereal(date, longitude) {
    const jd = date.getTime() / 86400000 + 2440587.5;
    const t = (jd - 2451545) / 36525;
    return ((280.46061837 + 360.98564736629 * (jd - 2451545) + .000387933 * t * t - t * t * t / 38710000 + longitude) % 360 + 360) % 360 * D;
  }
  function precession(date) {
    const t = (date.getTime() / 86400000 + 2440587.5 - 2451545) / 36525;
    const zeta = (2306.2181 * t + .30188 * t * t + .017998 * t * t * t) / 3600 * D;
    const z = (2306.2181 * t + 1.09468 * t * t + .018203 * t * t * t) / 3600 * D;
    const theta = (2004.3109 * t - .42665 * t * t - .041833 * t * t * t) / 3600 * D;
    const ry = [[Math.cos(theta), 0, -Math.sin(theta)], [0, 1, 0], [Math.sin(theta), 0, Math.cos(theta)]];
    return mm(mm(rz(z), ry), rz(zeta));
  }
  function camera(date, lat, lon, azimuth, altitude, fov, width, height) {
    const lst = sidereal(date, lon), p = lat * D, az = azimuth * D, alt = altitude * D;
    const horizontal = [[-Math.sin(lst), Math.cos(lst), 0], [-Math.sin(p) * Math.cos(lst), -Math.sin(p) * Math.sin(lst), Math.cos(p)], [Math.cos(p) * Math.cos(lst), Math.cos(p) * Math.sin(lst), Math.sin(p)]];
    const local = mm(horizontal, precession(date));
    const right = [Math.cos(az), -Math.sin(az), 0];
    const up = [-Math.sin(alt) * Math.sin(az), -Math.sin(alt) * Math.cos(az), Math.cos(alt)];
    const center = [Math.cos(alt) * Math.sin(az), Math.cos(alt) * Math.cos(az), Math.sin(alt)];
    const scale = Math.hypot(width, height) / (fov * D);
    return { local, inverse: transpose(local), right, up, center, width, height, scale };
  }
  function project(vector, c) {
    const v = mv(c.local, vector);
    if (v[2] < 0) return null;
    const x = dot(v, c.right), y = dot(v, c.up), z = dot(v, c.center);
    if (z < -.001) return null;
    const theta = Math.acos(clamp(z, -1, 1)), r = Math.hypot(x, y);
    const factor = r < 1e-10 ? c.scale : c.scale * theta / r;
    return [c.width / 2 + x * factor, c.height / 2 - y * factor, v[2]];
  }
  function unproject(x, y, c) {
    const dx = (x - c.width / 2) / c.scale, dy = (c.height / 2 - y) / c.scale;
    const theta = Math.hypot(dx, dy), s = theta < 1e-10 ? 1 : Math.sin(theta) / theta;
    const local = c.center.map((v, i) => v * Math.cos(theta) + (c.right[i] * dx + c.up[i] * dy) * s);
    return { local, equatorial: mv(c.inverse, local) };
  }
  // ICRS/J2000 to IAU galactic coordinates, standard rotation.
  const GALACTIC = [[-.0548755604, -.8734370902, -.4838350155], [.4941094279, -.4448296300, .7469822445], [-.8676661490, -.1980763734, .4559837762]];
  function galactic(v) { const g = mv(GALACTIC, v); return [Math.atan2(g[1], g[0]), Math.asin(clamp(g[2], -1, 1))]; }
  function galaxyFraming(date, lat, lon) {
    const c = camera(date, lat, lon, 0, 90, 150, 320, 680), inverse = transpose(GALACTIC);
    let best = { altitude: -90, azimuth: 0 };
    for (let l = 0; l < 360; l += 3) {
      const v = mv(c.local, mv(inverse, [Math.cos(l * D), Math.sin(l * D), 0]));
      const altitude = Math.asin(clamp(v[2], -1, 1)) / D;
      if (altitude > best.altitude) best = { altitude, azimuth: (Math.atan2(v[0], v[1]) / D + 360) % 360 };
    }
    return { azimuth: Math.round((best.azimuth - 20 + 360) % 360), altitude: Math.round(clamp(best.altitude + 20, 25, 80)) };
  }
  const api = { D, TAU, clamp, dot, mv, equatorial, sidereal, precession, camera, project, unproject, galactic, galaxyFraming };
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  else scope.StarSky = api;
})(typeof window !== 'undefined' ? window : globalThis);
