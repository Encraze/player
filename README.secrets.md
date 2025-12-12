# Spotify credentials (local-only)

Add these to `local.properties` (do not commit secrets):

```
spotify.clientId=YOUR_SPOTIFY_CLIENT_ID
spotify.redirectScheme=spotifyplayer
spotify.redirectHost=callback
```

- `spotify.clientId`: from Spotify Developer Dashboard.
- `spotify.redirectScheme`/`spotify.redirectHost`: must match the redirect URI registered in the dashboard and the manifest intent filter. Effective URI: `spotifyplayer://callback` (change if you alter scheme/host).

These values are read in `app/build.gradle` and exposed as:
- `BuildConfig.SPOTIFY_CLIENT_ID`
- `BuildConfig.SPOTIFY_REDIRECT_URI`

