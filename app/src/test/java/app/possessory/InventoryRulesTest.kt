package app.possessory

import app.possessory.data.InventoryItem
import app.possessory.domain.*
import app.possessory.services.BackupCrypto
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class InventoryRulesTest {
    @Test
    fun formattedMoneyKeepsCurrencyPrecisionWhenLanguageChanges() {
        val locales =
            "en-GB en-US en-AU es-ES es-419 fr-FR fr-CA de-DE it-IT pt-BR pt-PT nl-NL pl-PL sv-SE ja-JP hi-IN ar tr-TR id-ID"
        for (tag in locales.split(" ")) {
            val locale = Locale.forLanguageTag(tag)
            for (code in listOf("GBP", "JPY", "KWD")) {
                val currency = java.util.Currency.getInstance(code)
                val expected = java.math.BigDecimal.valueOf(49999, currency.defaultFractionDigits)
                val displayed = InventoryRules.money(49999L, code, locale)
                val parser =
                    java.text.NumberFormat.getCurrencyInstance(locale) as java.text.DecimalFormat
                parser.currency = currency
                parser.isParseBigDecimal = true
                val parsed = parser.parse(displayed) as java.math.BigDecimal
                assertEquals(
                    "$tag/$code changed the recorded amount: $displayed",
                    0,
                    expected.compareTo(parsed),
                )
                assertEquals(
                    "$tag/$code used the language's currency precision",
                    currency.defaultFractionDigits,
                    parsed.scale(),
                )
            }
        }
    }

    @Test
    fun totalsCanExceedOneRecordsIntegerLimitWithoutOverflow() {
        val total =
            java.math.BigInteger.valueOf(Long.MAX_VALUE).multiply(java.math.BigInteger.valueOf(2))
        assertEquals("£184,467,440,737,095,516.14", InventoryRules.money(total, "GBP", Locale.UK))
    }

    @Test
    fun amountsUseCurrencyPrecisionWithoutFloatingPoint() {
        assertEquals(499L, InventoryRules.amount("4.99", "GBP"))
        assertEquals(499L, InventoryRules.amount("499", "JPY"))
        assertEquals(1001L, InventoryRules.amount("1.001", "KWD"))
        assertEquals("4.99", InventoryRules.editableAmount(499, "GBP"))
        assertNull(InventoryRules.amount("", "GBP"))
        listOf("-1", "NaN", "1.999", "9e999999").forEach { bad ->
            assertTrue(runCatching { InventoryRules.amount(bad, "GBP") }.isFailure)
        }
    }

    @Test
    fun datesAndWarrantyBoundaries() {
        val today = LocalDate.of(2026, 10, 27)
        fun item(end: String) = InventoryItem(name = "Laptop", roomId = "room", warrantyEnd = end)
        assertEquals("expired", InventoryRules.warranty(item("2026-10-26"), today))
        assertEquals("soon", InventoryRules.warranty(item("2026-10-27"), today))
        assertEquals("soon", InventoryRules.warranty(item("2026-11-26"), today))
        assertEquals("covered", InventoryRules.warranty(item("2026-11-27"), today))
        assertEquals("none", InventoryRules.warranty(item(""), today))
        assertFalse(InventoryRules.validDate("2026-02-29"))
        assertTrue(InventoryRules.validDate("2028-02-29"))
    }

    @Test
    fun searchIncludesHiddenRecordFields() {
        val i =
            InventoryItem(
                name = "Laptop",
                roomId = "office",
                serial = "XYZ123",
                notes = "Gift from Jane",
                retailer = "Local shop",
                tags = "work, portable",
            )
        listOf("xyz123", "jane", "local", "portable", "office").forEach {
            assertTrue(InventoryRules.matches(i, it, "Home Office"))
        }
        assertFalse(InventoryRules.matches(i, "missing", "Home Office"))
    }

    @Test
    fun exportsEscapeQuotesAndSpreadsheetFormulas() {
        assertEquals("\"a\"\"b\"", InventoryRules.csv("a\"b"))
        assertEquals("\"'=HYPERLINK(1)\"", InventoryRules.csv("=HYPERLINK(1)"))
        assertEquals("\"'  @SUM(1)\"", InventoryRules.csv("  @SUM(1)"))
    }

    @Test
    fun voiceUsesConstrainedIntentsAndNeverDeletes() {
        assertEquals(VoiceIntent.Find("laptop"), VoiceParser.parse("Find my laptop."))
        assertEquals(VoiceIntent.Room("kitchen"), VoiceParser.parse("Show items in the kitchen"))
        assertEquals(VoiceIntent.Unknown, VoiceParser.parse("delete all belongings"))
        assertEquals(
            VoiceIntent.Move("printer", "office"),
            VoiceParser.parse("move the printer to office"),
        )
    }

    @Test
    fun encryptedBackupRoundTripAndTamperDetection() {
        val password = "correct horse battery staple".toCharArray()
        val bytes = "Private household record".toByteArray()
        val encrypted = BackupCrypto.encrypt(bytes, password)
        assertArrayEquals(bytes, BackupCrypto.decrypt(encrypted, password))
        assertFalse(encrypted.contentEquals(BackupCrypto.encrypt(bytes, password)))
        assertTrue(
            runCatching { BackupCrypto.decrypt(encrypted, "wrong password here".toCharArray()) }
                .isFailure
        )
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        assertTrue(runCatching { BackupCrypto.decrypt(encrypted, password) }.isFailure)
    }

    @Test
    fun streamedBackupsRemainCompatibleAndRejectTamperingAndOversize() {
        val bytes = ByteArray(1024 * 1024) { (it % 251).toByte() }
        val password = "correct horse battery staple".toCharArray()
        val output = java.io.ByteArrayOutputStream()
        BackupCrypto.encrypting(output, password).use { it.write(bytes) }
        assertArrayEquals(bytes, BackupCrypto.decrypt(output.toByteArray(), password))
        val streamed = java.io.ByteArrayOutputStream()
        BackupCrypto.decrypt(
            java.io.ByteArrayInputStream(BackupCrypto.encrypt(bytes, password)),
            streamed,
            password,
        )
        assertArrayEquals(bytes, streamed.toByteArray())
        assertTrue(
            runCatching {
                    BackupCrypto.decrypt(
                        java.io.ByteArrayInputStream(output.toByteArray()),
                        java.io.ByteArrayOutputStream(),
                        password,
                        100,
                    )
                }
                .isFailure
        )
        val corrupt = output.toByteArray()
        corrupt[corrupt.lastIndex] = (corrupt.last().toInt() xor 1).toByte()
        assertTrue(
            runCatching {
                    BackupCrypto.decrypt(
                        java.io.ByteArrayInputStream(corrupt),
                        java.io.ByteArrayOutputStream(),
                        password,
                    )
                }
                .isFailure
        )
    }
}
