# Spotify App Registration Guide

## Phase 1.2: Register Your App with Spotify

Follow these steps to register your app and obtain the necessary credentials.

### Step 1: Access Spotify Developer Dashboard

1. Go to [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
2. Log in with your Spotify account (or create one if you don't have one)

### Step 2: Create a New App

1. Click the **"Create app"** button
2. Fill in the app details:
   - **App name**: `Spotify Player` (or your preferred name)
   - **App description**: `Android app for managing Spotify playback with custom queue logic`
   - **Website**: (Optional - you can leave blank or use a placeholder)
   - **Redirect URI**: `spotifyplayer://callback`
     - **Important**: This is the custom URL scheme your app will use
     - No backend server needed - this is handled by Android's intent filters
   - **Which API/SDKs are you planning to use?**: Select **"Web API"**
3. Accept the terms and conditions
4. Click **"Save"**

### Step 3: Get Your Credentials

After creating the app, you'll see your app's dashboard. Here you'll find:

1. **Client ID**: 
   - Copy this value - you'll need it for authentication
   - It's visible on the app overview page

2. **Client Secret** (optional but recommended):
   - Click **"Show client secret"** to reveal it
   - Copy this value
   - **Note**: For PKCE flow (which we're using), Client Secret is optional, but it's good to have it

### Step 4: Configure Redirect URI

1. In your app's dashboard, go to **"Edit Settings"**
2. Under **"Redirect URIs"**, make sure you have:
   ```
   spotifyplayer://callback
   ```
3. Click **"Add"** if it's not already there
4. Click **"Save"**

### Step 5: Store Your Credentials

1. Open `app/src/main/res/values/spotify_config.xml` (create if it doesn't exist)
2. Add your credentials:
   ```xml
   <resources>
       <string name="spotify_client_id">YOUR_CLIENT_ID_HERE</string>
       <string name="spotify_client_secret">YOUR_CLIENT_SECRET_HERE</string>
       <string name="spotify_redirect_uri">spotifyplayer://callback</string>
   </resources>
   ```

**⚠️ Security Note**: 
- For production, consider using BuildConfig or environment variables
- Never commit credentials to version control
- The `spotify_config.xml` file should be in `.gitignore`

### Step 6: Verify Configuration

Your redirect URI `spotifyplayer://callback` should match:
- The redirect URI in Spotify Dashboard
- The intent filter in `AndroidManifest.xml` (already configured)

### Required Scopes

When implementing authentication (Phase 1.3), you'll need these scopes:
- `user-read-private`
- `user-read-email`
- `user-library-read`
- `user-modify-playback-state`
- `user-read-playback-state`
- `user-read-currently-playing`

### Troubleshooting

**Problem**: "Invalid redirect URI" error
- **Solution**: Make sure the redirect URI in Spotify Dashboard exactly matches `spotifyplayer://callback` (case-sensitive)

**Problem**: Can't find Client Secret
- **Solution**: Click "Show client secret" button in the dashboard. If you can't see it, you may need to regenerate it.

**Problem**: App not showing in dashboard
- **Solution**: Make sure you're logged in with the correct Spotify account and check the "My Apps" section.

### Next Steps

After completing this phase:
- ✅ You have Client ID and Client Secret
- ✅ Redirect URI is configured in Spotify Dashboard
- ✅ Credentials are stored in the app

Proceed to **Phase 1.3: OAuth 2.0 Authentication Implementation**



