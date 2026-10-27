"""Reproducible coverage report; validation never implies human language approval."""
import hashlib
import json
import re
import xml.etree.ElementTree as ET
from localize import ROOT, extract, placeholders, validate

def main():
    source=extract()
    status=json.loads((ROOT/'localization/status.json').read_text(encoding='utf-8'))
    report={'release_approved':False,'runtime_translation':False,'source_sha256':hashlib.sha256(json.dumps(source,sort_keys=True).encode()).hexdigest(),'source_messages':len(source),'locales':{}}
    for locale,record in status.items():
        folder=ROOT/'localization/generated'/('values-b+'+locale.replace('-','+'))
        tree=ET.parse(folder/'strings.xml')
        values={e.attrib['name']:e.text or '' for e in tree.findall('string')}
        for group in tree.findall('plurals'):
            for e in group:values[f"plural:{group.attrib['name']}:{e.attrib['quantity']}"]=e.text or ''
        for kind in ('store','support'):
            values.update({kind+':'+k:v for k,v in json.loads((folder/(kind+'.json')).read_text(encoding='utf-8')).items() if isinstance(v,str)})
        missing=sorted(set(source)-set(values))
        mismatches=[key for key,value in source.items() if placeholders(value)!=placeholders(values.get(key,''))]
        assert not missing and not mismatches,(locale,missing,mismatches)
        report['locales'][locale]={'source_messages_covered':len(source),'missing':missing,'placeholder_errors':mismatches,'plural_forms':{group.attrib['name']:[e.attrib['quantity'] for e in group] for group in tree.findall('plurals')},'unchanged_strings_requiring_context_review':[key for key,value in source.items() if values.get(key)==value],'human_reviewer':record.get('human_reviewer'),'human_review_complete':record.get('state') in ('human_reviewed','release_approved'),'release_approved':record.get('release_approved',False)}
        report['locales'][locale]['possible_untranslated_fragments']={key:re.findall(r'[A-Za-z]+(?:[\s&"-]+[A-Za-z]+){2,}',value) for key,value in values.items() if locale.split('-')[0] in ('ar','ja','hi') and re.search(r'[A-Za-z]+(?:[\s&"-]+[A-Za-z]+){2,}',value)}
    (ROOT/'localization/quality-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(f'{len(status)} locales: {len(source)} messages each; no missing keys or placeholder errors. Human approval remains pending.')

if __name__=='__main__':main()
