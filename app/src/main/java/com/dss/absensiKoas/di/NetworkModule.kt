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
    }

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
     * Client "polos" TANPA AuthInterceptor & TANPA TokenAuthenticator.
     * Dipakai KHUSUS oleh TokenAuthenticator untuk memanggil endpoint
     * /api/v1/auth/refresh secara manual.
     *
     * Wajib terpisah dari client utama — kalau dipakai bareng, saat refresh
     * gagal authenticator akan terpanggil lagi untuk request refresh-nya
     * sendiri -> infinite loop / stack overflow.
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
            .build()
    }

    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        tokenManager: TokenManager,
        sessionManager: SessionManager,
        @Named("plain") plainClient: OkHttpClient,
        json: Json
    ): TokenAuthenticator {
        return TokenAuthenticator(
            tokenManager      = tokenManager,
            sessionManager    = sessionManager,
            plainOkHttpClient = plainClient,
            baseUrl           = BuildConfig.BASE_URL.trimEnd('/'),
            json              = json
        )
    }

    /**
     * Client UTAMA yang dipakai Retrofit untuk semua request aplikasi.
     * - addInterceptor(authInterceptor)   -> sisip header Authorization di setiap request
     * - authenticator(tokenAuthenticator) -> otomatis coba refresh token saat dapat 401,
     *   dan trigger "sesi habis" via SessionManager kalau refresh juga gagal
     *
     * Baris .authenticator(...) inilah yang HILANG di kode sebelumnya —
     * tanpa baris ini, TokenAuthenticator yang sudah dibuat tidak akan pernah
     * terpanggil sama sekali, sehingga fitur auto-refresh-token tidak berfungsi.
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
        json: Json
    ): Retrofit {

        android.util.Log.d("BASE_URL", BuildConfig.BASE_URL)

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideAbsensiApi(retrofit: Retrofit): AbsensiApi {
        return retrofit.create(AbsensiApi::class.java)
    }
}