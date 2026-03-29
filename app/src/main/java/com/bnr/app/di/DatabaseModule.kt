package com.bnr.app.di

import android.content.Context
import androidx.room.Room
import com.bnr.app.data.local.db.BNRDatabase
import com.bnr.app.data.local.db.dao.ChapterDao
import com.bnr.app.data.local.db.dao.DownloadDao
import com.bnr.app.data.local.db.dao.NovelDao
import com.bnr.app.data.local.db.dao.NovelUpdateDao
import com.bnr.app.data.local.db.dao.ReaderSettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BNRDatabase =
        Room.databaseBuilder(context, BNRDatabase::class.java, "bnr.db")
            .addMigrations(BNRDatabase.MIGRATION_1_2, BNRDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideNovelDao(db: BNRDatabase): NovelDao = db.novelDao()

    @Provides
    fun provideChapterDao(db: BNRDatabase): ChapterDao = db.chapterDao()

    @Provides
    fun provideDownloadDao(db: BNRDatabase): DownloadDao = db.downloadDao()

    @Provides
    fun provideReaderSettingsDao(db: BNRDatabase): ReaderSettingsDao = db.readerSettingsDao()

    @Provides
    fun provideNovelUpdateDao(db: BNRDatabase): NovelUpdateDao = db.novelUpdateDao()
}
