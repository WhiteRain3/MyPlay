// Builds every screen's HTML into ../html and renders each to ../<NN-slug>.png (412 x 915 dp at 2x;
// "tall" screens are the whole scrolled page). Also the large-screen frame, the style sheet, the
// overview and index.md's screen table. Run: node docs/design/phone-app/src/build.cjs
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');
const P = require('./parts.cjs');
const screens = require('./screens.cjs');
const styleSheet = require('./stylesheet.cjs');
const tablet = require('./tablet.cjs');

const OUT = path.join(__dirname, '..');
const HTML = path.join(OUT, 'html');
for (const f of fs.readdirSync(OUT)) if (/^\d\d-.*\.png$/.test(f)) fs.unlinkSync(path.join(OUT, f));
fs.rmSync(HTML, { recursive: true, force: true });
fs.mkdirSync(HTML, { recursive: true });

const n2 = (i) => String(i).padStart(2, '0');
screens.forEach((s, i) => { s.file = `${n2(i + 1)}-${s.slug}`; });
const extra = [
  { file: `${n2(screens.length + 1)}-large-screen`, title: 'Large screen — rail and list-detail', desc: 'Tablet or unfolded phone: a navigation rail, the tab root on the left, the page on the right. Same routes as the phone.' },
  { file: `${n2(screens.length + 2)}-style-sheet`, title: 'Style sheet', desc: 'Colours with computed contrast (text and non-text), type, space and shape, components, motion.' },
];

(async () => {
  const browser = await chromium.launch();
  const shot = async (file, html, w, h, full) => {
    fs.writeFileSync(path.join(HTML, file + '.html'), html);
    const page = await browser.newPage({ viewport: { width: w, height: h }, deviceScaleFactor: 2 });
    await page.goto('file://' + path.join(HTML, file + '.html'));
    await page.evaluate(() => document.fonts.ready);
    await page.screenshot({ path: path.join(OUT, file + '.png'), fullPage: !!full });
    await page.close();
  };
  for (const s of screens) await shot(s.file, P.doc(`MyPlay — ${s.title}`, s.html), 412, 915, s.tall);
  await shot(extra[0].file, tablet(), 1280, 800);
  await shot(extra[1].file, styleSheet(), 1600, 1000, true);
  const all = [...screens, ...extra];
  const cards = all.map((s) => `<figure><img src="../${s.file}.png"><figcaption><b>${s.file.slice(0, 2)}</b> ${s.title}</figcaption></figure>`).join('');
  await shot('00-overview', P.doc('MyPlay phone app — overview', `<div class="ov"><header><div style="display:flex;gap:14px;align-items:center">${P.mark(40)}<h1>MyPlay phone app · screens</h1></div><p>Dark only · 412 × 915 dp · Home, Screen, Car, Apps, More · world colours mark identity, one neutral colour marks interaction</p></header><div class="grid">${cards}</div></div>`, 2600,
    `.ov{padding:48px 56px 64px} header h1{font-size:40px;font-weight:650;letter-spacing:-1px} header p{color:var(--text2);font-size:18px;margin:10px 0 40px}
     .grid{display:grid;grid-template-columns:repeat(8,1fr);gap:40px 24px;align-items:start} figure img{width:100%;border-radius:18px;border:1px solid var(--edge2);display:block}
     figcaption{font-size:14px;color:var(--text2);margin-top:10px;line-height:19px} figcaption b{color:var(--text);margin-right:6px}`), 2600, 1000, true);
  // index.md: replace the generated table between the markers.
  const idx = path.join(OUT, 'index.md');
  const table = ['| # | File | What it shows |', '|---|---|---|', ...all.map((s) => `| ${s.file.slice(0, 2)} | [\`${s.file}.png\`](${s.file}.png) | **${s.title}.** ${s.desc}${s.tall ? ' *(whole scrolled page)*' : ''} |`)].join('\n');
  const md = fs.readFileSync(idx, 'utf8').replace(/<!-- screens -->[\s\S]*<!-- \/screens -->/, `<!-- screens -->\n${table}\n<!-- /screens -->`);
  fs.writeFileSync(idx, md);
  console.log('built', all.length, 'screens');
  await browser.close();
})().catch((e) => { console.error(e); process.exit(1); });
