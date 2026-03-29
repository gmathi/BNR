package com.bnr.app.di

import android.content.Context
import androidx.work.WorkManager
import coil.ImageLoader
import com.bnr.app.source.Source
import com.bnr.app.source.SourceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSourceManager(
        sources: Set<@JvmSuppressWildcards Source>
    ): SourceManager = SourceManager(sources)

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    /**
     * Coil ImageLoader reuses the same OkHttpClient so that Cloudflare-protected
     * cover images automatically get the solved cookies injected.
     */
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): ImageLoader = ImageLoader.Builder(context)
        .okHttpClient(okHttpClient)
        .crossfade(true)
        .build()
}
