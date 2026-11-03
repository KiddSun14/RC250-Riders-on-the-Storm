plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.autostrom.launcher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kiddsun14.autostrom.beta"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0-beta"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildTypes { release { isMinifyEnabled = false } }
}
