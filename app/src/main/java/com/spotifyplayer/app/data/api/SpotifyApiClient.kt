package com.spotifyplayer.app.data.api

import android.content.Context
import com.spotifyplayer.app.BuildConfig
import com.spotifyplayer.app.auth.SpotifyAuthManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class SpotifyApiClient private constructor(service: SpotifyApiService) {

    val api: SpotifyApiService = service

    companion object {
        private const val BASE_URL = "https://api.spotify.com/v1/"

        @Volatile
        private var INSTANCE: SpotifyApiClient? = null

        fun getInstance(context: Context): SpotifyApiClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildClient(context).also { INSTANCE = it }
            }
        }

        private fun buildClient(context: Context): SpotifyApiClient {
            val authManager = SpotifyAuthManager(context.applicationContext)

            val logging = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }

            val okHttp = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(AuthInterceptor { authManager.getAccessToken() })
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttp)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(SpotifyApiService::class.java)
            return SpotifyApiClient(service)
        }
    }
}

