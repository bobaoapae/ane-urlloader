plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin)
}

android {
    namespace = "br.com.redesurftank.aneurlloader"
    compileSdk = 34

    defaultConfig {
        minSdk = 22
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    api(libs.okhttp)
    api(libs.appcompat)
    api(libs.sentry.android)
    api(libs.dnsjava)
    implementation(files("C:/AIRSdks/AIRSDK_51.0.1/lib/android/FlashRuntimeExtensions.jar"))
}