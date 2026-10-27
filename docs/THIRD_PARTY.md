# Third-party components

AndroidX, Compose, Room, WorkManager and DataStore: Apache-2.0. Kotlin: Apache-2.0. Coil: Apache-2.0. Gson: Apache-2.0. Tesseract4Android wrapper: Apache-2.0, with separately licensed native dependencies including Tesseract, Leptonica, libjpeg and libpng. Review the complete notices before distribution.

The fourteen bundled OCR models (eng, spa, fra, deu, ita, por, nld, pol, swe, jpn, hin, ara, tur, ind) are from https://github.com/tesseract-ocr/tessdata_fast at commit 87416418657359cb625c412a48b6e1d6d41c29bd, fetched during development on 27 October 2026. SHA-256 provenance is recorded in localization/ocr-models.json. The upstream Apache license is bundled at assets/tessdata/LICENSE.txt. That repository is Apache-2.0. Tesseract4Android source and notices: https://github.com/adaptech-cz/Tesseract4Android. Preserve upstream license files in final distribution notices.

All project-owned geometric brand artwork in assets/brand and tools/brand_assets.py is authored for Possessory. No competitor graphics or third-party photographs are included. Material icons are supplied by the Apache-2.0 AndroidX Compose icon library.

Draft translations are generated during development using Argos model packages with CTranslate2, SentencePiece, Sacremoses and subword-nmt. These tools and model binaries are not included in the APK. Model sources, package versions and file hashes are recorded in localization/model-provenance.json. See https://github.com/argosopentech/argospm-index and https://github.com/argosopentech/argos-translate for upstream provenance; all translations still require language and release review.

Amazon Appstore SDK 3.0.9 is governed by Amazon SDK/service terms; it is not covered by the project's original-artwork statement. Review https://developer.amazon.com/docs/appstore-sdk/overview.html and the SDK distribution's notices before publication. Server dependencies FastAPI, Uvicorn and cryptography have their own upstream notices; retain those in deployment packaging.

Streaming AES-GCM backup I/O uses the explicit Bouncy Castle 1.86 provider because Fire Conscrypt buffers whole AEAD messages. It retains the existing AES-GCM backup format. Upstream release and license: https://www.bouncycastle.org/download/bouncy-castle-java/ and https://www.bouncycastle.org/about/license/. No Google Play Services dependency is introduced.
