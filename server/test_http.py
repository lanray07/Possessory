import unittest
from unittest.mock import patch
from fastapi.testclient import TestClient
from verifier import app

class HttpTests(unittest.TestCase):
    def test_health_does_not_disclose_secrets(self):
        with patch.dict('os.environ',{'AMAZON_SHARED_SECRET':'private-test-secret','AMAZON_PREMIUM_SKU':'premium','LICENSE_PRIVATE_KEY_PEM':'private-test-key'}):
            response=TestClient(app).get('/health')
            self.assertEqual(response.status_code,200)
            self.assertTrue(response.json()['configured'])
            self.assertNotIn('private-test',response.text)
            self.assertEqual(response.headers['cache-control'],'no-store')

    def test_requests_are_bounded_before_receipt_verification(self):
        response=TestClient(app).post('/verify',content=b'x'*4097,headers={'Content-Type':'application/json'})
        self.assertEqual(response.status_code,413)

    def test_unsupported_fields_and_unconfigured_verifier_fail_closed(self):
        client=TestClient(app)
        self.assertEqual(client.post('/verify',json={'userId':'u','receiptId':'r','inventory':'must-not-be-accepted'}).status_code,422)
        with patch.dict('os.environ',{},clear=True):
            self.assertEqual(client.post('/verify',json={'userId':'u','receiptId':'r'}).status_code,503)
