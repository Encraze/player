package com.spotifyplayer.app.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure storage for Spotify authentication tokens using EncryptedSharedPreferences
 */
class TokenStorage(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    /**
     * Save access token
     */
    fun saveAccessToken(token: String) {
        sharedPreferences.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }
    
    /**
     * Get access token
     */
    fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }
    
    /**
     * Save refresh token
     */
    fun saveRefreshToken(token: String) {
        sharedPreferences.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }
    
    /**
     * Get refresh token
     */
    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }
    
    /**
     * Save token expiration time (timestamp in milliseconds)
     */
    fun saveTokenExpirationTime(expirationTime: Long) {
        sharedPreferences.edit().putLong(KEY_EXPIRATION_TIME, expirationTime).apply()
    }
    
    /**
     * Get token expiration time (timestamp in milliseconds)
     */
    fun getTokenExpirationTime(): Long {
        return sharedPreferences.getLong(KEY_EXPIRATION_TIME, 0L)
    }
    
    /**
     * Check if access token is expired
     */
    fun isTokenExpired(): Boolean {
        val expirationTime = getTokenExpirationTime()
        return expirationTime == 0L || System.currentTimeMillis() >= expirationTime
    }
    
    /**
     * Check if user is logged in (has valid tokens)
     */
    fun isLoggedIn(): Boolean {
        return getAccessToken() != null && getRefreshToken() != null
    }
    
    /**
     * Clear all tokens (logout)
     */
    fun clearTokens() {
        sharedPreferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRATION_TIME)
            .apply()
    }
    
    companion object {
        private const val PREFS_NAME = "encrypted_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRATION_TIME = "expiration_time"
    }
}

