# New-graphic validation report

- Commit: `9515d8f0297c79096811714838d1b4bbc4b8b41c`
- Branch: `New-graphic`
- Static acceptance: `success`
- Compile + unit tests: `failure`
- Lint: `failure`
- Assemble release APK: `failure`

## Compile errors (first 60 matching lines)
```
> Task :hamyar-app:compileP09DebugKotlin FAILED
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/navigation/ZahraNavHost.kt:258:51 No value passed for parameter 'onBack'.
* What went wrong:
Execution failed for task ':hamyar-app:compileP09DebugKotlin' (registered by plugin 'com.android.internal.application').
org.gradle.api.tasks.TaskExecutionException: Execution failed for task ':hamyar-app:compileP09DebugKotlin' (registered by plugin 'com.android.internal.application').
BUILD FAILED in 1m 2s
```

## Lint errors (first 60 matching lines)
```
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/navigation/ZahraNavHost.kt:258:51 No value passed for parameter 'onBack'.
> Task :hamyar-app:compileP09DebugKotlin FAILED
* What went wrong:
Execution failed for task ':hamyar-app:compileP09DebugKotlin' (registered by plugin 'com.android.internal.application').
BUILD FAILED in 1m 5s
```

## Assemble errors (first 60 matching lines)
```
> Task :hamyar-app:checkP09ReleaseDuplicateClasses
> Task :hamyar-app:compileP09ReleaseKotlin FAILED
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/navigation/ZahraNavHost.kt:258:51 No value passed for parameter 'onBack'.
* What went wrong:
Execution failed for task ':hamyar-app:compileP09ReleaseKotlin' (registered by plugin 'com.android.internal.application').
BUILD FAILED in 40s
```

## Changed files
M	apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/components/LinedNotebook.kt
