package app.possessory

import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.possessory.services.Files
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpokenFeedbackDeviceTest {
    @Test
    fun englishSynthesisCompletesAtOwnerSelectedVolume() {
        org.junit.Assume.assumeTrue(
            BuildConfig.REVIEW_MODE &&
                InstrumentationRegistry.getArguments().getString("audibleCheck") == "true"
        )
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext
        val audio = app.getSystemService(AudioManager::class.java)
        assertTrue(
            "Set an audible media volume on the tablet",
            audio.getStreamVolume(AudioManager.STREAM_MUSIC) > 0 &&
                !audio.isStreamMute(AudioManager.STREAM_MUSIC),
        )
        val ready = CountDownLatch(1)
        val finished = CountDownLatch(1)
        var initialization = TextToSpeech.ERROR
        var error = false
        lateinit var tts: TextToSpeech
        instrumentation.runOnMainSync {
            tts =
                TextToSpeech(app) {
                    initialization = it
                    ready.countDown()
                }
        }
        try {
            assertTrue(ready.await(20, TimeUnit.SECONDS))
            assertEquals(TextToSpeech.SUCCESS, initialization)
            assertTrue(tts.setLanguage(Locale.UK) >= TextToSpeech.LANG_AVAILABLE)
            tts.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {
                    override fun onStart(id: String) {}

                    override fun onDone(id: String) {
                        finished.countDown()
                    }

                    override fun onError(id: String) {
                        error = true
                        finished.countDown()
                    }
                }
            )
            assertEquals(
                TextToSpeech.SUCCESS,
                tts.speak(
                    "Possessory voice test complete.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "qa-voice",
                ),
            )
            assertTrue(finished.await(30, TimeUnit.SECONDS))
            assertFalse(error)
            File(app.getExternalFilesDir(null), "spoken-feedback-check.json")
                .writeText(
                    Files.gson.toJson(
                        mapOf(
                            "engine_completed" to true,
                            "locale" to "en-GB",
                            "media_volume" to audio.getStreamVolume(AudioManager.STREAM_MUSIC),
                            "human_heard_confirmation" to false,
                        )
                    )
                )
        } finally {
            tts.stop()
            tts.shutdown()
        }
    }
}
