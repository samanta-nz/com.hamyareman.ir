# New-graphic validation report

- Commit: `2657ee9ae715a688efebc422a1675b6c4c31f94c`
- Branch: `New-graphic`
- Static acceptance: `failure`
- Compile + unit tests: `success`
- Lint: `failure`
- Assemble release APK: `failure`

## Compile errors (first 60 matching lines)
```
```

## Lint errors (first 60 matching lines)
```
Lint found 1 error, 88 warnings and 16 hints. First failure:
/home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/components/LinedNotebook.kt:143: Error: BoxWithConstraints scope is not used [UnusedBoxWithConstraintsScope from androidx.compose.foundation]
> Task :hamyar-app:lintP09Debug FAILED
Lint found 1 error, 88 warnings, 16 hints. First failure:
/home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/components/LinedNotebook.kt:143: Error: BoxWithConstraints scope is not used [UnusedBoxWithConstraintsScope from androidx.compose.foundation]
* What went wrong:
Execution failed for task ':hamyar-app:lintP09Debug' (registered by plugin 'com.android.internal.application').
> Lint found errors in the project; aborting build.
  Lint found 1 error, 88 warnings, 16 hints. First failure:
  /home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/components/LinedNotebook.kt:143: Error: BoxWithConstraints scope is not used [UnusedBoxWithConstraintsScope from androidx.compose.foundation]
BUILD FAILED in 3m 52s
```

## Assemble errors (first 60 matching lines)
```
> Task :hamyar-app:checkP09ReleaseDuplicateClasses
> Task :hamyar-app:mergeP09ReleaseAssets FAILED
ERROR: [lock/app-lock-bg.jpg] /home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/assets/lock/app-lock-bg.jpg [lock/app-lock-bg.jpg] /home/runner/work/com.hamyareman.ir/com.hamyareman.ir/assets/lock/app-lock-bg.jpg: Resource and asset merger: Duplicate resources
ERROR: [lock/safespace-lock-bg.jpg] /home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/assets/lock/safespace-lock-bg.jpg [lock/safespace-lock-bg.jpg] /home/runner/work/com.hamyareman.ir/com.hamyareman.ir/assets/lock/safespace-lock-bg.jpg: Resource and asset merger: Duplicate resources
ERROR: [poetry/poetry-bg.jpg] /home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/assets/poetry/poetry-bg.jpg [poetry/poetry-bg.jpg] /home/runner/work/com.hamyareman.ir/com.hamyareman.ir/assets/poetry/poetry-bg.jpg: Resource and asset merger: Duplicate resources
ERROR: [album/album-bg.jpg] /home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/assets/album/album-bg.jpg [album/album-bg.jpg] /home/runner/work/com.hamyareman.ir/com.hamyareman.ir/assets/album/album-bg.jpg: Resource and asset merger: Duplicate resources
* What went wrong:
Execution failed for task ':hamyar-app:mergeP09ReleaseAssets' (registered by plugin 'com.android.internal.application').
> [lock/app-lock-bg.jpg] /home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/assets/lock/app-lock-bg.jpg	[lock/app-lock-bg.jpg] /home/runner/work/com.hamyareman.ir/com.hamyareman.ir/assets/lock/app-lock-bg.jpg: Error: Duplicate resources
  [lock/safespace-lock-bg.jpg] /home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/assets/lock/safespace-lock-bg.jpg	[lock/safespace-lock-bg.jpg] /home/runner/work/com.hamyareman.ir/com.hamyareman.ir/assets/lock/safespace-lock-bg.jpg: Error: Duplicate resources
  [poetry/poetry-bg.jpg] /home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/assets/poetry/poetry-bg.jpg	[poetry/poetry-bg.jpg] /home/runner/work/com.hamyareman.ir/com.hamyareman.ir/assets/poetry/poetry-bg.jpg: Error: Duplicate resources
  [album/album-bg.jpg] /home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/assets/album/album-bg.jpg	[album/album-bg.jpg] /home/runner/work/com.hamyareman.ir/com.hamyareman.ir/assets/album/album-bg.jpg: Error: Duplicate resources
BUILD FAILED in 23s
```

## Changed files
M	apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt
