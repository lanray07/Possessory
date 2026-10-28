// Local-only composition: Chromium shapes Arabic/Indic text and supplies CJK font fallback.
// The application image is an unchanged real-device capture supplied by screenshot_pack.py.
const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require(require.resolve('playwright', {
  paths: [process.env.POSSESSORY_NODE_MODULES || path.join(__dirname, 'node_modules')],
}));
const escape = value => String(value).replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;');

(async () => {
  const input = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
  const browser = await chromium.launch({ headless: true,
    ...(process.env.POSSESSORY_CHROMIUM ? {executablePath:process.env.POSSESSORY_CHROMIUM} : {}) });
  try {
    const page = await browser.newPage({ viewport: { width: 1920, height: 1200 }, deviceScaleFactor: 1 });
    await page.route('**/*', route => route.abort());
    const rtl = input.locale === 'ar';
    for (const entry of input.entries) {
      const image = fs.readFileSync(entry.capture).toString('base64');
      await page.setContent(`<!doctype html><html lang="${escape(input.locale)}" dir="${rtl?'rtl':'ltr'}"><meta charset="utf-8"><style>
        *{box-sizing:border-box}body{margin:0;background:#F8F6F1;color:#24344A}
        header{position:absolute;top:34px;left:80px;right:80px}
        h1{font:54px/1.25 Georgia,'Yu Gothic','Nirmala UI','Segoe UI',sans-serif;margin:0;white-space:nowrap}
        p{font:26px/1.4 'Segoe UI','Yu Gothic','Nirmala UI',sans-serif;color:#546559;margin:14px 0 0;white-space:nowrap}
        img{position:absolute;top:210px;left:80px;width:1760px;height:940px;object-fit:contain}
        footer{position:absolute;bottom:8px;left:80px;font:26px 'Segoe UI',sans-serif;color:#8D522F;direction:ltr}
        </style><header><h1>${escape(entry.headline)}</h1><p>${escape(entry.caption)}</p></header>
        <img alt="" src="data:image/png;base64,${image}">${input.draft?'<footer>DRAFT — NOT APPROVED FOR PUBLICATION</footer>':''}</html>`);
      await page.evaluate(() => document.fonts.ready);
      await page.locator('img').evaluate(img => img.decode());
      const fits = await page.evaluate(() => ['h1','p'].every(tag => {
        const e=document.querySelector(tag);return e.scrollWidth <= e.clientWidth && e.getBoundingClientRect().bottom < 200;
      }));
      if (!fits) throw new Error(`Text overflows: ${entry.output}`);
      await page.screenshot({ path: entry.output });
    }
  } finally { await browser.close(); }
})().catch(error => { console.error(error.message);process.exitCode=1; });
