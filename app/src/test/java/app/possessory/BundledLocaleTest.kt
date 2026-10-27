package app.possessory

import android.content.res.Configuration
import android.view.View
import androidx.test.core.app.ApplicationProvider
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rGB", application = TestApp::class)
class BundledLocaleTest {
  @Test
  @Config(sdk = [35], qualifiers = "en-rGB")
  fun indonesianWorksWithModernLocaleCodes() {
    val app = ApplicationProvider.getApplicationContext<TestApp>()
    val localized =
      app.createConfigurationContext(
        Configuration(app.resources.configuration).apply {
          setLocale(Locale.forLanguageTag("id-ID"))
        }
      )
    assertEquals("Ringkasan", localized.getString(R.string.overview))
  }

  @Test
  fun everyPreviewLocaleHasBundledTextAndPreservedPlaceholders() {
    val app = ApplicationProvider.getApplicationContext<TestApp>()
    val english = app.getString(R.string.overview)
    for ((tag, _) in AppLanguage.targets) {
      val context =
        app.createConfigurationContext(
          Configuration(app.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(tag))
            setLayoutDirection(Locale.forLanguageTag(tag))
          }
        )
      assertTrue(context.getString(R.string.save).isNotBlank())
      if (!tag.startsWith("en"))
        assertNotEquals(
          "Locale fell back to English: $tag",
          english,
          context.getString(R.string.overview),
        )
      assertTrue(context.getString(R.string.welcome).contains("Possessory"))
      assertFalse(
        context.resources.getQuantityString(R.plurals.inventory_count, 3, 3).contains("%")
      )
    }
  }

  @Test
  fun languagePreferencePersistsAndArabicUsesRightToLeft() {
    val app = ApplicationProvider.getApplicationContext<TestApp>()
    try {
      AppLanguage.select(app, "es-ES")
      assertEquals("es-ES", AppLanguage.selected(app))
      assertEquals("Añadir artículo", AppLanguage.apply(app).getString(R.string.add_item))
      AppLanguage.select(app, "ar")
      val arabic = AppLanguage.apply(app)
      assertEquals(View.LAYOUT_DIRECTION_RTL, arabic.resources.configuration.layoutDirection)
      assertEquals("إضافة مقتنى", arabic.getString(R.string.add_item))
    } finally {
      AppLanguage.select(app, "")
      AppLanguage.apply(app)
    }
  }
}
