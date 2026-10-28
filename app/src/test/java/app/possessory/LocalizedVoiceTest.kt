package app.possessory

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import app.possessory.domain.*
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class LocalizedVoiceTest {
    @Test fun displayedCommandsParseAcrossAllBundledLocalesWithoutChangingUserNames() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        for ((tag, _) in AppLanguage.targets) {
            val ctx = base.createConfigurationContext(Configuration(base.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(tag))
            })
            fun text(id: Int) = ctx.getString(id)
            val terms = VoiceTerms(text(R.string.voice_add_prefix), text(R.string.voice_find_prefix),
                text(R.string.voice_room_prefix), text(R.string.voice_count_prefix),
                text(R.string.voice_move_prefix), text(R.string.voice_to_separator),
                text(R.string.voice_summary_command), text(R.string.voice_warranty_command),
                text(R.string.voice_receipt_command), text(R.string.voice_read_command),
                text(R.string.voice_add_command))
            val name = "Willow Lamp Élan"
            val room = "Study North"
            assertEquals(tag, VoiceIntent.Find(name), VoiceParser.parse("${terms.find} $name", terms))
            assertEquals(tag, VoiceIntent.Room(room), VoiceParser.parse("${terms.room} $room", terms))
            assertEquals(tag, VoiceIntent.Count(room), VoiceParser.parse("${terms.count} $room", terms))
            assertEquals(tag, VoiceIntent.Move(name, room), VoiceParser.parse("${terms.move} $name ${terms.to} $room", terms))
            assertEquals(tag, VoiceIntent.AddNamed(name, room), VoiceParser.parse("${terms.add} $name ${terms.to} $room", terms))
            for ((phrase, intent) in listOf(terms.summary to VoiceIntent.Summary,
                terms.warranties to VoiceIntent.Warranties, terms.receipt to VoiceIntent.Receipt,
                terms.read to VoiceIntent.ReadReminders, terms.addCommand to VoiceIntent.Add)) {
                assertEquals(tag, intent, VoiceParser.parse(phrase, terms))
            }
        }
    }
}
