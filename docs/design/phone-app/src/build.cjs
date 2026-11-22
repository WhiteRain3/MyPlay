// Builds every screen's HTML into ../html and renders each to ../<name>.png (412 x 915 dp at 2x).
// Run: node docs/design/phone-app/src/build.cjs   (Chromium + Playwright preinstalled)
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');
const P = require('./parts.cjs');
const screens = require('./screens.cjs');
const styleSheet = require('./stylesheet.cjs');

const OUT = path.join(__dirname, '..');
const HTML = path.join(OUT, 'html');
fs.mkdirSync(HTML, { recursive: true });

(async () => {
  const only = process.argv[2];
  const browser = await chromium.launch();
  const shot = async (file, html, w, h, full) => {
    fs.writeFileSync(path.join(HTML, file + '.html'), html);
    const page = await browser.newPage({ viewport: { width: w, height: h }, deviceScaleFactor: 2 });
    await page.goto('file://' + path.join(HTML, file + '.html'));
    await page.evaluate(() => document.fonts.ready);
    await page.screenshot({ path: path.join(OUT, file + '.png'), fullPage: !!full });
    await page.close();
  };
  for (const s of screens) {
    if (only && !s.file.startsWith(only)) continue;
    await shot(s.file, P.doc(`MyPlay — ${s.title}`, s.html), 412, 915);
    console.log('ok', s.file);
  }
  if (!only || only === '31') {
    await shot('31-style-sheet', styleSheet(), 1600, 1000, true);
    console.log('ok style sheet');
  }
  if (!only || only === '00') {
    const cards = screens.map((s) => `<figure><img src="../${s.file}.png"><figcaption><b>${s.file.slice(0, 2)}</b> ${s.title}</figcaption></figure>`).join('');
    const html = P.doc('MyPlay phone app — overview', `<div class="ov"><header><div style="display:flex;gap:14px;align-items:center">${P.mark(40)}<h1>MyPlay phone app · screens</h1></div><p>Dark only · 412 × 915 dp · five tabs: Home, Screen, Car, Apps, More · one accent per tab, from ORBIT's world colours</p></header><div class="grid">${cards}</div></div>`, 2200,
      `.ov{padding:48px 56px 64px} header h1{font-size:40px;font-weight:650;letter-spacing:-1px} header p{color:var(--text2);font-size:18px;margin:10px 0 40px}
       .grid{display:grid;grid-template-columns:repeat(6,1fr);gap:40px 28px} figure img{width:100%;border-radius:22px;border:1px solid var(--edge2);display:block}
       figcaption{font-size:15px;color:var(--text2);margin-top:12px;line-height:20px} figcaption b{color:var(--text);margin-right:6px}`);
    await shot('00-overview', html, 2200, 1000, true);
    console.log('ok overview');
  }
  await browser.close();
})().catch((e) => { console.error(e); process.exit(1); });
