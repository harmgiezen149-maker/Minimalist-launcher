plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}

// Generates the app's Baseline Profile and measures its cold start, on a
// device or emulator (see the "Baseline profile" workflow). Not part of the
// regular build or tests.
android {
    namespace = "io.github.minilauncher.baselineprofile"
    compileSdk = 37

    defaultConfig {
        // Profile collection needs Android 9 or later.
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

baselineProfile {
    // The workflow starts an emulator; a connected device needs API 33+.
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
