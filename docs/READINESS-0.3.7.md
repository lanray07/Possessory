# Possessory 0.3.7 — Amazon release candidate (version code 11)

The signed release is in `artifacts/Possessory-amazon-0.3.7-localized-release-signed.apk`. SHA-256: `5892adf76e2ea53f26688d5214f8542421e5eebe3a94baa0bfdec76a85389f3a`. APK v2/v3 signatures verified. The production verifier endpoint is packaged; the sandbox endpoint is absent.

All 19 locale packs and their support catalogues are bundled. Each catalogue has 335 messages; placeholder checks passed. Independent language review was waived. The final build passed 32 Android unit/render tests, eight localization tests, and release lint with 0 errors, 68 warnings and 9 hints.

The review build produced 175 physical Fire layout captures, including portrait and 200% text in English, German and Arabic. See `artifacts/reviewer-captures-0.3.7/manifest.json`. Separate marketing exports use eight physical Fire views per completed locale, including native receipt extraction on a fictional receipt. Only finished eight-image ZIP files in artifacts should be used.

The Fire App Tester build restored and verified a Premium licence on 0.3.7. Earlier sandbox purchase, restore and revocation coverage is documented in BILLING-0.3.4.md. An Amazon-distributed purchase and restore on this candidate are still unverified.

Amazon console, 3 November: the private test is in progress and the single invitation was delivered. The public draft contains signed code 11 and is Ready to Submit. The publisher authorized the public export declaration. The reviewer instructions and purchase-data questionnaire have been updated. Fourteen storefront descriptions and all ten applicable Amazon screenshot galleries are complete.

The complete local marketing archive is artifacts/Possessory-0.3.7-152-localized-marketing-APPROVED.zip. Its manifest gives file hashes and records the physical Fire capture evidence for eight images in each of 19 locales.

The one-time £4.99 entitlement was approved for test. The Amazon-distributed purchase and restore still require the tester account to complete two-step verification and install the private test. Production entitlement promotion and public submission remain pending.

On 3 November the Fire installed code 11 from com.amazon.venezia. Amazon's production SDK reported CERT_NOT_FOUND for AppstoreAuthenticationKey.pem. Premium purchase was disabled and restore did not grant an entitlement. Code 11 must not be submitted. The previous inventory backup was restored successfully. Code 12 adds Possessory's Amazon-issued SDK public key to the Amazon flavor. Purchase and restore must pass on the Amazon-delivered replacement before public submission.

Amazon accepted the signed code 12 APK in the public draft and confirmed Test submitted for its private test. Signed candidate SHA-256 is f20c9eb0f3c19501901437b6a0b7790a30d8da5e5c3554acbfc7d2ebe3146bb8. Both APK v2 and v3 signatures verified. The 32 unit/render tests passed. No paid purchase took place. The record backup remains in the Fire Downloads folder.
