# Spotify Playback Manager - Implementation Plan

## Project Overview
Android native app that manages Spotify playback with custom queue logic, shuffle algorithm, and playback statistics. The app acts as a remote control for Spotify, maintaining local state and statistics.

## Technology Stack
- **Platform**: Android Native (Kotlin/Java)
- **Database**: SQLite (via Room or SQLiteOpenHelper)
- **API**: Spotify Web API + Spotify Android SDK (for Connect integration)
- **Authentication**: OAuth 2.0 Authorization Code Flow with PKCE
- **Networking**: Retrofit/OkHttp or Volley
- **Architecture**: MVVM or Clean Architecture
- **Spotify Connect**: Spotify Android SDK for device discovery and connection management

---

## Phase 1: Project Setup & Authentication (Foundation)

### 1.1 Project Initialization ✅
**Objective**: Set up Android project with necessary dependencies

**Steps**:
1. Create new Android project (minimum SDK 24, target SDK 34+)
2. Add dependencies to `build.gradle`:
   - Retrofit + OkHttp for API calls
   - Gson/Moshi for JSON parsing
   - Room Database (or SQLiteOpenHelper)
   - AndroidX Lifecycle components
   - Coroutines for async operations
   - Secure SharedPreferences or EncryptedSharedPreferences for token storage
   - Spotify Android SDK (for Spotify Connect integration)
     - Add Spotify Maven repository
     - Include spotify-app-remote or spotify-android-auth SDK as needed
3. Configure ProGuard rules for API libraries
4. Set up network security config for API calls
5. Add internet permission to AndroidManifest.xml

**Deliverable**: Working Android project with dependencies configured

---

### 1.2 Spotify App Registration ✅
**Objective**: Register app with Spotify and obtain credentials

**Steps**:
1. Go to Spotify Developer Dashboard (https://developer.spotify.com/dashboard)
2. Create new app
3. Note down:
   - Client ID
   - Client Secret (optional for PKCE, but good to have)
4. Add redirect URI: `yourapp://callback` (or custom scheme)
   - **Note**: No backend/web server needed for mobile apps
   - This is a custom URL scheme that your Android app will handle via intent filters
   - Example: `spotifyplayer://callback` or `com.yourapp.spotify://callback`
5. Configure app settings in dashboard

**Deliverable**: Spotify app credentials (Client ID, Redirect URI)

---

### 1.3 OAuth 2.0 Authentication Implementation ✅
**Objective**: Implement secure authentication flow with token storage

**Steps**:
1. Create `SpotifyAuthManager` class:
   - Generate PKCE code verifier and challenge
   - Build authorization URL with scopes:
     - `user-read-private`
     - `user-read-email`
     - `user-library-read`
     - `user-modify-playback-state`
     - `user-read-playback-state`
     - `user-read-currently-playing`
   - Handle authorization callback
   - Exchange authorization code for access token
   - Store access token and refresh token securely (EncryptedSharedPreferences)
   - Implement token refresh logic
2. Create `TokenStorage` class:
   - Save/load access token
   - Save/load refresh token
   - Save/load token expiration time
   - Clear tokens on logout
3. Create Login Activity:
   - Launch Spotify authorization URL in WebView or Custom Tabs
   - Handle redirect callback
   - Extract authorization code
   - Exchange for tokens
   - Navigate to main app on success
   - Configure intent filter in AndroidManifest.xml:
     - Add `<intent-filter>` to handle custom URL scheme
     - Example: `<data android:scheme="yourapp" android:host="callback" />`
     - This allows the app to receive the OAuth redirect (no backend needed)
4. Create `TokenRefreshInterceptor`:
   - Intercept 401 responses
   - Automatically refresh token
   - Retry original request
5. Test authentication flow:
   - Login successfully
   - Verify token storage
   - Test token refresh
   - Test logout

**Deliverable**: Working authentication with persistent login

---

### 1.4 API Client Setup ✅
**Objective**: Create Retrofit client for Spotify Web API

**Steps**:
1. Create `SpotifyApiService` interface with endpoints:
   - `GET /v1/me/tracks` - Get saved tracks
   - `PUT /v1/me/player/play` - Start/resume playback
   - `PUT /v1/me/player/pause` - Pause playback
   - `GET /v1/me/player/currently-playing` - Get current track
   - `GET /v1/me/player` - Get playback state
   - `PUT /v1/me/player/transfer` - Transfer playback
   - `GET /v1/me/player/devices` - Get available devices
2. Create data models:
   - `Track` (id, name, artists, album, duration_ms, uri, etc.)
   - `SavedTrack` (track, added_at)
   - `PlaybackState` (is_playing, item, progress_ms, etc.)
   - `Device` (id, name, type, is_active)
   - API response wrappers
3. Create `SpotifyApiClient`:
   - Configure Retrofit with base URL: `https://api.spotify.com/v1/`
   - Add authentication interceptor (Bearer token)
   - Add token refresh interceptor
   - Add logging interceptor (for debugging)
   - Handle API errors (rate limits, network errors, etc.)
4. Create `ApiErrorHandler`:
   - Parse error responses
   - Convert to user-friendly messages
   - Handle rate limiting (429 responses)
   - Handle authentication errors (401)

**Deliverable**: Working API client with all necessary endpoints

---

## Phase 2: Database Schema & Data Models

### 2.1 Database Schema Design ✅
**Objective**: Design SQLite schema for tracks, statistics, and queue

**Steps**:
1. Create database schema with tables:

   **`tracks` table**:
   - `id` TEXT PRIMARY KEY (Spotify track ID)
   - `name` TEXT NOT NULL
   - `artists` TEXT (JSON array or comma-separated)
   - `album_name` TEXT
   - `album_image_url` TEXT
   - `duration_ms` INTEGER
   - `uri` TEXT NOT NULL
   - `added_at` INTEGER (timestamp when fetched from Spotify)
   - `created_at` INTEGER (timestamp when added to local DB)

   **`track_statistics` table**:
   - `track_id` TEXT PRIMARY KEY REFERENCES tracks(id)
   - `play_count` INTEGER DEFAULT 0
   - `skip_count` INTEGER DEFAULT 0
   - `last_played_at` INTEGER (timestamp, NULL if never played)
   - `last_skipped_at` INTEGER (timestamp, NULL if never skipped)
   - `total_play_time_ms` INTEGER DEFAULT 0 (optional)

   **`playback_history` table** (for 20 tracks history):
   - `id` INTEGER PRIMARY KEY AUTOINCREMENT
   - `track_id` TEXT REFERENCES tracks(id)
   - `played_at` INTEGER (timestamp)
   - `was_skipped` INTEGER (0 or 1, boolean)
   - `playback_position` INTEGER (position in history, for ordering)

   **`queue_items` table** (temporary, doesn't persist across restarts):
   - `id` INTEGER PRIMARY KEY AUTOINCREMENT
   - `track_id` TEXT REFERENCES tracks(id)
   - `queue_position` INTEGER (0 = current, negative = history, positive = upcoming)
   - `added_at` INTEGER (timestamp)

2. Create indexes:
   - `tracks(id)` - primary key
   - `track_statistics(track_id)` - foreign key
   - `track_statistics(play_count, last_played_at)` - for shuffle sorting
   - `playback_history(played_at DESC)` - for history retrieval
   - `queue_items(queue_position)` - for queue ordering

**Deliverable**: Database schema design document

---

### 2.2 Database Implementation ✅
**Objective**: Implement database access layer

**Steps**:
1. Create `AppDatabase` class (Room or SQLiteOpenHelper):
   - Define database version
   - Create tables in `onCreate`
   - Handle migrations in `onUpgrade`
2. Create DAO interfaces/classes:
   - `TrackDao`: Insert, update, get all tracks, get by ID
   - `TrackStatisticsDao`: 
     - Get/update statistics
     - Increment play count
     - Increment skip count
     - Update last played timestamp
     - Get tracks sorted by play count (for shuffle)
   - `PlaybackHistoryDao`:
     - Insert history entry
     - Get last 20 tracks
     - Clear old entries (keep only 20)
   - `QueueDao`:
     - Insert queue item
     - Get queue items (ordered by position)
     - Get current track (position = 0)
     - Get next N tracks (position > 0)
     - Get previous N tracks (position < 0)
     - Remove track from queue
     - Clear queue
3. Create repository classes:
   - `TrackRepository`: Manage tracks and metadata
   - `StatisticsRepository`: Manage playback statistics
   - `QueueRepository`: Manage playback queue
   - `HistoryRepository`: Manage playback history
4. Test database operations:
   - Insert test data
   - Query operations
   - Update operations
   - Delete operations

**Deliverable**: Working database with all DAOs and repositories

---

## Phase 3: Fetching & Storing Liked Tracks

### 3.1 Fetch All Liked Tracks from Spotify ✅
**Objective**: Retrieve complete list of user's liked tracks with pagination

**Steps**:
1. Create `TrackFetcher` class:
   - Implement pagination loop:
     - Start with offset = 0, limit = 50
     - Call `GET /v1/me/tracks?limit=50&offset={offset}`
     - Parse response (array of SavedTrack objects)
     - Extract track data
     - Increment offset by 50
     - Repeat until `next` field is null
   - Handle errors:
     - Network failures (retry with exponential backoff)
     - Rate limiting (wait and retry)
     - Authentication errors (trigger re-login)
2. Create progress tracking:
   - Calculate total tracks (from first response `total` field)
   - Track fetched count
   - Update UI with progress (X of Y tracks)
3. Transform API response to local models:
   - Convert Spotify track format to local Track model
   - Extract artist names (join array)
   - Extract album image URL (get largest available)
   - Store duration in milliseconds
4. Batch insert to database:
   - Collect tracks in batches (e.g., 50 at a time)
   - Insert batch to database
   - Initialize statistics entries (play_count = 0)
5. Create sync logic:
   - Check if tracks already exist in DB
   - Update existing tracks if metadata changed
   - Add new tracks
   - Optionally: detect removed tracks (tracks in DB but not in Spotify)

**Deliverable**: Complete track collection stored in local database

---

### 3.2 Track Metadata Caching ✅
**Objective**: Store all track metadata for offline access

**Steps**:
1. Store in database:
   - Track name
   - Artist names (as comma-separated string or JSON)
   - Album name
   - Album cover image URL
   - Duration
   - Spotify URI
2. Optional: Download and cache album images:
   - Download images from URLs
   - Store in app's cache directory
   - Reference cached path in database
   - Implement image loading with Glide/Picasso
3. Create `TrackMetadataManager`:
   - Get track by ID
   - Get all tracks
   - Search tracks (by name, artist)
   - Update metadata if needed

**Deliverable**: All track metadata accessible offline

---

## Phase 4: Playback Control & State Management

### 4.1 Playback Controller Implementation 🔄
**Objective**: Control Spotify playback via API

**Note on Spotify Connect**: 
- Using Spotify Android SDK (App Remote) for all playback control
- SDK provides reliable mobile playback: play, pause, skip, queue
- SDK automatically launches and connects to Spotify app
- Web API is only used for authentication and fetching tracks

**Steps**:
1. Create `PlaybackController` class:
   - `playTrack(trackId: String)`:
     - Get track URI from database
     - Call `PUT /v1/me/player/play` with body: `{"uris": ["spotify:track:TRACK_ID"]}`
     - Handle errors (show popup with error message)
   - `pausePlayback()`:
     - Call `PUT /v1/me/player/pause`
     - Handle errors
   - `resumePlayback()`:
     - Call `PUT /v1/me/player/play` (without URIs, resumes current)
     - Handle errors
   - `transferToSpotifyApp()`:
     - Option A (Web API): 
       - Call `GET /v1/me/player/devices`
       - Find Spotify app device (type = "Smartphone" or name contains "Spotify")
       - Call `PUT /v1/me/player/transfer` with device ID
     - Option B (Spotify Connect SDK - Recommended for better UX):
       - Use Spotify Android SDK's Connect API
       - Initialize Spotify App Remote connection
       - Transfer playback to Spotify app using SDK methods
       - Provides better device discovery and connection management
     - Handle errors
2. Implement error handling:
   - Network errors → show popup: "Network error. Please check your connection."
   - 401 Unauthorized → trigger token refresh, retry
   - 403 Forbidden → show popup: "Premium account required for playback control."
   - 404 Not Found → show popup: "Device not found. Please open Spotify app."
   - 429 Rate Limited → show popup: "Too many requests. Please wait a moment."
   - Generic errors → show popup with error message from API
3. Create `PlaybackStateMonitor`:
   - Poll `GET /v1/me/player/currently-playing` every 2-3 seconds when playing
   - Detect track changes:
     - Compare current track ID with previous
     - If different, track finished or skipped
   - Detect user-controlled playback:
     - If `is_playing` is true but app didn't send play command
     - Set app state to "paused" (waiting for user)
     - Don't send automatic next track
   - Store current track info:
     - Track ID
     - Progress (ms)
     - Is playing
     - Device info

**Deliverable**: Working playback control with error handling

---

### 4.2 Smart Playback Polling
**Objective**: Efficient polling based on track duration

**Steps**:
1. Implement duration-based polling:
   - When track starts playing:
     - Get track duration from database
     - Calculate end time: `startTime + duration`
   - Polling schedule:
     - Normal polling: Every 3 seconds (when playing)
     - 10 seconds before end: Increase frequency to every 1 second
     - 3 seconds before end: Make queue request (get next track ready)
     - At end time: Check if track changed, update statistics
2. Create `PlaybackScheduler`:
   - Schedule polling based on track progress
   - Cancel polling when paused
   - Resume polling when resumed
3. Handle edge cases:
   - Track duration unknown → use default polling
   - User seeks track → recalculate end time
   - Track paused → stop polling
   - Track resumed → restart polling with new schedule

**Deliverable**: Efficient polling that minimizes API calls

---

### 4.3 External Control Detection
**Objective**: Detect when user controls Spotify directly

**Steps**:
1. Track app-initiated actions:
   - Flag when app sends play command
   - Flag when app sends pause command
   - Flag when app sends next/previous
2. Compare with playback state:
   - If `is_playing` is true but app didn't send play → user controlled
   - If track changed but app didn't send next → user skipped
   - If `is_playing` is false but app sent play → user paused
3. Handle user control:
   - Set internal state to "paused" (even if Spotify is playing)
   - Stop automatic queue advancement
   - Show indicator: "Playback controlled externally"
   - Wait for user to resume from app
4. Resume from app:
   - When user clicks play in app:
     - Clear external control flag
     - Resume normal operation
     - Sync with current Spotify state

**Deliverable**: App correctly handles external Spotify control

---

## Phase 5: Queue Management System

### 5.1 Queue Data Structure
**Objective**: Implement local queue with history and upcoming tracks

**Steps**:
1. Create queue structure:
   - Current track: position = 0
   - History: positions -20 to -1 (20 tracks)
   - Upcoming: positions 1 to 30 (30 tracks)
   - Total: 51 tracks visible (20 + 1 + 30)
2. Implement `QueueManager` class:
   - `initializeQueue()`:
     - Get all tracks from database
     - Apply initial shuffle (random selection)
     - Select 30 tracks for upcoming queue
     - Set current track (first track or user selection)
     - Initialize history (empty or from previous session)
   - `getQueueItems()`:
     - Query database for positions -20 to 30
     - Return ordered list
   - `getCurrentTrack()`: Get track at position 0
   - `getNextTrack()`: Get track at position 1
   - `getPreviousTrack()`: Get track at position -1
   - `getUpcomingTracks(count: Int)`: Get tracks at positions 1 to count
   - `getHistoryTracks(count: Int)`: Get tracks at positions -count to -1

**Deliverable**: Queue data structure and basic operations

---

### 5.2 Queue Advancement Logic
**Objective**: Handle track completion and queue updates

**Steps**:
1. When track finishes (detected via polling):
   - **Note**: Statistics are NOT updated here - they are updated when track STARTS (see Phase 6.1)
   - Add to history:
     - Insert at position -20
     - Shift all history positions up by 1
     - Remove oldest history entry (position -20 becomes -19, etc.)
   - Remove from queue:
     - Delete current track (position 0)
   - Shift queue:
     - Decrement all upcoming positions by 1 (1→0, 2→1, etc.)
   - Add new track to bottom:
     - Get next track from shuffle logic
     - Insert at position 30
   - Update current:
     - Next track (old position 1) becomes current (position 0)
2. When user skips track:
   - Call `StatisticsUpdater.recordSkip(trackId)` (see Phase 6.1)
   - Same queue advancement as track finish
3. When user selects track from queue:
   - Get all tracks above selected (positions < selected position)
   - Call `StatisticsUpdater.recordMultipleSkips(trackIds)` for all skipped tracks (see Phase 6.1)
   - Remove all skipped tracks from queue
   - Set selected track as current (position 0)
   - Rebuild upcoming queue from shuffle logic
4. Create `QueueAdvancer` class:
   - `advanceQueue()`: Handle track completion (queue advancement only, no statistics)
   - `skipTrack(trackId: String)`: Handle user skip (calls StatisticsUpdater, then advances queue)
   - `jumpToTrack(position: Int)`: Handle track selection (calls StatisticsUpdater, then rebuilds queue)
   - **Important**: Statistics updates are handled by `StatisticsUpdater` from Phase 6.1

**Deliverable**: Queue automatically advances and updates

---

### 5.3 Shuffle Logic Implementation
**Objective**: Implement custom shuffle based on play statistics

**Steps**:
1. Create `ShuffleManager` class:
   - `getNextShuffleTracks(count: Int)`: Get next N tracks for queue
   - Algorithm:
     - Query tracks sorted by:
       - `play_count ASC` (least played first)
       - `last_played_at ASC` (if play_count equal, oldest first)
       - If `last_played_at` is NULL (never played), treat as oldest
     - Exclude tracks already in queue
     - Select top `count` tracks (30 for queue)
     - Return as list
2. Handle edge cases:
   - All tracks have same play count → sort by timestamp
   - No tracks available → use random selection
   - Not enough tracks → return all available
3. Integrate with queue:
   - When queue needs new tracks (bottom empty)
   - Call shuffle manager
   - Add tracks to queue positions 31-60 (or as needed)
4. Shuffle button:
   - When user clicks shuffle:
     - Clear current upcoming queue
     - Get new 30 tracks from shuffle logic
     - Replace upcoming queue
     - Keep current track and history

**Deliverable**: Custom shuffle algorithm working

---

## Phase 6: Statistics & History Tracking

### 6.1 Playback Statistics
**Objective**: Track play counts and timestamps

**Steps**:
1. Update statistics on play:
   - When track starts playing (detected via polling):
     - Increment `play_count` by 1
     - Update `last_played_at` to current timestamp
     - Add to playback_history table
2. Update statistics on skip:
   - When user skips track:
     - Increment `play_count` by 2
     - Update `last_skipped_at` to current timestamp
     - Mark in history as skipped
3. Update statistics on queue jump:
   - When user selects track from queue:
     - For each track above selected (positions < selected):
       - Increment `play_count` by 2
       - Update `last_skipped_at`
       - Mark as skipped in history
4. Create `StatisticsUpdater` class:
   - `recordPlay(trackId: String)`
   - `recordSkip(trackId: String)`
   - `recordMultipleSkips(trackIds: List<String>)`
   - Batch update for performance

**Deliverable**: Statistics accurately tracked and stored

---

### 6.2 Playback History
**Objective**: Maintain 20-track history for navigation

**Steps**:
1. Store history in database:
   - When track plays: Insert into `playback_history`
   - Keep only last 20 entries
   - Order by `played_at DESC`
2. History operations:
   - `getHistory(count: Int)`: Get last N tracks
   - `addToHistory(trackId: String, wasSkipped: Boolean)`
   - `clearHistory()`: Optional cleanup
3. Integrate with queue:
   - History is part of queue (positions -20 to -1)
   - When user navigates back, use history
   - When queue advances, update history

**Deliverable**: 20-track history maintained and accessible

---

## Phase 7: User Interface

### 7.1 Track List View
**Objective**: Display all liked tracks similar to Spotify

**Steps**:
1. Create `TrackListActivity` or Fragment:
   - RecyclerView with track items
   - Each item shows:
     - Album cover image (thumbnail)
     - Track name
     - Artist name(s)
     - Duration
     - Optional: Play count indicator
   - Implement search/filter:
     - Search by track name or artist
     - Filter options (optional)
   - Click listener:
     - On track click: Play track and navigate to playback view
2. Create `TrackAdapter`:
   - Bind track data to views
   - Load images (Glide/Picasso)
   - Handle click events
3. Implement pagination (if needed for large lists):
   - Load tracks in batches
   - Infinite scroll or "Load More" button

**Deliverable**: Scrollable track list with search

---

### 7.2 Playback Queue View
**Objective**: Show unified queue + history view (20 above + current + 30 below)

**Steps**:
1. Create `QueueViewActivity` or Fragment:
   - RecyclerView showing:
     - History section (20 tracks above, positions -20 to -1)
     - Current track section (position 0, highlighted)
     - Upcoming section (30 tracks below, positions 1 to 30)
   - Visual indicators:
     - Current track: Highlighted, larger, shows progress
     - History: Dimmed or different style
     - Upcoming: Normal style
   - Scroll to current track on load
   - Smooth scrolling as queue advances
2. Track item layout:
   - Album cover
   - Track name
   - Artist name
   - Position indicator (optional)
   - Play count (optional)
3. Click handling:
   - On track click: Jump to that track (play it)
   - Update statistics for skipped tracks
   - Rebuild queue
4. Auto-scroll:
   - When queue advances, smoothly scroll to keep current track visible
   - Or: Keep current track centered

**Deliverable**: Unified queue/history view with 51 tracks visible

---

### 7.3 Playback Controls UI
**Objective**: Play, pause, next, previous, shuffle buttons

**Steps**:
1. Create playback control bar (bottom of screen):
   - Previous button (skip to previous)
   - Play/Pause button (toggle)
   - Next button (skip to next)
   - Shuffle button (toggle shuffle mode)
   - Optional: Repeat button
2. Implement button actions:
   - Previous:
     - Get previous track from queue (position -1)
     - Play that track
     - Update statistics (current track = skipped)
   - Play/Pause:
     - If paused: Resume playback or play current track
     - If playing: Pause playback
   - Next:
     - Get next track from queue (position 1)
     - Play that track
     - Update statistics (current track = skipped)
   - Shuffle:
     - Toggle shuffle mode
     - If enabled: Rebuild upcoming queue with shuffle logic
     - If disabled: Use sequential order (or keep current order)
3. Update UI state:
   - Show play icon when paused
   - Show pause icon when playing
   - Highlight shuffle button when active
   - Disable buttons when no track available

**Deliverable**: Working playback controls

---

### 7.4 Current Track Display
**Objective**: Show currently playing track with metadata

**Steps**:
1. Create current track card/panel:
   - Large album cover image
   - Track name (large text)
   - Artist name(s)
   - Progress bar (shows playback progress)
   - Duration display (current / total)
2. Update in real-time:
   - Poll playback state
   - Update progress bar
   - Update time display
   - Update album cover if track changes
3. Optional features:
   - Tap to expand (full screen player)
   - Swipe gestures
   - Album art animation

**Deliverable**: Current track display with progress

---

### 7.5 Error Handling UI
**Objective**: Show user-friendly error messages

**Steps**:
1. Create error dialog/Toast system:
   - Network errors: "Network error. Please check your connection."
   - Premium required: "Spotify Premium is required for playback control."
   - Device not found: "Please open Spotify app to control playback."
   - Rate limited: "Too many requests. Please wait a moment."
   - Generic API errors: Show error message from API
2. Implement error display:
   - Use AlertDialog for important errors
   - Use Snackbar for transient errors
   - Use Toast for minor notifications
3. Error recovery:
   - Retry button for network errors
   - Auto-retry with exponential backoff
   - Clear error state on successful operation

**Deliverable**: User-friendly error messages

---

## Phase 8: Background Operation

### 8.1 Background Service
**Objective**: Keep app functional in background

**Steps**:
1. Create `PlaybackService` (Foreground Service):
   - Extend `Service` or use WorkManager
   - Start as foreground service (show notification)
   - Handle playback state polling
   - Handle queue advancement
   - Continue when app is in background
2. Create notification:
   - Show current track info
   - Play/Pause button (notification action)
   - Next/Previous buttons (notification actions)
   - Tap to open app
3. Implement service lifecycle:
   - Start service when playback starts
   - Stop service when playback stops (optional)
   - Handle service restart (if killed by system)
4. Battery optimization:
   - Request to ignore battery optimization
   - Use efficient polling (duration-based)
   - Minimize wake locks

**Deliverable**: App works in background with notification

---

### 8.2 Background State Management
**Objective**: Maintain state when app is backgrounded

**Steps**:
1. Save state:
   - Current track
   - Queue state
   - Playback state (playing/paused)
   - Save to SharedPreferences or database
2. Restore state:
   - When app returns to foreground
   - Load saved state
   - Sync with Spotify playback state
   - Update UI
3. Handle app termination:
   - Save critical state on app close
   - Restore on next launch (if needed)
   - Queue doesn't need to persist (as per requirements)

**Deliverable**: App maintains state across background/foreground

---

## Phase 9: Testing & Integration

### 9.1 Minimal Viable Product (MVP) - Fast Integration Test
**Objective**: Quick test of Spotify integration

**Steps**:
1. **Phase 1 only**: Authentication
   - Test login flow
   - Verify token storage
   - Test token refresh
2. **Phase 3 only**: Fetch tracks
   - Fetch first 50 tracks (skip pagination for now)
   - Store in database
   - Display in simple list
3. **Phase 4.1 only**: Basic playback
   - Play button: Play first track
   - Pause button: Pause playback
   - Verify playback works
4. **Phase 4.2 only**: Basic polling
   - Poll every 5 seconds
   - Display current track
   - Detect track changes
5. Test end-to-end:
   - Login → Fetch tracks → Play track → Verify it plays in Spotify

**Deliverable**: Working MVP that can play tracks from Spotify

---

### 9.2 Incremental Feature Addition
**Objective**: Add features one by one, testing each

**Steps**:
1. After MVP works:
   - Add full track fetching (pagination)
   - Add queue system (simple sequential)
   - Add next/previous buttons
   - Add statistics tracking
   - Add shuffle logic
   - Add queue view
   - Add background service
2. Test each feature:
   - Unit tests for logic
   - Integration tests for API calls
   - Manual testing for UI
3. Fix bugs as they appear

**Deliverable**: Complete feature set working

---

### 9.3 Edge Case Testing
**Objective**: Test all edge cases and error scenarios

**Steps**:
1. Test scenarios:
   - No internet connection
   - Spotify app not installed
   - User doesn't have Premium
   - Token expires during playback
   - User controls Spotify directly
   - App killed by system
   - Very large track collection (1000+ tracks)
   - Empty track collection
   - All tracks played equally (shuffle edge case)
2. Test error handling:
   - All error types show correct messages
   - App recovers from errors
   - No crashes on invalid data
3. Performance testing:
   - Large database queries
   - UI responsiveness
   - Memory usage
   - Battery usage

**Deliverable**: Robust app handling all edge cases

---

## Phase 10: Polish & Optimization

### 10.1 Performance Optimization
**Objective**: Optimize app performance

**Steps**:
1. Database optimization:
   - Add missing indexes
   - Optimize queries
   - Batch operations
   - Use transactions
2. Network optimization:
   - Cache API responses where possible
   - Reduce polling frequency when possible
   - Batch API calls
3. UI optimization:
   - Lazy loading for lists
   - Image caching
   - Smooth animations
   - Reduce overdraw

**Deliverable**: Fast, responsive app

---

### 10.2 User Experience Improvements
**Objective**: Enhance UX

**Steps**:
1. Add loading indicators:
   - When fetching tracks
   - When playing track
   - When updating queue
2. Add animations:
   - Smooth transitions
   - Progress animations
   - Queue scrolling
3. Add feedback:
   - Haptic feedback on button presses
   - Visual feedback for actions
   - Toast messages for confirmations
4. Improve error messages:
   - More specific messages
   - Actionable suggestions
   - Retry options

**Deliverable**: Polished user experience

---

## Implementation Order Summary

### Quick Start (MVP - Test Integration):
1. Phase 1: Project Setup & Authentication
2. Phase 3.1: Fetch tracks (first 50 only, skip pagination)
3. Phase 4.1: Basic playback (play/pause)
4. Phase 4.2: Basic polling (every 5 seconds)

### Then Add Features:
5. Phase 2: Database (full implementation)
6. Phase 3: Full track fetching (pagination)
7. Phase 5: Queue system
8. Phase 6: Statistics
9. Phase 7: Full UI
10. Phase 8: Background operation
11. Phase 9: Testing
12. Phase 10: Polish

---

## Key Technical Decisions

1. **Database**: Use Room for type safety and easier migrations, or SQLiteOpenHelper for more control
2. **Architecture**: MVVM with LiveData/Flow for reactive UI updates
3. **Networking**: Retrofit for type-safe API calls
4. **Image Loading**: Glide for efficient image loading and caching
5. **Background**: Foreground Service for reliable background operation
6. **State Management**: ViewModel + Repository pattern
7. **Error Handling**: Centralized error handler with user-friendly messages

---

## API Rate Limits Considerations

- **Rate Limits**: Spotify API has rate limits (check current limits)
- **Polling**: Use smart polling (duration-based) to minimize calls
- **Batching**: Batch operations where possible
- **Caching**: Cache API responses to avoid redundant calls
- **Error Handling**: Handle 429 (rate limited) responses gracefully

---

## Notes

- Queue doesn't persist across app restarts (as per requirements)
- All shuffle logic is local (app-managed)
- App acts as remote control for Spotify
- Statistics are local only
- Premium account required for playback control
- App detects external Spotify control and pauses operation

---

## Success Criteria

1. ✅ User can login and stay logged in
2. ✅ All liked tracks are fetched and stored locally
3. ✅ User can browse tracks and see metadata
4. ✅ User can play specific tracks by selecting them
5. ✅ Queue shows 20 history + current + 30 upcoming
6. ✅ Playback controls work (play, pause, next, previous)
7. ✅ Shuffle works with custom logic (least played first)
8. ✅ Statistics are tracked (play count, timestamps)
9. ✅ App works in background
10. ✅ External Spotify control is detected and handled
11. ✅ Errors are shown in user-friendly popups

---

This plan provides detailed steps for each phase. Follow phases sequentially, starting with MVP for quick integration testing, then adding features incrementally.

