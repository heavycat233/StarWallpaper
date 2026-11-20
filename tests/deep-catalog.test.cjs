const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');

const root = path.join(__dirname, '..');
const meta = JSON.parse(fs.readFileSync(path.join(root, 'data/athyg-preview-meta.json'), 'utf8'));
const source = fs.readFileSync(path.join(root, 'preview/deep-catalog.js'), 'ascii');
const prefix = 'window.STAR_DEEP_CATALOGUE=';

test('AT-HYG preview supplement decodes to the documented sky catalogue', () => {
  assert.ok(source.startsWith(prefix));
  const catalogue = JSON.parse(source.slice(prefix.length).trim().replace(/;$/, ''));
  const raw = zlib.gunzipSync(Buffer.from(catalogue.data, 'base64'));
  const count = catalogue.count;
  assert.equal(count, 822917);
  assert.equal(count, meta.additionalCount);
  assert.equal(count + meta.baseCount, meta.combinedCount);
  assert.equal(raw.length, count * 6);
  assert.deepEqual(catalogue.counts, meta.counts);

  const seen = Object.fromEntries(Array.from({length: 9}, (_, i) => [String(i + 3), 0]));
  let previousMagnitude = -1;
  for (let i = 0; i < count; i++) {
    const ra = raw[i] | (raw[count + i] << 8);
    const dec = raw[2 * count + i] | (raw[3 * count + i] << 8);
    const mag = raw[4 * count + i];
    assert.ok(ra <= 65535 && dec <= 65535);
    assert.ok(mag >= previousMagnitude && mag <= 208);
    previousMagnitude = mag;
    for (let limit = 3; limit <= 11; limit++) {
      if (mag <= (limit + 2) * 16) seen[String(limit)]++;
    }
  }
  assert.deepEqual(seen, meta.counts);
});
