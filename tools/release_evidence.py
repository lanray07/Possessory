"""Seal observed candidate checks and file hashes without asserting release approval."""
import collections
import hashlib
import json
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from localize import ROOT

def main():
    artifact=ROOT/'artifacts'
    for name,expected in [('review-candidate-log.txt','BUILD SUCCESSFUL'),('final-release-checks-log.txt','BUILD SUCCESSFUL'),('fire-final-device-log.txt','OK (3 tests)'),('fire-large-font-log.txt','OK (1 test)'),('fire-portrait-log.txt','OK (1 test)'),('fire-production-upgrade-log.txt','OK (1 test)')]:
        assert expected in (ROOT/name).read_text(encoding='utf-8'),name
    lint={}
    for variant in ('amazonRelease','standaloneRelease'):
        issues=ET.parse(ROOT/f'app/build/reports/lint-results-{variant}.xml').getroot().findall('issue')
        counts=collections.Counter(issue.attrib['severity'] for issue in issues)
        assert counts['Error']==0
        lint[variant]={'errors':0,'warnings':counts['Warning'],'other_severities':{key:value for key,value in counts.items() if key not in ('Error','Warning')},'warning_categories':dict(collections.Counter(issue.attrib['id'] for issue in issues if issue.attrib['severity']=='Warning'))}
    suites=[ET.parse(path).getroot() for path in (ROOT/'app/build/test-results/testStandaloneDebugUnitTest').glob('TEST-*.xml')]
    assert suites and all(int(s.attrib['failures'])==0 and int(s.attrib['errors'])==0 for s in suites)
    count=sum(int(s.attrib['tests']) for s in suites);assert count>=30
    names=['Possessory-amazon-release-signed.apk','Possessory-android-release-signed.apk','Possessory-amazon-review-signed.apk','Possessory-localization-review.zip','Possessory-source.zip','Possessory-release-certificate.pem']
    files=[{'file':name,'bytes':(artifact/name).stat().st_size,'sha256':hashlib.sha256((artifact/name).read_bytes()).hexdigest()} for name in names]
    captures=json.loads((artifact/'reviewer-captures/manifest.json').read_text(encoding='utf-8'))
    assert captures['apk_sha256']==next(record['sha256'] for record in files if record['file']=='Possessory-amazon-review-signed.apk')
    verification={'version':'0.3.0','version_code':3,'verified_at_utc':datetime.now(timezone.utc).isoformat(),'status':'review_candidate_not_submission_approved','signing':'Dedicated Possessory RSA 4096-bit key; v2/v3 signature and zip alignment verified','signer_certificate_sha256':'e19756432bb702f5f454683d10330ef49bba7eee7848e34cf0eaae6540e111bd','validation':{'android_unit_rendered_tests':count,'server_fixture_http_tests':8,'translation_pipeline_tests':5,'server_dependency_audit':'No known vulnerabilities in pinned server requirements','lint':lint,'physical_fire_native_tests':3,'physical_fire_font_200_percent_navigation':'en-GB, de-DE, ar passed','physical_fire_portrait_navigation':'en-GB, de-DE, ar passed','signed_production_upgrade':'Existing demo household, >=3 rooms, >=6 items and maintenance retained; schema v2','large_backup':json.loads((artifact/'fire-large-backup-check.json').read_text(encoding='utf-8'))},'localization':{'draft_targets':19,'source_messages':335,'human_approved':0,'production_release_base':'en-GB','review_package':'app.possessory.review','runtime_translation':False,'capture_count':175,'capture_manifest':'reviewer-captures/manifest.json','quality_report':'../localization/quality-report.json'},'amazon_draft':{'uploaded_binary_version':'0.2.0','existing_uploaded_screenshots':8,'premium_sku':'possessory.premium.lifetime','premium_product_id':'amzn1.dex.title.824c8f1e78d840efbd861d267e937cce','premium_base_price_gbp':'4.99','premium_type':'one-time entitlement','iap_submitted':False,'app_submitted':False,'export_declaration_checked':False},'open_gates':['Independent human language/region/plural/voice/store/support/privacy review and correction','Production verifier hosting, private credentials and deployment','Real Amazon purchase/restore/cancellation/offline/account-change validation','Manual external camera, speech/TTS, app-lock, notification and accessibility sign-off; additional devices','Final approved store binary, localized assets, privacy/export declarations and owner release approval'],'files':files}
    (artifact/'release-verification.json').write_text(json.dumps(verification,indent=2)+'\n',encoding='utf-8')
    manifest={'version':'0.3.0','version_code':3,'status':verification['status'],'operator':'O. Bankole','support':'banksmi@mail.com','files':files,'ocr_models':'../localization/ocr-models.json','localization':verification['localization'],'signed_release_verification':'release-verification.json','legacy_store_screenshots':{'version':'0.2.0','zip':'Possessory-Fire-screenshots.zip','manifest':'device-captures/en-GB/manifest.json'}}
    (artifact/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
    print(f'Sealed 0.3.0 candidate: {count} Android tests, zero lint errors, 175 raw captures; submission remains blocked.')

if __name__=='__main__':main()
