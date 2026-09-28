plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.hamyareman.ir.platform.feature.study"
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

// AGP 9: Kotlin داخلی است
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core-common"))
    implementation(project(":core-sync"))

    testImplementation(libs.junit)
}
