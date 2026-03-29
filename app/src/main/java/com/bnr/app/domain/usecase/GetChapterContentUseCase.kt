package com.bnr.app.domain.usecase

import com.bnr.app.data.local.saf.SafStorageManager
import com.bnr.app.domain.model.Chapter
import com.bnr.app.domain.model.ChapterContent
import com.bnr.app.domain.model.DownloadState
import com.bnr.app.domain.repository.ChapterRepository
import com.bnr.app.source.SourceResult
import javax.inject.Inject

/**
 * Offline-first: reads from local file if downloaded, falls back to network.
 */
class GetChapterContentUseCase @Inject constructor(
    private val chapterRepository: ChapterRepository,
    private val safStorageManager: SafStorageManager
) {
    suspend operator fun invoke(chapter: Chapter): SourceResult<ChapterContent> {
        // Try local file first
        if (chapter.downloadState == DownloadState.DOWNLOADED && chapter.localFilePath != null) {
            val local = safStorageManager.readChapterContent(chapter.localFilePath)
            if (local != null) return SourceResult.Success(local)
        }
        // Fall back to network
        return chapterRepository.getChapterContent(chapter.sourceId, chapter.url, chapter.id)
    }
}
