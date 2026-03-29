package com.bnr.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.bnr.app.data.local.db.entity.NovelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NovelDao {

    @Query("SELECT * FROM novels WHERE inLibrary = 1 ORDER BY addedToLibraryAt DESC")
    fun getLibraryNovels(): Flow<List<NovelEntity>>

    @Query("SELECT * FROM novels WHERE id = :id")
    suspend fun getNovelById(id: String): NovelEntity?

    @Upsert
    suspend fun upsertNovel(novel: NovelEntity)

    @Query("UPDATE novels SET inLibrary = 1, addedToLibraryAt = :timestamp WHERE id = :novelId")
    suspend fun addToLibrary(novelId: String, timestamp: Long)

    @Query("UPDATE novels SET inLibrary = 0, addedToLibraryAt = NULL WHERE id = :novelId")
    suspend fun removeFromLibrary(novelId: String)

    @Query("UPDATE novels SET lastReadChapterId = :chapterId WHERE id = :novelId")
    suspend fun updateLastReadChapter(novelId: String, chapterId: String)

    @Query("UPDATE novels SET chapterCount = :count WHERE id = :novelId")
    suspend fun updateChapterCount(novelId: String, count: Int)
}
