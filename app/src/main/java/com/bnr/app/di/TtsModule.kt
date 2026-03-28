package com.bnr.app.di

import com.bnr.app.tts.TtsEngine
import com.bnr.app.tts.impl.AndroidTtsEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TtsModule {

    @Binds
    @Singleton
    abstract fun bindDefaultTtsEngine(engine: AndroidTtsEngine): TtsEngine
}
