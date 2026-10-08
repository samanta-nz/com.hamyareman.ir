package com.hamyareman.ir.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry
import com.hamyareman.ir.ui.ailearning.AiAssessmentScreen
import com.hamyareman.ir.ui.ailearning.AiLearningHomeScreen
import com.hamyareman.ir.ui.appearance.AppearanceScreen
import com.hamyareman.ir.ui.art.ArtGalleryScreen
import com.hamyareman.ir.ui.art.DailyArtPromptScreen
import com.hamyareman.ir.ui.calmdown.BackgroundMusicScreen
import com.hamyareman.ir.ui.calmdown.BreathingScreen
import com.hamyareman.ir.ui.calmdown.CalmMenuScreen
import com.hamyareman.ir.ui.calmdown.JournalScreen
import com.hamyareman.ir.ui.calmdown.GratitudeJournalScreen
import com.hamyareman.ir.ui.content.ContentCategoryScreen
import com.hamyareman.ir.ui.content.ContentHubScreen
import com.hamyareman.ir.ui.content.ContentHtmlScreen
import com.hamyareman.ir.ui.cycle.MindfulnessScreen
import com.hamyareman.ir.ui.cycle.MoodCheckInScreen
import com.hamyareman.ir.ui.exercise.ExerciseDetailScreen
import com.hamyareman.ir.ui.exercise.ExerciseScreen
import com.hamyareman.ir.ui.gamification.BadgesScreen
import com.hamyareman.ir.ui.home.HomeScreen
import com.hamyareman.ir.ui.hub.AwarenessHubScreen
import com.hamyareman.ir.ui.hub.layerTo
import com.hamyareman.ir.ui.hub.HealthHubScreen
import com.hamyareman.ir.ui.hub.MedsScreen
import com.hamyareman.ir.ui.hub.ReadingCornerScreen
import com.hamyareman.ir.ui.hub.SchoolHubScreen
import com.hamyareman.ir.ui.hub.SleepLogScreen
import com.hamyareman.ir.ui.hub.WeeklyScheduleScreen
import com.hamyareman.ir.ui.study.ClassPlanScreen
import com.hamyareman.ir.ui.study.TomorrowPrepScreen
import com.hamyareman.ir.ui.learning.LearningHomeScreen
import com.hamyareman.ir.ui.learning.LessonScreen
import com.hamyareman.ir.ui.learning.PlacementTestScreen
import com.hamyareman.ir.ui.learning.RoadmapScreen
import com.hamyareman.ir.ui.more.MoreScreen
import com.hamyareman.ir.ui.more.AboutScreen
import com.hamyareman.ir.ui.more.ContactScreen
import com.hamyareman.ir.ui.recipes.RecipeDetailScreen
import com.hamyareman.ir.ui.recipes.RecipesScreen
import com.hamyareman.ir.ui.routine.RoutineScreen
import com.hamyareman.ir.ui.safespace.DiaryScreen
import com.hamyareman.ir.ui.safespace.HelplinesScreen
import com.hamyareman.ir.ui.safespace.NotebooksScreen
import com.hamyareman.ir.ui.safespace.PoetryBookScreen
import com.hamyareman.ir.ui.safespace.SafeContentGuard
import com.hamyareman.ir.ui.safespace.SafeSpaceScreen
import com.hamyareman.ir.ui.safespace.SafeFreeWritingScreen
import com.hamyareman.ir.ui.study.SecureMediaGalleryScreen
import com.hamyareman.ir.ui.safespace.WritingPromptScreen
import com.hamyareman.ir.ui.settings.AppLockScreen
import com.hamyareman.ir.ui.settings.PrivacySettingsScreen
import com.hamyareman.ir.ui.settings.RemindersScreen
import com.hamyareman.ir.ui.settings.SettingsScreen
import com.hamyareman.ir.ui.settings.SyncScreen
import com.hamyareman.ir.ui.settings.UserGuideScreen
import com.hamyareman.ir.ui.study.AcademyHubScreen
import com.hamyareman.ir.ui.study.AcademySoonScreen
import com.hamyareman.ir.ui.study.AudiobookScreen
import com.hamyareman.ir.ui.study.BookDetailScreen
import com.hamyareman.ir.ui.study.DownloadsScreen
import com.hamyareman.ir.ui.study.HealthProgressScreen
import com.hamyareman.ir.ui.study.LeaveScreen
import com.hamyareman.ir.ui.study.LessonPdfScreen
import com.hamyareman.ir.ui.study.LessonStudyScreen
import com.hamyareman.ir.ui.study.LessonTeachScreen
import com.hamyareman.ir.ui.study.LibraryScreen
import com.hamyareman.ir.ui.study.PdfUploadScreen
import com.hamyareman.ir.ui.study.ProgressChartsScreen
import com.hamyareman.ir.ui.study.QuizReviewScreen
import com.hamyareman.ir.ui.study.QuizScreen
import com.hamyareman.ir.ui.study.StudyHomeScreen
import com.hamyareman.ir.ui.study.VideoTeachScreen
import com.hamyareman.ir.ui.study.VirtualClassScreen
import com.hamyareman.ir.ui.water.WaterScreen
import com.hamyareman.ir.ui.wellness.BetweenLessonsHubScreen
import com.hamyareman.ir.ui.wellness.PracticeGroupScreen
import com.hamyareman.ir.ui.wellness.PracticeItemScreen
import com.hamyareman.ir.ui.wellness.SketchGalleryScreen
import com.hamyareman.ir.ui.wellness.WellnessScreen
import kotlinx.coroutines.launch

@Composable
fun ZahraNavHost() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val navContext = LocalContext.current
    LaunchedEffect(route) {
        com.hamyareman.ir.ui.safespace.SafeSpaceSession.onRouteChanged(navContext, route)
    }
    // لمس اعلان پخش → صفحه‌ی تدریس همان درس (قانون: صوت فقط در صفحه‌ی تدریس پخش می‌شود؛
    // پس بعد از لود شدن همان صفحه، پخش خودکار از TeachAudioBar شروع می‌شود).
    val sleepDestination = com.hamyareman.ir.ui.study.SleepLaunch.pendingDestination
    LaunchedEffect(sleepDestination) {
        val destination = sleepDestination ?: return@LaunchedEffect
        com.hamyareman.ir.ui.study.SleepLaunch.pendingDestination = null
        val target = when (destination) {
            com.hamyareman.ir.platform.core.notifications.ReminderReceiver.SLEEP_STORY ->
                Screen.PracticeGroup.of("cl-story")
            com.hamyareman.ir.platform.core.notifications.ReminderReceiver.SLEEP_BREATH ->
                Screen.SleepBreath.route
            else -> Screen.SleepNight.route
        }
        nav.navigate(target) {
            popUpTo(Screen.Home.route)
            launchSingleTop = true
        }
    }
    LaunchedEffect(com.hamyareman.ir.ui.study.TeachLaunch.pendingTeachPack) {
        val p = com.hamyareman.ir.ui.study.TeachLaunch.pendingTeachPack ?: return@LaunchedEffect
        if (com.hamyareman.ir.platform.feature.study.BookModuleRegistry.pack(p) != null) {
            com.hamyareman.ir.ui.study.TeachLaunch.pendingTeachPack = null
            nav.navigate(Screen.LessonTeach.of(p)) {
                popUpTo(Screen.Home.route)
                launchSingleTop = true
            }
        } else {
            com.hamyareman.ir.ui.study.TeachLaunch.pendingTeachPack = null
        }
    }
    val hideBar = route?.startsWith("study-teach") == true ||
        route?.startsWith("video-teach") == true
    Box(Modifier.fillMaxSize()) {
    Scaffold(bottomBar = {
        if (!hideBar) {
            NavigationBar {
                Tabs.forEach { tab ->
                    val navId = "nav.bottom.${tab.route}"
                    NavigationBarItem(
                        selected = route == tab.route ||
                            (tab.route == Screen.Study.route && (route?.startsWith("study-book") == true || route == Screen.StudyHome.route)),
                        onClick = {
                            // هرجا باشیم، لمس عنوان منوی پایین به صفحهٔ اصلی همان عنوان می‌رود
                            // (نه به زیرصفحه‌ی قبلیِ همان تب).
                            if (route == tab.route) return@NavigationBarItem
                            val landed = nav.popBackStack(tab.route, inclusive = false)
                            if (!landed) {
                                nav.navigate(tab.route) {
                                    popUpTo(Screen.Home.route) {
                                        inclusive = false
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                        },
                        icon = { Icon(tab.icon, tab.label) },
                        label = {
                            Text(
                                tab.label,
                                fontFamily = com.hamyareman.ir.ui.AppTypography.navBar.family,
                                fontWeight = com.hamyareman.ir.ui.AppTypography.navBar.weight,
                                fontSize = com.hamyareman.ir.ui.AppTypography.navBar.size)
                        })
                }
            }
        }
    }) { pad ->
        NavHost(nav, startDestination = Screen.Home.route, modifier = Modifier.padding(pad)) {
            composable(Screen.Home.route) { HomeScreen(nav) }
            composable(Screen.Study.route) { SchoolHubScreen(nav) }
            composable(Screen.StudyHome.route) { StudyHomeScreen(nav) }
            // placeholder مشترک جای صفحه و تب بازنشسته‌شدهٔ «همراه من» را گرفته است.
            composable(Screen.Placeholder.route) {
                com.hamyareman.ir.ui.study.BookNodeScreen(
                    title = "به‌زودی",
                    bucketKey = com.hamyareman.ir.ui.study.BooksMenu.SPACEHOLDER_KEY,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Screen.More.route) { MoreScreen(nav) }
            composable(Screen.About.route) { AboutScreen { nav.popBackStack() } }
            composable(Screen.Contact.route) { ContactScreen { nav.popBackStack() } }
            composable(Screen.HealthHub.route) { HealthHubScreen(nav) }
            composable(Screen.Academy.route) { AcademyHubScreen(nav) }
            composable(Screen.AcademySoon.route) { AcademySoonScreen() }
            composable(
                Screen.Book.route,
                listOf(navArgument("bookCode") { type = NavType.StringType })) { entry ->
                BookDetailScreen(
                    bookCode = entry.arguments?.getString("bookCode").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onTeach = { packId -> nav.navigate(Screen.LessonTeach.of(packId)) },
                    onStudy = { packId -> nav.navigate(Screen.LessonStudy.of(packId)) },
                    onVideoTeach = { packId -> nav.navigate(Screen.VideoTeach.of(packId)) },
                    onCharts = { nav.navigate(Screen.Charts.of(entry.arguments?.getString("bookCode"))) },
                    onOpenNode = { key, title, audio ->
                        nav.navigate(Screen.BookNode.of(key, title, audio))
                    })
            }
            composable(
                Screen.LessonTeach.route,
                listOf(navArgument("packId") { type = NavType.StringType })) { entry ->
                LessonTeachScreen(
                    packId = entry.arguments?.getString("packId").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onStudy = { packId -> nav.navigate(Screen.LessonStudy.of(packId)) },
                    onPdf = { packId -> nav.navigate(Screen.LessonPdf.of(packId)) })
            }
            composable(Screen.AwarenessHub.route) { AwarenessHubScreen(nav) { nav.popBackStack() } }
            composable(Screen.HealthProgress.route) { HealthProgressScreen(onBack = { nav.popBackStack() }) }
            composable(Screen.WeeklySchedule.route) { WeeklyScheduleScreen { nav.popBackStack() } }
            composable(Screen.ClassPlan.route) {
                ClassPlanScreen(
                    onBack = { nav.popBackStack() },
                    initialTab = 0,
                    onVirtualHours = { nav.navigate(Screen.VirtualClass.route) })
            }
            composable(Screen.ClassPlanShift.route) {
                ClassPlanScreen(
                    onBack = { nav.popBackStack() },
                    initialTab = 2,
                    onVirtualHours = { nav.navigate(Screen.VirtualClass.route) },
                    onHelp = { nav.navigate(Screen.UserGuide.of("school")) },
                )
            }
            composable(Screen.ClassPlanCalendar.route) {
                ClassPlanScreen(
                    onBack = { nav.popBackStack() },
                    initialTab = 1,
                    onVirtualHours = { nav.navigate(Screen.VirtualClass.route) })
            }
            composable(Screen.VirtualClass.route) { VirtualClassScreen { nav.popBackStack() } }
            composable(Screen.Subscription.route) { com.hamyareman.ir.ui.home.SubscriptionScreen { nav.popBackStack() } }
            composable(Screen.TomorrowPrep.route) { TomorrowPrepScreen { nav.popBackStack() } }
            composable(Screen.SleepNight.route) { com.hamyareman.ir.ui.study.SleepNightScreen { nav.popBackStack() } }
            composable(Screen.SleepBreath.route) { com.hamyareman.ir.ui.study.SleepBreathScreen { nav.popBackStack() } }
            composable(Screen.BackgroundMusic.route) { BackgroundMusicScreen { nav.popBackStack() } }
            composable(Screen.Meds.route) { MedsScreen { nav.popBackStack() } }
            composable(Screen.SleepLog.route) { SleepLogScreen { nav.popBackStack() } }
            composable(Screen.ReadingCorner.route) { ReadingCornerScreen { nav.popBackStack() } }
            composable(Screen.Appearance.route) {
                AppearanceScreen(
                    onBack = { nav.popBackStack() },
                    onHelp = { nav.navigate(Screen.UserGuide.of("appearance")) },
                )
            }
            composable(Screen.CycleCal.route) { com.hamyareman.ir.ui.cycle.CycleCalScreen { nav.popBackStack() } }
            composable(Screen.CycleLog.route) { com.hamyareman.ir.ui.cycle.CycleLogScreen { nav.popBackStack() } }
            composable(Screen.CycleToday.route) { com.hamyareman.ir.ui.cycle.CycleTodayScreen(nav) { nav.popBackStack() } }
            composable(Screen.Mood.route) { MoodCheckInScreen { nav.popBackStack() } }
            composable(Screen.Mindfulness.route) { MindfulnessScreen { nav.popBackStack() } }
            composable(Screen.Calm.route) { CalmMenuScreen(nav) }
            composable(Screen.CalmHub.route) { com.hamyareman.ir.ui.calmdown.CalmHubScreen(nav) { nav.popBackStack() } }
            composable(Screen.FreeReading.route) { com.hamyareman.ir.ui.study.FreeReadingScreen(nav) { nav.popBackStack() } }
            composable(Screen.PeriodTraining.route) { com.hamyareman.ir.ui.cycle.PeriodTrainingScreen { nav.popBackStack() } }
            composable(Screen.GeneralToolkit.route) {
                com.hamyareman.ir.ui.tools.GeneralToolkitScreen(
                    onBack = { nav.popBackStack() },
                    onOpen = { id -> nav.navigate(Screen.ToolHtml.of(id)) })
            }
            composable(Screen.ChemistryLab.route) { com.hamyareman.ir.ui.tools.ChemistryLabScreen { nav.popBackStack() } }
            composable(Screen.PhysicsLab.route) { com.hamyareman.ir.ui.tools.PhysicsLabScreen { nav.popBackStack() } }
            composable(Screen.BiologyLab.route) { com.hamyareman.ir.ui.tools.BiologyLabScreen { nav.popBackStack() } }
            composable(Screen.MathToolkit.route) {
                com.hamyareman.ir.ui.tools.MathToolkitScreen(
                    onBack = { nav.popBackStack() },
                    onOpen = { id -> nav.navigate(Screen.ToolHtml.of(id)) })
            }
            composable(
                Screen.ToolHtml.route,
                listOf(navArgument("toolId") { type = NavType.StringType })) { entry ->
                val id = entry.arguments?.getString("toolId").orEmpty()
                com.hamyareman.ir.ui.tools.ToolWebScreen(
                    toolId = id,
                    title = com.hamyareman.ir.ui.tools.toolTitle(id),
                    onBack = { nav.popBackStack() })
            }
            composable(Screen.Journal.route) {
                SafeContentGuard(onLocked = {
                    nav.navigate(Screen.SafeSpace.route) { popUpTo(Screen.Journal.route) { inclusive = true } }
                }) { JournalScreen { nav.popBackStack() } }
            }
            composable(Screen.GratitudeJournal.route) { GratitudeJournalScreen { nav.popBackStack() } }
            composable(Screen.Breath.route) { BreathingScreen { nav.popBackStack() } }
            composable(Screen.Routine.route) { RoutineScreen { nav.popBackStack() } }
            composable(Screen.SafeSpace.route) { SafeSpaceScreen(nav) }
            composable(Screen.Diary.route) {
                SafeContentGuard(onLocked = {
                    nav.navigate(Screen.SafeSpace.route) { popUpTo(Screen.Diary.route) { inclusive = true } }
                }) { DiaryScreen(onBack = { nav.popBackStack() }, onHelp = { nav.navigate(Screen.UserGuide.of("diary")) }) }
            }
            composable(Screen.Notebooks.route) {
                SafeContentGuard(onLocked = {
                    nav.navigate(Screen.SafeSpace.route) { popUpTo(Screen.Notebooks.route) { inclusive = true } }
                }) { NotebooksScreen { nav.popBackStack() } }
            }
            composable(Screen.Poetry.route) {
                SafeContentGuard(onLocked = {
                    nav.navigate(Screen.SafeSpace.route) { popUpTo(Screen.Poetry.route) { inclusive = true } }
                }) { PoetryBookScreen(onBack = { nav.popBackStack() }, onHelp = { nav.navigate(Screen.UserGuide.of("poetry")) }) }
            }
            composable(Screen.SafeFreeWriting.route) {
                SafeContentGuard(onLocked = {
                    nav.navigate(Screen.SafeSpace.route) { popUpTo(Screen.SafeFreeWriting.route) { inclusive = true } }
                }) { SafeFreeWritingScreen { nav.popBackStack() } }
            }
            composable(Screen.SecureGallery.route) {
                SafeContentGuard(onLocked = {
                    nav.navigate(Screen.SafeSpace.route) { popUpTo(Screen.SecureGallery.route) { inclusive = true } }
                }) {
                    SecureMediaGalleryScreen(
                        onBack = { nav.popBackStack() },
                        onHelp = { nav.navigate(Screen.UserGuide.of("album")) },
                    )
                }
            }
            composable(Screen.Writing.route) {
                SafeContentGuard(onLocked = {
                    nav.navigate(Screen.SafeSpace.route) { popUpTo(Screen.Writing.route) { inclusive = true } }
                }) { WritingPromptScreen { nav.popBackStack() } }
            }
            composable(Screen.Helplines.route) { HelplinesScreen { nav.popBackStack() } }
            composable(Screen.Library.route) { LibraryScreen { nav.popBackStack() } }
            composable(Screen.Audiobook.route) { AudiobookScreen { nav.popBackStack() } }
            composable(Screen.Leave.route) { LeaveScreen { nav.popBackStack() } }
            composable(Screen.School.route) {
                ClassPlanScreen(
                    onBack = { nav.popBackStack() },
                    initialTab = 2,
                    onVirtualHours = { nav.navigate(Screen.VirtualClass.route) })
            }
            composable(
                Screen.Quiz.route,
                listOf(navArgument("lessonId") { type = NavType.StringType; defaultValue = "" })) { entry ->
                QuizScreen(
                    lessonId = entry.arguments?.getString("lessonId").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onReview = { nav.navigate(Screen.QuizReview.route) })
            }

            composable(
                Screen.LessonStudy.route,
                listOf(navArgument("packId") { type = NavType.StringType })) { entry ->
                val packId = entry.arguments?.getString("packId").orEmpty()
                LessonStudyScreen(
                    packId = packId,
                    onBack = { nav.popBackStack() })
            }
            composable(
                Screen.LessonPdf.route,
                listOf(navArgument("packId") { type = NavType.StringType })) { entry ->
                LessonPdfScreen(
                    packId = entry.arguments?.getString("packId").orEmpty(),
                    onBack = { nav.popBackStack() })
            }
            composable(Screen.QuizReview.route) { QuizReviewScreen { nav.popBackStack() } }
            composable(Screen.Pdf.route) { PdfUploadScreen { nav.popBackStack() } }
            composable(
                Screen.Charts.route,
                listOf(navArgument("bookCode") { type = NavType.StringType; defaultValue = "" })) { entry ->
                ProgressChartsScreen(
                    bookCode = entry.arguments?.getString("bookCode").orEmpty().ifBlank { null },
                    onBack = { nav.popBackStack() },
                    onPickBook = { code -> nav.navigate(Screen.Charts.of(code)) })
            }
            composable(
                Screen.VideoTeach.route,
                listOf(navArgument("packId") { type = NavType.StringType })) { entry ->
                VideoTeachScreen(
                    packId = entry.arguments?.getString("packId").orEmpty(),
                    onBack = { nav.popBackStack() })
            }
            composable(Screen.Downloads.route) { DownloadsScreen { nav.popBackStack() } }
            composable(Screen.Art.route) { DailyArtPromptScreen({ nav.popBackStack() }, { nav.navigate(Screen.Gallery.route) }) }
            composable(Screen.Gallery.route) { ArtGalleryScreen { nav.popBackStack() } }
            composable(Screen.Learning.route) { LearningHomeScreen(nav) }
            composable(
                Screen.Lesson.route,
                listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                LessonScreen(
                    lessonId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onQuiz = { lessonId -> nav.navigate(Screen.Quiz.of(lessonId)) })
            }
            composable(Screen.Placement.route) { PlacementTestScreen { nav.popBackStack() } }
            composable(
                Screen.Roadmap.route,
                listOf(navArgument("track") { type = NavType.StringType; defaultValue = "" })) { entry ->
                RoadmapScreen(
                    onBack = { nav.popBackStack() },
                    trackFilter = entry.arguments?.getString("track").orEmpty())
            }
            composable(Screen.AiLearning.route) { AiLearningHomeScreen(nav) }
            composable(Screen.AiAssessment.route) { AiAssessmentScreen { nav.popBackStack() } }
            composable(Screen.Recipes.route) {
                RecipesScreen(
                    onBack = { nav.popBackStack() },
                    onDetail = { recipeId -> nav.navigate(Screen.RecipeDetail.of(recipeId)) })
            }
            composable(
                Screen.RecipeDetail.route,
                listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                RecipeDetailScreen(
                    recipeId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() })
            }
            composable(Screen.Exercise.route) {
                ExerciseScreen(onExerciseClick = { id -> nav.navigate(Screen.ExerciseDetail.of(id)) })
            }
            composable(Screen.ExerciseDetail.route, listOf(navArgument("id") { type = NavType.StringType })) { e ->
                ExerciseDetailScreen(
                    exerciseId = e.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() })
            }
            composable(Screen.Water.route) { WaterScreen() }
            composable(Screen.Settings.route) { SettingsScreen(nav) }
            composable(
                Screen.UserGuide.route,
                listOf(navArgument("section") { type = NavType.StringType; defaultValue = "" }),
            ) { entry ->
                UserGuideScreen(
                    onBack = { nav.popBackStack() },
                    initialSection = entry.arguments?.getString("section").orEmpty(),
                )
            }
            composable(Screen.UserProfile.route) {
                val container = LocalAppContainer.current
                val ctx = androidx.compose.ui.platform.LocalContext.current
                val scopeUp = rememberCoroutineScope()
                var profile by remember { mutableStateOf<com.hamyareman.ir.ui.profile.StudentProfile?>(null) }
                var loading by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    com.hamyareman.ir.ui.profile.StudentProfileState.loadAvatarMirror(ctx)
                    val uid = runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
                    profile = if (uid.isBlank()) null else
                        runCatching { com.hamyareman.ir.ui.profile.StudentProfileRepo.fetch(container.tables, uid) }.getOrNull()
                    profile?.let { fetched ->
                        com.hamyareman.ir.ui.profile.StudentProfileState.writeMirror(
                            ctx, fetched.grade, true, fetched.firstName, fetched.subscription, fetched.gender)
                    }
                    if (uid.isNotBlank()) {
                        runCatching { com.hamyareman.ir.ui.profile.AvatarSync.pull(ctx, uid) }
                    }
                    loading = false
                }
                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    else -> com.hamyareman.ir.ui.profile.UserProfileScreen(
                        profile = profile,
                        onBack = { nav.popBackStack() },
                        onLogout = {
                            scopeUp.launch {
                                runCatching { container.auth.logout() }
                                com.hamyareman.ir.ui.profile.StudentProfileState.clearMirror(ctx)
                                (ctx as? com.hamyareman.ir.MainActivity)?.onLoggedOut()
                            }
                        },
                        onOpenSubscription = { nav.navigate(Screen.Subscription.route) },
                        onSave = { p ->
                            val uid = runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
                            val toSave = p.copy(userId = uid, grade = profile?.grade ?: com.hamyareman.ir.ui.profile.StudentProfileState.grade)
                            val email = toSave.email.ifBlank { runCatching { container.auth.currentUser() }.getOrNull()?.email.orEmpty() }
                            val ok = runCatching {
                                com.hamyareman.ir.ui.profile.StudentProfileRepo.save(container.tables, email, toSave)
                            }.getOrDefault(false)
                            if (ok) {
                                com.hamyareman.ir.ui.profile.StudentProfileState.writeMirror(
                                    ctx, toSave.grade, true, toSave.firstName,
                                    profile?.subscription ?: com.hamyareman.ir.ui.profile.StudentProfileState.subscription.ifBlank { "free" },
                                    toSave.gender)
                                container.uiPrefs.applyDefaultForGender(toSave.gender)
                                profile = toSave.copy(subscription = profile?.subscription ?: "free")
                                runCatching { com.hamyareman.ir.ui.profile.AvatarSync.push(ctx, container.storage, uid) }
                            }
                            ok
                        })
                }
            }
            composable(Screen.Privacy.route) { PrivacySettingsScreen { nav.popBackStack() } }
            composable(Screen.Badges.route) { BadgesScreen { nav.popBackStack() } }
            composable(Screen.Lock.route) { AppLockScreen { nav.popBackStack() } }
            composable(Screen.Reminders.route) { RemindersScreen { nav.popBackStack() } }
            composable(Screen.Sync.route) { SyncScreen { nav.popBackStack() } }
            composable(Screen.ContentHub.route) {
                ContentHubScreen(
                    onBack = { nav.popBackStack() },
                    onCategory = { c -> nav.navigate(Screen.ContentCategory.of(c)) },
                )
            }
            composable(
                Screen.ContentCategory.route,
                listOf(navArgument("cat") { type = NavType.StringType }),
            ) { entry ->
                ContentCategoryScreen(
                    cat = entry.arguments?.getString("cat").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onOpen = { id -> nav.navigate(Screen.ContentHtml.of(id)) },
                )
            }
            composable(
                Screen.ContentHtml.route,
                listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                ContentHtmlScreen(
                    itemId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                Screen.BookNode.route,
                listOf(
                    navArgument("key") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType },
                    navArgument("audio") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                com.hamyareman.ir.ui.study.BookNodeScreen(
                    title = entry.arguments?.getString("title").orEmpty(),
                    bucketKey = entry.arguments?.getString("key").orEmpty(),
                    audioKey = entry.arguments?.getString("audio").orEmpty(),
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                Screen.Wellness.route,
                listOf(navArgument("cat") { type = NavType.StringType; defaultValue = "" })) { entry ->
                WellnessScreen(
                    onBack = { nav.popBackStack() },
                    onSketchGallery = { nav.navigate(Screen.SketchGallery.route) },
                    initialCategory = entry.arguments?.getString("cat").orEmpty().ifBlank { null })
            }
            composable(Screen.BetweenLessons.route) {
                BetweenLessonsHubScreen(nav) { nav.popBackStack() }
            }
            composable(
                Screen.PracticeGroup.route,
                listOf(navArgument("groupId") { type = NavType.StringType })) { entry ->
                PracticeGroupScreen(
                    nav = nav,
                    groupId = entry.arguments?.getString("groupId").orEmpty(),
                    onBack = { nav.popBackStack() })
            }
            composable(
                Screen.PracticeItem.route,
                listOf(navArgument("itemId") { type = NavType.StringType })) { entry ->
                PracticeItemScreen(
                    itemId = entry.arguments?.getString("itemId").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onWellness = { cat -> nav.layerTo(Screen.Wellness.of(cat)) },
                    onRoute = { r -> nav.layerTo(r) })
            }
            composable(Screen.SketchGallery.route) {
                SketchGalleryScreen(onBack = { nav.popBackStack() })
            }
        }
    }
    }
}
