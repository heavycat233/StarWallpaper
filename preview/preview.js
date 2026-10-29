(async function () {
  'use strict';
  const root = document.getElementById('starwall-preview');
  const $ = selector => root.querySelector(selector);
  const M = window.StarSky, C = window.StarCelestial, celestialData = window.STAR_CELESTIAL_DATA;
  const cities = {
    hongkong: { name: '香港', lat: 22.3193, lon: 114.1694, zone: 'Asia/Hong_Kong' },
    lhasa: { name: '拉萨', lat: 29.652, lon: 91.172, zone: 'Asia/Shanghai' },
    lijiang: { name: '丽江', lat: 26.872, lon: 100.230, zone: 'Asia/Shanghai' },
    sydney: { name: '悉尼', lat: -33.8688, lon: 151.2093, zone: 'Australia/Sydney' },
    atacama: { name: '阿塔卡马', lat: -23.023, lon: -67.755, zone: 'America/Santiago' },
    reykjavik: { name: '雷克雅未克', lat: 64.1466, lon: -21.9426, zone: 'Atlantic/Reykjavik' }
  };
  const state = { clock: 'digital', uiMode: 'minimal', magnitude: 8, faintBrightness: 180, brightSize: 75, nebulae: true, nebulaBrightness: 140, nebulaSize: 250, moon: true, moonSize: 8, example: false, celestialSceneVersion: 1, fov: 147, glow: 148, dust: 50, twinkle: !matchMedia('(prefers-reduced-motion: reduce)').matches, twinkleStrength: 140, labels: false, constellations: false, city: 'lhasa', ...cities.lhasa, azimuth: 270, altitude: 90, live: true, epoch: Date.now() };
  const design = { clockScale: 100, clockTop: 13, ink: '#f2eddf' };
  let records = [], visible = [], namedVisible = [], camera, busy = false, frame = 0, lastFrame = -Infinity, lastGeometry = 0, timer, width = 320, height = 680, dpr = 1, destroyed = false;
  const baseCanvas = $('.sw-sky'), dynamicCanvas = $('.sw-stars');
  const ctx = baseCanvas.getContext('2d'), dyn = dynamicCanvas.getContext('2d');
  const staticStars = document.createElement('canvas'), sctx = staticStars.getContext('2d');
  const galaxyCanvas = document.createElement('canvas'), gctx = galaxyCanvas.getContext('2d');
  const horizonCanvas = document.createElement('canvas'), hctx = horizonCanvas.getContext('2d');
  const nebulaCanvas = document.createElement('canvas'), nctx = nebulaCanvas.getContext('2d');
  const moonCanvas = document.createElement('canvas'), mctx = moonCanvas.getContext('2d');
  const nebulaSprites = new Map();
  let moonTexture, moon, moonDisk = null, returnView = null;
  const analog = $('.sw-analog'), actx = analog.getContext('2d');
  let galaxyKey = '', cachedGalaxy, lastClockSecond = -1;
  const chineseNames = { Vega: '织女星', Altair: '牛郎星', Deneb: '天津四', Sirius: '天狼星', Capella: '五车二', Arcturus: '大角星', Rigel: '参宿七', Betelgeuse: '参宿四', Antares: '心宿二', Spica: '角宿一', Fomalhaut: '北落师门', Aldebaran: '毕宿五', Polaris: '北极星', Canopus: '老人星', Achernar: '水委一', 'Rigil Kentaurus': '南门二', Procyon: '南河三', Pollux: '北河三', Regulus: '轩辕十四' };
  function parts(epoch = state.epoch, zone = state.zone) {
    const p = new Intl.DateTimeFormat('en-GB', { timeZone: zone, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23' }).formatToParts(new Date(epoch));
    return Object.fromEntries(p.filter(v => v.type !== 'literal').map(v => [v.type, v.value]));
  }
  function localInput() { const p = parts(); return `${p.year}-${p.month}-${p.day}T${p.hour}:${p.minute}`; }
  function localToEpoch(value) {
    const desired = new Date(value + ':00Z').getTime();
    if (!Number.isFinite(desired)) throw new Error('请输入有效的日期与时间。');
    let guess = desired;
    for (let i = 0; i < 4; i++) {
      const p = parts(guess);
      const projected = Date.UTC(+p.year, +p.month - 1, +p.day, +p.hour, +p.minute, +p.second);
      const next = guess + desired - projected;
      if (Math.abs(next - guess) < 1000) break;
      guess = next;
    }
    const p = parts(guess);
    if (`${p.year}-${p.month}-${p.day}T${p.hour}:${p.minute}` !== value) throw new Error('该当地时间处于夏令时跳转空档，请选择另一个时间。');
    return guess;
  }
  function error(message = '') { $('.sw-error').hidden = !message; $('.sw-error').textContent = message; }
  function remember() {
    const data = { ...state, epoch: state.live ? null : state.epoch };
    const snapshot = { modelContent: { clock: state.clock, uiMode: state.uiMode, magnitude: state.magnitude, faintBrightness: state.faintBrightness, brightSize: state.brightSize, nebulae: state.nebulae, nebulaBrightness: state.nebulaBrightness, nebulaSize: state.nebulaSize, moon: state.moon, moonSize: state.moonSize, example: state.example, celestialSceneVersion: 1, fov: state.fov, city: state.city, location: state.name, latitude: state.lat, longitude: state.lon, timezone: state.zone, live: state.live, date: state.live ? '实时' : new Date(state.epoch).toISOString(), glow: state.glow, dust: state.dust, twinkle: state.twinkle, twinkleStrength: state.twinkleStrength, labels: state.labels, constellations: state.constellations, azimuth: state.azimuth, altitude: state.altitude, viewingDirection: state.altitude === 90 ? '天顶' : directions(state.azimuth) }, privateContent: { settings: data, design: { ...design }, returnView } };
    if (window.openai?.setWidgetState) window.openai.setWidgetState(snapshot).catch(() => {});
  }
  function restore(snapshot) {
    const publicState = snapshot?.modelContent || (snapshot && !snapshot.privateContent ? snapshot : null);
    const input = snapshot?.privateContent?.settings || (publicState && { ...publicState, name: publicState.location ?? publicState.name, lat: publicState.latitude ?? publicState.lat, lon: publicState.longitude ?? publicState.lon, zone: publicState.timezone ?? publicState.zone, epoch: publicState.epoch ?? Date.parse(publicState.date) });
    if (!input || typeof input !== 'object') return;
    for (const key of ['magnitude', 'faintBrightness', 'brightSize', 'nebulaBrightness', 'nebulaSize', 'moonSize', 'fov', 'glow', 'dust', 'twinkleStrength', 'lat', 'lon', 'azimuth', 'altitude']) {
      const bounds = { magnitude: [3, 11], faintBrightness: [50, 400], brightSize: [50, 100], nebulaBrightness: [0, 250], nebulaSize: [100, 400], moonSize: [1, 12], fov: [120, 180], glow: [0, 200], dust: [0, 150], twinkleStrength: [0, 200], lat: [-90, 90], lon: [-180, 180], azimuth: [0, 359], altitude: [0, 90] }[key];
      if (Number.isFinite(input[key])) state[key] = M.clamp(input[key], ...bounds);
    }
    if (input.clock === 'analog' || input.clock === 'digital') state.clock = input.clock;
    if (input.uiMode === 'standard' || input.uiMode === 'minimal') state.uiMode = input.uiMode;
    if (cities[input.city] || input.city === 'custom') state.city = input.city;
    if (typeof input.name === 'string') state.name = input.name.slice(0, 30);
    for (const key of ['twinkle', 'labels', 'constellations', 'live', 'nebulae', 'moon', 'example']) if (typeof input[key] === 'boolean') state[key] = input[key];
    if (Array.from($('#sw-zone').options).some(o => o.value === input.zone)) state.zone = input.zone;
    state.epoch = !state.live && Number.isFinite(input.epoch) && input.epoch >= Date.UTC(1900, 0, 1) && input.epoch <= Date.UTC(2101, 0, 1) ? input.epoch : Date.now();
    const d = snapshot.privateContent?.design;
    if (d && Number.isFinite(d.clockScale)) design.clockScale = M.clamp(d.clockScale, 80, 120);
    if (d && Number.isFinite(d.clockTop)) design.clockTop = M.clamp(d.clockTop, 8, 25);
    if (d && /^#[0-9a-f]{6}$/i.test(d.ink)) design.ink = d.ink;
    const rv = snapshot.privateContent?.returnView;
    if (rv && Number.isFinite(rv.azimuth) && Number.isFinite(rv.altitude)) returnView = {azimuth:M.clamp(rv.azimuth,0,359),altitude:M.clamp(rv.altitude,0,90)};
    return input;
  }
  const restored = restore(window.openai?.widgetState);
  window.addEventListener('openai:set_globals', event => {
    if (event.detail?.globals?.widgetState) { restore(event.detail.globals.widgetState); syncControls(); rebuild(); }
  });
  function directions(az) { return ['北', '东北', '东', '东南', '南', '西南', '西', '西北'][Math.round(az / 45) % 8]; }
  function syncControls() {
    const minimal = state.uiMode === 'minimal';
    root.dataset.uiMode = state.uiMode;
    root.querySelectorAll('[data-ui-mode]').forEach(button => button.setAttribute('aria-pressed', button.dataset.uiMode === state.uiMode));
    for (const [id, key] of [['sw-magnitude', 'magnitude'], ['sw-faint-brightness', 'faintBrightness'], ['sw-bright-size','brightSize'], ['sw-nebula-brightness','nebulaBrightness'], ['sw-nebula-size','nebulaSize'], ['sw-moon-size','moonSize'], ['sw-fov', 'fov'], ['sw-glow', 'glow'], ['sw-dust', 'dust'], ['sw-twinkle-strength', 'twinkleStrength'], ['sw-azimuth', 'azimuth'], ['sw-altitude', 'altitude'], ['sw-lat', 'lat'], ['sw-lon', 'lon'], ['sw-city', 'city'], ['sw-zone', 'zone']]) $('#' + id).value = state[key];
    $('#sw-nebulae').checked=state.nebulae; $('#sw-moon').checked=state.moon;
    $('#sw-bright-value').textContent=state.brightSize+'%';
    $('#sw-nebula-bright-value').textContent=state.nebulaBrightness+'%';
    $('#sw-nebula-size-value').textContent=(state.nebulaSize/100).toFixed(1)+'×';
    $('#sw-moon-value').textContent=state.moonSize+'×';
    $('#sw-twinkle').checked = state.twinkle; $('#sw-labels').checked = state.labels && !minimal; $('#sw-constellations').checked = state.constellations && !minimal;
    $('#sw-twinkle-value').textContent=state.twinkleStrength+'%';
    $('#sw-mag-value').textContent = `${state.magnitude} 等`;
    $('#sw-faint-value').textContent = `${state.faintBrightness}%`;
    $('#sw-fov-value').textContent = `${state.fov}°`;
    $('#sw-glow-value').textContent = `${state.glow}%`;
    $('#sw-dust-value').textContent = `${state.dust}%`;
    $('#sw-az-value').textContent = `${state.azimuth}°`;
    $('#sw-alt-value').textContent = `${state.altitude}°`;
    const zenith = state.altitude === 90;
    root.querySelectorAll('[data-azimuth]').forEach(button => button.setAttribute('aria-pressed', !zenith && Number(button.dataset.azimuth) === state.azimuth));
    $('.sw-zenith').setAttribute('aria-pressed', zenith);
    $('.sw-view-value').textContent = zenith ? '天顶 · 仰角 90°' : `${directions(state.azimuth)} ${state.azimuth}° · 仰角 ${state.altitude}°`;
    $('#sw-datetime').value = localInput();
    $('.sw-zone-label').textContent = state.zone;
    $('.sw-place').textContent = state.name;
    $('.sw-location-kind').textContent = state.city === 'custom' ? '自定义坐标' : '手动预设';
    $('.sw-coordinates').textContent = `${Math.abs(state.lat).toFixed(2)}° ${state.lat < 0 ? 'S' : 'N'} / ${Math.abs(state.lon).toFixed(2)}° ${state.lon < 0 ? 'W' : 'E'}`;
    $('.sw-direction').textContent = zenith ? '天顶 · 90°' : `${directions(state.azimuth)} · ${state.azimuth}° / 仰角 ${state.altitude}°`;
    baseCanvas.setAttribute('aria-label', `真实星空：${state.name}，${zenith ? '天顶' : directions(state.azimuth) + '方位角' + state.azimuth + '度'}，仰角${state.altitude}度`);
    $('.sw-local-date').textContent = `当地星空 · ${state.live ? '实时' : '指定时间'}`;
    const p=parts();
    $('.sw-scene-info').textContent=`${state.example?'夜景示例':state.live?'实时星空':'指定时刻'} · ${state.name} · ${p.month}/${p.day} ${p.hour}:${p.minute}`;
    root.querySelectorAll('[data-clock]').forEach(button => button.setAttribute('aria-pressed', button.dataset.clock === state.clock));
    $('.sw-analog').hidden = state.clock !== 'analog'; $('.sw-digital').hidden = state.clock !== 'digital';
    $('.sw-clock').style.top = design.clockTop + '%';
    $('.sw-clock').style.transform = `scale(${design.clockScale / 100})`;
    $('.sw-clock').style.color = design.ink;
    updateClock(true);
  }
  function updateClock(force = false) {
    if (state.live) state.epoch = Date.now();
    const sec = Math.floor(state.epoch / 1000);
    if (!force && sec === lastClockSecond) return;
    lastClockSecond = sec;
    const p = parts();
    $('.sw-scene-info').textContent=`${state.example?'夜景示例':state.live?'实时星空':'指定时刻'} · ${state.name} · ${p.month}/${p.day} ${p.hour}:${p.minute}`;
    $('.sw-date').textContent = new Intl.DateTimeFormat('zh-CN', { timeZone: state.zone, month: 'long', day: 'numeric', weekday: 'long' }).format(new Date(state.epoch));
    $('.sw-hours').textContent = p.hour; $('.sw-minutes').textContent = p.minute;
    analog.setAttribute('aria-label', `传统指针时钟 ${p.hour}:${p.minute}:${p.second}`);
    if (state.clock !== 'analog') return;
    actx.clearRect(0, 0, 520, 520);
    actx.save(); actx.translate(260, 260);
    actx.strokeStyle = design.ink; actx.fillStyle = design.ink;
    for (let i = 0; i < 60; i++) {
      actx.save(); actx.rotate(i / 60 * M.TAU); actx.globalAlpha = i % 5 ? .28 : .88; actx.lineWidth = i % 5 ? 1 : 2.3;
      actx.beginPath(); actx.moveTo(0, -220); actx.lineTo(0, i % 5 ? -215 : -205); actx.stroke(); actx.restore();
    }
    actx.font = '400 20px "Segoe UI", sans-serif'; actx.textAlign = 'center'; actx.textBaseline = 'middle'; actx.globalAlpha = .76;
    for (let i = 1; i <= 12; i++) actx.fillText(String(i), Math.sin(i / 12 * M.TAU) * 179, -Math.cos(i / 12 * M.TAU) * 179);
    const hand = (angle, length, lineWidth, alpha) => {
      actx.save(); actx.rotate(angle); actx.lineWidth = lineWidth; actx.lineCap = 'round'; actx.globalAlpha = alpha;
      actx.beginPath(); actx.moveTo(0, 13); actx.lineTo(0, -length); actx.stroke(); actx.restore();
    };
    hand((+p.hour % 12 + +p.minute / 60) / 12 * M.TAU, 112, 6, 1);
    hand((+p.minute + +p.second / 60) / 60 * M.TAU, 164, 3.6, 1);
    actx.strokeStyle = '#d4b88b'; hand(+p.second / 60 * M.TAU, 181, 1.6, .85);
    actx.globalAlpha = 1; actx.fillStyle = design.ink; actx.beginPath(); actx.arc(0, 0, 5, 0, M.TAU); actx.fill(); actx.restore();
  }
  function hash(x, y, z) {
    let h = Math.imul(x, 374761393) + Math.imul(y, 668265263) + Math.imul(z, 2147483647);
    h = Math.imul(h ^ h >>> 13, 1274126177); return ((h ^ h >>> 16) >>> 0) / 4294967295;
  }
  function noise(x, y, z) {
    const ix = Math.floor(x), iy = Math.floor(y), iz = Math.floor(z);
    let fx = x - ix, fy = y - iy, fz = z - iz;
    fx = fx * fx * (3 - 2 * fx); fy = fy * fy * (3 - 2 * fy); fz = fz * fz * (3 - 2 * fz);
    const mix = (a, b, t) => a + (b - a) * t;
    return mix(mix(mix(hash(ix, iy, iz), hash(ix + 1, iy, iz), fx), mix(hash(ix, iy + 1, iz), hash(ix + 1, iy + 1, iz), fx), fy), mix(mix(hash(ix, iy, iz + 1), hash(ix + 1, iy, iz + 1), fx), mix(hash(ix, iy + 1, iz + 1), hash(ix + 1, iy + 1, iz + 1), fx), fy), fz);
  }
  function paintGalaxy() {
    const key = [Math.floor(state.epoch / 60000), state.lat, state.lon, state.azimuth, state.altitude, state.fov, state.glow, state.dust, width, height].join('|');
    if (key === galaxyKey && cachedGalaxy) return;
    galaxyKey = key;
    const gw = Math.min(280, Math.ceil(width)), gh = Math.round(gw * height / width);
    galaxyCanvas.width = gw; galaxyCanvas.height = gh;
    horizonCanvas.width=gw; horizonCanvas.height=gh;
    const c = M.camera(new Date(state.epoch), state.lat, state.lon, state.azimuth, state.altitude, state.fov, gw, gh);
    const image = gctx.createImageData(gw, gh), data = image.data, mask=hctx.createImageData(gw,gh);
    for (let y = 0; y < gh; y++) for (let x = 0; x < gw; x++) {
      const i = (y * gw + x) * 4, v = M.unproject(x + .5, y + .5, c), q = v.equatorial;
      if (v.local[2] <= 0) { data[i] = 3; data[i + 1] = 6; data[i + 2] = 12; data[i + 3] = 255; continue; }
      mask.data[i+3]=255;
      const [l, b] = M.galactic(q);
      const n1 = noise(q[0] * 11 + 23, q[1] * 11 + 14, q[2] * 11 + 7);
      const n2 = noise(q[0] * 37 + 71, q[1] * 37 + 53, q[2] * 37 + 17);
      const n3 = noise(q[0] * 95 + 59, q[1] * 95 + 2, q[2] * 95 + 43);
      const turbulence = n1 * .55 + n2 * .3 + n3 * .15;
      const latitude = b + (n1 - .5) * .08;
      const broad = Math.exp(-latitude * latitude / .035), middle = Math.exp(-latitude * latitude / .009), narrow = Math.exp(-latitude * latitude / .0017);
      const center = Math.exp(-(Math.atan2(Math.sin(l), Math.cos(l)) ** 2) / .8);
      const density = .72 + center * 1.3 + .3 * Math.sin(l * 3 + 1);
      const lane = Math.exp(-((b - .018 * Math.sin(l * 5) - (n2 - .5) * .045) ** 2) / .00085);
      const dark = lane * (.46 + .5 * n1) * state.dust / 100;
      const g = Math.max(0, (broad * 10 + middle * 20 * turbulence + narrow * 17 * turbulence) * density * (1 - Math.min(.94, dark)) * state.glow / 100);
      const vignette = Math.max(.43, 1 - ((x / gw - .5) ** 2 + (y / gh - .5) ** 2) * 1.05);
      const horizon = M.clamp(v.local[2] * 8, .15, 1);
      data[i] = Math.min(255, (5 + g * (.84 + center * .28)) * vignette * horizon);
      data[i + 1] = Math.min(255, (10 + g * (.88 + center * .12)) * vignette * horizon);
      data[i + 2] = Math.min(255, (23 + g * 1.17) * vignette * horizon);
      data[i + 3] = 255;
    }
    gctx.putImageData(image, 0, 0); hctx.putImageData(mask,0,0); cachedGalaxy = true;
  }
  function nebulaSprite(nebula,index) {
    if(nebulaSprites.has(nebula.id))return nebulaSprites.get(nebula.id);
    const canvas=document.createElement('canvas'); canvas.width=canvas.height=128;
    const context=canvas.getContext('2d'),image=context.createImageData(128,128);
    for(let y=0;y<128;y++)for(let x=0;x<128;x++){
      const nx=(x-63.5)/64,ny=(y-63.5)/64,r=Math.hypot(nx,ny),i=(y*128+x)*4;
      const a=noise(nx*4+index*19,ny*4+13,7),b=noise(nx*13+8,ny*13+index*9,19);
      const envelope=Math.exp(-r*r*4.5)*M.clamp((1-r)*6,0,1);
      let density=(.22+.9*a+.35*b)*envelope;
      if(nebula.id==='NGC2237')density*=.35+.8*Math.exp(-((r-.42)**2)/.025);
      const dark=.7*Math.exp(-((nx+.18*Math.sin(ny*8))**2)/.022)*(.25+.75*b);
      density*=1-dark;
      for(let k=0;k<3;k++)image.data[i+k]=nebula.color[k]*(.85+.3*b);
      image.data[i+3]=Math.round(255*M.clamp(density*.82*nebula.intensity,0,1));
    }
    context.putImageData(image,0,0);nebulaSprites.set(nebula.id,canvas);return canvas;
  }
  function paintNebulae(){
    let count=0;
    nctx.setTransform(dpr,0,0,dpr,0,0);nctx.clearRect(0,0,width,height);
    if(state.nebulae&&state.nebulaBrightness>0){
      nctx.globalCompositeOperation='screen';
      celestialData.nebulae.forEach((nebula,index)=>{
        const ray=M.equatorial(nebula.ra,nebula.dec),p=C.project(ray,camera);
        if(!p||p[2]<0)return;
        const {east,north}=C.basis(ray),pa=nebula.pa*M.D;
        const major=north.map((v,i)=>v*Math.cos(pa)+east[i]*Math.sin(pa));
        const minor=east.map((v,i)=>v*Math.cos(pa)-north[i]*Math.sin(pa));
        const factor=state.nebulaSize/100*M.D/120;
        const q=C.project(C.offset(ray,major,nebula.major*factor),camera),r=C.project(C.offset(ray,minor,nebula.minor*factor),camera);
        if(!q||!r)return;
        const ax=q[0]-p[0],ay=q[1]-p[1],bx=r[0]-p[0],by=r[1]-p[1],extent=Math.hypot(ax,ay)+Math.hypot(bx,by);
        if(p[0]+extent<0||p[0]-extent>width||p[1]+extent<0||p[1]-extent>height)return;
        count++;
        nctx.save();nctx.transform(bx/64,by/64,ax/64,ay/64,p[0],p[1]);
        const strength=state.nebulaBrightness/100;
        for(let pass=0;pass<Math.ceil(strength);pass++){
          nctx.globalAlpha=Math.min(1,strength-pass);nctx.drawImage(nebulaSprite(nebula,index),-64,-64);
        }
        nctx.restore();
      });
      nctx.globalCompositeOperation='destination-in';nctx.globalAlpha=1;nctx.drawImage(horizonCanvas,0,0,width,height);
      nctx.globalCompositeOperation='source-over';ctx.drawImage(nebulaCanvas,0,0,width,height);
    }
    root.dataset.nebulaCount=String(count);
  }
  function paintMoon(){
    moonDisk=null;
    moon=C.moonAt(state.epoch,state.lat,state.lon);
    const p=C.project(moon.ray,camera),radius=camera.scale*moon.angularRadius*state.moonSize;
    const inView=moon.altitude>0&&p&&p[0]+radius>0&&p[0]-radius<width&&p[1]+radius>0&&p[1]-radius<height;
    const status=moon.altitude<=0?'地平线下':inView?'当前视野内':'视野外';
    $('.sw-moon-status').textContent=`${moon.phaseName} · 照亮 ${(moon.fraction*100).toFixed(1)}% · 仰角 ${moon.altitude.toFixed(1)}° · ${state.moon?status:'已隐藏'}`;
    $('.sw-focus-moon').disabled=moon.altitude<=0;
    root.dataset.moonVisible=String(Boolean(state.moon&&inView));
    root.dataset.moonPhase=moon.fraction.toFixed(5);
    if(!state.moon||!inView||!moonTexture)return;
    moonDisk={x:p[0],y:p[1],radius};
    const size=128,tex=celestialData.moonTexture,lighting=C.moonLighting(moon,camera),L=lighting.light,N=lighting.north;
    moonCanvas.width=moonCanvas.height=size;
    const image=mctx.createImageData(size,size),b=moon.libration.latitude*M.D,l=moon.libration.longitude*M.D;
    const center=[Math.cos(b)*Math.cos(l),Math.cos(b)*Math.sin(l),Math.sin(b)],east=[-Math.sin(l),Math.cos(l),0],north=[-Math.sin(b)*Math.cos(l),-Math.sin(b)*Math.sin(l),Math.cos(b)];
    for(let y=0;y<size;y++)for(let x=0;x<size;x++){
      const nx=(x+.5-size/2)/(size/2),ny=(y+.5-size/2)/(size/2),r2=nx*nx+ny*ny;
      if(r2>1)continue;
      const gx=p[0]+nx*radius,gy=p[1]+ny*radius;
      if(M.unproject(gx,gy,camera).local[2]<0)continue;
      const nz=Math.sqrt(1-r2),xe=nx*(-N[1])+ny*N[0],yn=nx*N[0]+ny*N[1];
      const v=center.map((n,k)=>nz*n+xe*east[k]+yn*north[k]);
      const tx=((Math.atan2(v[1],v[0])/M.TAU+.5)*tex.width+tex.width)%tex.width,ty=(.5-Math.asin(M.clamp(v[2],-1,1))/Math.PI)*tex.height;
      const sample=moonTexture[Math.min(tex.height-1,Math.floor(ty))*tex.width+Math.floor(tx)];
      const dot=nx*L[0]+ny*L[1]+nz*L[2],shade=dot>0?.45+.55*Math.sqrt(dot):.035;
      const value=(.40+.60*sample/255)*shade*245,i=(y*size+x)*4;
      image.data[i]=value;image.data[i+1]=value*.98;image.data[i+2]=value*.92;image.data[i+3]=255*M.clamp((1-Math.sqrt(r2))*size/2,0,1);
    }
    mctx.putImageData(image,0,0);
    ctx.drawImage(moonCanvas,p[0]-radius,p[1]-radius,radius*2,radius*2);
  }
  function color(bv) {
    if (bv < .0) return '174,198,255';
    if (bv < .4) return '204,218,255';
    if (bv < .85) return '239,236,225';
    if (bv < 1.4) return '250,221,184';
    return '255,190,151';
  }
  function paintStar(context, s, brightness = 1) {
    const magnitude = s[3], taper=M.clamp((6.5-magnitude)/3.5,0,1),size = Math.max(.28, 1.7 - magnitude * .17) * Math.max(.85, width / 320)*(1-(1-state.brightSize/100)*taper);
    const t = M.clamp((magnitude - 5) / 3, 0, 1), faintGain = 1 + (state.faintBrightness / 100 - 1) * t * t * (3 - 2 * t);
    const alpha = M.clamp(1.6 * Math.pow(10, -.115 * Math.max(-1, magnitude)), .08, 1) * brightness * faintGain;
    const rgb = s[4];
    if (magnitude < 3) {
      const halo=4.3*(.8+.2*state.brightSize/100),glow = context.createRadialGradient(s[0], s[1], 0, s[0], s[1], size * halo);
      glow.addColorStop(0, `rgba(${rgb},${.28 * brightness})`); glow.addColorStop(1, `rgba(${rgb},0)`);
      context.fillStyle = glow; context.fillRect(s[0] - size * halo, s[1] - size * halo, size * halo*2, size * halo*2);
    }
    context.fillStyle = `rgba(${rgb},${Math.min(1, alpha)})`; context.beginPath(); context.arc(s[0], s[1], size, 0, M.TAU); context.fill();
    if (magnitude < .7) {
      context.strokeStyle = `rgba(${rgb},${.23 * brightness})`; context.lineWidth = .55;
      const arm=5*state.brightSize/100;context.beginPath(); context.moveTo(s[0] - arm, s[1]); context.lineTo(s[0] + arm, s[1]); context.moveTo(s[0], s[1] - arm); context.lineTo(s[0], s[1] + arm); context.stroke();
    }
  }
  function paintConstellations() {
    ctx.strokeStyle='rgba(151,169,200,.52)';ctx.lineWidth=.75;
    for(const figure of window.STAR_CONSTELLATIONS.figures){
      for(const path of figure[4]){
        let previous=null;
        for(const [lon,dec] of path){
          const point=M.project(M.equatorial(lon/15,dec),camera);
          if(previous&&point){ctx.beginPath();ctx.moveTo(previous[0],previous[1]);ctx.lineTo(point[0],point[1]);ctx.stroke();}
          previous=point;
        }
      }
    }
    ctx.textAlign='center';ctx.font='10px "Segoe UI", "Microsoft YaHei", sans-serif';ctx.fillStyle='rgba(224,217,199,.78)';
    const occupied=[],named=new Set();
    for(const figure of window.STAR_CONSTELLATIONS.figures){
      if(named.has(figure[0]))continue;
      named.add(figure[0]);
      const point=M.project(M.equatorial(figure[2]/15,figure[3]),camera);
      if(!point||point[0]<25||point[0]>width-25||point[1]<25||point[1]>height-25)continue;
      if(occupied.some(other=>Math.hypot(point[0]-other[0],point[1]-other[1])<52))continue;
      ctx.fillText(figure[1],point[0],point[1]);occupied.push(point);
    }
    ctx.textAlign='start';
  }
  function rebuild() {
    if (busy || !records.length || destroyed) return;
    busy = true;
    try {
      if (state.live) state.epoch = Date.now();
      camera = M.camera(new Date(state.epoch), state.lat, state.lon, state.azimuth, state.altitude, state.fov, width, height);
      paintGalaxy();
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0); ctx.clearRect(0, 0, width, height); ctx.drawImage(galaxyCanvas, 0, 0, width, height);
      paintNebulae();
      visible = []; namedVisible = []; let shown = 0;
      sctx.setTransform(dpr, 0, 0, dpr, 0, 0); sctx.clearRect(0, 0, width, height);
      for (let i = 0; i < records.length; i++) {
        const star = records[i]; if (star.mag > state.magnitude) break;
        const p = M.project(star.vector, camera);
        if (!p || p[0] < -5 || p[0] > width + 5 || p[1] < -5 || p[1] > height + 5) continue;
        shown++;
        const s = [p[0], p[1], p[2], star.mag, star.color, i * 2.399963229728653];
        if (star.mag < 6.5) visible.push(s); else paintStar(sctx, s);
      }
      for (const n of window.STAR_CATALOGUE.named) {
        if (n[3] > state.magnitude) continue;
        const p = M.project(M.equatorial(n[1], n[2]), camera);
        if (p && p[0] > 5 && p[0] < width - 5 && p[1] > 12 && p[1] < height - 12) namedVisible.push([p[0], p[1], chineseNames[n[0]] || n[0]]);
      }
      ctx.drawImage(staticStars, 0, 0, width, height);
      if (state.constellations && state.uiMode !== 'minimal') paintConstellations();
      if (state.labels && state.uiMode !== 'minimal') {
        ctx.font = '9px "Segoe UI", "Microsoft YaHei", sans-serif'; ctx.fillStyle = 'rgba(229,223,208,.88)';
        const occupied = [];
        for (const n of namedVisible) {
          if (occupied.some(p => Math.hypot(p[0] - n[0], p[1] - n[1]) < 42)) continue;
          const x=n[0]+7+ctx.measureText(n[2]).width>width-5?n[0]-7-ctx.measureText(n[2]).width:n[0]+7;
          ctx.fillText(n[2], x, n[1] - 5); occupied.push(n);
        }
      }
      paintMoon();
      $('.sw-render-status').textContent = `当前视野 ${shown.toLocaleString('zh-CN')} 颗恒星 · ${state.magnitude} 等 / ${state.fov}°`;
      root.dataset.starCount = String(shown); root.dataset.rendered = 'true';
      lastGeometry = performance.now();
      paintDynamic(performance.now());
    } finally { busy = false; }
  }
  function paintDynamic(t) {
    dyn.setTransform(dpr, 0, 0, dpr, 0, 0); dyn.clearRect(0, 0, width, height);
    for (const s of visible) {
      if(moonDisk&&Math.hypot(s[0]-moonDisk.x,s[1]-moonDisk.y)<moonDisk.radius+1)continue;
      const strength = Math.min(.48,(.15 + .20 * (1 - s[2])) * state.twinkleStrength/100);
      const wave = state.twinkle ? 1 - strength + strength * Math.sin(t * .0013 + s[5]) * Math.sin(t * .0007 + s[5] * 1.7) : 1;
      paintStar(dyn, s, wave);
    }
    if(moonDisk)dyn.drawImage(moonCanvas,moonDisk.x-moonDisk.radius,moonDisk.y-moonDisk.radius,moonDisk.radius*2,moonDisk.radius*2);
  }
  function animate(t) {
    if (destroyed || !root.isConnected) { destroyed = true; clearInterval(timer); resize.disconnect(); return; }
    if (!document.hidden && t - lastFrame > 40) {
      if (state.twinkle) paintDynamic(t);
      updateClock();
      if (state.live && t - lastGeometry > 60000) { $('#sw-datetime').value = localInput(); rebuild(); }
      lastFrame = t;
    }
    frame = requestAnimationFrame(animate);
  }
  let pending;
  function changed(geometry = true) {
    syncControls(); error();
    if (geometry) { clearTimeout(pending); pending = setTimeout(rebuild, 65); } else paintDynamic(performance.now());
    remember();
  }
  function nightExample(){
    if(!state.example)returnView={azimuth:state.azimuth,altitude:state.altitude};
    const p=parts(Date.now()),day=Date.UTC(+p.year,+p.month-1,+p.day);
    let chosen;
    for(let d=1;d<=30;d++){
      const date=new Date(day+d*86400000).toISOString().slice(0,10),epoch=localToEpoch(date+'T01:30'),m=C.moonAt(epoch,state.lat,state.lon);
      if(m.altitude>25&&m.fraction>.15){chosen={epoch,m};break;}
    }
    if(!chosen){error('未来一个月该时段月球较低，请手动选择日期与时间。');return false;}
    state.live=false;state.example=true;state.epoch=chosen.epoch;
    state.azimuth=Math.round((chosen.m.azimuth+8)%360);state.altitude=Math.round(M.clamp(chosen.m.altitude+22,35,80));
    return true;
  }
  root.querySelectorAll('[data-clock]').forEach(button => button.addEventListener('click', () => { state.clock = button.dataset.clock; changed(false); }));
  root.querySelectorAll('[data-ui-mode]').forEach(button => button.addEventListener('click', () => { state.uiMode = button.dataset.uiMode; changed(); }));
  root.querySelectorAll('[data-azimuth]').forEach(button => button.addEventListener('click', () => {
    state.azimuth = Number(button.dataset.azimuth);
    if (state.altitude === 90) state.altitude = 45;
    changed();
  }));
  $('.sw-zenith').addEventListener('click', () => { state.altitude = 90; changed(); });
  for (const [id, key] of [['sw-magnitude', 'magnitude'], ['sw-faint-brightness', 'faintBrightness'], ['sw-bright-size','brightSize'], ['sw-nebula-brightness','nebulaBrightness'], ['sw-nebula-size','nebulaSize'], ['sw-moon-size','moonSize'], ['sw-fov', 'fov'], ['sw-glow', 'glow'], ['sw-dust', 'dust'], ['sw-twinkle-strength', 'twinkleStrength'], ['sw-azimuth', 'azimuth'], ['sw-altitude', 'altitude']]) {
    $('#' + id).addEventListener('input', event => { state[key] = Number(event.target.value); changed(); });
  }
  $('#sw-twinkle').addEventListener('change', e => { state.twinkle = e.target.checked; changed(false); });
  $('#sw-labels').addEventListener('change', e => { state.labels = e.target.checked; if(state.labels)state.uiMode='standard'; changed(); });
  $('#sw-constellations').addEventListener('change', e => { state.constellations = e.target.checked; if(state.constellations)state.uiMode='standard'; changed(); });
  $('#sw-nebulae').addEventListener('change',e=>{state.nebulae=e.target.checked;changed();});
  $('#sw-moon').addEventListener('change',e=>{state.moon=e.target.checked;changed();});
  $('.sw-night-example').addEventListener('click',()=>{if(nightExample())changed();});
  $('.sw-focus-moon').addEventListener('click',()=>{const m=C.moonAt(state.live?Date.now():state.epoch,state.lat,state.lon);if(m.altitude<=0)return;state.moon=true;state.azimuth=Math.round(m.azimuth)%360;state.altitude=Math.round(m.altitude);changed();});
  $('#sw-city').addEventListener('change', e => {
    state.example=false;
    state.city = e.target.value;
    if (cities[state.city]) Object.assign(state, cities[state.city]);
    else { state.name = '自定义位置'; $('.sw-advanced').open = true; }
    changed();
  });
  $('#sw-zone').addEventListener('change', e => { state.zone = e.target.value; changed(false); });
  for (const key of ['lat', 'lon']) $('#sw-' + key).addEventListener('change', event => {
    const value = Number(event.target.value), max = key === 'lat' ? 90 : 180;
    if (event.target.value.trim() === '' || !Number.isFinite(value) || Math.abs(value) > max) { error(`请输入 −${max}° 到 ${max}° 之间的${key === 'lat' ? '纬度' : '经度'}。`); return; }
    state[key] = value; state.city = 'custom'; state.name = '自定义位置'; state.example=false; changed();
  });
  $('#sw-datetime').addEventListener('change', event => {
    try {
      const year = Number(event.target.value.slice(0, 4));
      if (year < 1900 || year > 2100) throw new Error('预览支持 1900–2100 年。');
      state.epoch = localToEpoch(event.target.value); state.live = false; state.example=false; changed();
    } catch (e) { error(e.message); }
  });
  $('.sw-realtime').addEventListener('click', () => { if(state.example&&returnView)Object.assign(state,returnView);state.example=false;state.live = true; state.epoch = Date.now(); changed(); });
  $('.sw-frame-galaxy').addEventListener('click', () => { Object.assign(state, M.galaxyFraming(new Date(state.live ? Date.now() : state.epoch), state.lat, state.lon)); changed(); });
  const resize = new ResizeObserver(entries => {
    const rect = entries[0].contentRect;
    if (rect.width < 1 || rect.height < 1) return;
    width = rect.width; height = rect.height; dpr = Math.min(window.devicePixelRatio || 1, 2);
    for (const c of [baseCanvas, dynamicCanvas, staticStars, nebulaCanvas]) { c.width = Math.round(width * dpr); c.height = Math.round(height * dpr); }
    galaxyKey = ''; rebuild();
  });
  resize.observe($('.sw-phone'));
  syncControls();
  try {
    const bytes = Uint8Array.from(atob(window.STAR_CATALOGUE.data), c => c.charCodeAt(0));
    const stream = new Blob([bytes]).stream().pipeThrough(new DecompressionStream('gzip'));
    const buffer = await new Response(stream).arrayBuffer();
    const view = new Uint8Array(buffer),N=window.STAR_CATALOGUE.count;
    if (buffer.byteLength !== window.STAR_CATALOGUE.count * 6) throw new Error('星表数据校验失败');
    for (let i = 0; i < N; i++) {
      const ra = (view[i]|view[N+i]<<8) / 65535 * 24, dec = ((view[N*2+i]|view[N*3+i]<<8)<<16>>16) / 32767 * 90;
      const mag = view[N*4+i] / 16 - 2, bv = view[N*5+i] / 64 - .5;
      records.push({ vector: M.equatorial(ra, dec), mag, color: color(bv) });
    }
    const textureBytes=Uint8Array.from(atob(celestialData.moonTexture.data),c=>c.charCodeAt(0));
    moonTexture=new Uint8Array(await new Response(new Blob([textureBytes]).stream().pipeThrough(new DecompressionStream('gzip'))).arrayBuffer());
    if(moonTexture.length!==celestialData.moonTexture.width*celestialData.moonTexture.height)throw new Error('月面纹理数据校验失败');
    if(!restored?.celestialSceneVersion)nightExample();
    syncControls();rebuild(); frame = requestAnimationFrame(animate);
  } catch (e) { error(`真实星表未能载入：${e.message}`); $('.sw-render-status').textContent = '星表未载入'; }
  if (globalThis.Tweak) {
    const tweak = new Tweak({ container: $('.sw-phone'), onChange: () => { syncControls(); remember(); } });
    tweak.addSlider(design, 'clockScale', { min: 80, max: 120, label: '时钟大小', unit: '%' });
    tweak.addSlider(design, 'clockTop', { min: 8, max: 25, label: '时钟距顶部', unit: '%' });
    tweak.addColorPicker(design, 'ink', { label: '时钟颜色' });
  }
})();
