package com.bestplus.mobileinspector.di

import android.content.Context
import androidx.room.Room
import com.bestplus.mobileinspector.data.local.AppDatabase
import com.bestplus.mobileinspector.data.local.SettingsDataStore
import com.bestplus.mobileinspector.data.local.dao.RouteSheetDao
import com.bestplus.mobileinspector.data.remote.OneCApi
import com.bestplus.mobileinspector.data.remote.OneCDataSource
import com.bestplus.mobileinspector.data.repository.RouteRepositoryImpl
import com.bestplus.mobileinspector.domain.repository.RouteRepository
import com.bestplus.mobileinspector.domain.repository.SettingsRepository
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /* -------- Network -------- */

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(settings: SettingsRepository): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val token = kotlinx.coroutines.runBlocking { settings.getUserSession()?.token }
                val request = if (!token.isNullOrBlank()) {
                    chain.request().newBuilder()
                        .header("Authorization", "Basic $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            // Placeholder baseUrl — actual URL passed via @Url in OneCApi
            .baseUrl("http://localhost/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideOneCApi(retrofit: Retrofit): OneCApi =
        retrofit.create(OneCApi::class.java)

    /* -------- Database -------- */

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "inspector.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideRouteSheetDao(db: AppDatabase): RouteSheetDao =
        db.routeSheetDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsDataStore): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindRouteRepository(impl: RouteRepositoryImpl): RouteRepository
}
