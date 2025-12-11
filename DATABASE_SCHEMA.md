# Database Schema Documentation

## Overview

The app uses Room (SQLite) to store tracks, playback statistics, queue, and history locally.

## Tables

### 1. `tracks`
Stores all liked tracks fetched from Spotify.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | TEXT | PRIMARY KEY | Spotify track ID |
| `name` | TEXT | NOT NULL | Track name |
| `artists` | TEXT | NOT NULL | Comma-separated artist names |
| `album_name` | TEXT | NOT NULL | Album name |
| `album_image_url` | TEXT | NULLABLE | URL to album cover image |
| `duration_ms` | INTEGER | NOT NULL | Track duration in milliseconds |
| `uri` | TEXT | NOT NULL | Spotify track URI (spotify:track:xxx) |
| `added_at` | INTEGER | NOT NULL | Timestamp when fetched from Spotify |
| `created_at` | INTEGER | NOT NULL | Timestamp when added to local DB |

**Purpose**: Store all user's liked tracks for offline access and queue management.

---

### 2. `track_statistics`
Stores playback statistics for each track.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `track_id` | TEXT | PRIMARY KEY, FOREIGN KEY → tracks(id) | Track ID |
| `play_count` | INTEGER | DEFAULT 0 | Number of times played (+1 per play, +2 per skip) |
| `skip_count` | INTEGER | DEFAULT 0 | Number of times skipped |
| `last_played_at` | INTEGER | NULLABLE | Timestamp of last play (NULL if never played) |
| `last_skipped_at` | INTEGER | NULLABLE | Timestamp of last skip (NULL if never skipped) |
| `total_play_time_ms` | INTEGER | DEFAULT 0 | Total time track has been played (optional) |

**Purpose**: Track play counts and timestamps for custom shuffle logic.

**Indexes**:
- `track_id` (unique) - For foreign key lookups
- `(play_count, last_played_at)` - For shuffle sorting (least played first, oldest first)

**Shuffle Logic**:
- Sort by `play_count ASC` (least played first)
- If equal, sort by `last_played_at ASC` (oldest first)
- NULL `last_played_at` treated as oldest

---

### 3. `playback_history`
Stores the last 20 played tracks for navigation.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Auto-generated ID |
| `track_id` | TEXT | FOREIGN KEY → tracks(id) | Track ID |
| `played_at` | INTEGER | NOT NULL | Timestamp when played |
| `was_skipped` | INTEGER | DEFAULT 0 | Boolean: 1 if skipped, 0 if played |
| `playback_position` | INTEGER | NOT NULL | Position in history for ordering |

**Purpose**: Maintain 20-track history for "previous" navigation.

**Indexes**:
- `played_at DESC` - For retrieving recent history
- `track_id` - For track lookups

**Behavior**:
- Keep only last 20 entries
- Oldest entries are automatically removed when new ones are added

---

### 4. `queue_items`
Stores current playback queue (temporary, doesn't persist across restarts).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Auto-generated ID |
| `track_id` | TEXT | FOREIGN KEY → tracks(id) | Track ID |
| `queue_position` | INTEGER | UNIQUE, NOT NULL | Position in queue |
| `added_at` | INTEGER | NOT NULL | Timestamp when added to queue |

**Purpose**: Manage playback queue with history and upcoming tracks.

**Indexes**:
- `queue_position` (unique) - For ordering and position lookups
- `track_id` - For track lookups

**Queue Position Convention**:
- `-20 to -1`: History (20 tracks behind current)
- `0`: Current track playing
- `1 to 30`: Upcoming tracks (30 tracks ahead)
- **Total**: 51 tracks visible (20 + 1 + 30)

**Behavior**:
- Queue is cleared on app restart (as per requirements)
- Positions shift as tracks are played
- New tracks added to position 30 as queue advances

---

## Relationships

```
tracks (1) ←→ (1) track_statistics
  ↓
  ↓ (1:N)
  ↓
playback_history

tracks (1) ←→ (N) queue_items
```

- Each track can have one statistics entry
- Each track can appear multiple times in history
- Each track can appear once in the queue (unique position)

---

## Data Flow

### 1. Initial Load
```
Spotify API → tracks table
            → track_statistics table (initialized with 0 counts)
```

### 2. Playback Start
```
User selects track
  ↓
Update track_statistics (play_count +1, last_played_at)
  ↓
Add to playback_history
  ↓
Set as current in queue_items (position = 0)
```

### 3. Track Skip
```
User skips track
  ↓
Update track_statistics (play_count +2, last_skipped_at)
  ↓
Mark as skipped in playback_history
  ↓
Advance queue (shift positions)
```

### 4. Queue Jump
```
User selects track from queue
  ↓
All tracks above selected → marked as skipped (+2)
  ↓
Selected track → current (position = 0)
  ↓
Rebuild upcoming queue from shuffle logic
```

### 5. Shuffle
```
Query track_statistics
  ↓
Sort by: play_count ASC, last_played_at ASC
  ↓
Select top 30 tracks (excluding current queue)
  ↓
Add to queue_items (positions 1-30)
```

---

## Performance Considerations

### Indexes
All critical queries are indexed:
- Track lookups by ID (primary keys)
- Statistics sorting for shuffle (composite index)
- History retrieval by time (DESC index)
- Queue ordering by position (unique index)

### Query Patterns
- **Frequent**: Get queue items, update statistics
- **Moderate**: Get shuffle tracks, add to history
- **Infrequent**: Fetch all tracks, clear queue

### Optimization
- Use transactions for batch operations
- Limit history to 20 entries (auto-cleanup)
- Queue is temporary (no persistence overhead)
- Statistics updates are atomic

---

## Storage Estimates

For 10,000 tracks:
- `tracks`: ~10,000 rows × ~500 bytes = ~5 MB
- `track_statistics`: ~10,000 rows × ~100 bytes = ~1 MB
- `playback_history`: 20 rows × ~50 bytes = ~1 KB
- `queue_items`: 51 rows × ~50 bytes = ~2.5 KB

**Total**: ~6 MB for 10,000 tracks (excluding album images)

Note: Album images are referenced by URL and fetched on-demand, not stored in the database.

---

## Migration Strategy

### Version 1 (Initial)
- Create all tables
- Create all indexes
- No data migration needed

### Future Versions
- Use Room migrations for schema changes
- Preserve user statistics during updates
- Clear queue on schema changes (temporary data)

---

## Notes

- Queue doesn't persist across app restarts (by design)
- Statistics are local only (not synced to Spotify)
- Album images are referenced by URL (not stored locally)
- All timestamps are in milliseconds (Unix epoch)
- Foreign keys enforce referential integrity
- CASCADE delete ensures cleanup when tracks are removed

