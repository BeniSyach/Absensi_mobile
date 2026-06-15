package com.dss.absensiKoas.data.api

import com.dss.absensiKoas.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Interceptor yang otomatis menambahkan header Authorization: Bearer {token}
 * dan X-Device-Info ke setiap request (kecuali login/registrasi).
 */
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip auth header untuk endpoint publik
        val path = originalRequest.url.encodedPath
        val isPublicEndpoint = path.contains("/auth/login") ||
                path.contains("/auth/refresh") ||
                path.contains("/user/registrasi")

        if (isPublicEndpoint) {
            return chain.proceed(originalRequest)
        }

        val token = runBlocking { tokenManager.getAccessToken() }

        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("X-Device-Info", "Android ${android.os.Build.VERSION.RELEASE} ${android.os.Build.MODEL}")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(newRequest)
    }
}