package com.bnr.app.data.local.db

import com.bnr.app.data.local.db.entity.ChapterEntity
import com.bnr.app.data.local.db.entity.NovelEntity
import com.bnr.app.data.local.db.entity.NovelReaderSettingsEntity
import com.bnr.app.domain.model.Chapter
import com.bnr.app.domain.model.DownloadState
import com.bnr.app.domain.model.Novel
import com.bnr.app.domain.model.NovelReaderSettings
import com.bnr.app.domain.model.NovelStatus

// ─── Novel ───────────────────────────────────────────────────────────────────

fun NovelEntity.toDomain(): Novel = Novel(
    id = id,
    sourceId = sourceId,
    url = url,
    title = title,
    author = author,
    coverUrl = coverUrl,
    description = description,
    genres = genres.parseJsonStringList(),
    status = NovelStatus.valueOf(status),
    inLibrary = inLibrary,
    lastReadChapterId = lastReadChapterId,
    addedToLibraryAt = addedToLibraryAt,
    chapterCount = chapterCount
)

fun Novel.toEntity(): NovelEntity = NovelEntity(
    id = id,
    sourceId = sourceId,
    url = url,
    title = title,
    author = author,
    coverUrl = coverUrl,
    description = description,
    genres = genres.toJsonString(),
    status = status.name,
    chapterCount = chapterCount,
    inLibrary = inLibrary,
    lastReadChapterId = lastReadChapterId,
    addedToLibraryAt = addedToLibraryAt
)

// ─── Chapter ─────────────────────────────────────────────────────────────────

fun ChapterEntity.toDomain(): Chapter = Chapter(
    id = id,
    novelId = novelId,
    sourceId = sourceId,
    url = url,
    title = title,
    chapterNumber = chapterNumber,
    uploadedAt = uploadedAt,
    isRead = isRead,
    downloadState = DownloadState.valueOf(downloadState),
    localFilePath = localFilePath
)

fun Chapter.toEntity(): ChapterEntity = ChapterEntity(
    id = id,
    novelId = novelId,
    sourceId = sourceId,
    url = url,
    title = title,
    chapterNumber = chapterNumber,
    uploadedAt = uploadedAt,
    isRead = isRead,
    downloadState = downloadState.name,
    localFilePath = localFilePath
)

// ─── NovelReaderSettings ─────────────────────────────────────────────────────

fun NovelReaderSettingsEntity.toDomain(): NovelReaderSettings = NovelReaderSettings(
    novelId = novelId,
    readerModeEnabled = readerModeEnabled,
    fontSize = fontSize,
    ttsVoiceId = ttsVoiceId,
    ttsEngineId = ttsEngineId
)

fun NovelReaderSettings.toEntity(): NovelReaderSettingsEntity = NovelReaderSettingsEntity(
    novelId = novelId,
    readerModeEnabled = readerModeEnabled,
    fontSize = fontSize,
    ttsVoiceId = ttsVoiceId,
    ttsEngineId = ttsEngineId
)

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun List<String>.toJsonString(): String =
    "[${joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }}]"

private fun String.parseJsonStringList(): List<String> {
    if (isBlank() || this == "[]") return emptyList()
    return removePrefix("[").removeSuffix("]")
        .split(",")
        .map { it.trim().removeSurrounding("\"").replace("\\\"", "\"") }
        .filter { it.isNotEmpty() }
}
