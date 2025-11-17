package com.musagenius.ocrapp.domain.model

/**
 * Filter options for searching scans
 */
data class SearchFilter(
    val query: String? = null,
    val language: String? = null,
    val dateRange: DateRange? = null,
    val tags: List<String> = emptyList(),
    val favoritesOnly: Boolean = false,
    val minConfidenceScore: Float? = null,
    val sortBy: SortBy = SortBy.DATE_DESC
)

/**
 * Date range filter
 */
data class DateRange(
    val startDate: Long,
    val endDate: Long
)

/**
 * Sorting options
 */
enum class SortBy {
    DATE_ASC,
    DATE_DESC,
    CONFIDENCE_ASC,
    CONFIDENCE_DESC,
    TITLE_ASC,
    TITLE_DESC
}
