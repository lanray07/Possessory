"""Compose real device captures into Amazon-sized marketing images.

Requires a captures/manifest.json mapping every screen to {file, locale, device}.
Never invents application UI. Locale approval is mandatory unless --draft is supplied.
"""
import argparse, json, hashlib
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parents[1]
SCREENS=['hero','rooms','warranties','ocr','reports','voice','offline','reminders']
def main():
    p=argparse.ArgumentParser();p.add_argument('--locale',required=True);p.add_argument('--captures',type=Path,required=True);p.add_argument('--output',type=Path);p.add_argument('--draft',action='store_true');p.add_argument('--asset-review',type=Path);a=p.parse_args()
    status=json.loads((ROOT/'localization/status.json').read_text(encoding='utf-8'))
    metadata=ROOT/'store'/f'{a.locale}.json'
    if not metadata.exists():metadata=ROOT/'localization/generated'/('values-b+'+a.locale.replace('-','+'))/'store.json'
    store=json.loads(metadata.read_text(encoding='utf-8'))
    manifest=json.loads((a.captures/'manifest.json').read_text(encoding='utf-8'))
    if not a.draft and not status[a.locale]['release_approved']:
        assert a.asset_review,'Locale or scoped asset approval is required'
        review=json.loads(a.asset_review.read_text(encoding='utf-8'))
        policy=json.loads((ROOT/'localization/release-policy.json').read_text(encoding='utf-8'))
        assert policy['independent_human_review_required'] is False
        assert review['approved'] and review['scope']=='marketing_only' and review['locale']==a.locale=='en-GB'
        assert review['store_copy_sha256']==hashlib.sha256(metadata.read_bytes()).hexdigest()
        for screen in SCREENS:
            assert review['captures_sha256'][screen]==hashlib.sha256((a.captures/manifest[screen]['file']).read_bytes()).hexdigest()
    for screen in SCREENS:
        record=manifest[screen]
        assert record['locale']==a.locale,'Capture locale must match marketing text'
        assert record['device'] in ['fire-tablet','android-phone'],'Use a real-device capture'
        path=(a.captures/record['file']).resolve();assert a.captures.resolve() in path.parents
        assert path.is_file(),f'Missing capture {screen}'
    output=a.output or ROOT/'artifacts/screenshots'/a.locale;output.mkdir(parents=True,exist_ok=True)
    title=ImageFont.truetype('C:/Windows/Fonts/georgia.ttf',54);body=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',26)
    # Arabic and Indic compositions need a shaped font/rendering path and are intentionally blocked.
    assert a.locale not in ['ar','hi-IN'],'Shaping, RTL and font QA required before screenshot composition'
    for number,screen in enumerate(SCREENS,1):
        image=Image.new('RGB',(1920,1200),'#F8F6F1');d=ImageDraw.Draw(image)
        headline=store[screen+'_headline'];caption=store[screen+'_caption']
        assert d.textlength(headline,font=title)<1760,'Headline requires a reviewed layout adjustment'
        assert d.textlength(caption,font=body)<1760,'Caption requires a reviewed layout adjustment'
        d.text((80,40),headline,font=title,fill='#24344A');d.text((80,116),caption,font=body,fill='#546559')
        capture=Image.open(a.captures/manifest[screen]['file']).convert('RGB');capture.thumbnail((1760,940),Image.Resampling.LANCZOS)
        image.paste(capture,((1920-capture.width)//2,210))
        if a.draft:d.text((80,1160),'DRAFT — NOT APPROVED FOR PUBLICATION',font=body,fill='#8D522F')
        image.save(output/f'{number:02}-{screen}.png')
    print(output)
if __name__=='__main__':main()
