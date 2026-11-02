"""Compose eight real-device marketing views per locale with immutable provenance."""
import argparse
import hashlib
import json
import subprocess
import sys
import zipfile
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
SCREENS=['hero','rooms','warranties','ocr','reports','voice','offline','reminders']
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    p=argparse.ArgumentParser()
    p.add_argument('--version',required=True)
    p.add_argument('--captures',type=Path,required=True)
    p.add_argument('--apk',type=Path,required=True)
    p.add_argument('--test-apk',type=Path,required=True)
    p.add_argument('--approved',action='store_true')
    a=p.parse_args()
    assert all(c.isdigit() or c=='.' for c in a.version)
    status=json.loads((ROOT/'localization/status.json').read_text(encoding='utf-8'))
    output=ROOT/'artifacts'/f'marketing-{a.version}-localized'
    output.mkdir(parents=True,exist_ok=True)
    provenance={'version':a.version,'package':'app.possessory.iapqa','device':'Amazon KFTRWI',
        'apk_sha256':sha(a.apk),'test_apk_sha256':sha(a.test_apk),
        'test_source_sha256':sha(ROOT/'app/src/androidTestAmazon/java/app/possessory/MarketingCaptureTest.kt'),
        'native_capture_script_sha256':sha(ROOT/'tools/capture_marketing_fire.py'),
        'approved':a.approved,'approval_scope':'Marketing assets only','uploaded':False,
        'limits':['Sandbox entitlement; not LAT certification.','Fictional English demo records are preserved as user-entered data.','The receipt image is synthetic; this does not certify real receipt accuracy.','Fire has no speech recognizer; the actual typed fallback is shown.'],
        'locales':{}}
    for locale in status:
        captures=a.captures/locale
        evidence=json.loads((captures/'evidence.json').read_text(encoding='utf-8'))
        assert evidence['locale']==locale and evidence['screens']==8
        assert evidence['sandboxPremium'] and evidence['offlineActiveNetworkNull'] and evidence['realOcrTextVerified']
        assert 'OK (1 test)' in (captures/'capture-log.txt').read_text(encoding='utf-8-sig')
        manifest={}
        for screen in SCREENS:
            path=captures/f'{screen}.png'
            with Image.open(path) as image: assert image.size==(1920,1200)
            manifest[screen]={'file':path.name,'locale':locale,'device':'fire-tablet','sha256':sha(path)}
        (captures/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8',newline='\n')
        args=[sys.executable,str(ROOT/'tools/screenshot_pack.py'),'--locale',locale,'--captures',str(captures),'--output',str(output/locale)]
        if a.approved: assert status[locale]['release_approved']
        else: args.append('--draft')
        subprocess.run(args,check=True)
        exports=[]
        for index,screen in enumerate(SCREENS,1):
            path=output/locale/f'{index:02}-{screen}.png'
            with Image.open(path) as image: assert image.size==(1920,1200) and image.mode=='RGB'
            exports.append({'file':path.name,'sha256':sha(path)})
        copy=ROOT/'localization/generated'/('values-b+'+locale.replace('-','+'))/'store.json'
        provenance['locales'][locale]={'captures':manifest,'exports':exports,'store_copy_sha256':sha(copy),'evidence':evidence}
    (output/'manifest.json').write_text(json.dumps(provenance,indent=2)+'\n',encoding='utf-8',newline='\n')
    archive=ROOT/'artifacts'/f'Possessory-{a.version}-152-localized-marketing-{"APPROVED" if a.approved else "DRAFT"}.zip'
    with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
        for path in sorted(output.rglob('*')):
            if path.is_file():z.write(path,path.relative_to(output).as_posix())
    with zipfile.ZipFile(archive) as z:assert z.testzip() is None
    print(archive)

if __name__=='__main__':main()
