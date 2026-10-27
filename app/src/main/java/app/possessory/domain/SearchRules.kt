package app.possessory.domain

import app.possessory.data.InventoryItem
import java.time.LocalDate
import java.util.Locale

data class SearchCriteria(
    val condition: String = "",
    val currency: String = "",
    val minimum: String = "",
    val maximum: String = "",
    val purchasedAfter: String = "",
    val purchasedBefore: String = "",
    val numberLocale: String? = Locale.getDefault().toLanguageTag(),
) {
    fun amount(value: String): Long? =
        InventoryRules.amount(value, currency, Locale.forLanguageTag(numberLocale ?: "en-GB"))

    fun validate() {
        require(
            InventoryRules.validDate(purchasedAfter) && InventoryRules.validDate(purchasedBefore)
        )
        require(
            purchasedAfter.isBlank() ||
                purchasedBefore.isBlank() ||
                purchasedAfter <= purchasedBefore
        )
        require((minimum.isBlank() && maximum.isBlank()) || currency.isNotBlank())
        val min = if (minimum.isBlank()) null else amount(minimum)
        val max = if (maximum.isBlank()) null else amount(maximum)
        require(min == null || max == null || min <= max)
    }

    fun matches(item: InventoryItem): Boolean {
        if (condition.isNotBlank() && condition != item.condition) return false
        if (currency.isNotBlank() && currency != item.currency) return false
        if (
            purchasedAfter.isNotBlank() &&
                (item.purchaseDate.isBlank() || item.purchaseDate < purchasedAfter)
        )
            return false
        if (
            purchasedBefore.isNotBlank() &&
                (item.purchaseDate.isBlank() || item.purchaseDate > purchasedBefore)
        )
            return false
        val min = if (minimum.isBlank()) null else amount(minimum)
        val max = if (maximum.isBlank()) null else amount(maximum)
        if (min != null && (item.priceMinor == null || item.priceMinor < min)) return false
        if (max != null && (item.priceMinor == null || item.priceMinor > max)) return false
        return true
    }
}

data class SavedSearch(
    val name: String,
    val query: String,
    val room: String,
    val category: String,
    val filter: String,
    val sort: String,
    val criteria: SearchCriteria,
)

object ReminderRules {
    fun offsets(value: String): List<Int> =
        if (value.isBlank()) emptyList()
        else
            value
                .split(',')
                .map { it.trim().toInt().also { n -> require(n in 0..3650) } }
                .distinct()
                .sortedDescending()

    fun dates(value: String): List<LocalDate> =
        if (value.isBlank()) emptyList()
        else value.split(',').map { LocalDate.parse(it.trim()) }.distinct().sorted()

    fun due(item: InventoryItem, today: LocalDate): Boolean {
        if (item.status != "Active") return false
        if (today in dates(item.reminderDates)) return true
        val expiry = runCatching { LocalDate.parse(item.warrantyEnd) }.getOrNull() ?: return false
        val days = offsets(item.reminderOffsets).ifEmpty { listOf(item.reminderDays) }
        return days.any { expiry.minusDays(it.toLong()) == today }
    }

    /** Catch up at most seven days after background scheduling delays. */
    fun dueSince(item: InventoryItem, lastCheck: LocalDate?, today: LocalDate): Boolean {
        val start =
            (lastCheck ?: today.minusDays(1))
                .coerceAtLeast(today.minusDays(7))
                .coerceAtMost(today.minusDays(1))
        return (1..java.time.temporal.ChronoUnit.DAYS.between(start, today).toInt()).any {
            due(item, start.plusDays(it.toLong()))
        }
    }
}
