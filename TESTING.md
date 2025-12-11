# Testing Guide

## Phase 1.3: Testing Authentication Flow

### Prerequisites

1. ✅ Spotify app registered in Developer Dashboard
2. ✅ Client ID and Client Secret added to `spotify_config.xml`
3. ✅ Redirect URI configured: `spotifyplayer://callback`
4. ✅ Android device or emulator ready

### Testing Steps

#### 1. Build and Install the App

```bash
./gradlew assembleDebug
```

Or use Android Studio: Build → Build Bundle(s) / APK(s) → Build APK(s)

#### 2. Launch the App

The app should open to the Login screen showing:
- App logo
- "Spotify Player" title
- "Manage your Spotify playback..." subtitle
- "Login with Spotify" button

#### 3. Test Login Flow

1. **Tap "Login with Spotify"**
   - Browser should open with Spotify login page
   - URL should start with `https://accounts.spotify.com/authorize`

2. **Login to Spotify**
   - Enter your Spotify credentials
   - If already logged in, you'll see the authorization screen

3. **Grant Permissions**
   - Review the requested permissions:
     - Read your email address
     - Access your private information
     - Read your library
     - Control playback
     - Read playback state
     - Read currently playing
   - Click "Agree" or "Accept"

4. **Redirect Back to App**
   - Browser should redirect to `spotifyplayer://callback`
   - App should automatically open
   - You should see a "Login successful!" toast message
   - App should navigate to Main screen

5. **Verify Main Screen**
   - Should show "Logged in successfully!"
   - Should display partial access token (first 20 characters)
   - Should show "Logout" button

#### 4. Test Token Storage

1. **Close the app completely** (swipe away from recent apps)
2. **Reopen the app**
3. **Expected**: App should go directly to Main screen (not Login screen)
4. **Verify**: Access token should still be displayed

This confirms that tokens are stored securely in EncryptedSharedPreferences.

#### 5. Test Logout

1. **Tap "Logout" button**
2. **Expected**: App should navigate back to Login screen
3. **Verify**: Tokens are cleared from storage

#### 6. Test Re-login

1. **Tap "Login with Spotify" again**
2. **Expected**: 
   - If still logged in to Spotify in browser, should skip login
   - Should go directly to authorization screen
   - Should redirect back to app successfully

### Common Issues and Solutions

#### Issue: "Invalid redirect URI"

**Cause**: Redirect URI mismatch between app and Spotify Dashboard

**Solution**:
1. Check `spotify_config.xml`: Should be `spotifyplayer://callback`
2. Check Spotify Dashboard: Should have exact same URI
3. Check AndroidManifest.xml: Intent filter should match

#### Issue: "Authorization failed: access_denied"

**Cause**: User denied permissions or Spotify account issue

**Solution**:
1. Try logging in again
2. Make sure you click "Agree" on authorization screen
3. Check if Spotify account is active

#### Issue: App doesn't open after redirect

**Cause**: Intent filter not configured correctly

**Solution**:
1. Check AndroidManifest.xml has correct intent filter
2. Verify scheme is `spotifyplayer` and host is `callback`
3. Rebuild and reinstall the app

#### Issue: "Token exchange failed"

**Cause**: Network error or invalid credentials

**Solution**:
1. Check internet connection
2. Verify Client ID and Client Secret in `spotify_config.xml`
3. Check logcat for detailed error messages

#### Issue: App crashes on launch

**Cause**: Missing dependencies or configuration

**Solution**:
1. Sync Gradle files in Android Studio
2. Clean and rebuild: Build → Clean Project, then Build → Rebuild Project
3. Check logcat for stack trace

### Viewing Logs

To see detailed authentication logs:

```bash
adb logcat | grep -E "SpotifyAuth|LoginActivity|TokenStorage"
```

Or in Android Studio: View → Tool Windows → Logcat

Filter by:
- `SpotifyAuthManager`
- `LoginActivity`
- `TokenStorage`

### Expected Log Output

Successful login should show:

```
D/SpotifyAuthManager: Building authorization URL
D/LoginActivity: Starting authorization
D/LoginActivity: Received authorization code
D/SpotifyAuthManager: Exchanging code for token
D/SpotifyAuthManager: Successfully obtained access token
D/LoginActivity: Login successful
```

### What's Working

After Phase 1.3, you should have:
- ✅ OAuth 2.0 authentication with PKCE
- ✅ Secure token storage (EncryptedSharedPreferences)
- ✅ Persistent login (tokens survive app restart)
- ✅ Logout functionality
- ✅ Token refresh infrastructure (will be used in Phase 1.4)

### Next Steps

Once authentication is working:
- Proceed to **Phase 1.4: API Client Setup**
- Implement Spotify API calls using stored tokens
- Test API calls with real Spotify data

### Security Notes

- Tokens are stored in EncryptedSharedPreferences (secure)
- PKCE flow is used (no client secret sent to app)
- Tokens automatically refresh when expired
- Logout clears all stored tokens

### Troubleshooting Checklist

- [ ] Client ID is correct in `spotify_config.xml`
- [ ] Redirect URI matches in Spotify Dashboard and app
- [ ] Internet connection is working
- [ ] Spotify account is active and has Premium (for playback control)
- [ ] App has internet permission in AndroidManifest.xml
- [ ] Intent filter is configured correctly
- [ ] Latest code is built and installed

If all checks pass and it still doesn't work, check logcat for detailed error messages.

