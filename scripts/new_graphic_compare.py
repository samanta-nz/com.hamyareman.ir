#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from dataclasses import dataclass
from pathlib import Path
from datetime import datetime, timezone
from typing import Iterable

ROOT = Path(__file__).resolve().parents[1]
REPORT = ROOT / 'new-graphic-session' / 'COMPARISON.md'

@dataclass
class Check:
    key: str
    title: str
    status: str
    evidence: str

def read(path: str) -> str:
    p = ROOT / path
    return p.read_text(encoding='utf-8') if p.exists() else ''

def exists(path: str) -> bool:
    return (ROOT / path).exists()

def count(s: str, needle: str) -> int:
    return s.count(needle)

def git_safe(*args: str) -> str:
    try:
        return subprocess.check_output(['git', *args], cwd=ROOT, text=True).strip()
    except Exception:
        return ''

def branch_sync():
    main_sha = git_safe('rev-parse', 'refs/remotes/origin/main') or git_safe('rev-parse', 'main')
    head_sha = git_safe('rev-parse', 'HEAD')
    counts = git_safe('rev-list', '--left-right', '--count', f'{main_sha}...{head_sha}') if main_sha and head_sha else ''
    return main_sha, head_sha, counts

def changed_paths():
    main_sha, head_sha, _ = branch_sync()
    if not main_sha or not head_sha:
        return []
    out = git_safe('diff', '--name-only', f'{main_sha}...{head_sha}')
    return [x for x in out.splitlines() if x.strip()]

def requirement_checks():
    lock = read('shared/core-designsystem/src/main/java/com/hamyareman/ir/platform/core/designsystem/PinLockGate.kt')
    safe = read('apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/SafeSpaceScreens.kt')
    notebook = read('apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/components/LinedNotebook.kt')
    book = read('apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/components/BookVisualEngine.kt')
    diary = read('apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt')
    poetry = read('apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/PoetryBookScreen.kt')
    album = read('apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/SecureMediaGallery.kt')
    player = read('apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/PremiumMediaPlayer.kt')
    sleep = read('apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/SleepNightScreen.kt')
    tile = read('apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/calmdown/BackgroundMusicTileHost.kt')
    free_reading = read('apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/FreeReadingScreen.kt')
    theme = read('shared/core-designsystem/src/main/java/com/hamyareman/ir/platform/core/designsystem/Theme.kt')
    components = read('shared/core-designsystem/src/main/java/com/hamyareman/ir/platform/core/designsystem/Components.kt')
    prefs = read('apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/appearance/UiPrefs.kt')
    assets = read('apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/components/DesignAssets.kt')
    home = read('apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/home/HomeScreen.kt')

    checks = []
    checks.append(Check('lock-app','App Lock: الگوی واقعی 3×3 مستقل','PASS' if all(x in lock for x in ['PatternLockGrid','LockVariant.App','onPatternComplete','PatternFeedback.Success']) else 'MISSING','PinLockGate.kt'))
    checks.append(Check('lock-safe','Safe Space Lock: همان engine با variant مستقل','PASS' if all(x in safe for x in ['LockVariant.SafeSpace','LockBackdropLayer','SafeSpaceSession']) else 'MISSING','SafeSpaceScreens.kt'))
    checks.append(Check('notebook-shared','Notebook engine مشترک + بدون vertical scrolling','PASS' if all(x in notebook for x in ['RealisticBookPage','HorizontalPager','NOTEBOOK_PAGE_SEPARATOR']) and 'verticalScroll' not in notebook else 'PARTIAL',f'LinedNotebook.kt؛ verticalScroll={count(notebook,"verticalScroll")}'))
    checks.append(Check('book-depth','واقع‌گرایی کتاب: stack / perspective / shadow','PASS' if all(x in book for x in ['stackDepth','rotationY','shadowElevation','cameraDistance']) else 'PARTIAL','BookVisualEngine.kt'))
    checks.append(Check('diary','Diary: frame + pager + image placement/wrap','PASS' if all(x in diary for x in ['RealisticDeskFrame','RealisticBookPager','DiaryTouchPlacement','ImageWrap']) else 'PARTIAL','DiaryScreens.kt'))
    checks.append(Check('poetry','Poetry: shared pager + no vertical guides + hemistich editor','PASS' if all(x in poetry for x in ['RealisticDeskFrame','RealisticBookPager','showVerticalGuides = false','PoetryHemistichEditor']) else 'PARTIAL','PoetryBookScreen.kt'))
    checks.append(Check('album','Album: adaptive grid + secure media actions','PASS' if all(x in album for x in ['RealisticDeskFrame','GridCells.Adaptive','shareSecureMedia']) else 'PARTIAL','SecureMediaGallery.kt'))
    checks.append(Check('player','Media player واقعی با کنترل‌های اصلی','PASS' if all(x in player for x in ['PremiumMediaPlayer','seek','speed']) else 'PARTIAL','PremiumMediaPlayer.kt / gallery'))
    checks.append(Check('sleep','بشنو و بخواب: tile in-flow و بدون resize animation میزبان','PASS' if 'BackgroundMusicTileHost' in sleep and 'Spacer(Modifier.height(92.dp))' in sleep and 'animateDpAsState' not in tile and 'tween(340)' not in tile else 'PARTIAL','SleepNightScreen.kt + BackgroundMusicTileHost.kt'))
    checks.append(Check('free-reading','کتاب متنی/صوتی با مسیر واقعی خواندن/پخش','PASS' if all(x in free_reading for x in ['FreeAudioReader','FreeHtmlReader','PlaybackController']) else 'PARTIAL','FreeReadingScreen.kt'))
    checks.append(Check('theme-global','تم سراسری: HamyarNavy + PlatformTheme + shared controls','PASS' if all(x in theme for x in ['HamyarNavy','PlatformTheme']) and all(x in components for x in ['MaterialTheme.colorScheme.primary','TopAppBarDefaults.topAppBarColors']) and 'BrandTheme.HamyarNavy' in prefs else 'PARTIAL','Theme.kt / Components.kt / UiPrefs.kt'))
    checks.append(Check('theme-colors','کامپوننت‌های گرافیکی از scheme فعال استفاده می‌کنند','PASS' if count(read('apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/components/RealisticUi.kt'),'MaterialTheme.colorScheme') >= 3 and count(book,'MaterialTheme.colorScheme') >= 2 and count(album,'MaterialTheme.colorScheme') >= 3 else 'PARTIAL','RealisticUi.kt / BookVisualEngine.kt / SecureMediaGallery.kt'))
    checks.append(Check('remote-assets','remote + APK fallback برای graphic assets','PASS' if all(x in assets for x in ['c539776.parspack.net','removePrefix("assets/")','readable(remote)']) else 'MISSING','DesignAssets.kt'))
    required = [
        'assets/diary/cover-leather-brown.png',
        'assets/diary/cover-leather-brown-inside.png',
        'assets/diary/cover-leather-brown-sheet.png',
        'assets/diary/cover-navy-floral.png',
        'assets/diary/cover-navy-floral-inside.png',
        'assets/diary/cover-navy-floral-sheet.png',
        'assets/lock/app-lock-bg.jpg',
        'assets/lock/safespace-lock-bg.jpg',
        'assets/poetry/poetry-bg.jpg',
        'assets/album/album-bg.jpg',
    ]
    missing = [p for p in required if not exists(p)]
    checks.append(Check('assets','assets واقعی مورد اشاره در مشخصات موجودند','PASS' if not missing else 'PARTIAL','missing: ' + ', '.join(missing) if missing else 'root assets present'))
    jpg_mappings = [x for x in ['cover-navy-floral.jpg','cover-leather-brown.jpg'] if x in assets]
    checks.append(Check('asset-png-wiring','DesignAssets دقیقاً به PNGهای مرجع وصل است','PARTIAL' if jpg_mappings else 'PASS','کلیدهای جلد اصلی در DesignAssets هنوز JPG هستند؛ PNGهای مرجع در root assets موجودند.'))
    checks.append(Check('home','داشبورد پویا + motion','PASS' if all(x in home for x in ['GreetingBanner','rememberInfiniteTransition','WisdomCard','QuickTile']) else 'PARTIAL','HomeScreen.kt'))
    return checks

def render(chks):
    main_sha, head_sha, counts = branch_sync()
    changed = changed_paths()
    passed = sum(x.status == 'PASS' for x in chks)
    partial = sum(x.status == 'PARTIAL' for x in chks)
    missing = sum(x.status == 'MISSING' for x in chks)
    lines = [
        '# New-graphic — Requirements Comparison',
        '',
        f'- Generated: {datetime.now(timezone.utc).isoformat()}',
        f'- Branch: New-graphic',
        f'- HEAD: {head_sha}',
        f'- main: {main_sha}',
        f'- rev-list main...HEAD: {counts}',
        '',
        '## Summary',
        '',
        f'- PASS: **{passed}**',
        f'- PARTIAL: **{partial}**',
        f'- MISSING: **{missing}**',
        '',
        '## Requirement audit',
        '',
        '| Status | Requirement | Evidence |',
        '|---|---|---|',
    ]
    for c in chks:
        evidence = c.evidence.replace('|','\\|')
        lines.append(f'| {c.status} | {c.title} | {evidence} |')
    lines += ['', '## Unmet / partial requirements requiring follow-up', '']
    for c in chks:
        if c.status != 'PASS':
            lines.append(f'- **{c.status}** — {c.title}: {c.evidence}')
    lines += ['', '## Changed paths in branch delta', '']
    lines.extend(f'- `{p}`' for p in changed)
    if not changed:
        lines.append('- No branch-specific paths detected against main.')
    lines += ['', '## Scope guard', '', 'این گزارش فقط روی New-graphic تولید می‌شود و هیچ push یا merge به main انجام نمی‌دهد.', '']
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text('\\n'.join(lines) + '\\n', encoding='utf-8')
    print('\\n'.join(lines))

if __name__ == '__main__':
    render(requirement_checks())
