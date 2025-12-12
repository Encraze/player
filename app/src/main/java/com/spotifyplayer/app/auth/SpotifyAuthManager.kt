package com.spotifyplayer.app.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SpotifyAuthManager(context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val prefs = EncryptedSharedPreferences.create(
        PREFS_NAME,
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(accessToken: String, expiresInSeconds: Long) {
        val expiresAt = System.currentTimeMillis() + expiresInSeconds * 1000
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()
    }

    fun clearToken() {
        prefs.edit().clear().apply()
    }

    fun getAccessToken(): String? {
        if (!isTokenValid()) return null
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun isTokenValid(): Boolean {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        if (token.isNullOrBlank()) return false
        // Add small buffer to avoid near-expiry tokens
        return System.currentTimeMillis() + TOKEN_BUFFER_MS < expiresAt
    }

    companion object {
        private const val PREFS_NAME = "spotify_auth"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val TOKEN_BUFFER_MS = 60_000 // 1 minute buffer
    }
}

