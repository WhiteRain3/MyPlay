// The Waze screens: three looks (options) and every key screen in the recommended look (A).
// Each screen says which template it draws and which of the template's fields it uses, for
// index.md. All names, places, times and labels are sample data.
const P = require('./parts.cjs');
const { NIGHT, DAY, ic, map, clock, hub, song, turn, disc, header, row, more, hint, spinner, card, primary, button } = P;

// ---------------------------------------------------------------- sample data

const PLACES = [
  { icon: 'house', title: 'Home', texts: ['Birch Lane 4, Northfield'], end: '12 km' },
  { icon: 'briefcase', title: 'Work', texts: ['Harbour Street 21'], end: '18 km' },
  { icon: 'history', title: 'Riverside Sports Hall', texts: ['Mill Road 9, Eastbrook'], end: '6.4 km' },
  { icon: 'history', title: 'Lindenhof Bakery', texts: ['Market Square 2'], end: '2.1 km' },
  { icon: 'history', title: 'Dr. Patel Dental Care', texts: ['Station Road 140'], end: '9.8 km' },
  { icon: 'history', title: 'Grandma', texts: ['Orchard Close 17, Westby'], end: '41 km' },
  { icon: 'history', title: 'Garden Centre', texts: ['Ring Road 3'], end: '15 km' },
];
const RESULTS = [
  { icon: 'coffee', title: 'Corner Roasters', texts: ['Market Square 8'], end: '1.2 km' },
  { icon: 'coffee', title: 'The Little Bean', texts: ['Canal Walk 31'], end: '2.6 km' },
  { icon: 'coffee', title: 'Mill Café', texts: ['Mill Road 1, Eastbrook'], end: '6.0 km' },
  { icon: 'coffee', title: 'Station Kiosk', texts: ['Central Station'], end: '7.3 km' },
  { icon: 'coffee', title: 'Hilltop Coffee', texts: ['Summit Way 12'], end: '11 km' },
  { icon: 'coffee', title: 'Coffee & Co.', texts: ['Ring Road 88'], end: '14 km' },
];
const ROUTES = [
  { title: '24 min', texts: ['via A9 · 18 km'] },
  { title: '27 min', texts: ['via Ring Road · 16 km'] },
  { title: '31 min', texts: ['via B14 · 21 km, toll'] },
];
// route preview map (option A: the map is visible right of the column, x 708-992)
const RP = {
  routes: [
    { d: 'M760 610 C 700 480 720 300 800 240 S 900 180 930 170' },
    { d: 'M760 610 C 900 560 990 420 960 300 S 940 200 930 170' },
    { d: 'M760 610 C 760 480 820 420 850 330 S 930 220 930 170', main: true },
  ],
  car: { x: 760, y: 610, a: 20 }, pin: { x: 930, y: 170 },
};
/** Option A's narrow column: the distance leads the second line (it is in that text already, as a DistanceSpan). */
const inl = (p) => ({ ...p, texts: [`${p.end} · ${p.texts[0]}`], end: null });
const NAV = {
  routes: [{ d: 'M640 470 L640 330 C640 290 660 278 700 278 L 1300 262', main: true }],
  car: { x: 640, y: 470 },
};

// The Waze navigation strips (sample icons: Waze sends its own).
const STRIP = [{ icon: 'triangle-alert' }, { icon: 'search' }, { icon: 'volume-2' }];
const MAPSTRIP = [{ icon: 'move' }, { icon: 'plus' }, { icon: 'minus' }];

// ---------------------------------------------------------------- frames

/** ORBIT's home corners while Waze shows a screen: clock, hub, song (turn only while navigating). */
const corners = (t, o = {}) => clock(t) + hub(t, o.hub || {}) + song(t);

/** Option A's column: a glass card on the driver's side of the free area, map to its right. */
const COL = { x: 288, y: 43, w: 420, h: 634 };
const column = (t, inner) => card(t, COL.x, COL.y, COL.w, COL.h, inner);

/** The map action strip (Waze's zoom / pan): small discs down the right edge of the free area. */
const mapStrip = (t, { focusedIndex = -1, pan = false, actions = MAPSTRIP } = {}) =>
  `<div style="position:absolute;left:928px;top:${677 - actions.length * 64 - (actions.length - 1) * 14}px;display:flex;flex-direction:column;gap:14px">${actions.map((a, i) => disc(t, { ...a, size: 64, focused: i === focusedIndex, on: pan && a.icon === 'move' })).join('')}</div>`;

/** The action strip (Waze's top actions): a row of discs along the top of the free area, right. */
const actionStrip = (t, { focusedIndex = -1, actions = STRIP } = {}) =>
  `<div style="position:absolute;right:${1280 - 992}px;top:43px;display:flex;gap:14px">${actions.map((a, i) => disc(t, { ...a, size: 72, focused: i === focusedIndex })).join('')}</div>`;

const SCREENS = [];
const add = (s) => SCREENS.push(s);

// ================================================================= the three looks

// ---- A: Side column (recommended)
add({
  slug: 'option-a-column-list', group: 'option', title: 'Option A · Side column — destinations',
  template: 'MapWithContentTemplate › ListTemplate', look: 'A',
  desc: 'A glass column on the driver’s side of the free area (x 288–708); Waze’s map stays visible to its right. Rows are ORBIT pills, the focused row carries the focus ring. The first recommendation; not chosen.',
  html: (t) => map(t, { car: { x: 850, y: 420 }, seed: 3 }) + corners(t) + column(t,
    header(t, { back: false, appIcon: true, title: 'Where to?', strip: [{ icon: 'search', size: 64 }] }) +
    PLACES.slice(0, 5).map((p, i) => row(t, { ...inl(p), focused: i === 0 })).join('') + more(t, 2)) + mapStrip(t),
});
add({
  slug: 'option-a-column-route', group: 'option', title: 'Option A · Side column — route preview',
  template: 'RoutePreviewNavigationTemplate', look: 'A',
  desc: 'Routes as radio rows; “Go now” is a full-width accent pill at the column’s foot and holds the focus when the screen opens. The routes stay visible on the map beside the column.',
  html: (t) => map(t, RP) + corners(t) + column(t,
    header(t, { title: 'Work' }) +
    ROUTES.map((r, i) => row(t, { ...r, radio: i === 0, h: 92 })).join('') +
    '<div style="flex:1"></div>' + primary(t, 'Go now') +
    `<div style="display:flex;justify-content:center;margin-top:12px">${hint(t, 'Turn for another route')}</div>`) + mapStrip(t),
});

// ---- B: Centre stage
const stage = (t, inner) => card(t, P.FREE.x, P.FREE.y, P.FREE.w, P.FREE.h, inner, 'padding:24px 30px');
add({
  slug: 'option-b-stage-list', group: 'option', title: 'Option B · Centre stage — destinations',
  template: 'MapWithContentTemplate › ListTemplate', look: 'B',
  desc: 'One wide glass panel fills the whole free area. The biggest type and the longest names, but the map is covered while a list is up.',
  html: (t) => map(t, { car: { x: 850, y: 420 }, seed: 3 }) + corners(t) + stage(t,
    header(t, { back: false, appIcon: true, title: 'Where to?', strip: [{ icon: 'search', size: 64 }] }) +
    PLACES.slice(0, 5).map((p, i) => row(t, { ...p, focused: i === 0, big: true, h: 92 })).join('') + more(t, 2)),
});
add({
  slug: 'option-b-stage-route', group: 'option', title: 'Option B · Centre stage — route preview',
  template: 'RoutePreviewNavigationTemplate', look: 'B',
  desc: 'Each route is an ORBIT ring with its time inside; the chosen one is lit. “Go now” sits under them, focused. Clear, but the routes on the map are hidden behind the panel.',
  html: (t) => map(t, { ...RP, dim: 0.2 }) + corners(t) + stage(t,
    header(t, { title: 'Work' }) +
    `<div style="display:flex;justify-content:space-around;margin-top:18px">${ROUTES.map((r, i) => `<div style="display:flex;flex-direction:column;align-items:center;gap:12px;width:200px">
      <div style="width:176px;height:176px;box-sizing:border-box;border-radius:50%;border:${i === 0 ? 9 : 5}px solid ${i === 0 ? t.accent : t.track};background:${i === 0 ? t.rowFocus : 'transparent'};display:flex;flex-direction:column;align-items:center;justify-content:center">
        <div style="font-size:44px;font-weight:800;line-height:44px">${r.title.split(' ')[0]}</div><div style="font-size:22px;color:${t.secondary}">min</div></div>
      <div style="font-size:21px;color:${i === 0 ? t.text : t.secondary};text-align:center">${r.texts[0]}</div></div>`).join('')}</div>` +
    '<div style="flex:1"></div>' + `<div style="display:flex;justify-content:center">${primary(t, 'Go now', { w: 420 })}</div>` +
    `<div style="display:flex;justify-content:center;margin-top:12px">${hint(t, 'Turn for another route')}</div>`),
});
// ---- C: Orbit wheel (chosen)
/**
 * The orbit wheel: rows on an arc round a centre far to the left, the focused row in the middle and
 * biggest, one row either side. A thin arc left of the rows is the list's position (the accent dot
 * is where the focus is in the whole list); "N more" above and below say how far it goes on.
 * opts.ring false: the middle row is lit but the focus ring is elsewhere (e.g. on "Go now").
 */
function wheel(t, items, { focusedIndex = 0, render, cy = 370, w = 560, ring = true }) {
  const cx = -322, R = 640, step = 17;
  let s = '';
  const n = items.length;
  if (n > 1) {
    const span = Math.min(34, (n - 1) * 8);
    const a0 = -span / 2, a1 = span / 2, rr = R - 18;
    const pt = (deg) => [cx + rr * Math.cos(deg * Math.PI / 180), cy + rr * Math.sin(deg * Math.PI / 180)];
    const [x0, y0] = pt(a0), [x1, y1] = pt(a1), [dx, dy] = pt(a0 + (a1 - a0) * focusedIndex / (n - 1));
    s += `<svg width="1280" height="720" style="position:absolute;left:0;top:0"><path d="M${x0} ${y0} A ${rr} ${rr} 0 0 1 ${x1} ${y1}" fill="none" stroke="${t.secondary}" stroke-opacity="0.45" stroke-width="5" stroke-linecap="round"/><circle cx="${dx}" cy="${dy}" r="8" fill="${t.accent}" stroke="${t.halo}" stroke-width="3"/></svg>`;
  }
  items.forEach((it, i) => {
    const k = i - focusedIndex;
    if (Math.abs(k) > 1) return;
    const a = k * step * Math.PI / 180;
    const x = cx + R * Math.cos(a), y = cy + R * Math.sin(a);
    const f = k === 0;
    const ww = f ? w : w - 90, h = f ? 112 : 84;
    s += `<div style="position:absolute;left:${x}px;top:${y - h / 2}px;width:${ww}px;opacity:${f ? 1 : 0.72}"><div style="background:${t.glass};border-radius:${h / 2}px">${render(it, f && ring, h, f)}</div></div>`;
  });
  const above = focusedIndex - 1, below = n - focusedIndex - 2;
  const cnt = (txt, icon, y) => `<div style="position:absolute;left:330px;top:${y}px;display:flex;align-items:center;gap:6px;font-size:20px;${P.floatText(t, t.secondary)}">${ic(icon, 20, t.secondary)}${txt}</div>`;
  if (above > 0) s += cnt(`${above} more`, 'arrow-up', cy - 187 - 42 - 34);
  if (below > 0) s += cnt(`${below} more`, 'chevron-down', cy + 187 + 42 + 8);
  return s;
}
/** A wheel screen's title, floating at the top left of the free area; [back] adds Back; [end] discs on the right. */
const title = (t, text, { back = false, appIcon = false, end = '', focusedBack = false } = {}) =>
  `<div style="position:absolute;left:300px;top:46px;display:flex;align-items:center;gap:16px">${back ? disc(t, { icon: 'arrow-left', size: 64, focused: focusedBack }) : ''}${appIcon ? `<div style="width:56px;height:56px;border-radius:50%;background:${t.glass};border:2px solid ${t.edge};display:flex;align-items:center;justify-content:center">${appIcon}</div>` : ''}<div style="font-size:34px;font-weight:800;${P.floatText(t, t.text)}">${text}</div></div>` +
  (end ? `<div style="position:absolute;right:${1280 - 992}px;top:46px;display:flex;gap:14px">${end}</div>` : '');
const dimMap = (t, o = {}) => map(t, { car: { x: 850, y: 420 }, seed: 3, dim: 0.4, ...o });
/** "Go now": the big accent disc right of the wheel. */
const goDisc = (t, focused = true) => `<div style="position:absolute;left:800px;top:${370 - 95}px;width:190px;height:190px;border-radius:50%;background:${t.accent};color:${t.onAccent};display:flex;flex-direction:column;align-items:center;justify-content:center;gap:6px;${focused ? P.focus(t) : ''}">${ic('navigation', 52, t.onAccent, 2.6)}<div style="font-size:30px;font-weight:800">Go now</div></div>`;
const routeRow = (t) => (r, ring, h, mid) => row(t, { ...r, radio: r === ROUTES[0], h, big: mid, focused: ring });
const placeRow = (t) => (p, ring, h, mid) => row(t, { ...p, focused: ring, h, big: mid });
const pinIcon = (t) => ic('map-pin', 28, t.accent);
const bottomHint = (t, text) => `<div style="position:absolute;left:288px;width:704px;top:640px;display:flex;justify-content:center">${hint(t, text)}</div>`;

add({
  slug: 'option-c-wheel-list', group: 'option', title: 'Option C · Orbit wheel — destinations (chosen)',
  template: 'MapWithContentTemplate › ListTemplate', look: 'C',
  desc: 'The list turns like the dial: the focused place is always in the middle and biggest, one place either side, a position arc on the left. Chosen by the owner.',
  html: (t) => dimMap(t) + corners(t) + title(t, 'Where to?', { appIcon: pinIcon(t), end: disc(t, { icon: 'search', size: 64 }) }) +
    wheel(t, PLACES, { focusedIndex: 2, render: placeRow(t) }),
});
add({
  slug: 'option-c-wheel-route', group: 'option', title: 'Option C · Orbit wheel — route preview (chosen)',
  template: 'RoutePreviewNavigationTemplate', look: 'C',
  desc: 'Routes on the arc; “Go now” is a big accent disc to their right and holds the focus. Chosen by the owner.',
  html: (t) => dimMap(t, { ...RP, car: RP.car }) + corners(t) + title(t, 'Work', { back: true }) +
    wheel(t, ROUTES, { focusedIndex: 0, render: routeRow(t), w: 460, ring: false }) + goDisc(t),
});

// ================================================================= look C, every key screen

add({
  slug: 'destinations', title: 'Destinations (Waze’s first screen)', group: 'C',
  template: 'MapWithContentTemplate › ListTemplate (or PlaceListNavigationTemplate)',
  fields: 'header: Action.APP_ICON + title · header actionStrip (search) · itemList rows: image, title, texts (address, DistanceSpan), browsable · onItemVisibilityChangedDelegate',
  desc: 'Opens on the first place, in the middle of the wheel. Turning moves the next place into the middle; the arc on the left shows where in the list the focus is. Search sits top right of the free area (the d-pad’s up reaches it).',
  html: (t) => dimMap(t) + corners(t) + title(t, 'Where to?', { appIcon: pinIcon(t), end: disc(t, { icon: 'search', size: 64 }) }) +
    wheel(t, PLACES, { focusedIndex: 0, render: placeRow(t) }),
});
add({
  slug: 'destinations-turned', title: 'Destinations, turned further', group: 'C',
  template: 'MapWithContentTemplate › ListTemplate',
  fields: 'itemList (as above)',
  desc: 'Four turns on: the dot has moved down the arc, and “3 more” above / “2 more” below say how far the list goes either way.',
  html: (t) => dimMap(t) + corners(t) + title(t, 'Where to?', { appIcon: pinIcon(t), end: disc(t, { icon: 'search', size: 64 }) }) +
    wheel(t, PLACES, { focusedIndex: 4, render: placeRow(t) }),
});
add({
  slug: 'search-driving', title: 'Search while driving', group: 'C',
  template: 'SearchTemplate',
  fields: 'headerAction (BACK) · searchHint · initialSearchText · itemList (rows: image, title, texts, DistanceSpan) · isLoading · showKeyboardByDefault (refused while driving)',
  desc: 'Typing is refused while driving, so the search text Waze sent sits in a locked field where the title goes, and the focus is on the first result in the wheel.',
  html: (t) => dimMap(t, { seed: 5 }) + corners(t) +
    `<div style="position:absolute;left:300px;top:46px;display:flex;align-items:center;gap:14px">${disc(t, { icon: 'arrow-left', size: 64 })}
      <div style="width:420px;height:64px;box-sizing:border-box;border-radius:32px;background:${t.glass};border:2px solid ${t.edge};display:flex;align-items:center;gap:12px;padding:0 20px">${ic('search', 28, t.secondary)}<div style="flex:1;font-size:28px;font-weight:700">coffee</div>${ic('lock', 24, t.secondary)}</div></div>
     <div style="position:absolute;left:382px;top:118px;font-size:20px;${P.floatText(t, t.secondary)}">Typing works when you’re parked</div>` +
    wheel(t, RESULTS, { focusedIndex: 0, render: placeRow(t), cy: 400 }),
});
add({
  slug: 'search-parked-keyboard', title: 'Search while parked: the letter ring', group: 'C',
  template: 'SearchTemplate (our keyboard)',
  fields: 'searchHint · the text we own (sendSearchTextChanged on each letter, sendSearchSubmitted on Search) · itemList (its size, as you type)',
  desc: 'Parked, the knob types on a ring of letters that fills the free area: turn to a letter, push to type it. Space, delete and Search close the ring. The text and the number of places found so far sit in the middle; Search (or the d-pad down) opens the results wheel.',
  html: (t) => {
    const letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('');
    const keys = [...letters, { i: 'space' }, { i: 'delete' }, { i: 'search', s: 1 }];
    const n = keys.length;
    const cx = 640, cy = 360, R = 252;
    let ring = `<div style="position:absolute;left:${cx - 310}px;top:${cy - 310}px;width:620px;height:620px;box-sizing:border-box;border-radius:50%;background:${t.glass};border:2px solid ${t.edge}"></div>`;
    ring += `<div style="position:absolute;left:${cx - 200}px;top:${cy - 200}px;width:400px;height:400px;border-radius:50%;border:2px solid ${t.edge}"></div>`;
    keys.forEach((L, i) => {
      const a = (-90 + i * 360 / n) * Math.PI / 180;
      const x = cx + R * Math.cos(a), y = cy + R * Math.sin(a);
      const f = L === 'F';
      const sz = f ? 62 : 48;
      const inner = typeof L === 'string' ? `<span style="font-size:${f ? 36 : 28}px;font-weight:800">${L}</span>` : ic(L.i, 26, L.s ? t.onAccent : t.text);
      ring += `<div style="position:absolute;left:${x - sz / 2}px;top:${y - sz / 2}px;width:${sz}px;height:${sz}px;border-radius:50%;display:flex;align-items:center;justify-content:center;color:${f ? t.onAccent : t.text};background:${f || L.s ? t.accent : typeof L === 'string' ? 'transparent' : t.track};${f ? P.focus(t) : ''}">${inner}</div>`;
    });
    ring += `<div style="position:absolute;left:${cx - 170}px;top:${cy - 150}px;width:340px;display:flex;flex-direction:column;align-items:center;gap:14px">
      <div style="width:300px;height:60px;box-sizing:border-box;border-radius:30px;background:${t.row};border:2px solid ${t.accent};display:flex;align-items:center;gap:10px;padding:0 18px">${ic('search', 26, t.secondary)}<div style="font-size:30px;font-weight:700">cof<span style="color:${t.accent}">|</span></div></div>
      <div style="font-size:96px;font-weight:800;line-height:96px;color:${t.accent}">F</div>
      ${hint(t, 'Push to type')}
      <div style="font-size:21px;color:${t.secondary};display:flex;align-items:center;gap:6px">${ic('chevron-down', 22, t.secondary)}6 places so far</div></div>`;
    return map(t, { car: { x: 850, y: 420 }, seed: 5, dim: 0.3 }) + corners(t) + ring;
  },
});
add({
  slug: 'search-no-results', title: 'Search with no results', group: 'C',
  template: 'SearchTemplate',
  fields: 'itemList empty → itemList.noItemsMessage (else our “No results”)',
  desc: 'Where the wheel would be, one glass disc with Waze’s message for an empty list. The focus falls back to Back, so one push leaves.',
  html: (t) => dimMap(t, { seed: 5 }) + corners(t) +
    `<div style="position:absolute;left:300px;top:46px;display:flex;align-items:center;gap:14px">${disc(t, { icon: 'arrow-left', size: 64, focused: true })}
      <div style="width:420px;height:64px;box-sizing:border-box;border-radius:32px;background:${t.glass};border:2px solid ${t.edge};display:flex;align-items:center;gap:12px;padding:0 20px">${ic('search', 28, t.secondary)}<div style="flex:1;font-size:28px;font-weight:700">zqx bakery</div>${ic('lock', 24, t.secondary)}</div></div>` +
    `<div style="position:absolute;left:${640 - 180}px;top:${400 - 180}px;width:360px;height:360px;border-radius:50%;background:${t.glass};border:2px solid ${t.edge};display:flex;flex-direction:column;align-items:center;justify-content:center;gap:18px;text-align:center">
      ${ic('map-pin-off', 64, t.secondary)}<div style="font-size:30px;font-weight:800;max-width:260px;line-height:1.25">No results for “zqx bakery”</div></div>`,
});
add({
  slug: 'route-preview', title: 'Route preview', group: 'C',
  template: 'RoutePreviewNavigationTemplate',
  fields: 'header / title + headerAction (BACK) · itemList rows (title = time, texts = via, distance) with onSelectedDelegate + selectedIndex · navigateAction (FLAG_PRIMARY) · panModeDelegate (not shown: no map buttons)',
  desc: '“Go now” holds the focus when the screen opens: one push starts the drive. The chosen route is lit in the middle of the wheel; turning the knob back moves the ring onto the routes. Waze draws the routes in the map’s free middle.',
  html: (t) => map(t, { ...RP, dim: 0.25 }) + corners(t) + title(t, 'Work', { back: true }) +
    wheel(t, ROUTES, { focusedIndex: 0, render: routeRow(t), w: 460, ring: false }) + goDisc(t) + bottomHint(t, 'Turn for another route'),
});
add({
  slug: 'route-preview-choosing', title: 'Route preview, turning to another route', group: 'C',
  template: 'RoutePreviewNavigationTemplate',
  fields: 'itemList (onSelectedDelegate.sendSelected on push) · navigateAction',
  desc: 'The second route turned into the middle with the focus ring; the radio dot still on the chosen one. A push picks it, Waze redraws, and the focus jumps back to “Go now”.',
  html: (t) => map(t, { ...RP, dim: 0.25 }) + corners(t) + title(t, 'Work', { back: true }) +
    wheel(t, ROUTES, { focusedIndex: 1, render: routeRow(t), w: 460 }) + goDisc(t, false) + bottomHint(t, 'Push to choose this route'),
});
add({
  slug: 'route-preview-loading', title: 'Route preview, loading', group: 'C',
  template: 'RoutePreviewNavigationTemplate',
  fields: 'isLoading · header / title · navigateAction (not drawn until routes come)',
  desc: 'Only ORBIT’s spinning ring in the wheel’s middle: Waze sends no words while loading, so we add none. Back is focused.',
  html: (t) => map(t, { car: RP.car, pin: RP.pin, dim: 0.25 }) + corners(t) + title(t, 'Work', { back: true, focusedBack: true }) +
    `<div style="position:absolute;left:${640 - 60}px;top:${370 - 60}px">${spinner(t, 120, 9)}</div>`,
});

/** C navigation: only the turn with its lanes, the hub (arrival), the speed, the clock and the song. */
const speedDisc = (t, v = '48') => `<div style="position:absolute;left:76px;top:340px;width:120px;height:120px;border-radius:50%;background:${t.glass};border:2px solid ${t.edge};display:flex;flex-direction:column;align-items:center;justify-content:center"><div style="font-size:46px;font-weight:800;line-height:46px">${v}</div><div style="font-size:18px;color:${t.secondary}">km/h</div></div>`;
const TRIP = { eta: '14:32', left: '24 min', km: '18 km', p: 0.18 };
const LANES = [{ dir: 'u' }, { dir: 'u' }, { dir: 'r', on: true }, { dir: 'r', on: true }];
const navBase = (t, o = {}) => map(t, { ...NAV, ...(o.map || {}) }) + (o.noTurn ? '' : turn(t, { lanes: LANES, ...(o.turn || {}) })) + clock(t) + hub(t, { trip: o.trip || TRIP }) + speedDisc(t) + song(t);

add({
  slug: 'navigating', title: 'Navigating: the turn, lanes, arrival, speed — nothing else', group: 'C',
  template: 'NavigationTemplate',
  fields: 'navigationInfo (RoutingInfo: current step, maneuver, distance, lanes → ORBIT’s turn disc and lane pill) · destinationTravelEstimate (→ the hub: arrival, time and distance left, remainingTimeColor) · actionStrip and mapActionStrip not drawn: they are in the actions wheel',
  desc: 'The owner’s pick: no map buttons and no action strip on screen. The turn with its lanes (the lanes to take in Navigate’s green), the arrival in the hub’s trip ring, and the speed. Back or the pad’s centre opens Waze’s actions.',
  html: (t) => navBase(t),
});
add({
  slug: 'navigating-actions', title: 'Waze’s actions, on one press', group: 'C',
  template: 'NavigationTemplate (actionStrip + mapActionStrip, in a wheel)',
  fields: 'actionStrip actions then mapActionStrip actions: icon · title (when Waze sends one) · Action.PAN (→ “Move map”, ours) · each push sends its onClickDelegate',
  desc: 'Back or the pad’s centre while navigating opens Waze’s action strip and map buttons as a wheel: the same arc as the lists, the first action in the middle and focused. A push runs it and closes the wheel; Back closes it. Labels are the actions’ titles; an action with only an icon shows its icon alone.',
  html: (t) => navBase(t, { map: { dim: 0.45 } }) +
    wheel(t, [{ icon: 'triangle-alert', title: 'Report' }, { icon: 'volume-2', title: 'Sound' }, { icon: 'search', title: 'Search' }, { icon: 'x', title: 'End route' }, { icon: 'move', title: 'Move map' }, { icon: 'plus', title: '' }, { icon: 'minus', title: '' }],
      { focusedIndex: 0, w: 460, render: (a, ring, h, mid) => row(t, { icon: a.icon, title: a.title || ' ', h, big: mid, focused: ring }) }) +
    bottomHint(t, 'Push to run · Back to close'),
});
add({
  slug: 'navigating-pan', title: 'Moving the map (from the actions wheel)', group: 'C',
  template: 'NavigationTemplate',
  fields: 'Action.PAN from the wheel · panModeDelegate.sendPanModeChanged · SurfaceCallback onScroll / onScale',
  desc: 'Picking “Move map” in the wheel: the turn goes (as Android Auto), the d-pad moves the map, turning zooms, Back finishes. No buttons appear on the map.',
  html: (t) => map(t, { ...NAV, seed: 11 }) + clock(t) + hub(t, { trip: TRIP }) + song(t) +
    `<div style="position:absolute;left:640px;top:360px">${['▲', '▶', '▼', '◀'].map((a, i) => `<div style="position:absolute;font-size:28px;color:${t.text};text-shadow:0 0 8px ${t.halo};left:${[0, 70, 0, -70][i] - 10}px;top:${[-70, 0, 70, 0][i] - 18}px">${a}</div>`).join('')}<div style="width:18px;height:18px;border-radius:50%;border:3px solid ${t.text};transform:translate(-50%,-50%)"></div></div>` +
    `<div style="position:absolute;left:288px;top:43px;padding:12px 22px;border-radius:40px;background:${t.glass};border:2px solid ${t.edge};display:flex;gap:26px">${hint(t, 'Pad moves · turn zooms')}<div style="font-size:20px;color:${t.secondary}">Back to finish</div></div>`,
});
add({
  slug: 'navigating-message', title: 'A navigation message (arrived)', group: 'C',
  template: 'NavigationTemplate (navigationInfo = MessageInfo)',
  fields: 'MessageInfo: title, text, image · destinationTravelEstimate (if any)',
  desc: 'A MessageInfo takes the turn’s place: its image in the accent disc, its title and text floating beside it like the distance and road.',
  html: (t) => map(t, { car: { x: 640, y: 470 }, pin: { x: 690, y: 430 }, seed: 9 }) + P.plate(t, -40, -40, 640, 300) +
    `<div style="position:absolute;left:40px;top:40px;display:flex;align-items:center;gap:22px">
      <div style="width:132px;height:132px;border-radius:50%;background:${t.accent};display:flex;align-items:center;justify-content:center">${ic('flag', 66, t.onAccent, 2.6)}</div>
      <div><div style="font-size:46px;font-weight:800;${P.floatText(t, t.text)}">You have arrived</div><div style="font-size:26px;margin-top:6px;${P.floatText(t, t.secondary)}">Work · Harbour Street 21</div></div></div>` +
    clock(t) + hub(t, { trip: { eta: '14:32', left: 'Now', km: '0 m', p: 1 } }) + speedDisc(t, '0') + song(t),
});
add({
  slug: 'navigating-rerouting', title: 'A navigation message (loading)', group: 'C',
  template: 'NavigationTemplate (RoutingInfo.isLoading, or a MessageInfo with no image)',
  fields: 'RoutingInfo.isLoading · or MessageInfo title / text',
  desc: 'While Waze works out the way, the turn disc becomes ORBIT’s spinning ring; any words it sends float beside it.',
  html: (t) => map(t, { car: { x: 640, y: 470 }, seed: 9 }) + P.plate(t, -40, -40, 560, 300) +
    `<div style="position:absolute;left:40px;top:40px;display:flex;align-items:center;gap:22px">
      <div style="width:132px;height:132px;border-radius:50%;background:${t.glass};border:2px solid ${t.edge};display:flex;align-items:center;justify-content:center">${spinner(t, 92, 8)}</div>
      <div style="font-size:40px;font-weight:800;${P.floatText(t, t.text)}">Recalculating…</div></div>` +
    clock(t) + hub(t, { trip: { eta: '14:35', left: '27 min', km: '19 km', p: 0.18 } }) + speedDisc(t) + song(t),
});
add({
  slug: 'alert', title: 'An alert while driving', group: 'C',
  template: 'Alert (AppManager.showAlert)',
  fields: 'title · subtitle · icon · actions (first two) · durationMillis (→ the ring round the icon) · callbackDelegate (timeout / cancel)',
  desc: 'ORBIT’s alert pill, between the hub and the song, clear of the turn and lanes. The alert’s time left runs as a ring round its icon, so the driver sees it will go by itself. The first action is focused; Back dismisses.',
  html: (t) => navBase(t) +
    `<div style="position:absolute;left:300px;top:556px;width:620px;box-sizing:border-box;border-radius:60px;background:${t.glass};border:2px solid ${t.edge};padding:14px 18px 14px 14px;display:flex;align-items:center;gap:16px">
      <div style="position:relative;width:84px;height:84px;flex:none"><svg width="84" height="84" style="position:absolute"><circle cx="42" cy="42" r="38" fill="none" stroke="${t.track}" stroke-width="6"/><circle cx="42" cy="42" r="38" fill="none" stroke="${t.caution}" stroke-width="6" stroke-linecap="round" stroke-dasharray="${2 * Math.PI * 38 * 0.62} 999" transform="rotate(-90 42 42)"/></svg>
        <div style="position:absolute;left:12px;top:12px;width:60px;height:60px;border-radius:50%;background:${t.caution}38;display:flex;align-items:center;justify-content:center">${ic('route', 32, t.caution)}</div></div>
      <div style="flex:1;min-width:0"><div style="font-size:26px;font-weight:800;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">Faster route found</div><div style="font-size:20px;color:${t.secondary};white-space:nowrap">Saves 6 min via B14</div></div>
      ${button(t, 'Take it', { focused: true, filled: true, h: 60 })}${button(t, 'No', { h: 60 })}</div>`,
});
add({
  slug: 'message', title: 'A message screen', group: 'C',
  template: 'MessageTemplate (alone, or in MapWithContentTemplate)',
  fields: 'header / title + headerAction · icon · message · isLoading · actions (up to two; FLAG_PRIMARY filled) · debugMessage (never shown in the car)',
  desc: 'A message is not a list, so no wheel: its icon in a ring, the words, and its buttons, the primary one focused. Back is the title’s.',
  html: (t) => dimMap(t, { seed: 13 }) + corners(t) + title(t, 'Waze', { back: true }) +
    `<div style="position:absolute;left:288px;top:130px;width:704px;display:flex;flex-direction:column;align-items:center;gap:24px;text-align:center">
      <div style="width:150px;height:150px;border-radius:50%;background:${t.glass};border:6px solid ${t.caution};box-sizing:border-box;display:flex;align-items:center;justify-content:center">${ic('locate-fixed', 70, t.caution)}</div>
      <div style="font-size:34px;font-weight:800;line-height:1.25;max-width:560px;${P.floatText(t, t.text)}">Location is off on your phone. Turn it on to navigate.</div>
      <div style="display:flex;gap:24px;margin-top:10px">${button(t, 'Try again', { focused: true, filled: true, h: 76 })}${button(t, 'Not now', { h: 76 })}</div></div>`,
});
add({
  slug: 'message-loading', title: 'A message screen, loading', group: 'C',
  template: 'MessageTemplate (isLoading)',
  fields: 'isLoading · message (if any)',
  desc: 'ORBIT’s ring spinner in place of the icon; no buttons, so Back is focused.',
  html: (t) => dimMap(t, { seed: 13 }) + corners(t) + title(t, 'Waze', { back: true, focusedBack: true }) +
    `<div style="position:absolute;left:288px;top:220px;width:704px;display:flex;flex-direction:column;align-items:center;gap:26px">${spinner(t, 130, 10)}<div style="font-size:32px;font-weight:800;${P.floatText(t, t.text)}">Loading your places…</div></div>`,
});
add({
  slug: 'route-preview-day', title: 'Route preview by day', group: 'C', day: true,
  template: 'RoutePreviewNavigationTemplate', fields: 'as Route preview',
  desc: 'ORBIT’s day glass and darkened accent (each at least 4.9:1); the same wheel.',
  html: (t) => map(t, { ...RP, dim: 0.15 }) + corners(t) + title(t, 'Work', { back: true }) +
    wheel(t, ROUTES, { focusedIndex: 0, render: routeRow(t), w: 460, ring: false }) + goDisc(t) + bottomHint(t, 'Turn for another route'),
});
add({
  slug: 'navigating-day', title: 'Navigating by day', group: 'C', day: true,
  template: 'NavigationTemplate', fields: 'as Navigating',
  desc: 'The day glass over a day map; still only the turn, lanes, arrival and speed.',
  html: (t) => navBase(t),
});

// ================================================================= MyPlay's own worlds, in the same look

const homeMap = (t, o = {}) => map(t, { car: { x: 640, y: 470 }, seed: 21, dim: 0.4, ...o });
function setting(t, { title: ti, value = null, sw = null, note = null }, ring, h, mid) {
  const end = sw === null ? `<div style="font-size:23px;font-weight:700;color:${ring ? t.accent : t.secondary};white-space:nowrap">${P.esc(value)}</div>${ic('chevron-right', 26, t.secondary)}`
    : `<div style="width:76px;height:42px;border-radius:21px;background:${sw ? t.accent : t.track};position:relative;flex:none"><div style="position:absolute;top:5px;left:${sw ? 39 : 5}px;width:32px;height:32px;border-radius:50%;background:${sw ? t.onAccent : t.secondary}"></div></div>`;
  return `<div style="min-height:${h}px;box-sizing:border-box;border-radius:${h / 2}px;background:${ring ? t.rowFocus : t.row};padding:8px 24px 8px 30px;display:flex;align-items:center;gap:16px;${ring ? P.focus(t) : ''}">
    <div style="flex:1;min-width:0"><div style="font-size:${mid ? 30 : 25}px;font-weight:800;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${P.esc(ti)}</div>${note && mid ? `<div style="font-size:20px;color:${t.secondary}">${P.esc(note)}</div>` : ''}</div>${end}</div>`;
}
const appIcon = (icon, colour, size = 96) => `<div style="width:${size}px;height:${size}px;border-radius:50%;background:${colour};display:flex;align-items:center;justify-content:center;flex:none">${ic(icon, Math.round(size * 0.48), '#fff', 2.4)}</div>`;
const APPS = [
  ['Waze', 'map-pin', '#2E8BC0'], ['Music', 'music', '#7C4DDB'], ['Podcasts', 'radio', '#C2185B'], ['Audiobooks', 'file-text', '#B7791F'],
  ['Messages', 'message-circle', '#17734A'], ['Phone', 'phone', '#1F9D63'], ['Phone screen', 'smartphone', '#3A5AA6'], ['Car data', 'gauge', '#8A5A00'],
];
const SETTINGS = [
  { title: 'Colors', value: 'Car’s day/night' },
  { title: 'Speed on the car screen', value: 'Car, else phone' },
  { title: 'Speed colour over the limit', value: 'over by 5 km/h' },
  { title: 'Close panels by themselves', value: 'After 8 s' },
  { title: 'Shade behind ORBIT’s parts', value: 'Soft' },
  { title: 'Ice and phone warnings', sw: true },
  { title: 'Limit sign pulse', sw: false },
  { title: 'Message notifications', sw: true },
  { title: 'Read messages aloud', sw: false, note: 'Can be turned on only while the car is stopped' },
];
const worldIcon = (t, icon, colour) => ic(icon, 28, colour);

add({
  slug: 'settings', title: 'Settings world', group: 'more', section: 'Settings',
  template: 'MyPlay · SettingsWorld (SettingsPage)',
  fields: 'SettingRow: title · value (a choice’s label) · switch + on · note',
  desc: 'The settings on the same wheel: a choice shows its value and a chevron (a push steps it on), a switch shows ORBIT’s accent switch. Opens on the first setting.',
  html: (t) => homeMap(t) + corners(t) + title(t, 'Settings', { appIcon: worldIcon(t, 'settings', '#9DB4E8') }) +
    wheel(t, SETTINGS, { focusedIndex: 0, w: 600, render: (r, ring, h, mid) => setting(t, r, ring, h, mid) }),
});
add({
  slug: 'settings-switch', title: 'Settings world, on a switch with a note', group: 'more', section: 'Settings',
  template: 'MyPlay · SettingsWorld (SettingsPage)',
  fields: 'SettingRow: title · switch + on · note',
  desc: 'Turned to the end: the focus on a switch, a push flips it. The note (it can only be turned on while stopped) shows under the title in the middle row.',
  html: (t) => homeMap(t) + corners(t) + title(t, 'Settings', { appIcon: worldIcon(t, 'settings', '#9DB4E8') }) +
    wheel(t, SETTINGS, { focusedIndex: 8, w: 600, render: (r, ring, h, mid) => setting(t, r, ring, h, mid) }),
});
function notice(t, { icon, colour, title: ti, text, actions, ring }) {
  return `<div style="position:absolute;left:470px;top:43px;width:500px;box-sizing:border-box;border-radius:44px;background:${t.glass};border:2px solid ${t.edge};padding:16px 20px;display:flex;flex-direction:column;gap:14px">
    <div style="display:flex;align-items:center;gap:16px">${appIcon(icon, colour, 64)}<div style="flex:1;min-width:0"><div style="font-size:27px;font-weight:800;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${P.esc(ti)}</div><div style="font-size:21px;color:${t.secondary};white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${P.esc(text)}</div></div></div>
    <div style="display:flex;gap:16px;justify-content:flex-end">${actions.map((a, i) => button(t, a, { h: 60, focused: i === ring, filled: i === 0 && a !== 'Dismiss' })).join('')}</div></div>`;
}
add({
  slug: 'notice-message', title: 'A message banner while driving', group: 'more', section: 'Notices and banners',
  template: 'MyPlay · Notice (CarNotice, message = true)',
  fields: 'CarNotice: icon · title · text (hidden while driving for a message) · actions (labels) · asked = false → the ring on Dismiss',
  desc: 'The alert’s glass and buttons, at the top of the free area, 500 px wide so it clears the turn’s distance and the clock. Nobody asked for it, so the ring is on Dismiss: a stray push never acts. A message’s text is hidden while driving.',
  html: (t) => navBase(t) +
    notice(t, { icon: 'message-circle', colour: '#17734A', title: 'Sam Taylor', text: 'Message · hidden while driving', actions: ['Read aloud', 'Dismiss'], ring: 1 }),
});
add({
  slug: 'notice-asked', title: 'A banner the driver asked for', group: 'more', section: 'Notices and banners',
  template: 'MyPlay · Notice (CarNotice, asked = true)',
  fields: 'CarNotice: icon · title · text · actions · asked = true → the ring on the first action',
  desc: 'After “Call back” the driver expects to act: the ring is on the first button, so one push calls.',
  html: (t) => homeMap(t, { dim: 0 }) + corners(t) +
    notice(t, { icon: 'phone', colour: '#1F9D63', title: 'Call back Alex Morgan?', text: 'Missed call · 13:52', actions: ['Call', 'Dismiss'], ring: 0 }),
});
const NOTICES = [
  { ico: 'message-circle', col: '#17734A', title: 'Sam Taylor', texts: ['Message · hidden while driving'], end: '14:02', act: 'Read aloud' },
  { ico: 'phone', col: '#1F9D63', title: 'Alex Morgan', texts: ['Missed call'], end: '13:52', act: 'Call' },
  { ico: 'sun', col: '#E08A1E', title: 'Weather', texts: ['Rain from 16:00'], end: '12:30' },
  { clear: true, title: 'Clear all' },
];
add({
  slug: 'notifications', title: 'Notifications list', group: 'more', section: 'Notices and banners',
  template: 'MyPlay · NoticeList (NoticeListPage)',
  fields: 'NoticeRow: notice (icon, title) · text · time · action (the label a push runs) · a null notice = Clear all',
  desc: 'The notices on the wheel: the app’s icon, the title, the text, the time at the end and, when there is one, the action a push runs as a small pill. Clear all is the wheel’s last row.',
  html: (t) => homeMap(t) + corners(t) + title(t, 'Notifications', { appIcon: worldIcon(t, 'bell', t.accent) }) +
    wheel(t, NOTICES, { focusedIndex: 0, w: 600, render: (n, ring, h, mid) => n.clear ? row(t, { icon: 'x', title: n.title, h, focused: ring })
      : `<div style="min-height:${h}px;box-sizing:border-box;border-radius:${h / 2}px;background:${ring ? t.rowFocus : t.row};padding:10px 22px 10px 14px;display:flex;align-items:center;gap:16px;${ring ? P.focus(t) : ''}">${appIcon(n.ico, n.col, mid ? 72 : 56)}
        <div style="flex:1;min-width:0"><div style="font-size:${mid ? 30 : 25}px;font-weight:800">${n.title}</div><div style="font-size:21px;color:${t.secondary};white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${n.texts[0]}</div></div>
        ${n.act && mid ? `<div style="padding:8px 18px;border-radius:24px;background:${t.track};font-size:21px;font-weight:800;white-space:nowrap">${n.act}</div>` : ''}<div style="font-size:21px;color:${t.secondary}">${n.end}</div></div>` }),
});
const initials = (t, name, size, colour) => `<div style="width:${size}px;height:${size}px;border-radius:50%;background:${colour};display:flex;align-items:center;justify-content:center;font-size:${size * 0.36}px;font-weight:800;color:${t.onAccent}">${name.split(' ').map((w) => w[0]).join('')}</div>`;
const CALLS = '#5FD39A';
add({
  slug: 'call-ringing', title: 'A call ringing', group: 'more', section: 'Calls',
  template: 'MyPlay · Ringing (CarCall, ringing = true)',
  fields: 'CarCall: name · ringing · canEnd → Answer / Decline (CALL_ANSWER, CALL_DECLINE)',
  desc: 'Centred in the free area: the caller’s initials in Calls’ green ring, the name, and two big discs. Answer holds the focus; Decline is a turn away. While navigating, the turn, lanes and hub stay.',
  html: (t) => navBase(t, { map: { dim: 0.35 } }) + P.plate(t, 300, 40, 680, 640) +
    `<div style="position:absolute;left:288px;top:83px;width:704px;display:flex;flex-direction:column;align-items:center;gap:18px">
      <div style="width:200px;height:200px;border-radius:50%;border:7px solid ${CALLS};box-sizing:border-box;display:flex;align-items:center;justify-content:center;background:${t.glass}">${initials(t, 'Alex Morgan', 160, CALLS)}</div>
      <div style="font-size:46px;font-weight:800;${P.floatText(t, t.text)}">Alex Morgan</div><div style="font-size:26px;margin-top:-10px;${P.floatText(t, t.secondary)}">Incoming call</div>
      <div style="display:flex;gap:90px;margin-top:22px">
        <div style="display:flex;flex-direction:column;align-items:center;gap:12px"><div style="width:112px;height:112px;border-radius:50%;background:${CALLS};display:flex;align-items:center;justify-content:center;${P.focus({ ...t, accent: CALLS })}">${ic('phone', 52, t.onAccent, 2.6)}</div><div style="font-size:24px;font-weight:800;${P.floatText(t, t.text)}">Answer</div></div>
        <div style="display:flex;flex-direction:column;align-items:center;gap:12px"><div style="width:112px;height:112px;border-radius:50%;background:${t.warning};display:flex;align-items:center;justify-content:center;transform:rotate(135deg)">${ic('phone', 52, '#fff', 2.6)}</div><div style="font-size:24px;font-weight:800;${P.floatText(t, t.text)}">Decline</div></div></div></div>`,
});
add({
  slug: 'call-in-call', title: 'In a call', group: 'more', section: 'Calls',
  template: 'MyPlay · InCall (CarCall, ringing = false)',
  fields: 'CarCall: name · sinceMs (the time) · canEnd → the red hang-up disc',
  desc: 'The call shrinks to a pill at the bottom of the free area, like the alert, so the turn and lanes come back. The ring is not on it until the driver turns there; hang-up is red and one turn on.',
  html: (t) => navBase(t) +
    `<div style="position:absolute;left:400px;top:597px;display:flex;align-items:center;gap:18px">
      <div style="height:76px;border-radius:38px;background:${t.glass};border:2px solid ${t.edge};display:flex;align-items:center;gap:14px;padding:0 28px 0 8px">${initials(t, 'Alex Morgan', 60, CALLS)}<div style="font-size:26px;font-weight:800">Alex Morgan · 3:12</div></div>
      <div style="width:76px;height:76px;border-radius:50%;background:${t.warning};display:flex;align-items:center;justify-content:center;transform:rotate(135deg)">${ic('phone', 36, '#fff', 2.6)}</div></div>`,
});
const PHONEROWS = [['Alex Morgan', 'Missed · 13:52', 'missed'], ['Sam Taylor', 'Mobile · yesterday', 'history'], ['Home', 'Favourite', 'heart'], ['Dr. Patel Dental Care', 'Yesterday', 'history'], ['Phone', 'Keypad and all contacts', 'smartphone']];
add({
  slug: 'calls-world', title: 'Calls world', group: 'more', section: 'Calls',
  template: 'MyPlay · PhoneWorld (PhonePage)',
  fields: 'PhoneRow: title · subtitle · kind (RECENT, MISSED, FAVOURITE, PHONE_APP — last) · loading · permitted',
  desc: 'Recent calls, then favourites, then the phone app, on the wheel. A missed call is in the warning red; the first row is in the middle, so one push calls back.',
  html: (t) => homeMap(t) + corners(t) + title(t, 'Calls', { appIcon: worldIcon(t, 'phone', CALLS) }) +
    wheel(t, PHONEROWS, { focusedIndex: 0, render: ([ti, sub, k], ring, h, mid) => {
      const red = k === 'missed';
      return `<div style="min-height:${h}px;box-sizing:border-box;border-radius:${h / 2}px;background:${ring ? t.rowFocus : t.row};padding:10px 26px 10px 14px;display:flex;align-items:center;gap:16px;${ring ? P.focus(t) : ''}">
        <div style="width:${mid ? 64 : 56}px;height:${mid ? 64 : 56}px;border-radius:50%;background:${t.track};display:flex;align-items:center;justify-content:center">${ic(red ? 'phone' : k, 28, red ? t.warning : k === 'heart' ? CALLS : t.secondary)}</div>
        <div style="flex:1"><div style="font-size:${mid ? 32 : 26}px;font-weight:800;color:${red ? t.warning : t.text}">${ti}</div><div style="font-size:21px;color:${t.secondary}">${sub}</div></div>${k === 'smartphone' ? ic('chevron-right', 28, t.secondary) : ''}</div>`;
    } }),
});
add({
  slug: 'apps-ring', title: 'Apps: a ring of apps', group: 'more', section: 'Apps',
  template: 'MyPlay · AppsWorld (CarAppEntry list, page / pages)',
  fields: 'CarAppEntry: label · icon · kind · ringed tile · page / pages',
  desc: 'The grid becomes ORBIT’s dial: eight apps round a ring, the focused one bigger with the focus ring, its name in the middle. Turning goes round the ring; past the last app the next page turns in (the dots).',
  html: (t) => {
    const cx = 640, cy = 360, R = 220;
    let s = homeMap(t) + corners(t) + `<div style="position:absolute;left:${cx - 300}px;top:${cy - 300}px;width:600px;height:600px;border-radius:50%;background:${t.glass};border:2px solid ${t.edge}"></div>`;
    APPS.forEach(([l, i, c], k) => {
      const a = (-90 + k * 45) * Math.PI / 180, f = k === 0, sz = f ? 120 : 96;
      s += `<div style="position:absolute;left:${cx + R * Math.cos(a) - sz / 2}px;top:${cy + R * Math.sin(a) - sz / 2}px;border-radius:50%;${f ? P.focus(t) : 'opacity:0.85'}">${appIcon(i, c, sz)}</div>`;
    });
    s += `<div style="position:absolute;left:${cx - 140}px;top:${cy - 56}px;width:280px;display:flex;flex-direction:column;align-items:center;gap:12px"><div style="font-size:40px;font-weight:800">Waze</div>${hint(t, 'Push to open')}
      <div style="display:flex;gap:10px;margin-top:6px">${[1, 0].map((on) => `<div style="width:${on ? 30 : 12}px;height:12px;border-radius:6px;background:${on ? t.accent : t.track}"></div>`).join('')}</div></div>`;
    return s;
  },
});
add({
  slug: 'phone-apps', title: 'Phone apps list', group: 'more', section: 'Apps',
  template: 'MyPlay · AppsWorld, phone apps (CarAppEntry, kind PHONE_SCREEN / STANDALONE)',
  fields: 'CarAppEntry: label · icon · kind',
  desc: 'The phone’s own apps that the owner allowed on the car, on the wheel: there can be many, and a name reads quicker than an icon. Each opens on the phone’s mirrored screen. (Assumed: the same CarAppEntry list as the ring.)',
  html: (t) => homeMap(t) + corners(t) + title(t, 'Phone apps', { back: true }) +
    wheel(t, [['Weather', 'sun', '#E08A1E'], ['Parking', 'car', '#3A5AA6'], ['Fuel prices', 'gauge', '#8A5A00'], ['Calendar', 'timer', '#C2185B'], ['Notes', 'file-text', '#5B6478'], ['Translate', 'globe', '#17734A']],
      { focusedIndex: 1, render: ([l, i, c], ring, h, mid) => `<div style="min-height:${h}px;box-sizing:border-box;border-radius:${h / 2}px;background:${ring ? t.rowFocus : t.row};padding:10px 26px 10px 14px;display:flex;align-items:center;gap:18px;${ring ? P.focus(t) : ''}">${appIcon(i, c, mid ? 80 : 60)}<div style="flex:1;font-size:${mid ? 34 : 27}px;font-weight:800">${l}</div>${ic('smartphone', 26, t.secondary)}</div>` }),
});



// ================================================================= the status column: four options
// Today's top-right column (clock, outside temperature with the ice sign, phone battery) as four
// ORBIT-like alternatives on the C navigating home. The arrival stays in the hub. Sample: 14:07,
// 2 °C (ice: the warning red and the snowflake, at 4 °C or less), battery 82 % (or 18 %).

/** Text along a circle's arc: centred at angle [deg] (0 = top, clockwise), on radius r round (cx, cy). */
function arcText(id, cx, cy, r, deg, text, style, { below = false } = {}) {
  // a path running clockwise over the top (or anticlockwise under the bottom, so text reads upright)
  const rad = (d) => (d - 90) * Math.PI / 180;
  const span = 70;
  const a0 = below ? deg + span : deg - span, a1 = below ? deg - span : deg + span;
  const p = (d) => `${cx + r * Math.cos(rad(d))} ${cy + r * Math.sin(rad(d))}`;
  return `<path id="${id}" d="M ${p(a0)} A ${r} ${r} 0 0 ${below ? 0 : 1} ${p(a1)}" fill="none"/><text style="${style}" text-anchor="middle"><textPath href="#${id}" startOffset="50%">${text}</textPath></text>`;
}
/** A glass badge centred on (x, y), sitting on a ring and hiding it behind itself. */
const badge = (t, x, y, inner, h = 46) => `<div style="position:absolute;left:${x}px;top:${y}px;transform:translate(-50%,-50%);height:${h}px;padding:0 14px;border-radius:${h / 2}px;background:${t.night ? '#0A0F17' : '#F4F6FA'};border:2px solid ${t.edge};display:flex;align-items:center;gap:6px;white-space:nowrap">${inner}</div>`;
const iceBadge = (t) => `${ic('snowflake', 22, t.warning, 2.6)}<span style="font-size:24px;font-weight:800;color:${t.warning}">2°</span>`;
/** Navigating in C without the status column (the clock, temperature and battery). */
const navNoStatus = (t, { speed = true } = {}) => map(t, NAV) + turn(t, { lanes: LANES }) + hub(t, { trip: TRIP }) + (speed ? speedDisc(t) : '') + song(t);
const iceStyle = (t) => `font:800 24px 'Roboto Flex',Inter,sans-serif;fill:${t.warning}`;
const STATUS_FIELDS = 'clock (phone time) · outside temperature (car, when reported) + ice sign at ≤ 4 °C · phone battery level + charging (when the phone tells it) · arrival stays in the hub (TravelEstimate)';

add({
  slug: 'status-a-speed-ring', fileName: '35-status-a-speed-ring', group: 'status', section: 'Status',
  title: 'Status option a · the speed ring', template: 'ORBIT Status → the speed disc', fields: STATUS_FIELDS,
  desc: 'The speed disc grows a ring, like the hub and the song. The time is a badge set into the top of the ring, and the temperature with its ice sign a badge at the bottom. The battery is a thin arc on the ring’s right side that empties as the phone does, with no number until ≤ 20 % or charging. The top-right corner is map again.',
  html: (t) => {
    const cx = 136, cy = 356, R = 76;
    const batt = 0.82, b0 = 30, b1 = 150; // the battery arc: 30°..150° (right side), filled from the bottom
    const rad = (d) => (d - 90) * Math.PI / 180, pt = (d, r = R) => `${cx + r * Math.cos(rad(d))} ${cy + r * Math.sin(rad(d))}`;
    const fillTo = b1 - (b1 - b0) * batt;
    let svg = `<svg width="1280" height="720" style="position:absolute;left:0;top:0;overflow:visible">
      <circle cx="${cx}" cy="${cy}" r="${R}" fill="none" stroke="${t.track}" stroke-width="7"/>
      <path d="M ${pt(b1)} A ${R} ${R} 0 0 0 ${pt(fillTo)}" fill="none" stroke="${t.secondary}" stroke-width="7" stroke-linecap="round"/>
    </svg>`;
    return navNoStatus(t, { speed: false }) + P.plate(t, -10, 220, 300, 280) +
      `<div style="position:absolute;left:${cx - 64}px;top:${cy - 64}px;width:128px;height:128px;border-radius:50%;background:${t.glass};border:2px solid ${t.edge};display:flex;flex-direction:column;align-items:center;justify-content:center"><div style="font-size:50px;font-weight:800;line-height:50px">48</div><div style="font-size:18px;color:${t.secondary}">km/h</div></div>` +
      svg + badge(t, cx, cy - R, `<span style="font-size:32px;font-weight:800">14:07</span>`, 50) + badge(t, cx, cy + R, iceBadge(t), 42);
  },
});
add({
  slug: 'status-b-satellite', fileName: '35-status-b-satellite', group: 'status', section: 'Status',
  title: 'Status option b · a satellite of the speed disc', template: 'ORBIT Status → a satellite pill', fields: STATUS_FIELDS,
  desc: 'A small upright glass pill sits on a faint orbit round the speed disc, outside the free area. From the top: the time (big), the temperature with the ice sign, and the battery as a tiny bar with its number. Everything still reads in one glance, in one place, but no longer in a corner of its own.',
  html: (t) => {
    const cx = 136, cy = 400;
    const orbit = `<svg width="1280" height="720" style="position:absolute;left:0;top:0"><circle cx="${cx}" cy="${cy}" r="108" fill="none" stroke="${t.secondary}" stroke-opacity=".35" stroke-width="2" stroke-dasharray="4 7"/></svg>`;
    const sat = `<div style="position:absolute;left:186px;top:236px;width:96px;box-sizing:border-box;padding:12px 0 14px;border-radius:48px;background:${t.glass};border:2px solid ${t.edge};display:flex;flex-direction:column;align-items:center;gap:6px">
      <div style="font-size:28px;font-weight:800;line-height:28px">14:07</div>
      <div style="display:flex;align-items:center;gap:4px;font-size:22px;font-weight:800;color:${t.warning}">${ic('snowflake', 20, t.warning, 2.6)}2°</div>
      <div style="display:flex;align-items:center;gap:5px;font-size:18px;color:${t.secondary}"><div style="width:26px;height:12px;border:2px solid ${t.secondary};border-radius:3px;padding:1px;box-sizing:content-box"><div style="width:82%;height:100%;background:${t.secondary};border-radius:1px"></div></div>82%</div></div>`;
    return navNoStatus(t, { speed: false }) + P.plate(t, -10, 220, 320, 300) + orbit +
      `<div style="position:absolute;left:${cx - 60}px;top:${cy - 60}px;width:120px;height:120px;border-radius:50%;background:${t.glass};border:2px solid ${t.edge};display:flex;flex-direction:column;align-items:center;justify-content:center"><div style="font-size:46px;font-weight:800;line-height:46px">48</div><div style="font-size:18px;color:${t.secondary}">km/h</div></div>` + sat;
  },
});
add({
  slug: 'status-c-song-ring', fileName: '35-status-c-song-ring', group: 'status', section: 'Status',
  title: 'Status option c · the time on the song’s ring', template: 'ORBIT Status → the song disc', fields: STATUS_FIELDS,
  desc: 'The time is a badge set into the top of the song’s ring; the temperature and ice sign a badge on its left side. The battery is hidden until it matters: drawn here at 18 %, a red badge on the ring’s lower left. The top-right corner is map again, and the right side keeps one part, the song.',
  html: (t) => {
    const cx = 1164, cy = 604, R = 76;
    return navNoStatus(t) + P.plate(t, 1000, 380, 300, 340) +
      badge(t, cx, cy - R, `<span style="font-size:34px;font-weight:800">14:07</span>`, 52) +
      badge(t, cx - R - 6, cy - 34, iceBadge(t), 42) +
      badge(t, cx - R - 6, cy + 40, `<div style="width:24px;height:12px;border:2px solid ${t.warning};border-radius:3px;padding:1px;box-sizing:content-box"><div style="width:18%;height:100%;background:${t.warning}"></div></div><span style="font-size:22px;font-weight:800;color:${t.warning}">18%</span>`, 42);
  },
});
add({
  slug: 'status-d-corner-orb', fileName: '35-status-d-corner-orb', group: 'status', section: 'Status',
  title: 'Status option d · a corner orb (mine)', template: 'ORBIT Status → a quarter disc in the corner', fields: STATUS_FIELDS,
  desc: 'My idea: the top-right corner becomes a planet rising out of the screen’s edge, a quarter disc of glass, so every corner is now round (turn, orb, hub, song). The time is big inside it. Its rim is the battery ring, emptying from the bottom like the trip ring fills. The temperature and ice sign sit on the rim’s low end. It stays right of the free area (x ≥ 1010).',
  html: (t) => {
    const R = 250, cx = 1280, cy = 0, rr = R - 10;
    const batt = 0.82;
    // the rim runs from the left edge of the quarter (180°) to the bottom (90°), in screen angles
    const pt = (a) => `${cx + rr * Math.cos(a * Math.PI / 180)} ${cy + rr * Math.sin(a * Math.PI / 180)}`;
    const end = 90 + 90 * batt;
    return navNoStatus(t) +
      `<div style="position:absolute;left:${cx - R}px;top:${cy - R}px;width:${2 * R}px;height:${2 * R}px;border-radius:50%;background:${t.glass};border:2px solid ${t.edge}"></div>
       <svg width="1280" height="720" style="position:absolute;left:0;top:0"><path d="M ${pt(180)} A ${rr} ${rr} 0 0 0 ${pt(90)}" fill="none" stroke="${t.track}" stroke-width="7"/><path d="M ${pt(90)} A ${rr} ${rr} 0 0 1 ${pt(end)}" fill="none" stroke="${t.secondary}" stroke-width="7" stroke-linecap="round"/></svg>
       <div style="position:absolute;right:34px;top:34px;text-align:right"><div style="font-size:54px;font-weight:800;line-height:54px">14:07</div>
         <div style="margin-top:12px;display:flex;justify-content:flex-end;align-items:center;gap:6px;font-size:26px;font-weight:800;color:${t.warning}">${ic('snowflake', 24, t.warning, 2.6)}2°</div>
         <div style="margin-top:6px;font-size:20px;color:${t.secondary}">82%</div></div>`;
  },
});
// ---- the owner's pick: (c), and c2 (the time over the album art), plus no music
/** The song disc with a chosen [art] (CSS background) and [over] drawn centred on the art. */
const songWith = (t, art, over = '', p = 0.38) => P.plate(t, 1040, 470, 260, 260) + `<div style="position:absolute;left:1088px;top:528px;width:152px;height:152px">
    <svg width="152" height="152" style="position:absolute"><circle cx="76" cy="76" r="72.5" fill="none" stroke="${t.track}" stroke-width="7"/>${p === null ? '' : `<circle cx="76" cy="76" r="72.5" fill="none" stroke="${t.music}" stroke-width="7" stroke-linecap="round" stroke-dasharray="${2 * Math.PI * 72.5 * p} 999" transform="rotate(-90 76 76)"/>`}</svg>
    <div style="position:absolute;left:13px;top:13px;width:126px;height:126px;border-radius:50%;overflow:hidden;background:${art};display:flex;align-items:center;justify-content:center">${over}</div></div>`;
/** A pale, busy cover: the hard case for white text. */
const PALE_ART = 'radial-gradient(circle at 30% 30%,#FFFFFF 0 18%,transparent 19%),radial-gradient(circle at 75% 65%,#FFE07A 0 22%,transparent 23%),linear-gradient(135deg,#FFF4C2,#FFC9E0 50%,#BDEBFF)';
const navNoClock = (t) => map(t, NAV) + turn(t, { lanes: LANES }) + hub(t, { trip: TRIP }) + speedDisc(t);
const SONG_C = { cx: 1164, cy: 604, R: 76 };
const iceSide = (t) => badge(t, SONG_C.cx - SONG_C.R - 6, SONG_C.cy - 34, iceBadge(t), 42);

add({
  slug: 'status-c2-scrim', fileName: '35-status-c2-scrim', group: 'status', section: 'Status',
  title: 'Status c2 · the time over the art, on a soft scrim', template: 'ORBIT Status → the song disc', fields: STATUS_FIELDS,
  desc: 'The time is centred on the album art, white and extra bold, over a soft dark scrim that fades out before the disc’s edge, so the art still shows round it. Drawn on a pale, busy cover, the hard case. The temperature badge stays on the ring’s left; the battery appears only when low.',
  html: (t) => navNoClock(t) + songWith(t, PALE_ART,
      `<div style="position:absolute;inset:0;background:radial-gradient(closest-side,rgba(6,8,13,0.82) 0,rgba(6,8,13,0.68) 55%,rgba(6,8,13,0) 100%)"></div><div style="position:relative;font-size:38px;font-weight:800;color:#fff;text-shadow:0 1px 4px rgba(0,0,0,.6)">14:07</div>`) + iceSide(t),
});
add({
  slug: 'status-c2-badge', fileName: '35-status-c2-badge', group: 'status', section: 'Status',
  title: 'Status c2 · the time over the art, on a glass badge', template: 'ORBIT Status → the song disc', fields: STATUS_FIELDS,
  desc: 'The same place, but the time sits on a small pill of ORBIT’s glass in the middle of the art: legible on any cover, at the cost of hiding a strip of it. Same pale cover.',
  html: (t) => navNoClock(t) + songWith(t, PALE_ART,
      `<div style="height:50px;padding:0 14px;border-radius:25px;background:rgba(8,12,20,0.86);border:2px solid rgba(255,255,255,0.14);display:flex;align-items:center;font-size:34px;font-weight:800;color:#EEF1F7">14:07</div>`) + iceSide(t),
});
add({
  slug: 'status-c-nomusic', fileName: '35-status-c-nomusic', group: 'status', section: 'Status',
  title: 'Status · no music (for c and c2)', template: 'ORBIT Status → the empty song disc', fields: STATUS_FIELDS + ' · no song (SongSlot null)',
  desc: 'Nothing playing: the music circle stays where it is, empty glass with just its track ring, and holds the time, big and centred. With c2 the time does not move at all when music stops or starts; with c it drops from the ring’s top badge into the middle. The temperature badge keeps its place.',
  html: (t) => navNoClock(t) + songWith(t, t.glass, `<div style="font-size:40px;font-weight:800;color:${t.text}">14:07</div>`, null) + iceSide(t),
});

module.exports = { SCREENS, NIGHT, DAY };
