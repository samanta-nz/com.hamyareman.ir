plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.hamyareman.ir.platform.feature.playback"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = false
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// AGP 9: پلاگین Kotlin Android حذف شده (Kotlin داخلی است)؛
// گزینه‌های کامپایلر از `android.kotlinOptions` به این بلوک منتقل شده‌اند.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // `api` چون سرویس/کنترلر در امضای عمومی ماژول دیده می‌شوند.
    api(libs.androidx.media3.session)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // پرامپت ۰۱: حافظه‌ی پیشرفت + سینک
    implementation(project(":core-common"))
    implementation(project(":core-appwrite"))
    implementation(project(":core-sync"))

    testImplementation(libs.junit)
}
