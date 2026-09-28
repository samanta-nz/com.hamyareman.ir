plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.hamyareman.ir.platform.core.security"
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
}

// AGP 9: پلاگین Kotlin Android حذف شده (Kotlin داخلی است)؛
// گزینه‌های کامپایلر از `android.kotlinOptions` به این بلوک منتقل شده‌اند.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // `api` چون MainActivity در اپ‌ها باید از FragmentActivity ارث ببرد و
    // BiometricPrompt/FragmentActivity باید در compile classpath مصرف‌کننده دیده شوند.
    api(libs.androidx.biometric)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(project(":core-common"))
    testImplementation(libs.junit)
}
