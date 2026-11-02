# Possessory 0.3.7 — Amazon release candidate (version code 11)

The signed release is in `artifacts/Possessory-amazon-0.3.7-localized-release-signed.apk`. SHA-256: `5892adf76e2ea53f26688d5214f8542421e5eebe3a94baa0bfdec76a85389f3a`. APK v2/v3 signatures verified. The production verifier endpoint is packaged; the sandbox endpoint is absent.

All 19 locale packs and their support catalogues are bundled. Each catalogue has 335 messages; placeholder checks passed. Independent language review was waived. The final build passed 32 Android unit/render tests, eight localization tests, and release lint with 0 errors, 68 warnings and 9 hints.

The review build produced 175 physical Fire layout captures, including portrait and 200% text in English, German and Arabic. See `artifacts/reviewer-captures-0.3.7/manifest.json`. Separate marketing exports use eight physical Fire views per completed locale, including native receipt extraction on a fictional receipt. Only finished eight-image ZIP files in artifacts should be used.

The Fire App Tester build restored and verified a Premium licence on 0.3.7. Earlier sandbox purchase, restore and revocation coverage is documented in BILLING-0.3.4.md. An Amazon-distributed purchase and restore on this candidate are still unverified.

Amazon console, 2 November: both public and private-test drafts contain signed code 11. Fourteen storefront descriptions were completed. The £4.99 one-time entitlement is approved for TEST only. The private draft has one tester group containing only the approved Fire account. Draft preparation does not send an invitation.

Release gates: verify the finished localized galleries, obtain the publisher's Amazon export declaration, submit the private test, verify Amazon-distributed purchase and restore, and confirm the production entitlement and final public submission. Neither draft has been submitted.
