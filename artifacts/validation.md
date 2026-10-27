# Possessory validation — 27 October 2026

Engineering preview 0.1.0 (version code 1). This is not a production release approval.

## Completed checks

- Amazon debug APK, standalone debug APK and unsigned Amazon release APK built successfully with Gradle 8.13 / JDK 17 / Android SDK 36. Minimum Android API 26, target API 36.
- Latest combined build: `assembleAmazonDebug assembleStandaloneDebug testStandaloneDebugUnitTest lintAmazonDebug lintStandaloneDebug assembleAmazonRelease` — BUILD SUCCESSFUL.
- 17 Android JVM/Robolectric tests passed: seven inventory rules, one signed-license verification, four rendered UI/navigation, one restore/merge and four storage/receipt-validation tests.
- Four Python receipt-verification tests passed, including cancellation, product/type mismatch, test-transaction rejection and RSA signature verification.
- Three translation-pipeline tests passed, covering placeholder/brand protection, missing/empty strings and exclusion of operator identity/contact from translation.
- Amazon debug lint: zero errors, 23 warnings. Standalone debug lint: zero errors, 24 warnings. Warnings include pinned dependency updates, pluralisation review and resource cleanup; they are not represented as completed localisation QA.
- Debug APK signature checked with Android apksigner. Standalone manifest has no INTERNET permission; Amazon variant includes it for billing/verification. No Google Play Services or Play Billing dependency is required.
- Inspected actual Compose native-graphics renders. Full application navigation reads Room records and opens warranty, report and settings screens. Wordmark wrapping was found and repaired. These are Robolectric development renders, not screenshots captured from Fire hardware.
- Backup tests verify attachment preservation, checksum failure, traversal rejection, database foreign keys/cascades and idempotent restore that preserves newer local records. Encryption tests verify round trip, wrong-password rejection and ciphertext tampering. Aggregate purchase values have an integer-overflow regression test.
- 30 original code-authored brand asset files exist. Translation extraction tracks 221 app/store/support messages across 19 locale records; zero locales are release-approved. No translation-provider credentials were available and no translation API was called.

## Limits of this evidence

No physical Fire tablet was connected. The configured Android emulator could not start with its missing acceleration driver; software fallback exited during startup. Camera behaviour, native Android OCR execution, speech/TTS, notification timing and real Amazon purchase/restore/refund flows remain unverified on devices. Server tests use fixtures and temporary signing keys, not real Amazon receipts.

Release minification is disabled because Amazon SDK 3.0.9 reports stack-map warnings through R8. The Amazon receiver also fails JVM verification in Robolectric, so core tests use the standalone variant. The Amazon variant compiles with its real SDK; this does not prove its runtime billing behaviour.

The release APK is unsigned. The debug APKs are installable development builds, not submission binaries. Billing is disabled until the SKU, HTTPS verification service and public signing key are configured. There is no deployed verification service, public marketing site, approved privacy policy or approved worldwide translation/screenshot set.

The [release tracker](../docs/RELEASE.md) records additional unfinished requirements, including app lock, expanded voice commands, advanced filtering/saved searches, richer reports, maintenance history/document improvements and accessibility/localisation/device QA. The full master brief remains unfinished.

Operator and support details supplied by the owner: O. Bankole, banksmi@mail.com. No store submission or publishing was performed.
