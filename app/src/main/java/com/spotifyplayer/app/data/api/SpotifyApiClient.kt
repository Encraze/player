package com.spotifyplayer.app.data.api

import android.content.Context
import com.google.gson.GsonBuilder
import com.spotifyplayer.app.R
import com.spotifyplayer.app.api.AuthInterceptor
import com.spotifyplayer.app.api.TokenRefreshInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Spotify API client - sets up Retrofit with all necessary configurations
 */
class SpotifyApiClient(private val context: Context) {
    
    private val baseUrl: String by lazy {
        context.getString(R.string.spotify_api_base_url)
    }
    
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(context))
            .addInterceptor(TokenRefreshInterceptor(context))
            .addInterceptor(createLoggingInterceptor())
            .build()
    }
    
    private val gson by lazy {
        GsonBuilder()
            .setLenient()
            .create()
    }
    
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    val apiService: SpotifyApiService by lazy {
        retrofit.create(SpotifyApiService::class.java)
    }
    
    /**
     * Create logging interceptor for debugging
     */
    private fun createLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            // Enable detailed logging for debugging
            level = HttpLoggingInterceptor.Level.BODY
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: SpotifyApiClient? = null
        
        /**
         * Get singleton instance of SpotifyApiClient
         */
        fun getInstance(context: Context): SpotifyApiClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SpotifyApiClient(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}

