# Bundled translation validation — 27 October 2026

Possessory engineering preview 0.2.0 (version code 2) contains 19 locale targets and 211 app strings per target, with no missing source keys. English regional variants use the source catalogue; other packs use local Argos/CTranslate2 generation plus AI-authored draft corrections. These packs are not human-reviewed or release-approved.

## Verified

- Five Python localisation-tool checks pass: source extraction, protected placeholders/brand, English voice-command literals, missing/empty values, static-resource bundling, stale-source rejection and release approval enforcement.
- Twenty-two Android JVM/Robolectric tests pass, including all previous domain/storage/backup checks, lookup of all 19 locale targets, saved language choice, Arabic RTL configuration and Indonesian resource lookup on Android API 28 and 35.
- Spanish and Arabic native-graphics screen renders passed and were visually inspected. User-entered room names remain unchanged.
- APK ZIP inspection confirms that both debug variants contain assets/localization.json listing all 19 locale targets with runtime_translation=false. No Argos model archive, translation model.bin, CTranslate2 library or Python file is present. The existing English receipt-OCR model is unrelated and remains bundled.
- Both Indonesian resource-code aliases are shipped for Android compatibility. Arabic, Polish and other applicable plural categories are present; grammatical review is still required.
- Gradle checks source/resource checksums before building and does not contact translation services or download translation models.

Choose a language during onboarding or in Settings → App language. The choice is stored privately. Recorded item names, notes, serial numbers, room names and receipt contents are not translated. Amounts remain in their recorded currencies.

Preview packs are packaged from app/src/preview/res into debug APKs. Release packaging uses approved resources only; no locale has yet met human-review/release-approval requirements. Current release builds retain the English base UI. No Appstore publishing was performed.

These checks verify bundling and representative resource/layout behaviour; they do not constitute linguistic approval or physical Fire-device certification. Native speech languages, English-only command parsing, text expansion, accessibility, regional terminology and legal wording still need release review. See localization/quality-report.json and docs/RELEASE.md.
