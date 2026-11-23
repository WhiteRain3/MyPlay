// ORBIT parts for the Waze screen mockups, drawn as HTML at the car's 1280 x 720.
// Colours, sizes and the focus ring are ORBIT's (app/.../ui/car/myplay/orbit/OrbitTokens.kt,
// OrbitParts.kt, OrbitSkin.kt). Everything here is sample data.
const ICONS = Object.assign(require('./icons.json'), {
  minus: '<path d="M5 12h14"/>',
  briefcase: '<path d="M16 20V4a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/><rect width="20" height="14" x="2" y="6" rx="2"/>',
  'corner-up-right': '<polyline points="15 14 20 9 15 4"/><path d="M4 20v-7a4 4 0 0 1 4-4h12"/>',
  'turn-left': '<polyline points="9 14 4 9 9 4"/><path d="M20 20v-7a4 4 0 0 0-4-4H4"/>',
  move: '<polyline points="5 9 2 12 5 15"/><polyline points="9 5 12 2 15 5"/><polyline points="15 19 12 22 9 19"/><polyline points="19 9 22 12 19 15"/><line x1="2" x2="22" y1="12" y2="12"/><line x1="12" x2="12" y1="2" y2="22"/>',
  fuel: '<line x1="3" x2="15" y1="22" y2="22"/><line x1="4" x2="14" y1="9" y2="9"/><path d="M14 22V4a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v18"/><path d="M14 13h2a2 2 0 0 1 2 2v2a2 2 0 0 0 2 2a2 2 0 0 0 2-2V9.83a2 2 0 0 0-.59-1.42L18 5"/>',
  coffee: '<path d="M10 2v2"/><path d="M14 2v2"/><path d="M16 8a1 1 0 0 1 1 1v8a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V9a1 1 0 0 1 1-1h14a4 4 0 1 1 0 8h-1"/><path d="M6 2v2"/>',
  delete: '<path d="M10 5a2 2 0 0 0-1.344.519l-6.328 5.74a1 1 0 0 0 0 1.481l6.328 5.741A2 2 0 0 0 10 19h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2z"/><path d="m12 9 6 6"/><path d="m18 9-6 6"/>',
  flag: '<path d="M4 22V4a1 1 0 0 1 .4-.8A6 6 0 0 1 8 2c3 0 5 2 7.333 2q2 0 3.067-.8A1 1 0 0 1 20 4v10a1 1 0 0 1-.4.8A6 6 0 0 1 16 16c-3 0-5-2-8-2a6 6 0 0 0-4 1.528"/>',
  'map-pin-off': '<path d="M12.75 7.09a3 3 0 0 1 2.16 2.16"/><path d="M17.072 17.072c-1.634 2.17-3.527 3.912-4.471 4.727a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 1.432-4.568"/><path d="m2 2 20 20"/><path d="M8.475 2.818A8 8 0 0 1 20 10c0 1.183-.31 2.377-.81 3.533"/><path d="M9.13 9.13a3 3 0 0 0 3.74 3.74"/>',
  'space': '<path d="M22 17v1c0 .5-.5 1-1 1H3c-.5 0-1-.5-1-1v-1"/>',
  'corner-down-left': '<polyline points="9 10 4 15 9 20"/><path d="M20 4v7a4 4 0 0 1-4 4H4"/>',
});

const NIGHT = {
  night: true, glass: 'rgba(8,12,20,0.86)', edge: 'rgba(255,255,255,0.14)', track: '#24323A',
  text: '#EEF1F7', secondary: '#AAB3C8', halo: '#06080D', haloA: 'rgba(6,8,13,0.6)', background: '#0B1318',
  accent: '#6EE7A8', onAccent: '#06251A', caution: '#FFC857', warning: '#FF6B6B', music: '#A78BFA',
  row: 'rgba(255,255,255,0.06)', rowFocus: 'rgba(110,231,168,0.16)',
};
const DAY = {
  night: false, glass: 'rgba(244,246,250,0.90)', edge: 'rgba(14,18,27,0.2)', track: '#D3DAE3',
  text: '#0E121B', secondary: '#4A5468', halo: '#FFFFFF', haloA: 'rgba(255,255,255,0.6)', background: '#E9EEF2',
  accent: '#0E7A4C', onAccent: '#FFFFFF', caution: '#8A5A00', warning: '#C62828', music: '#6A3FD1',
  row: 'rgba(14,18,27,0.06)', rowFocus: 'rgba(14,122,76,0.12)',
};

/** The free area that Waze's content may use (MC-1): x 288-992, y 43-677. */
const FREE = { x: 288, y: 43, w: 704, h: 634 };

const ic = (name, size, color, stroke = 2.2) => {
  if (!ICONS[name]) throw new Error('no icon ' + name);
  return `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="${color}" stroke-width="${stroke}" stroke-linecap="round" stroke-linejoin="round" style="display:block;flex:none">${ICONS[name]}</svg>`;
};

/** ORBIT's focus ring: 5 px halo gap, 7 px accent, then a soft halo (box-shadow follows the shape). */
const focus = (t) => `box-shadow:0 0 0 5px ${t.halo},0 0 0 12px ${t.accent},0 0 0 20px ${t.haloA};`;

const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;');

// ---------------------------------------------------------------- the map (a stand-in for Waze's surface)

function rng(seed) { let s = seed; return () => (s = (s * 16807) % 2147483647) / 2147483647; }

/** A made-up street map. routes: [{d, main}] in screen px; car: {x,y,a}; pin: {x,y}. */
function map(t, { routes = [], car = null, pin = null, seed = 7, dim = 0 } = {}) {
  const r = rng(seed);
  const c = t.night
    ? { bg: '#111B22', minor: '#1F2C35', major: '#33444E', majorEdge: '#111B22', water: '#0B2533', park: '#13261D', label: '#5C6E78', main: '#4F9DFF', mainEdge: '#0B3D7A', alt: '#5E6E7A' }
    : { bg: '#E7EBE4', minor: '#FFFFFF', major: '#FFFFFF', majorEdge: '#C9D0C8', water: '#B4D2E6', park: '#CFE4C6', label: '#7A867C', main: '#2D7FF0', mainEdge: '#1A4F99', alt: '#9AA6AE' };
  let s = `<svg width="1280" height="720" style="position:absolute;left:0;top:0"><rect width="1280" height="720" fill="${c.bg}"/>`;
  s += `<path d="M-20 560 C 180 520 260 640 420 700 L 420 760 L -20 760 Z" fill="${c.water}"/>`;
  s += `<path d="M1010 -10 C 1060 120 1180 140 1300 120 L 1300 -10 Z" fill="${c.water}"/>`;
  for (let i = 0; i < 9; i++) {
    const x = r() * 1200, y = r() * 650, w = 60 + r() * 120, h = 40 + r() * 90;
    s += `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="${c.park}"/>`;
  }
  s += `<g transform="rotate(-14 640 360)">`;
  for (let x = -300; x < 1600; x += 70 + r() * 60) s += `<line x1="${x}" y1="-400" x2="${x + (r() - 0.5) * 80}" y2="1100" stroke="${c.minor}" stroke-width="${5 + r() * 3}"/>`;
  for (let y = -300; y < 1100; y += 60 + r() * 60) s += `<line x1="-400" y1="${y}" x2="1700" y2="${y + (r() - 0.5) * 80}" stroke="${c.minor}" stroke-width="${5 + r() * 3}"/>`;
  s += `</g>`;
  const majors = ['M-20 250 C 300 300 600 180 1300 260', 'M520 -20 C 560 260 470 480 560 760', 'M-20 470 C 400 430 800 520 1300 430', 'M860 -20 C 820 300 980 520 900 760'];
  for (const d of majors) s += `<path d="${d}" fill="none" stroke="${c.majorEdge}" stroke-width="20"/><path d="${d}" fill="none" stroke="${c.major}" stroke-width="15"/>`;
  for (const rt of routes.filter((x) => !x.main)) s += `<path d="${rt.d}" fill="none" stroke="${c.alt}" stroke-width="11" stroke-linecap="round" stroke-linejoin="round"/>`;
  for (const rt of routes.filter((x) => x.main)) s += `<path d="${rt.d}" fill="none" stroke="${c.mainEdge}" stroke-width="17" stroke-linecap="round" stroke-linejoin="round"/><path d="${rt.d}" fill="none" stroke="${c.main}" stroke-width="11" stroke-linecap="round" stroke-linejoin="round"/>`;
  if (pin) s += `<g transform="translate(${pin.x} ${pin.y})"><path d="M0 0 C -6 -14 -20 -22 -20 -36 A 20 20 0 1 1 20 -36 C 20 -22 6 -14 0 0Z" fill="${c.main}" stroke="${t.night ? '#fff' : '#fff'}" stroke-width="3"/><circle cy="-36" r="7" fill="#fff"/></g>`;
  if (car) s += `<g transform="translate(${car.x} ${car.y}) rotate(${car.a || 0})"><circle r="30" fill="${c.main}" opacity="0.22"/><path d="M0 -20 L 15 16 L 0 8 L -15 16 Z" fill="#fff" stroke="${c.mainEdge}" stroke-width="3" stroke-linejoin="round"/></g>`;
  if (dim) s += `<rect width="1280" height="720" fill="${t.halo}" opacity="${dim}"/>`;
  return s + '</svg>';
}

// ---------------------------------------------------------------- ORBIT's corner parts

/** A soft plate behind a part, as ORBIT's SH-1 shade. */
const plate = (t, x, y, w, h) => `<div style="position:absolute;left:${x}px;top:${y}px;width:${w}px;height:${h}px;border-radius:50%;background:radial-gradient(closest-side,${t.night ? 'rgba(6,8,13,0.5)' : 'rgba(255,255,255,0.4)'},transparent)"></div>`;

const floatText = (t, color) => `color:${color};text-shadow:0 2px 6px ${t.night ? 'rgba(6,8,13,0.85)' : 'rgba(255,255,255,0.85)'};`;

function clock(t, time = '14:07') {
  return plate(t, 1010, -20, 300, 150) + `<div style="position:absolute;right:40px;top:30px;text-align:right">
    <div style="font-size:46px;font-weight:800;line-height:46px;${floatText(t, t.text)}">${time}</div>
    <div style="font-size:22px;margin-top:6px;${floatText(t, t.secondary)}">8° · 82%</div></div>`;
}

/** The hub (192 disc, bottom left). trip: {eta, left, km, p} draws the trip ring and the arrival. */
function hub(t, { trip = null, speed = '48', focused = false } = {}) {
  let s = plate(t, -30, 430, 330, 320);
  const ring = trip ? `<svg width="212" height="212" style="position:absolute;left:-10px;top:-10px"><circle cx="106" cy="106" r="101" fill="none" stroke="${t.track}" stroke-width="7"/><circle cx="106" cy="106" r="101" fill="none" stroke="${t.accent}" stroke-width="7" stroke-linecap="round" stroke-dasharray="${2 * Math.PI * 101 * trip.p} 999" transform="rotate(-90 106 106)"/></svg>` : '';
  const inner = trip
    ? `<div style="font-size:42px;font-weight:800;line-height:42px;color:${t.text}">${trip.eta}</div>
       <div style="font-size:22px;font-weight:700;color:${trip.late ? t.caution : t.accent};margin-top:6px">${trip.left}</div>
       <div style="font-size:20px;color:${t.secondary}">${trip.km}</div>`
    : `<div style="font-size:62px;font-weight:800;line-height:62px;color:${t.text}">${speed}</div><div style="font-size:22px;color:${t.secondary}">km/h</div>`;
  s += `<div style="position:absolute;left:40px;top:488px;width:192px;height:192px">${ring}
    <div style="position:absolute;inset:0;border-radius:50%;background:${t.glass};border:2px solid ${t.edge};display:flex;flex-direction:column;align-items:center;justify-content:center;${focused ? focus(t) : ''}">${inner}</div></div>`;
  return s;
}

/** The song disc (152 ring, bottom right). */
function song(t, p = 0.38) {
  return plate(t, 1040, 470, 260, 260) + `<div style="position:absolute;left:1088px;top:528px;width:152px;height:152px">
    <svg width="152" height="152" style="position:absolute"><circle cx="76" cy="76" r="72.5" fill="none" stroke="${t.track}" stroke-width="7"/><circle cx="76" cy="76" r="72.5" fill="none" stroke="${t.music}" stroke-width="7" stroke-linecap="round" stroke-dasharray="${2 * Math.PI * 72.5 * p} 999" transform="rotate(-90 76 76)"/></svg>
    <div style="position:absolute;left:13px;top:13px;width:126px;height:126px;border-radius:50%;background:conic-gradient(from 200deg,#3B2A6E,#B4568A,#F0A35E,#3B2A6E)"></div></div>`;
}

/** ORBIT's turn: an accent disc 132 with the arrow, the distance and road floating beside it, lanes under. */
function turn(t, { dist = '300 m', road = 'Harbour Street', icon = 'corner-up-right', lanes = null } = {}) {
  let s = plate(t, -40, -40, 520, 300);
  s += `<div style="position:absolute;left:40px;top:40px;display:flex;align-items:flex-start;gap:22px">
    <div style="display:flex;flex-direction:column;align-items:center">
      <div style="width:132px;height:132px;border-radius:50%;background:${t.accent};display:flex;align-items:center;justify-content:center">${ic(icon, 78, t.onAccent, 2.6)}</div>
      ${lanes ? `<div style="margin-top:10px;max-width:132px;display:flex;gap:3px;padding:4px 8px;border-radius:99px;background:${t.glass};border:1.5px solid ${t.edge}">${lanes.map((l) => ic(l.dir === 'r' ? 'corner-up-right' : 'arrow-up', 26, l.on ? t.accent : t.secondary + '73', 2.6)).join('')}</div>` : ''}
    </div>
    <div style="min-height:132px;display:flex;flex-direction:column;justify-content:center">
      <div style="font-size:62px;font-weight:800;line-height:62px;${floatText(t, t.text)}">${dist}</div>
      <div style="font-size:26px;margin-top:4px;${floatText(t, t.secondary)}">${esc(road)}</div>
    </div></div>`;
  return s;
}

// ---------------------------------------------------------------- template content parts

/** A round icon button (an Action with an icon). title adds a pill label (a titled Action). */
function disc(t, { icon, size = 64, focused = false, on = false, title = null, iconSize = null } = {}) {
  const fill = on ? t.accent : t.glass;
  const fg = on ? t.onAccent : t.text;
  if (title) {
    return `<div style="height:${size}px;padding:0 24px 0 ${icon ? 16 : 24}px;border-radius:${size}px;background:${fill};border:2px solid ${t.edge};display:flex;align-items:center;gap:10px;font-size:24px;font-weight:700;color:${fg};${focused ? focus(t) : ''}">${icon ? ic(icon, 30, fg) : ''}${esc(title)}</div>`;
  }
  return `<div style="width:${size}px;height:${size}px;flex:none;border-radius:50%;background:${fill};border:2px solid ${t.edge};display:flex;align-items:center;justify-content:center;${focused ? focus(t) : ''}">${ic(icon, iconSize || Math.round(size * 0.47), fg)}</div>`;
}

/** The header: header action (back / app icon), the title, the header's action strip. */
function header(t, { back = true, appIcon = false, title = '', strip = [], focusedBack = false } = {}) {
  const lead = back ? disc(t, { icon: 'arrow-left', size: 64, focused: focusedBack })
    : appIcon ? `<div style="width:64px;height:64px;border-radius:50%;background:${t.track};display:flex;align-items:center;justify-content:center">${ic('map-pin', 32, t.accent)}</div>` : '';
  return `<div style="display:flex;align-items:center;gap:18px;height:72px">${lead}
    <div style="flex:1;min-width:0;font-size:32px;font-weight:800;color:${t.text};white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${esc(title)}</div>
    ${strip.map((a) => disc(t, a)).join('')}</div>`;
}

/**
 * One Row: image (icon in a disc) or place marker, title, up to two texts, the distance (a
 * DistanceSpan in the text, pulled to the end), radio (a selectable list) or chevron (browsable).
 */
function row(t, { icon = null, marker = null, title, texts = [], end = null, radio = null, chevron = false, focused = false, h = 84, big = false } = {}) {
  const ts = big ? 32 : 27;
  const lead = radio !== null
    ? `<div style="width:30px;height:30px;flex:none;border-radius:50%;border:3px solid ${radio ? t.accent : t.secondary};display:flex;align-items:center;justify-content:center;margin:0 6px">${radio ? `<div style="width:16px;height:16px;border-radius:50%;background:${t.accent}"></div>` : ''}</div>`
    : marker ? `<div style="width:52px;height:52px;flex:none;border-radius:50%;background:${t.accent};color:${t.onAccent};font-size:24px;font-weight:800;display:flex;align-items:center;justify-content:center">${marker}</div>`
    : icon ? `<div style="width:56px;height:56px;flex:none;border-radius:50%;background:${t.track};display:flex;align-items:center;justify-content:center">${ic(icon, 28, focused ? t.accent : t.secondary)}</div>` : '';
  return `<div style="min-height:${h}px;flex:none;box-sizing:border-box;border-radius:${h / 2}px;background:${focused ? t.rowFocus : t.row};padding:10px 26px 10px ${lead ? 14 : 30}px;display:flex;align-items:center;gap:16px;${focused ? focus(t) : ''}">
    ${lead}
    <div style="flex:1;min-width:0">
      <div style="font-size:${ts}px;font-weight:800;line-height:1.15;color:${t.text};white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${esc(title)}</div>
      ${texts.map((x) => `<div style="font-size:21px;line-height:1.3;color:${t.secondary};white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${esc(x)}</div>`).join('')}
    </div>
    ${end ? `<div style="font-size:22px;font-weight:700;color:${focused ? t.text : t.secondary};white-space:nowrap">${esc(end)}</div>` : ''}
    ${chevron ? ic('chevron-right', 28, t.secondary) : ''}
  </div>`;
}

/** "2 more" under a list that goes on: the knob keeps turning, the list follows. */
const more = (t, n) => `<div style="display:flex;align-items:center;justify-content:center;gap:8px;font-size:20px;color:${t.secondary};margin-top:2px">${ic('chevron-down', 22, t.secondary)}${n} more</div>`;

/** A knob hint, as ORBIT's hints: the knob glyph and a few words. */
const hint = (t, text) => `<div style="display:flex;align-items:center;gap:10px;font-size:20px;color:${t.secondary}"><svg width="26" height="26" viewBox="0 0 26 26"><circle cx="13" cy="13" r="10" fill="none" stroke="${t.secondary}" stroke-width="2.5"/><circle cx="13" cy="13" r="3.5" fill="${t.secondary}"/></svg>${text}</div>`;

/** ORBIT's spinner: a track ring with an accent arc (isLoading). */
const spinner = (t, size = 96, stroke = 8) => `<svg width="${size}" height="${size}"><circle cx="${size / 2}" cy="${size / 2}" r="${size / 2 - stroke}" fill="none" stroke="${t.track}" stroke-width="${stroke}"/><circle cx="${size / 2}" cy="${size / 2}" r="${size / 2 - stroke}" fill="none" stroke="${t.accent}" stroke-width="${stroke}" stroke-linecap="round" stroke-dasharray="${(size / 2 - stroke) * 2 * Math.PI * 0.28} 999" transform="rotate(-40 ${size / 2} ${size / 2})"/></svg>`;

/** A glass card. */
const card = (t, x, y, w, h, inner, extra = '') => `<div style="position:absolute;left:${x}px;top:${y}px;width:${w}px;height:${h}px;box-sizing:border-box;border-radius:44px;background:${t.glass};border:2px solid ${t.edge};padding:22px 24px;display:flex;flex-direction:column;gap:10px;backdrop-filter:blur(6px);${extra}">${inner}</div>`;

/** The big primary button (navigateAction, FLAG_PRIMARY). */
const primary = (t, label, { focused = true, icon = 'navigation', w = null, h = 88 } = {}) =>
  `<div style="height:${h}px;${w ? `width:${w}px;` : ''}flex:none;border-radius:${h / 2}px;background:${t.accent};color:${t.onAccent};display:flex;align-items:center;justify-content:center;gap:14px;font-size:34px;font-weight:800;${focused ? focus(t) : ''}">${ic(icon, 36, t.onAccent, 2.6)}${esc(label)}</div>`;

/** A plain button (Action in a Message / Alert). */
const button = (t, label, { focused = false, filled = false, h = 72 } = {}) =>
  `<div style="height:${h}px;padding:0 34px;border-radius:${h / 2}px;background:${filled ? t.accent : t.track};color:${filled ? t.onAccent : t.text};display:flex;align-items:center;font-size:26px;font-weight:800;${focused ? focus(t) : ''}">${esc(label)}</div>`;

function page(t, body) {
  return `<!doctype html><html><head><meta charset="utf-8"><style>
@font-face{font-family:'Roboto Flex';src:url('../../phone-app/src/fonts/RobotoFlex-wght.woff2') format('woff2');font-weight:100 1000}
html,body{margin:0;padding:0;background:${t.background}}
#s{position:relative;width:1280px;height:720px;overflow:hidden;font-family:'Roboto Flex',Inter,sans-serif;color:${t.text}}
</style></head><body><div id="s">${body}</div></body></html>`;
}

module.exports = { NIGHT, DAY, FREE, ic, focus, esc, map, plate, floatText, clock, hub, song, turn, disc, header, row, more, hint, spinner, card, primary, button, page };
