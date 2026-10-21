import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.nabdaldhikr.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.nabdaldhikr.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    androidResources {
        localeFilters += listOf("ar", "en")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-core:1.7.8")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.10.0")

    // JSON
    implementation("com.google.code.gson:gson:2.14.0")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")

    // DataStore for settings
    implementation("androidx.datastore:datastore-preferences:1.2.1")

    // WorkManager for reminders
    implementation("androidx.work:work-runtime-ktx:2.11.1")

    // Location & Play Services for Qibla
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Lifecycle Runtime Compose
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
