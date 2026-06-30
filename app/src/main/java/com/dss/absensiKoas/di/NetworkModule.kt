package com.dss.absensiKoas.di

import com.dss.absensiKoas.BuildConfig
import com.dss.absensiKoas.data.api.AbsensiApi
import com.dss.absensiKoas.data.api.AuthInterceptor
import com.dss.absensiKoas.data.api.TokenAuthenticator
import com.dss.absensiKoas.data.local.SessionManager
import com.dss.absensiKoas.data.local.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    @Named("BASE_URL")
    fun provideBaseUrl(): String = BuildConfig.BASE_URL

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    /**
     * Client khusus untuk refresh token.
     * Tidak memakai AuthInterceptor maupun Authenticator
     * agar tidak terjadi infinite loop.
     */
    @Provides
    @Singleton
    @Named("plain")
    fun providePlainOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        tokenManager: TokenManager,
        sessionManager: SessionManager,
        @Named("plain") plainClient: OkHttpClient,
        @Named("BASE_URL") baseUrl: String,
        json: Json
    ): TokenAuthenticator {
        return TokenAuthenticator(
            tokenManager = tokenManager,
            sessionManager = sessionManager,
            plainOkHttpClient = plainClient,
            baseUrl = baseUrl.trimEnd('/'),
            json = json
        )
    }

    /**
     * Client utama aplikasi.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
        @Named("BASE_URL") baseUrl: String
    ): Retrofit {

        android.util.Log.d("BASE_URL", baseUrl)

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(
                json.asConverterFactory(
                    "application/json".toMediaType()
                )
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideAbsensiApi(
        retrofit: Retrofit
    ): AbsensiApi {
        return retrofit.create(AbsensiApi::class.java)
    }
}