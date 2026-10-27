"""Build a static, tracking-free draft site from the same reviewed catalogues as the APK."""
import html
import json
import xml.etree.ElementTree as ET
from pathlib import Path
from localize import ROOT

STYLE='''*{box-sizing:border-box}body{margin:0;background:#f8f6f1;color:#24344a;font:18px/1.65 system-ui,sans-serif}main,header,footer{max-width:1080px;margin:auto;padding:28px}a{color:#24344a;text-underline-offset:4px}nav{display:flex;flex-wrap:wrap;gap:12px;font-size:14px}.notice{background:#fff1cd;padding:18px;border-radius:12px}h1{font:clamp(38px,6vw,68px)/1.1 Georgia,serif;max-width:850px}h2{font:32px/1.2 Georgia,serif}section{padding:24px 0;border-top:1px solid #d4d6d0}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:24px}article{padding:24px;background:white;border-radius:18px}img{max-width:100%;height:auto;border-radius:12px}footer{font-size:14px}summary{cursor:pointer;font-weight:600}button,a:focus-visible,summary:focus-visible{outline-color:#875c19}'''

def main():
    status=json.loads((ROOT/'localization/status.json').read_text(encoding='utf-8'))
    destination=ROOT/'docs/site';destination.mkdir(parents=True,exist_ok=True)
    e=lambda value:html.escape(str(value))
    for locale in status:
        source=ROOT/'localization/generated'/('values-b+'+locale.replace('-','+'))
        store=json.loads((source/'store.json').read_text(encoding='utf-8'))
        support=json.loads((source/'support.json').read_text(encoding='utf-8'))
        labels={node.attrib['name']:(node.text or '').replace("\\'","'").replace('\\n','\n').replace('\\"','"') for node in ET.parse(source/'strings.xml').findall('string')}
        headings={'getting_started':'welcome','where_is_my_data':'privacy','receipts':'receipts_documents','backup':'backup_restore','restore':'restore','reminders':'reminders','voice':'voice','reports':'reports','premium':'premium','warranties':'warranties','delete':'delete_all','app_lock':'app_lock'}
        nav=''.join(f'<a href="{code}.html" lang="{code}" hreflang="{code}">{code}</a>' for code in status)
        features=''.join(f'<article><h2>{e(store[key+"_headline"])}</h2><p>{e(store[key+"_caption"])}</p></article>' for key in ('rooms','warranties','ocr','reports','voice','offline','reminders'))
        faq=''.join(f'<section><details><summary>{e(labels[headings[key]])}</summary><p>{e(value)}</p></details></section>' for key,value in support.items() if key not in ('title','draft_notice','operator','contact_email','contact_label'))
        page=f'''<!doctype html><html lang="{locale}" dir="{'rtl' if locale=='ar' else 'ltr'}"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex,nofollow"><meta name="description" content="{e(store['short_description'])}"><title>{e(store['title'])}</title><style>{STYLE}</style><header><a href="index.html">POSSESSORY</a><nav aria-label="{e(labels['app_language'])}">{nav}</nav></header><main><p class="notice">{e(support['draft_notice'])}</p><h1>{e(store['hero_headline'])}</h1><p>{e(store['hero_caption'])}</p><p>{e(store['description'])}</p><div class="grid">{features}</div><section><h2>{e(store['iap_title'])}</h2><p>{e(store['iap_description'])}</p></section><section><h2>{e(support['title'])}</h2>{faq}</section><section><h2>{e(labels['privacy'])}</h2><p style="white-space:pre-line">{e(labels['privacy_body'])}</p><a href="https://github.com/lanray07/Possessory/blob/main/docs/privacy.md">{e(labels['privacy'])}</a></section></main><footer>O. Bankole · <a href="mailto:banksmi@mail.com">banksmi@mail.com</a></footer></html>'''
        (destination/(locale+'.html')).write_text(page,encoding='utf-8')
    (destination/'index.html').write_text('<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex,nofollow"><title>Possessory — language review</title><style>'+STYLE+'</style><main><h1>Possessory</h1><p>Draft marketing and support pages for language review. Not a production release.</p><nav>'+nav+'</nav></main></html>',encoding='utf-8')
    (destination/'README.txt').write_text('Generated with tools/support_site.py. All pages are drafts and noindex. No hosting deployment has been performed. All translations and page layouts require human review before publication.\n',encoding='utf-8')
    print('Built 19 draft marketing/support pages; publication and human review remain gated.')

if __name__=='__main__':main()
