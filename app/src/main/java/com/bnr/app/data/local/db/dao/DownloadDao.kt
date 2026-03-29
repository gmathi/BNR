package com.bnr.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.bnr.app.data.local.db.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads WHERE chapterId = :chapterId")
    fun getDownloadByChapterId(chapterId: String): Flow<DownloadEntity?>

    @Upsert
    suspend fun upsertDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE chapterId = :chapterId")
    suspend fun deleteDownload(chapterId: String)

    @Query("SELECT * FROM downloads WHERE state IN ('QUEUED', 'DOWNLOADING')")
    suspend fun getPendingDownloads(): List<DownloadEntity>

    @Query("UPDATE downloads SET state = :state, workerId = :workerId WHERE chapterId = :chapterId")
    suspend fun updateState(chapterId: String, state: String, workerId: String?)
}
