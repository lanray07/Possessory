import unittest
import tempfile, json, hashlib
from pathlib import Path
from unittest.mock import patch
import localize
from localize import validate, extract
from translate_offline import pieces

class LocalisationChecks(unittest.TestCase):
    def test_owner_waiver_removes_only_human_review_gate(self):
        source={'save':'Save'}
        record={'state':'validated','human_reviewer':None,'issues':[],
                'reviewed_source_sha256':hashlib.sha256(json.dumps(source,sort_keys=True).encode()).hexdigest()}
        policy={'independent_human_review_required':False,'authority':'explicit_user_instruction'}
        with self.assertRaises(AssertionError):localize.approve_record(dict(record),source,{})
        with self.assertRaises(AssertionError):localize.approve_record(dict(record,issues=['Layout defect']),source,policy)
        with self.assertRaises(AssertionError):localize.approve_record(record,{'save':'Save record'},policy)
        with self.assertRaises(AssertionError):localize.approve_record(dict(record,state='machine_translated'),source,policy)
        localize.approve_record(record,source,policy)
        self.assertTrue(record['release_approved'])
        self.assertIsNone(record['human_reviewer'])
        self.assertEqual(record['approval_basis'],'owner_waived_human_review_with_technical_checks')
    def test_placeholders_and_brand_are_protected(self):
        source={'a':'Possessory %1$d items\\n%2$s'}
        validate(source,{'a':'Possessory %1$d objets\\n%2$s'})
        for text in ['Possessory %d objets\\n%2$s','%1$d objets\\n%2$s','Possessory %1$d objets %2$s']:
            with self.assertRaises(AssertionError):validate(source,{'a':text})
    def test_catalogue_includes_plurals_and_store_but_excludes_legal_identity(self):
        data=extract()
        self.assertIn('plural:reminder_count:one',data)
        self.assertIn('store:hero_headline',data)
        self.assertNotIn('store:support_email',data)
        self.assertNotIn('store:operator',data)
    def test_missing_or_empty_strings_fail(self):
        for target in [{},{'a':''},{'a':'text','extra':'text'}]:
            with self.assertRaises(AssertionError):validate({'a':'text'},target)
    def test_offline_translation_keeps_commands_and_placeholders_literal(self):
        source='Possessory: %1$d items\\nTry “Find my laptop” · 90 days · PDF'
        parts=pieces(source)
        self.assertEqual(''.join(part for _,part in parts),source)
        protected=[part for protected,part in parts if protected]
        for token in ['Possessory','%1$d','\\n','“Find my laptop”','90','PDF']:self.assertIn(token,protected)
    def test_preview_bundles_static_resources_and_release_requires_approval(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);source=root/'app/src/main/res/values/strings.xml';source.parent.mkdir(parents=True)
            source.write_text('<resources><string name="save">Save</string></resources>',encoding='utf-8')
            for name in ['store','support']:
                (root/name).mkdir();(root/name/'en-GB.json').write_text('{}')
            status=root/'localization/status.json';status.parent.mkdir()
            status.write_text(json.dumps({'es-ES':{'state':'machine_translated','release_approved':False,'bundled_in_preview':True,'issues':['Human review pending']}}))
            (root/'localization/editorial-overrides.json').write_text(json.dumps({'es-ES':{'save':'Guardar registro'}}))
            with patch.object(localize,'ROOT',root),patch.object(localize,'CONFIG',{'locales':{'es-ES':'ES'}}):
                localize.resources('es-ES',{'save':'Guardar','store:description':'Inventario privado','support:premium':'Verificación de compra'})
                localize.bundle_resources(preview=True)
                generated=root/'localization/generated/values-b+es+ES'
                metadata=json.loads((generated/'metadata.json').read_text(encoding='utf-8'))
                # Git's LF checkout must preserve every byte used by the packaged checksum contract.
                for name,field in [('strings.xml','strings_sha256'),('store.json','store_sha256'),('support.json','support_sha256')]:
                    raw=(generated/name).read_bytes()
                    self.assertNotIn(b'\r\n',raw)
                    self.assertEqual(hashlib.sha256(raw.replace(b'\r\n',b'\n')).hexdigest(),metadata[field])
                self.assertNotIn(b'\r\n',(root/'app/src/preview/locales.properties').read_bytes())
                self.assertIn('Guardar registro',(root/'app/src/preview/res/values-b+es+ES/strings.xml').read_text(encoding='utf-8'))
                self.assertFalse(json.loads(status.read_text())['es-ES']['release_approved'])
                self.assertFalse(json.loads((root/'app/src/debug/assets/localization.json').read_text())['runtime_translation'])
                with self.assertRaises(AssertionError):localize.bundle_resources(preview=False)
                source.write_text('<resources><string name="save">Save record</string></resources>',encoding='utf-8')
                with self.assertRaises(AssertionError):localize.bundle_resources(preview=True)
                (root/'localization/editorial-overrides.json').write_text(json.dumps({'es-ES':{'save':'Guardar %d'}}))
                with self.assertRaises(AssertionError):localize.resources('es-ES',{'save':'Guardar'})
                (root/'localization/editorial-overrides.json').write_text(json.dumps({'es-ES':{'save':'Guardar registro'}}))
                status.write_text(json.dumps({'es-ES':{'state':'human_reviewed','release_approved':False,'human_reviewer':'Recorded reviewer'}}))
                before=(generated/'strings.xml').read_bytes()
                with self.assertRaisesRegex(AssertionError,'Human-reviewed'):
                    localize.resources('es-ES',{'save':'Guardar'})
                self.assertEqual(before,(generated/'strings.xml').read_bytes())
if __name__=='__main__':unittest.main()
