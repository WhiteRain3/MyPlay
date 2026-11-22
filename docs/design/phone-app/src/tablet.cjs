// One large-screen frame (plan §11): navigation rail + list-detail, landscape 1280 x 800 dp.
const P = require('./parts.cjs');
const { I, row, card, tag, btn, carPic } = P;
module.exports = function tablet() {
  const rail = [['house', 'Home'], ['monitor', 'Screen'], ['car', 'Car', 1], ['layout-grid', 'Apps'], ['ellipsis', 'More']]
    .map(([i, n, on]) => `<div style="display:flex;flex-direction:column;align-items:center;gap:4px;height:72px;justify-content:center"><div class="ring" style="width:48px;height:40px;border-radius:20px;display:grid;place-items:center;${on ? 'background:color-mix(in srgb,var(--a-car) 16%,transparent);color:var(--a-car);border:2px solid var(--a-car)' : 'color:var(--text2)'}">${I(i, 22)}</div><span style="font-size:12px;${on ? 'color:var(--text);font-weight:650' : 'color:var(--text3)'}">${n}</span></div>`).join('');
  const list = card([
    row({ ic: 'circle-dot', label: 'Buttons', value: '2 changed', changed: true, cls: 'selrow' }), row({ ic: 'map', label: 'Maps', value: 'Last used' }), row({ ic: 'gauge', label: 'Speed', value: 'Auto · Northvale' }),
    row({ ic: 'volume-2', label: 'Sound', value: 'Shizuku or root' }), row({ ic: 'message-circle', label: 'Messages and calls', value: 'Banners on' }), row({ ic: 'mic', label: 'Voice assistant', value: 'Same as the phone' }),
  ].join(''));
  const detail = `<div style="font-size:22px;font-weight:620;margin:6px 0 16px">Buttons</div>
    ${card(`<div style="padding:12px;display:flex;justify-content:center">${require('./screens.cjs').controls('knob')}</div>`)}
    <div class="sec" style="padding-left:4px">Knob push</div>${card([row({ label: 'Press', value: 'Press what has the ring', style: 'min-height:56px' }), row({ label: 'Press and hold', value: '—', cls: 'off', nochev: true, tag: tag('Needs holds', 'mute'), style: 'min-height:56px' }), row({ label: 'Double press', value: 'Next song', style: 'min-height:56px' }), row({ label: 'Triple press', value: 'Previous song', style: 'min-height:56px' })].join(''))}`;
  const body = `<div class="t-car" style="width:1280px;height:800px;display:flex;background:var(--bg)">
    <nav style="width:96px;border-right:1px solid var(--edge);display:flex;flex-direction:column;align-items:center;padding-top:24px;gap:8px">${P.mark(36)}<div style="height:24px"></div>${rail}</nav>
    <section style="width:420px;padding:20px 16px;border-right:1px solid var(--edge)"><div class="top" style="padding:0 0 0 4px"><div class="tglyph">${I('car', 18, 2.2)}</div><h1 style="margin-left:6px">Car</h1><div class="iconbtn">${I('search', 22)}</div></div>${list}</section>
    <section style="flex:1;padding:20px 32px">${detail}</section></div>`;
  return P.doc('MyPlay — large screen', body, 1280, '.selrow{background:rgba(220,227,239,.08)}');
};
