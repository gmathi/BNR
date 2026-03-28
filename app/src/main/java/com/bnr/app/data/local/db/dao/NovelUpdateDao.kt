package com.bnr.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.bnr.app.data.local.db.entity.NovelUpdateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NovelUpdateDao {
    @Query("SELECT * FROM novel_updates WHERE novelId = :novelId")
    fun getUpdateInfo(novelId: String): Flow<NovelUpdateEntity?>

    @Query("""
        SELECT nu.* FROM novel_updates nu
        INNER JOIN novels n ON n.id = nu.novelId
        WHERE n.inLibrary = 1
    """)
    fun getUpdatesForLibraryNovels(): Flow<List<NovelUpdateEntity>>

    @Upsert
    suspend fun upsert(entity: NovelUpdateEntity)

    @Query("UPDATE novel_updates SET newChapterCount = 0 WHERE novelId = :novelId")
    suspend fun clearNewChapters(novelId: String)

    @Query("SELECT knownChapterCount FROM novel_updates WHERE novelId = :novelId")
    suspend fun getKnownChapterCount(novelId: String): Int?
}
