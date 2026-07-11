plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Numéro de build injecté par la CI (-PbuildNumber=N) : versionne les APK
// d'essai et alimente la vérification de mise à jour intégrée
val buildNumber = (project.findProperty("buildNumber") as String?)?.toIntOrNull() ?: 0

android {
    namespace = "com.mkmemories.copilot"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mkmemories.copilot"
        minSdk = 28
        targetSdk = 35
        versionCode = if (buildNumber > 0) buildNumber else 1
        versionName = "0.1.0" + if (buildNumber > 0) " (build $buildNumber)" else ""
        buildConfigField("int", "BUILD_NUMBER", "$buildNumber")
    }

    signingConfigs {
        // Clé d'ESSAI committée volontairement : elle garantit une signature
        // stable entre les builds CI pour que la mise à jour intégrée installe
        // par-dessus. TODO Play Store : vraie clé via secrets, jamais dans git.
        create("testing") {
            storeFile = rootProject.file("keystore/testing.keystore")
            storePassword = "mkcopilot"
            keyAlias = "mkcopilot"
            keyPassword = "mkcopilot"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("testing")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.car.app)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.org.json)
}
