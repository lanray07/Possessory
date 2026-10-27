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
    fun amount(text: String, currency: String, locale: Locale = Locale.getDefault()): Long? {
        if (text.isBlank()) return null
        val decimals = Currency.getInstance(currency).defaultFractionDigits.coerceAtLeast(0)
        val parser = NumberFormat.getNumberInstance(locale) as java.text.DecimalFormat
        parser.isParseBigDecimal = true
        parser.isGroupingUsed = false
        val position = java.text.ParsePosition(0)
        val input = text.trim()
        val value =
            parser.parse(input, position) as? BigDecimal
                ?: throw IllegalArgumentException("invalid_amount")
        require(position.index == input.length) { "invalid_amount" }
        require(value.signum() >= 0) { "invalid_amount" }
        return value.movePointRight(decimals).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
    }

    fun editableAmount(
        value: Long?,
        currency: String,
        locale: Locale = Locale.getDefault(),
    ): String =
        value?.let {
            val digits = Currency.getInstance(currency).defaultFractionDigits.coerceAtLeast(0)
            NumberFormat.getNumberInstance(locale)
                .apply {
                    isGroupingUsed = false
                    minimumFractionDigits = digits
                    maximumFractionDigits = digits
                }
                .format(BigDecimal.valueOf(it, digits))
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
            if (value.trimStart().firstOrNull() in listOf('=', '+', '-', '@', '\t', '\r'))
                "'" + value
            else value
        return "\"${safe.replace("\"", "\"\"")}\""
    }
}

sealed interface VoiceIntent {
    data object Add : VoiceIntent

    data class Find(val query: String) : VoiceIntent

    data class Room(val name: String) : VoiceIntent

    data object Warranties : VoiceIntent

    data object WarrantiesThisMonth : VoiceIntent

    data object Summary : VoiceIntent

    data object Receipt : VoiceIntent

    data object Unknown : VoiceIntent

    data class AddNamed(val name: String, val room: String) : VoiceIntent

    data class Move(val name: String, val room: String) : VoiceIntent

    data class Count(val room: String) : VoiceIntent

    data object ReadReminders : VoiceIntent
}

data class VoiceTerms(
    val add: String = "add",
    val find: String = "find",
    val room: String = "show items in",
    val count: String = "how many items are in",
    val move: String = "move",
    val to: String = "to",
    val summary: String = "show my inventory summary",
    val warranties: String = "show warranties",
    val receipt: String = "add a receipt",
    val read: String = "read my warranty reminders",
    val addCommand: String = "add a new item",
)

object VoiceParser {
    fun parse(text: String, terms: VoiceTerms = VoiceTerms()): VoiceIntent {
        val s = text.trim().lowercase(Locale.ROOT).removeSuffix(".")
        val original = text.trim().removeSuffix(".")
        if (s.removeSuffix("?") == "which warranties expire this month")
            return VoiceIntent.WarrantiesThisMonth
        fun tail(prefix: String): String? =
            if (prefix.isNotBlank() && s.startsWith(prefix.lowercase(Locale.ROOT) + " "))
                original.drop(prefix.length).trim()
            else null
        fun roomName(value: String) = value.removePrefix("the ")
        if (s == terms.summary.lowercase(Locale.ROOT)) return VoiceIntent.Summary
        if (s == terms.warranties.lowercase(Locale.ROOT)) return VoiceIntent.Warranties
        if (s == terms.receipt.lowercase(Locale.ROOT)) return VoiceIntent.Receipt
        if (s == terms.read.lowercase(Locale.ROOT)) return VoiceIntent.ReadReminders
        if (s == terms.addCommand.lowercase(Locale.ROOT)) return VoiceIntent.Add
        tail(terms.count)?.let {
            return VoiceIntent.Count(roomName(it).removeSuffix("?"))
        }
        tail(terms.move)?.let { value ->
            val split =
                Regex("\\s+" + Regex.escape(terms.to) + "\\s+", RegexOption.IGNORE_CASE).find(value)
            if (split != null)
                return VoiceIntent.Move(
                    value
                        .take(split.range.first)
                        .trim()
                        .removePrefix(if (terms.move == "move") "the " else ""),
                    roomName(value.drop(split.range.last + 1).trim()),
                )
        }
        tail(terms.add)?.let { value ->
            if (value.lowercase(Locale.ROOT) !in listOf("a new item", "item", "a receipt")) {
                val split =
                    Regex("\\s+" + Regex.escape(terms.to) + "\\s+", RegexOption.IGNORE_CASE)
                        .find(value)
                if (split != null)
                    return VoiceIntent.AddNamed(
                        value.take(split.range.first).removePrefix("a ").trim(),
                        roomName(value.drop(split.range.last + 1).trim()),
                    )
            }
        }
        tail(terms.room)?.let {
            return VoiceIntent.Room(roomName(it))
        }
        tail(terms.find)?.let {
            return VoiceIntent.Find(it.removePrefix("my "))
        }
        if (terms != VoiceTerms()) return parse(original)
        return when {
            s == "add a new item" || s == "add item" -> VoiceIntent.Add
            s.startsWith("find my ") -> VoiceIntent.Find(s.removePrefix("find my "))
            s.startsWith("find ") -> VoiceIntent.Find(s.removePrefix("find "))
            s.startsWith("show items in ") ->
                VoiceIntent.Room(s.removePrefix("show items in ").removePrefix("the "))
            s in
                listOf(
                    "show warranties",
                    "which warranties expire this month?",
                    "which warranties expire this month",
                ) -> VoiceIntent.Warranties
            s == "add a receipt" -> VoiceIntent.Receipt
            s == "show my inventory summary" -> VoiceIntent.Summary
            else -> VoiceIntent.Unknown
        }
    }
}
