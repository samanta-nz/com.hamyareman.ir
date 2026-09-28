import java.util.Properties

buildscript {
    // AGP 9 (built-in Kotlin) به‌صورت پیش‌فرض KGP 2.2.10 را با خود می‌آورد.
    // چون پلاگین کامپایلر Compose باید هم‌نسخه‌ی Kotlin پروژه باشد، همان نسخه‌ی
    // [versions] kotlin در gradle/libs.versions.toml را اینجا پین می‌کنیم.
    // ⚠️ این دو عدد باید همیشه یکی بمانند.
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// ---------------------------------------------------------------------------------
// local.properties به‌صورت پیش‌فرض «منبع پراپرتی Gradle» نیست؛ آن را بارگذاری می‌کنیم
// تا همان‌طور که در README آمده، appwrite.projectId با findProperty خوانده شود.
// ترتیب اولویت: ‏-P/gradle.properties  >  local.properties  >  متغیر محیطی  >  پیش‌فرض
// ---------------------------------------------------------------------------------
val localProperties = Properties().apply {
    val file = rootDir.resolve("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
localProperties.stringPropertyNames().forEach { key ->
    if (!hasProperty(key)) extra[key] = localProperties.getProperty(key)
}

fun appwriteConfig(propertyKey: String, envKey: String, default: String): String {
    val fromGradle = (findProperty(propertyKey) as? String)?.trim().orEmpty()
    if (fromGradle.isNotEmpty()) return fromGradle
    val fromEnv = System.getenv(envKey)?.trim().orEmpty()
    if (fromEnv.isNotEmpty()) return fromEnv
    return default
}

// این سه مقدار در build.gradle.kts هر دو اپ برای BuildConfig استفاده می‌شوند.
extra["resolvedAppwriteEndpoint"] =
    appwriteConfig("appwrite.endpoint", "APPWRITE_ENDPOINT", "https://fra.cloud.appwrite.io/v1")
extra["resolvedAppwriteProjectId"] =
    appwriteConfig("appwrite.projectId", "APPWRITE_PROJECT_ID", "")
extra["resolvedAppwriteDatabaseId"] =
    appwriteConfig("appwrite.databaseId", "APPWRITE_DATABASE_ID", "main_db")

if ((extra["resolvedAppwriteProjectId"] as String).isBlank()) {
    logger.lifecycle(
        "⚠️  appwrite.projectId تنظیم نشده — اپ‌ها در «حالت محلی» اجرا می‌شوند. " +
            "مقدار آن را در local.properties بگذارید (نمونه: local.properties.example).",
    )
}
