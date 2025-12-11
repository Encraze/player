package com.spotifyplayer.app.api

import android.content.Context
import android.util.Log
import com.spotifyplayer.app.auth.SpotifyAuthManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that automatically refreshes expired tokens
 * and retries failed requests with new token
 */
class TokenRefreshInterceptor(private val context: Context) : Interceptor {
    
    private val authManager = SpotifyAuthManager(context)
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Make the initial request
        var response = chain.proceed(originalRequest)
        
        // If we get 401 Unauthorized, try to refresh the token
        if (response.code == 401) {
            Log.d(TAG, "Received 401, attempting to refresh token")
            
            synchronized(this) {
                // Close the original response
                response.close()
                
                // Try to refresh the token
                val refreshResult = runBlocking {
                    authManager.refreshAccessToken()
                }
                
                refreshResult.onSuccess { tokenResponse ->
                    Log.d(TAG, "Token refreshed successfully")
                    
                    // Get the new access token
                    val newAccessToken = tokenResponse.accessToken
                    
                    // Retry the original request with new token
                    val newRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $newAccessToken")
                        .build()
                    
                    response = chain.proceed(newRequest)
                }
                
                refreshResult.onFailure { exception ->
                    Log.e(TAG, "Failed to refresh token", exception)
                    // Return the original 401 response
                }
            }
        }
        
        return response
    }
    
    companion object {
        private const val TAG = "TokenRefreshInterceptor"
    }
}

