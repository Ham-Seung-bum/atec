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

    signingConfigs {
        // CI 러너마다 새 debug 키가 생성되면 서명이 달라져 기기에서 업데이트 설치가 실패한다
        // ("앱이 설치되지 않음"). 저장소에 고정한 공용 debug 키로 항상 같은 서명을 쓴다.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
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
