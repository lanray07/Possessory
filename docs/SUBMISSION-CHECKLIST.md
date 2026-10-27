# Possessory 0.3.1 submission handoff

Candidate: version code 4, package `app.possessory`. This is a review candidate. The Amazon draft was last observed with version code 2 and eight earlier screenshots. No app or IAP submission has been performed.

| Check | Evidence or next action |
| --- | --- |
| Release signing | Existing Possessory certificate; v2/v3 signatures. The release contains production verifier trust only. |
| Automated app checks | Current results are in artifacts/submission-checks-0.3.1.json. |
| Localization | 19 offline draft packs, 335 messages each, with key/placeholder coverage. Human sign-offs remain zero. Updated drafts include Cloudflare privacy/support disclosures and clearer free/Premium store copy. |
| Reviewer screenshots | Earlier 175 captures are historical 0.3.0 layout references, not evidence for changed text. Current captures require the owner to unlock the Fire tablet. |
| Native imports | PNG/PDF content-provider imports preserved bytes and rendered on Fire. Actual receipt selection through the external picker remains unverified. |
| Native reminders | A due synthetic fixture ran through WorkManager, posted a notification and was cancelled by the app's opt-out setting. Long-running power-management delivery remains a manual check. |
| Speech/TTS | KFTRWI has no recognizer. English TTS initialized; German/Arabic/Japanese/Hindi were unsupported. Typed fallback and audible output need interactive review. Do not claim universal voice-language support. |
| Camera/picker | Handlers exist. Interactive cancellation/selection checks are blocked by the secure lock screen. Unlock on-device; never send the credential to chat. |
| App lock | A system credential is configured. The owner must complete native prompts for enable/unlock, cancellation and background/reopen checks. No credential bypass or device-security change is appropriate. |
| Accessibility/devices | Existing RTL/large-font/portrait renders are automated evidence. TalkBack, keyboard focus and additional physical devices need sign-off. |
| Billing | Cloudflare endpoints and App Tester purchase/restore were verified. Worker fixtures cover cancellation/mismatches/environment separation. Production/LAT cancellation, account changes and offline-expiry device behavior remain unverified. |
| Privacy form | Reconcile purchase/user identifiers, purchase history, optional device-provider speech and Cloudflare processing with Amazon's questionnaire definitions. Operator review is required. Possessory does not upload inventory/photos. |
| Listing | Candidate copy/instructions are in store/amazon-submission-draft.json and store/amazon-testing-instructions.txt. Confirm the candidate's filename/version in the console before calling it uploaded. |
| Export declaration | Amazon requires the operator to certify import/export compliance, including encryption. It remains unchecked. This is an owner legal declaration. |

The master brief makes localization a release requirement and requires human review. The owner requested reviewer handoff packs. Do not mark machine drafts human-reviewed or silently waive the requirement. An English-first release needs an explicit owner scope decision.

For remaining billing validation on Fire tablets, use Amazon Live App Testing (LAT) with approved testers. Amazon documents test-only IAP submission and no-charge LAT purchases. Submit the entitlement to **Test**, prepare a LAT binary/metadata, obtain owner declarations and validate the Cloudflare verifier with Amazon receipts. Do not promote to Test and Live or make a paid purchase merely to close a checklist item.

Sources checked 27 October 2026: [IAP test submission](https://developer.amazon.com/docs/in-app-purchasing/iap-create-and-submit-iap-items.html), [testing tools and no-charge LAT](https://developer.amazon.com/apps-and-games/blogs/2025/10/how-to-choose-the-right-tool-for-IAP-testing), [privacy labels](https://developer.amazon.com/docs/app-submission/appstore-privacy-labels.html).
