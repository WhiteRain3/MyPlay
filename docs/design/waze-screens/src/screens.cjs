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
  desc: 'A glass column on the driver’s side of the free area (x 288–708); Waze’s map stays visible to its right. Rows are ORBIT pills, the focused row carries the focus ring. Recommended.',
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

// ---- C: Orbit wheel
/** Items on an arc round a centre far to the left, the focused one in the middle, biggest. */
function wheel(t, items, { focusedIndex = 0, render }) {
  const cx = -330, cy = 330, R = 640;
  let s = '';
  items.forEach((it, i) => {
    const k = i - focusedIndex;
    if (Math.abs(k) > 1) return;
    const a = k * 17 * Math.PI / 180;
    const x = cx + R * Math.cos(a), y = cy + R * Math.sin(a);
    const f = k === 0;
    const w = f ? 560 : 470, h = f ? 112 : 84;
    s += `<div style="position:absolute;left:${x}px;top:${y - h / 2}px;width:${w}px;opacity:${[1, 0.7][Math.abs(k)]}">${render(it, f, h)}</div>`;
  });
  return s;
}
add({
  slug: 'option-c-wheel-list', group: 'option', title: 'Option C · Orbit wheel — destinations',
  template: 'MapWithContentTemplate › ListTemplate', look: 'C',
  desc: 'The list turns like the dial: the focused place is always in the middle and biggest, its neighbours shrink and fade along an arc. No scrolling to think about, but only three places are seen at once.',
  html: (t) => map(t, { car: { x: 850, y: 420 }, seed: 3, dim: 0.35 }) + corners(t) +
    `<div style="position:absolute;left:300px;top:52px;display:flex;align-items:center;gap:16px"><div style="font-size:32px;font-weight:800;${P.floatText(t, t.text)}">Where to?</div></div>` +
    wheel(t, PLACES, { focusedIndex: 2, render: (p, f, h) => `<div style="background:${t.glass};border-radius:${h / 2}px">${row(t, { ...p, focused: f, h, big: f })}</div>` }) +
    `<div style="position:absolute;left:900px;top:52px">${disc(t, { icon: 'search', size: 72 })}</div>`,
});
add({
  slug: 'option-c-wheel-route', group: 'option', title: 'Option C · Orbit wheel — route preview',
  template: 'RoutePreviewNavigationTemplate', look: 'C',
  desc: 'Routes on the arc; “Go now” is a big accent disc to their right. Striking and very ORBIT, but the wheel and the disc are two places to look, and long lists only ever show three rows.',
  html: (t) => map(t, { ...RP, dim: 0.3 }) + corners(t) +
    `<div style="position:absolute;left:300px;top:52px;display:flex;align-items:center;gap:16px">${disc(t, { icon: 'arrow-left', size: 64 })}<div style="font-size:32px;font-weight:800;${P.floatText(t, t.text)}">Work</div></div>` +
    wheel(t, ROUTES, { focusedIndex: 1, render: (r, f, h) => `<div style="background:${t.glass};border-radius:${h / 2}px">${row(t, { ...r, radio: r === ROUTES[0], h, big: f })}</div>` }) +
    `<div style="position:absolute;left:790px;top:420px;width:190px;height:190px;border-radius:50%;background:${t.accent};color:${t.onAccent};display:flex;flex-direction:column;align-items:center;justify-content:center;gap:6px;${P.focus(t)}">${ic('navigation', 52, t.onAccent, 2.6)}<div style="font-size:30px;font-weight:800">Go now</div></div>`,
});

// ================================================================= option A, every key screen

add({
  slug: 'destinations', title: 'Destinations (Waze’s first screen)', group: 'A',
  template: 'MapWithContentTemplate › ListTemplate (or PlaceListNavigationTemplate)',
  fields: 'header: Action.APP_ICON + title · header actionStrip (search) · itemList rows: image, title, texts (address, DistanceSpan), browsable · mapController.mapActionStrip',
  desc: 'Opens with the first place focused. Five rows fit with no scrolling; “2 more” shows the list goes on. The distance (Waze’s DistanceSpan) is pulled to the row’s end so it can be read at a glance.',
  html: (t) => map(t, { car: { x: 850, y: 420 }, seed: 3 }) + corners(t) + column(t,
    header(t, { back: false, appIcon: true, title: 'Where to?', strip: [{ icon: 'search', size: 64 }] }) +
    PLACES.slice(0, 5).map((p, i) => row(t, { ...inl(p), focused: i === 0 })).join('') + more(t, 2)) + mapStrip(t),
});
add({
  slug: 'destinations-scrolled', title: 'Destinations, turned to the end', group: 'A',
  template: 'MapWithContentTemplate › ListTemplate',
  fields: 'itemList (as above) · onItemVisibilityChangedDelegate gets the rows now shown',
  desc: 'Turning past the fifth row moves the list a row at a time, the focus stays one row from the edge, and “2 earlier” appears at the top.',
  html: (t) => map(t, { car: { x: 850, y: 420 }, seed: 3 }) + corners(t) + column(t,
    header(t, { back: false, appIcon: true, title: 'Where to?', strip: [{ icon: 'search', size: 64 }] }) +
    `<div style="display:flex;align-items:center;justify-content:center;gap:8px;font-size:20px;color:${t.secondary}">${ic('arrow-up', 20, t.secondary)}2 earlier</div>` +
    PLACES.slice(2, 7).map((p, i) => row(t, { ...inl(p), focused: i === 3 })).join('')) + mapStrip(t),
});
add({
  slug: 'search-driving', title: 'Search while driving', group: 'A',
  template: 'SearchTemplate',
  fields: 'headerAction (BACK) · searchHint · initialSearchText · itemList (rows: image, title, texts, DistanceSpan) · isLoading · showKeyboardByDefault (refused while driving)',
  desc: 'Typing is refused while driving (as Android Auto), so the field shows the search text Waze sent, with a lock, and the focus goes straight to the first result.',
  html: (t) => map(t, { car: { x: 850, y: 420 }, seed: 5 }) + corners(t) + column(t,
    `<div style="display:flex;align-items:center;gap:14px;height:72px">${disc(t, { icon: 'arrow-left', size: 64 })}
      <div style="flex:1;height:64px;border-radius:32px;background:${t.row};border:2px solid ${t.edge};display:flex;align-items:center;gap:12px;padding:0 20px">${ic('search', 28, t.secondary)}<div style="flex:1;font-size:28px;font-weight:700">coffee</div>${ic('lock', 24, t.secondary)}</div></div>` +
    `<div style="font-size:20px;color:${t.secondary};padding-left:8px;margin:-4px 0 10px">Typing works when you’re parked</div>` +
    RESULTS.slice(0, 5).map((p, i) => row(t, { ...inl(p), focused: i === 0 })).join('') + more(t, 1)) + mapStrip(t),
});
add({
  slug: 'search-parked-keyboard', title: 'Search while parked: the letter ring', group: 'A',
  template: 'SearchTemplate (our keyboard)',
  fields: 'searchHint · the text we own (sendSearchTextChanged on each letter, sendSearchSubmitted on Search) · itemList (its size, as you type)',
  desc: 'Parked, the knob types on a ring of letters that fills the free area: turn to a letter, push to type it. Space, delete and Search close the ring. The text and the number of places Waze has found so far sit in the middle; Search (or the d-pad down) goes to the results list.',
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
  slug: 'search-no-results', title: 'Search with no results', group: 'A',
  template: 'SearchTemplate',
  fields: 'itemList empty → itemList.noItemsMessage (else our “No results”)',
  desc: 'The message Waze gives for an empty list, centred in the column; the focus falls back to Back so one push leaves.',
  html: (t) => map(t, { car: { x: 850, y: 420 }, seed: 5 }) + corners(t) + column(t,
    `<div style="display:flex;align-items:center;gap:14px;height:72px">${disc(t, { icon: 'arrow-left', size: 64, focused: true })}
      <div style="flex:1;height:64px;border-radius:32px;background:${t.row};border:2px solid ${t.edge};display:flex;align-items:center;gap:12px;padding:0 20px">${ic('search', 28, t.secondary)}<div style="flex:1;font-size:28px;font-weight:700">zqx bakery</div>${ic('lock', 24, t.secondary)}</div></div>` +
    `<div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:18px;text-align:center">
      <div style="width:120px;height:120px;border-radius:50%;background:${t.track};display:flex;align-items:center;justify-content:center">${ic('map-pin-off', 56, t.secondary)}</div>
      <div style="font-size:30px;font-weight:800;max-width:330px;line-height:1.25">No results for “zqx bakery”</div></div>`) + mapStrip(t),
});
add({
  slug: 'route-preview', title: 'Route preview', group: 'A',
  template: 'RoutePreviewNavigationTemplate',
  fields: 'header / title + headerAction (BACK) · itemList rows (title = time, texts = via, distance) with onSelectedDelegate + selectedIndex · navigateAction (FLAG_PRIMARY) · actionStrip · mapActionStrip · panModeDelegate',
  desc: '“Go now” holds the focus when the screen opens: one push starts the drive. Turning the knob back moves through the routes (each is a radio row: a push picks it, Waze redraws the map). The map’s visible area is right of the column, so Waze fits the routes there.',
  html: (t) => map(t, RP) + corners(t) + column(t,
    header(t, { title: 'Work' }) +
    ROUTES.map((r, i) => row(t, { ...r, radio: i === 0, h: 92 })).join('') +
    '<div style="flex:1"></div>' + primary(t, 'Go now') +
    `<div style="display:flex;justify-content:center;margin-top:12px">${hint(t, 'Turn for another route')}</div>`) + mapStrip(t),
});
add({
  slug: 'route-preview-choosing', title: 'Route preview, turning to another route', group: 'A',
  template: 'RoutePreviewNavigationTemplate',
  fields: 'itemList (onSelectedDelegate.sendSelected on push) · navigateAction',
  desc: 'The focus ring on the second route; the radio dot still on the chosen one. A push picks it and the focus jumps back to “Go now”.',
  html: (t) => map(t, RP) + corners(t) + column(t,
    header(t, { title: 'Work' }) +
    ROUTES.map((r, i) => row(t, { ...r, radio: i === 0, h: 92, focused: i === 1 })).join('') +
    '<div style="flex:1"></div>' + primary(t, 'Go now', { focused: false }) +
    `<div style="display:flex;justify-content:center;margin-top:12px">${hint(t, 'Push to choose this route')}</div>`) + mapStrip(t),
});
add({
  slug: 'route-preview-loading', title: 'Route preview, loading', group: 'A',
  template: 'RoutePreviewNavigationTemplate',
  fields: 'isLoading · header / title · navigateAction (not drawn until routes come)',
  desc: 'Only ORBIT’s spinning ring: Waze sends no words while loading, so we add none. Back is focused.',
  html: (t) => map(t, { car: RP.car, pin: RP.pin }) + corners(t) + column(t,
    header(t, { title: 'Work', focusedBack: true }) +
    `<div style="flex:1;display:flex;align-items:center;justify-content:center">${spinner(t, 120, 9)}</div>`) + mapStrip(t),
});
add({
  slug: 'navigating', title: 'Navigating: the action strip and map buttons', group: 'A',
  template: 'NavigationTemplate',
  fields: 'navigationInfo (RoutingInfo → ORBIT’s turn disc and lanes) · destinationTravelEstimate (→ the hub: arrival, time and distance left, remainingTimeColor) · actionStrip (top) · mapActionStrip (right)',
  desc: 'ORBIT draws the turn and the trip itself; Waze’s action strip becomes a row of discs along the top of the free area and its map buttons a column down the right edge, both clear of the middle where Waze draws the car. Here the knob is on the first strip action.',
  html: (t) => map(t, NAV) + turn(t, { lanes: [{ dir: 'u' }, { dir: 'u' }, { dir: 'r', on: true }] }) + clock(t) +
    hub(t, { trip: { eta: '14:32', left: '24 min', km: '18 km', p: 0.18 } }) + song(t) + actionStrip(t, { focusedIndex: 0 }) + mapStrip(t),
});
add({
  slug: 'navigating-titled-strip', title: 'Navigating: an action with a title', group: 'A',
  template: 'NavigationTemplate',
  fields: 'actionStrip with a titled Action (icon + title) · mapActionStrip',
  desc: 'When Waze sends an action with words, it becomes a pill in the same row. Here the knob is on the map’s zoom-in disc.',
  html: (t) => map(t, NAV) + turn(t, { dist: '1.2 km', road: 'Ring Road', icon: 'arrow-up' }) + clock(t) +
    hub(t, { trip: { eta: '14:41', left: '33 min', km: '24 km', p: 0.1, late: true } }) + song(t) +
    actionStrip(t, { actions: [{ icon: 'triangle-alert' }, { icon: 'x', title: 'Stop', size: 72 }] }) + mapStrip(t, { focusedIndex: 1 }),
});
add({
  slug: 'navigating-pan', title: 'Moving the map (pan mode)', group: 'A',
  template: 'NavigationTemplate',
  fields: 'mapActionStrip (Action.PAN) · panModeDelegate.sendPanModeChanged · SurfaceCallback onScroll / onScale',
  desc: 'Pan mode hides the turn and the action strip (as Android Auto). The pan disc is lit; the d-pad moves the map, turning zooms, Back leaves pan mode.',
  html: (t) => map(t, { ...NAV, seed: 11 }) + clock(t) + hub(t, { trip: { eta: '14:32', left: '24 min', km: '18 km', p: 0.18 } }) + song(t) +
    mapStrip(t, { pan: true, focusedIndex: 0 }) +
    `<div style="position:absolute;left:640px;top:360px;transform:translate(-50%,-50%)">${['▲', '▶', '▼', '◀'].map((a, i) => `<div style="position:absolute;font-size:28px;color:${t.text};text-shadow:0 0 8px ${t.halo};left:${[0, 70, 0, -70][i] - 10}px;top:${[-70, 0, 70, 0][i] - 18}px">${a}</div>`).join('')}<div style="width:18px;height:18px;border-radius:50%;border:3px solid ${t.text};transform:translate(-50%,-50%)"></div></div>` +
    `<div style="position:absolute;left:288px;top:43px;padding:12px 22px;border-radius:40px;background:${t.glass};border:2px solid ${t.edge};display:flex;gap:26px">${hint(t, 'Pad moves · turn zooms')}<div style="font-size:20px;color:${t.secondary}">Back to finish</div></div>`,
});
add({
  slug: 'navigating-message', title: 'A navigation message (arrived)', group: 'A',
  template: 'NavigationTemplate (navigationInfo = MessageInfo)',
  fields: 'MessageInfo: title, text, image · destinationTravelEstimate (if any)',
  desc: 'A MessageInfo takes the turn’s place: its image in the accent disc, its title and text floating beside it like the distance and road.',
  html: (t) => map(t, { car: { x: 640, y: 470 }, pin: { x: 690, y: 430 }, seed: 9 }) +
    P.plate(t, -40, -40, 640, 300) +
    `<div style="position:absolute;left:40px;top:40px;display:flex;align-items:center;gap:22px">
      <div style="width:132px;height:132px;border-radius:50%;background:${t.accent};display:flex;align-items:center;justify-content:center">${ic('flag', 66, t.onAccent, 2.6)}</div>
      <div><div style="font-size:46px;font-weight:800;${P.floatText(t, t.text)}">You have arrived</div><div style="font-size:26px;margin-top:6px;${P.floatText(t, t.secondary)}">Work · Harbour Street 21</div></div></div>` +
    clock(t) + hub(t, { trip: { eta: '14:32', left: 'Now', km: '0 m', p: 1 } }) + song(t) + actionStrip(t) + mapStrip(t),
});
add({
  slug: 'navigating-rerouting', title: 'A navigation message (loading)', group: 'A',
  template: 'NavigationTemplate (RoutingInfo.isLoading, or a MessageInfo with no image)',
  fields: 'RoutingInfo.isLoading · or MessageInfo title / text',
  desc: 'While Waze works out the way, the turn disc becomes ORBIT’s spinning ring; any words it sends float beside it.',
  html: (t) => map(t, { car: { x: 640, y: 470 }, seed: 9 }) + P.plate(t, -40, -40, 560, 300) +
    `<div style="position:absolute;left:40px;top:40px;display:flex;align-items:center;gap:22px">
      <div style="width:132px;height:132px;border-radius:50%;background:${t.glass};border:2px solid ${t.edge};display:flex;align-items:center;justify-content:center">${spinner(t, 92, 8)}</div>
      <div style="font-size:40px;font-weight:800;${P.floatText(t, t.text)}">Recalculating…</div></div>` +
    clock(t) + hub(t, { trip: { eta: '14:35', left: '27 min', km: '19 km', p: 0.18 } }) + song(t) + actionStrip(t) + mapStrip(t),
});
add({
  slug: 'alert', title: 'An alert while driving', group: 'A',
  template: 'Alert (AppManager.showAlert)',
  fields: 'title · subtitle · icon · actions (first two) · durationMillis (→ the ring round the icon) · callbackDelegate (timeout / cancel)',
  desc: 'ORBIT’s alert pill, between the hub and the song, clear of the turn. New: the alert’s time left runs as a ring round its icon, so the driver sees it will go by itself. The first action is focused; Back dismisses.',
  html: (t) => map(t, NAV) + turn(t, { lanes: [{ dir: 'u' }, { dir: 'u' }, { dir: 'r', on: true }] }) + clock(t) +
    hub(t, { trip: { eta: '14:32', left: '24 min', km: '18 km', p: 0.18 } }) + song(t) + actionStrip(t) + mapStrip(t) +
    `<div style="position:absolute;left:288px;top:556px;width:620px;box-sizing:border-box;border-radius:60px;background:${t.glass};border:2px solid ${t.edge};padding:14px 18px 14px 14px;display:flex;align-items:center;gap:16px">
      <div style="position:relative;width:84px;height:84px;flex:none"><svg width="84" height="84" style="position:absolute"><circle cx="42" cy="42" r="38" fill="none" stroke="${t.track}" stroke-width="6"/><circle cx="42" cy="42" r="38" fill="none" stroke="${t.caution}" stroke-width="6" stroke-linecap="round" stroke-dasharray="${2 * Math.PI * 38 * 0.62} 999" transform="rotate(-90 42 42)"/></svg>
        <div style="position:absolute;left:12px;top:12px;width:60px;height:60px;border-radius:50%;background:${t.caution}38;display:flex;align-items:center;justify-content:center">${ic('route', 32, t.caution)}</div></div>
      <div style="flex:1;min-width:0"><div style="font-size:26px;font-weight:800;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">Faster route found</div><div style="font-size:20px;color:${t.secondary};white-space:nowrap">Saves 6 min via B14</div></div>
      ${button(t, 'Take it', { focused: true, filled: true, h: 60 })}${button(t, 'No', { h: 60 })}</div>`,
});
add({
  slug: 'message', title: 'A message screen', group: 'A',
  template: 'MessageTemplate (alone, or in MapWithContentTemplate)',
  fields: 'header / title + headerAction · icon · message · isLoading · actions (up to two; FLAG_PRIMARY filled) · debugMessage (never shown in the car)',
  desc: 'Centred in the free area on glass. The primary action (or the first) is focused; a second action is a plain pill beside it. Back is the header action.',
  html: (t) => map(t, { car: { x: 850, y: 420 }, seed: 13, dim: 0.25 }) + corners(t) + card(t, 338, 103, 604, 514,
    header(t, { title: 'Waze' }) +
    `<div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:22px;text-align:center">
      <div style="width:120px;height:120px;border-radius:50%;background:${t.track};display:flex;align-items:center;justify-content:center">${ic('locate-fixed', 60, t.caution)}</div>
      <div style="font-size:32px;font-weight:800;line-height:1.25;max-width:500px">Location is off on your phone. Turn it on to navigate.</div></div>
    <div style="display:flex;justify-content:center;gap:22px;margin-bottom:10px">${button(t, 'Try again', { focused: true, filled: true })}${button(t, 'Not now')}</div>`),
});
add({
  slug: 'message-loading', title: 'A message screen, loading', group: 'A',
  template: 'MessageTemplate (isLoading)',
  fields: 'isLoading · message (if any)',
  desc: 'ORBIT’s ring spinner in place of the icon; no buttons, so Back is focused.',
  html: (t) => map(t, { car: { x: 850, y: 420 }, seed: 13, dim: 0.25 }) + corners(t) + card(t, 338, 103, 604, 514,
    header(t, { title: 'Waze', focusedBack: true }) +
    `<div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:26px">${spinner(t, 110, 9)}<div style="font-size:30px;font-weight:800">Loading your places…</div></div>`),
});
add({
  slug: 'route-preview-day', title: 'Route preview by day', group: 'A', day: true,
  template: 'RoutePreviewNavigationTemplate',
  fields: 'as Route preview',
  desc: 'ORBIT’s day glass and darkened accent (each at least 4.9:1); the same layout.',
  html: (t) => map(t, RP) + corners(t) + column(t,
    header(t, { title: 'Work' }) +
    ROUTES.map((r, i) => row(t, { ...r, radio: i === 0, h: 92 })).join('') +
    '<div style="flex:1"></div>' + primary(t, 'Go now') +
    `<div style="display:flex;justify-content:center;margin-top:12px">${hint(t, 'Turn for another route')}</div>`) + mapStrip(t),
});
add({
  slug: 'navigating-day', title: 'Navigating by day', group: 'A', day: true,
  template: 'NavigationTemplate', fields: 'as Navigating',
  desc: 'The day glass over a day map.',
  html: (t) => map(t, NAV) + turn(t, { lanes: [{ dir: 'u' }, { dir: 'u' }, { dir: 'r', on: true }] }) + clock(t) +
    hub(t, { trip: { eta: '14:32', left: '24 min', km: '18 km', p: 0.18 } }) + song(t) + actionStrip(t, { focusedIndex: 1 }) + mapStrip(t),
});

module.exports = { SCREENS, NIGHT, DAY };
