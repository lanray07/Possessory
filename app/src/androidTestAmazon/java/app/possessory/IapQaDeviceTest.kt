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
            assertEquals("verified", app.billing.state.value.status)
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
        }
    }
}
