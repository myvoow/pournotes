plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.pournotes"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.pournotes"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        androidResources.localeFilters += listOf("fr")
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    buildFeatures { compose = true }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.05.01"))
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    // Material 3 Expressive (MaterialExpressiveTheme, MotionScheme, LoadingIndicator, flexible app bars) is in the 1.5 alphas.
    implementation("androidx.compose.material3:material3:1.5.0-alpha24")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    // QR login: Google's scanner UI from Play services, no camera permission.
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    testImplementation("junit:junit:4.13.2")
    // Android's org.json is a stub in local unit tests.
    testImplementation("org.json:json:20250517")
}
