package com.spotifyplayer.app.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.spotifyplayer.app.BuildConfig
import com.spotifyplayer.app.R

class LoginActivity : AppCompatActivity() {

    private lateinit var authManager: SpotifyAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        authManager = SpotifyAuthManager(this)

        startLogin()
    }

    private fun startLogin() {
        val request = AuthorizationRequest.Builder(
            BuildConfig.SPOTIFY_CLIENT_ID,
            AuthorizationResponse.Type.TOKEN,
            BuildConfig.SPOTIFY_REDIRECT_URI
        )
            .setScopes(
                arrayOf(
                    "app-remote-control",
                    "streaming",
                    "user-library-read"
                )
            )
            .build()

        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode != REQUEST_CODE) return

        val response = AuthorizationClient.getResponse(resultCode, intent)
        when (response.type) {
            AuthorizationResponse.Type.TOKEN -> {
                val token = response.accessToken
                val expiresIn = response.expiresIn.toLong()
                authManager.saveToken(token, expiresIn)
                Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show()
                goToMain()
            }
            AuthorizationResponse.Type.ERROR -> {
                val message = response.error ?: "Unknown error"
                Log.e(TAG, "Auth error: $message")
                Toast.makeText(this, getString(R.string.login_error, message), Toast.LENGTH_LONG).show()
                finish()
            }
            else -> {
                // Cancel or other responses
                Log.w(TAG, "Auth canceled or other response: ${response.type}")
                finish()
            }
        }
    }

    private fun goToMain() {
        val intent = Intent(this, com.spotifyplayer.app.MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    companion object {
        private const val REQUEST_CODE = 1337
        private const val TAG = "LoginActivity"
    }
}

