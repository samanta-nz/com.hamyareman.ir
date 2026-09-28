pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// این پروژه نسخه‌ی جداشده‌ی «فقط اپ زهرا» از ZahraPadarPlatform است.
// اپ پدر و ماژول مشترکی که فقط پدر لازم دارد اینجا نیست — به Padar-App-Final.zip نگاه کنید.
rootProject.name = "HamyarPlatform"

include(
    ":hamyar-app",
    ":hamyar-admin",
    ":core-common",
    ":core-designsystem",
    ":core-appwrite",
    ":core-security",
    ":core-notifications",
    ":core-sync",
    ":feature-pairing",
    ":feature-hearttoheart",
    ":feature-calls",
    ":feature-playback",
    ":feature-study",
)

project(":hamyar-app").projectDir = file("apps/hamyar-app")
project(":hamyar-admin").projectDir = file("apps/hamyar-admin")
project(":core-common").projectDir = file("shared/core-common")
project(":core-designsystem").projectDir = file("shared/core-designsystem")
project(":core-appwrite").projectDir = file("shared/core-appwrite")
project(":core-security").projectDir = file("shared/core-security")
project(":core-notifications").projectDir = file("shared/core-notifications")
project(":core-sync").projectDir = file("shared/core-sync")
project(":feature-pairing").projectDir = file("shared/feature-pairing")
project(":feature-hearttoheart").projectDir = file("shared/feature-hearttoheart")
project(":feature-calls").projectDir = file("shared/feature-calls")
project(":feature-playback").projectDir = file("shared/feature-playback")
project(":feature-study").projectDir = file("shared/feature-study")
