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
        versionCode = 6
        versionName = "1.0.0"

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
            // 개인 사용(Play 스토어 미배포, Q6)이라 별도 릴리스 키 없이 공용 debug 키로 서명해
            // 바로 설치 가능한 APK를 만든다. 스토어 배포로 전환하면 전용 키로 교체한다.
            signingConfig = signingConfigs.getByName("debug")
            // R8 전체 최적화가 런타임에 CameraX 콜백을 잘못 제거할 위험을 피하려고 축소는 끈다.
            // 앱이 작아 APK 크기(N-05 ≤ 10MB)에는 영향이 없다.
            isMinifyEnabled = false
            isShrinkResources = false
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
    androidTestImplementation(libs.androidx.test.runner)
}
