package com.spotifyplayer.app.playback

import android.content.Context
import com.spotifyplayer.app.data.db.AppDatabase
import com.spotifyplayer.app.data.db.entity.QueueItemEntity
import com.spotifyplayer.app.data.db.entity.TrackEntity
import com.spotifyplayer.app.data.db.dao.TrackPlayCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QueueManager(context: Context) {

    private val db = AppDatabase.getInstance(context.applicationContext)
    private val queueDao = db.queueDao()
    private val trackDao = db.trackDao()
    private val statsDao = db.trackStatisticsDao()
    private val shuffleManager = ShuffleManager(context)

    /**
     * Initialize queue if empty: current = first track, upcoming = next up to 30, no history.
     */
    suspend fun ensureInitialized(tracks: List<TrackEntity>): Boolean = withContext(Dispatchers.IO) {
        val existing = queueDao.getAllOrdered()
        if (existing.isNotEmpty()) return@withContext true
        initialize(tracks)
    }

    suspend fun initialize(tracks: List<TrackEntity>): Boolean = withContext(Dispatchers.IO) {
        if (tracks.isEmpty()) return@withContext false
        val items = mutableListOf<QueueItemEntity>()
        val current = tracks.first()
        items.add(
            QueueItemEntity(
                trackId = current.id,
                queuePosition = 0,
                addedAt = System.currentTimeMillis()
            )
        )
        val upcoming = pickNextTracks(
            exclude = setOf(current.id),
            count = 30,
            allTracks = tracks,
            currentIndex = tracks.indexOfFirst { it.id == current.id }.let { if (it == -1) 0 else it }
        )
        upcoming.forEachIndexed { index, track ->
            items.add(
                QueueItemEntity(
                    trackId = track.id,
                    queuePosition = index + 1,
                    addedAt = System.currentTimeMillis()
                )
            )
        }
        queueDao.clear()
        queueDao.upsertAll(items)
        true
    }

    suspend fun getCurrentTrack(): TrackEntity? = withContext(Dispatchers.IO) {
        val current = queueDao.getCurrent() ?: return@withContext null
        trackDao.getById(current.trackId)
    }

    suspend fun moveToNext(tracks: List<TrackEntity>): TrackEntity? = moveToPosition(1, tracks)

    suspend fun moveToPrevious(tracks: List<TrackEntity>): TrackEntity? = moveToPosition(-1, tracks)

    suspend fun jumpTo(targetPosition: Int, tracks: List<TrackEntity>): TrackEntity? =
        moveToPosition(targetPosition, tracks)

    suspend fun getQueueSnapshot(): List<Triple<Int, TrackEntity, Long>> = withContext(Dispatchers.IO) {
        val ordered = queueDao.getAllOrdered()
        val trackMap = trackDao.getAll().associateBy { it.id }
        val ids = ordered.map { it.trackId }
        val playCounts: Map<String, Long> = statsDao.getPlayCounts(ids).associate { it.trackId to it.playCount }
        ordered.mapNotNull { qi ->
            val track = trackMap[qi.trackId] ?: return@mapNotNull null
            Triple(qi.queuePosition, track, playCounts[qi.trackId] ?: 0L)
        }
    }

    /**
     * Rebuild queue so that the track at targetPos becomes position 0.
     * Items before it become history (-1, -2, ...), items after become upcoming (1, 2, ...).
     */
    private suspend fun moveToPosition(targetPos: Int, tracks: List<TrackEntity>): TrackEntity? = withContext(Dispatchers.IO) {
        val ordered = queueDao.getAllOrdered()
        val target = ordered.firstOrNull { it.queuePosition == targetPos } ?: return@withContext null

        val before = ordered.filter { it.queuePosition < targetPos }.sortedBy { it.queuePosition }
        val after = ordered.filter { it.queuePosition > targetPos }.sortedBy { it.queuePosition }

        val newItems = mutableListOf<QueueItemEntity>()

        // history: take last 20 from before, most recent at -1
        val history = before.takeLast(20).asReversed()
        history.forEachIndexed { index, item ->
            newItems.add(
                item.copy(
                    queuePosition = -(index + 1),
                    addedAt = System.currentTimeMillis()
                )
            )
        }

        // current
        newItems.add(
            target.copy(
                queuePosition = 0,
                addedAt = System.currentTimeMillis()
            )
        )

        // upcoming: keep all after to allow deeper upcoming when replaying history
        val keptAfter = after.toMutableList()
        val excludeIds = newItems.map { it.trackId }.toMutableSet().also { set ->
            keptAfter.forEach { set.add(it.trackId) }
        }
        val currentIdx = tracks.indexOfFirst { it.id == target.trackId }.let { if (it == -1) 0 else it }
        val fillNeeded = (30 - keptAfter.size).coerceAtLeast(0)
        if (fillNeeded > 0) {
            val fillTracks = pickNextTracks(
                exclude = excludeIds,
                count = fillNeeded,
                allTracks = tracks,
                currentIndex = currentIdx
            )
            fillTracks.forEachIndexed { idx, track ->
                keptAfter.add(
                    QueueItemEntity(
                        trackId = track.id,
                        queuePosition = keptAfter.size + idx + 1,
                        addedAt = System.currentTimeMillis()
                    )
                )
            }
        }
        keptAfter.forEachIndexed { index, item ->
            newItems.add(
                item.copy(
                    queuePosition = index + 1,
                    addedAt = System.currentTimeMillis()
                )
            )
        }

        queueDao.clear()
        queueDao.upsertAll(newItems)

        trackDao.getById(target.trackId)
    }

    private suspend fun pickNextTracks(
        exclude: Set<String>,
        count: Int,
        allTracks: List<TrackEntity>,
        currentIndex: Int
    ): List<TrackEntity> = withContext(Dispatchers.IO) {
        if (count <= 0 || allTracks.isEmpty()) return@withContext emptyList()

        // Delegate to ShuffleManager for consistent shuffle logic
        shuffleManager.getNextShuffleTracks(
            count = count,
            excludeIds = exclude
        )
    }

    /**
     * Rebuild upcoming queue with fresh shuffle.
     * Called when user clicks shuffle button.
     * Keeps current track and history, replaces upcoming with new shuffle.
     *
     * @return true if shuffle was successful, false if no current track
     */
    suspend fun shuffleUpcoming(): Boolean = withContext(Dispatchers.IO) {
        val ordered = queueDao.getAllOrdered()
        if (ordered.isEmpty()) return@withContext false

        // Separate current, history, and upcoming
        val current = ordered.firstOrNull { it.queuePosition == 0 } ?: return@withContext false
        val history = ordered.filter { it.queuePosition < 0 }.sortedBy { it.queuePosition }

        // Get all track IDs to exclude from shuffle
        val excludeIds = mutableSetOf<String>().apply {
            add(current.trackId)
            history.forEach { add(it.trackId) }
        }

        // Get fresh shuffle for upcoming queue (30 tracks)
        val newUpcoming = shuffleManager.getNextShuffleTracks(
            count = 30,
            excludeIds = excludeIds
        )

        if (newUpcoming.isEmpty()) return@withContext false

        // Rebuild queue: history + current + new upcoming
        val newItems = mutableListOf<QueueItemEntity>()

        // Keep history as-is
        history.forEach { item ->
            newItems.add(
                item.copy(addedAt = System.currentTimeMillis())
            )
        }

        // Keep current as-is
        newItems.add(
            current.copy(addedAt = System.currentTimeMillis())
        )

        // Add new upcoming tracks
        newUpcoming.forEachIndexed { index, track ->
            newItems.add(
                QueueItemEntity(
                    trackId = track.id,
                    queuePosition = index + 1,
                    addedAt = System.currentTimeMillis()
                )
            )
        }

        // Replace entire queue
        queueDao.clear()
        queueDao.upsertAll(newItems)

        true
    }
}

