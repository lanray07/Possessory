# Possessory

Native, local-first home inventory app for compatible Fire tablets and Android devices. Kotlin, Jetpack Compose, Material 3, Room, DataStore, WorkManager and on-device Tesseract. No mandatory Google Play Services, account, analytics or cloud sync.

This is an engineering preview. Read [the release tracker](docs/RELEASE.md) for the complete implementation status and release blockers. Premium purchases are disabled until an Amazon product and the included verification service are configured; 19 locale packs are bundled as offline drafts in preview APKs and await language/layout review. Operator: O. Bankole. Support: banksmi@mail.com.

## Build

Install JDK 17 and Android SDK 36. Put `sdk.dir=YOUR/SDK/PATH` in local.properties, or configure ANDROID_HOME.

```powershell
.\gradlew.bat :app:assembleAmazonDebug :app:assembleStandaloneDebug :app:testStandaloneDebugUnitTest :app:lintAmazonDebug :app:lintStandaloneDebug
```

Debug APK: app/build/outputs/apk/amazon/debug/app-amazon-debug.apk. This APK uses a debug signing identity and is for development/testing only. The min SDK is 26; older Fire OS releases are excluded.

## Supporting tools

```powershell
python tools/resources.py
python tools/localize.py
python tools/brand_assets.py
```

Translation is development-only and free locally: install tools/requirements-localization.txt, run tools/localize.py --translate with that Python environment, then tools/localize.py --bundle-preview. Nineteen draft locale packs are compiled into preview APKs; models and Python are not shipped. Choose a language during onboarding or in Settings. Release builds use approved packs only; language and layout approval is still pending. See localization/README.md.

Inventory is empty on first launch. Deterministic demonstration data exists only in tests. Exports use Android’s document destination picker. Attachments live in private app storage, and deleting an item also deletes its attachment files. Uninstalling deletes local records; make a backup first.

Build release variants with `.\gradlew.bat :app:assembleAmazonRelease :app:assembleStandaloneRelease`. Sign the unsigned build output with `python tools/sign_release.py INPUT.apk OUTPUT.apk`. The signing tool uses a dedicated Possessory key in the access-restricted `~/.android/possessory-signing/` directory, outside this repository. Preserve both the keystore and its credentials securely; updates must use the same key. Never upload that directory to GitHub. The signed APK is a release build of the engineering preview, and does not itself constitute release approval.

Original brand files and a 1024×500 promotional banner are in assets/brand. The screenshot composition tool requires capture metadata, matching locales and existing real-device screenshots; it does not invent UI or use development renders as device captures. See tools/screenshot_pack.py.

Tests cover domain boundaries, encrypted backup authentication, backup validation, database integrity and rendered UI interactions. Device tests remain necessary for OCR native libraries, speech, camera, notifications and Fire OS behaviour.

Amazon and standalone variants share the inventory implementation. Only the Amazon variant includes the Appstore SDK, billing receiver and internet permission. Core Robolectric tests use the standalone variant because the vendor receiver cannot be verified by the JVM. This does not verify Amazon purchase integration; real Fire-device tests remain required. Standalone builds do not offer purchases.


## 0.3.0 reviewer handoff

Build `:app:assembleAmazonReview` for the separate app.possessory.review evaluation APK. It bundles nineteen offline draft locale packs and clearly labelled evaluation access. It does not grant or simulate an Amazon entitlement and must never be submitted to a store. The production release variant retains the English base while human locale approvals are pending.

Run `python tools/localization_quality.py`, `python tools/reviewer_evidence.py` (after physical captures), and `python tools/reviewer_handoff.py` to prepare the CSV/glossary/site/device handoff ZIP. Support and store changes invalidate the complete catalogue's review hash. See docs/RELEASE.md for evidence, remaining manual checks and production purchase/hosting gates. The approved Premium base price is £4.99 once; store pricing comes from Amazon.
