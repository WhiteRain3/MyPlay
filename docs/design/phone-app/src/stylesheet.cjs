// The one-page style sheet (31-style-sheet.png). Contrast ratios are computed, not typed.
const P = require('./parts.cjs');
const { I, ring, row, card, tag, NEXT, ALSO, btn, seg, slider, tabBar, carGlyph, mark } = P;

function lum(hex) {
  const n = hex.replace('#', '');
  const c = [0, 2, 4].map((i) => parseInt(n.slice(i, i + 2), 16) / 255).map((v) => (v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4));
  return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
}
const cr = (a, b) => { const [x, y] = [lum(a), lum(b)].sort((p, q) => q - p); return (x + 0.05) / (y + 0.05); };
const f = (n) => n.toFixed(1) + ':1';

const BG = '#0A1015', SURF = '#121A21', SURF2 = '#1A242D', ON = '#0A1015';
const surfaces = [['bg', BG, 'Page'], ['surface', SURF, 'Cards'], ['surface2', SURF2, 'Discs, segmented'], ['sheet', '#151E26', 'Bottom sheet'], ['track', '#2A3540', 'Off track, dividers']];
const texts = [['text', '#EEF1F7', 'Labels, titles'], ['text2', '#AAB3C8', 'Values, section heads'], ['text3', '#8A95A8', 'Help lines, chevrons']];
const accents = [['Home', '#6EE7A8', 'ORBIT Navigate'], ['Screen', '#A78BFA', 'ORBIT Music'], ['Car', '#FFC24A', 'ORBIT Car'], ['Apps', '#FF8FB1', 'ORBIT Apps'], ['More', '#9DB4E8', 'ORBIT Settings']];
const signals = [['ok', '#6EE7A8', 'Connected, done'], ['caution', '#FF9A57', 'Needs attention: badge, card dot'], ['warn', '#FF7A7A', 'Errors, risky switches']];
const INT = '#DCE3EF';
// Non-text parts that show state (WCAG 1.4.11): >= 3:1 against every surface they sit on.
const nontext = [['Switch outline (off)', '#8A95A8'], ['Slider rail', '#67748A'], ['Dashed ring (not connected)', '#7A8698'], ['Focus ring', INT], ['Changed mark', '#AAB3C8']];

// Fail the build if any text colour drops under 4.5:1 where it is used.
for (const [, c] of texts) for (const s of [BG, SURF, SURF2]) if (cr(c, s) < 4.5) throw new Error(`contrast ${c} on ${s} = ${cr(c, s)}`);
for (const [, c] of [...accents, ...signals, ['', INT]]) { if (cr(c, BG) < 4.5 || cr(c, SURF) < 4.5) throw new Error('accent contrast ' + c); if (cr(ON, c) < 4.5) throw new Error('on-accent ' + c); }
for (const [n, c] of nontext) for (const s of [BG, SURF, SURF2]) if (cr(c, s) < 3) throw new Error(`non-text contrast ${n} ${c} on ${s} = ${cr(c, s)}`);

const sw = (c, name, use, extra) => `<div class="swatch"><div class="chip" style="background:${c}"></div><div><b>${name}</b> <code>${c}</code><div class="use">${use}</div>${extra ?? ''}</div></div>`;

module.exports = function styleSheet() {
  const colours = `<div class="cols3">
    <div><h3>Surfaces</h3>${surfaces.map(([n, c, u]) => sw(c, n, u)).join('')}<div class="use" style="margin-top:8px">Edges: white 7 % (cards), 14 % (sheet, ghost button)</div></div>
    <div><h3>Text</h3>${texts.map(([n, c, u]) => sw(c, n, u, `<div class="cr">${f(cr(c, BG))} on bg · ${f(cr(c, SURF))} on surface · ${f(cr(c, SURF2))} on surface2</div>`)).join('')}
      <h3 style="margin-top:22px">Signals</h3>${signals.map(([n, c, u]) => sw(c, n, u, `<div class="cr">${f(cr(c, BG))} on bg · ${f(cr(c, SURF))} on surface</div>`)).join('')}</div>
    <div><h3>Interaction (one colour, every tab)</h3>${sw(INT, 'int', 'Switches, sliders, primary buttons, selection, focus', `<div class="cr">${f(cr(INT, SURF))} on surface · text on fill ${f(cr(ON, INT))}</div>`)}
      <h3 style="margin-top:22px">World colour per tab: identity only</h3>${accents.map(([n, c, u]) => sw(c, n, u, `<div class="cr">${f(cr(c, SURF))} on surface</div>`)).join('')}
      <div class="use" style="margin-top:8px">Only the selected tab's ring, the root title glyph and the hero ring use it, as ORBIT tints the world you are in. Never a control, never text.</div>
      <h3 style="margin-top:22px">Non-text contrast (≥ 3:1, checked)</h3>${nontext.map(([n, c]) => `<div class="cr" style="display:flex;gap:10px;align-items:center;margin-bottom:6px"><i style="width:28px;height:4px;border-radius:2px;background:${c}"></i>${n} · ${f(cr(c, BG))} bg · ${f(cr(c, SURF))} surface · ${f(cr(c, SURF2))} surface2</div>`).join('')}</div></div>`;

  const types = [['Root title', 30, 650, -0.6, 'Screen'], ['Hero', 24, 650, -0.4, 'Connected'], ['Page title / sheet', 22, 620, -0.3, 'Pad shortcuts'], ['Button', 16, 620, -0.1, 'Install 2.4'], ['Label', 16, 520, 0, 'Follow the car’s lights'], ['Value', 14, 400, 0, 'Last used'], ['Help', 13, 400, 0, 'Once a day, from GitHub'], ['Section', 13, 600, 0.2, 'Knob push'], ['Tab label', 12, 520, 0, 'Apps'], ['Tag', 11.5, 600, 0.1, 'Next connection']];
  const typeHtml = `<table class="type">${types.map(([n, s, w, ls, ex]) => `<tr><td class="use">${n}<br><code>${s} / ${w}${ls ? ' / ' + ls : ''}</code></td><td style="font-size:${s}px;font-weight:${w};letter-spacing:${ls}px">${ex}</td></tr>`).join('')}</table>
    <div class="use" style="margin-top:12px">Roboto Flex (OFL), the platform's own font, so the phone matches these pictures. Sentence case. Labels ≤ 30 characters, help ≤ 60 (checked by the build). Text scales to 200 %; rows grow, never clip.</div>`;

  const spaceHtml = `<div style="display:flex;gap:18px;align-items:flex-end;margin-bottom:18px">${[4, 8, 12, 16, 20, 24, 32, 48].map((n) => `<div style="text-align:center"><div style="width:${n}px;height:${n}px;background:var(--acc);border-radius:3px;margin:0 auto"></div><code>${n}</code></div>`).join('')}</div>
    <ul class="notes">
      <li>4 dp grid. Side gutter 16. Gap between cards 12. Section head 20 above, 8 below.</li>
      <li>Touch target ≥ 48 × 48 everywhere; rows 56–64 (72 with help).</li>
      <li>Radii: card 24 · picture 20 · sheet 28 · buttons and tags fully round · icon disc 40 circle.</li>
      <li>Tab bar 72 + gesture area 24. Top bar 72 (root) / 64 (page).</li>
    </ul>`;

  const comps = `<div class="cols3 t-screen">
    <div><h3>Rows</h3>${card([
      row({ ic: 'palette', label: 'Colours', value: 'Slate + own', changed: true }),
      row({ ic: 'sun-moon', label: "Follow the car's lights", sw: true }),
      row({ ic: 'gauge', label: 'Speed source', help: 'Car first, then phone GPS', value: 'Auto', tag: ALSO }),
      row({ label: 'Prefer 60 fps', sw: false, tag: NEXT }),
      row({ ic: 'globe', label: 'Project page', ext: true }),
    ].join(''))}
    <div class="use" style="margin-top:10px">Icon disc · label · one help line at most · value or control on the right. The dot = changed from default. Chevron opens a page; ↗ leaves to another tab or the web.</div>
    <h3 style="margin-top:26px">Switch</h3><div style="display:flex;gap:18px;align-items:center"><div class="sw on"></div><div class="sw"></div><span class="use">52 × 32 in a 48 target. A light haptic on change.</span></div>
    </div>
    <div><h3>Slider and segmented</h3>${card(slider('Text size', 'Larger', 66.6, { ticks: 4, ends: ['Small', 'Largest'], changed: true }))}<div style="height:14px"></div>${seg(["Car's", 'Dark', 'Light'], 0)}
    <div class="use" style="margin-top:10px">The slider thumb is a ring (ORBIT's highlight). Steps snap; the value shows in the accent.</div>
    <h3 style="margin-top:26px">Buttons</h3><div style="display:flex;gap:10px;flex-wrap:wrap">${btn('Install', 'primary')}${btn('Disconnect', 'ghost')}${btn('Reset this page', 'text', 'rotate-ccw')}</div>
    <div class="use" style="margin-top:10px">One primary per screen. Others are ghost or text.</div>
    <h3 style="margin-top:26px">Tags</h3><div style="display:flex;gap:8px;flex-wrap:wrap">${NEXT}${ALSO}${tag('In use', 'acc')}${tag('Allowed', 'ok')}${tag('Left: nothing', 'warn')}</div>
    </div>
    <div><h3>Rings (status)</h3><div style="display:flex;gap:22px;align-items:center;flex-wrap:wrap">
      <div class="t-home">${ring(84, { w: 4, color: 'var(--acc)', inner: carGlyph(32, true) })}<div class="use c">Connected</div></div>
      <div>${ring(84, { w: 3, color: '#5A6676', dashed: '3 9', track: false, inner: carGlyph(30), innerColor: 'var(--text3)' })}<div class="use c">Not connected</div></div>
      <div>${ring(84, { w: 4, pct: 0.64, color: 'var(--acc)', inner: '<b style="font-size:16px;color:var(--text)">64%</b>' })}<div class="use c">Progress</div></div>
      <div>${ring(84, { w: 4, color: 'var(--warn)', pct: 0.3, inner: `<span style="color:var(--warn)">${I('circle-alert', 30)}</span>` })}<div class="use c">Error</div></div>
    </div>
    <h3 style="margin-top:26px">Card and sheet</h3>
    <div style="position:relative;height:250px;border-radius:24px;overflow:hidden;border:1px solid var(--edge)"><div style="padding:16px">${card(row({ ic: 'download', label: 'Version 2.4 is ready', value: '' }), 'acc')}</div>
      <div class="scrim"></div><div class="sheet" style="position:absolute;padding-bottom:20px"><div class="grab"></div><h2 style="font-size:19px">Share the log</h2><div class="btns" style="margin-top:16px">${btn('Cancel', 'ghost small')}${btn('Share', 'primary small')}</div></div></div>
    <h3 style="margin-top:26px">Snackbar (undo)</h3><div style="position:relative;height:56px"><div class="snack" style="position:absolute;inset:0;bottom:auto">Car forgotten<b>Undo</b></div></div>
    </div></div>
    <h3 style="margin-top:34px">Tab bar</h3><div style="position:relative;width:412px;height:96px;background:var(--bg);border-radius:0 0 20px 20px;overflow:hidden">${tabBar('screen', { connected: true, badgeMore: true })}</div>
    <div class="use" style="margin-top:10px;max-width:760px">Selected tab: its icon in a ring, in that tab's accent. Home's icon is the car: outline = not connected, filled = connected (no dot). A dot on a tab means "needs attention" only (amber).</div>`;

  const motion = `<ul class="notes">
    <li><b>Pages</b>: shared axis X, 300 ms, emphasized easing (cubic-bezier(.2,0,0,1)); Back runs it in reverse and follows the predictive-back gesture.</li>
    <li><b>Tabs</b>: fade through, 200 ms; each tab keeps its stack and scroll.</li>
    <li><b>Sheets</b>: rise 280 ms, scrim fades to 66 %; drag down or Back closes.</li>
    <li><b>Rings</b>: progress sweeps clockwise from 12 o'clock; waiting rings breathe (scale 1 → 1.06, opacity .25 → 0, 1.6 s loop); connected ring draws once, 600 ms.</li>
    <li><b>Switches</b>: thumb 150 ms + light haptic. Reorder lift: 8 dp shadow + accent edge + haptic.</li>
    <li><b>Changes</b>: the live car picture cross-fades 200 ms on every setting change, so the change is seen, not explained.</li>
    <li>Respect "Remove animations": every motion falls back to an instant cut or a 100 ms fade.</li>
  </ul>`;

  const body = `<div class="ss">
    <header><div style="display:flex;gap:16px;align-items:center">${mark(48)}<h1>MyPlay phone app · style sheet</h1></div>
    <p>Dark only. A sibling of ORBIT, not a copy: its rounds, rings, calm glass and world colours, set in plain lists a phone can scan.</p></header>
    <section><h2>Colour</h2>${colours}</section>
    <div class="cols2"><section><h2>Type scale</h2>${typeHtml}</section><section><h2>Space and shape</h2>${spaceHtml}<h2 style="margin-top:30px">Motion</h2>${motion}</section></div>
    <section><h2>Components</h2>${comps}</section>
  </div>`;
  return P.doc('MyPlay phone app — style sheet', body, 1600, `
    .ss{padding:56px 64px 72px} header h1{font-size:42px;font-weight:650;letter-spacing:-1px} header p{color:var(--text2);font-size:19px;margin:12px 0 8px}
    section{margin-top:44px} h2{font-size:26px;font-weight:640;letter-spacing:-.4px;margin-bottom:20px;padding-bottom:12px;border-bottom:1px solid var(--edge)}
    h3{font-size:15px;font-weight:650;color:var(--text2);margin-bottom:14px}
    .cols3{display:grid;grid-template-columns:1fr 1fr 1fr;gap:48px} .cols2{display:grid;grid-template-columns:1fr 1fr;gap:64px}
    .swatch{display:flex;gap:14px;align-items:center;margin-bottom:12px} .chip{width:56px;height:56px;border-radius:50%;border:1px solid var(--edge2);flex:none}
    .swatch b{font-size:15px} code{font-family:ui-monospace,Menlo,monospace;font-size:13px;color:var(--text2)} .use{font-size:13.5px;color:var(--text3);line-height:19px} .use.c{text-align:center;margin-top:8px}
    .cr{font-size:12.5px;color:var(--text2);margin-top:2px}
    table.type{border-collapse:collapse;width:100%} table.type td{padding:9px 0;border-bottom:1px solid var(--edge);vertical-align:middle} table.type td:first-child{width:200px}
    .notes{padding-left:18px;color:var(--text2);font-size:15px;line-height:23px} .notes li{margin-bottom:6px} .notes b{color:var(--text)}
  `);
};
