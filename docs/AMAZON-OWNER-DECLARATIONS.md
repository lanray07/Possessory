# Amazon submission: prepared facts and remaining operator decisions

Prepared 28 October 2026 for O. Bankole, support banksmi@mail.com. These are evidence-based form notes, not signed declarations. The live console could not be inspected because the inbuilt browser control failed during initialization.

## Privacy questionnaire

Answer the app-level collection question **Yes**. Possessory transmits Amazon user IDs and purchase receipt IDs for purchase verification. The verifier receives connection IP addresses. Advertising and analytics are absent from the app. Inventory, photographed receipts, room photos, notes and serial numbers are not included in verifier requests.

| Data | Prepared answer / purpose | Evidence and qualification |
| --- | --- | --- |
| User IDs | Collected; app functionality | Amazon user ID reaches the Cloudflare verifier and Amazon RVS. |
| Purchase history | Collected; app functionality | Receipt ID and entitlement status validate purchase/restore. |
| Device or other IDs | Collected; security/fraud prevention | Connection IP is used for rate limiting; request logging is disabled. |
| Voice or sound recordings | Optional feature; disclose provider processing | Android's speech provider may transmit audio. Possessory does not save raw recordings. Fire has no available recognizer; that does not establish practices on other devices. |
| Payment details | Not accessible to Possessory | Amazon handles card/bank details outside the app. |
| Inventory/photos/files | On-device storage, no developer upload | Explicit exports may use the document provider selected by the user. |

Apply Amazon's service-provider and user-initiated-transfer exceptions to the actual console questions. Do not declare all network operations to be reportable third-party transfers, or declare every SDK data practice exempt. The IAP SDK documents user ID, marketplace, country and purchase-history responses, but this is not a complete SDK privacy attestation. Confirm the operator's actual support email handling and any infrastructure outside this repository before final submission.

## Encryption/export facts

The app **uses encryption**. Password-protected backups use AES-256-GCM with a 128-bit authentication tag and PBKDF2-HMAC-SHA256 (600,000 iterations). Purchases use HTTPS and signed RSA-SHA256 entitlements. Plain ZIP backups and exports are unencrypted. Device-unlock protection does not independently encrypt the local inventory database or attachments.

These facts come from `BackupCrypto` in `app/src/main/java/app/possessory/services/Files.kt`, `LicenseVerifier.kt`, and the Cloudflare verifier. They are not an export classification, licence determination or certification of worldwide legal compliance. The owner must review the exact Amazon export statement when the console is accessible. No box has been checked and no owner signature is inferred.

## LAT setup prepared

Use entitlement `possessory.premium.lifetime`, display name Possessory Premium, GBP 4.99 one-time base price. Submit the IAP to **Test**, then use the LAT-distributed production package with approved testers. LAT purchases are free. Production RVS must verify purchase/restore; App Tester receipts remain on the sandbox endpoint. Do not substitute a sideloaded QA package for the LAT distribution.

The production Worker now accepts Amazon-verified LAT/publishing-test transactions. It still rejects malformed flags, wrong product/receipt/type, arbitrary client fields, invalid Amazon responses and effective cancellations. Local tests are not evidence that an actual LAT transaction has passed. The remaining device sequence is LAT purchase, restore after restart, account change, cancellation/revocation and retained inventory verification. No test invitations or real paid orders have been sent.

Sources checked 28 October 2026: [privacy definitions](https://developer.amazon.com/docs/app-submission/appstore-privacy-labels.html), [SDK responses](https://developer.amazon.com/docs/in-app-purchasing/iap-implement-iap.html), [LAT for IAP](https://developer.amazon.com/docs/in-app-purchasing/iap-lat.html), [RVS fields](https://developer.amazon.com/docs/in-app-purchasing/iap-rvs-for-android-apps.html).
