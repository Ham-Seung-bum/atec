// AGP 9+ compiles Kotlin natively; the org.jetbrains.kotlin.android plugin must not be applied.
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.atec.autoshot"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.atec.autoshot"
        minSdk = 29
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0-m1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
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
}

dependencies {
    implementation(libs.androidx.activity)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.rules)
}
