package com.bnr.app.di

import com.bnr.app.source.Source
import com.bnr.app.source.impl.ExampleSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class SourceModule {

    /**
     * Register each source implementation here with @Binds @IntoSet.
     * SourceManager receives the full Set<Source> automatically via Hilt multibinding.
     */
    @Binds
    @IntoSet
    abstract fun bindExampleSource(source: ExampleSource): Source
}
