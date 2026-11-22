// Every phone screen, in order. Option lists, value types and defaults come from the code
// (claude/myplay 0.1.47); cars, regions, apps and dates are invented sample data.
const P = require('./parts.cjs');
const { I, L, H, carGlyph, mark, ring, row, card, sec, note, tag, NEXT, ALSO, btn, seg, slider, appdot, topRoot, topSub, phone, carPic, appPic, deckPic } = P;

const W = '#6EE7A8', V = '#A78BFA', A = '#FFC24A', K = '#FF8FB1', B = '#9DB4E8', C = '#5FD39A';
const S = [];
const add = (slug, title, desc, o) => S.push({ slug, title, desc, tall: !!o.tall, html: phone(o) });
const pad = (x, style = '') => `<div class="pad" style="${style}">${x}</div>`;
const gap = (h) => `<div style="height:${h}px"></div>`;
const centre = (x) => `<div style="display:flex;justify-content:center">${x}</div>`;
const cards = (...rows) => pad(card(rows.join('')));
const hero = (r, t, s) => `<div class="hero">${r}<div class="t">${L(t)}</div>${s ? `<div class="s" style="text-align:center;max-width:320px">${H(s)}</div>` : ''}</div>`;
const homeTop = `<div class="top"><div class="wordmark">${mark(30)}MyPlay</div><div class="iconbtn" aria-label="Search">${I('search', 22)}</div></div>`;
const picCard = (o = {}) => card(`<div style="padding:12px 12px 6px">${carPic(354, o)}</div>${row({ label: 'Car screen', value: o.design ?? 'ORBIT', style: 'min-height:52px' })}`);
const check = `<span style="color:var(--int)">${I('check', 22, 2.5)}</span>`;
const choice = (label, on, help) => row({ label, help, right: on ? check : '', nochev: true, style: help ? '' : 'min-height:56px' });
const sheet = (title, sub, inner, extra = '') => `<div class="scrim"></div><div class="sheet"><div class="grab"></div><h2>${L(title)}</h2>${sub ? `<div class="sub">${H(sub)}</div>` : ''}<div style="margin-top:16px">${inner}</div>${extra}</div>`;
const picker = (title, sub, opts, sel) => sheet(title, sub, card(opts.map((o, i) => choice(o, i === sel)).join('')));
const dialog = (title, sub, inner, b1, b2) => `<div class="scrim"></div><div class="dialog"><h2>${L(title)}</h2>${sub ? `<div class="sub">${sub}</div>` : ''}${inner}<div style="display:flex;justify-content:flex-end;gap:4px;margin-top:20px">${btn(b1, 'text')}${btn(b2, 'primary small')}</div></div>`;
const snack = (t, a) => `<div class="snack">${t}<b>${a}</b></div>`;
const reset = centre(btn('Reset this page', 'text small', 'rotate-ccw'));
const r56 = 'min-height:56px';
const sticky = (x) => `<div style="position:sticky;top:0;z-index:2;background:var(--bg);padding-bottom:8px">${x}</div>`;
const holdNote = note("This car doesn't send holds", 'info');

// ===================== Home =====================
const statusHero = (o) => `<div class="hero" style="padding-top:0">${o.ring}<div class="t">${L(o.t)}</div><div class="s">${H(o.s)}</div></div>`;
const connectedRing = ring(176, { w: 6, glow: true, inner: carGlyph(64, true), halos: [{ r: 98, o: 0.12, w: 1.5 }] });

add('home-connected', 'Home — connected', 'The status hero in the Home world colour, Details collapsed, the small car picture (tap → Screen), one primary action.', {
  tab: 'home',
  body: homeTop + statusHero({ ring: connectedRing, t: 'Connected', s: 'Sedan · USB' }) + gap(14) +
    cards(row({ ic: 'info', label: 'Details', value: '1080p · 60 fps' })) + gap(12) + pad(picCard()) + gap(14) +
    pad(`<div class="btns">${btn('Preview', 'primary', 'monitor')}${btn('Disconnect', 'ghost')}</div>`),
});

add('home-connecting', 'Home — connecting', 'The ring sweeps while MyPlay connects; nothing to do but wait or cancel.', {
  tab: 'home',
  body: homeTop + statusHero({ ring: ring(176, { w: 6, pct: 0.3, inner: carGlyph(60, false), halos: [{ r: 98, o: 0.12, w: 1.5 }] }), t: 'Connecting to Sedan…', s: 'USB' }) + gap(18) +
    centre(btn('Cancel', 'ghost')) + gap(24) + pad(picCard({ dim: true })),
});

add('home-not-connected', 'Home — not connected (Bluetooth off)', 'One reason, one action. The other reasons (Wi-Fi off, Wireless off in MyPlay, service stopped) use the same layout.', {
  tab: 'home',
  body: homeTop + statusHero({ ring: ring(176, { w: 3, color: '#7A8698', dashed: '3 11', track: false, inner: carGlyph(60, false), innerColor: 'var(--text2)' }), t: 'Not connected', s: 'Bluetooth is off' }) + gap(18) +
    pad(btn('Turn on Bluetooth', 'primary wide', 'bluetooth')) + note('Or plug in a USB cable').replace('class="note"', 'class="note" style="justify-content:center"') + gap(18) +
    cards(row({ ic: 'sparkles', label: 'Finish setup', help: 'Notification access, car audio', value: '2 left', right: '<i class="badge" style="position:static;box-shadow:none"></i>' })) + gap(12) + pad(picCard({ dim: true })),
});

add('home-trust', 'Home — trust a new car', 'The one security decision in the app. The fingerprint is in ⓘ; the warning is a single line.', {
  tab: 'home',
  body: homeTop + statusHero({ ring: ring(176, { w: 4, color: 'var(--caution)', dashed: '10 8', inner: `<span style="color:var(--caution)">${I('shield-check', 60, 1.6)}</span>` }), t: 'New car — trust it?', s: 'Head unit · USB' }) + gap(16) +
    pad(card(row({ ic: 'key-round', label: 'SHA-256 4F2A 9C1E 07B3 D5A8…', info: true, nochev: true, warn: 'Only trust the car you are in now' }))) + gap(16) +
    pad(`<div class="btns">${btn('Not now', 'ghost')}${btn('Trust', 'primary', 'shield-check')}</div>`) + note('Then MyPlay reconnects', 'refresh-cw'),
});

add('home-update', 'Home — update available', 'The hero stays; the update card sits below it (dismissible for that version). Badge on More.', {
  tab: 'home', badgeMore: true,
  body: homeTop + statusHero({ ring: ring(132, { w: 5, glow: true, inner: carGlyph(48, true) }), t: 'Connected', s: 'Sedan · USB' }) + gap(14) +
    pad(card(`<div style="padding:16px;display:flex;gap:14px;align-items:center"><div class="ic">${I('download', 20)}</div>
      <div class="lbl"><div class="l" style="font-weight:600">${L('MyPlay 2.4 is ready')}</div><div class="h">${H('You have 2.3.1')}</div></div>
      <div class="iconbtn" aria-label="Dismiss">${I('x', 18)}</div></div><div style="padding:0 16px 16px" class="btns">${btn('Install', 'primary small', 'download')}${btn("What's new", 'ghost small')}</div>`, 'acc')) + gap(12) +
    pad(picCard()),
});

add('home-deck', 'Home — an archived design in use', 'A quiet card when DECK (or an older layout) is on; it opens More › Archive.', {
  tab: 'home',
  body: homeTop + statusHero({ ring: ring(132, { w: 5, glow: true, inner: carGlyph(48, true) }), t: 'Connected', s: 'Sedan · USB' }) + gap(14) +
    cards(row({ ic: 'archive', label: 'Using DECK', help: 'Kept as it was. Not updated any more.' })) + gap(12) +
    pad(card(`<div style="padding:12px 12px 6px">${deckPic(354)}</div>${row({ label: 'Car screen', value: 'DECK', style: 'min-height:52px' })}`)),
});

add('home-error', 'Home — an error', 'Couldn’t connect: the reason in one line, Try again, and a “What happened” row (agreed in review).', {
  tab: 'home',
  body: homeTop + statusHero({ ring: ring(176, { w: 5, color: 'var(--warn)', pct: 0.3, inner: `<span style="color:var(--warn)">${carGlyph(60, false)}</span>` }), t: "Couldn't connect", s: 'The car stopped the video' }) + gap(18) +
    pad(btn('Try again', 'primary wide', 'refresh-cw')) + gap(16) +
    cards(row({ ic: 'circle-alert', icCls: 'warn', label: 'What happened', value: '12:38' }), row({ ic: 'activity', label: 'Run a car check' })) + gap(12) + pad(picCard({ dim: true })),
});

// ===================== Screen =====================
add('screen', 'Screen — the tab root', 'The large car picture, clean. Tapping a part opens its page; Design is the first row. In the build the picture is the live render (CarPicture), not this drawing.', {
  tab: 'screen',
  body: topRoot('Screen') + pad(carPic(380)) + note('Tap a part of the picture to change it', 'hand') + gap(12) +
    cards(row({ ic: 'layers', label: 'Design', value: 'ORBIT' }), row({ ic: 'sparkles', label: 'Looks', value: 'Midnight (changed)' }),
      row({ ic: 'palette', label: 'Colours', value: 'AARE · own colours', changed: true }), row({ ic: 'image', label: 'Wallpaper', value: 'None' }),
      row({ ic: 'map', label: 'Map colours', value: 'Muted', changed: true }), row({ ic: 'type', label: 'Size', value: 'Larger · Auto', changed: true })),
});

add('screen-pressed', 'Screen — while the picture is pressed', 'Part rings show only while a finger is down (and on the first visit). The hub’s speed opens Car › Speed; Design is the first row.', {
  tab: 'screen',
  body: topRoot('Screen') + pad(carPic(380, { pressed: true })) + note('Tap a part of the picture to change it', 'hand') + gap(12) +
    cards(row({ ic: 'layers', label: 'Design', value: 'ORBIT' }), row({ ic: 'sparkles', label: 'Looks', value: 'Midnight (changed)' })),
});

add('screen-design', 'Screen › Design', 'ORBIT in use, the RACE slot, and ORBIT’s own settings. Panel time is an Int picker (3/5/8/15 s/Never).', {
  tab: 'screen',
  body: topSub('Design') + pad(card(`<div style="padding:12px">${carPic(354)}</div>
      <div class="row">${ring(28, { w: 3, color: 'var(--int)', track: false, fill: 'var(--int)' })}<div class="lbl"><div class="l">ORBIT</div><div class="h">${H('Rounds and rings over the map')}</div></div>${tag('In use', 'acc')}</div>`, '', 'border:2px solid var(--int)')) + gap(12) +
    pad(card(`<div class="row">${ring(28, { w: 2, color: '#7A8698', track: false })}<div class="lbl"><div class="l" style="color:var(--text2)">RACE</div><div class="h">${H('A sportier design')}</div></div>${tag('Coming later', 'mute')}</div>`)) + gap(12) +
    cards(row({ ic: 'layers', label: "Shade behind ORBIT's parts", value: 'Soft' }), row({ ic: 'timer', label: 'Close panels after', value: '5 s', under: ALSO })) +
    note('DECK and older layouts are in More › Archive', 'archive'),
});

const look = (name, cols, o = {}) => `<div class="card" style="padding:14px;position:relative;${o.sel ? 'border:2px solid var(--int);' : ''}height:132px;display:flex;flex-direction:column;justify-content:space-between">
  <div style="position:relative;height:60px;border-radius:14px;background:${cols[0]};overflow:hidden">
    <div style="position:absolute;left:12px;top:12px;width:36px;height:36px;border-radius:50%;border:4px solid ${cols[1]}"></div>
    <div style="position:absolute;right:12px;top:10px;width:40px;height:40px;border-radius:50%;background:${cols[2]}"></div>
    <div style="position:absolute;left:58px;top:24px;right:62px;height:6px;border-radius:3px;background:${cols[1]};opacity:.6"></div></div>
  <div style="display:flex;align-items:center;justify-content:space-between"><span style="font-size:15px;font-weight:560">${L(name)}</span>${o.sel ? tag('In use', 'acc') : ''}${o.more ? `<span class="iconbtn" style="width:32px;height:32px;margin-right:-6px" aria-label="More">${I('ellipsis', 18)}</span>` : ''}</div></div>`;
const grid2 = (items) => `<div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">${items.join('')}</div>`;
const saveTile = `<div class="card" style="height:132px;border:1.5px dashed #7A8698;background:transparent;display:grid;place-items:center;text-align:center;color:var(--text)"><div>${centre(ring(44, { w: 2, color: 'var(--int)', track: false, inner: I('plus', 20) }))}<div style="font-size:14px;font-weight:560;margin-top:8px">Save this look</div></div></div>`;
const looksBody = topSub('Looks') + note('The look now set is changed and not saved', 'info') + sec('Built in') + pad(grid2([
  look('MyPlay', ['#0B0F0F', '#3DDBC6', 'linear-gradient(135deg,#3DDBC6,#4CC9F0)']),
  look('Midnight', ['#0A0E18', '#8AB4F8', 'linear-gradient(135deg,#4CC9F0,#8F5BFF)'], { sel: true }),
  look('Forest', ['#0E1612', '#5FD39A', 'linear-gradient(135deg,#B5E48C,#2D9A6A)']),
  look('Ocean', ['#0C1620', '#2EF2D0', 'linear-gradient(135deg,#BDE3FF,#4CC9F0)']),
  look('High contrast', ['#000000', '#FFD600', 'linear-gradient(135deg,#FFFFFF,#FFD600)']),
])) + sec('Yours') + pad(grid2([look('Night drive', ['#0D141A', '#FFB000', 'linear-gradient(135deg,#FFB000,#FF5A4E)'], { more: true }), saveTile])) +
  gap(14) + centre(btn('Open a look file', 'text small', 'download'));
add('screen-looks', 'Screen › Looks — a saved look’s menu', 'The real built-in looks. Each saved look has a menu (Rename, Share as a file, Delete with undo); no page-level Share.', {
  tab: 'screen', tall: true, body: looksBody,
  overlay: `<div class="menu" style="left:140px;top:640px">${[['file-text', 'Rename'], ['share-2', 'Share as a file'], ['x', 'Delete']].map(([i, t], k) => `<div class="${k === 2 ? 'danger' : ''}">${I(i, 18)}${t}</div>`).join('')}</div>`,
});

add('screen-looks-apply', 'Screen › Looks — the apply sheet', 'Before a look is applied: what it changes, and a confirm because this saved look uses DECK.', {
  tab: 'screen', body: topSub('Looks') + sec('Yours', '', true) + pad(grid2([look('Night drive', ['#0D141A', '#FFB000', 'linear-gradient(135deg,#FFB000,#FF5A4E)']), saveTile])),
  overlay: sheet('Use Night drive?', 'This look changes:', `<div style="display:flex;flex-wrap:wrap;gap:8px;margin-bottom:16px">${['Colours', 'Wallpaper', 'Map colours', 'Older-layout settings'].map((t) => tag(t, 'mute')).join('')}</div>
    ${card(row({ ic: 'triangle-alert', icCls: 'warn', label: 'This look uses DECK', help: 'The car screen switches from ORBIT', nochev: true }))}
    <div class="btns" style="margin-top:20px">${btn('Cancel', 'ghost')}${btn('Use it', 'primary')}</div>`),
});

const disc = (bg, name, sel, size = 44) => `<div style="display:flex;flex-direction:column;align-items:center;width:72px"><div class="disc${sel ? ' sel' : ''}" style="background:${bg};width:${size}px;height:${size}px"></div><div class="disc-l">${name}</div></div>`;
const themes = [['AARE', '#0B0F0F', '#3DDBC6'], ['Android Auto', '#202124', '#8AB4F8'], ['Android Auto (2019)', '#1A1A1A', '#8AB4F8'], ['CarPlay', '#000000', '#0A84FF'], ['Material You', '#1B1B22', '#C3B8FF'], ['High contrast', '#000000', '#FFD600'], ['Night', '#000000', '#FFB000'], ['Retro', '#000000', '#33FF66']];
add('screen-colours', 'Screen › Colours', 'The preview is an app screen inside ORBIT — what these colours change — labelled “App screens”, sticky while the page scrolls. The real 8 themes and accents.', {
  tab: 'screen', tall: true,
  body: topSub('Colours') + sticky(pad(`<div style="position:relative">${appPic(380, { bg: '#0B0F0F', surface: '#161D1C', accent: '#3DDBC6', text: '#E8EEED', text2: '#9AA6A4' })}<span class="tag mute" style="position:absolute;left:10px;bottom:10px">App screens · live</span></div>`)) +
    sec('Theme') + pad(card(`<div style="display:grid;grid-template-columns:repeat(4,1fr);gap:14px 0;padding:16px 4px">${themes.map(([n, b, a], i) => disc(`linear-gradient(135deg,${b} 50%,${a} 50%)`, n, i === 0)).join('')}</div>`)) +
    sec('Accent') + pad(card(`<div style="display:flex;gap:10px;align-items:center;padding:16px 16px 6px;flex-wrap:wrap">
      <div class="chip on">Theme's</div>${['#FFB000', '#FF5A4E', '#33FF66', '#2EF2D0', '#8AB4F8', '#E8E8E8'].map((c) => `<div class="disc" style="background:${c};width:30px;height:30px"></div>`).join('')}</div>
      <div style="padding:6px 16px 16px"><div class="chip">Use the phone's colours</div></div>` +
      row({ ic: 'palette', label: 'Your own colours', value: 'Night: 2 set', changed: true }))) +
    sec('Day and night', ALSO) + pad(seg(['Dark', "Car's", 'Light'], 1)) + gap(10) + reset,
});

const role = (n, c, own) => `<div class="row tall"><div class="disc" style="width:36px;height:36px;background:${c};border:1px solid var(--edge2)"></div><div class="lbl"><div class="l">${L(n)}</div><div class="h">${H(own ? `Yours · ${c}` : `The look's own · ${c}`)}</div></div>${own ? `<div class="iconbtn" aria-label="Back to the look's own">${I('rotate-ccw', 18)}</div>` : ''}</div>`;
add('screen-own-colours', 'Screen › Colours › Your own colours', 'Night and day apart; seven parts, each “Yours” or “The look’s own”, with undo per part and a contrast warning.', {
  tab: 'screen', tall: true,
  body: topSub('Your own colours') + pad(seg(['Night', 'Day'], 0)) + gap(12) +
    pad(card(row({ ic: 'triangle-alert', icCls: 'warn', label: 'Hard to read', help: 'Dim text on Cards: 2.9 to 1, aim for 4.5', nochev: true }), 'warn')) + gap(12) +
    pad(card([role('Background', '#0B0F0F'), role('Cards', '#2A2333', true), role('Bar', '#161D1C'), role('Accent', '#FFB000', true), role('Text on the accent', '#1A1200'), role('Text', '#E8EEED'), role('Dim text', '#6B6478')].join(''))) +
    gap(10) + centre(btn('Clear all my colours', 'text small danger')),
});

const wp = (n, bg, sel) => `<div style="display:flex;flex-direction:column;align-items:center;gap:8px"><div style="width:58px;height:58px;border-radius:18px;background:${bg};${sel ? 'box-shadow:0 0 0 3px var(--surface),0 0 0 5px var(--int)' : 'border:1px solid var(--edge2)'}">${n === 'None' ? `<div style="height:100%;display:grid;place-items:center;color:var(--text3)">${I('x', 22)}</div>` : ''}</div><div style="font-size:13px;color:${sel ? 'var(--text)' : 'var(--text2)'}">${n}</div></div>`;
const photoBg = 'linear-gradient(160deg,#2B3A55 0%,#5E4B6E 45%,#C98B6B 80%,#E8B77A 100%)';
add('screen-wallpaper', 'Screen › Wallpaper', 'None, three built-in gradients, or a photo; the photo has Edit and Choose another. Live preview at the top.', {
  tab: 'screen',
  body: topSub('Wallpaper') + pad(`<div style="height:214px;border-radius:20px;background:${photoBg};position:relative;overflow:hidden;border:1px solid var(--edge)"><div style="position:absolute;inset:0;background:rgba(5,8,12,.25)"></div><span class="tag mute" style="position:absolute;left:10px;bottom:10px">Live</span></div>`) + gap(16) +
    pad(card(`<div style="display:flex;justify-content:space-between;padding:16px 14px">${wp('None', '#1A242D')}${wp('Dusk', 'linear-gradient(160deg,#1F2A44,#6B3F69)')}${wp('Forest', 'linear-gradient(160deg,#0F2A1E,#3F6B4A)')}${wp('Ocean', 'linear-gradient(160deg,#0B2236,#1F6F8B)')}${wp('Photo', photoBg, true)}</div>`)) + gap(12) +
    cards(row({ ic: 'sliders-horizontal', label: 'Edit the photo', value: 'Darker 20 %', changed: true }), row({ ic: 'image', label: 'Choose another photo' })) + note('A photo is not in backups or shared looks', 'info'),
});

add('screen-photo-editor', 'Screen › Wallpaper › Edit the photo', 'Full screen: drag and pinch on the car-shaped frame, Darker and Blur, then Use as wallpaper.', {
  tab: 'screen', tabs: false,
  body: `<div class="top sub"><div class="iconbtn" aria-label="Cancel">${I('x', 22)}</div><h1>Edit the photo</h1>${btn('Start again', 'text small')}</div>` +
    pad(`<div style="height:300px;border-radius:12px;background:${photoBg};position:relative;overflow:hidden"><div style="position:absolute;inset:0;background:rgba(5,8,12,.2)"></div>
      <div style="position:absolute;left:0;right:0;top:44px;height:212px;border:2px solid var(--int);box-shadow:0 0 0 999px rgba(5,8,12,.55)"></div></div>`) +
    note('Drag to move, pinch to zoom', 'hand') + gap(16) +
    pad(card(slider('Darker', '20 %', 25, { ends: ['0 %', '80 %'], changed: true }) + '<div style="height:1px;background:var(--edge);margin:0 16px"></div>' + slider('Blur', '0', 0, { ticks: 11, ends: ['0', '10'] }))) +
    `<div style="position:absolute;left:16px;right:16px;bottom:16px">${btn('Use as wallpaper', 'primary wide')}</div>`,
});

const mapSample = (filter) => `<svg width="100%" height="120" viewBox="0 0 180 120" style="display:block;border-radius:14px;filter:${filter}"><rect width="180" height="120" fill="#E8E4DA"/><path d="M0 70 C60 60 100 90 180 50" stroke="#FFFFFF" stroke-width="12" fill="none"/><path d="M0 70 C60 60 100 90 180 50" stroke="#F6C55B" stroke-width="7" fill="none"/><path d="M70 0 L90 120" stroke="#FFFFFF" stroke-width="7"/><rect x="110" y="10" width="50" height="30" rx="4" fill="#BFE0B0"/><path d="M0 110 C40 100 80 115 180 100 L180 120 L0 120Z" fill="#A9CFF0"/><circle cx="88" cy="74" r="7" fill="#1A73E8" stroke="#fff" stroke-width="3"/></svg>`;
add('screen-map-colours', 'Screen › Map colours', 'Before/after is the page’s preview (sticky). Presets as chips, then the six sliders from the code.', {
  tab: 'screen', tall: true,
  body: topSub('Map colours') + sticky(pad(card(`<div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;padding:12px">
      <div>${mapSample('none')}<div class="disc-l" style="text-align:left">As the app draws it</div></div>
      <div>${mapSample('saturate(.55) brightness(.62) contrast(1.05)')}<div class="disc-l" style="text-align:left">On the car screen</div></div></div>`))) +
    sec('Preset') + pad(`<div class="chips">${['None', 'Muted', 'Cool', 'Warm', 'Darker nights', 'Grey'].map((c, i) => `<div class="chip${i === 1 ? ' on' : ''}">${c}</div>`).join('')}</div>`) +
    sec('Fine-tune') + pad(card([slider('Hue shift', '0°', 50, { ends: ['-180°', '180°'] }), slider('Colourfulness', '55 %', 27.5, { ends: ['0 %', '200 %'], changed: true }), slider('Brightness', '-38', 31, { ends: ['-100', '100'], changed: true }), slider('Contrast', '105 %', 52.5, { ends: ['0 %', '200 %'], changed: true }), slider('Tint', '0 %', 0, { ends: ['0 %', '100 %'] }), slider('Darker at night', '0 %', 0, { ends: ['0 %', '90 %'] })].join('<div style="height:1px;background:var(--edge);margin:0 16px"></div>'))) + gap(12) + reset,
});

add('screen-size', 'Screen › Size', 'Text and control size are three-step segmented controls (Default/Larger/Largest). How much fits: Auto or a fixed dpi from a list.', {
  tab: 'screen',
  body: topSub('Size') + pad(`<div style="position:relative">${carPic(380)}<span class="tag mute" style="position:absolute;left:50%;top:10px;transform:translateX(-50%)">Live</span></div>`) +
    sec('Text size', '<i class="changed"></i>') + pad(seg(['Default', 'Larger', 'Largest'], 1)) +
    sec('Control size') + pad(seg(['Default', 'Larger', 'Largest'], 0)) +
    sec('How much fits', NEXT) + pad(seg(['Auto', 'Fixed'], 0)) + note('Fixed: 120, 160, 200, 213 or 240 dpi', 'info') + gap(6) + reset,
});

add('screen-size-fixed', 'Screen › Size — a fixed density', 'Choosing Fixed opens the dpi list (an Int picker); the chosen value shows on its own row.', {
  tab: 'screen',
  body: topSub('Size') + pad(carPic(380)) + sec('How much fits', NEXT) + pad(seg(['Auto', 'Fixed'], 1)) + gap(12) + cards(row({ label: 'Density', value: '213 dpi', changed: true })),
  overlay: picker('How much fits', 'Higher draws larger, so less fits', ['120 dpi', '160 dpi', '200 dpi', '213 dpi', '240 dpi'], 3),
});

// ===================== Car =====================
const limitSign = `<svg width="20" height="20" viewBox="0 0 20 20"><circle cx="10" cy="10" r="8" fill="none" stroke="currentColor" stroke-width="2.4"/><text x="10" y="13.5" font-size="8" font-weight="800" text-anchor="middle" fill="currentColor">50</text></svg>`;
const carRoot = () => topRoot('Car') + cards(
  row({ ic: 'circle-dot', label: 'Buttons', value: '2 changed', changed: true }), row({ ic: 'map', label: 'Maps', value: 'Last used' }),
  row({ ic: 'gauge', icHtml: limitSign, label: 'Speed', value: 'Auto · Northvale' }),
  row({ ic: 'volume-2', label: 'Sound', value: 'Shizuku or root' }), row({ ic: 'message-circle', label: 'Messages and calls', value: 'Banners on' }),
  row({ ic: 'mic', label: 'Voice assistant', value: 'Same as the phone' })) + gap(12) +
  cards(row({ ic: 'users', label: 'Passenger mode', sw: false, info: true, warn: 'The driver must not use the screen' }));
add('car', 'Car — the tab root', 'Every page with its value. Distinct icons: knob for Buttons, a limit sign for Speed. Passenger mode carries the warning line.', { tab: 'car', body: carRoot() });
add('car-200', 'Car — at 200 % font size', 'Text grows, rows grow and values wrap under the label; nothing clips. Icons and targets keep their size.', { tab: 'car', f200: true, tall: true, body: carRoot() });

function controls(sel) {
  const on = (k) => (k === sel ? '#DCE3EF' : '#7A8698');
  const fillOn = (k) => (k === sel ? 'rgba(220,227,239,.12)' : '#151E26');
  const t = (x, y, s) => `<text x="${x}" y="${y}" font-size="12" font-weight="600" fill="#AAB3C8" text-anchor="middle">${s}</text>`;
  return `<svg width="380" height="300" viewBox="0 0 380 300">
    ${t(190, 16, 'DASH KNOB')}
    <g transform="translate(190 112)"><circle r="70" fill="none" stroke="#2A3540" stroke-width="1.5"/>
      <circle r="52" fill="${fillOn('knob')}" stroke="${on('knob')}" stroke-width="3"/>
      ${Array.from({ length: 24 }, (_, i) => `<line transform="rotate(${i * 15})" x1="0" y1="-44" x2="0" y2="-38" stroke="#6A7686" stroke-width="2"/>`).join('')}
      ${[0, 90, 180, 270].map((a) => `<path transform="rotate(${a})" d="M-7 -58 L0 -67 L7 -58 Z" fill="${on('pad')}"/>`).join('')}
      <circle r="22" fill="#1A242D" stroke="${on('knob')}" stroke-width="2"/></g>
    ${t(190, 210, 'Knob push · Pad')}
    <g transform="translate(66 80)"><circle r="22" fill="${fillOn('menu')}" stroke="${on('menu')}" stroke-width="2"/><path d="M-8 -6 H8 M-8 0 H8 M-8 6 H8" stroke="#AAB3C8" stroke-width="2.2" stroke-linecap="round"/></g>${t(66, 118, 'Menu')}
    <g transform="translate(66 150)"><circle r="22" fill="${fillOn('back')}" stroke="${on('back')}" stroke-width="2"/><path d="M5 -6 L-5 0 L5 6" fill="none" stroke="#AAB3C8" stroke-width="2.4" stroke-linecap="round"/></g>${t(66, 188, 'Back')}
    ${t(190, 240, 'STEERING WHEEL')}
    <g transform="translate(190 268)"><rect x="-60" y="-16" width="120" height="32" rx="16" fill="${fillOn('wheel')}" stroke="${on('wheel')}" stroke-width="2"/>${Array.from({ length: 9 }, (_, i) => `<line x1="${-40 + i * 10}" y1="-8" x2="${-40 + i * 10}" y2="8" stroke="#6A7686" stroke-width="2"/>`).join('')}</g>
    ${t(300, 272, 'Wheel click')}
  </svg>`;
}
const gest = (label, value, o = {}) => row({ label, value: o.off ? '—' : value, changed: o.changed, style: r56, cls: o.off ? 'off' : '', nochev: o.off, tag: o.off ? tag('Needs holds', 'mute') : '' });
add('car-buttons', 'Car › Buttons', 'Dash knob and steering wheel in one drawing. Knob push has press, hold, double, triple and hold + turn; holds show disabled with the note, because this car sends none.', {
  tab: 'car', tall: true,
  body: topSub('Buttons') + pad(card(`<div style="padding:8px 0 4px">${controls('knob')}</div>`)) + note('Tap a control to change what it does', 'hand') +
    sec('Knob push') + holdNote + gap(8) + pad(card([
      gest('Press', 'Press what has the ring'), gest('Press and hold', 'Next song', { off: true }), gest('Double press', 'Next song'),
      gest('Triple press', 'Previous song'), gest('Hold and turn', 'Skip songs', { off: true })].join(''))) + gap(12) +
    cards(row({ ic: 'layout-grid', label: 'ORBIT home: pad shortcuts', value: '1 changed', changed: true }), row({ ic: 'hand', label: 'Teach a car button', value: '2 taught' }), row({ ic: 'timer', label: 'Timing', value: '250 ms' }),
      row({ ic: 'scan-line', label: 'Show what a button did', sw: true })) + gap(10) + reset,
});

add('car-buttons-action', 'Car › Buttons — choosing an action', 'An action list, not a switch. Choices that would leave no way out are not offered (the code’s rule).', {
  tab: 'car', body: topSub('Buttons') + pad(card(`<div style="padding:8px 0 4px">${controls('knob')}</div>`)),
  overlay: sheet('Knob push · Double press', null, card(['Press what has the ring', 'Back', 'Home', 'Home, then the app grid', 'Voice assistant', 'Play or pause', 'Next song', 'Previous song', 'Open the maps app', 'Open the music app', 'Open the phone', 'Nothing'].map((o, i) => choice(o, i === 6)).join('').replaceAll('min-height:56px', 'min-height:48px'))),
});

const padKey = (x, y, icon, key, act, col, sel) => `<div style="position:absolute;left:${x - 60}px;top:${y - 30}px;width:120px;display:flex;flex-direction:column;align-items:center">
  ${ring(60, { w: sel ? 3 : 2, color: sel ? 'var(--int)' : '#7A8698', track: false, fill: '#151E26', inner: `<span style="color:${col}">${I(icon, 24)}</span>` })}
  <div style="font-size:13px;font-weight:650;margin-top:4px">${act}</div><div style="font-size:12px;color:var(--text3)">${key}</div></div>`;
add('car-pad-shortcuts', 'Car › Buttons › ORBIT home: pad shortcuts', 'The code’s names. “Home: information only” on top; the four keys and their real actions (Music, Maps, The dial, Apps, Calls, Nothing).', {
  tab: 'car', tall: true,
  body: topSub('ORBIT home: pad shortcuts') + cards(row({ ic: 'info', label: 'Home: information only', help: 'No ring on home; needs The dial or Apps', sw: false })) + gap(8) +
    `<div style="position:relative;height:330px;margin:0 auto;width:340px"><div style="position:absolute;left:40px;top:25px;width:260px;height:260px;border-radius:50%;border:1.5px solid #26313B;background:radial-gradient(circle,#111A21,#0E151B)"></div>
    ${padKey(170, 52, 'map', 'Pad up', 'Maps', W, false)}${padKey(170, 158, 'music', 'Knob push', 'Music', V, false)}${padKey(282, 158, 'circle-dot', 'Pad left or right', 'The dial', B, false)}${padKey(170, 264, 'phone', 'Pad down', 'Calls', C, true)}</div>` +
    cards(row({ label: 'Knob push', value: 'Music', style: r56 }), row({ label: 'Pad up', value: 'Maps', style: r56 }), row({ label: 'Pad left or right', value: 'The dial', style: r56 }), row({ label: 'Pad down', value: 'Calls', changed: true, style: r56 })) +
    note("Works on ORBIT's home while the ring is hidden", 'info') + gap(8) + reset,
});

add('car-timing', 'Car › Buttons › Timing', 'The two timing sliders. The hold slider is disabled with the reason, because this car sends no holds.', {
  tab: 'car',
  body: topSub('Timing') + pad(card(slider('Double press within', '250 ms', 30, { ends: ['150 ms', '500 ms'] }))) + note('A single press waits this long first', 'info') + gap(12) +
    cards(row({ label: 'Hold is longer than', value: 'Not used', nochev: true, cls: 'off', tag: tag('Needs holds', 'mute') })) + holdNote + gap(8) + reset,
});

const forget = `<div class="iconbtn" aria-label="Forget">${I('x', 18)}</div>`;
add('car-teach', 'Car › Buttons › Teach a car button', 'Choose what it should do, then Teach it. Taught buttons list their action and key, each with Forget (undo).', {
  tab: 'car',
  body: topSub('Teach a car button') + cards(row({ ic: 'hand', label: 'What it does', value: 'Open the maps app' })) + gap(16) + pad(btn('Teach it', 'primary wide', 'hand')) +
    sec('Buttons taught') + cards(row({ ic: 'circle-dot', label: 'Open the music app', help: 'Key 0x0054', right: forget }), row({ ic: 'circle-dot', label: 'Voice assistant', help: 'Key 0x0057', right: forget })),
});

add('car-teach-waiting', 'Car › Teach — waiting for the button', 'A breathing ring counts down the 30 seconds; the car screen shows the same prompt.', {
  tab: 'car',
  body: topSub('Teach a car button') + gap(48) + hero(ring(190, { w: 4, color: 'var(--int)', pct: 0.8, halos: [{ r: 108, o: 0.18 }, { r: 122, o: 0.08 }], inner: I('hand', 54, 1.8) }), 'Press the button in the car', 'It will do: Open the maps app') +
    `<div class="center" style="font-size:15px;color:var(--text2);margin-top:6px">Waiting 24 more seconds</div>` + gap(16) + centre(btn('Cancel', 'ghost small')),
});

add('car-teach-offline', 'Car › Teach — not connected', 'Teach (and the Car check) need the car: one line and nothing to tap until it connects.', {
  tab: 'car',
  body: topSub('Teach a car button') + cards(row({ ic: 'hand', label: 'What it does', value: 'Open the maps app' })) + gap(16) + pad(btn('Teach it', 'disabled wide')) + note('Connect to the car first', 'cable'),
});

add('car-maps', 'Car › Maps', 'Map on home and “Home when maps untouched” are pickers (the latter an Int: Off/1/2/5/10 min). Map colours is a cross-tab link.', {
  tab: 'car',
  body: topSub('Maps') + cards(row({ ic: 'house', label: 'Map on home', value: 'Last used', under: ALSO }), row({ ic: 'history', label: 'Home when maps untouched', value: 'Off', under: ALSO }),
    row({ ic: 'sun-moon', label: "Follow the car's lights", sw: true }), row({ ic: 'locate-fixed', label: "Use the car's GPS", help: 'Needs mock location on the phone', sw: true })) + gap(12) +
    cards(row({ ic: 'palette', label: 'Map colours', value: 'Muted', tag: tag('Screen', 'mute'), ext: true })) +
    sec('Maps apps') + cards(row({ lead: appdot('map', W), label: 'Maps' }), row({ lead: appdot('route', '#4CC9F0'), label: 'TrailNav' })),
});

add('car-maps-idle', 'Car › Maps — Home when maps untouched', 'The Int picker for myplay_maps_idle_home.', {
  tab: 'car', body: topSub('Maps') + cards(row({ ic: 'history', label: 'Home when maps untouched', value: 'Off' })),
  overlay: picker('Home when maps untouched', 'Maps full screen goes back home', ['Off', 'After 1 min', 'After 2 min', 'After 5 min', 'After 10 min'], 0),
});

const speedTop = topSub('Speed') + sec('Speed source', ALSO, true) + pad(seg(['Auto', 'Car only', 'Off'], 0)) + note("Car first, else the phone's location", 'info') + gap(12);
add('car-speed', 'Car › Speed', 'Source is three-way; the margin is an Int picker (Off/5/9/15). The limits file shows size and date, Look for a newer one and Delete.', {
  tab: 'car', tall: true,
  body: speedTop + cards(row({ ic: 'triangle-alert', label: 'Warn when over by', value: '9 km/h' }), row({ ic: 'map-pin', label: 'Region', value: 'Northvale' })) + gap(12) +
    pad(card(row({ ic: 'file-text', label: 'Limits file', help: '26 MB, from 2026-11-01', nochev: true }) + `<div class="btns" style="padding:0 16px 16px">${btn('Look for a newer one', 'ghost small')}${btn('Delete', 'text small danger')}</div>`)) + gap(12) +
    cards(row({ ic: 'wifi', label: 'Download on mobile data', sw: false })) + note('Works offline. Looks for a newer file monthly.', 'lock') + gap(12) +
    cards(row({ ic: 'route', label: 'Limit from Waze', help: 'Needs the MyPlay module', sw: false, off: true, under: tag('Coming later', 'mute') })),
});

add('car-speed-download', 'Car › Speed — downloading', 'The file row becomes a progress ring with Cancel; the rest of the page stays usable.', {
  tab: 'car',
  body: speedTop + cards(row({ ic: 'triangle-alert', label: 'Warn when over by', value: '9 km/h' }), row({ ic: 'map-pin', label: 'Region', value: 'Northvale' })) + gap(12) +
    pad(card(`<div class="row" style="min-height:84px">${ring(52, { w: 4, pct: 0.64, color: 'var(--int)', inner: `<span style="font-size:13px;font-weight:700;color:var(--text)">64%</span>` })}
      <div class="lbl"><div class="l">${L('Downloading…')}</div><div class="h">${H('17 of 26 MB · Northvale')}</div></div>${btn('Cancel', 'text small')}</div>`, 'acc')),
});

add('car-speed-offline', 'State — offline (Car › Speed)', 'Offline: one line where the action failed, Try again; never a dialog.', {
  tab: 'car',
  body: speedTop + cards(row({ ic: 'map-pin', label: 'Region', value: 'Northvale' })) + gap(12) +
    pad(card(row({ ic: 'wifi', icCls: 'warn', label: 'No internet', help: 'Try again when the phone is online', nochev: true }) + `<div style="padding:0 16px 16px">${btn('Try again', 'ghost small', 'refresh-cw')}</div>`, 'warn')),
});

const method = (n, h, st) => row({ label: n, help: h, right: st === 'ready' ? tag('Ready', 'ok') : st === 'use' ? tag('In use', 'acc') : btn('Set up', 'ghost small'), nochev: true });
add('car-sound', 'Car › Sound', 'The busiest page: the switch, the method in use, the three methods best first with their states, and Play music when connected.', {
  tab: 'car', tall: true,
  body: topSub('Sound') + cards(row({ ic: 'volume-2', label: 'Phone audio in the car', sw: true })) + gap(12) +
    pad(card(`<div class="row" style="min-height:76px">${ring(44, { w: 3, color: 'var(--ok)', inner: `<span style="color:var(--ok)">${I('check', 20, 2.6)}</span>` })}<div class="lbl"><div class="l">Using: Shizuku or root</div><div class="h">${H('Music and navigation sound reach the car')}</div></div></div>`)) +
    sec('Methods, best first') + cards(method('System audio routing', 'Needs root and a system install', 'setup'), method('Shizuku or root', 'Ready, through Shizuku', 'use'), method('Screen-capture permission', 'Works everywhere, with limits', 'setup')) + gap(12) +
    cards(row({ ic: 'circle-play', label: 'Play music when connected', sw: false })),
});

add('car-sound-setup', 'Car › Sound — nothing set up yet', 'No method ready: the status says so, and the method that can work now is one tap.', {
  tab: 'car',
  body: topSub('Sound') + cards(row({ ic: 'volume-2', label: 'Phone audio in the car', sw: true })) + gap(12) +
    pad(card(row({ ic: 'triangle-alert', icCls: 'warn', label: 'No method set up yet', help: 'Choose one below', nochev: true }), 'warn')) +
    sec('Methods, best first') + cards(method('System audio routing', 'Needs root and a system install', 'setup'),
      row({ label: 'Shizuku or root', help: 'Shizuku is running: tap to allow', right: btn('Allow', 'primary small'), nochev: true }), method('Screen-capture permission', 'Works everywhere, with limits', 'setup')),
});

const msgRows = (off) => [
  row({ ic: 'message-circle', label: 'Show message notifications', sw: !off, off, under: ALSO }), row({ ic: 'users', label: 'Group messages', sw: !off, off }),
  row({ ic: 'volume-2', label: 'Read messages aloud', sw: false, off, under: ALSO })];
add('car-messages', 'Car › Messages and calls', 'The normal state. Do Not Disturb adds its access row when turned on; Auto-reply shows its text field when on.', {
  tab: 'car', tall: true,
  body: topSub('Messages and calls') + cards(...msgRows(false)) + sec('While driving') + cards(
    row({ ic: 'moon', label: 'Do Not Disturb', sw: true }), row({ label: 'Allow MyPlay to turn it on', help: 'Opens Do Not Disturb access', value: '', style: 'padding-left:70px' }),
    row({ ic: 'file-text', label: 'Auto-reply', sw: true }), `<div style="padding:0 16px 16px 70px"><div class="field" style="margin-top:0;font-size:15px">I'm driving right now, I'll answer later.</div></div>`) +
    sec('Calls') + cards(row({ ic: 'phone', label: 'Calls on phone speaker', help: "If the car's mic doesn't work", sw: false, under: NEXT })),
});

add('car-messages-permission', 'State — permissions asked in context', 'Two inline cards, each over only the rows that need it. Rows stay readable; their controls are disabled (dashed).', {
  tab: 'car', tall: true,
  body: topSub('Messages and calls') + pad(card(`<div class="row" style="align-items:flex-start;padding-top:16px"><div class="ic">${I('bell', 20)}</div><div class="lbl"><div class="l">Allow notification access</div><div class="h">${H('Needed to see messages in the car')}</div><div style="margin-top:12px">${btn('Allow', 'primary small')}</div></div></div>`, 'acc')) + gap(10) +
    cards(...msgRows(true), row({ ic: 'file-text', label: 'Auto-reply', sw: false, off: true })) + gap(16) +
    pad(card(`<div class="row" style="align-items:flex-start;padding-top:16px"><div class="ic">${I('contact', 20)}</div><div class="lbl"><div class="l">Allow calls and contacts</div><div class="h">${H('So names show and calls can be answered')}</div><div style="margin-top:12px">${btn('Allow', 'primary small')}</div></div></div>`, 'acc')) + gap(10) +
    cards(row({ ic: 'phone', label: 'Calls on phone speaker', help: "If the car's mic doesn't work", sw: false, off: true })),
});

add('car-voice', 'Car › Voice assistant', 'Same as the phone (default) or a chosen app; each says where it opens.', {
  tab: 'car',
  body: topSub('Voice assistant') + cards(choice('Same as the phone', true, 'Assistant'), row({ lead: appdot('mic', B), label: 'Assistant', help: 'On the car screen, with the car mic', nochev: true }),
    row({ lead: appdot('message-circle', V), label: 'Chatbot', help: 'Opens on the phone', nochev: true })),
});

// ===================== Apps =====================
const app = (icon, col, name, on, o = {}) => row({ lead: appdot(icon, col), label: name, help: o.help, sw: on, right: o.gear ? `<div class="iconbtn" aria-label="${name} settings">${I('settings', 20)}</div>` : undefined, style: o.help ? '' : r56 });
add('apps', 'Apps — the whole list (scrolled)', 'The tab root is the app list: Phone apps on top, every group, the built-ins (Phone, Notifications…), a hidden app, Car data, and “Apps MyPlay can’t show” collapsed.', {
  tab: 'apps', tall: true,
  body: `<div class="top"><div class="tglyph">${I('layout-grid', 18, 2.2)}</div><h1 style="margin-left:6px">Apps</h1>${btn('Reorder', 'text small')}<div class="iconbtn" aria-label="Search">${I('search', 22)}</div></div>` +
    cards(row({ ic: 'smartphone', label: 'Phone apps', help: "Your phone's apps on the car", value: 'Off' })) +
    sec('Maps and car apps') + cards(app('map', W, 'Maps', true), app('route', '#4CC9F0', 'TrailNav', true)) +
    sec('Media') + cards(app('music', V, 'Music', true), app('radio', '#C9A8FF', 'Podcasts', false)) +
    sec('Messaging') + cards(app('message-circle', C, 'Messages', true)) +
    sec('MyPlay') + cards(app('phone', C, 'Phone', true), app('bell', B, 'Notifications', true), app('monitor', K, 'Phone screen', true, { gear: true }),
      app('globe', K, 'Browser', true, { gear: true }), app('tv', K, 'YouTube (web)', true, { help: 'Shown while Browser is on' }), app('activity', A, 'Car data', false)) + gap(12) +
    cards(row({ ic: 'chevron-down', label: "Apps MyPlay can't show", value: '2' })),
});

add('apps-reorder', 'Apps — reorder', 'Drag or the arrows. A hidden app keeps its place, greyed, so showing it again puts it back where it was.', {
  tab: 'apps',
  body: `<div class="top sub"><div class="iconbtn" aria-label="Close">${I('x', 22)}</div><h1>Order in the car</h1>${btn('Done', 'primary small')}<div style="width:8px"></div></div>` +
    note('Drag, or use the arrows', 'grip-vertical') + gap(10) + pad(card([['map', W, 'Maps'], ['route', '#4CC9F0', 'TrailNav'], ['music', V, 'Music'], ['radio', '#C9A8FF', 'Podcasts', 1], ['message-circle', C, 'Messages'], ['phone', C, 'Phone'], ['monitor', K, 'Phone screen'], ['globe', K, 'Browser']]
      .map(([ic, col, n, hid], i) => `<div class="row" style="min-height:60px;${i === 2 ? 'margin:6px 8px;border-radius:18px;background:#1F2A34;border:1.5px solid var(--int);box-shadow:0 12px 28px rgba(0,0,0,.5)' : ''}"><span style="color:var(--text3)">${I('grip-vertical', 20)}</span><div style="${hid ? 'opacity:.45' : ''}">${appdot(ic, col)}</div><div class="lbl"><div class="l" style="${hid ? 'color:var(--text3)' : ''}">${n}</div>${hid ? '<div class="h">Hidden</div>' : ''}</div><div class="iconbtn" aria-label="Move up" style="width:44px;height:44px;${i === 0 ? 'opacity:.3' : ''}">${I('arrow-up', 20)}</div><div class="iconbtn" aria-label="Move down" style="width:44px;height:44px">${I('arrow-down', 20)}</div></div>`).join(''))),
});

add('apps-phone-apps', 'Apps › Phone apps', 'The risky switch with its warning line (reason in ⓘ). Each allowed app chooses what knob turns move: Up and down, or Left and right.', {
  tab: 'apps', tall: true,
  body: topSub('Phone apps') + cards(row({ ic: 'smartphone', label: 'Launch phone apps on the car', sw: true, info: true, warn: 'Use them only when parked' })) +
    sec('Allowed', tag('3', 'mute')) + cards(row({ lead: appdot('file-text', '#C9CDD6'), label: 'Notes', value: 'Up and down' }), row({ lead: appdot('radio', '#FF8A4C'), label: 'Radio', value: 'Left and right', changed: true }),
      row({ lead: appdot('check', C), label: 'Tasks', value: 'Up and down' }), row({ ic: 'plus', label: 'Add apps', nochev: true })) +
    sec('While they are open') + cards(row({ ic: 'sun', label: 'No-sleep mode', sw: false })) +
    sec('YouTube app') + cards(row({ lead: appdot('tv', K), label: 'YouTube app on the car', help: 'Plays only when parked', right: btn('Put it on', 'ghost small'), nochev: true })),
});

add('apps-phone-apps-no-root', 'Apps › Phone apps — without root or Shizuku', 'The switch is disabled and one line says why, with a link to set it up on the Sound page.', {
  tab: 'apps',
  body: topSub('Phone apps') + pad(card(row({ ic: 'lock', icCls: 'warn', label: 'Needs root or Shizuku', help: 'Set one up in Car › Sound, then come back', nochev: true }) + `<div style="padding:0 16px 16px">${btn('Open Sound', 'ghost small', 'external-link')}</div>`, 'warn')) + gap(12) +
    cards(row({ ic: 'smartphone', label: 'Launch phone apps on the car', sw: false, off: true }), row({ lead: appdot('tv', K), label: 'YouTube app on the car', value: '', off: true, nochev: true })),
});

// ===================== More =====================
add('more', 'More — the tab root', 'Rare things, each one tap away; values on the right. The update tag uses the attention colour, matching the tab badge.', {
  tab: 'more', badgeMore: true,
  body: topRoot('More') + cards(row({ ic: 'download', label: 'Updates', value: `<span class="tag" style="background:color-mix(in srgb,var(--caution) 16%,transparent);color:var(--caution)">2.4 ready</span>` }),
    row({ ic: 'car-front', label: 'Cars', value: '2 cars' }), row({ ic: 'wifi', label: 'Connection', value: 'Wireless on' }), row({ ic: 'history', label: 'Backup', value: 'Saved 3 Nov' }),
    row({ ic: 'activity', label: 'Diagnostics', value: 'Car check: not run' })) + gap(12) +
    cards(row({ ic: 'sliders-horizontal', label: 'Advanced', value: 'Normal' }), row({ ic: 'archive', label: 'Archive', value: 'Not used' }), row({ ic: 'info', label: 'About', value: '2.3.1' })),
});

const updRows = (checked = 'Checked 09:12') => cards(row({ ic: 'sparkles', label: "What's new" }), row({ ic: 'refresh-cw', label: 'Check now', value: checked, nochev: true }), row({ ic: 'bell', label: 'Check automatically', help: 'Once a day, from GitHub', sw: true }));
add('more-updates', 'More › Updates — ready', 'The one home for updates. The hero ring is in More’s world colour; Install is the primary action.', {
  tab: 'more', badgeMore: true,
  body: topSub('Updates') + hero(ring(150, { w: 5, glow: true, inner: I('download', 46, 1.8) }), 'MyPlay 2.4 is ready', 'You have 2.3.1') + gap(18) + pad(btn('Download and install 2.4', 'primary wide', 'download')) + gap(18) + updRows(),
});
add('more-updates-installing', 'More › Updates — installing', 'Download progress in the ring and on the button; then Android’s own confirm window.', {
  tab: 'more', badgeMore: true,
  body: topSub('Updates') + hero(ring(150, { w: 5, pct: 0.42, color: 'var(--int)', inner: `<span style="font-size:30px;font-weight:700">42%</span>` }), 'Downloading 2.4…', 'Then confirm in Android’s window') + gap(18) + pad(btn('Installing 2.4…', 'disabled wide')) + gap(18) + updRows(),
});
add('more-updates-failed', 'More › Updates — failed, and “allow unknown apps”', 'Each failure is one line with Try again. The first install needs Android’s “install unknown apps” permission: one card, one action.', {
  tab: 'more', badgeMore: true,
  body: topSub('Updates') + hero(ring(120, { w: 5, color: 'var(--warn)', pct: 0.3, inner: `<span style="color:var(--warn)">${I('circle-alert', 40)}</span>` }), "Couldn't install 2.4", 'The download was incomplete') + gap(14) + pad(btn('Try again', 'primary wide', 'refresh-cw')) + gap(14) +
    pad(card(row({ ic: 'shield-check', label: 'Allow MyPlay to install apps', help: 'Needed once, in the screen that opens', right: btn('Allow', 'ghost small'), nochev: true }), 'acc')) + gap(12) + updRows(),
});
add('more-updates-current', 'More › Updates — up to date', 'Nothing to do: a calm full ring and the version.', {
  tab: 'more',
  body: topSub('Updates') + hero(ring(150, { w: 5, inner: I('check', 50, 2.2) }), 'Up to date', 'MyPlay 2.4.0') + gap(28) + updRows('Checked just now'),
});
add('more-whats-new', 'More › Updates › What’s new', 'Release notes as short lines, newest first. Shown once after an update, never on a fresh install; the update question is asked here once after the rebuild.', {
  tab: 'more', tall: true,
  body: topSub("What's new", '<div style="width:48px"></div>') + sec('2.4', '', true) + pad(card(['New looks: Forest and Ocean', 'Waze limits stay off the arrow', 'Faster start when the car connects', 'Car check: pad steps in one'].map((t) => `<div class="row" style="min-height:52px"><span style="color:var(--acc)">${I('sparkles', 18)}</span><div class="lbl"><div class="l" style="font-size:15px">${t}</div></div></div>`).join(''))) +
    gap(12) + pad(card(row({ ic: 'bell', label: 'Check for updates by itself?', help: 'Once a day, from GitHub', nochev: true }) + `<div class="btns" style="padding:0 16px 16px">${btn('No', 'ghost small')}${btn('Yes', 'primary small')}</div>`, 'acc')) +
    sec('2.3') + pad(card(['Triple press: previous song', 'This car sends no holds: noted'].map((t) => `<div class="row" style="min-height:52px"><span style="color:var(--text3)">${I('check', 18)}</span><div class="lbl"><div class="l" style="font-size:15px">${t}</div></div></div>`).join(''))),
});

const carRow = (n, h, trusted) => row({ lead: `<div class="ic">${I('car-front', 20)}</div>`, label: n, help: h, under: trusted ? tag('Trusted', 'ok') : '' });
add('more-cars', 'More › Cars', 'One list: each car with its last connection and how, and a Trusted tag (the pin). Forget lives on the car’s page.', {
  tab: 'more',
  body: topSub('Cars') + cards(carRow('Sedan', 'Today 08:12 · USB', true), carRow('Hatchback', '14 Nov · Wireless', true), carRow('Head unit server', '2 Nov · Wi-Fi', false)),
});
add('more-car', 'More › Cars › a car', 'A car’s own page: its trust fingerprint, the per-car slot, and Forget (undo snackbar; the next connection asks for Trust again).', {
  tab: 'more',
  body: topSub('Sedan', '<div style="width:48px"></div>') + hero(ring(120, { w: 4, inner: carGlyph(44, true) }), 'Sedan', 'Last connected today 08:12 · USB') + gap(16) +
    cards(row({ ic: 'shield-check', label: 'Trusted', help: 'SHA-256 4F2A 9C1E 07B3 D5A8…', nochev: true }), row({ ic: 'wrench', label: 'Car fixes', value: 'None', under: tag('Coming later', 'mute') })) + gap(16) +
    centre(btn('Forget this car', 'ghost danger', 'x')),
});
add('more-cars-undo', 'More › Cars — after Forget (undo snackbar)', 'Forget acts at once and offers Undo for a few seconds: the rule for things that can be undone.', {
  tab: 'more',
  body: topSub('Cars') + cards(carRow('Hatchback', '14 Nov · Wireless', true), carRow('Head unit server', '2 Nov · Wi-Fi', false)),
  overlay: snack('Sedan forgotten', 'Undo'),
});
add('more-cars-empty', 'State — an empty list (More › Cars)', 'Faint rings, one line.', {
  tab: 'more',
  body: topSub('Cars', '<div style="width:48px"></div>') + `<div style="padding-top:110px">${hero(ring(150, { w: 2, color: '#7A8698', track: false, dashed: '2 8', halos: [{ r: 95, o: 0.25 }, { r: 120, o: 0.12 }], inner: carGlyph(56, false), innerColor: 'var(--text2)' }), 'No cars yet', 'Cars appear here after the first connection.')}</div>`,
});

add('more-connection', 'More › Connection', 'Only how the phone reaches the car: Wireless and the optional Bluetooth address (video moved to Advanced).', {
  tab: 'more',
  body: topSub('Connection') + cards(row({ ic: 'wifi', label: 'Wireless', sw: true }), row({ ic: 'bluetooth', label: "Phone's Bluetooth address", help: 'Optional, if the car needs it', value: 'Auto' })),
});

add('more-backup', 'More › Backup', 'Save and Load, the last save, and the one line about what is not in the file.', {
  tab: 'more',
  body: topSub('Backup') + cards(row({ ic: 'download', label: 'Save to a file', help: 'Last saved 3 Nov', right: btn('Save', 'primary small'), nochev: true }), row({ ic: 'file-text', label: 'Load from a file', help: 'Replaces your settings; asks first', right: btn('Load', 'ghost small'), nochev: true })) +
    note('Photos, certificates and trusted cars are not in it', 'info'),
});
add('more-backup-confirm', 'More › Backup — load confirm', 'Loading can’t be undone, so it asks (the confirm rule).', {
  tab: 'more',
  body: topSub('Backup') + cards(row({ ic: 'download', label: 'Save to a file', help: 'Last saved 3 Nov', nochev: true }), row({ ic: 'file-text', label: 'Load from a file', help: 'Replaces your settings; asks first', nochev: true })),
  overlay: dialog('Replace your settings?', 'Your current settings are overwritten by the file. This can’t be undone.', '', 'Cancel', 'Replace'),
});
add('more-backup-loaded', 'More › Backup — after loading', 'The result, skipped keys counted, and the after-loading list: what to set again on this phone.', {
  tab: 'more', tall: true,
  body: topSub('Backup') + pad(card(row({ ic: 'check', label: 'Settings loaded', help: '3 left out as unknown or blocked', nochev: true }), 'acc')) +
    sec('Set these again on this phone') + cards(row({ ic: 'image', label: 'Wallpaper photo', value: 'Choose' }), row({ ic: 'gauge', label: 'Download Northvale limits', value: '26 MB' }),
      row({ ic: 'shield-check', label: 'Certificates', value: 'Import' }), row({ ic: 'car-front', label: 'Trusted cars', help: 'Each car asks once', nochev: true })),
});

add('more-diagnostics', 'More › Diagnostics', 'Three tools, each with its state.', {
  tab: 'more',
  body: topSub('Diagnostics') + cards(row({ ic: 'activity', label: 'Car check', value: 'Not run' }), row({ ic: 'gauge', label: 'Car data', value: 'Live' }), row({ ic: 'scan-line', label: 'Connection log', value: '214 lines' })),
});

const steps = 12;
const stepBar = (n) => pad(`<div style="display:flex;gap:4px">${Array.from({ length: steps }, (_, i) => `<i style="flex:1;height:4px;border-radius:2px;background:${i < n - 1 ? 'var(--int)' : i === n - 1 ? '#8A95A8' : '#2A3540'}"></i>`).join('')}</div>`);
function fourButtons() {
  const node = (x, y, label, st) => {
    const done = st === 'done', wait = st === 'wait';
    const inner = done ? `<span style="color:var(--on)">${I('check', 24, 3)}</span>` : `<span style="color:${wait ? 'var(--int)' : 'var(--text2)'};font-size:12px;font-weight:700">${label}</span>`;
    return `<div style="position:absolute;left:${x - 32}px;top:${y - 32}px">${ring(64, { w: 3, color: done || wait ? 'var(--int)' : '#7A8698', track: false, fill: done ? 'var(--int)' : '#121A21', dashed: wait ? '6 8' : undefined, halos: wait ? [{ r: 40, o: 0.25 }] : [], inner })}</div>`;
  };
  return `<div style="position:relative;width:260px;height:262px;margin:0 auto"><div style="position:absolute;left:92px;top:87px;width:76px;height:76px;border-radius:50%;border:2px solid #3A4652;background:#151E26;display:grid;place-items:center;font-size:13px;color:var(--text2)">Knob</div>
    ${node(130, 34, '1', 'done')}${node(226, 125, '2', 'done')}${node(130, 220, '3', 'wait')}${node(34, 125, '4', 'todo')}</div>`;
}
add('more-car-check', 'More › Diagnostics › Car check — the 4-button step', 'Step 5: the four buttons round the knob, in any order. The four pad steps are drawn merged into step 6 (12 steps, not 15).', {
  tab: 'more',
  body: topSub('Car check', '<div class="val" style="padding-right:16px">5 of 12</div>') + stepBar(5) +
    `<div class="hero" style="padding-top:20px"><div class="t" style="margin-top:0">${L('Press each of the 4 buttons')}</div><div class="s">${H('Round the knob, one at a time')}</div></div>` + gap(12) + fourButtons() + gap(14) +
    cards(row({ ic: 'scan-line', label: 'Heard from the car', value: '2 of 4', nochev: true })) + gap(14) +
    pad(`<div class="btns">${btn('Nothing happened', 'ghost')}${btn('Next', 'primary')}</div>`),
});
add('more-car-check-share', 'More › Car check — done, share', 'The steps summary, then “Include alert texts (may contain street names)”, unticked each time, and Share the log.', {
  tab: 'more', tall: true,
  body: topSub('Car check', '<div style="width:48px"></div>') + hero(ring(120, { w: 5, inner: I('check', 44, 2.4) }), 'All steps done', '12 steps · 2 to look at') + gap(14) +
    cards(row({ ic: 'circle-dot', label: 'Knob', value: tag('All good', 'ok'), nochev: true }), row({ ic: 'circle-dot', label: 'Buttons round the knob', value: tag('3 of 4', 'warn'), nochev: true }),
      row({ ic: 'circle-dot', label: 'Holds', value: tag('None sent', 'mute'), nochev: true })) + gap(14) +
    pad(card(`<div class="row"><div class="cb"></div><div class="lbl"><div class="l">Include alert texts</div><div class="h">${H('May contain street names')}</div></div></div>` + row({ ic: 'file-text', label: 'See what is shared' }))) + gap(14) +
    pad(btn('Share the log', 'primary wide', 'share-2')),
});
add('more-car-check-offline', 'More › Car check — not connected', 'The check listens to the car, so it starts only when connected.', {
  tab: 'more',
  body: topSub('Car check', '<div style="width:48px"></div>') + `<div style="padding-top:80px">${hero(ring(150, { w: 3, color: '#7A8698', dashed: '3 10', track: false, inner: I('cable', 48, 1.8), innerColor: 'var(--text2)' }), 'Connect first', 'The check listens to what the car sends')}</div>` + gap(20) + centre(btn('Start', 'disabled')),
});

const live = (k, v) => `<div class="row" style="min-height:48px"><div class="lbl"><div class="l" style="font-size:15px;color:var(--text2)">${k}</div></div><div class="val" style="color:var(--text);font-variant-numeric:tabular-nums">${v}</div></div>`;
add('more-car-data', 'More › Diagnostics › Car data', 'The car-screen switch, the car’s identity, and live values (tabular numbers).', {
  tab: 'more', tall: true,
  body: topSub('Car data') + cards(row({ ic: 'monitor', label: 'Show on the car screen', sw: false })) +
    sec('Car') + pad(card([live('Vehicle', 'Sample Motors Sedan 2019'), live('Head unit', 'Generic HU 4.2'), live('Connection', 'USB · 1080p 60 fps')].join(''))) +
    sec('Live data', tag('Live', 'ok')) + pad(card([live('Driving', 'Parked'), live('Lighting', 'Night'), live('Speed', '0 km/h'), live('Gear', 'P'), live('Fuel', '62 % · 480 km'), live('Outside', '7 °C'), live('Doors', 'All closed'), live('Tyre pressure', '230 · 230 · 240 · 240 kPa')].join(''))),
});
add('more-log', 'More › Diagnostics › Connection log', 'The two log switches (Detailed protocol log lives only here), the last lines, Copy / Save / Clear.', {
  tab: 'more', tall: true,
  body: topSub('Connection log') + cards(row({ ic: 'scan-line', label: 'Detailed protocol log', sw: false }), row({ ic: 'circle-dot', label: "Log the car's buttons and knob", sw: true })) + gap(12) +
    pad(`<div class="card" style="padding:14px 16px;font-family:ui-monospace,Menlo,monospace;font-size:11.5px;line-height:18px;color:var(--text2);height:300px;overflow:hidden">${['08:12:01 USB attached', '08:12:01 Version 1.7 agreed', '08:12:02 TLS done', '08:12:02 Service list: 14', '08:12:03 Video 1920×1080 @60', '08:12:03 Audio: media, guidance', '08:12:04 Focus: projection', '08:12:09 Key 19 down', '08:12:09 Key 19 up', '08:12:15 Night mode on', '08:13:40 Sensor: speed 0', '08:14:02 Key 65536 turn +1', '08:14:02 Key 65536 turn +1', '08:15:30 Ping 12 ms', '08:16:11 Key 4 down'].join('<br>')}</div>`) + gap(12) +
    pad(`<div class="btns">${btn('Copy', 'ghost small')}${btn('Save', 'ghost small')}${btn('Clear', 'text small danger')}</div>`) + note('Logs stay on the phone until you share them', 'lock'),
});

add('more-advanced', 'More › Advanced', 'Video: Maximum resolution is an Int picker (480p/720p/1080p/1440p), the others are switches. Certificates, car fixes, Developer.', {
  tab: 'more', tall: true,
  body: topSub('Advanced') + sec('Video', NEXT, true) + cards(row({ label: 'Maximum resolution', value: '1080p', style: r56 }), row({ label: 'Prefer 60 fps', sw: true, style: r56 }), row({ label: 'Force H.264 video', sw: false, style: r56 }), row({ label: 'Constant bitrate video', sw: false, style: r56 })) +
    sec('Security') + cards(row({ ic: 'shield-check', label: 'Certificates', value: "MyPlay's" })) +
    sec('Car fixes') + cards(row({ ic: 'wrench', label: 'Per-vehicle overrides', value: '0 rules' })) + gap(12) +
    cards(row({ ic: 'code', label: 'Developer' })),
});
add('more-advanced-resolution', 'More › Advanced — Maximum resolution', 'The picker pattern for an Int setting: a sheet of the real values, the current one checked.', {
  tab: 'more', body: topSub('Advanced') + sec('Video', NEXT, true) + cards(row({ label: 'Maximum resolution', value: '1080p', style: r56 })),
  overlay: picker('Maximum resolution', 'From the next connection', ['480p', '720p', '1080p', '1440p'], 2),
});

add('more-certificates', 'More › Advanced › Certificates', 'The list with expiry, Import and Download, and the automatic update switch.', {
  tab: 'more',
  body: topSub('Certificates') + cards(row({ ic: 'shield-check', label: "MyPlay's", help: 'Built in · until 2031-04-30', nochev: true }), row({ ic: 'key-round', label: 'Downloaded', help: 'Phone cert · until 2027-03-12', right: btn('Remove', 'text small danger'), nochev: true })) + gap(12) +
    pad(`<div class="btns">${btn('Import…', 'ghost small', 'file-text')}${btn('Download', 'ghost small', 'download')}</div>`) + gap(12) +
    cards(row({ ic: 'refresh-cw', label: 'Update it automatically', sw: false })),
});
add('more-certificates-password', 'More › Certificates — PKCS#12 password', 'The one text dialog in the app; it survives rotation.', {
  tab: 'more', body: topSub('Certificates') + cards(row({ ic: 'shield-check', label: "MyPlay's", help: 'Built in · until 2031-04-30', nochev: true })),
  overlay: dialog('PKCS#12 password', 'For phone-cert.p12', `<div class="field focus">••••••••<span style="width:2px;height:22px;background:var(--int);margin-left:2px"></span></div>`, 'Cancel', 'Import'),
});

add('more-developer', 'More › Advanced › Developer', 'The real sub-switches. Risky ones carry the warning line in the warning colour, reason in ⓘ.', {
  tab: 'more', tall: true,
  body: topSub('Developer') + pad(card(row({ ic: 'terminal', label: 'Head unit server', sw: true, info: true, warn: 'No password: anyone on the Wi-Fi can connect' }) +
    [['Accept from the network', true, 'Anyone on the Wi-Fi can connect'], ['Off after one session', true], ['Phone screen on the server', false, 'They can see and tap the phone'], ['Allow localhost', false]]
      .map(([n, on, w]) => row({ label: n, sw: on, warn: w, info: !!w, style: 'padding-left:70px' })).join(''))) + gap(12) +
    cards(row({ ic: 'layers', label: "Show the map's free area", sw: false }), row({ ic: 'file-text', label: "Log the maps app's texts", sw: false }), row({ ic: 'square-stack', label: 'Phone parts sample' }), row({ ic: 'scan-line', label: 'Detailed protocol log', help: 'Lives in Connection log', ext: true })),
});

add('more-archive', 'More › Archive', 'DECK and the older layouts, kept as they were.', {
  tab: 'more',
  body: topSub('Archive') + pad(`<div class="card" style="padding:14px 16px;display:flex;gap:12px;align-items:center;background:transparent;border:1.5px dashed #7A8698">${I('archive', 20)}<span style="font-size:14px;color:var(--text2)">${H('Kept as it was. Not updated any more.')}</span></div>`) + gap(12) +
    pad(card(`<div style="padding:12px 12px 4px">${deckPic(354)}</div>` + row({ label: 'Use DECK instead of ORBIT', help: 'Its worlds still use ORBIT’s look', sw: false }))) + gap(12) +
    cards(row({ ic: 'layout-grid', label: 'Older layouts', help: 'Taskbar, dock, grid, map behind' })),
});
add('more-archive-layouts', 'More › Archive › Older layouts', 'Everything that does nothing under ORBIT, grouped and working as before. Weather keeps its privacy line.', {
  tab: 'more', tall: true,
  body: topSub('Older layouts') + cards(row({ ic: 'monitor', label: "Use MyPlay's own screen", help: 'Off: the older layouts below', sw: true })) +
    sec('Layout') + cards(row({ label: 'Kind of layout', value: 'Taskbar', style: r56 }), row({ label: 'Ready-made layout', value: 'Layout D', style: r56 }), row({ label: 'Bar side', value: 'Left', style: r56 }), row({ label: 'Bar items', value: '5', style: r56 }), row({ label: 'Dark bar', sw: false, style: r56 })) +
    sec('Cards, clock and status') + cards(row({ label: 'Dashboard cards', value: '4', style: r56 }), row({ label: 'Clock', value: 'Large', style: r56 }), row({ label: 'Status icons', value: 'Battery, signal', style: r56 })) +
    sec('Weather') + cards(row({ label: 'Show the weather', help: 'Sends an approximate position to Open-Meteo', sw: false })),
});

add('more-about', 'More › About', 'Version, links, thanks, licences, map data.', {
  tab: 'more',
  body: topSub('About', '<div style="width:48px"></div>') + `<div class="hero" style="padding:10px 0 20px">${mark(96)}<div class="t">MyPlay</div><div class="s">Version 2.3.1</div></div>` +
    cards(row({ ic: 'sparkles', label: "What's new" }), row({ ic: 'globe', label: 'Project page', ext: true }), row({ ic: 'circle-alert', label: 'Report a problem', ext: true }), row({ ic: 'heart', label: 'Thanks' }), row({ ic: 'file-text', label: 'Open-source licences' }), row({ ic: 'map', label: 'Map data', value: 'OpenStreetMap' })),
});

// ===================== Search =====================
add('search', 'Search — results', 'One search icon per tab. Results name their tab; a setting under an off switch says “Turn on X first”.', {
  tab: 'car',
  body: `<div class="top sub"><div class="iconbtn" aria-label="Back">${I('arrow-left', 22)}</div><div class="field focus" style="flex:1;margin:0 12px 0 0;height:48px">reply<span style="width:2px;height:22px;background:var(--int);margin-left:2px"></span></div></div>` +
    sec('Results', '', true) + cards(row({ ic: 'file-text', label: 'Auto-reply', help: 'Car › Messages and calls' }), row({ ic: 'message-circle', label: 'Reply text', help: 'Car › Messages and calls', under: `<span class="tag mute" style="gap:4px">${I('info', 12)} Turn on Auto-reply first</span>` }), row({ ic: 'phone', label: 'Calls on phone speaker', help: 'Car › Messages and calls' })),
});

// ===================== Setup =====================
const setupTop = (n) => `<div style="padding:20px 24px 0;display:flex;gap:8px;align-items:center">${[1, 2, 3, 4].map((i) => `<i style="width:${i === n ? 28 : 10}px;height:10px;border-radius:5px;background:${i <= n ? 'var(--int)' : '#2A3540'}"></i>`).join('')}<span style="margin-left:auto;font-size:14px;color:var(--text2)">${n} of 4</span></div>`;
const setupBottom = (b, skip = 'Skip for now') => `<div style="position:absolute;left:16px;right:16px;bottom:16px">${b}<div style="display:flex;justify-content:center;margin-top:8px">${btn(skip, 'text')}</div></div>`;
add('setup-1', 'Setup 1 of 4 — what connecting needs', 'First run, no tab bar. Only nearby devices, location and notifications; the rest is asked in context later.', {
  tab: 'home', tabs: false,
  body: setupTop(1) + `<div style="padding-top:72px">${hero(ring(150, { w: 4, glow: true, halos: [{ r: 92, o: 0.14 }, { r: 110, o: 0.06 }], inner: mark(68) }), 'Find your car', 'MyPlay needs three things to connect')}</div>` + gap(22) +
    cards(row({ ic: 'bluetooth', label: 'Nearby devices', help: 'To find the car', right: tag('Allowed', 'ok'), nochev: true }), row({ ic: 'map-pin', label: 'Location', help: "To join the car's Wi-Fi", nochev: true }), row({ ic: 'bell', label: 'Notifications', help: 'Shows when you are connected', nochev: true })) +
    setupBottom(btn('Allow', 'primary wide')),
});
add('setup-2', 'Setup 2 of 4 — Sound', 'The method this phone can use now, offered as one action; the others stay on Car › Sound.', {
  tab: 'home', tabs: false,
  body: setupTop(2) + `<div style="padding-top:72px">${hero(ring(150, { w: 4, glow: true, inner: I('volume-2', 52, 1.8) }), 'Sound in the car', "Let the phone's sound reach the car")}</div>` + gap(22) +
    cards(row({ ic: 'smartphone', label: 'Screen-capture permission', help: 'Works on every phone, with limits', right: tag('Best here', 'acc'), nochev: true })) + note('Root or Shizuku work better: Car › Sound', 'info') +
    setupBottom(btn('Set up', 'primary wide')),
});
add('setup-3', 'Setup 3 of 4 — the update question', 'Asked once; the answer writes check_updates (the default stays off).', {
  tab: 'home', tabs: false,
  body: setupTop(3) + `<div style="padding-top:72px">${hero(ring(150, { w: 4, glow: true, inner: I('download', 52, 1.8) }), 'Check for updates?', 'Once a day, from GitHub')}</div>` +
    `<div style="position:absolute;left:16px;right:16px;bottom:16px" class="btns">${btn('No', 'ghost')}${btn('Yes', 'primary')}</div>`,
});
add('setup-4', 'Setup 4 of 4 — connect', 'USB or wireless, one line each; Done goes to Home.', {
  tab: 'home', tabs: false,
  body: setupTop(4) + `<div style="padding-top:72px">${hero(ring(150, { w: 4, glow: true, inner: carGlyph(56, false) }), 'Connect to the car', 'Either way works')}</div>` + gap(22) +
    cards(row({ ic: 'cable', label: 'USB', help: 'Plug the phone into the car', nochev: true }), row({ ic: 'wifi', label: 'Wireless', help: 'Start the car; the phone joins by itself', nochev: true })) +
    setupBottom(btn('Done', 'primary wide'), 'Back'),
});

S.controls = controls;
module.exports = S;
