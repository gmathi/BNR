package com.bnr.app.di

import com.bnr.app.data.repository.ChapterRepositoryImpl
import com.bnr.app.data.repository.DownloadRepositoryImpl
import com.bnr.app.data.repository.NovelRepositoryImpl
import com.bnr.app.data.repository.NovelUpdateRepositoryImpl
import com.bnr.app.data.repository.ReaderSettingsRepositoryImpl
import com.bnr.app.domain.repository.ChapterRepository
import com.bnr.app.domain.repository.DownloadRepository
import com.bnr.app.domain.repository.NovelRepository
import com.bnr.app.domain.repository.NovelUpdateRepository
import com.bnr.app.domain.repository.ReaderSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNovelRepository(impl: NovelRepositoryImpl): NovelRepository

    @Binds
    @Singleton
    abstract fun bindChapterRepository(impl: ChapterRepositoryImpl): ChapterRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindReaderSettingsRepository(impl: ReaderSettingsRepositoryImpl): ReaderSettingsRepository

    @Binds
    @Singleton
    abstract fun bindNovelUpdateRepository(impl: NovelUpdateRepositoryImpl): NovelUpdateRepository
}
