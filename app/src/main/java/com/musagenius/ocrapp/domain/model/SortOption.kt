package com.musagenius.ocrapp.domain.model

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
