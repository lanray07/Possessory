"""Package source/translation pairs and explicit sign-off fields for human review."""
import csv
import hashlib
import io
import json
import zipfile
import argparse
from pathlib import Path
from localize import ROOT, extract


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--output',type=Path,default=ROOT/'artifacts/Possessory-localization-review.zip')
    parser.add_argument('--captures',type=Path,default=ROOT/'artifacts/reviewer-captures')
    parser.add_argument('--version',default='0.3.0')
    parser.add_argument('--reference-captures',type=Path,help='Older screenshots, labelled as historical references only')
    parser.add_argument('--review-apk',type=Path,help='Current signed review APK to identify by digest; distributed separately')
    args=parser.parse_args()
    source=extract()
    status=json.loads((ROOT/'localization/status.json').read_text(encoding='utf-8'))
    quality=json.loads((ROOT/'localization/quality-report.json').read_text(encoding='utf-8'))
    output=args.output
    source_hash=hashlib.sha256(json.dumps(source,sort_keys=True).encode()).hexdigest()
    instructions=(
        'POSSESSORY HUMAN LOCALIZATION REVIEW\n\n'
        'Every locale is a draft. No reviewer or approval has been fabricated.\n'
        'Open the UTF-8 CSV for your locale. Check every English source and draft translation.\n'
        'Put corrected wording in reviewed_translation, your name in reviewer, and pass/fix/reject in decision.\n'
        'Review app strings, plurals, voice phrases, support/privacy text and store/IAP metadata.\n'
        'Keep placeholder tokens (%1$s, %d), brand name, technical identifiers and date patterns intact.\n'
        'User-entered item names and receipt contents must never be translated automatically.\n'
        'Check regional spelling, currency/date presentation, plural grammar, RTL and text expansion.\n'
        'Use the separate .review application for on-device review; it is not the store release.\n'
        'Its evaluation access does not simulate a purchase or grant an Amazon entitlement.\n'
        'Record tested device, locale, font scale, orientation, speech availability and remaining defects.\n'
        'Return completed CSVs and device review notes. Release approval is a separate owner decision.\n'
        'A changed source hash invalidates a previous sign-off for changed messages.\n'
    )
    if not (args.captures/'manifest.json').exists():
        instructions+='\nCURRENT DEVICE SCREENSHOTS ARE PENDING. Inspect the current review APK. Any previous-version screenshots are historical layout references, not evidence that the updated text was checked on-device.\n'
    with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as archive:
        archive.writestr('READ-ME.txt',instructions)
        archive.write(ROOT/'localization/glossary.csv','glossary.csv')
        archive.write(ROOT/'localization/quality-report.json','automated-coverage.json')
        editorial=ROOT/'localization/ai-editorial-review.json'
        if editorial.exists():archive.write(editorial,'ai-editorial-review.json')
        evidence=args.captures/'manifest.json'
        if evidence.exists():
            recorded=json.loads(evidence.read_text(encoding='utf-8'))
            assert recorded['source_catalogue_sha256']==source_hash,'Device evidence must match the current catalogue'
            assert recorded['version']==args.version,'Device evidence must match the selected version'
            archive.write(evidence,'device-capture-evidence.json')
        archive.write(ROOT/'docs/privacy.md','operator-privacy-review.md')
        if args.reference_captures:
            historical=json.loads((args.reference_captures/'manifest.json').read_text(encoding='utf-8'))
            archive.writestr('historical-device-capture-evidence.json',json.dumps(historical,indent=2))
            for capture in historical['captures']:
                path=(args.reference_captures/capture['path']).resolve()
                assert path.is_relative_to(args.reference_captures.resolve())
                assert hashlib.sha256(path.read_bytes()).hexdigest()==capture['sha256']
                archive.write(path,'historical-layout-reference/'+capture['path'])
        for page in sorted((ROOT/'docs/site').glob('*.html')):archive.write(page,'website/'+page.name)
        application={'file':args.review_apk.name,'sha256':hashlib.sha256(args.review_apk.read_bytes()).hexdigest()} if args.review_apk else None
        archive.writestr('source-manifest.json',json.dumps({'version':args.version,'review_application':application,'source_sha256':source_hash,'messages':len(source),'human_review_complete':False,'current_device_capture_evidence_present':evidence.exists(),'historical_references_included':bool(args.reference_captures),'locales':list(status)},indent=2))
        for locale in status:
            folder='values-b+'+locale.replace('-','+')
            generated=ROOT/'localization/generated'/folder
            import xml.etree.ElementTree as ET
            tree=ET.parse(generated/'strings.xml');values={e.attrib['name']:e.text or '' for e in tree.findall('string')}
            for plural in tree.findall('plurals'):
                for e in plural:values[f"plural:{plural.attrib['name']}:{e.attrib['quantity']}"]=e.text or ''
            for kind in ['store','support']:
                values.update({kind+':'+k:v for k,v in json.loads((generated/(kind+'.json')).read_text(encoding='utf-8')).items() if isinstance(v,str)})
            stream=io.StringIO(newline='');writer=csv.writer(stream)
            writer.writerow(['locale','key','source_english','draft_translation','reviewed_translation','reviewer','decision','comment'])
            fragments=quality['locales'][locale].get('possible_untranslated_fragments',{})
            for key,value in source.items():writer.writerow([locale,key,value,values.get(key,''),'','','','Possible untranslated source fragment: verify and correct in context.' if key in fragments else ''])
            for key,value in values.items():
                if key.startswith('plural:') and key not in source:writer.writerow([locale,key,source.get(key.rsplit(':',1)[0]+':other',''),value,'','','','Review this locale-specific plural category.'])
            archive.writestr(locale+'/review.csv','\ufeff'+stream.getvalue())
            archive.writestr(locale+'/device-review.json',json.dumps({'locale':locale,'source_sha256':source_hash,'reviewer':None,'devices':[],'rtl_checked':False,'font_scaling_checked':False,'voice_commands_checked':False,'release_approved':False},indent=2))
            site=ROOT/'docs/site'/(locale+'.html')
            if site.exists():archive.writestr(locale+'/website-review.txt',f'Open ../website/{locale}.html for the draft marketing, support and privacy page. All wording requires human review.\n')
            for capture_set in ('standard','large-font','portrait'):
                captures=args.captures/capture_set/locale
                if captures.exists():
                    for capture in sorted(captures.glob('*.png')):archive.write(capture,locale+'/device-screens/'+capture_set+'/'+capture.name)
    print(output)


if __name__=='__main__':main()
