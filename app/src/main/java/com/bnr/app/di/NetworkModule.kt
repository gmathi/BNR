package com.bnr.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.bnr.app.core.network.CloudflareInterceptor
import com.bnr.app.core.network.HostCookieJar
import com.bnr.app.core.network.UserAgentInterceptor
import com.bnr.app.data.remote.NetworkClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHostCookieJar(): HostCookieJar = HostCookieJar()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cookieJar: HostCookieJar,
        cloudflareInterceptor: CloudflareInterceptor,
        userAgentInterceptor: UserAgentInterceptor
    ): OkHttpClient = NetworkClient.buildOkHttpClient(
        cookieJar = cookieJar,
        cloudflareInterceptor = cloudflareInterceptor,
        userAgentInterceptor = userAgentInterceptor,
        enableLogging = false
    )

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore
}
