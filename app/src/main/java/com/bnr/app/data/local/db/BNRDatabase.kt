package com.bnr.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bnr.app.data.local.db.dao.ChapterDao
import com.bnr.app.data.local.db.dao.DownloadDao
import com.bnr.app.data.local.db.dao.NovelDao
import com.bnr.app.data.local.db.dao.ReaderSettingsDao
import com.bnr.app.data.local.db.entity.ChapterEntity
import com.bnr.app.data.local.db.entity.DownloadEntity
import com.bnr.app.data.local.db.entity.NovelEntity
import com.bnr.app.data.local.db.entity.NovelReaderSettingsEntity

@Database(
    entities = [
        NovelEntity::class,
        ChapterEntity::class,
        DownloadEntity::class,
        NovelReaderSettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class BNRDatabase : RoomDatabase() {
    abstract fun novelDao(): NovelDao
    abstract fun chapterDao(): ChapterDao
    abstract fun downloadDao(): DownloadDao
    abstract fun readerSettingsDao(): ReaderSettingsDao
}
