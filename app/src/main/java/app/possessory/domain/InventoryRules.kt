package app.possessory.domain

import app.possessory.data.InventoryItem
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Currency
import java.util.Locale

object InventoryRules {
  fun amount(text: String, currency: String): Long? {
    if (text.isBlank()) return null
    val decimals = Currency.getInstance(currency).defaultFractionDigits.coerceAtLeast(0)
    val value = text.trim().toBigDecimalOrNull() ?: throw IllegalArgumentException("invalid_amount")
    require(value.signum() >= 0) { "invalid_amount" }
    return value.movePointRight(decimals).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
  }

  fun editableAmount(value: Long?, currency: String): String =
    value?.let {
      BigDecimal.valueOf(it, Currency.getInstance(currency).defaultFractionDigits.coerceAtLeast(0))
        .toPlainString()
    } ?: ""

  fun money(value: Long, code: String, locale: Locale = Locale.getDefault()): String {
    return money(BigInteger.valueOf(value), code, locale)
  }

  fun money(value: BigInteger, code: String, locale: Locale = Locale.getDefault()): String {
    val currency = Currency.getInstance(code)
    return NumberFormat.getCurrencyInstance(locale)
      .apply { this.currency = currency }
      .format(BigDecimal(value, currency.defaultFractionDigits.coerceAtLeast(0)))
  }

  fun validDate(value: String): Boolean =
    value.isBlank() || runCatching { LocalDate.parse(value) }.isSuccess

  fun warranty(item: InventoryItem, today: LocalDate = LocalDate.now()): String {
    val end = runCatching { LocalDate.parse(item.warrantyEnd) }.getOrNull() ?: return "none"
    return when {
      end < today -> "expired"
      end <= today.plusDays(item.reminderDays.toLong()) -> "soon"
      else -> "covered"
    }
  }

  fun matches(item: InventoryItem, query: String, room: String): Boolean =
    listOf(
        item.name,
        item.description,
        item.brand,
        item.model,
        item.serial,
        item.tags,
        item.notes,
        item.retailer,
        item.category,
        room,
      )
      .joinToString(" ")
      .contains(query.trim(), true)

  fun csv(value: String): String {
    val safe =
      if (value.trimStart().firstOrNull() in listOf('=', '+', '-', '@', '\t', '\r')) "'" + value
      else value
    return "\"${safe.replace("\"", "\"\"")}\""
  }
}

sealed interface VoiceIntent {
  data object Add : VoiceIntent

  data class Find(val query: String) : VoiceIntent

  data class Room(val name: String) : VoiceIntent

  data object Warranties : VoiceIntent

  data object Summary : VoiceIntent

  data object Receipt : VoiceIntent

  data object Unknown : VoiceIntent
}

object VoiceParser {
  fun parse(text: String): VoiceIntent {
    val s = text.trim().lowercase(Locale.ROOT).removeSuffix(".")
    return when {
      s == "add a new item" || s == "add item" -> VoiceIntent.Add
      s.startsWith("find my ") -> VoiceIntent.Find(s.removePrefix("find my "))
      s.startsWith("find ") -> VoiceIntent.Find(s.removePrefix("find "))
      s.startsWith("show items in ") ->
        VoiceIntent.Room(s.removePrefix("show items in ").removePrefix("the "))
      s.contains("warrant") -> VoiceIntent.Warranties
      s == "add a receipt" -> VoiceIntent.Receipt
      s.contains("summary") -> VoiceIntent.Summary
      else -> VoiceIntent.Unknown
    }
  }
}
