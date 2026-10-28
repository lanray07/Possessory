# Possessory 0.3.1 submission handoff

Candidate: version code 4, package `app.possessory`. This is a review candidate. The Amazon draft was last observed with version code 2 and eight earlier screenshots. No app or IAP submission has been performed.

| Check | Evidence or next action |
| --- | --- |
| Release signing | Existing Possessory certificate; v2/v3 signatures. The release contains production verifier trust only. |
| Automated app checks | Current results are in artifacts/submission-checks-0.3.1.json. |
| Localization | 19 offline draft packs, 335 messages each, with key/placeholder coverage. Human sign-offs remain zero. Updated drafts include Cloudflare privacy/support disclosures and clearer free/Premium store copy. |
| Reviewer screenshots | 175 fresh 0.3.1 review captures passed navigation assertions: 133 standard, 21 at 200% text size and 21 portrait. APK/source/image hashes are in artifacts/reviewer-captures-0.3.1/manifest.json. Seven separate current production store screenshots are in artifacts/store-captures-0.3.1. Review captures show draft/evaluation labels and must not be used as store screenshots. |
| Native imports | PNG/PDF content-provider imports preserved bytes and rendered on Fire. Synthetic receipt selection through the real system picker passed with exact imported-byte SHA-256 preservation. Real receipt OCR accuracy remains a separate manual check. |
| Native reminders | A due synthetic fixture ran through WorkManager, posted a notification and was cancelled by the app's opt-out setting. Long-running power-management delivery remains a manual check. |
| Speech/TTS | KFTRWI has no recognizer. English TTS initialized; German/Arabic/Japanese/Hindi were unsupported. Typed fallback without microphone permission passed. English TTS completed and the owner confirmed the phrase was clearly audible on 28 October 2026. This is not a TalkBack or multilingual voice sign-off. Do not claim universal voice-language support. |
| Camera/picker | Document-picker and camera cancellation passed without adding attachments. Positive system-picker selection also passed. Owner-captured camera JPEG import passed (2592 × 1944); disposable fixture records and the imported photo were removed. |
| App lock | Owner-assisted enable/unlock passed, along with credential cancellation, background/recreation relocking and secure-window screenshot protection. The isolated review app lock was restored off; the device credential was unchanged. |
| Accessibility/devices | Existing RTL/large-font/portrait renders are automated evidence. TalkBack, keyboard focus and additional physical devices need sign-off. |
| Billing | Cloudflare endpoints and App Tester purchase/restore were verified. Worker fixtures cover cancellation/mismatches/environment separation. Production/LAT cancellation, account changes and offline-expiry device behavior remain unverified. |
| Privacy form | Reconcile purchase/user identifiers, purchase history, optional device-provider speech and Cloudflare processing with Amazon's questionnaire definitions. Operator review is required. Possessory does not upload inventory/photos. |
| Listing | Candidate copy/instructions are in store/amazon-submission-draft.json and store/amazon-testing-instructions.txt. Confirm the candidate's filename/version in the console before calling it uploaded. |
| Export declaration | Amazon requires the operator to certify import/export compliance, including encryption. It remains unchecked. This is an owner legal declaration. |

The master brief makes localization a release requirement and requires human review. The owner requested reviewer handoff packs. Do not mark machine drafts human-reviewed or silently waive the requirement. An English-first release needs an explicit owner scope decision.

For remaining billing validation on Fire tablets, use Amazon Live App Testing (LAT) with approved testers. Amazon documents test-only IAP submission and no-charge LAT purchases. Submit the entitlement to **Test**, prepare a LAT binary/metadata, obtain owner declarations and validate the Cloudflare verifier with Amazon receipts. Do not promote to Test and Live or make a paid purchase merely to close a checklist item.

Sources checked 27 October 2026: [IAP test submission](https://developer.amazon.com/docs/in-app-purchasing/iap-create-and-submit-iap-items.html), [testing tools and no-charge LAT](https://developer.amazon.com/apps-and-games/blogs/2025/10/how-to-choose-the-right-tool-for-IAP-testing), [privacy labels](https://developer.amazon.com/docs/app-submission/appstore-privacy-labels.html).
