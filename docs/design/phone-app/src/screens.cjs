// Every phone screen, as { file, title, html-body }. Sample data is invented.
const P = require('./parts.cjs');
const { I, L, H, carGlyph, mark, ring, row, card, sec, note, tag, NEXT, ALSO, btn, seg, slider, appdot, topRoot, topSub, phone, carPic, deckPic } = P;

const W = '#6EE7A8', V = '#A78BFA', A = '#FFC24A', K = '#FF8FB1', B = '#9DB4E8', C = '#5FD39A';
const S = [];
const add = (file, title, o) => S.push({ file, title, html: phone(o) });
const pad = (x, style = '') => `<div class="pad" style="${style}">${x}</div>`;
const gap = (h) => `<div style="height:${h}px"></div>`;
const homeTop = `<div class="top"><div class="wordmark">${mark(30)}MyPlay</div><div class="iconbtn">${I('search', 22)}</div></div>`;
const picCard = (o = {}) => card(`<div style="padding:12px 12px 6px">${carPic(354, o)}</div>${row({ label: 'Car screen', value: 'ORBIT', cls: '', style: 'min-height:52px' })}`);

// ============ 1. Home ============
add('01-home-connected', 'Home — connected', {
  tab: 'home', connected: true,
  body: homeTop + `<div class="hero">${ring(176, { w: 6, color: W, glow: true, inner: carGlyph(64, true), halos: [{ r: 98, o: 0.12, w: 1.5 }] })}
    <div class="t">Connected</div><div class="s">Sedan · USB</div></div>` + gap(14) +
    pad(card(row({ ic: 'info', icCls: 'plain', label: 'Details', value: '1080p · 60 fps' }))) + gap(12) +
    pad(picCard()) + gap(14) +
    pad(`<div class="btns">${btn('Preview', 'primary', 'monitor')}${btn('Disconnect', 'ghost')}</div>`),
});

add('02-home-not-connected', 'Home — not connected (Bluetooth off)', {
  tab: 'home', connected: false,
  body: homeTop + `<div class="hero">${ring(176, { w: 4, color: '#5A6676', dashed: '3 11', track: false, inner: carGlyph(60, false), innerColor: 'var(--text3)' })}
    <div class="t">Not connected</div><div class="s">${H('Bluetooth is off')}</div></div>` + gap(18) +
    pad(btn('Turn on Bluetooth', 'primary wide', 'bluetooth')) +
    `<div class="note" style="justify-content:center;padding-top:12px">${H('Or plug in a USB cable')}</div>` + gap(20) +
    pad(card(row({ ic: 'sparkles', label: 'Finish setup', help: 'Sound and notifications', value: '2 left' }))) + gap(12) +
    pad(picCard({ dim: true })),
});

add('03-home-update', 'Home — update available', {
  tab: 'home', connected: true, badgeMore: true,
  body: homeTop + pad(card(`<div style="padding:16px 16px 16px;display:flex;gap:14px;align-items:center">
      ${ring(56, { w: 4, color: W, inner: I('download', 22) })}
      <div class="lbl"><div class="l" style="font-size:17px;font-weight:600">${L('Version 2.4 is ready')}</div><div class="h">${H('New looks, faster start')}</div></div>
      <div class="iconbtn" style="width:40px;height:40px">${I('x', 18)}</div></div>
      <div style="padding:0 16px 16px" class="btns">${btn('Install', 'primary small', 'download')}${btn("What's new", 'ghost small')}</div>`, 'acc')) + gap(12) +
    pad(card(`<div class="row" style="min-height:84px">${ring(52, { w: 3, color: W, inner: carGlyph(26, true) })}<div class="lbl"><div class="l">Connected</div><div class="h">Sedan · USB</div></div><div class="val">Details</div><span class="chev">${I('chevron-right', 20)}</span></div>`)) + gap(12) +
    pad(picCard()) + gap(14) +
    pad(`<div class="btns">${btn('Preview', 'ghost', 'monitor')}${btn('Disconnect', 'ghost')}</div>`),
});

// ============ 2. Screen ============
add('04-screen', 'Screen — the tab root', {
  tab: 'screen', connected: true,
  body: topRoot('Screen') + pad(`<div class="picwrap">${carPic(380, { hot: true })}</div>`) +
    note('Tap a part of the picture to change it', 'hand') + gap(14) +
    pad(card([
      row({ ic: 'layers', label: 'Design', value: 'ORBIT' }),
      row({ ic: 'sparkles', label: 'Looks', value: 'Midnight' }),
      row({ ic: 'palette', label: 'Colours', value: 'Slate + own', changed: true }),
      row({ ic: 'image', label: 'Wallpaper', value: 'Photo', changed: true }),
      row({ ic: 'map', label: 'Map colours', value: 'Night blue' }),
      row({ ic: 'type', label: 'Size', value: 'Larger · Auto', changed: true }),
    ].join(''))),
});

const look = (name, cols, o = {}) => `<div class="card" style="padding:14px;${o.sel ? 'border:2px solid var(--acc);' : ''}${o.dash ? 'border:1.5px dashed #3A4652;background:transparent;' : ''}height:132px;display:flex;flex-direction:column;justify-content:space-between">
  ${o.dash ? `<div style="flex:1;display:grid;place-items:center;color:var(--acc)">${ring(48, { w: 2, color: 'var(--acc)', track: false, inner: I('plus', 22) })}</div>`
    : `<div style="position:relative;height:60px;border-radius:14px;background:${cols[0]};overflow:hidden">
    <div style="position:absolute;left:12px;top:12px;width:36px;height:36px;border-radius:50%;border:4px solid ${cols[1]}"></div>
    <div style="position:absolute;right:12px;top:10px;width:40px;height:40px;border-radius:50%;background:${cols[2]}"></div>
    <div style="position:absolute;left:58px;top:24px;right:62px;height:6px;border-radius:3px;background:${cols[1]};opacity:.6"></div></div>`}
  <div style="display:flex;align-items:center;justify-content:space-between"><span style="font-size:15px;font-weight:560">${L(name)}</span>${o.sel ? tag('In use', 'acc') : ''}</div></div>`;
const grid2 = (items) => `<div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">${items.join('')}</div>`;
const looksBody = topSub('Looks') + sec('Built in', '', true) + pad(grid2([
  look('Midnight', ['#0D161C', '#6EE7A8', 'linear-gradient(135deg,#4CC9F0,#8F5BFF)'], { sel: true }),
  look('Ember', ['#1A100C', '#FFC24A', 'linear-gradient(135deg,#FF8A4C,#FF5A8A)']),
  look('Glacier', ['#0C1620', '#9DB4E8', 'linear-gradient(135deg,#BDE3FF,#4CC9F0)']),
  look('Moss', ['#0E1612', '#5FD39A', 'linear-gradient(135deg,#B5E48C,#2D9A6A)']),
])) + sec('Yours') + pad(grid2([
  look('Night drive', ['#0A0F1A', '#A78BFA', 'linear-gradient(135deg,#8F5BFF,#3A2C8A)']),
  look('Save this look', [], { dash: true }),
])) + gap(18) + pad(`<div class="btns">${btn('Open a file', 'ghost small', 'download')}${btn('Share', 'ghost small', 'share-2')}</div>`);
add('05-screen-looks', 'Screen › Looks', { tab: 'screen', connected: true, body: looksBody });

add('06-screen-looks-apply', 'Screen › Looks — apply sheet (changes the design)', {
  tab: 'screen', connected: true, body: looksBody,
  overlay: `<div class="scrim"></div><div class="sheet"><div class="grab"></div>
    <h2>${L('Use Ember?')}</h2><div class="sub">${H('This look changes:')}</div>
    <div style="display:flex;flex-wrap:wrap;gap:8px;margin:14px 0 18px">${['Colours', 'Wallpaper', 'Map colours', 'Older-layout settings'].map((t) => tag(t, 'mute')).join('')}</div>
    ${card(row({ ic: 'triangle-alert', icCls: 'warn', label: 'This look uses DECK', help: 'The car screen switches from ORBIT', nochev: true }))}
    <div class="btns" style="margin-top:20px">${btn('Cancel', 'ghost')}${btn('Use Ember', 'primary')}</div></div>`,
});

const preview = (label = 'Live') => pad(`<div style="position:relative;display:flex;justify-content:center">${carPic(300)}<span class="tag acc" style="position:absolute;bottom:10px;left:50%;transform:translateX(-50%)">${label}</span></div>`);
const disc = (bg, name, sel) => `<div style="display:flex;flex-direction:column;align-items:center"><div class="disc${sel ? ' sel' : ''}" style="background:${bg}"></div><div class="disc-l">${L(name)}</div></div>`;
add('07-screen-colours', 'Screen › Colours', {
  tab: 'screen', connected: true,
  body: topSub('Colours') + preview() + sec('App screens', tag('Colours of apps on the car', 'mute')) +
    pad(card(`<div style="padding:14px 16px 0;font-size:14px;color:var(--text2)">Theme</div><div class="discs" style="justify-content:space-between">
      ${disc('linear-gradient(135deg,#1B2633 50%,#6EE7A8 50%)', 'Slate', true)}${disc('linear-gradient(135deg,#1E1E22 50%,#C9CDD6 50%)', 'Graphite')}${disc('linear-gradient(135deg,#0F2233 50%,#4CC9F0 50%)', 'Ocean')}${disc('linear-gradient(135deg,#23182E 50%,#A78BFA 50%)', 'Plum')}${disc('linear-gradient(135deg,#14241B 50%,#5FD39A 50%)', 'Forest')}</div>
      <div style="padding:4px 16px 0;font-size:14px;color:var(--text2)">Accent</div><div class="discs" style="gap:12px">
      ${['#6EE7A8', '#4CC9F0', '#A78BFA', '#FF8FB1', '#FFC24A', '#FF8A4C', '#C9CDD6'].map((c, i) => `<div class="disc${i === 0 ? ' sel' : ''}" style="background:${c};width:36px;height:36px"></div>`).join('')}</div>` +
      row({ ic: 'palette', label: 'Your own colours', value: 'On', changed: true }))) +
    sec('Day and night', ALSO) + pad(seg(["Car's", 'Dark', 'Light'], 0)) + gap(10) +
    `<div style="display:flex;justify-content:center">${btn('Reset this page', 'text small', 'rotate-ccw')}</div>`,
});

add('08-screen-size', 'Screen › Size', {
  tab: 'screen', connected: true,
  body: topSub('Size') + preview() + gap(16) + pad(card(
    slider('Text size', 'Larger', 66.6, { ticks: 4, ends: ['Small', 'Largest'], changed: true }) +
    `<div style="height:1px;background:var(--edge);margin:0 16px"></div>` +
    slider('Control size', 'Normal', 33.3, { ticks: 4, ends: ['Small', 'Large'] }) +
    `<div style="height:1px;background:var(--edge);margin:0 16px"></div>` +
    slider('How much fits', 'Auto', 50, { ticks: 5, ends: ['Bigger', 'More'], tag: NEXT }))) +
    note('Applies to ORBIT and every app on the car', 'info') + gap(10) +
    `<div style="display:flex;justify-content:center">${btn('Reset this page', 'text small', 'rotate-ccw')}</div>`,
});

add('09-screen-design', 'Screen › Design', {
  tab: 'screen', connected: true,
  body: topSub('Design') + pad(card(`<div style="padding:12px">${carPic(354)}</div>
      <div class="row" style="min-height:60px">${ring(28, { w: 3, color: 'var(--acc)', track: false, fill: 'var(--acc)', inner: '' })}<div class="lbl"><div class="l">ORBIT</div><div class="h">${H('Rounds and rings over the map')}</div></div>${tag('In use', 'acc')}</div>`, '', 'border:2px solid var(--acc)')) + gap(12) +
    pad(card(`<div class="row" style="min-height:64px">${ring(28, { w: 2, color: '#4A5666', track: false })}<div class="lbl"><div class="l" style="color:var(--text2)">RACE</div><div class="h">${H('A sportier design')}</div></div>${tag('Coming later', 'mute')}</div>`)) + gap(12) +
    pad(card(row({ ic: 'timer', label: 'Panels close after', value: '5 s', under: ALSO }))) +
    note('DECK and older layouts are in More › Archive', 'archive'),
});

// ============ 3. Car ============
add('10-car', 'Car — the tab root', {
  tab: 'car', connected: true,
  body: topRoot('Car') + pad(card([
    row({ ic: 'circle-gauge', label: 'Buttons', value: '3 changed', changed: true }),
    row({ ic: 'map', label: 'Maps', value: 'Last used' }),
    row({ ic: 'gauge', label: 'Speed', value: 'Auto · Northvale' }),
    row({ ic: 'volume-2', label: 'Sound', value: 'Best' }),
    row({ ic: 'message-circle', label: 'Messages and calls', value: 'Banners on' }),
    row({ ic: 'mic', label: 'Voice assistant', value: "Phone's default" }),
  ].join(''))) + gap(12) +
    pad(card(row({ ic: 'users', label: 'Passenger mode', help: 'For a passenger, not the driver', sw: false }))),
});

function controls(sel) {
  const on = (k) => (k === sel ? 'var(--acc)' : '#3A4652');
  const fillOn = (k) => (k === sel ? 'color-mix(in srgb, var(--acc) 16%, transparent)' : '#151E26');
  const t = (x, y, s) => `<text x="${x}" y="${y}" font-size="12" font-weight="600" fill="#AAB3C8" text-anchor="middle" font-family="Inter">${s}</text>`;
  return `<svg width="380" height="176" viewBox="0 0 380 176">
    <g transform="translate(70 80)"><circle r="54" fill="${fillOn('pad')}" stroke="${on('pad')}" stroke-width="2"/>
      ${[0, 90, 180, 270].map((a) => `<path transform="rotate(${a})" d="M-9 -30 L0 -42 L9 -30 Z" fill="#AAB3C8"/>`).join('')}
      <circle r="18" fill="#1A242D" stroke="#3A4652" stroke-width="2"/></g>${t(70, 166, 'Pad')}
    <g transform="translate(190 80)">
      <circle r="66" fill="none" stroke="${on('knob')}" stroke-width="2" opacity=".35"/>
      <circle r="58" fill="${fillOn('knob')}" stroke="${on('knob')}" stroke-width="3"/>
      ${Array.from({ length: 24 }, (_, i) => `<line transform="rotate(${i * 15})" x1="0" y1="-50" x2="0" y2="-44" stroke="#6A7686" stroke-width="2"/>`).join('')}
      <circle r="26" fill="#1A242D" stroke="${on('knob')}" stroke-width="2"/></g>${t(190, 166, 'Knob')}
    <g transform="translate(310 52)"><circle r="24" fill="${fillOn('menu')}" stroke="${on('menu')}" stroke-width="2"/><path d="M-8 -6 H8 M-8 0 H8 M-8 6 H8" stroke="#AAB3C8" stroke-width="2.2" stroke-linecap="round"/></g>${t(310, 92, 'Menu')}
    <g transform="translate(310 122)"><circle r="24" fill="${fillOn('back')}" stroke="${on('back')}" stroke-width="2"/><path d="M5 -6 L-5 0 L5 6" fill="none" stroke="#AAB3C8" stroke-width="2.4" stroke-linecap="round"/></g>${t(310, 162, 'Back')}
  </svg>`;
}
const r56 = 'min-height:56px';
add('11-car-buttons', 'Car › Buttons', {
  tab: 'car', connected: true,
  body: topSub('Buttons') + pad(card(`<div style="padding:12px 0 6px">${controls('knob')}</div>`)) +
    note('Tap a control to change what it does', 'hand') +
    sec('Knob push') + pad(card([
      row({ label: 'Press', value: 'Select', style: r56 }),
      row({ label: 'Hold', value: 'Voice assistant', changed: true, style: r56 }),
      row({ label: 'Double press', value: 'Home', changed: true, style: r56 }),
      row({ label: 'Triple press', value: 'Off', style: r56, tag: tag('New', 'acc') }),
    ].join(''))) + gap(12) + pad(card([
      row({ ic: 'plus', label: 'Pad shortcuts', value: '4 set', changed: true }),
      row({ ic: 'hand', label: 'Teach a car button', value: '2 taught' }),
      row({ ic: 'timer', label: 'Timing', value: 'Normal' }),
    ].join(''))),
});

function padDiagram() {
  const node = (x, y, icon, label, sel, col) => `<div style="position:absolute;left:${x - 60}px;top:${y - 32}px;width:120px;display:flex;flex-direction:column;align-items:center">
    ${ring(64, { w: sel ? 3 : 2, color: sel ? 'var(--acc)' : '#3A4652', track: false, fill: sel ? 'color-mix(in srgb, var(--acc) 14%, #121A21)' : '#151E26', inner: `<span style="color:${col}">${I(icon, 24)}</span>` })}
    <div style="font-size:13px;font-weight:600;margin-top:5px;white-space:nowrap;color:${sel ? 'var(--text)' : 'var(--text2)'}">${label}</div></div>`;
  return `<div style="position:relative;height:330px;margin:0 auto;width:340px">
    <div style="position:absolute;left:35px;top:25px;width:270px;height:270px;border-radius:50%;border:1.5px solid #26313B;background:radial-gradient(circle,#111A21,#0E151B)"></div>
    ${node(170, 56, 'map', 'Up · Maps', true, W)}${node(170, 160, 'music', 'Centre · Music', false, V)}
    ${node(170, 264, 'phone', 'Down · Calls', false, C)}${node(272, 160, 'layout-grid', 'Side · Apps', false, K)}</div>`;
}
add('12-car-pad-shortcuts', 'Car › Buttons › Pad shortcuts (with picker sheet)', {
  tab: 'car', connected: true,
  body: topSub('Pad shortcuts') + note('On the home screen, the pad opens:', 'info') + gap(10) + padDiagram(),
  overlay: `<div class="scrim" style="background:rgba(3,6,9,.4)"></div><div class="sheet" style="padding-bottom:36px"><div class="grab"></div>
    <h2>${L('Pad up opens')}</h2>
    <div class="card" style="margin-top:14px">${[
      ['map', 'Maps', W, true], ['music', 'Music', V], ['phone', 'Calls', C], ['layout-grid', 'Apps', K], ['x', 'Nothing', '#8A95A8'],
    ].map(([ic, n, col, on]) => `<div class="row" style="min-height:56px">${appdot(ic, col, 36)}<div class="lbl"><div class="l">${n}</div></div>${on ? `<span style="color:var(--acc)">${I('check', 22, 2.5)}</span>` : ''}</div>`).join('')}</div></div>`,
});

add('13-car-teach', 'Car › Buttons › Teach a car button (waiting)', {
  tab: 'car', connected: true,
  body: topSub('Teach a car button') + gap(48) + `<div class="hero">${ring(190, { w: 4, color: 'var(--acc)', pct: 0.72, glow: true, halos: [{ r: 108, o: 0.18 }, { r: 122, o: 0.08 }], inner: I('hand', 54, 1.8) })}
    <div class="t">${L('Press a car button')}</div><div class="s">${H('Waiting · 8 s')}</div></div>` + gap(16) +
    `<div style="display:flex;justify-content:center">${btn('Cancel', 'ghost small')}</div>` +
    sec('Taught') + pad(card([
      row({ ic: 'circle-dot', icCls: 'plain', label: 'Star button', value: 'Maps' }),
      row({ ic: 'circle-dot', icCls: 'plain', label: 'Mode button', value: 'Music' }),
    ].join(''))),
});

add('14-car-speed', 'Car › Speed — region downloading', {
  tab: 'car', connected: true,
  body: topSub('Speed') + pad(card([
    row({ ic: 'gauge', label: 'Speed source', help: 'Car first, then phone GPS', value: 'Auto', under: ALSO }),
    row({ ic: 'map-pin', label: 'Region', value: 'Northvale' }),
  ].join(''))) + gap(12) +
    pad(card(`<div class="row" style="min-height:84px">${ring(52, { w: 4, pct: 0.64, color: 'var(--acc)', inner: `<span style="font-size:13px;font-weight:700;color:var(--text)">64%</span>` })}
      <div class="lbl"><div class="l">${L('Downloading limits')}</div><div class="h">${H('18 of 28 MB · Northvale')}</div></div>${btn('Cancel', 'text small')}</div>`, 'acc')) + gap(12) +
    pad(card(slider('Warn when over by', '+5 km/h', 25, { ticks: 5, ends: ['0', '+20'], changed: true }))) +
    note('Works offline. Looks for a newer file monthly.', 'lock') + gap(12) +
    pad(card(row({ ic: 'route', icCls: 'plain', label: 'Limit from Waze', help: 'Needs the MyPlay module', sw: false, dim: true }))),
});

add('15-car-maps', 'Car › Maps', {
  tab: 'car', connected: true,
  body: topSub('Maps') + pad(card([
    row({ ic: 'house', label: 'Map on home', value: 'Last used', under: ALSO }),
    row({ ic: 'history', label: 'Go home when idle', sw: true }),
    row({ ic: 'sun-moon', label: "Follow the car's lights", sw: true }),
    row({ ic: 'locate-fixed', label: "Use the car's GPS", help: 'Needs mock location on the phone', sw: false }),
  ].join(''))) + gap(12) +
    pad(card(row({ ic: 'palette', label: 'Map colours', value: 'Night blue', tag: tag('Screen', 'mute'), ext: true }))) +
    sec('Maps apps') + pad(card([
      row({ lead: appdot('map', W), label: 'Maps', value: 'Home map' }),
      row({ lead: appdot('route', '#4CC9F0'), label: 'TrailNav' }),
    ].join(''))),
});

// ============ 4. Apps ============
const app = (icon, col, name, on, o = {}) => row({ lead: appdot(icon, col), label: name, help: o.help, sw: on, right: o.gear ? `<div class="iconbtn" style="width:44px;height:44px">${I('settings', 20)}</div>` : undefined, style: o.help ? '' : r56 });
add('16-apps', 'Apps — the car screen’s app list', {
  tab: 'apps', connected: true,
  body: `<div class="top"><h1>Apps</h1><div class="iconbtn">${I('list-ordered', 22)}</div><div class="iconbtn">${I('search', 22)}</div></div>` +
    pad(card(row({ ic: 'smartphone', label: 'Phone apps', help: 'Your phone’s apps on the car', value: '3 apps' }), 'acc')) +
    sec('On the car screen', tag('9 of 10', 'mute')) + pad(card([
      app('map', W, 'Maps', true), app('route', '#4CC9F0', 'TrailNav', true), app('music', V, 'Music', true),
      app('radio', '#C9A8FF', 'Podcasts', false), app('message-circle', C, 'Messages', true),
      app('monitor', K, 'Phone screen', true, { gear: true }), app('globe', K, 'Browser', true, { gear: true }),
      app('tv', K, 'YouTube', true, { gear: true, help: 'Plays only when parked' }), app('activity', A, 'Car data', false),
    ].join(''))),
});

add('17-apps-reorder', 'Apps — reorder mode', {
  tab: 'apps', connected: true,
  body: `<div class="top sub"><div class="iconbtn">${I('x', 22)}</div><h1>Reorder</h1>${btn('Done', 'primary small')}<div style="width:8px"></div></div>` +
    note('Drag, or use the arrows', 'grip-vertical') + gap(10) + pad(card([
      ['map', W, 'Maps'], ['route', '#4CC9F0', 'TrailNav'], ['message-circle', C, 'Messages'], ['monitor', K, 'Phone screen'], ['globe', K, 'Browser'], ['tv', K, 'YouTube'],
    ].map(([ic, col, n], i) => `${i === 2 ? `<div class="row" style="min-height:60px;margin:6px 8px;border-radius:18px;background:#1F2A34;border:1.5px solid var(--acc);box-shadow:0 12px 28px rgba(0,0,0,.5);transform:translateX(6px)"><span style="color:var(--acc)">${I('grip-vertical', 20)}</span>${appdot('music', V)}<div class="lbl"><div class="l">Music</div></div><div class="iconbtn" style="width:44px;height:44px">${I('arrow-up', 20)}</div><div class="iconbtn" style="width:44px;height:44px">${I('arrow-down', 20)}</div></div>` : ''}
      <div class="row" style="min-height:60px"><span style="color:var(--text3)">${I('grip-vertical', 20)}</span>${appdot(ic, col)}<div class="lbl"><div class="l">${n}</div></div><div class="iconbtn" style="width:44px;height:44px;${i === 0 ? 'opacity:.3' : ''}">${I('arrow-up', 20)}</div><div class="iconbtn" style="width:44px;height:44px">${I('arrow-down', 20)}</div></div>`).join(''))),
});

add('18-apps-phone-apps', 'Apps › Phone apps', {
  tab: 'apps', connected: true,
  body: topSub('Phone apps') + pad(card(row({ ic: 'smartphone', label: 'Show phone apps', help: 'Use them only when parked', sw: true }))) +
    sec('Allowed', tag('3', 'mute')) + pad(card([
      row({ lead: appdot('file-text', '#C9CDD6'), label: 'Notes', help: 'Knob turns: scroll', value: '' }),
      row({ lead: appdot('radio', '#FF8A4C'), label: 'Radio', help: 'Knob turns: volume', value: '' }),
      row({ lead: appdot('check', C), label: 'Tasks', help: 'Knob turns: scroll', value: '' }),
      row({ ic: 'plus', label: 'Add apps', nochev: true, cls: 'link' }),
    ].join(''))) + sec('While they are open') + pad(card(row({ ic: 'sun', label: 'Keep the phone awake', sw: true }))),
});

// ============ 5. More ============
add('19-more', 'More — the tab root', {
  tab: 'more', connected: true, badgeMore: true,
  body: topRoot('More') + pad(card([
    row({ ic: 'download', label: 'Updates', value: tag('2.4 ready') }),
    row({ ic: 'car-front', label: 'Cars', value: '2 cars' }),
    row({ ic: 'wifi', label: 'Connection', value: 'Wireless on' }),
    row({ ic: 'history', label: 'Backup', value: 'Saved 3 Nov' }),
    row({ ic: 'activity', label: 'Diagnostics', value: 'Car check: not run' }),
  ].join(''))) + gap(12) + pad(card([
    row({ ic: 'sliders-horizontal', label: 'Advanced', value: 'Normal' }),
    row({ ic: 'archive', label: 'Archive', value: 'Not used' }),
    row({ ic: 'info', label: 'About', value: '2.3.1' }),
  ].join(''))),
});

add('20-more-updates', 'More › Updates', {
  tab: 'more', connected: true, badgeMore: true,
  body: topSub('Updates') + `<div class="hero">${ring(150, { w: 5, color: 'var(--acc)', glow: true, inner: I('download', 46, 1.8) })}
    <div class="t">${L('Version 2.4 is ready')}</div><div class="s">${H('You have 2.3.1')}</div></div>` + gap(18) +
    pad(btn('Install 2.4', 'primary wide', 'download')) + gap(18) + pad(card([
      row({ ic: 'sparkles', label: "What's new in 2.4" }),
      row({ ic: 'refresh-cw', label: 'Check now', value: 'Checked 09:12', nochev: true }),
      row({ ic: 'bell', label: 'Check automatically', help: 'Once a day, from GitHub', sw: true }),
    ].join(''))),
});

function padCheck() {
  const node = (x, y, label, st) => {
    const done = st === 'done', wait = st === 'wait';
    const inner = done ? `<span style="color:var(--on)">${I('check', 26, 3)}</span>` : `<span style="color:${wait ? 'var(--acc)' : 'var(--text3)'};font-size:13px;font-weight:700">${label}</span>`;
    return `<div style="position:absolute;left:${x - 36}px;top:${y - 36}px">${ring(72, { w: 3, color: done ? 'var(--acc)' : wait ? 'var(--acc)' : '#3A4652', track: false, fill: done ? 'var(--acc)' : '#121A21', dashed: wait ? '6 8' : undefined, halos: wait ? [{ r: 44, o: 0.25 }, { r: 52, o: 0.1 }] : [], inner })}</div>`;
  };
  return `<div style="position:relative;width:260px;height:260px;margin:0 auto">
    <div style="position:absolute;inset:30px;border-radius:50%;border:1.5px solid #26313B"></div>
    ${node(130, 40, 'Up', 'done')}${node(220, 130, 'Right', 'done')}${node(130, 220, 'Down', 'wait')}${node(40, 130, 'Left', 'todo')}
    <div style="position:absolute;inset:0;display:grid;place-items:center;font-size:15px;color:var(--text2);font-weight:600">2 of 4</div></div>`;
}
add('21-more-car-check', 'More › Diagnostics › Car check — the 4-button step', {
  tab: 'more', connected: true,
  body: topSub('Car check', `<div class="val" style="padding-right:16px">5 of 11</div>`) +
    pad(`<div style="display:flex;gap:4px">${Array.from({ length: 11 }, (_, i) => `<i style="flex:1;height:4px;border-radius:2px;background:${i < 4 ? 'var(--acc)' : i === 4 ? 'color-mix(in srgb, var(--acc) 50%, #2A3540)' : '#2A3540'}"></i>`).join('')}</div>`) +
    `<div class="hero" style="padding-top:22px"><div class="t" style="margin-top:0">${L('Press each pad button')}</div><div class="s">${H('Up, right, down and left, in any order')}</div></div>` + gap(18) +
    padCheck() + gap(30) + pad(card(row({ ic: 'scan-line', icCls: 'plain', label: 'Heard from the car', value: 'Up · Right', nochev: true }))) + gap(16) +
    pad(`<div class="btns">${btn('Nothing happened', 'ghost')}${btn('Next', 'primary')}</div>`),
});

add('22-more-car-check-share', 'More › Diagnostics › Car check — share sheet', {
  tab: 'more', connected: true,
  body: topSub('Car check', '<div style="width:48px"></div>') + `<div class="hero" style="padding-top:12px">${ring(150, { w: 5, color: 'var(--acc)', inner: I('check', 52, 2.4) })}
    <div class="t">Done</div><div class="s">11 steps · 2 to look at</div></div>` + gap(16) + pad(card([
      row({ ic: 'circle-gauge', label: 'Knob', value: tag('All good', 'ok'), nochev: true }),
      row({ ic: 'plus', label: 'Pad', value: tag('Left: nothing', 'warn'), nochev: true }),
    ].join(''))),
  overlay: `<div class="scrim"></div><div class="sheet"><div class="grab"></div>
    <h2>${L('Share the log')}</h2><div class="sub">${H('Phone numbers and names are removed.')}</div>
    <div class="card" style="margin-top:18px">${row({ ic: 'triangle-alert', label: 'Include alert texts', help: 'Map alerts can name streets', sw: false })}${row({ ic: 'file-text', label: 'See what is shared' })}</div>
    <div class="btns" style="margin-top:20px">${btn('Cancel', 'ghost')}${btn('Share', 'primary', 'share-2')}</div></div>`,
});

add('23-more-advanced', 'More › Advanced', {
  tab: 'more', connected: true,
  body: topSub('Advanced') + sec('Video', NEXT, true) + pad(card([
    row({ label: 'Highest resolution', sw: true, style: r56 }),
    row({ label: 'Prefer 60 fps', sw: true, style: r56 }),
    row({ label: 'Force H.264', sw: false, style: r56 }),
    row({ label: 'Steady bitrate', sw: false, style: r56 }),
  ].join(''))) + sec('Security') + pad(card(row({ ic: 'shield-check', label: 'Certificates', value: 'Valid to Mar 2027' }))) +
    sec('Car fixes') + pad(card([
      row({ ic: 'bluetooth', label: "Phone's Bluetooth address", value: 'Auto' }),
      row({ ic: 'wrench', label: 'Per-car fixes', value: 'None' }),
    ].join(''))) + gap(12) + pad(card(row({ ic: 'code', icCls: 'plain', label: 'Developer' }))),
});

add('24-more-developer', 'More › Advanced › Developer', {
  tab: 'more', connected: true,
  body: topSub('Developer') + pad(card(row({ ic: 'terminal', label: 'Head unit server', help: 'For testing with a computer', sw: true }) +
    [['Desktop head unit', true], ['Open head units', false], ['Headunit apps', false], ['Browser bridge', false]]
      .map(([n, on]) => row({ label: n, sw: on, style: 'min-height:52px;padding-left:70px' })).join(''))) + gap(12) +
    pad(card([
      row({ ic: 'layers', label: 'Show map areas', sw: false }),
      row({ ic: 'file-text', label: 'Log maps text', sw: false }),
      row({ ic: 'square-stack', label: 'Phone parts sample' }),
      row({ ic: 'scan-line', label: 'Detailed protocol log', help: 'Lives in Connection log', ext: true }),
    ].join(''))),
});

add('25-more-archive', 'More › Archive', {
  tab: 'more', connected: true,
  body: topSub('Archive') + pad(`<div class="card" style="padding:14px 16px;display:flex;gap:12px;align-items:center;background:transparent;border-style:dashed">${I('archive', 20)}<span style="font-size:14px;color:var(--text2)">${H('Kept as it was. Not updated any more.')}</span></div>`) + gap(12) +
    pad(card(`<div style="padding:12px 12px 4px">${deckPic(354)}</div>` + row({ label: 'Use DECK instead of ORBIT', help: 'A rail of apps and tiles', sw: false }))) + gap(12) +
    pad(card(row({ ic: 'layout-grid', label: 'Older layouts', help: 'Taskbar, dock, grid, map behind' }))),
});

add('26-more-about', 'More › About', {
  tab: 'more', connected: true,
  body: topSub('About', '<div style="width:48px"></div>') + `<div class="hero" style="padding:10px 0 20px">${mark(96)}<div class="t">MyPlay</div><div class="s">Version 2.3.1</div></div>` +
    pad(card([
      row({ ic: 'sparkles', label: "What's new" }),
      row({ ic: 'globe', label: 'Project page', ext: true }),
      row({ ic: 'circle-alert', label: 'Report a problem', ext: true }),
      row({ ic: 'heart', label: 'Thanks' }),
      row({ ic: 'file-text', label: 'Open-source licences' }),
      row({ ic: 'map', label: 'Map data', value: 'OpenStreetMap' }),
    ].join(''))),
});

// ============ 6. States ============
add('27-state-setup', 'State — first run (Setup, step 1 of 4)', {
  tab: 'home', tabs: false,
  body: `<div style="padding:20px 24px 0;display:flex;gap:8px;align-items:center">${[0, 1, 2, 3].map((i) => `<i style="width:${i === 0 ? 28 : 10}px;height:10px;border-radius:5px;background:${i === 0 ? 'var(--acc)' : '#2A3540'}"></i>`).join('')}<span style="margin-left:auto;font-size:14px;color:var(--text2)">1 of 4</span></div>
    <div class="hero" style="padding-top:76px">${ring(160, { w: 4, color: 'var(--acc)', glow: true, halos: [{ r: 96, o: 0.14 }, { r: 116, o: 0.06 }], inner: mark(72) })}
    <div class="t" style="font-size:28px">${L('Find your car')}</div><div class="s">${H('MyPlay needs three things to connect')}</div></div>` + gap(24) +
    pad(card([
      row({ ic: 'bluetooth', label: 'Nearby devices', help: 'To find the car', right: tag('Allowed', 'ok'), nochev: true }),
      row({ ic: 'map-pin', label: 'Location', help: "To join the car's Wi-Fi", nochev: true }),
      row({ ic: 'bell', label: 'Notifications', help: 'Shows when you are connected', nochev: true }),
    ].join(''))) +
    `<div style="position:absolute;left:16px;right:16px;bottom:24px">${btn('Allow', 'primary wide')}<div style="display:flex;justify-content:center;margin-top:8px">${btn('Skip for now', 'text')}</div></div>`,
});

add('28-state-permission', 'State — a permission asked in context (Car › Messages and calls)', {
  tab: 'car', connected: true,
  body: topSub('Messages and calls') + pad(card(`<div class="row" style="min-height:84px;align-items:flex-start;padding-top:16px">
      <div class="ic">${I('contact', 20)}</div><div class="lbl"><div class="l">${L('Allow contacts and calls')}</div><div class="h">${H('So names show on the car screen')}</div>
      <div style="margin-top:12px;display:flex;gap:8px">${btn('Allow', 'primary small')}${btn('Not now', 'text small')}</div></div></div>`, 'acc')) + gap(12) +
    pad(card([
      row({ ic: 'message-circle', label: 'Banners', sw: true, dim: true, tag: ALSO }),
      row({ ic: 'users', label: 'Group chats', sw: false, dim: true }),
      row({ ic: 'volume-2', label: 'Read aloud', sw: true, dim: true }),
      row({ ic: 'moon', label: 'Do not disturb', sw: false, dim: true }),
      row({ ic: 'file-text', label: 'Auto-reply', value: 'Off', dim: true }),
      row({ ic: 'phone', label: 'Calls on phone speaker', help: "If the car's mic doesn't work", sw: false, under: NEXT }),
    ].join(''))),
});

add('29-state-error', 'State — an error (connection failed)', {
  tab: 'home', connected: false,
  body: homeTop + `<div class="hero">${ring(176, { w: 5, color: 'var(--warn)', pct: 0.3, inner: `<span style="color:var(--warn)">${carGlyph(60, false)}</span>` })}
    <div class="t">${L("Couldn't connect")}</div><div class="s">${H('The car stopped the video')}</div></div>` + gap(18) +
    pad(btn('Try again', 'primary wide', 'refresh-cw')) + gap(18) +
    pad(card([
      row({ ic: 'circle-alert', icCls: 'warn', label: 'What happened', value: '12:38' }),
      row({ ic: 'activity', icCls: 'plain', label: 'Run a car check' }),
    ].join(''))) + gap(12) + pad(picCard({ dim: true })),
});

add('30-state-empty', 'State — an empty list (More › Cars)', {
  tab: 'more', connected: false,
  body: topSub('Cars', '<div style="width:48px"></div>') + `<div class="hero" style="padding-top:120px">${ring(150, { w: 2, color: '#3A4652', track: false, dashed: '2 8', halos: [{ r: 95, o: 0.25 }, { r: 120, o: 0.12 }], inner: carGlyph(56, false), innerColor: 'var(--text3)' })}
    <div class="t">${L('No cars yet')}</div><div class="s" style="text-align:center;max-width:280px">${H('Cars appear here after the first connection.')}</div></div>`,
});

module.exports = S;
