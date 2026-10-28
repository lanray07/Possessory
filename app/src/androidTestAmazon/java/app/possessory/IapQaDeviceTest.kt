package app.possessory

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.possessory.services.*
import com.amazon.device.drm.LicensingService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IapQaDeviceTest {
    @Test
    fun appTesterPurchaseAndRestoreRequireCloudflareVerifiedLicense() = runBlocking {
        org.junit.Assume.assumeTrue(BuildConfig.APPLICATION_ID.endsWith(".iapqa"))
        assertTrue(BuildConfig.DEBUG)
        assertFalse(BuildConfig.REVIEW_MODE)
        assertEquals("sandbox", BuildConfig.BILLING_ENV)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as PossessoryApp
        ActivityScenario.launch(MainActivity::class.java).use {
            withTimeout(45000) {
                app.billing.state.first { state -> state.price != null && !state.busy }
            }
            assertEquals(
                "Real store calls are forbidden in this test",
                "SANDBOX",
                LicensingService.getAppstoreSDKMode(),
            )
            instrumentation.runOnMainSync { app.billing.purchase() }
            withTimeout(180000) {
                app.billing.state.first { state -> state.premium && !state.busy }
            }
            // A late product-data callback can set status=ready after a cached restore.
            // Require a fresh explicit server-verified restore before inspecting the licence.
            instrumentation.runOnMainSync { app.billing.restore() }
            withTimeout(45000) { app.billing.state.first { it.status == "verified" && !it.busy } }
            val saved = app.settings.data.first()[stringPreferencesKey("signed_license")]
            assertNotNull(saved)
            val claims =
                LicenseVerifier.verify(
                    Files.gson.fromJson(saved, SignedLicense::class.java),
                    BuildConfig.LICENSE_PUBLIC_KEY,
                    BuildConfig.AMAZON_SKU,
                    expectedEnvironment = "sandbox",
                )
            assertTrue(claims.active)
            instrumentation.runOnMainSync { app.billing.restore() }
            withTimeout(45000) {
                app.billing.state.first { state -> state.status == "verified" && !state.busy }
            }
            assertTrue(app.billing.state.value.premium)
            // Exercise the real signed receipt at its boundary without changing the device clock.
            val signed = Files.gson.fromJson(saved, SignedLicense::class.java)
            assertTrue(runCatching {
                LicenseVerifier.verify(signed, BuildConfig.LICENSE_PUBLIC_KEY,
                    BuildConfig.AMAZON_SKU, now = claims.validUntil,
                    expectedEnvironment = "sandbox")
            }.isFailure)
            assertTrue(runCatching {
                LicenseVerifier.verify(signed, BuildConfig.LICENSE_PUBLIC_KEY,
                    BuildConfig.AMAZON_SKU, expectedEnvironment = "production")
            }.isFailure)
            // Callback fixtures are deliberately distinct from end-to-end account switching.
            instrumentation.runOnMainSync {
                app.billing.onUserDataResponse(
                    com.amazon.device.iap.internal.model.UserDataResponseBuilder()
                        .setRequestId(com.amazon.device.iap.model.RequestId())
                        .setRequestStatus(com.amazon.device.iap.model.UserDataResponse.RequestStatus.SUCCESSFUL)
                        .setUserData(com.amazon.device.iap.internal.model.UserDataBuilder()
                            .setUserId("possessory-isolated-other-test-user").setMarketplace("GB").build())
                        .build())
            }
            assertFalse(app.billing.state.value.premium)
            withTimeout(5000) { app.settings.data.first { it[stringPreferencesKey("signed_license")] == null } }
            for (status in listOf(com.amazon.device.iap.model.PurchaseResponse.RequestStatus.FAILED,
                com.amazon.device.iap.model.PurchaseResponse.RequestStatus.INVALID_SKU)) {
                instrumentation.runOnMainSync {
                    app.billing.onPurchaseResponse(
                        com.amazon.device.iap.internal.model.PurchaseResponseBuilder()
                            .setRequestId(com.amazon.device.iap.model.RequestId())
                            .setRequestStatus(status).build())
                }
                assertFalse(app.billing.state.value.premium)
                assertFalse(app.billing.state.value.busy)
                assertEquals("retry", app.billing.state.value.status)
            }
            instrumentation.runOnMainSync { app.billing.restore() }
            withTimeout(45000) { app.billing.state.first { it.premium && !it.busy } }
            Unit
        }
    }
}
