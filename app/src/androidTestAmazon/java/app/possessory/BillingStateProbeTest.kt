package app.possessory

import androidx.datastore.preferences.core.*
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.possessory.services.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Opt-in probes for externally configured App Tester/offline scenarios. No purchase call. */
@RunWith(AndroidJUnit4::class)
class BillingStateProbeTest {
    @Test fun checkConfiguredSandboxState() = runBlocking {
        Assume.assumeTrue(BuildConfig.APPLICATION_ID.endsWith(".iapqa") && BuildConfig.DEBUG)
        assertEquals("sandbox", BuildConfig.BILLING_ENV)
        val args = InstrumentationRegistry.getArguments()
        val mode = args.getString("billingProbe") ?: return@runBlocking
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as PossessoryApp
        val clock = longPreferencesKey("license_last_seen")
        val originalClock = app.settings.data.first()[clock]
        if (mode == "account-without-purchase") {
            assertNotNull("Start with the previous test user's cached entitlement",
                app.settings.data.first()[stringPreferencesKey("signed_license")])
        }
        if (mode == "expired-offline") {
            val saved = app.settings.data.first()[stringPreferencesKey("signed_license")]
            assertNotNull(saved)
            val claims = LicenseVerifier.verify(Files.gson.fromJson(saved, SignedLicense::class.java),
                BuildConfig.LICENSE_PUBLIC_KEY, BuildConfig.AMAZON_SKU, expectedEnvironment = "sandbox")
            // Advance only this disposable package's persisted monotonic floor, never device time.
            app.settings.edit { it[clock] = claims.validUntil + 1 }
        }
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                when (mode) {
                    "account-without-purchase" -> {
                        withTimeout(60000) { app.billing.state.first { it.status == "checked" && !it.busy } }
                        assertFalse(app.billing.state.value.premium)
                        withTimeout(10000) { app.settings.data.first { it[stringPreferencesKey("signed_license")] == null } }
                    }
                    "restored" -> {
                        withTimeout(60000) { app.billing.state.first { it.premium && !it.busy } }
                        InstrumentationRegistry.getInstrumentation().runOnMainSync { app.billing.restore() }
                        withTimeout(60000) { app.billing.state.first { it.status == "verified" && !it.busy } }
                        assertTrue(app.billing.state.value.premium)
                    }
                    "revoked" -> {
                        withTimeout(60000) { app.billing.state.first { it.status == "revoked" && !it.busy } }
                        assertFalse(app.billing.state.value.premium)
                        withTimeout(10000) { app.settings.data.first { it[stringPreferencesKey("signed_license")] == null } }
                    }
                    "cached-offline" -> {
                        withTimeout(10000) { app.billing.state.first { it.premium } }
                        delay(3000)
                        assertTrue(app.billing.state.value.premium)
                    }
                    "expired-offline" -> {
                        delay(5000)
                        assertFalse(app.billing.state.value.premium)
                    }
                    else -> error("Unknown billing probe")
                }
            }
        } finally {
            if (mode == "expired-offline") app.settings.edit {
                if (originalClock == null) it.remove(clock) else it[clock] = originalClock
            }
        }
        Unit
    }
}
