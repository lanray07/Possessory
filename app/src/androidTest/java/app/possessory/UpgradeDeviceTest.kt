package app.possessory

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Read-only verification after updating the previously installed, signed 0.2 demo build. */
@RunWith(AndroidJUnit4::class)
class UpgradeDeviceTest {
    @Test
    fun signedProductionUpdateRetainsExistingDemoRecords() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        org.junit.Assume.assumeFalse(BuildConfig.REVIEW_MODE)
        org.junit.Assume.assumeTrue(
            InstrumentationRegistry.getArguments().getString("existingDemoUpgrade") == "true"
        )
        assertEquals("app.possessory", context.packageName)
        assertFalse(BuildConfig.REVIEW_MODE)
        assertEquals(5, BuildConfig.VERSION_CODE)
        val app = context.applicationContext as PossessoryApp
        val dao = app.db.inventory()
        assertTrue(dao.households().first().any { it.name == "Willow House" })
        assertTrue(dao.rooms().first().size >= 3)
        assertTrue(dao.items().first().size >= 6)
        assertTrue(dao.maintenance().first().isNotEmpty())
        assertEquals(2, app.db.openHelper.readableDatabase.version)
    }
}
