import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing: keystore.properties locally (never committed), SIGNING_* variables in CI.
val signing = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    System.getenv("SIGNING_STORE_FILE")?.let {
        put("storeFile", it)
        put("storePassword", System.getenv("SIGNING_STORE_PASSWORD"))
        put("keyAlias", System.getenv("SIGNING_KEY_ALIAS"))
        put("keyPassword", System.getenv("SIGNING_KEY_PASSWORD"))
    }
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
    signingConfigs {
        if (signing.isNotEmpty()) create("release") {
            storeFile = file(signing.getProperty("storeFile"))
            storePassword = signing.getProperty("storePassword")
            keyAlias = signing.getProperty("keyAlias")
            keyPassword = signing.getProperty("keyPassword")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
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
