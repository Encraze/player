package com.spotifyplayer.app.playback

import android.content.Context
import com.spotifyplayer.app.data.db.AppDatabase
import com.spotifyplayer.app.data.db.entity.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages shuffle logic for the playback queue.
 *
 * Implements least-played-first shuffle algorithm:
 * - Tracks with lowest play_count come first
 * - If play_count is equal, oldest last_played_at comes first
 * - Tracks never played (last_played_at = NULL) are treated as oldest
 * - Excludes tracks already in queue
 * - Falls back to sequential selection if not enough least-played tracks available
 */
class ShuffleManager(context: Context) {

    private val db = AppDatabase.getInstance(context.applicationContext)
    private val trackDao = db.trackDao()
    private val statsDao = db.trackStatisticsDao()

    /**
     * Get next N tracks for shuffle, using least-played-first algorithm.
     *
     * @param count Number of tracks to return
     * @param excludeIds Track IDs to exclude (already in queue)
     * @return List of tracks ordered by least-played-first, up to [count] tracks
     */
    suspend fun getNextShuffleTracks(
        count: Int,
        excludeIds: Set<String> = emptySet()
    ): List<TrackEntity> = withContext(Dispatchers.IO) {
        if (count <= 0) return@withContext emptyList()

        // Get all tracks to know total available
        val allTracks = trackDao.getAll()
        if (allTracks.isEmpty()) return@withContext emptyList()

        // First, try least-played ordering
        // This query orders by: play_count ASC, COALESCE(last_played_at, 0) ASC
        // NULL last_played_at is treated as 0 (oldest)
        val leastPlayedIds = statsDao.getLeastPlayed(
            limit = count,
            excludeIds = excludeIds.toList()
        )

        // Get track entities for the least-played IDs, preserving order
        val trackMap = trackDao.getByIds(leastPlayedIds).associateBy { it.id }
        val orderedLeastPlayed = leastPlayedIds.mapNotNull { trackMap[it] }

        // If we got enough tracks from least-played, return them
        if (orderedLeastPlayed.size >= count) {
            return@withContext orderedLeastPlayed.take(count)
        }

        // Not enough tracks from least-played query - need to fill remaining
        val missing = count - orderedLeastPlayed.size
        val result = orderedLeastPlayed.toMutableList()

        // Fallback: pick remaining tracks sequentially, avoiding already picked and excluded
        val pickedIds = result.map { it.id }.toMutableSet().also { it.addAll(excludeIds) }
        val availableTracks = allTracks.filter { !pickedIds.contains(it.id) }

        if (availableTracks.isEmpty()) {
            // All tracks are already picked/excluded - return what we have
            return@withContext result
        }

        // Add remaining tracks up to count
        result.addAll(availableTracks.take(missing))

        result
    }

    /**
     * Get tracks for initial queue setup (no exclusions).
     *
     * @param count Number of tracks to return
     * @return List of tracks using least-played-first algorithm
     */
    suspend fun getInitialShuffleTracks(count: Int): List<TrackEntity> =
        getNextShuffleTracks(count = count, excludeIds = emptySet())

    /**
     * Rebuild upcoming queue with fresh shuffle.
     * This method is designed to be called when the shuffle button is pressed.
     *
     * @param count Number of upcoming tracks to generate (typically 30)
     * @param currentTrackId ID of current track (will be excluded)
     * @param historyIds IDs of tracks in history (will be excluded)
     * @return List of tracks for the new upcoming queue
     */
    suspend fun rebuildUpcoming(
        count: Int,
        currentTrackId: String?,
        historyIds: Set<String> = emptySet()
    ): List<TrackEntity> {
        val excludeIds = mutableSetOf<String>().apply {
            currentTrackId?.let { add(it) }
            addAll(historyIds)
        }

        return getNextShuffleTracks(count = count, excludeIds = excludeIds)
    }

    /**
     * Get statistics summary for shuffle algorithm validation/debugging.
     *
     * @return Map of track ID to play count
     */
    suspend fun getShuffleStats(): Map<String, Long> = withContext(Dispatchers.IO) {
        val allTracks = trackDao.getAll()
        val stats = statsDao.getPlayCounts(allTracks.map { it.id })
        stats.associate { it.trackId to it.playCount }
    }
}
