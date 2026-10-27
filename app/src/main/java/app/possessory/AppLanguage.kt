package app.possessory

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

object AppLanguage {
    const val PREFERENCES = "interface-language"
    val targets =
        listOf(
            "en-GB" to "English (UK)",
            "en-US" to "English (US)",
            "en-AU" to "English (Australia)",
            "es-ES" to "Español (España)",
            "es-419" to "Español (Latinoamérica)",
            "fr-FR" to "Français (France)",
            "fr-CA" to "Français (Canada)",
            "de-DE" to "Deutsch",
            "it-IT" to "Italiano",
            "pt-BR" to "Português (Brasil)",
            "pt-PT" to "Português (Portugal)",
            "nl-NL" to "Nederlands",
            "pl-PL" to "Polski",
            "sv-SE" to "Svenska",
            "ja-JP" to "日本語",
            "hi-IN" to "हिन्दी",
            "ar" to "العربية",
            "tr-TR" to "Türkçe",
            "id-ID" to "Bahasa Indonesia",
        )

    fun selected(context: Context): String =
        if (android.os.Build.VERSION.SDK_INT >= 33)
            context
                .getSystemService(android.app.LocaleManager::class.java)
                ?.applicationLocales
                ?.toLanguageTags()
                ?.substringBefore(',') ?: ""
        else
            context
                .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getString("language", "") ?: ""

    fun apply(context: Context): Context {
        val tag = selected(context)
        val locale =
            if (tag.isBlank()) Resources.getSystem().configuration.locales[0]
            else Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val configuration =
            Configuration(context.resources.configuration).apply {
                setLocale(locale)
                setLayoutDirection(locale)
            }
        return context.createConfigurationContext(configuration)
    }

    fun select(context: Context, language: String) {
        require(language.isEmpty() || targets.any { it.first == language })
        context
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString("language", language)
            .commit()
        if (android.os.Build.VERSION.SDK_INT >= 33)
            context.getSystemService(android.app.LocaleManager::class.java)?.applicationLocales =
                android.os.LocaleList.forLanguageTags(language)
    }
}
