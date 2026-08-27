plugins {
    id("com.android.application")
}

android {
    namespace = "com.codecrafter.applock"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.codecrafter.applock"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
