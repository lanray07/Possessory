# Possessory 0.3.2 billing and marketing handoff

Recorded 28 October 2026. Production APK remains version code 5, package `app.possessory`. This work changes test tooling and evidence, not production app code. No paid transaction, Amazon upload or app submission was performed.

## Local billing evidence

- The isolated debug package `app.possessory.iapqa` uses Amazon App Tester and the Cloudflare sandbox verifier. It is **not a store-upload APK**.
- Existing sandbox entitlement recovery and explicit restore passed with a cryptographically verified signed licence. This run can restore an existing App Tester purchase; it must not be described as proof of a new production purchase.
- A real sandbox-signed licence was rejected at its expiry boundary and when checked against the production environment.
- Injected SDK callback fixtures verified that changing the user clears Premium and its stored licence; FAILED and INVALID_SKU callbacks leave Premium off and clear busy state. Restoring the original App Tester user then restored verified Premium. These callback fixtures are not end-to-end Amazon account switching.
- A process restart with Wi-Fi disabled retained Premium from a valid cached licence. A separate offline restart with this disposable package's persisted clock floor advanced beyond expiry did not unlock Premium. The tablet clock was never changed; the stored floor was restored and the process stopped.
- Prior server fixtures cover cancellation, receipt mismatches and environment separation. Production/LAT receipt validation and account/refund scenarios remain separate release checks.

The final four instrumented scenarios passed; raw logs and hashes are in `artifacts/billing-marketing-checks-0.3.2/summary.json`. The original App Tester catalogue was restored byte-for-byte, sandbox mode and keep-awake settings were restored, Wi-Fi was enabled, and the two disposable QA packages were removed. Production inventory was not edited. Further App Tester UI mutations stopped after another app appeared during navigation; end-to-end cancellation/account switching therefore remain unverified.

## Eight marketing compositions

`artifacts/Possessory-0.3.2-eight-marketing-screenshots-DRAFT.zip` contains eight 1920×1200 RGB PNG compositions, original captures, a contact sheet and SHA-256 provenance manifest. All show the actual Fire tablet app with fictional Willow House records. Nothing is painted over or substituted inside the app UI.

The views are dashboard, rooms, warranties, receipt review, rendered PDF preview, voice interface, offline inventory and warranty details. Six use Compose navigation captures; receipt review and offline inventory were completed with native UI navigation and ADB screenshots. The receipt preview did not settle under the Compose capture clock but loaded correctly in the native app and was visually verified. Offline capture was taken with `dumpsys connectivity` reporting `Active default network: none`.

The Fire has no speech recognizer: the voice screenshot honestly shows the typed fallback and compatibility notice. The receipt is synthetic; this image is not a claim of real-world OCR accuracy. Premium was enabled by a verified sandbox entitlement, not an evaluation bypass.

All compositions are explicitly marked **DRAFT — NOT APPROVED FOR PUBLICATION**. They are English-only. Independent human review was subsequently waived by the owner on 28 October 2026; no human review is claimed. The original nineteen-locale scope, localized marketing variants and additional device variants are not declared complete.

Amazon's [tablet screenshot specifications](https://developer.amazon.com/docs/app-submission/appstore-details.html) were checked on 28 October 2026: 1920×1200 is accepted and eight screenshots are within the required three-to-ten range.

## Remaining submission gates

1. **Localization completion:** Independent human review is waived by explicit owner instruction ("ignore human review", 28 October 2026). It is no longer a blocker. Complete remaining technical/regional/layout checks, bundle the intended locales into the production candidate, and generate matching localized marketing. The existing production APK still bundles English only; the nineteen-locale scope has not been reduced.
2. **Amazon/LAT billing:** upload/test the production candidate and entitlement through LAT; verify production receipt processing, purchase/restore, cancellation/refund and account switching. App Tester uses sandbox trust and cannot certify the production route.
3. **Owner declarations:** review the prepared privacy questionnaire and processing disclosures, then make Amazon's export/encryption compliance declaration. These remain owner decisions.
4. **Console completion:** replace the last-observed code-2 binary with the approved signed candidate, upload approved assets, reconcile locale/device targeting and £4.99 one-time IAP setup, and clear Amazon's final validation. The current console state is unverified because the built-in browser helper fails before startup with a missing kernel-assets path.
5. **Remaining brief QA:** human screen-reader usability, real receipt accuracy review, long-duration reminder/power-management checks and additional physical-device coverage remain unverified. The corresponding automated/device checks are documented separately; they do not substitute for these reviews.

The release has not been submitted and must not be described as fully ready while these gates remain open.
