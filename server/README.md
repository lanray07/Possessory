# Amazon receipt verification service

The deployed host is Cloudflare Workers; see [its deployment guide](cloudflare/README.md). The Python implementation in this folder remains an alternative host implementation. Both accept only Amazon user/receipt identifiers, verify Amazon RVS, match the exact configured one-time ENTITLED SKU, check cancellation and reject test transactions in production. They return an RSA-SHA256 signed license lasting at most 90 days for offline use, capped by an earlier effective cancellation. Neither accepts household inventory content. Production purchase/restore testing is still required.

Install server/requirements.txt in an isolated Python environment. Configure AMAZON_SHARED_SECRET, AMAZON_PREMIUM_SKU and LICENSE_PRIVATE_KEY_PEM in the deployment's secret store, then serve `verifier:app` behind an HTTPS endpoint. Apply request-body limits and per-client/receipt rate limits at the hosting edge. Disable request-body and outbound-URL logging: Amazon RVS URLs contain the shared secret. Do not deploy with debug error pages.

Use separate RSA keys and deployments for sandbox and production. Set AMAZON_RVS_SANDBOX=true only for App Tester; the production default is false. Keep the private RSA key off the developer client and APK. Put the base64 DER SubjectPublicKeyInfo public key in POSSESSORY_LICENSE_PUBLIC_KEY at Android build time, with POSSESSORY_AMAZON_SKU and the HTTPS POSSESSORY_VERIFIER_URL ending in /verify. Empty configuration disables purchase initiation. Store price is fetched through Amazon product data; the proposed £4.99 price is not hardcoded.

Tests: `python -m unittest discover -s server -v`. They use local receipt fixtures and throwaway RSA keys only; passing tests are not evidence of a real verified Amazon purchase.

Cached licenses are signed, not encrypted. They contain purchase identifiers and are stored in private app preferences and excluded from inventory backups. A signed cancellation revokes Premium. Network failures preserve a previously valid cached license. Startup checks expiry, an in-process timer revokes expired access, and a monotonic clock plus persisted last-seen time limits ordinary device-clock rollback. Rooted-device tampering is not claimed to be prevented. Real purchase/expiry device validation remains required. Existing records and deletion remain available without Premium. This policy needs owner approval before sale.

Primary reference: https://developer.amazon.com/docs/in-app-purchasing/iap-rvs-for-android-apps.html


The alternative Python stack requires Python 3.12 (see .python-version). Install requirements-dev.txt for HTTP tests and pip-audit. The service rejects bodies over 4096 bytes and extra inventory fields, and returns no-store/nosniff headers. Configure rate limits and disable request/outbound-URL logging at the host before production. /health discloses only whether configuration is present, never secret values. server/index.py is the FastAPI hosting entry point; this alternative has not been deployed.

Set AMAZON_PREMIUM_SKU=possessory.premium.lifetime to match the owner-created entitlement. Its Amazon product ID is amzn1.dex.title.824c8f1e78d840efbd861d267e937cce. Keep AMAZON_SHARED_SECRET and LICENSE_PRIVATE_KEY_PEM in the host secret store, never in chat or the APK.
