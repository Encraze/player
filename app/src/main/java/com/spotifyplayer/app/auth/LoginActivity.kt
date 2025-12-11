package com.spotifyplayer.app.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.spotifyplayer.app.MainActivity
import com.spotifyplayer.app.R
import kotlinx.coroutines.launch

/**
 * Login activity that handles Spotify OAuth 2.0 authentication
 */
class LoginActivity : AppCompatActivity() {
    
    private lateinit var authManager: SpotifyAuthManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        try {
            authManager = SpotifyAuthManager(this)
            
            // Check if already logged in
            if (authManager.isLoggedIn()) {
                navigateToMain()
                return
            }
            
            // Check if this is a redirect callback from Spotify
            val uri = intent?.data
            if (uri != null && uri.scheme == "spotifyplayer") {
                handleAuthorizationResponse(uri)
            } else {
                // Setup login button
                setupLoginButton()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            showError("Initialization error: ${e.message}")
        }
    }
    
    /**
     * Setup login button click handler
     */
    private fun setupLoginButton() {
        val loginButton = findViewById<android.widget.Button>(R.id.loginButton)
        loginButton.setOnClickListener {
            startSpotifyAuthorization()
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        
        // Handle redirect callback
        val uri = intent?.data
        if (uri != null && uri.scheme == "spotifyplayer") {
            handleAuthorizationResponse(uri)
        }
    }
    
    /**
     * Start Spotify authorization flow by opening browser
     */
    private fun startSpotifyAuthorization() {
        try {
            val authUrl = authManager.buildAuthorizationUrl()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting authorization", e)
            showError("Failed to start authorization: ${e.message}")
        }
    }
    
    /**
     * Handle authorization response from Spotify redirect
     */
    private fun handleAuthorizationResponse(uri: Uri) {
        val code = uri.getQueryParameter("code")
        val error = uri.getQueryParameter("error")
        
        when {
            error != null -> {
                Log.e(TAG, "Authorization error: $error")
                showError("Authorization failed: $error")
                finish()
            }
            code != null -> {
                // Exchange authorization code for access token
                exchangeCodeForToken(code)
            }
            else -> {
                Log.e(TAG, "No code or error in redirect URI")
                showError("Invalid authorization response")
                finish()
            }
        }
    }
    
    /**
     * Exchange authorization code for access token
     */
    private fun exchangeCodeForToken(code: String) {
        lifecycleScope.launch {
            try {
                val result = authManager.exchangeCodeForToken(code)
                
                result.onSuccess { tokenResponse ->
                    Log.d(TAG, "Successfully obtained access token")
                    Toast.makeText(
                        this@LoginActivity,
                        "Login successful!",
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToMain()
                }
                
                result.onFailure { exception ->
                    Log.e(TAG, "Failed to exchange code for token", exception)
                    showError("Login failed: ${exception.message}")
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during token exchange", e)
                showError("Login error: ${e.message}")
                finish()
            }
        }
    }
    
    /**
     * Navigate to main activity
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    /**
     * Show error message
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    companion object {
        private const val TAG = "LoginActivity"
    }
}

