package com.musagenius.ocrapp.domain.model

/**
 * Filter options for scan history
 */
data class FilterOptions(
    val language: String? = null,
    val dateRange: DateRange? = null,
    val minConfidence: Float? = null,
    val favoritesOnly: Boolean = false
) {
    /**
     * Check if any filters are active
     */
    fun isActive(): Boolean {
        return language != null ||
               dateRange != null ||
               minConfidence != null ||
               favoritesOnly
    }

    /**
     * Get count of active filters
     */
    fun activeFilterCount(): Int {
        var count = 0
        if (language != null) count++
        if (dateRange != null) count++
        if (minConfidence != null) count++
        if (favoritesOnly) count++
        return count
    }

    /**
     * Clear all filters
     */
    fun clear(): FilterOptions {
        return FilterOptions()
    }
}

/**
 * Predefined date range options
 */
enum class DateRangeOption(val displayName: String) {
    ALL_TIME("All Time"),
    TODAY("Today"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    LAST_90_DAYS("Last 90 Days"),
    CUSTOM("Custom Range");

    fun toDateRange(): DateRange? {
        if (this == ALL_TIME || this == CUSTOM) return null

        val now = System.currentTimeMillis()
        val startTime = when (this) {
            TODAY -> {
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            LAST_7_DAYS -> {
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.DAY_OF_YEAR, -7)
                cal.timeInMillis
            }
            LAST_30_DAYS -> {
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.DAY_OF_YEAR, -30)
                cal.timeInMillis
            }
            LAST_90_DAYS -> {
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.DAY_OF_YEAR, -90)
                cal.timeInMillis
            }
            else -> return null
        }

        return DateRange(startTime, now)
    }

    companion object {
        fun fromDisplayName(name: String): DateRangeOption? {
            return entries.find { it.displayName == name }
        }

        /**
         * Try to match a DateRange to a predefined DateRangeOption
         * Returns the matching option, CUSTOM if there's a range but no match, or ALL_TIME if null
         */
        fun fromDateRange(dateRange: DateRange?): DateRangeOption {
            if (dateRange == null) return ALL_TIME

            val now = System.currentTimeMillis()
            val dayInMillis = 24 * 60 * 60 * 1000L

            // Check if end date is approximately "now" (within 1 hour)
            if (kotlin.math.abs(dateRange.endDate - now) > 60 * 60 * 1000) {
                return CUSTOM
            }

            // Calculate days difference
            val daysDiff = ((now - dateRange.startDate) / dayInMillis).toInt()

            return when {
                // TODAY: start date should be today at midnight
                daysDiff == 0 -> {
                    val cal = java.util.Calendar.getInstance()
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.set(java.util.Calendar.SECOND, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    val todayStart = cal.timeInMillis
                    if (kotlin.math.abs(dateRange.startDate - todayStart) < dayInMillis) TODAY else CUSTOM
                }
                // LAST_7_DAYS: approximately 7 days ago
                daysDiff in 6..8 -> LAST_7_DAYS
                // LAST_30_DAYS: approximately 30 days ago
                daysDiff in 28..32 -> LAST_30_DAYS
                // LAST_90_DAYS: approximately 90 days ago
                daysDiff in 88..92 -> LAST_90_DAYS
                else -> CUSTOM
            }
        }
    }
}
