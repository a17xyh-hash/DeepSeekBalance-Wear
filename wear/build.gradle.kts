plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    // 受限环境（沙箱 / CI）里默认的 ~/.android/debug.keystore 可能不可写。
    // 若项目内存在 keystore/debug.keystore，则优先使用它；不存在则回退到默认行为。
    signingConfigs {
        getByName("debug") {
            val local = rootProject.file("keystore/debug.keystore")
            if (local.exists()) {
                storeFile = local
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    namespace = "com.example.dsbalance.wear"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.example.dsbalance.wear"
        minSdk = 30          // Wear OS 3+
        targetSdk = 35
        versionCode = 28
        versionName = "3.9"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging {
        resources.excludes += setOf(
            "META-INF/androidx/lifecycle/lifecycle-common/LICENSE.txt",
            "commonMain/**",
            "META-INF/kotlin-project-structure-metadata.json"
        )
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.material3)
    implementation(libs.horologist.network.awareness)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.compose.ui.tooling)
}
