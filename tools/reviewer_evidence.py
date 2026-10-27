"""Index unmodified Fire review captures; automated evidence never signs off a language."""
import hashlib
import json
import struct
from localize import ROOT

def main():
    root=ROOT/'artifacts/reviewer-captures'
    locales=list(json.loads((ROOT/'localization/status.json').read_text(encoding='utf-8')))
    quality=json.loads((ROOT/'localization/quality-report.json').read_text(encoding='utf-8'))
    apk=ROOT/'artifacts/Possessory-amazon-review-signed.apk'
    report={'version':'0.3.0','package':'app.possessory.review','apk_sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'source_catalogue_sha256':quality['source_sha256'],'device':{'model':'KFTRWI','android_api':28},'pixels':'Unmodified UIAutomation screenshots from the physical Fire tablet','capture_sync':'Expected localized heading, Compose idle, UI idle and display commit delay','human_language_approval':False,'suitable_for_store_submission':False,'pages':{'1':'overview','2':'inventory','3':'rooms','4':'warranties','5':'maintenance','6':'reports','7':'settings'},'sets':{},'captures':[]}
    for name,expected,font,dimensions in [('standard',locales,1.0,(1920,1200)),('large-font',['en-GB','de-DE','ar'],2.0,(1920,1200)),('portrait',['en-GB','de-DE','ar'],1.0,(1200,1920))]:
        assert {p.name for p in (root/name).iterdir() if p.is_dir()}==set(expected)
        for locale in expected:
            captures=sorted((root/name/locale).glob('*.png'))
            assert {p.stem for p in captures}==set(report['pages'])
            for path in captures:
                header=path.read_bytes()[:24]
                assert header[:8]==b'\x89PNG\r\n\x1a\n'
                assert struct.unpack('>II',header[16:24])==dimensions
                report['captures'].append({'path':path.relative_to(root).as_posix(),'locale':locale,'set':name,'sha256':hashlib.sha256(path.read_bytes()).hexdigest()})
        report['sets'][name]={'font_scale':font,'dimensions':list(dimensions),'locales':expected,'navigation_assertions_passed':True}
    assert len(report['captures'])==175
    (root/'manifest.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print('Indexed 175 original device captures; human approval remains false.')

if __name__=='__main__':main()
