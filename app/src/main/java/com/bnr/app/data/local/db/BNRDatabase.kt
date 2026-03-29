package com.bnr.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bnr.app.data.local.db.dao.ChapterDao
import com.bnr.app.data.local.db.dao.DownloadDao
import com.bnr.app.data.local.db.dao.NovelDao
import com.bnr.app.data.local.db.dao.NovelUpdateDao
import com.bnr.app.data.local.db.dao.ReaderSettingsDao
import com.bnr.app.data.local.db.entity.ChapterEntity
import com.bnr.app.data.local.db.entity.DownloadEntity
import com.bnr.app.data.local.db.entity.NovelEntity
import com.bnr.app.data.local.db.entity.NovelReaderSettingsEntity
import com.bnr.app.data.local.db.entity.NovelUpdateEntity

@Database(
    entities = [
        NovelEntity::class,
        ChapterEntity::class,
        DownloadEntity::class,
        NovelReaderSettingsEntity::class,
        NovelUpdateEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class BNRDatabase : RoomDatabase() {
    abstract fun novelDao(): NovelDao
    abstract fun chapterDao(): ChapterDao
    abstract fun downloadDao(): DownloadDao
    abstract fun readerSettingsDao(): ReaderSettingsDao
    abstract fun novelUpdateDao(): NovelUpdateDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS novel_updates (
                        novelId TEXT NOT NULL PRIMARY KEY,
                        newChapterCount INTEGER NOT NULL DEFAULT 0,
                        knownChapterCount INTEGER NOT NULL DEFAULT 0,
                        lastCheckedAt INTEGER NOT NULL DEFAULT 0,
                        lastUpdatedAt INTEGER,
                        FOREIGN KEY(novelId) REFERENCES novels(id) ON DELETE CASCADE
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE novel_reader_settings ADD COLUMN readerColorPreset TEXT NOT NULL DEFAULT 'default'")
                db.execSQL("ALTER TABLE novel_reader_settings ADD COLUMN customBgColor INTEGER")
                db.execSQL("ALTER TABLE novel_reader_settings ADD COLUMN customTextColor INTEGER")
            }
        }
    }
}
