# Possessory release tracker

This repository is an engineering preview, not a production or Amazon submission approval. The source brief ends mid-sentence in section 21. No publishing has been performed.

## Implemented

Native Kotlin/Compose app, adaptive tablet navigation and compact navigation, light/dark appearance, optional household setup, Room persistence with foreign keys and exported schema v1, DataStore settings, inventory CRUD/duplicate/archive/status/move, custom rooms, record search, room/category/status/receipt/maintenance filters and sorting. Purchase and estimated values are distinct, using integer minor units per currency. Totals remain separate per currency.

Private photo/receipt/PDF imports, external camera capture through FileProvider, attachment viewing, first-page PDF review, image rotation and edge trimming, on-device Tesseract English OCR, editable extraction suggestions, warranty dates and configurable lead-time presets, daily opt-in WorkManager reminders, recurring maintenance tasks, typed/push-to-talk constrained English commands with capability checks and TTS. Speech availability and network behaviour depend on the device provider.

PDF/CSV/JSON export with serial/value redaction; complete ZIP backup with SHA-256 attachment checksums, archive traversal/size checks, record-reference validation, import preview, stable-ID duplicate detection and existing-record-wins merge. Optional AES-256-GCM backups use random 128-bit salt, 96-bit IV and PBKDF2-HMAC-SHA256 with 600,000 iterations. No account, ads, analytics or silent sync. Database and attachments are not separately encrypted by this app. Network permission supports Amazon billing and the configured receipt-verification endpoint; inventory and photographed receipts are not sent to that service. Platform speech/camera/document handlers have their own permissions and behaviour.

Original geometric icon and SVG illustrations are authored by tools/brand_assets.py. They are original project artwork, not stock photography. Locale pipeline extracts app strings, plurals, store/support text and screenshot captions. The 0.2.0 preview bundles 19 static locale packs generated locally with free Argos/CTranslate2 models and draft editorial corrections. Model weights and a translation service are not shipped. In-app language selection is available on older Fire/Android versions as well as device-language fallback. No locale is human-reviewed or release-approved yet.

## Release blockers and unfinished requirements

- Release minification is disabled: SDK 3.0.9's obfuscated bytecode reports missing stack-map tables in R8 and cannot be verified by Robolectric's JVM. The Amazon variant packages the vendor SDK through D8; validate real Fire purchase/restore behaviour before considering shrinking. Core tests use the standalone variant and do not substitute for vendor integration tests.

- Amazon product configuration, deployment of the included receipt-verification service, sandbox/production key separation, and real purchase/restore/cancellation tests. The app includes SDK 3.0.9 callbacks, store price lookup, restore pagination, server RVS verification, signed cached entitlements, expiry handling and failure recovery. Missing build configuration disables purchase initiation. No purchase success or premium entitlement is simulated. Verified Premium removes household/item limits. Existing records remain accessible on expiry. Expanded tools are exposed for engineering evaluation in this preview; final feature-level gating needs product approval and testing.
- Final package identity, trademark clearance, public support/policy domain, privacy approval, release signing identity and developer-console product configuration. Owner supplied operator O. Bankole and support banksmi@mail.com. Do not reuse another project's signing key.
- All 19 requested locales need completed translations, glossary/human review, plural-category review, resource coverage, regional terminology, RTL and text expansion QA. Room/category/condition/status labels now use translated resources. Report column headers still need resource extraction before release. The 0.2.0 preview bundles 19 draft locale packs; release builds exclude them until approved.
- Actual Fire tablet testing, Android compatibility testing, accessibility/font scaling/keyboard/TalkBack checks, camera/OCR/voice capability coverage, background notifications and rotation/state restoration checks.
- Genuine device captures: eight store compositions per approved listing locale and supported form factor. No device screenshots are claimed. Robolectric render files, if generated, are development evidence only and must not be described as Fire device captures.
- Expanded voice creation/move/count commands and locale dictionaries; current parser only navigates/adds through a form and never deletes by voice.
- Arbitrary interactive receipt cropping, multi-page OCR, additional OCR languages and extraction-quality evaluation; current trim removes 10% from each edge of the review image without altering the retained original.
- Optional app lock; saved searches; date/value/condition search filters; custom reminder dates/multiple reminders; editable maintenance history and supporting maintenance documents; room photography; property management after verified purchase.
- Richer report options (category/high-value selections, photo/receipt references, localised column labels) and persistent multi-page preview; current preview shows first six filtered items, PDF is text-based and pagination wraps long fields.
- Marketing website, localised support content, complete onboarding illustrations integration and approved store screenshot compositions. An original promotional banner and a guarded composition script exist; the script requires eight matching real-device captures and blocks unapproved locales unless explicitly generating drafts.
- Schema migration fixture tests when v2 exists. Current v1 intentionally has no destructive-migration fallback.
- Encrypted backup recovery UX and performance/memory review at maximum supported archive size. Current attachment import is capped at 25 MiB each and restore is capped at 100 MiB decompressed. Large backups beyond that cap need streaming or segmented backup work before release.

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

Use JDK 17, Android SDK 36 and the checked-in Gradle 8.13 wrapper. Set sdk.dir in ignored local.properties or ANDROID_HOME. Run `.\gradlew.bat :app:assembleAmazonDebug :app:assembleStandaloneDebug :app:testStandaloneDebugUnitTest :app:lintAmazonDebug :app:lintStandaloneDebug`. Release output is unsigned until the owner supplies a signing configuration. Never submit a debug APK.

Validation results are recorded separately in artifacts/validation.md after commands complete. Native-graphics Robolectric renders exercise the implemented Compose screens; they are not Fire-device captures. The available emulator currently cannot boot: its hardware acceleration driver is missing and software fallback exits during startup. No physical Fire tablet is connected.
