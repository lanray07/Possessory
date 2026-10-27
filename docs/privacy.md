# Possessory privacy notice

Effective date: 27 October 2026.

Operator: O. Bankole. Contact: banksmi@mail.com.

This notice describes Possessory version 0.3.1, a release candidate. It will be updated before introducing materially different data practices.

Possessory stores household records, photos, receipts, warranty details, maintenance records and preferences on your device in private application storage. No account is required. This preview has no advertising or analytics SDK and does not silently upload inventory or synchronise records.

Receipt text extraction runs locally using one of fourteen bundled OCR language models. Receipt imports retain their original file. Microphone permission is requested only when you choose voice input. The Android speech provider may transmit audio to its own service and may require internet. Possessory does not retain raw recordings; it displays the recognised transcript for you to review. The device’s text-to-speech provider handles spoken responses.

Document and image pickers grant access to the files you select. Camera capture is delegated to the installed camera application. Document previews may open another application only when you choose to view an attachment. Those applications have their own privacy policies.

Reports and backups are written to the location you select. That location might be a cloud provider if you select one in the system picker. Plain exports and ZIP backups are not encrypted by Possessory. Optional password-protected backups use AES-GCM and require a password of at least twelve characters. Passwords cannot be recovered. Optional app lock uses the device PIN, pattern or password confirmation screen; the credential is not stored by Possessory. App lock hides recent-app previews and screenshots. Database and attachment encryption beyond device-level protections is not claimed.

You can delete a belonging and its associated files, or delete all local data in Settings. Previously exported files remain wherever you saved them. Uninstalling removes private app data. Keep a verified backup if you want to retain it.

When Amazon billing is configured and you purchase or restore, the Amazon user ID and purchase receipt ID are sent over HTTPS to the Possessory verification service hosted on Cloudflare Workers. That service checks Amazon’s Receipt Verification Service and issues a signed entitlement. Household inventory, receipts you photograph, item serial numbers and documents are not included. The service uses the connection IP address and a hash of the purchase identifiers for short-lived rate limiting. It has no customer database, and application request logging is disabled. Cloudflare processes network metadata to operate and secure its service. A cached signed entitlement can remain usable offline for up to 90 days, or until an earlier verified cancellation takes effect; connecting refreshes its verification. Billing is disabled in unconfigured builds. Amazon separately processes purchases under its own policies.

Possessory has no app account or account-registration feature. Local data can be deleted in Settings without creating or deleting an account. If you email support, your email address and the information you choose to include are processed to respond to your request. Please contact banksmi@mail.com with privacy questions or requests concerning support correspondence.

The app is intended for household owners and renters and is not designed for children.

This notice is published in the project repository. Changes will be recorded here with an updated effective date. The Cloudflare billing disclosure is a release candidate requiring operator review before public purchases. Translated privacy notices in the 0.3.1 reviewer pack include the hosting disclosure as machine drafts and require independent human review before publication.
