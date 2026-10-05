# New-graphic validation report

- Commit: `a24f0163c71dc6e4c6f6b67369aba0ac9c2df4ff`
- Branch: `New-graphic`
- Static acceptance: `success`
- Compile + unit tests: `cancelled`
- Lint: `skipped`
- Assemble release APK: `skipped`

## Compile errors (first 60 matching lines)
```
> Task :hamyar-app:compileP09DebugKotlin FAILED
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/navigation/ZahraNavHost.kt:71:39 Unresolved reference 'NotebooksScreen'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/navigation/ZahraNavHost.kt:315:22 Unresolved reference 'NotebooksScreen'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:286:13 Argument type mismatch: actual type is 'List<Any>', but 'List<DiaryPageModel>' was expected.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:606:33 Unresolved reference 'DiaryTouchPlacement'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:614:53 Cannot infer type for value parameter 'panX'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:614:59 Cannot infer type for value parameter 'panY'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:614:65 Cannot infer type for value parameter 'zoom'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:614:71 Cannot infer type for value parameter 'turn'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:614:77 Cannot infer type for value parameter 'w'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:614:80 Cannot infer type for value parameter 'h'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:691:13 'fun item(key: Any? = ..., contentType: Any? = ..., content: ComposableFunction1<LazyItemScope, Unit>): Unit' cannot be called in this context with an implicit receiver. Use an explicit receiver if necessary.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:693:17 'fun item(key: Any? = ..., contentType: Any? = ..., content: ComposableFunction1<LazyItemScope, Unit>): Unit' cannot be called in this context with an implicit receiver. Use an explicit receiver if necessary.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:695:13 'fun <T> LazyListScope.items(items: List<T>, noinline key: ((T) -> Any)? = ..., noinline contentType: (T) -> Any? = ..., crossinline itemContent: ComposableFunction2<LazyItemScope, T, Unit>): Unit' cannot be called in this context with an implicit receiver. Use an explicit receiver if necessary.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:745:9 Unresolved reference 'DiaryBookViewer'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:749:24 Cannot infer type for value parameter 'page'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:750:35 Unresolved reference 'entry'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:750:47 Unresolved reference 'pageIndex'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:759:1 Modifier 'private' is not applicable to 'local function'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:802:29 Unresolved reference 'DiaryRenderedPage'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:829:1 Modifier 'private' is not applicable to 'local function'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:881:1 Modifier 'private' is not applicable to 'local function'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:911:21 Unresolved reference 'DiaryImageSlot'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:915:21 Unresolved reference 'DiaryImageSlot'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:923:1 Modifier 'private' is not applicable to 'local function'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:1162:2 Syntax error: Expecting '}'.
* What went wrong:
Execution failed for task ':hamyar-app:compileP09DebugKotlin' (registered by plugin 'com.android.internal.application').
org.gradle.api.tasks.TaskExecutionException: Execution failed for task ':hamyar-app:compileP09DebugKotlin' (registered by plugin 'com.android.internal.application').
BUILD FAILED in 1m 17s
```

## Changed files
A	.github/workflows/grade9-release-v2.5.7.yml
