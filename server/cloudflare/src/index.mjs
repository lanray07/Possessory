const encoder = new TextEncoder();
const RSA = { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' };
const TTL = 90 * 86400;

class HttpError extends Error {
  constructor(status, message) { super(message); this.status = status; }
}
const reply = (value, status = 200, extra = {}) => new Response(JSON.stringify(value), {
  status, headers: { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff', ...extra },
});

async function bounded(response, maximum, status) {
  const reader = response.body?.getReader();
  if (!reader) return new Uint8Array();
  const chunks = []; let size = 0;
  try {
    for (;;) {
      const { value, done } = await reader.read();
      if (done) break;
      size += value.byteLength;
      if (size > maximum) {
        await reader.cancel();
        throw new HttpError(status, status === 413 ? 'Request too large' : 'Verification temporarily unavailable');
      }
      chunks.push(value);
    }
  } finally { reader.releaseLock(); }
  const bytes = new Uint8Array(size); let offset = 0;
  for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.byteLength; }
  return bytes;
}

function parseRequest(bytes) {
  let value;
  try { value = JSON.parse(new TextDecoder('utf-8', { fatal: true }).decode(bytes)); }
  catch { throw new HttpError(422, 'Invalid receipt request'); }
  if (!value || Array.isArray(value) || Object.keys(value).sort().join(',') !== 'receiptId,userId')
    throw new HttpError(422, 'Only purchase identifiers are accepted');
  for (const [key, maximum] of [['userId', 512], ['receiptId', 1024]]) {
    if (typeof value[key] !== 'string' || value[key].length < 1 || value[key].length > maximum || /\s/u.test(value[key]))
      throw new HttpError(422, 'Invalid purchase identifier');
  }
  return value;
}

export function claimsFromReceipt(receipt, userId, receiptId, sku, environment, now) {
  if (!receipt || receipt.productId !== sku || receipt.productType !== 'ENTITLED' || receipt.receiptId !== receiptId)
    throw new HttpError(422, 'Receipt did not match the configured entitlement');
  if (environment === 'production' && receipt.testTransaction !== false)
    throw new HttpError(422, 'Test or unidentified receipt cannot grant production access');
  const cancel = receipt.cancelDate ?? null;
  if (cancel !== null && (typeof cancel !== 'number' || !Number.isFinite(cancel)))
    throw new HttpError(422, 'Invalid cancellation date');
  const active = cancel === null || cancel > now * 1000;
  return { sku, userId, receiptId, active, issuedAt: now,
    validUntil: active && cancel !== null ? Math.min(now + TTL, Math.ceil(cancel / 1000)) : now + TTL,
    environment };
}

const base64url = bytes => btoa(String.fromCharCode(...new Uint8Array(bytes))).replaceAll('+', '-').replaceAll('/', '_').replace(/=+$/, '');
const configured = env => ['production', 'sandbox'].includes(env.DEPLOYMENT_ENV) &&
  ['AMAZON_SHARED_SECRET', 'AMAZON_PREMIUM_SKU', 'LICENSE_PRIVATE_KEY_PEM'].every(key => typeof env[key] === 'string' && env[key].trim()) &&
  typeof env.IP_LIMITER?.limit === 'function' && typeof env.RECEIPT_LIMITER?.limit === 'function';

export function createVerifier({ fetchAmazon = (...args) => fetch(...args), clock = () => Date.now() } = {}) {
  let cachedPem; let cachedKey;
  async function sign(claims, pem) {
    if (pem !== cachedPem || !cachedKey) {
      const encoded = pem.replace('-----BEGIN PRIVATE KEY-----', '').replace('-----END PRIVATE KEY-----', '').replace(/\s/g, '');
      const bytes = Uint8Array.from(atob(encoded), c => c.charCodeAt(0));
      cachedPem = pem;
      cachedKey = crypto.subtle.importKey('pkcs8', bytes, RSA, false, ['sign']);
    }
    const key = await cachedKey;
    if (key.algorithm.modulusLength < 2048) throw new Error('Invalid license key');
    const payload = encoder.encode(JSON.stringify(claims));
    const signature = await crypto.subtle.sign(RSA, key, payload);
    return { payload: base64url(payload), signature: base64url(signature) };
  }
  return {
    async fetch(request, env) {
      const path = new URL(request.url).pathname;
      if (path === '/health') return request.method === 'GET'
        ? reply({ service: 'Possessory receipt verification', configured: Boolean(configured(env)) })
        : reply({ detail: 'Method not allowed' }, 405, { Allow: 'GET' });
      if (path !== '/verify') return reply({ detail: 'Not found' }, 404);
      if (request.method !== 'POST') return reply({ detail: 'Method not allowed' }, 405, { Allow: 'POST' });
      let stage = 'ip-limit';
      try {
        if (typeof env.IP_LIMITER?.limit !== 'function') throw new HttpError(503, 'Verification is not configured');
        const ip = request.headers.get('CF-Connecting-IP') || 'unknown';
        if (!(await env.IP_LIMITER.limit({ key: ip })).success)
          return reply({ detail: 'Please retry later' }, 429, { 'Retry-After': '60' });
        stage = 'request';
        const input = parseRequest(await bounded(request, 4096, 413));
        if (!configured(env)) throw new HttpError(503, 'Verification is not configured');
        stage = 'receipt-limit';
        const digest = await crypto.subtle.digest('SHA-256', encoder.encode(JSON.stringify([input.userId, input.receiptId])));
        if (!(await env.RECEIPT_LIMITER.limit({ key: base64url(digest) })).success)
          return reply({ detail: 'Please retry later' }, 429, { 'Retry-After': '60' });
        const prefix = 'https://appstore-sdk.amazon.com/' + (env.DEPLOYMENT_ENV === 'sandbox' ? 'sandbox/' : '') + 'version/1.0/verifyReceiptId/developer/';
        // Never log the outbound URL: Amazon's API embeds the developer secret in its path.
        const url = prefix + encodeURIComponent(env.AMAZON_SHARED_SECRET) + '/user/' + encodeURIComponent(input.userId) + '/receiptId/' + encodeURIComponent(input.receiptId);
        stage = 'upstream';
        const response = await fetchAmazon(url, { headers: { Accept: 'application/json' }, redirect: 'manual', signal: AbortSignal.timeout(10000) });
        const now = Math.floor(clock() / 1000);
        let claims;
        if (response.status === 410) {
          await response.body?.cancel();
          claims = { sku: env.AMAZON_PREMIUM_SKU, ...input, active: false, issuedAt: now, validUntil: now + TTL, environment: env.DEPLOYMENT_ENV };
        } else {
          if (response.status !== 200) {
            await response.body?.cancel();
            throw new HttpError([400, 497].includes(response.status) ? 422 : 503, 'Receipt could not be verified');
          }
          let receipt;
          try { receipt = JSON.parse(new TextDecoder('utf-8', { fatal: true }).decode(await bounded(response, 65536, 503))); }
          catch { throw new HttpError(503, 'Verification temporarily unavailable'); }
          claims = claimsFromReceipt(receipt, input.userId, input.receiptId, env.AMAZON_PREMIUM_SKU, env.DEPLOYMENT_ENV, now);
        }
        stage = 'signing';
        return reply(await sign(claims, env.LICENSE_PRIVATE_KEY_PEM));
      } catch (error) {
        const diagnostic = env.DEPLOYMENT_ENV === 'sandbox' ? { failureStage: stage, errorType: ['TypeError', 'ReferenceError', 'OperationError', 'DataError', 'HttpError'].includes(error.name) ? error.name : 'Error' } : {};
        return reply({ detail: error instanceof HttpError ? error.message : 'Verification temporarily unavailable', ...diagnostic }, error instanceof HttpError ? error.status : 503);
      }
    },
  };
}

export default createVerifier();
