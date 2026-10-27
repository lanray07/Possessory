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
    assertEquals(VoiceIntent.Unknown, VoiceParser.parse("move the printer to office"))
  }

  @Test
  fun encryptedBackupRoundTripAndTamperDetection() {
    val password = "correct horse battery staple".toCharArray()
    val bytes = "Private household record".toByteArray()
    val encrypted = BackupCrypto.encrypt(bytes, password)
    assertArrayEquals(bytes, BackupCrypto.decrypt(encrypted, password))
    assertFalse(encrypted.contentEquals(BackupCrypto.encrypt(bytes, password)))
    assertTrue(
      runCatching { BackupCrypto.decrypt(encrypted, "wrong password here".toCharArray()) }.isFailure
    )
    encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
    assertTrue(runCatching { BackupCrypto.decrypt(encrypted, password) }.isFailure)
  }
}
