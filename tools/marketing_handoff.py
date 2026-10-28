"""Package actual Fire captures as a provenance-bound, explicitly unapproved English draft."""
import hashlib
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
    captures = ROOT / 'artifacts/marketing-captures-0.3.2'
    output = ROOT / 'artifacts/marketing-0.3.2-en-GB-draft'
    proof = json.loads((captures / 'evidence.json').read_text(encoding='utf-8'))
    assert proof['screens'] == 8 and proof['sandboxPremium'] and proof['offlineActiveNetworkNull']
    manifest = {}
    for name in SCREENS:
        path = captures / f'{name}.png'
        with Image.open(path) as image:
            assert image.size == (1920, 1200)
        manifest[name] = dict(file=path.name, locale='en-GB', device='fire-tablet', sha256=digest(path))
    (captures / 'manifest.json').write_text(json.dumps(manifest, indent=2)+'\n', encoding='utf-8', newline='\n')
    subprocess.run([sys.executable, str(ROOT/'tools/screenshot_pack.py'), '--locale', 'en-GB',
                    '--captures', str(captures), '--output', str(output), '--draft'], check=True)
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
    provenance = dict(recorded_at=datetime.now(timezone.utc).isoformat(), version='0.3.2',
        package='app.possessory.iapqa', device='Amazon KFTRWI', locale='en-GB',
        apk_sha256=digest(ROOT/'artifacts/Possessory-0.3.2-iapqa-TEST-ONLY.apk'),
        test_apk_sha256=digest(ROOT/'artifacts/Possessory-0.3.2-iapqa-tests-TEST-ONLY.apk'),
        test_source_sha256=digest(ROOT/'app/src/androidTestAmazon/java/app/possessory/MarketingCaptureTest.kt'),
        approved=False, uploaded=False, captures=manifest, exports=exports, evidence=proof,
        limitations=['Real app navigation in sandbox build, not production/LAT certification.',
                     'Fictional demo records; receipt review shown, not OCR accuracy certification.',
                     'Fire has no speech recognizer; screenshot shows actual typed fallback.',
                     'English draft only; human language approvals and other device variants pending.'])
    (output/'manifest.json').write_text(json.dumps(provenance,indent=2)+'\n',encoding='utf-8',newline='\n')
    archive = ROOT/'artifacts/Possessory-0.3.2-eight-marketing-screenshots-DRAFT.zip'
    with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
        for path in sorted(output.iterdir()): z.write(path,'marketing/'+path.name)
        for path in sorted(captures.iterdir()): z.write(path,'raw-captures/'+path.name)
    with zipfile.ZipFile(archive) as z: assert z.testzip() is None
    print(archive)

if __name__ == '__main__':
    main()
