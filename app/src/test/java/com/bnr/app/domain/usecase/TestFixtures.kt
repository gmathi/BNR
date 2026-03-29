package com.bnr.app.domain.usecase

import com.bnr.app.domain.model.Chapter
import com.bnr.app.domain.model.ChapterContent
import com.bnr.app.domain.model.DownloadState
import com.bnr.app.domain.model.Novel
import com.bnr.app.domain.model.NovelReaderSettings
import com.bnr.app.domain.model.NovelStatus

fun makeNovel(
    id: String = "src::slug",
    sourceId: String = "com.source.test",
    url: String = "/novel/slug",
    title: String = "Test Novel",
    author: String = "Test Author",
    coverUrl: String = "https://example.com/cover.jpg",
    description: String = "A test novel description.",
    genres: List<String> = listOf("Action", "Fantasy"),
    status: NovelStatus = NovelStatus.ONGOING,
    inLibrary: Boolean = false,
    lastReadChapterId: String? = null,
    addedToLibraryAt: Long? = null,
    chapterCount: Int = 10
): Novel = Novel(
    id = id,
    sourceId = sourceId,
    url = url,
    title = title,
    author = author,
    coverUrl = coverUrl,
    description = description,
    genres = genres,
    status = status,
    inLibrary = inLibrary,
    lastReadChapterId = lastReadChapterId,
    addedToLibraryAt = addedToLibraryAt,
    chapterCount = chapterCount
)

fun makeChapter(
    id: String = "novel::1",
    novelId: String = "src::slug",
    sourceId: String = "com.source.test",
    url: String = "/novel/slug/chapter-1",
    title: String = "Chapter 1",
    chapterNumber: Float = 1f,
    uploadedAt: Long? = 1_700_000_000_000L,
    isRead: Boolean = false,
    downloadState: DownloadState = DownloadState.NOT_DOWNLOADED,
    localFilePath: String? = null
): Chapter = Chapter(
    id = id,
    novelId = novelId,
    sourceId = sourceId,
    url = url,
    title = title,
    chapterNumber = chapterNumber,
    uploadedAt = uploadedAt,
    isRead = isRead,
    downloadState = downloadState,
    localFilePath = localFilePath
)

fun makeChapterContent(
    chapterId: String = "novel::1",
    title: String = "Chapter 1",
    paragraphs: List<String> = listOf("Paragraph one.", "Paragraph two."),
    previousChapterUrl: String? = null,
    nextChapterUrl: String? = "/novel/slug/chapter-2"
): ChapterContent = ChapterContent(
    chapterId = chapterId,
    title = title,
    paragraphs = paragraphs,
    previousChapterUrl = previousChapterUrl,
    nextChapterUrl = nextChapterUrl
)

fun makeReaderSettings(
    novelId: String = "src::slug",
    readerModeEnabled: Boolean = true,
    fontSize: Float = 16f,
    ttsVoiceId: String? = null,
    ttsEngineId: String = "android",
    readerColorPreset: String = "default",
    customBgColor: Long? = null,
    customTextColor: Long? = null
): NovelReaderSettings = NovelReaderSettings(
    novelId = novelId,
    readerModeEnabled = readerModeEnabled,
    fontSize = fontSize,
    ttsVoiceId = ttsVoiceId,
    ttsEngineId = ttsEngineId,
    readerColorPreset = readerColorPreset,
    customBgColor = customBgColor,
    customTextColor = customTextColor
)
