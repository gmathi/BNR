package com.bnr.app.domain.repository

import com.bnr.app.domain.model.NovelUpdateInfo
import kotlinx.coroutines.flow.Flow

interface NovelUpdateRepository {
    /** Flow of update info for all library novels. */
    fun getUpdatesForLibraryNovels(): Flow<List<NovelUpdateInfo>>

    /** Flow of update info for a single novel. */
    fun getUpdateInfo(novelId: String): Flow<NovelUpdateInfo?>

    /**
     * Called by the worker after fetching the current chapter count.
     * If [currentChapterCount] > [knownCount], sets newChapterCount = delta and records lastUpdatedAt.
     */
    suspend fun recordCheckResult(
        novelId: String,
        currentChapterCount: Int,
        checkedAt: Long
    )

    /** Called when user opens a novel detail — clears the new chapter badge. */
    suspend fun markAsSeen(novelId: String)

    /** Returns the last known chapter count for a novel, or null if never checked. */
    suspend fun getKnownChapterCount(novelId: String): Int?
}
