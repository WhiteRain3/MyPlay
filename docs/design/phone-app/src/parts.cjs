// Shared building blocks for the phone-app screens (HTML strings).
// Every label and help line goes through L() / H(), which enforce the plan's limits (§9.1).
const fs = require('fs');
const path = require('path');

const ICONS = JSON.parse(fs.readFileSync(path.join(__dirname, 'icons.json'), 'utf8'));
const CSS = fs.readFileSync(path.join(__dirname, 'style.css'), 'utf8');

const LIMIT_LABEL = 30, LIMIT_HELP = 60;
const L = (s) => { if (s.length > LIMIT_LABEL) throw new Error(`Label over ${LIMIT_LABEL}: "${s}" (${s.length})`); return s; };
const H = (s) => { if (s.length > LIMIT_HELP) throw new Error(`Help over ${LIMIT_HELP}: "${s}" (${s.length})`); return s; };

/** A Lucide icon (ISC licence), inline. */
function I(name, size = 24, sw = 2) {
  if (!ICONS[name]) throw new Error('No icon ' + name);
  return `<svg class="i" width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="${sw}" stroke-linecap="round" stroke-linejoin="round">${ICONS[name]}</svg>`;
}

/** MyPlay's own car glyph: outline = not connected, filled = connected (plan §4). */
const CAR_BODY = 'M5 11l1.6-4.2A2 2 0 0 1 8.5 5.5h7a2 2 0 0 1 1.9 1.3L19 11h.5a1.5 1.5 0 0 1 1.5 1.5V17a1 1 0 0 1-1 1h-1v1.2a1 1 0 0 1-1 1h-1a1 1 0 0 1-1-1V18H8v1.2a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V18H4a1 1 0 0 1-1-1v-4.5A1.5 1.5 0 0 1 4.5 11z';
function carGlyph(size = 24, filled = false) {
  if (filled) return `<svg class="i" width="${size}" height="${size}" viewBox="0 0 24 24"><path d="${CAR_BODY}" fill="currentColor"/><path d="M7.2 11l1.1-3h7.4l1.1 3z" fill="#0A1015" opacity=".75"/><circle cx="7" cy="14.5" r="1.3" fill="#0A1015"/><circle cx="17" cy="14.5" r="1.3" fill="#0A1015"/></svg>`;
  return `<svg class="i" width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"><path d="${CAR_BODY}"/><path d="M7.4 11h9.2"/><circle cx="7" cy="14.5" r="1" fill="currentColor"/><circle cx="17" cy="14.5" r="1" fill="currentColor"/></svg>`;
}

/** MyPlay mark: a ring with a planet on it (ORBIT's idea, drawn small). */
function mark(size = 28) {
  return `<svg width="${size}" height="${size}" viewBox="0 0 32 32"><circle cx="16" cy="16" r="11" fill="none" stroke="var(--a-home)" stroke-width="3"/><circle cx="16" cy="16" r="4.5" fill="#EEF1F7"/><circle cx="25" cy="9" r="3.6" fill="var(--a-screen)" stroke="#0A1015" stroke-width="2"/></svg>`;
}

/** A ring: progress, state or decoration. */
function ring(size, o = {}) {
  const w = o.w ?? 6, pct = o.pct ?? 1, r = (size - w) / 2 - (o.inset ?? 0), c = 2 * Math.PI * r;
  const col = o.color ?? 'var(--acc)';
  const track = o.track === false ? '' : `<circle cx="${size / 2}" cy="${size / 2}" r="${r}" fill="none" stroke="${o.track ?? 'var(--track)'}" stroke-width="${w}"/>`;
  const dash = o.dashed ? `stroke-dasharray="${o.dashed}"` : `stroke-dasharray="${c * pct} ${c}"`;
  const glow = o.glow ? `<circle cx="${size / 2}" cy="${size / 2}" r="${r}" fill="none" stroke="${col}" stroke-width="${w * 4}" opacity=".10" ${dash} transform="rotate(-90 ${size / 2} ${size / 2})" stroke-linecap="round"/>` : '';
  const halos = (o.halos || []).map((h) => `<circle cx="${size / 2}" cy="${size / 2}" r="${h.r}" fill="none" stroke="${col}" stroke-width="${h.w ?? 2}" opacity="${h.o}"/>`).join('');
  const fill = o.fill ? `<circle cx="${size / 2}" cy="${size / 2}" r="${r - w / 2}" fill="${o.fill}"/>` : '';
  return `<div style="position:relative;width:${size}px;height:${size}px;flex:none">
  <svg width="${size}" height="${size}" style="position:absolute;inset:0;overflow:visible">${halos}${fill}${track}${glow}<circle cx="${size / 2}" cy="${size / 2}" r="${r}" fill="none" stroke="${col}" stroke-width="${w}" ${dash} stroke-linecap="round" transform="rotate(-90 ${size / 2} ${size / 2})"/></svg>
  <div style="position:absolute;inset:0;display:grid;place-items:center;color:${o.innerColor ?? col}">${o.inner ?? ''}</div></div>`;
}

// ---------- Rows ----------
function sw(on) { return `<div class="sw${on ? ' on' : ''}"></div>`; }
function row(o) {
  const ic = o.ic ? `<div class="ic ${o.icCls ?? ''}">${o.icHtml ?? I(o.ic, 20)}</div>` : (o.lead ?? '');
  const help = (o.help ? `<div class="h">${H(o.help)}</div>` : '') + (o.under ? `<div style="margin-top:5px">${o.under}</div>` : '');
  const tag = o.tag ? ` ${o.tag}` : '';
  let right = '';
  if (o.value !== undefined) right += `<div class="val">${o.changed ? '<i class="changed"></i>' : ''}${o.value}</div>`;
  else if (o.changed) right += '<i class="changed"></i>';
  if (o.right) right += o.right;
  if (o.sw !== undefined) right += sw(o.sw);
  if (o.ext) right += `<span class="chev">${I('external-link', 18)}</span>`;
  else if (o.chev !== false && o.sw === undefined && !o.right && !o.nochev) right += `<span class="chev">${I('chevron-right', 20)}</span>`;
  const cls = ['row', o.ic || o.lead ? '' : 'noicon', o.help || o.under ? 'tall' : '', o.dim ? 'dim' : '', o.cls ?? ''].join(' ');
  return `<div class="${cls}" ${o.style ? `style="${o.style}"` : ''}>${ic}<div class="lbl"><div class="l">${L(o.label)}${tag}</div>${help}</div>${right}</div>`;
}
const card = (inner, cls = '', style = '') => `<div class="card ${cls}" ${style ? `style="${style}"` : ''}>${inner}</div>`;
const sec = (t, extra = '', first = false) => `<div class="sec${first ? ' first' : ''}" style="display:flex;align-items:center;gap:8px">${L(t)}${extra}</div>`;
const note = (t, icon) => `<div class="note">${icon ? I(icon, 16) : ''}<span>${H(t)}</span></div>`;
const tag = (t, cls = '') => `<span class="tag ${cls}">${t}</span>`;
const NEXT = tag('Next connection');
const ALSO = tag('Also on the car', 'mute');
const btn = (t, cls = 'primary', icon) => `<div class="btn ${cls}">${icon ? I(icon, 20) : ''}${L(t)}</div>`;
const seg = (opts, on) => `<div class="seg">${opts.map((x, i) => `<div class="${i === on ? 'on' : ''}">${L(x)}</div>`).join('')}</div>`;
function slider(label, value, pct, o = {}) {
  const ticks = o.ticks ? `<div class="ticks">${Array.from({ length: o.ticks }, () => '<i></i>').join('')}</div>` : '';
  const ends = o.ends ? `<div class="ends"><span>${o.ends[0]}</span><span>${o.ends[1]}</span></div>` : '';
  return `<div class="slider"><div class="head"><div class="l">${L(label)}${o.changed ? '<i class="changed"></i>' : ''}${o.tag ?? ''}</div><div class="v">${value}</div></div>
  <div class="track">${ticks}<div class="fill" style="width:${pct}%"></div><div class="thumb" style="left:${pct}%"></div></div>${ends}</div>`;
}
const appdot = (icon, color, size = 40) => `<div class="appdot" style="background:${color};width:${size}px;height:${size}px">${I(icon, size * 0.5, 2.2)}</div>`;

// ---------- Chrome ----------
function statusBar() {
  const bat = `<svg width="26" height="14" viewBox="0 0 26 14"><rect x="1" y="1" width="21" height="12" rx="3.5" fill="none" stroke="#EEF1F7" stroke-opacity=".5" stroke-width="1.5"/><rect x="3" y="3" width="14" height="8" rx="2" fill="#EEF1F7"/><rect x="23" y="4.5" width="2" height="5" rx="1" fill="#EEF1F7" fill-opacity=".5"/></svg>`;
  const sig = `<svg width="18" height="14" viewBox="0 0 18 14">${[0, 1, 2, 3].map((i) => `<rect x="${i * 4.6}" y="${10 - i * 3}" width="3.2" height="${4 + i * 3}" rx="1" fill="#EEF1F7" ${i === 3 ? 'fill-opacity=".4"' : ''}/>`).join('')}</svg>`;
  return `<div class="status"><span>12:40</span><span class="sys">${sig}${I('wifi', 16, 2.4)}${bat}</span></div>`;
}
const TABS = [
  ['home', 'Home'], ['screen', 'Screen'], ['car', 'Car'], ['apps', 'Apps'], ['more', 'More'],
];
function tabBar(active, o = {}) {
  const icon = (k) => ({
    home: carGlyph(24, o.connected), screen: I('monitor', 22), car: I('circle-gauge', 22), apps: I('layout-grid', 22), more: I('ellipsis', 22),
  })[k];
  return `<nav class="tabbar">${TABS.map(([k, n]) => `<div class="tb ${k}-tab${k === active ? ' on' : ''}"><div class="ring">${icon(k)}${k === 'more' && o.badgeMore ? '<i class="badge"></i>' : ''}</div><span>${n}</span></div>`).join('')}</nav>`;
}
function topRoot(title, right = '') {
  return `<div class="top"><h1>${L(title)}</h1>${right}<div class="iconbtn">${I('search', 22)}</div></div>`;
}
function topSub(title, right) {
  return `<div class="top sub"><div class="iconbtn">${I('arrow-left', 22)}</div><h1>${L(title)}</h1>${right ?? `<div class="iconbtn">${I('search', 22)}</div>`}</div>`;
}

/** A full phone screen. */
function phone(o) {
  const tabs = o.tabs === false ? '' : tabBar(o.tab, o);
  return `<div class="phone t-${o.tab}">${statusBar()}<div class="content${o.tabs === false ? ' full' : ''}">${o.body}</div>${tabs}${o.overlay ?? ''}<div class="navhandle"></div></div>`;
}
function doc(title, inner, w = 412, extraCss = '') {
  return `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=${w}">
<title>${title}</title>
<style>
${CSS}
body { width: ${w}px; }
${extraCss}
</style></head><body>
${inner}
</body></html>
`;
}

// ---------- The car screen picture (ORBIT home, original drawing) ----------
let picId = 0;
function carPic(w, o = {}) {
  const id = 'p' + (picId++);
  const h = Math.round((w * 720) / 1280);
  const hot = o.hot ? (() => {
    const lab = (x, y, t) => { const tw = t.length * 19 + 36; return `<g><rect x="${x}" y="${y}" width="${tw}" height="52" rx="26" fill="#A78BFA"/><text x="${x + tw / 2}" y="${y + 35}" font-size="30" font-weight="700" text-anchor="middle" fill="#0A1015">${t}</text></g>`; };
    return `<g fill="none" stroke="#A78BFA" stroke-width="5" stroke-dasharray="14 12">
      <circle cx="160" cy="160" r="128"/><circle cx="170" cy="560" r="146"/><circle cx="1110" cy="560" r="146"/><circle cx="300" cy="420" r="62"/></g>
      ${lab(300, 70, 'Maps')}${lab(330, 590, 'Design')}${lab(372, 380, 'Speed')}${lab(830, 600, 'Sound')}`;
  })() : '';
  const dim = o.dim ? `<rect width="1280" height="720" fill="#0A1015" opacity=".55"/>` : '';
  const acc = o.accent ?? '#6EE7A8';
  return `<svg class="pic" width="${w}" height="${h}" viewBox="0 0 1280 720" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <linearGradient id="${id}a" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#4CC9F0"/><stop offset=".55" stop-color="#8F5BFF"/><stop offset="1" stop-color="#FF5A8A"/></linearGradient>
    <radialGradient id="${id}v" cx=".5" cy=".5" r=".75"><stop offset=".55" stop-color="#000" stop-opacity="0"/><stop offset="1" stop-color="#000" stop-opacity=".55"/></radialGradient>
  </defs>
  <rect width="1280" height="720" fill="${o.mapBg ?? '#0D161C'}"/>
  <path d="M820 -10 C 900 120, 1060 140, 1290 120 L1290 -10 Z" fill="#0C1B28"/>
  <path d="M-10 300 C 120 280, 260 340, 330 300 S 420 200, 380 120 L 300 -10 L -10 -10 Z" fill="#0F1D19" opacity=".9"/>
  <g fill="none" stroke-linecap="round">
    <path d="M-20 470 C 240 430, 420 500, 590 420 S 930 290, 1300 320" stroke="#1C2932" stroke-width="30"/>
    <path d="M660 -20 C 630 200, 700 400, 650 760" stroke="#1A262E" stroke-width="24"/>
    <path d="M-20 650 L 1300 600" stroke="#151F26" stroke-width="12"/>
    <path d="M300 -20 L 420 760" stroke="#151F26" stroke-width="12"/>
    <path d="M980 -20 L 900 760" stroke="#151F26" stroke-width="12"/>
    <path d="M-20 180 L 1300 230" stroke="#151F26" stroke-width="10"/>
    <path d="M520 -20 C 480 260, 560 300, 460 760" stroke="#131C22" stroke-width="8"/>
    <path d="M650 760 C 655 600, 640 470, 720 400 S 900 330, 1060 240" stroke="${acc}" stroke-width="36" opacity=".14"/>
    <path d="M650 760 C 655 600, 640 470, 720 400 S 900 330, 1060 240" stroke="${acc}" stroke-width="14"/>
  </g>
  <circle cx="654" cy="610" r="22" fill="#EEF1F7" stroke="${acc}" stroke-width="7"/>
  <rect width="1280" height="720" fill="url(#${id}v)"/>
  <g>
    <circle cx="160" cy="160" r="108" fill="#080C14" fill-opacity=".88" stroke="#fff" stroke-opacity=".14" stroke-width="3"/>
    <path d="M140 200 V150 a26 26 0 0 1 26 -26 h28" fill="none" stroke="#EEF1F7" stroke-width="15" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M190 100 l34 24 -34 24 z" fill="#EEF1F7" stroke="#EEF1F7" stroke-width="6" stroke-linejoin="round"/>
    <text x="160" y="240" font-size="30" font-weight="600" fill="#AAB3C8" text-anchor="middle">300 m</text>
  </g>
  <text x="1210" y="118" font-size="76" font-weight="600" fill="#EEF1F7" text-anchor="end" letter-spacing="-2">12:40</text>
  <g>
    <circle cx="170" cy="560" r="118" fill="#080C14" fill-opacity=".88" stroke="#fff" stroke-opacity=".14" stroke-width="3"/>
    <circle cx="170" cy="560" r="131" fill="none" stroke="#24323A" stroke-width="10"/>
    <circle cx="170" cy="560" r="131" fill="none" stroke="${acc}" stroke-width="10" stroke-linecap="round" stroke-dasharray="${2 * Math.PI * 131 * 0.62} 2000" transform="rotate(-90 170 560)"/>
    <text x="170" y="580" font-size="92" font-weight="700" fill="#EEF1F7" text-anchor="middle" letter-spacing="-3">48</text>
    <text x="170" y="625" font-size="26" font-weight="500" fill="#AAB3C8" text-anchor="middle">km/h</text>
    <circle cx="300" cy="420" r="44" fill="#fff" stroke="#D32F2F" stroke-width="10"/>
    <text x="300" y="434" font-size="38" font-weight="800" fill="#111" text-anchor="middle">50</text>
  </g>
  <g>
    <circle cx="1110" cy="560" r="131" fill="none" stroke="#24323A" stroke-width="10"/>
    <circle cx="1110" cy="560" r="131" fill="none" stroke="#A78BFA" stroke-width="10" stroke-linecap="round" stroke-dasharray="${2 * Math.PI * 131 * 0.35} 2000" transform="rotate(-90 1110 560)"/>
    <circle cx="1110" cy="560" r="118" fill="url(#${id}a)"/>
    <circle cx="1110" cy="560" r="40" fill="#080C14" fill-opacity=".35"/>
    <path d="M1098 540 l30 20 -30 20 z" fill="#fff" stroke="#fff" stroke-width="6" stroke-linejoin="round"/>
  </g>
  ${hot}${dim}
</svg>`;
}

/** DECK, the archived design: a rail and tiles (simple drawing). */
function deckPic(w) {
  const h = Math.round((w * 720) / 1280);
  const rail = [0, 1, 2, 3, 4].map((i) => `<rect x="40" y="${70 + i * 120}" width="90" height="90" rx="22" fill="${i === 1 ? '#FFC24A' : '#1E2A33'}"/>`).join('');
  return `<svg class="pic" width="${w}" height="${h}" viewBox="0 0 1280 720"><rect width="1280" height="720" fill="#0D141A"/>
  <rect x="0" y="0" width="170" height="720" fill="#111A21"/>${rail}
  <rect x="210" y="40" width="660" height="640" rx="36" fill="#15212A"/>
  <path d="M260 600 C 420 520, 520 420, 820 120" stroke="#6EE7A8" stroke-width="14" fill="none" stroke-linecap="round"/>
  <rect x="900" y="40" width="340" height="310" rx="36" fill="#211B33"/><rect x="940" y="80" width="120" height="120" rx="20" fill="#8F5BFF"/>
  <rect x="940" y="230" width="250" height="20" rx="10" fill="#3A3350"/><rect x="940" y="270" width="160" height="20" rx="10" fill="#2C2740"/>
  <rect x="900" y="370" width="340" height="310" rx="36" fill="#1A2430"/><text x="1070" y="560" font-size="96" font-weight="700" fill="#EEF1F7" text-anchor="middle">12:40</text>
  </svg>`;
}

module.exports = { I, L, H, carGlyph, mark, ring, sw, row, card, sec, note, tag, NEXT, ALSO, btn, seg, slider, appdot, statusBar, tabBar, topRoot, topSub, phone, doc, carPic, deckPic, CSS };
