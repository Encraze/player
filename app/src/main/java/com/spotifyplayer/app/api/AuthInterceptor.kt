package com.spotifyplayer.app.api

import android.content.Context
import com.spotifyplayer.app.auth.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that adds Authorization header with access token to all requests
 */
class AuthInterceptor(context: Context) : Interceptor {
    
    private val tokenStorage = TokenStorage(context)
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Get access token
        val accessToken = tokenStorage.getAccessToken()
        
        // If we have a token, add it to the request
        val request = if (accessToken != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            originalRequest
        }
        
        return chain.proceed(request)
    }
}

