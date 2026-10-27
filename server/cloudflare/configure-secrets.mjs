// Run only after the private directory ACL has been restricted to the current owner.
// Secrets go to Wrangler over stdin, never argv, source control or console output.
import { createPrivateKey, createPublicKey, generateKeyPairSync } from 'node:crypto';
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { homedir } from 'node:os';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const directory = join(homedir(), '.android', 'possessory-receipt-verifier');
if (!existsSync(directory)) throw new Error('Create the owner-only private directory first.');
const root = dirname(fileURLToPath(import.meta.url));
const wrangler = join(root, 'node_modules', 'wrangler', 'bin', 'wrangler.js');
const publicConfiguration = {};
for (const environment of ['production', 'sandbox']) {
  const filename = join(directory, `${environment}-private.pem`);
  if (!existsSync(filename)) {
    const existing = spawnSync(process.execPath, [wrangler, 'secret', 'list', '--env', environment, '--format', 'json'], {
      cwd: root, encoding: 'utf8', env: { ...process.env, WRANGLER_SEND_METRICS: 'false' },
    });
    if (existing.error || existing.status !== 0) throw new Error(`Cannot inspect ${environment} secret names; restore the existing license key before continuing.`);
    if (JSON.parse(existing.stdout).some(binding => binding.name === 'LICENSE_PRIVATE_KEY_PEM')) {
      throw new Error(`Existing ${environment} signing key is missing locally. Restore its private backup; refusing to rotate client trust.`);
    }
    const pair = generateKeyPairSync('rsa', { modulusLength: 3072 });
    writeFileSync(filename, pair.privateKey.export({ type: 'pkcs8', format: 'pem' }), { flag: 'wx', mode: 0o600 });
  }
  const pem = readFileSync(filename, 'utf8');
  const key = createPrivateKey(pem);
  if (key.asymmetricKeyType !== 'rsa' || key.asymmetricKeyDetails.modulusLength < 2048) throw new Error('Unexpected license key.');
  const publicKey = createPublicKey(key).export({ type: 'spki', format: 'der' }).toString('base64');
  writeFileSync(join(directory, `${environment}-public-key.txt`), publicKey, { mode: 0o600 });
  const secret = environment === 'production'
    ? readFileSync(join(directory, 'amazon-shared-secret.txt'), 'utf8').trim()
    : 'possessory-sandbox'; // Amazon does not validate the developer secret on its sandbox endpoint.
  if (!secret) throw new Error('Amazon verification key is missing.');
  const result = spawnSync(process.execPath, [wrangler, 'secret', 'bulk', '--env', environment], {
    cwd: root, input: JSON.stringify({ AMAZON_SHARED_SECRET: secret, LICENSE_PRIVATE_KEY_PEM: pem }),
    encoding: 'utf8', env: { ...process.env, WRANGLER_SEND_METRICS: 'false', CLOUDFLARE_LOAD_DEV_VARS_FROM_DOT_ENV: 'false' },
  });
  if (result.error || result.status !== 0) throw new Error(`Cloudflare secret update failed for ${environment}; no secret values have been printed.`);
  publicConfiguration[environment] = { verifierUrl: `https://possessory-receipts-${environment}.lanraybanks.workers.dev/verify`, publicKey };
  console.log(`${environment}: two private secret bindings configured.`);
}
if (publicConfiguration.production.publicKey === publicConfiguration.sandbox.publicKey) throw new Error('Sandbox and production must use different keys.');
writeFileSync(join(directory, 'public-build-config.json'), JSON.stringify(publicConfiguration, null, 2) + '\n', { mode: 0o600 });
writeFileSync(join(root, 'android-public-config.json'), JSON.stringify(publicConfiguration, null, 2) + '\n');
console.log('Separate license keys preserved privately. Public Android build configuration is ready.');
