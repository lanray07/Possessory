# Amazon receipt verification service

Owner-operated service; not deployed or configured by this build. It accepts only Amazon user/receipt identifiers, verifies Amazon RVS, matches the exact configured one-time ENTITLED SKU, checks cancellation and rejects test transactions in production. It returns an RSA-SHA256 signed license lasting at most 90 days for offline use. It never accepts household inventory content. Production purchase/restore testing is still required.

Install server/requirements.txt in an isolated Python environment. Configure AMAZON_SHARED_SECRET, AMAZON_PREMIUM_SKU and LICENSE_PRIVATE_KEY_PEM in the deployment's secret store, then serve `verifier:app` behind an HTTPS endpoint. Apply request-body limits and per-client/receipt rate limits at the hosting edge. Disable request-body and outbound-URL logging: Amazon RVS URLs contain the shared secret. Do not deploy with debug error pages.

Use separate RSA keys and deployments for sandbox and production. Set AMAZON_RVS_SANDBOX=true only for App Tester; the production default is false. Keep the private RSA key off the developer client and APK. Put the base64 DER SubjectPublicKeyInfo public key in POSSESSORY_LICENSE_PUBLIC_KEY at Android build time, with POSSESSORY_AMAZON_SKU and the HTTPS POSSESSORY_VERIFIER_URL ending in /verify. Empty configuration disables purchase initiation. Store price is fetched through Amazon product data; the proposed £4.99 price is not hardcoded.

Tests: `python -m unittest discover -s server -v`. They use local receipt fixtures and throwaway RSA keys only; passing tests are not evidence of a real verified Amazon purchase.

Cached licenses are signed, not encrypted. They contain purchase identifiers and are stored in private app preferences and excluded from inventory backups. A signed cancellation revokes Premium. Network failures preserve a previously valid cached license. Startup checks its expiry; expiry while an app remains continuously open and hardening against device-clock rollback require further release validation. Existing records and deletion remain available without Premium. This policy needs owner approval before sale.

Primary reference: https://developer.amazon.com/docs/in-app-purchasing/iap-rvs-for-android-apps.html
