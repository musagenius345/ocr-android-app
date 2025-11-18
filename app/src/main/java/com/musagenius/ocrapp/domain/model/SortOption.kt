package com.musagenius.ocrapp.domain.model

/**
 * Extension to get display name for SortBy enum
 */
fun SortBy.getDisplayName(): String {
    return when (this) {
        SortBy.DATE_DESC -> "Date (Newest First)"
        SortBy.DATE_ASC -> "Date (Oldest First)"
        SortBy.TITLE_ASC -> "Title (A-Z)"
        SortBy.TITLE_DESC -> "Title (Z-A)"
        SortBy.CONFIDENCE_DESC -> "Confidence (High to Low)"
        SortBy.CONFIDENCE_ASC -> "Confidence (Low to High)"
    }
}

/**
 * Get all sort options for display
 */
fun getAllSortOptions(): List<SortBy> {
    return listOf(
        SortBy.DATE_DESC,
        SortBy.DATE_ASC,
        SortBy.TITLE_ASC,
        SortBy.TITLE_DESC,
        SortBy.CONFIDENCE_DESC,
        SortBy.CONFIDENCE_ASC
    )
}
