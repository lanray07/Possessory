# Bundled APK localisation

The 0.3.2 engineering preview contains static Android resource packs for all 19 requested locale targets. Local Argos/CTranslate2 models generate the drafts during development; only text resources and a small locale manifest enter the APK. Models, Python, API keys and a runtime translation service are not included. The initial model download requires internet on the developer's computer. Translating and using the packaged app strings does not.

Choose the app language during onboarding or under Settings â†’ App language. Device language remains the default. Dates and currencies follow the selected locale; recorded amounts and currency codes are not converted. User-entered item names, room names, notes, receipt contents and serial numbers are not translated.

## Rebuild translations

```powershell
.\.venv\Scripts\python.exe -m pip install -r tools/requirements-localization.txt
.\.venv\Scripts\python.exe tools/localize.py --translate
.\.venv\Scripts\python.exe tools/localize.py --bundle-preview
.\gradlew.bat :app:assembleAmazonDebug :app:assembleStandaloneDebug
```

English variants use the source catalogue and regional spelling. Other languages use the official Argos packages, with AI-authored draft corrections to core interface labels in overrides.json. Spanish/Latin American Spanish and French/French Canadian packs currently share base-language translations; regional terminology still needs review. Model download URLs, versions and SHA-256 hashes are recorded in model-provenance.json. Model binaries stay in ignored cache/models and are not distributed in the app or source archive.

The bundler validates every string key, placeholder and brand token against the source and checks source/resource hashes. Gradle refuses to build when bundled packs are stale or modified without rebundling. It never contacts a translation service or downloads models during an APK build.

## Preview versus release

All generated packs are **drafts**. No language is marked human-reviewed or release-approved. Preview resources live in app/src/preview/res and are included in debug and separate review builds. Release resources live in app/src/localized/res and are populated only by `tools/localize.py --bundle-approved` after the status record meets the existing review/approval checks. Current release builds retain the English base resources. Do not publish the debug APK as an approved worldwide release.

Language quality, regional terminology, plural grammar, accessibility, truncation and device layout still require review, particularly Arabic and Hindi. Localized voice phrase dictionaries are bundled; English fallback remains available. Device recognition and TTS language support must be checked independently. The translator does not add voice-engine languages or change warranty/legal semantics by design; machine output still requires qualified review.

Optional DeepL support remains available with `--translate --provider deepl` and a development environment key. It is not used by the bundled free workflow.

## Human reviewer handoff

Run `python tools/localization_quality.py` then `python tools/reviewer_handoff.py`. The ZIP contains one CSV and device sign-off checklist per locale, paired with a source hash. Review all 335 messages, including plural forms, store/IAP, support and privacy. Record corrections, the real reviewer name, decisions and device observations. No human approval is inferred from coverage or model output.

Build the isolated evaluation app with `:app:assembleAmazonReview`. Sign it with the existing signing tool. Its package is app.possessory.review and its banner explicitly states that evaluation access grants no purchase entitlement. Do not submit that APK. Device screenshots are review evidence, not proof of linguistic correctness.

Return completed reviewer files to the owner. Corrections must be applied to generated XML/JSON, validated and rebundled; source changes invalidate sign-off for changed messages. Promotion still requires the existing human-review and owner-approval checks in tools/localize.py. Draft site pages are generated with tools/support_site.py and are not published automatically.

## AI editorial corrections

The owner requested an editorial review by the assistant. localization/ai-editorial-review.json records the actual limited scope: ten critical operational messages across sixteen non-English targets, with 160 draft entries corrected. This does not mean all 335 messages per locale have been reviewed. English source variants were not rewritten in this pass. Independent human review, regional validation and owner release approval remain pending. No approval status is changed.

Run `python -X utf8 tools/apply_editorial.py`, `python -X utf8 tools/localize.py --bundle-preview` and `python -X utf8 tools/localization_quality.py` to apply and validate these recorded corrections without downloading a model. Future generation also applies editorial-overrides.json in the resource writer. The reviewer ZIP includes the AI editorial report separately from human sign-off forms.
