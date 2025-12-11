# Spotify Player

Android native app that manages Spotify playback with custom queue logic, shuffle algorithm, and playback statistics.

## Project Setup

This project is set up according to Phase 1.1 of the implementation plan.

### Requirements

- Android Studio Hedgehog (2023.1.1) or later
- JDK 8 or higher
- Android SDK 24 (minimum) / 34 (target)
- Gradle 8.1+

### Project Structure

```
app/
├── build.gradle              # App-level build configuration
├── proguard-rules.pro        # ProGuard rules for code obfuscation
└── src/main/
    ├── AndroidManifest.xml   # App manifest with permissions and activities
    ├── java/com/spotifyplayer/app/
    │   └── MainActivity.kt   # Main activity
    └── res/
        ├── layout/           # Layout files
        ├── values/           # String resources
        └── xml/              # XML configurations (network security, backup rules)
```

### Dependencies

- **Retrofit + OkHttp**: API calls and networking
- **Gson**: JSON parsing
- **Room Database**: Local SQLite database
- **AndroidX Lifecycle**: ViewModel and LiveData
- **Coroutines**: Asynchronous operations
- **Security Crypto**: Encrypted SharedPreferences for token storage

### Configuration

1. **Network Security**: Configured in `app/src/main/res/xml/network_security_config.xml`
   - HTTPS required for Spotify API
   - Cleartext allowed only for localhost (development)

2. **OAuth Redirect**: Custom URL scheme `spotifyplayer://callback`
   - Configured in AndroidManifest.xml
   - No backend server needed

3. **ProGuard**: Rules configured in `app/proguard-rules.pro`
   - Protects API models and Retrofit interfaces
   - Removes logging in release builds

### Spotify Setup

Before you can use the app, you need to register it with Spotify:

1. **Register your app** in [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
2. **Get your credentials** (Client ID and Client Secret)
3. **Configure redirect URI**: `spotifyplayer://callback`
4. **Store credentials** in `app/src/main/res/values/spotify_config.xml`

📖 **Detailed instructions**: See [SPOTIFY_SETUP.md](SPOTIFY_SETUP.md)

### Next Steps

Follow the implementation plan:
- ✅ Phase 1.1: Project Setup & Authentication (Complete)
- ✅ Phase 1.2: Spotify App Registration (Complete)
- ✅ Phase 1.3: OAuth 2.0 Authentication Implementation (Complete)
- ✅ Phase 1.4: API Client Setup (Complete)
- 🔄 Phase 2: Database Schema & Data Models (Next)

### Testing

To test the authentication flow, see [TESTING.md](TESTING.md)

## Building

```bash
./gradlew build
```

## Running

Open the project in Android Studio and run on an emulator or device.

