"""Development-only translation. Credentials never enter the APK.

python tools/localize.py                 # extract and validate, no network
python tools/localize.py --translate    # local free engine; install requirements-localization.txt
python tools/localize.py --translate --provider deepl # optional development-only cloud provider
python tools/localize.py --bundle-preview # compile draft packs into debug APKs
python tools/localize.py --approve de-DE # requires recorded human reviewer
"""
import argparse, hashlib, json, os, re, urllib.request, shutil, subprocess, sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
CONFIG=json.loads((ROOT/'localization/config.json').read_text(encoding='utf-8'))
TOKENS=re.compile(r'%(?:\d+\$)?[dsf]|Possessory|\\n')

def placeholders(text): return sorted(TOKENS.findall(text))
def resource_folder(locale):
    # Android resource folders retain the historical Indonesian language code.
    return 'values-in-rID' if locale=='id-ID' else 'values-b+'+locale.replace('-','+')
def validate(source, translated):
    assert set(source)==set(translated), 'Missing/extra keys'
    for key,value in source.items():
        assert translated[key].strip(), f'Empty string {key}'
        assert placeholders(value)==placeholders(translated[key]), f'Placeholder mismatch: {key}'

def extract():
    tree=ET.parse(ROOT/'app/src/main/res/values/strings.xml')
    result={e.attrib['name']:e.text or '' for e in tree.findall('string')}
    for group in tree.findall('plurals'):
        for e in group:result[f"plural:{group.attrib['name']}:{e.attrib['quantity']}"]=e.text or ''
    store=json.loads((ROOT/'store/en-GB.json').read_text(encoding='utf-8'))
    result.update({f'store:{k}':v for k,v in store.items() if isinstance(v,str) and k not in {'operator','support_email'}})
    support=json.loads((ROOT/'support/en-GB.json').read_text(encoding='utf-8'))
    result.update({f'support:{k}':v for k,v in support.items() if k not in {'operator','contact_email'}})
    return result

def translate(text, target, key, cache):
    cachekey=hashlib.sha256((target+'\0'+text).encode()).hexdigest()
    if cachekey in cache:return cache[cachekey]
    # XML tags protect placeholders and brand spelling through the provider.
    matches=[]
    def protect(m):
        matches.append(m.group());return f'<x id="{len(matches)-1}">{m.group()}</x>'
    protected=TOKENS.sub(protect,text)
    body=json.dumps({'text':[protected], 'source_lang':'EN','target_lang':target,'tag_handling':'xml','ignore_tags':['x']}).encode()
    req=urllib.request.Request(CONFIG['endpoint'],data=body,headers={'Authorization':'DeepL-Auth-Key '+key,'Content-Type':'application/json'})
    with urllib.request.urlopen(req,timeout=30) as r: value=json.load(r)['translations'][0]['text']
    value=re.sub(r'<x\b[^>]*id="(\d+)"[^>]*>.*?</x>',lambda m:matches[int(m[1])],value,flags=re.S)
    assert placeholders(text)==placeholders(value),'Provider changed a placeholder'
    cache[cachekey]=value;return value

def resources(locale, data):
    folder='values-b+'+locale.replace('-','+')
    root=ET.Element('resources');plurals={}
    for key,value in data.items():
        if key.startswith(('store:','support:')):continue
        if key.startswith('plural:'):
            _,name,quantity=key.split(':')
            if name not in plurals:plurals[name]=ET.SubElement(root,'plurals',name=name)
            ET.SubElement(plurals[name],'item',quantity=quantity).text=value.replace("\\'", "'").replace("'", "\\'").replace('"','\\"')
        else:ET.SubElement(root,'string',name=key).text=value.replace("\\'", "'").replace("'", "\\'").replace('"','\\"')
    # CLDR categories with neutral/other fallback drafts; grammar remains a review requirement.
    extra={'ar':['zero','two','few','many'],'pl':['few','many'],'fr':['many'],'pt':['many'],'es':['many'],'it':['many']}.get(locale.split('-')[0],[])
    for group in plurals.values():
        quantities={e.attrib['quantity'] for e in group}
        other=next(e.text for e in group if e.attrib['quantity']=='other')
        for quantity in extra:
            if quantity not in quantities:ET.SubElement(group,'item',quantity=quantity).text=other
    forms_path=ROOT/'localization/plural-forms.json'
    if forms_path.exists():
        forms=json.loads(forms_path.read_text(encoding='utf-8')).get(locale.split('-')[0],{})
        for name,values in forms.items():
            if name not in plurals:continue
            for element in plurals[name]:
                if element.attrib['quantity'] in values:
                    value=values[element.attrib['quantity']]
                    assert placeholders(value)==placeholders(next(e.text for e in plurals[name] if e.attrib['quantity']=='other'))
                    element.text=value.replace("'", "\\'")
    path=ROOT/'localization/generated'/folder/'strings.xml';path.parent.mkdir(parents=True,exist_ok=True)
    ET.indent(root);ET.ElementTree(root).write(path,encoding='utf-8',xml_declaration=True)
    (path.parent/'metadata.json').write_text(json.dumps({'locale':locale,'source_sha256':hashlib.sha256((ROOT/'app/src/main/res/values/strings.xml').read_bytes()).hexdigest(),'strings_sha256':hashlib.sha256(path.read_bytes()).hexdigest()},indent=2),encoding='utf-8')
    # Generated files are staged here; only approved resources may be copied into app/src/main/res.
    store={k.removeprefix('store:'):v for k,v in data.items() if k.startswith('store:')}
    (path.parent/'store.json').write_text(json.dumps(store,ensure_ascii=False,indent=2),encoding='utf-8')
    support={k.removeprefix('support:'):v for k,v in data.items() if k.startswith('support:')}
    (path.parent/'support.json').write_text(json.dumps(support,ensure_ascii=False,indent=2),encoding='utf-8')
    metadata=json.loads((path.parent/'metadata.json').read_text(encoding='utf-8'))
    metadata['catalogue_sha256']=hashlib.sha256(json.dumps(extract(),sort_keys=True).encode()).hexdigest()
    for kind in ('store','support'):metadata[kind+'_sha256']=hashlib.sha256((path.parent/(kind+'.json')).read_bytes()).hexdigest()
    (path.parent/'metadata.json').write_text(json.dumps(metadata,indent=2),encoding='utf-8')

def bundle_resources(preview=False):
    status=json.loads((ROOT/'localization/status.json').read_text(encoding='utf-8'))
    selected=[locale for locale in CONFIG['locales'] if (status.get(locale,{}).get('bundled_in_preview') if preview else status.get(locale,{}).get('release_approved') and status.get(locale,{}).get('state')=='release_approved' and not status.get(locale,{}).get('issues'))]
    assert selected,'No translations meet the selected packaging policy'
    source=extract();app_source={k:v for k,v in source.items() if not k.startswith(('store:','support:'))}
    source_hash=hashlib.sha256((ROOT/'app/src/main/res/values/strings.xml').read_bytes()).hexdigest()
    catalogue_hash=hashlib.sha256(json.dumps(source,sort_keys=True).encode()).hexdigest()
    checked=[]
    for locale in selected:
        folder='values-b+'+locale.replace('-','+')
        path=ROOT/'localization/generated'/folder/'strings.xml'
        metadata=json.loads((path.parent/'metadata.json').read_text(encoding='utf-8'))
        assert metadata['locale']==locale and metadata['source_sha256']==source_hash,'Translations must be regenerated for the current source'
        assert metadata.get('catalogue_sha256')==catalogue_hash,'Store/support translations must match the current source'
        for kind in ('store','support'):assert metadata.get(kind+'_sha256')==hashlib.sha256((path.parent/(kind+'.json')).read_bytes()).hexdigest(),'Translated store/support files changed since validation'
        if not preview:assert status[locale].get('reviewed_source_sha256')==catalogue_hash,'Human review must match the complete current catalogue'
        assert metadata['strings_sha256']==hashlib.sha256(path.read_bytes()).hexdigest(),'Translation files changed since validation'
        tree=ET.parse(path);data={e.attrib['name']:e.text or '' for e in tree.findall('string')}
        for group in tree.findall('plurals'):
            for e in group:data[f"plural:{group.attrib['name']}:{e.attrib['quantity']}"]=e.text or ''
        assert set(app_source)<=set(data),'Missing bundled strings'
        for key in set(data)-set(app_source):
            parts=key.split(':')
            assert len(parts)==3 and parts[0]=='plural' and parts[2] in {'zero','one','two','few','many','other'} and f'plural:{parts[1]}:other' in app_source,'Unknown bundled key'
            assert placeholders(data[key])==placeholders(app_source[f'plural:{parts[1]}:other']),'Plural placeholder mismatch'
        validate(app_source,{key:data[key] for key in app_source})
        checked.append((locale,folder,path,metadata['strings_sha256']))
    target=ROOT/'app/src'/('preview' if preview else 'localized')
    manifest=target/'locales.properties';old=[]
    if manifest.exists():
        old=re.search(r'^locales=(.*)$',manifest.read_text(encoding='utf-8'),re.M).group(1).split(',')
    for locale in set(old)-set(selected):
        assert locale in CONFIG['locales']
        stale=target/'res'/resource_folder(locale)/'strings.xml'
        if stale.exists():stale.unlink()
    lines=['# Generated by tools/localize.py; no runtime translation service.',f'source.sha256={source_hash}','locales='+','.join(selected)]
    for locale,folder,path,digest in checked:
        destination=target/'res'/resource_folder(locale)/'strings.xml';destination.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(path,destination)
        obsolete=target/'res'/folder/'strings.xml'
        if locale=='id-ID':
            obsolete.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(path,obsolete)
            lines.append(f'alias.{locale}={folder}')
        elif obsolete!=destination and obsolete.exists():obsolete.unlink()
        lines.append(f'sha256.{locale}={digest}')
        lines.append(f'folder.{locale}={resource_folder(locale)}')
        lines.append(f'support.sha256.{locale}='+hashlib.sha256((path.parent/'support.json').read_bytes()).hexdigest())
    manifest.write_text('\n'.join(lines)+'\n',encoding='utf-8')
    locale_config=ET.Element('locale-config',{'xmlns:android':'http://schemas.android.com/apk/res/android'})
    for locale in selected:ET.SubElement(locale_config,'locale',{'android:name':locale})
    xml=target/'res/xml/locales_config.xml';xml.parent.mkdir(parents=True,exist_ok=True);ET.ElementTree(locale_config).write(xml,encoding='utf-8',xml_declaration=True)
    asset=ROOT/'app/src'/('debug' if preview else 'main')/'assets/localization.json';asset.parent.mkdir(parents=True,exist_ok=True)
    asset.write_text(json.dumps({'runtime_translation':False,'machine_review_pending':preview,'source_sha256':source_hash,'locales':selected},indent=2)+'\n')
    support_assets=asset.parent/'support';support_assets.mkdir(exist_ok=True)
    for locale in selected:
        source=ROOT/'localization/generated'/('values-b+'+locale.replace('-','+'))/'support.json'
        shutil.copyfile(source,support_assets/(locale+'.json'))
    base_support=ROOT/'app/src/main/assets/support';base_support.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(ROOT/'support/en-GB.json',base_support/'en-GB.json')
    print(f'Bundled {len(selected)} locales for '+('engineering preview' if preview else 'release'))

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--translate',action='store_true');parser.add_argument('--provider',choices=['argos','deepl'],default='argos');parser.add_argument('--approve');parser.add_argument('--bundle-preview',action='store_true');parser.add_argument('--bundle-approved',action='store_true');args=parser.parse_args()
    assert not (args.bundle_preview and args.bundle_approved),'Choose one packaging policy'
    if args.bundle_preview or args.bundle_approved:
        bundle_resources(preview=args.bundle_preview);return
    if args.translate and args.provider=='argos':
        subprocess.run([sys.executable,str(ROOT/'tools/translate_offline.py')],check=True);return
    source=extract();directory=ROOT/'localization';(directory/'source.json').write_text(json.dumps(source,ensure_ascii=False,indent=2),encoding='utf-8')
    path=directory/'status.json';status=json.loads(path.read_text(encoding='utf-8')) if path.exists() else {}
    cachepath=directory/'cache/translations.json';cache=json.loads(cachepath.read_text(encoding='utf-8')) if cachepath.exists() else {}
    for locale,target in CONFIG['locales'].items():
        record=status.setdefault(locale,{'state':'extracted','human_reviewer':None,'release_approved':False,'issues':[]})
        if args.translate:
            key=os.environ.get('DEEPL_AUTH_KEY');assert key,'Set DEEPL_AUTH_KEY in the development shell'
            try:
                data={k:translate(v,target,key,cache) for k,v in source.items()};record['state']='machine_translated'
                validate(source,data);resources(locale,data);record['state']='validated';record['issues']=['Human review, plural categories, RTL, screenshots and on-device text expansion still required']
            except Exception as exc:record['issues']=[type(exc).__name__+': '+str(exc)]
        elif record['state'] in ['not_started','extracted']:record['issues']=['Translation provider credentials and human review pending']
    if args.approve:
        r=status[args.approve];assert r['state']=='human_reviewed' and r['human_reviewer'] and not r['issues'] and r.get('reviewed_source_sha256')==hashlib.sha256(json.dumps(source,sort_keys=True).encode()).hexdigest(),'Record completed human review for the current catalogue and clear all checks before approval'
        r['state']='release_approved';r['release_approved']=True
    path.write_text(json.dumps(status,indent=2),encoding='utf-8')
    cachepath.parent.mkdir(exist_ok=True);cachepath.write_text(json.dumps(cache,ensure_ascii=False),encoding='utf-8')
    print(f'Extracted {len(source)} messages; {len(status)} locale records. Approved: {sum(r["release_approved"] for r in status.values())}')
if __name__=='__main__':main()
