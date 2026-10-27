import unittest, base64, json
from cryptography.hazmat.primitives.asymmetric import rsa, padding
from cryptography.hazmat.primitives import hashes, serialization
from verifier import claims_from_receipt, sign_claims

class VerificationTests(unittest.TestCase):
    def receipt(self,**extra):return dict(productId='premium',productType='ENTITLED',receiptId='r',testTransaction=False,cancelDate=None,**extra)
    def test_matching_entitlement(self):
        c=claims_from_receipt(self.receipt(),'u','r','premium',False,1000)
        self.assertTrue(c['active']);self.assertEqual(c['validUntil'],1000+90*86400)
        self.assertEqual(c['environment'],'production')
    def test_sandbox_licenses_are_explicitly_separated(self):
        receipt=self.receipt();receipt['testTransaction']=True
        self.assertEqual(claims_from_receipt(receipt,'u','r','premium',True,1000)['environment'],'sandbox')
    def test_refunds_revoke_access(self):
        r=self.receipt();r['cancelDate']=999000
        self.assertFalse(claims_from_receipt(r,'u','r','premium',False,1000)['active'])
    def test_future_cancellation_caps_offline_license(self):
        r=self.receipt();r['cancelDate']=1001500
        c=claims_from_receipt(r,'u','r','premium',False,1000)
        self.assertTrue(c['active']);self.assertEqual(c['validUntil'],1002)
    def test_mismatch_and_sandbox_are_rejected(self):
        for change in [dict(productId='other'),dict(productType='CONSUMABLE'),dict(receiptId='another'),dict(testTransaction=True),dict(cancelDate='invalid'),dict(cancelDate=float('nan')),dict(cancelDate=float('inf'))]:
            r=self.receipt();r.update(change)
            with self.assertRaises(ValueError):claims_from_receipt(r,'u','r','premium',False,1000)
    def test_rsa_signature_is_verifiable(self):
        key=rsa.generate_private_key(public_exponent=65537,key_size=2048)
        pem=key.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.NoEncryption())
        claims=claims_from_receipt(self.receipt(),'u','r','premium',False,1000)
        token=sign_claims(claims,pem);decode=lambda v:base64.urlsafe_b64decode(v+'='*((-len(v))%4))
        key.public_key().verify(decode(token['signature']),decode(token['payload']),padding.PKCS1v15(),hashes.SHA256())
        self.assertEqual(json.loads(decode(token['payload'])),claims)
if __name__=='__main__':unittest.main()
