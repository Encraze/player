package com.spotifyplayer.app.auth

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.spotifyplayer.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Manages Spotify OAuth 2.0 authentication with PKCE flow
 */
class SpotifyAuthManager(private val context: Context) {
    
    private val tokenStorage = TokenStorage(context)
    
    // Spotify credentials from resources
    private val clientId: String by lazy {
        context.getString(R.string.spotify_client_id)
    }
    
    private val clientSecret: String by lazy {
        context.getString(R.string.spotify_client_secret)
    }
    
    private val redirectUri: String by lazy {
        context.getString(R.string.spotify_redirect_uri)
    }
    
    private val authBaseUrl: String by lazy {
        context.getString(R.string.spotify_auth_base_url)
    }
    
    // PKCE code verifier (store temporarily during auth flow)
    private var codeVerifier: String? = null
    
    /**
     * Generate PKCE code verifier (random string)
     */
    private fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
    
    /**
     * Generate PKCE code challenge from verifier (SHA256 hash)
     */
    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val digest = messageDigest.digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
    
    /**
     * Build Spotify authorization URL with PKCE
     */
    fun buildAuthorizationUrl(): String {
        // Generate and store code verifier
        codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier!!)
        
        val scopes = listOf(
            "user-read-private",
            "user-read-email",
            "user-library-read",
            "user-modify-playback-state",
            "user-read-playback-state",
            "user-read-currently-playing"
        ).joinToString(" ")
        
        return Uri.parse("$authBaseUrl/authorize")
            .buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("scope", scopes)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", codeChallenge)
            .build()
            .toString()
    }
    
    /**
     * Exchange authorization code for access token
     */
    suspend fun exchangeCodeForToken(code: String): Result<TokenResponse> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$authBaseUrl/api/token")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.doOutput = true
            
            // Build request body
            val params = mutableMapOf(
                "grant_type" to "authorization_code",
                "code" to code,
                "redirect_uri" to redirectUri,
                "client_id" to clientId,
                "code_verifier" to (codeVerifier ?: "")
            )
            
            val postData = params.entries.joinToString("&") { (key, value) ->
                "${Uri.encode(key)}=${Uri.encode(value)}"
            }
            
            // Send request
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(postData)
                writer.flush()
            }
            
            // Read response
            val responseCode = connection.responseCode
            val inputStream = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            
            val response = BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
            
            connection.disconnect()
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val jsonResponse = JSONObject(response)
                val accessToken = jsonResponse.getString("access_token")
                val refreshToken = jsonResponse.optString("refresh_token", null)
                val expiresIn = jsonResponse.getInt("expires_in")
                
                val tokenResponse = TokenResponse(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresIn = expiresIn
                )
                
                // Store tokens
                saveTokens(tokenResponse)
                
                Result.success(tokenResponse)
            } else {
                Log.e(TAG, "Token exchange failed: $responseCode - $response")
                Result.failure(Exception("Token exchange failed: $response"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error exchanging code for token", e)
            Result.failure(e)
        }
    }
    
    /**
     * Refresh access token using refresh token
     */
    suspend fun refreshAccessToken(): Result<TokenResponse> = withContext(Dispatchers.IO) {
        try {
            val refreshToken = tokenStorage.getRefreshToken()
                ?: return@withContext Result.failure(Exception("No refresh token available"))
            
            val url = URL("$authBaseUrl/api/token")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.doOutput = true
            
            // Build request body
            val params = mapOf(
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken,
                "client_id" to clientId
            )
            
            val postData = params.entries.joinToString("&") { (key, value) ->
                "${Uri.encode(key)}=${Uri.encode(value)}"
            }
            
            // Send request
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(postData)
                writer.flush()
            }
            
            // Read response
            val responseCode = connection.responseCode
            val inputStream = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            
            val response = BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
            
            connection.disconnect()
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val jsonResponse = JSONObject(response)
                val accessToken = jsonResponse.getString("access_token")
                val newRefreshToken = jsonResponse.optString("refresh_token", refreshToken)
                val expiresIn = jsonResponse.getInt("expires_in")
                
                val tokenResponse = TokenResponse(
                    accessToken = accessToken,
                    refreshToken = newRefreshToken,
                    expiresIn = expiresIn
                )
                
                // Store new tokens
                saveTokens(tokenResponse)
                
                Result.success(tokenResponse)
            } else {
                Log.e(TAG, "Token refresh failed: $responseCode - $response")
                Result.failure(Exception("Token refresh failed: $response"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing token", e)
            Result.failure(e)
        }
    }
    
    /**
     * Save tokens to secure storage
     */
    private fun saveTokens(tokenResponse: TokenResponse) {
        tokenStorage.saveAccessToken(tokenResponse.accessToken)
        tokenResponse.refreshToken?.let { tokenStorage.saveRefreshToken(it) }
        
        // Calculate expiration time (current time + expires_in seconds - 5 minute buffer)
        val expirationTime = System.currentTimeMillis() + (tokenResponse.expiresIn - 300) * 1000
        tokenStorage.saveTokenExpirationTime(expirationTime)
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return tokenStorage.isLoggedIn()
    }
    
    /**
     * Logout (clear tokens)
     */
    fun logout() {
        tokenStorage.clearTokens()
        codeVerifier = null
    }
    
    /**
     * Get current access token
     */
    fun getAccessToken(): String? {
        return tokenStorage.getAccessToken()
    }
    
    companion object {
        private const val TAG = "SpotifyAuthManager"
    }
}

/**
 * Token response data class
 */
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String?,
    val expiresIn: Int
)

