package com.spotifyplayer.app.data.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import retrofit2.Response

/**
 * Handles API errors and converts them to user-friendly messages
 */
object ApiErrorHandler {
    
    private const val TAG = "ApiErrorHandler"
    
    /**
     * Parse error response and return user-friendly message
     */
    fun <T> handleError(response: Response<T>): String {
        return when (response.code()) {
            400 -> "Bad request. Please check your input."
            401 -> "Authentication failed. Please log in again."
            403 -> "Forbidden. You may need Spotify Premium for this feature."
            404 -> "Resource not found. Please make sure Spotify is running."
            429 -> "Too many requests. Please wait a moment and try again."
            500, 502, 503 -> "Spotify service is temporarily unavailable. Please try again later."
            else -> {
                // Try to parse error message from response
                val errorBody = response.errorBody()?.string()
                if (errorBody != null) {
                    try {
                        val error = Gson().fromJson(errorBody, SpotifyError::class.java)
                        error.error.message ?: "An error occurred: ${response.code()}"
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse error body", e)
                        "An error occurred: ${response.code()}"
                    }
                } else {
                    "An error occurred: ${response.code()}"
                }
            }
        }
    }
    
    /**
     * Handle exception and return user-friendly message
     */
    fun handleException(exception: Exception): String {
        Log.e(TAG, "API Exception", exception)
        return when (exception) {
            is java.net.UnknownHostException -> "No internet connection. Please check your network."
            is java.net.SocketTimeoutException -> "Request timed out. Please try again."
            is java.io.IOException -> "Network error. Please check your connection."
            else -> "An error occurred: ${exception.message}"
        }
    }
}

/**
 * Spotify API error response
 */
data class SpotifyError(
    @SerializedName("error")
    val error: ErrorDetails
)

data class ErrorDetails(
    @SerializedName("status")
    val status: Int,
    
    @SerializedName("message")
    val message: String?
)


