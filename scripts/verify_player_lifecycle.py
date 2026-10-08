from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")

managed = read("apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/ManagedWebMedia.kt")
service = read("apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/HtmlAudioKeepAliveService.kt")
manifest = read("apps/hamyar-app/src/main/AndroidManifest.xml")
whisper = read("apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/calmdown/CalmWhispersScreen.kt")
vm = read("apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/calmdown/NatureWhisperPlayerViewModel.kt")
tile = read("apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/calmdown/BackgroundMusicTileHost.kt")
content = read("apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/content/ContentScreens.kt")

checks = [
    ("generic HTML lifecycle bridge", "HamyarMediaLifecycle" in managed),
    ("screen-lock detection", "power?.isInteractive != false" in managed),
    ("HTML audio keep-alive service", "startForeground(" in service),
    ("CPU wake lock", "PowerManager.PARTIAL_WAKE_LOCK" in service),
    ("service media type", 'android:foregroundServiceType="mediaPlayback"' in manifest),
    ("nature full player", 'background-music-full.html' in vm),
    ("nature Activity-level retention", "NatureWhisperPlayerViewModel" in whisper and "ViewModelProvider(activity)" in whisper),
    ("nature navigation dialog", "AlertDialog" in whisper and "ادامه پخش" in whisper and "قطع پخش" in whisper),
    ("nature timer survives route", "viewModelScope.launch" in vm and "secondsLeft.intValue" in vm),
    ("HTML keepAlive contract", 'addJavascriptInterface(KeepAliveBridge' in vm and '"HamyarHost"' in vm),
    ("Web Audio monitoring on music tile", "watchWebAudio = true" in tile),
    ("content route cleanup", "ManagedWebMediaEffect" in content and "stopManagedMedia()" in content),
]

errors = [name for name, ok in checks if not ok]
if errors:
    raise SystemExit("Player lifecycle verification failed:\n- " + "\n- ".join(errors))

# Guard against the old destructive route cleanup returning.
if "onDispose" in whisper and "stopManagedMedia" in whisper:
    raise SystemExit("CalmWhispersScreen still performs destructive onDispose media cleanup")

print("PLAYER LIFECYCLE CONTRACTS: PASS")
for name, _ in checks:
    print("PASS:", name)
