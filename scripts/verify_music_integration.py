#!/usr/bin/env python3
"""Static guardrails for the 2.4.8 music/iframe fix."""
from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def need(path: str, needle: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    if needle not in text:
        raise SystemExit(f"FAIL: {path}: missing {needle!r}")


def forbid(path: str, needle: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    if needle in text:
        raise SystemExit(f"FAIL: {path}: forbidden {needle!r}")


def main() -> int:
    bridge = "apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/MusicFrameOverlayBridge.kt"
    tile = "apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/calmdown/BackgroundMusicTileHost.kt"
    sleep = "apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/SleepNightScreen.kt"
    player = "Bucket/Html-files/background-music.html"

    need(bridge, "var activeFrame=null;")
    need(bridge, "HamyarMusicOverlay.onChanged")
    forbid(bridge, "position:fixed")
    forbid(bridge, "100dvh")
    forbid(bridge, "data-hamyar-music-open")

    need(tile, 'MUSIC_TILE_KEY = "Bucket/Html-files/background-music-tile.html"')
    forbid(tile, "#backgroundMusicFrame")
    forbid(tile, "body.is-open{overflow:visible!important}")

    need(sleep, "Spacer(Modifier.height(92.dp))")
    need(sleep, ".align(Alignment.TopCenter)")
    need(sleep, ".zIndex(20f)")
    need(sleep, "BackgroundMusicTileHost(")

    # The player is the single shared, self-contained source.
    p = (ROOT / player).read_text(encoding="utf-8")
    tracks = p.count('"audio":"')
    if tracks != 29:
        raise SystemExit(f"FAIL: {player}: expected 29 embedded tracks, got {tracks}")
    if "HamyaremanBackground" not in p:
        raise SystemExit(f"FAIL: {player}: shared player API missing")

    # The two diagnostic samples record the actual lesson invocation and prevent
    # accidentally regressing back to the nonexistent legacy filename.
    for sample in (
        "ci-report/lesson-yoga-stripped.html",
        "ci-report/lesson-sport-stripped.html",
    ):
        need(sample, "src:'./background-music.html'")

    print("OK: music iframe + sleep tile guardrails")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
