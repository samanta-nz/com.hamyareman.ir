plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val appwriteEndpoint = findProperty("resolvedAppwriteEndpoint") as? String
    ?: "https://fra.cloud.appwrite.io/v1"
val appwriteProjectId = findProperty("resolvedAppwriteProjectId") as? String ?: "6a9d59e3002751cc3ea8"
val appwriteDatabaseId = findProperty("resolvedAppwriteDatabaseId") as? String ?: "ZahraDB"
// کلید API داخل APK کامپایل نمی‌شود؛ از تنظیمات اتصال سرور خوانده می‌شود.
val appwriteApiKey = ""

android {
    namespace = "com.hamyareman.admin"

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storeType = "pkcs12"
        }
    }

    compileSdk = 37

    defaultConfig {
        applicationId = "com.hamyareman.admin"
        minSdk = 26
        targetSdk = 36
        versionCode = 11
        versionName = "1.10"
        buildConfigField("String", "APPWRITE_ENDPOINT", "\"$appwriteEndpoint\"")
        buildConfigField("String", "APPWRITE_PROJECT_ID", "\"$appwriteProjectId\"")
        manifestPlaceholders["appwriteProjectId"] = appwriteProjectId
        buildConfigField("String", "APPWRITE_DATABASE_ID", "\"$appwriteDatabaseId\"")
        buildConfigField("String", "APPWRITE_API_KEY", "\"$appwriteApiKey\"")
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
        }
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.coil.compose)
    implementation(project(":core-common"))
    implementation(project(":core-designsystem"))
    implementation(project(":core-appwrite"))
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    debugImplementation(libs.androidx.compose.ui.tooling)
}
