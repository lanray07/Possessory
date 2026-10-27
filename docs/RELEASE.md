# Possessory release tracker

Version 0.3.0 is a release candidate for review, not submission approval. No Amazon submission has been performed. The master brief requires independent human language review; the owner requested reviewer handoff packs rather than waiving that requirement.

## Implemented in the candidate

Native Kotlin/Compose inventory, household management, room photos, record editing/duplicate/move/archive/status, separate totals per currency, locale-aware date and amount presentation, condition/date/value filters and saved searches. Saved numeric filters retain their input locale. Room database schema v2 has an explicit v1 migration without destructive fallback.

Private photo/PDF imports and camera capture, original-file retention, interactive crop/rotation, page selection and fourteen offline OCR models. Extraction suggestions require user review. Constrained localized voice phrases support navigation, search, item drafts, confirmed moves, room counts and spoken warranty reminders; device speech and TTS availability vary.

Multiple warranty lead times and custom dates; opt-in daily reminders catch up after background delays within seven days. Editable maintenance records, recurring completion history and supporting documents. PDF/CSV/JSON reports include optional serials, prices, photos (PDF) and document references, category/currency/high-value filters, pagination and native PDF preview. Private notes/descriptions/tags remain excluded from reports.

Optional native device-credential app lock and secure-window protection. No app PIN is stored. Private database/attachments are not separately encrypted. Plain ZIP and authenticated AES-GCM backups include room photos and saved searches, checksums, archive/reference checks and existing-record-wins merge. Password-protected export streams encryption; failed authenticated imports are discarded before preview/restore. Maximum decompressed restore is 100 MiB, individual imports 25 MiB. Metadata is capped at 8 MiB. Restore previews stage attachments in private cache instead of retaining their complete contents in RAM. The physical Fire tablet passed the 99 MiB encryption/decryption/checksum/staged-preview round trip in 35.374 seconds.

Premium feature gates use verified entitlements in production. Existing records, ordinary editing/deletion and restoring backups remain available without Premium. The separate app.possessory.review package provides clearly labelled evaluation access with no purchase or entitlement simulation. It must never be uploaded as the store binary.

All nineteen offline draft locale packs cover the current 335 source messages, including app/plurals, store/IAP and support. Text resources, not translation engines or credentials, are bundled. Review builds include drafts; production builds exclude unapproved packs. The reviewer archive contains paired source/translation CSVs, explicit sign-off fields and device-review checklists. Generated marketing/support pages under docs/site are noindex drafts, not a published site.

## Submission gates still open

- Independent human review/sign-off for all nineteen locales, regional wording, plural grammar, voice phrases and store/support/privacy claims. Do not promote machine drafts by editing approval flags without evidence.
- Remaining device sign-off: real receipt accuracy, app-lock lifecycle with the owner’s credential, external camera/document flows, speech/TTS, notification timing, TalkBack/keyboard and additional devices. Automated native OCR/PDF/backup checks, seven-screen navigation in nineteen locales, and English/German/Arabic 200% font and portrait checks passed on KFTRWI API 28. These checks do not constitute human language or accessibility approval.
- Cloudflare production and sandbox receipt verifiers are deployed with separate private RSA keys, private Amazon verification configuration, request limits and rate limits. See server/cloudflare/README.md and artifacts/cloudflare-verification.json for the current evidence. No paid plan or account upgrade was requested. Production transactions and the updated Cloudflare privacy disclosure still require validation/review.
- Amazon App Tester purchase and restore passed on the Fire tablet through the deployed Cloudflare sandbox and Amazon RVS, including Android verification of the returned RSA signature. Production purchase/restore, cancellation, offline-expiry and account-change device checks remain open. Server fixtures do not replace those checks. The distinct Cloudflare candidate has a configured production URL/public key; earlier APKs retain their prior configuration.
- Amazon entitlement draft exists: SKU possessory.premium.lifetime, product ID amzn1.dex.title.824c8f1e78d840efbd861d267e937cce, one-time base price £4.99 GBP approved by the owner. Console currently says Ready to Submit; it has not been submitted. Regional prices come from Amazon, never a hardcoded app conversion.
- Update the Amazon binary, localized listing/screenshots, privacy disclosures and test instructions only after the candidate passes its gates. The current uploaded draft is the earlier signed 0.2.0 English binary and eight real Fire captures.
- Owner review of final legal/privacy/export declarations and release approval. Export declaration remains unchecked. No unsupported legal or human-language certification has been made.
- Release shrinking remains disabled for Amazon SDK 3.0.9 stack-map issues; D8 packages the real SDK. Core Robolectric tests use the standalone variant because vendor obfuscated bytecode fails JVM verification.

The dedicated APK signing key is outside the repository under the owner's Android signing directory. Preserve it and its credentials securely. Never commit or upload signing keys, Amazon shared secrets, license private keys or development environments.

## Official Amazon requirements checked 27 October 2026

[Android Appstore distribution change](https://developer.amazon.com/apps-and-games/blogs/2025/02/upcoming-changes-to-amazon-appstore-for-android-devices-and-coins-program): non-Amazon Android Appstore support ended 20 August 2025; Fire tablets remain supported. Android compatibility is a separate distribution decision.

[Appstore details and image specifications](https://developer.amazon.com/docs/app-submission/appstore-details.html): tablet listing requires 114px and 512px PNG icons and 3–10 screenshots. Accepted screenshot sizes include 1280×800 and 1920×1200; portrait reversal is supported. Optional promo image is 1024×500. Recheck the console at submission.

[International distribution](https://developer.amazon.com/docs/app-submission/distribute-your-app-internationally.html): binary languages, listing languages, marketplace routing and country availability differ. The source lists thirteen listing localisations, including Dutch, Australian/Indian/US English, French, German, Italian, Japanese, Polish, Brazilian/European Portuguese, Spanish and Swedish. Device voice and TTS support must be detected separately.

| Retail marketplace | Main source-listed language | Currency | App locale targets relevant to planning |
| --- | --- | --- | --- |
| Amazon.com | English US | USD | en-US |
| Amazon.ca | English / French | CAD | fr-CA, English fallback |
| Amazon.com.mx | Spanish | MXN | es-419 |
| Amazon.com.br | Portuguese | BRL | pt-BR |
| Amazon.co.uk | English UK | GBP | en-GB |
| Amazon.de | German | EUR | de-DE |
| Amazon.fr | French | EUR | fr-FR |
| Amazon.it | Italian | EUR | it-IT |
| Amazon.es | Spanish | EUR | es-ES |
| Amazon.co.jp | Japanese | JPY | ja-JP |
| Amazon.in | English India | INR | English fallback; hi-IN app target |
| Amazon.com.au | English Australia | AUD | en-AU |

This matrix is planning data, not a claim that every regional variant has its own listing tab or that all target languages map to a dedicated marketplace. Arabic, Hindi, Indonesian and Turkish app languages do not imply matching storefront languages. Ireland routing depends on device/account history; verify distribution selections in the developer console.

## Build and verification

Use JDK 17, Android SDK 36 and the checked-in Gradle 8.13 wrapper. Set sdk.dir in ignored local.properties or ANDROID_HOME. Run `.\gradlew.bat :app:assembleAmazonDebug :app:assembleStandaloneDebug :app:testStandaloneDebugUnitTest :app:lintAmazonDebug :app:lintStandaloneDebug`. Gradle release output is unsigned; use tools/sign_release.py to produce the separately signed release artifacts. Never submit a debug APK.

Validation results are recorded separately in artifacts/validation.md after commands complete. Native-graphics Robolectric renders exercise the implemented Compose screens; they are not Fire-device captures. The emulator could not boot because hardware acceleration was unavailable and software fallback exited. A physical KFTRWI Fire tablet running Android API 28 was subsequently connected and used for signed-release installation, launch, backup restore and screen-capture checks.

## Candidate device evidence

The signed production update from 0.2.0 to 0.3.0 preserved Willow House, at least three rooms, six existing items and the maintenance record on the connected Fire tablet. The read-only UpgradeDeviceTest requires the pre-existing demo fixture and the explicit `existingDemoUpgrade=true` instrumentation argument; it must never seed or erase production records. Review tools run only in the isolated .review package. Test classes skip incompatible variants.

Review APK verification command: build with `-PdeviceTestBuildType=review`, install the equally signed app/test APKs, and run AndroidJUnitRunner with `-e class app.possessory.ReleaseDeviceTest`. Production upgrade verification uses `-PdeviceTestBuildType=release`, `-e class app.possessory.UpgradeDeviceTest -e existingDemoUpgrade true`. The production upgrade test is opt-in because it checks the previously installed demo dataset.

The reviewer handoff contains 175 unmodified physical-device captures: 133 standard captures (19 locales × 7 screens), 21 at 200% text size, and 21 portrait captures. The latter sets cover en-GB, de-DE and ar. It includes the review APK digest, per-image hashes, source-catalogue digest and explicit false human-approval flags. Some machine phrases remain untranslated or inaccurate; reviewer CSVs flag suspected fragments. Do not publish draft website pages or store assets before review.
