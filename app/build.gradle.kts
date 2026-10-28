import java.io.File
import java.security.MessageDigest
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}
android {
    namespace = "app.possessory"
    compileSdk = 36
    flavorDimensions += "store"
    productFlavors {
        create("amazon") { dimension = "store" }
        create("standalone") { dimension = "store" }
    }
    defaultConfig {
        applicationId = "app.possessory"
        minSdk = 26
        targetSdk = 36
        versionCode = 9
        versionName = "0.3.6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "AMAZON_SKU", "\"" + providers.environmentVariable("POSSESSORY_AMAZON_SKU").getOrElse("possessory.premium.lifetime").replace("\"", "\\\"") + "\"")
        buildConfigField("String", "BILLING_ENV", "\"production\"")
        buildConfigField("boolean", "DRAFT_TRANSLATIONS", "false")
        buildConfigField("boolean", "REVIEW_MODE", "false")
        buildConfigField("String", "VERIFIER_URL", "\"" + providers.environmentVariable("POSSESSORY_VERIFIER_URL").getOrElse("").replace("\"", "\\\"") + "\"")
        buildConfigField("String", "LICENSE_PUBLIC_KEY", "\"" + providers.environmentVariable("POSSESSORY_LICENSE_PUBLIC_KEY").getOrElse("").replace("\"", "\\\"") + "\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    // Desktop OSGi metadata is duplicated by Bouncy Castle and JSpecify; Android does not use it.
    packaging.resources.excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
    testBuildType = providers.gradleProperty("deviceTestBuildType").getOrElse("debug")
    sourceSets.getByName("debug").res.srcDir("src/preview/res")
    sourceSets.getByName("main").res.srcDir("src/localized/res")
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    // Amazon SDK 3.0.9 emits stack-map warnings in R8; use D8 packaging until physical-device QA.
    buildTypes {
        debug {
            buildConfigField("boolean", "DRAFT_TRANSLATIONS", "true")
            buildConfigField("String", "BILLING_ENV", "\"sandbox\"")
            buildConfigField("String", "VERIFIER_URL", "\"" + providers.environmentVariable("POSSESSORY_SANDBOX_VERIFIER_URL").getOrElse("").replace("\"", "\\\"") + "\"")
            buildConfigField("String", "LICENSE_PUBLIC_KEY", "\"" + providers.environmentVariable("POSSESSORY_SANDBOX_LICENSE_PUBLIC_KEY").getOrElse("").replace("\"", "\\\"") + "\"")
        }
        release { isMinifyEnabled = false; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") }
        create("review") {
            initWith(getByName("release"))
            applicationIdSuffix = ".review"
            matchingFallbacks += listOf("release")
            buildConfigField("boolean", "DRAFT_TRANSLATIONS", "true")
            buildConfigField("boolean", "REVIEW_MODE", "true")
            buildConfigField("String", "VERIFIER_URL", "\"\"")
            buildConfigField("String", "LICENSE_PUBLIC_KEY", "\"\"")
        }
        create("iapqa") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".iapqa"
            matchingFallbacks += listOf("debug")
            buildConfigField("boolean", "REVIEW_MODE", "false")
            buildConfigField("String", "BILLING_ENV", "\"sandbox\"")
        }
    }
    sourceSets.getByName("review").res.srcDir("src/preview/res")
    sourceSets.getByName("review").assets.srcDir("src/debug/assets")
    sourceSets.getByName("iapqa").res.srcDir("src/preview/res")
    sourceSets.getByName("iapqa").assets.srcDir("src/debug/assets")
    testOptions { unitTests.isReturnDefaultValues = true; unitTests.isIncludeAndroidResources = true }
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
val verifyBundledTranslations by tasks.registering {
    doLast {
        val source = file("src/main/res/values/strings.xml")
        fun digest(file: File) = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
        for (kind in listOf("preview", "localized")) {
            val manifest = file("src/$kind/locales.properties")
            if (!manifest.exists()) continue
            val metadata = Properties().apply { manifest.inputStream().use { load(it) } }
            check(metadata.getProperty("source.sha256") == digest(source)) { "Stale bundled translations: run tools/localize.py after updating translations." }
            for (locale in metadata.getProperty("locales").split(',')) {
                check(locale.matches(Regex("[A-Za-z0-9-]+")))
                val folder = metadata.getProperty("folder.$locale", "values-b+${locale.replace('-', '+')}")
                check(folder.matches(Regex("values-[A-Za-z0-9+_-]+")))
                val strings = file("src/$kind/res/$folder/strings.xml")
                check(strings.exists() && digest(strings) == metadata.getProperty("sha256.$locale")) { "Bundled translation checksum mismatch: $locale" }
                val support = file("src/${if (kind == "preview") "debug" else "main"}/assets/support/$locale.json")
                check(support.exists() && digest(support) == metadata.getProperty("support.sha256.$locale")) { "Bundled support checksum mismatch: $locale" }
                val alias = metadata.getProperty("alias.$locale")
                if (alias != null) {
                    check(alias.matches(Regex("values-[A-Za-z0-9+_-]+")))
                    check(digest(file("src/$kind/res/$alias/strings.xml")) == metadata.getProperty("sha256.$locale")) { "Bundled locale alias mismatch: $locale" }
                }
            }
        }
    }
}
tasks.named("preBuild").configure { dependsOn(verifyBundledTranslations) }
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.05.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("org.bouncycastle:bcprov-jdk18on:1.86")
    "amazonImplementation"("com.amazon.device:amazon-appstore-sdk:3.0.9")
    implementation("cz.adaptech.tesseract4android:tesseract4android:4.9.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation(platform("androidx.compose:compose-bom:2025.05.01"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.05.01"))
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
