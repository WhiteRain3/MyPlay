// Renders every screen in screens.cjs to ../<NN-slug>.png at the car's 1280 x 720 (1x: the PNG is the
// car's own pixels), keeps the HTML in ../html, and writes ../overview.png.
// Run: node docs/design/waze-screens/src/build.cjs
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');
const P = require('./parts.cjs');
const { SCREENS, NIGHT, DAY } = require('./screens.cjs');

const OUT = path.join(__dirname, '..');
const HTML = path.join(OUT, 'html');
for (const f of fs.readdirSync(OUT)) if (/\.png$/.test(f)) fs.unlinkSync(path.join(OUT, f));
fs.rmSync(HTML, { recursive: true, force: true });
fs.mkdirSync(HTML, { recursive: true });

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1280, height: 720 }, deviceScaleFactor: 1 });
  const files = [];
  for (const [i, s] of SCREENS.entries()) {
    s.file = s.fileName || `${String(i + 1).padStart(2, '0')}-${s.slug}`;
    const html = P.page(s.day ? DAY : NIGHT, s.html(s.day ? DAY : NIGHT));
    fs.writeFileSync(path.join(HTML, s.file + '.html'), html);
    await page.goto('file://' + path.join(HTML, s.file + '.html'));
    await page.evaluate(() => document.fonts.ready);
    await page.screenshot({ path: path.join(OUT, s.file + '.png') });
    files.push(s.file);
  }
  // overview: every screen at a quarter size, captioned
  const tiles = SCREENS.map((s) => `<figure><img src="../${s.file}.png"><figcaption>${s.file.slice(0, 2)} · ${P.esc(s.title)}</figcaption></figure>`).join('');
  const ov = `<!doctype html><meta charset="utf-8"><style>body{margin:0;background:#0B1318;font:16px Inter,sans-serif;color:#AAB3C8}
  main{display:grid;grid-template-columns:repeat(4,320px);gap:20px 16px;padding:24px}figure{margin:0}img{width:320px;height:180px;display:block;border-radius:8px}
  figcaption{margin-top:6px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}h1{color:#EEF1F7;font-size:22px;margin:24px 24px 0}</style>
  <h1>MyPlay · Waze screens in ORBIT</h1><main>${tiles}</main>`;
  fs.writeFileSync(path.join(HTML, 'overview.html'), ov);
  const op = await browser.newPage({ viewport: { width: 1392, height: 400 }, deviceScaleFactor: 1 });
  await op.goto('file://' + path.join(HTML, 'overview.html'));
  await op.screenshot({ path: path.join(OUT, 'overview.png'), fullPage: true });
  await browser.close();
  fs.writeFileSync(path.join(__dirname, 'screens.json'), JSON.stringify(SCREENS.map(({ file, title, group, template, fields, desc, look }) => ({ file, title, group, template, fields, desc, look })), null, 1));
  console.log(files.join('\n'));
})();
