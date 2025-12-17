package com.spotifyplayer.app.playback

import android.content.Context
import com.spotifyplayer.app.data.db.AppDatabase
import com.spotifyplayer.app.data.repository.HistoryRepository
import com.spotifyplayer.app.data.repository.StatisticsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Coordinates statistics and history updates for playback events.
 *
 * This class ensures that both track_statistics and playback_history tables
 * are updated consistently when tracks are played or skipped.
 *
 * According to Phase 6.1 of the plan:
 * - Play: increment play_count by 1, update last_played_at, add to history
 * - Skip: increment play_count by 2, update last_played_at, mark as skipped in history
 * - Multiple skips: increment play_count by 2 for each, update timestamps, mark all as skipped
 */
class StatisticsUpdater(context: Context) {

    private val db = AppDatabase.getInstance(context.applicationContext)
    private val statsRepo = StatisticsRepository(db.trackStatisticsDao())
    private val historyRepo = HistoryRepository(db.playbackHistoryDao())

    /**
     * Record a track play event.
     *
     * Updates:
     * - Increments play_count by 1
     * - Updates last_played_at timestamp
     * - Adds entry to playback_history with wasSkipped=false
     * - Trims history to keep only last 20 entries
     *
     * @param trackId The track that was played
     * @param timestamp The timestamp when the track started playing
     */
    suspend fun recordPlay(trackId: String, timestamp: Long) = withContext(Dispatchers.IO) {
        // Update statistics
        statsRepo.recordPlay(trackId, timestamp)

        // Add to history
        historyRepo.addToHistory(
            trackId = trackId,
            wasSkipped = false,
            playedAt = timestamp,
            playbackPosition = null
        )

        // Keep only last 20 history entries
        historyRepo.trimTo(20)
    }

    /**
     * Record a track skip event (user skipped without playing).
     *
     * Updates:
     * - Increments play_count by 2 (penalty for skipping)
     * - Updates last_played_at timestamp
     * - Adds entry to playback_history with wasSkipped=true
     * - Trims history to keep only last 20 entries
     *
     * @param trackId The track that was skipped
     * @param timestamp The timestamp when the track was skipped
     */
    suspend fun recordSkip(trackId: String, timestamp: Long) = withContext(Dispatchers.IO) {
        // Update statistics with skip penalty
        statsRepo.recordSkip(trackId, timestamp)

        // Add to history as skipped
        historyRepo.addToHistory(
            trackId = trackId,
            wasSkipped = true,
            playedAt = timestamp,
            playbackPosition = null
        )

        // Keep only last 20 history entries
        historyRepo.trimTo(20)
    }

    /**
     * Record multiple skip events (when jumping ahead in queue).
     *
     * Updates for each track:
     * - Increments play_count by 2 (penalty for skipping)
     * - Updates last_played_at timestamp
     * - Adds entry to playback_history with wasSkipped=true
     * - Trims history to keep only last 20 entries
     *
     * @param trackIds List of tracks that were skipped
     * @param timestamp The timestamp when the tracks were skipped
     */
    suspend fun recordMultipleSkips(trackIds: List<String>, timestamp: Long) = withContext(Dispatchers.IO) {
        if (trackIds.isEmpty()) return@withContext

        // Update statistics for all skipped tracks
        statsRepo.recordMultipleSkips(trackIds, timestamp)

        // Add each to history as skipped
        trackIds.forEach { trackId ->
            historyRepo.addToHistory(
                trackId = trackId,
                wasSkipped = true,
                playedAt = timestamp,
                playbackPosition = null
            )
        }

        // Keep only last 20 history entries
        historyRepo.trimTo(20)
    }

    /**
     * Get recent playback history.
     *
     * @param limit Number of history entries to retrieve (default 20)
     * @return List of history entries ordered by most recent first
     */
    suspend fun getHistory(limit: Int = 20) = withContext(Dispatchers.IO) {
        historyRepo.getRecent(limit)
    }

    /**
     * Custom increment for special cases (e.g., partial skip penalty).
     * This is used in MainActivity for handling skips based on playback position.
     *
     * @param trackId The track to update
     * @param delta The amount to increment play_count
     * @param timestamp The timestamp for the update
     */
    suspend fun incrementPlayCount(trackId: String, delta: Int, timestamp: Long) = withContext(Dispatchers.IO) {
        statsRepo.incrementPlayCount(trackId, delta, timestamp)
    }

    /**
     * Custom increment for multiple tracks (e.g., batch skip penalties).
     *
     * @param trackIds List of tracks to update
     * @param delta The amount to increment play_count for each
     * @param timestamp The timestamp for the update
     */
    suspend fun incrementPlayCount(trackIds: List<String>, delta: Int, timestamp: Long) = withContext(Dispatchers.IO) {
        statsRepo.incrementPlayCount(trackIds, delta, timestamp)
    }

    /**
     * Clear all playback history (optional maintenance operation).
     * This does NOT affect track statistics, only the history table.
     */
    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        historyRepo.clearHistory()
    }
}

