"""Package actual Fire captures with provenance and optional scoped English asset approval."""
import hashlib
import argparse
import json
import subprocess
import sys
import zipfile
from datetime import datetime, timezone
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
SCREENS = ['hero', 'rooms', 'warranties', 'ocr', 'reports', 'voice', 'offline', 'reminders']

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--version',default='0.3.2')
    parser.add_argument('--test-apk',type=Path)
    parser.add_argument('--asset-review',type=Path)
    args=parser.parse_args()
    assert all(c.isdigit() or c=='.' for c in args.version)
    captures = ROOT / f'artifacts/marketing-captures-{args.version}'
    output = ROOT / f'artifacts/marketing-{args.version}-en-GB-{"reviewed" if args.asset_review else "draft"}'
    proof = json.loads((captures / 'evidence.json').read_text(encoding='utf-8'))
    assert proof['screens'] == 8 and proof['sandboxPremium'] and proof['offlineActiveNetworkNull']
    manifest = {}
    for name in SCREENS:
        path = captures / f'{name}.png'
        with Image.open(path) as image:
            assert image.size == (1920, 1200)
        manifest[name] = dict(file=path.name, locale='en-GB', device='fire-tablet', sha256=digest(path))
    (captures / 'manifest.json').write_text(json.dumps(manifest, indent=2)+'\n', encoding='utf-8', newline='\n')
    approval_args=['--asset-review',str(args.asset_review)] if args.asset_review else ['--draft']
    subprocess.run([sys.executable, str(ROOT/'tools/screenshot_pack.py'), '--locale', 'en-GB',
                    '--captures', str(captures), '--output', str(output), *approval_args], check=True)
    exports = []
    contact = Image.new('RGB', (1280, 1600), '#F8F6F1')
    for i, name in enumerate(SCREENS):
        path = output / f'{i+1:02}-{name}.png'
        with Image.open(path) as image:
            assert image.size == (1920, 1200) and image.mode == 'RGB'
            preview = image.resize((640, 400), Image.Resampling.LANCZOS)
            contact.paste(preview, ((i%2)*640, (i//2)*400))
        exports.append(dict(file=path.name, sha256=digest(path), size=[1920,1200]))
    contact.save(output/'contact-sheet.jpg', quality=92)
    provenance = dict(recorded_at=datetime.now(timezone.utc).isoformat(), version=args.version,
        package='app.possessory.iapqa', device='Amazon KFTRWI', locale='en-GB',
        apk_sha256=digest(ROOT/f'artifacts/Possessory-{args.version}-iapqa-TEST-ONLY.apk'),
        test_apk_sha256=digest(args.test_apk or ROOT/f'artifacts/Possessory-{args.version}-iapqa-tests-TEST-ONLY.apk'),
        test_source_sha256=digest(ROOT/'app/src/androidTestAmazon/java/app/possessory/MarketingCaptureTest.kt'),
        approved=bool(args.asset_review), approval_scope='English marketing assets only', application_release_approved=False,
        uploaded=False, captures=manifest, exports=exports, evidence=proof,
        limitations=['Real app navigation in sandbox build, not production/LAT certification.',
                     'Fictional demo records; receipt review shown, not OCR accuracy certification.',
                     'Fire has no speech recognizer; screenshot shows actual typed fallback.',
                     'English assets only; application release approval and other locale/device variants remain separate. Independent human review is waived.'])
    if args.asset_review: provenance['asset_review']=json.loads(args.asset_review.read_text(encoding='utf-8'))
    (output/'manifest.json').write_text(json.dumps(provenance,indent=2)+'\n',encoding='utf-8',newline='\n')
    archive = ROOT/f'artifacts/Possessory-{args.version}-eight-marketing-screenshots-{"ENGLISH" if args.asset_review else "DRAFT"}.zip'
    with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
        for path in sorted(output.iterdir()): z.write(path,'marketing/'+path.name)
        for path in sorted(captures.iterdir()): z.write(path,'raw-captures/'+path.name)
    with zipfile.ZipFile(archive) as z: assert z.testzip() is None
    print(archive)

if __name__ == '__main__':
    main()
