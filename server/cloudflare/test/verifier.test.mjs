import test from 'node:test';
import assert from 'node:assert/strict';
import { generateKeyPairSync, verify } from 'node:crypto';
import { createVerifier, claimsFromReceipt } from '../src/index.mjs';

const pair = generateKeyPairSync('rsa', { modulusLength: 2048 });
const pem = pair.privateKey.export({ type: 'pkcs8', format: 'pem' });
const now = 1793116800;
const limiter = () => ({ limit: async () => ({ success: true }) });
const environment = (extra = {}) => ({ AMAZON_SHARED_SECRET: 'fixture-secret', AMAZON_PREMIUM_SKU: 'possessory.premium.lifetime', LICENSE_PRIVATE_KEY_PEM: pem, DEPLOYMENT_ENV: 'production', IP_LIMITER: limiter(), RECEIPT_LIMITER: limiter(), ...extra });
const receipt = (extra = {}) => ({ productId: 'possessory.premium.lifetime', productType: 'ENTITLED', receiptId: 'receipt', testTransaction: false, cancelDate: null, ...extra });
const request = (body = { userId: 'user', receiptId: 'receipt' }) => new Request('https://verifier.test/verify', { method: 'POST', body: JSON.stringify(body), headers: { 'Content-Type': 'application/json', 'CF-Connecting-IP': '192.0.2.1' } });
const service = (result = receipt(), check = () => {}) => createVerifier({ clock: () => now * 1000, fetchAmazon: async (url, options) => { check(url, options); return result instanceof Response ? result : Response.json(result); } });
async function decoded(response) {
  assert.equal(response.status, 200);
  const token = await response.json();
  const payload = Buffer.from(token.payload, 'base64url');
  assert(verify('RSA-SHA256', payload, pair.publicKey, Buffer.from(token.signature, 'base64url')));
  return JSON.parse(payload.toString('utf8'));
}

test('health exposes no secret values and never enables caching', async () => {
  const response = await service().fetch(new Request('https://verifier.test/health'), environment());
  assert.deepEqual(await response.json(), { service: 'Possessory receipt verification', configured: true });
  assert.equal(response.headers.get('cache-control'), 'no-store');
  assert.equal(response.headers.get('x-content-type-options'), 'nosniff');
  const missing = await service().fetch(new Request('https://verifier.test/health'), {});
  assert.equal((await missing.json()).configured, false);
});
test('routes and methods are constrained', async () => {
  assert.equal((await service().fetch(new Request('https://verifier.test/other'), {})).status, 404);
  const response = await service().fetch(new Request('https://verifier.test/verify'), {});
  assert.equal(response.status, 405); assert.equal(response.headers.get('allow'), 'POST');
});
test('oversized bodies are rejected before upstream calls', async () => {
  const response = await service(null, () => assert.fail('upstream called')).fetch(new Request('https://verifier.test/verify', { method: 'POST', body: 'x'.repeat(4097) }), environment());
  assert.equal(response.status, 413);
});
test('inventory fields, malformed JSON and invalid identifiers are rejected', async () => {
  for (const value of [{ userId: 'user', receiptId: 'receipt', inventory: 'private' }, [], null, { userId: ' ', receiptId: 'r' }, { userId: 'u'.repeat(513), receiptId: 'r' }, { userId: 'u', receiptId: 1 }])
    assert.equal((await service().fetch(request(value), environment())).status, 422);
  const bad = new Request('https://verifier.test/verify', { method: 'POST', body: '{bad' });
  assert.equal((await service().fetch(bad, environment())).status, 422);
});
test('unconfigured secrets, invalid environment and absent rate bindings fail closed', async () => {
  for (const extra of [{ AMAZON_SHARED_SECRET: '' }, { LICENSE_PRIVATE_KEY_PEM: '' }, { DEPLOYMENT_ENV: 'staging' }, { IP_LIMITER: undefined }, { RECEIPT_LIMITER: undefined }])
    assert.equal((await service().fetch(request(), environment(extra))).status, 503);
});
test('IP throttling rejects before receipt verification', async () => {
  const response = await service(null, () => assert.fail('upstream called')).fetch(request(), environment({ IP_LIMITER: { limit: async () => ({ success: false }) } }));
  assert.equal(response.status, 429); assert.equal(response.headers.get('retry-after'), '60');
});
test('receipt limiter uses a hash rather than customer or receipt identifiers', async () => {
  const response = await service(null, () => assert.fail('upstream called')).fetch(request(), environment({ RECEIPT_LIMITER: { limit: async ({ key }) => { assert.match(key, /^[A-Za-z0-9_-]{43}$/); assert(!key.includes('receipt')); return { success: false }; } } }));
  assert.equal(response.status, 429);
});
test('valid production receipts generate Android-compatible RSA-SHA256 claims', async () => {
  const claims = await decoded(await service(receipt(), (url, options) => { assert.equal(new URL(url).hostname, 'appstore-sdk.amazon.com'); assert(!url.includes('/sandbox/')); assert.equal(options.redirect, 'manual'); }).fetch(request(), environment()));
  assert.deepEqual(claims, { sku: 'possessory.premium.lifetime', userId: 'user', receiptId: 'receipt', active: true, issuedAt: now, validUntil: now + 90 * 86400, environment: 'production' });
});
test('SKU, product type and receipt mismatches cannot grant access', async () => {
  for (const change of [{ productId: 'other' }, { productType: 'CONSUMABLE' }, { receiptId: 'different' }, { cancelDate: true }])
    assert.equal((await service(receipt(change)).fetch(request(), environment())).status, 422);
});
test('unidentified transactions are rejected in production', async () => {
  for (const value of [undefined, null, 'false', 'true', 0, 1])
    assert.equal((await service(receipt({ testTransaction: value })).fetch(request(), environment())).status, 422);
});
test('Amazon-verified LAT and publishing-test transactions work through production RVS', async () => {
  for (const betaProduct of [true, false]) {
    const claims = await decoded(await service(receipt({ testTransaction: true, betaProduct }), url => {
      assert.equal(new URL(url).hostname, 'appstore-sdk.amazon.com');
      assert(!new URL(url).pathname.includes('/sandbox/'));
    }).fetch(request(), environment()));
    assert.equal(claims.active, true);
    assert.equal(claims.environment, 'production');
  }
});
test('client flags cannot manufacture a LAT entitlement and cancelled LAT receipts revoke', async () => {
  for (const extra of [{ betaProduct: true }, { testTransaction: true }]) {
    const result = await service(null, () => assert.fail('unverified flags reached Amazon'))
      .fetch(request({ userId: 'user', receiptId: 'receipt', ...extra }), environment());
    assert.equal(result.status, 422);
  }
  const claims = await decoded(await service(receipt({ testTransaction: true, betaProduct: true, cancelDate: now * 1000 - 1 }))
    .fetch(request(), environment()));
  assert.equal(claims.active, false);
});
test('sandbox uses Amazon sandbox URL and can only issue sandbox claims', async () => {
  const claims = await decoded(await service(receipt({ testTransaction: true }), url => assert(new URL(url).pathname.startsWith('/sandbox/'))).fetch(request(), environment({ DEPLOYMENT_ENV: 'sandbox' })));
  assert.equal(claims.environment, 'sandbox'); assert.equal(claims.active, true);
});
test('cancellation and Amazon HTTP 410 produce signed revocations', async () => {
  for (const result of [receipt({ cancelDate: now * 1000 - 1 }), new Response(null, { status: 410 })]) {
    const claims = await decoded(await service(result).fetch(request(), environment()));
    assert.equal(claims.active, false);
  }
});
test('future cancellation bounds offline validity to its effective time', () => {
  const claims = claimsFromReceipt(receipt({ cancelDate: (now + 100) * 1000 }), 'u', 'receipt', 'possessory.premium.lifetime', 'production', now);
  assert.equal(claims.validUntil, now + 100);
});
test('Amazon errors are sanitized without echoing the secret or identifiers', async () => {
  for (const status of [400, 497, 496, 429, 500]) {
    const response = await service(new Response('fixture-secret/user/receipt', { status })).fetch(request(), environment());
    assert.equal(response.status, [400, 497].includes(status) ? 422 : 503);
    assert(!String(await response.text()).includes('fixture-secret'));
  }
});
test('oversized or malformed Amazon responses fail closed', async () => {
  for (const value of ['x'.repeat(65537), '{broken'])
    assert.equal((await service(new Response(value)).fetch(request(), environment())).status, 503);
});
test('redirect responses are rejected without following their destination', async () => {
  const response = await service(new Response(null, { status: 302, headers: { Location: 'https://untrusted.example' } }), (_url, options) => assert.equal(options.redirect, 'manual')).fetch(request(), environment());
  assert.equal(response.status, 503);
});
test('network failures and malformed signing keys reveal no implementation details', async () => {
  const network = createVerifier({ fetchAmazon: async () => { throw new Error('private URL fixture-secret'); } });
  assert.equal((await network.fetch(request(), environment())).status, 503);
  const response = await service().fetch(request(), environment({ LICENSE_PRIVATE_KEY_PEM: 'invalid private fixture' }));
  assert.equal(response.status, 503); assert(!String(await response.text()).includes('fixture'));
});
