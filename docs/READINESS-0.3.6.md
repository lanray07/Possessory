# Possessory 0.3.6 readiness

28 October 2026. Version code 9 is a signed engineering candidate, not submission-approved. No Amazon upload or submission was performed in this pass. The production package is English-only; the separate review package contains all 19 draft locale targets.

## Changes and verification

- Fixed currency precision independently of the selected language. Previously, Japanese formatting could display GBP 499.99 as GBP 500. A regression test checks GBP, JPY and KWD across all 19 locale targets. All 32 Android unit/render tests pass.
- Android release lint completed with zero errors, 41 warnings and nine hints. The final label-only review rebuild also passed. No warning-free claim is made.
- Recorded 1,535 explicit AI editorial overrides across the non-English catalogues. Corrected privacy disclosures, backup semantics, receipt/crop labels, item conditions and repeated words. All 19 catalogues have 335 messages and pass structural validation; eight localization tests pass. This is not full linguistic approval.
- Fixed the Cloudflare verifier to accept authenticated Amazon publishing/LAT test receipts through production RVS. Client-supplied test flags remain rejected; cancellation and identity checks remain enforced. Nineteen Worker tests and ten Python verifier tests pass. Production deployment: `20ac6ca1-6c57-44ed-96ba-595a998b33ab`. Six endpoint checks pass; those checks are not a real LAT purchase.
- Added a local Chromium renderer for Arabic shaping and Hindi/Japanese fonts in marketing compositions. Typography proofs are separate from finished marketing exports.
- The final 0.3.6 review APK passed three physical Fire navigation runs: 133 standard captures across 19 locales, 21 captures at 200% text size and 21 portrait captures (English, German and Arabic). The Japanese overview visibly retains GBP 499.99. Capture hashes are indexed in `artifacts/reviewer-captures-0.3.6/manifest.json`. These are automated navigation/layout evidence, not full accessibility or language certification. Original tablet settings were restored and read back: keep-awake 0, text scale 1.0, auto-rotation 1, rotation 0.

## Artifacts

- `artifacts/Possessory-amazon-0.3.6-release-signed.apk`: production package, English-only candidate.
- `artifacts/Possessory-amazon-0.3.6-review-signed.apk`: isolated evaluation app with draft translations; never submit this package.
- `artifacts/Possessory-0.3.6-review-tests.apk`: instrumentation only; never submit.
- `artifacts/Possessory-0.3.6-localization-with-device-evidence.zip`: all locale review CSVs, 175 original captures, policy and review APK.
- `artifacts/Possessory-0.3.3-eight-marketing-screenshots-ENGLISH.zip`: eight previously completed English marketing images with original capture provenance. These have not been relabelled as new-version or localized captures.

Exact hashes and evidence belong in the versioned artifact manifest. The 0.3.5 device capture set is retained as historical evidence; it predates the currency correction.

## Remaining submission gates

1. Finish full-catalogue wording checks and production bundling. Independent human review was explicitly waived; no human approval is claimed or required by the current policy. Structural completeness alone does not establish semantic correctness.
2. Finish localized marketing variants from matching real app captures. Eight English assets exist; all-language asset sets do not yet exist.
3. Restore the inbuilt-browser automation runtime. Both browser and native control fail before connecting with `failed to write kernel assets ... The system cannot find the path specified (os error 3)`. This prevents observing or changing the current Amazon forms, uploading the new APK/assets and configuring LAT.
4. Configure the approved GBP 4.99 one-time entitlement for testing and run free Amazon Live App Testing purchase/restore/cancellation/account-change checks against the production package and verifier. Existing App Tester evidence is distinct from LAT validation.
5. Review the actual owner declarations. [Owner declaration handoff](AMAZON-OWNER-DECLARATIONS.md) supplies code-backed privacy/encryption facts without inventing legal certifications.
6. Complete remaining manual/device coverage: real multilingual receipt accuracy, multilingual speech/TTS, notification timing, TalkBack/keyboard navigation and additional device sizes/models. Existing synthetic OCR, automated navigation and backup tests do not certify these checks. The owner's earlier confirmation that the English voice test was audible remains valid; it does not establish all-language speech support.

No paid purchase, tester invitation, owner legal certification or store submission was made in this pass.
