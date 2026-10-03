# Verification targets

Expected source ref: `19ad0d7f3c9c482616ce3beee512859997028bc5`

Expected base APK from release report:
- versionName: `2.4.4`
- versionCode: `244`
- size: `72,110,272` bytes
- SHA-256: `bdf9b72c2a8bd5258768e7088453a4c10062d50c6f125b8e4034fe8ae5d8aa8f`

Expected hotfix checks:

1. `MusicFrameOverlayBridge.kt` exists and uses a **capture-phase** `message` listener (`..., true`) and does not use `position:fixed` / `100dvh` for lesson iframe expansion.
2. `HmkWebViewClient.kt` still installs the bridge on `onPageFinished`.
3. `ContentScreens.kt` still has the Compose BackHandler/bridge registration for closing the music popup.
4. `BackgroundMusicTileHost.kt` has no `animateDpAsState` or `tween(340)` for WebView height.
5. The sleep tile WebView injects the safe style that converts its nested music iframe from fixed to local absolute flow.
6. No `Bucket/Html-files/background-music*.html` file is modified by the hotfix.
