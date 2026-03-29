package com.bnr.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.bnr.app.data.local.db.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {

    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY chapterNumber ASC")
    fun getChaptersByNovelId(novelId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapterById(id: String): ChapterEntity?

    @Upsert
    suspend fun upsertChapters(chapters: List<ChapterEntity>)

    @Upsert
    suspend fun upsertChapter(chapter: ChapterEntity)

    @Query("UPDATE chapters SET isRead = 1 WHERE id = :chapterId")
    suspend fun markRead(chapterId: String)

    @Query("UPDATE chapters SET downloadState = :state, localFilePath = :path WHERE id = :chapterId")
    suspend fun updateDownloadState(chapterId: String, state: String, path: String?)

    @Query("SELECT * FROM chapters WHERE novelId = :novelId AND isRead = 0 ORDER BY chapterNumber ASC LIMIT 1")
    suspend fun getFirstUnreadChapter(novelId: String): ChapterEntity?
}
