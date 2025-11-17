package com.musagenius.ocrapp.data.mapper

import com.musagenius.ocrapp.data.local.entity.ScanEntity
import com.musagenius.ocrapp.domain.model.ScanResult

/**
 * Mapper functions to convert between data and domain models
 */

/**
 * Convert ScanEntity (data layer) to ScanResult (domain layer)
 */
fun ScanEntity.toDomain(): ScanResult {
    return ScanResult(
        id = id,
        timestamp = timestamp,
        imageUri = imageUri,
        extractedText = extractedText,
        language = language,
        confidenceScore = confidenceScore,
        title = title,
        tags = tags.split(",").filter { it.isNotBlank() },
        notes = notes,
        isFavorite = isFavorite,
        modifiedTimestamp = modifiedTimestamp
    )
}

/**
 * Convert ScanResult (domain layer) to ScanEntity (data layer)
 */
fun ScanResult.toEntity(): ScanEntity {
    return ScanEntity(
        id = id,
        timestamp = timestamp,
        imageUri = imageUri,
        extractedText = extractedText,
        language = language,
        confidenceScore = confidenceScore,
        title = title,
        tags = tags.joinToString(","),
        notes = notes,
        isFavorite = isFavorite,
        modifiedTimestamp = modifiedTimestamp
    )
}

/**
 * Convert list of ScanEntity to list of ScanResult
 */
fun List<ScanEntity>.toDomain(): List<ScanResult> {
    return map { it.toDomain() }
}

/**
 * Convert list of ScanResult to list of ScanEntity
 */
fun List<ScanResult>.toEntity(): List<ScanEntity> {
    return map { it.toEntity() }
}
