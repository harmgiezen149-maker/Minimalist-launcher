plugins {
    alias(libs.plugins.android.application)
}

// CI numbers every build, so each APK installs as an update over the last one.
val buildNumber = providers.environmentVariable("GITHUB_RUN_NUMBER").map(String::toInt).getOrElse(1)
val commit = providers.environmentVariable("GITHUB_SHA").map { it.take(7) }.getOrElse("local")

android {
    namespace = "io.github.minilauncher"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.minilauncher"
        minSdk = 26
        targetSdk = 36
        versionCode = buildNumber
        versionName = "1.$buildNumber"
        buildConfigField("String", "COMMIT", "\"$commit\"")
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
        textReport = true
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
}
