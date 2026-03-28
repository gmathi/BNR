package com.bnr.app.domain.usecase

import com.bnr.app.domain.model.Chapter
import com.bnr.app.domain.repository.DownloadRepository
import javax.inject.Inject

class DownloadChapterUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(chapter: Chapter) =
        repository.enqueueDownload(
            chapterId  = chapter.id,
            chapterUrl = chapter.url,
            novelId    = chapter.novelId,
            sourceId   = chapter.sourceId
        )
}
