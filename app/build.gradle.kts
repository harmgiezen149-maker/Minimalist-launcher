import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.roborazzi)
}

// CI numbers every build, so each APK installs as an update over the last one.
val buildNumber = providers.environmentVariable("GITHUB_RUN_NUMBER").map(String::toInt).getOrElse(1)
val commit = providers.environmentVariable("GITHUB_SHA").map { it.take(7) }.getOrElse("local")

android {
    namespace = "io.github.minilauncher"
    // 37 because AndroidX core 1.19 requires it; the app still targets 36.
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.minilauncher"
        minSdk = 26
        targetSdk = 36
        versionCode = buildNumber
        versionName = "1.$buildNumber"
        buildConfigField("String", "COMMIT", "\"$commit\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        // Robolectric tests inflate the real layouts and read the real strings.
        unitTests.isIncludeAndroidResources = true
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            // JaCoCo coverage of the JVM unit tests: createDebugUnitTestCoverageReport
            enableUnitTestCoverage = true
        }
        // No signing config here: CI signs the release APK in a separate job
        // that is the only place the key is ever decrypted (see README).
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // Lint errors fail the build. The full list, warnings included, is
        // printed in the CI log and kept as an HTML report.
        abortOnError = true
    }
}

// Robolectric loads the app's classes through its own class loader; without
// this JaCoCo would not count the code those tests cover.
tasks.withType<Test>().configureEach {
    extensions.findByType(JacocoTaskExtension::class.java)?.apply {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
    testImplementation(libs.junit)
    // Real org.json for unit tests; the android.jar stub throws "not mocked".
    testImplementation(libs.json)
    // Screens and settings on a simulated Android, plus screenshot comparison.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.roborazzi)
    // Smoke tests on an emulator, with Espresso's accessibility checks.
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.espresso.accessibility)
}
