# Possessory 0.3.7 (code 12)

Amazon confirmed public submission on 3 November 2026. Its console shows Submitted. Amazon estimated availability before 17:00 London time on 8 November, subject to review.

The submitted APK is artifacts/Possessory-amazon-0.3.7-localized-v12-signed.apk. Its SHA-256 is f20c9eb0f3c19501901437b6a0b7790a30d8da5e5c3554acbfc7d2ebe3146bb8. APK v2 and v3 signatures verified. Code 12 packages the Possessory Amazon SDK authentication public key missing from code 11. All 32 unit/render tests passed. Release lint reported zero errors, 68 warnings and nine hints.

The Amazon Appstore installed code 12 on the test Fire. The app displayed the one-time GBP 4.99 price and Amazon confirmed the test purchase. Possessory showed Premium entitlement verified and enabled the additional household feature. After restarting the app, Restore Purchases completed the SDK purchase-update and fulfillment callbacks and the app again showed the verified entitlement. The checkout appeared after returning to the app from the Fire home screen. The production entitlement was promoted and Amazon showed Approved.

All 19 localization catalogues are bundled. The previous device QA included 175 layout captures. The 152 marketing screenshots use eight Fire captures per locale. Fourteen storefront descriptions and ten applicable Amazon galleries were completed. Independent language review was waived. The archive is artifacts/Possessory-0.3.7-152-localized-marketing-APPROVED.zip.

Submission confirmation and live test screenshots are stored locally under artifacts/amazon-public-submitted-code12.jpg, artifacts/amazon-premium-purchase-code12.png and artifacts/amazon-premium-restored-code12.png.
