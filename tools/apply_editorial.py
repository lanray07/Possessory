"""Apply recorded AI draft corrections without promoting a locale to human-reviewed."""
import hashlib
import json
import xml.etree.ElementTree as ET
from localize import ROOT, extract, resources, validate

def main():
    source=extract()
    patches=json.loads((ROOT/'localization/editorial-overrides.json').read_text(encoding='utf-8'))
    status_path=ROOT/'localization/status.json'
    status_bytes=status_path.read_bytes()
    report={'review_type':'AI editorial review, not independent human review',
            'scope':'Ten reminder, purchase, backup, export and privacy-label messages; remaining catalogue and regional review pending',
            'source_catalogue_sha256':hashlib.sha256(json.dumps(source,sort_keys=True).encode()).hexdigest(),
            'human_review_complete':False,'release_approved':False,'locales':{}}
    for locale, corrections in patches.items():
        folder=ROOT/'localization/generated'/('values-b+'+locale.replace('-','+'))
        tree=ET.parse(folder/'strings.xml')
        values={e.attrib['name']:(e.text or '').replace("\\'","'").replace('\\"','"') for e in tree.findall('string')}
        for group in tree.findall('plurals'):
            for e in group:values[f"plural:{group.attrib['name']}:{e.attrib['quantity']}"]=(e.text or '').replace("\\'","'").replace('\\"','"')
        for kind in ('store','support'):
            values.update({kind+':'+k:v for k,v in json.loads((folder/(kind+'.json')).read_text(encoding='utf-8')).items() if isinstance(v,str)})
        data={key:values[key] for key in source}
        validate(source,{**data,**corrections})
        resources(locale,data)
        report['locales'][locale]={'reviewed_keys':sorted(corrections),'corrected_draft_keys':len(corrections),'human_review_complete':False}
    assert status_path.read_bytes()==status_bytes,'Editorial corrections must never modify approval records'
    (ROOT/'localization/ai-editorial-review.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(f'Applied {sum(len(v) for v in patches.values())} AI draft corrections; human approvals unchanged.')

if __name__=='__main__':main()
