# New-graphic validation report

- Commit: `e51a78feb4c770c23198a123c4227786b2dca680`
- Branch: `New-graphic`
- Static acceptance: `success`
- Compile + unit tests: `failure`
- Lint: `failure`
- Assemble release APK: `failure`

## Compile errors (first 60 matching lines)
```
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/navigation/ZahraNavHost.kt:71:39 Unresolved reference 'NotebooksScreen'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/navigation/ZahraNavHost.kt:315:22 Unresolved reference 'NotebooksScreen'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:259:13 Argument type mismatch: actual type is 'List<Any>', but 'List<DiaryPageModel>' was expected.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:603:33 Unresolved reference 'DiaryTouchPlacement'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:611:53 Cannot infer type for value parameter 'panX'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:611:59 Cannot infer type for value parameter 'panY'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:611:65 Cannot infer type for value parameter 'zoom'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:611:71 Cannot infer type for value parameter 'turn'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:611:77 Cannot infer type for value parameter 'w'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:611:80 Cannot infer type for value parameter 'h'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:688:13 'fun item(key: Any? = ..., contentType: Any? = ..., content: ComposableFunction1<LazyItemScope, Unit>): Unit' cannot be called in this context with an implicit receiver. Use an explicit receiver if necessary.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:690:17 'fun item(key: Any? = ..., contentType: Any? = ..., content: ComposableFunction1<LazyItemScope, Unit>): Unit' cannot be called in this context with an implicit receiver. Use an explicit receiver if necessary.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:692:13 'fun <T> LazyListScope.items(items: List<T>, noinline key: ((T) -> Any)? = ..., noinline contentType: (T) -> Any? = ..., crossinline itemContent: ComposableFunction2<LazyItemScope, T, Unit>): Unit' cannot be called in this context with an implicit receiver. Use an explicit receiver if necessary.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:742:9 Unresolved reference 'DiaryBookViewer'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:746:24 Cannot infer type for value parameter 'page'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:747:35 Unresolved reference 'entry'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:747:47 Unresolved reference 'pageIndex'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:756:1 Modifier 'private' is not applicable to 'local function'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:799:29 Unresolved reference 'DiaryRenderedPage'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:826:1 Modifier 'private' is not applicable to 'local function'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:878:1 Modifier 'private' is not applicable to 'local function'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:908:21 Unresolved reference 'DiaryImageSlot'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:912:21 Unresolved reference 'DiaryImageSlot'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:920:1 Modifier 'private' is not applicable to 'local function'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:1159:2 Syntax error: Expecting '}'.
> Task :hamyar-app:compileP09DebugKotlin FAILED
* What went wrong:
Execution failed for task ':hamyar-app:compileP09DebugKotlin' (registered by plugin 'com.android.internal.application').
org.gradle.api.tasks.TaskExecutionException: Execution failed for task ':hamyar-app:compileP09DebugKotlin' (registered by plugin 'com.android.internal.application').
BUILD FAILED in 41s
```

## Lint errors (first 60 matching lines)
```
> Task :hamyar-app:compileP09DebugKotlin FAILED
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/navigation/ZahraNavHost.kt:71:39 Unresolved reference 'NotebooksScreen'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/navigation/ZahraNavHost.kt:315:22 Unresolved reference 'NotebooksScreen'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:259:13 Argument type mismatch: actual type is 'List<Any>', but 'List<DiaryPageModel>' was expected.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:603:33 Unresolved reference 'DiaryTouchPlacement'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:611:53 Cannot infer type for value parameter 'panX'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:611:59 Cannot infer type for value parameter 'panY'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:611:65 Cannot infer type for value parameter 'zoom'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:611:71 Cannot infer type for value parameter 'turn'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:611:77 Cannot infer type for value parameter 'w'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:611:80 Cannot infer type for value parameter 'h'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:688:13 'fun item(key: Any? = ..., contentType: Any? = ..., content: ComposableFunction1<LazyItemScope, Unit>): Unit' cannot be called in this context with an implicit receiver. Use an explicit receiver if necessary.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:690:17 'fun item(key: Any? = ..., contentType: Any? = ..., content: ComposableFunction1<LazyItemScope, Unit>): Unit' cannot be called in this context with an implicit receiver. Use an explicit receiver if necessary.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:692:13 'fun <T> LazyListScope.items(items: List<T>, noinline key: ((T) -> Any)? = ..., noinline contentType: (T) -> Any? = ..., crossinline itemContent: ComposableFunction2<LazyItemScope, T, Unit>): Unit' cannot be called in this context with an implicit receiver. Use an explicit receiver if necessary.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:742:9 Unresolved reference 'DiaryBookViewer'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:746:24 Cannot infer type for value parameter 'page'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:747:35 Unresolved reference 'entry'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:747:47 Unresolved reference 'pageIndex'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:756:1 Modifier 'private' is not applicable to 'local function'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:799:29 Unresolved reference 'DiaryRenderedPage'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:826:1 Modifier 'private' is not applicable to 'local function'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:878:1 Modifier 'private' is not applicable to 'local function'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:908:21 Unresolved reference 'DiaryImageSlot'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:912:21 Unresolved reference 'DiaryImageSlot'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:920:1 Modifier 'private' is not applicable to 'local function'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:1159:2 Syntax error: Expecting '}'.
* What went wrong:
Execution failed for task ':hamyar-app:compileP09DebugKotlin' (registered by plugin 'com.android.internal.application').
BUILD FAILED in 46s
```

## Assemble errors (first 60 matching lines)
```
> Task :hamyar-app:checkP09ReleaseDuplicateClasses
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/navigation/ZahraNavHost.kt:71:39 Unresolved reference 'NotebooksScreen'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/navigation/ZahraNavHost.kt:315:22 Unresolved reference 'NotebooksScreen'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:259:13 Argument type mismatch: actual type is 'List<Any>', but 'List<DiaryPageModel>' was expected.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:603:33 Unresolved reference 'DiaryTouchPlacement'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:611:53 Cannot infer type for value parameter 'panX'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:611:59 Cannot infer type for value parameter 'panY'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:611:65 Cannot infer type for value parameter 'zoom'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:611:71 Cannot infer type for value parameter 'turn'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:611:77 Cannot infer type for value parameter 'w'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:611:80 Cannot infer type for value parameter 'h'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:688:13 'fun item(key: Any? = ..., contentType: Any? = ..., content: ComposableFunction1<LazyItemScope, Unit>): Unit' cannot be called in this context with an implicit receiver. Use an explicit receiver if necessary.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:690:17 'fun item(key: Any? = ..., contentType: Any? = ..., content: ComposableFunction1<LazyItemScope, Unit>): Unit' cannot be called in this context with an implicit receiver. Use an explicit receiver if necessary.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:692:13 'fun <T> LazyListScope.items(items: List<T>, noinline key: ((T) -> Any)? = ..., noinline contentType: (T) -> Any? = ..., crossinline itemContent: ComposableFunction2<LazyItemScope, T, Unit>): Unit' cannot be called in this context with an implicit receiver. Use an explicit receiver if necessary.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:742:9 Unresolved reference 'DiaryBookViewer'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:746:24 Cannot infer type for value parameter 'page'. Specify it explicitly.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:747:35 Unresolved reference 'entry'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:747:47 Unresolved reference 'pageIndex'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:756:1 Modifier 'private' is not applicable to 'local function'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:799:29 Unresolved reference 'DiaryRenderedPage'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:826:1 Modifier 'private' is not applicable to 'local function'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:878:1 Modifier 'private' is not applicable to 'local function'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:908:21 Unresolved reference 'DiaryImageSlot'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:912:21 Unresolved reference 'DiaryImageSlot'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:920:1 Modifier 'private' is not applicable to 'local function'.
e: file:///home/runner/work/com.hamyareman.ir/com.hamyareman.ir/apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:1159:2 Syntax error: Expecting '}'.
> Task :hamyar-app:compileP09ReleaseKotlin FAILED
* What went wrong:
Execution failed for task ':hamyar-app:compileP09ReleaseKotlin' (registered by plugin 'com.android.internal.application').
BUILD FAILED in 32s
```

## Changed files
M	.release-trigger/grade9-v2.5.7
