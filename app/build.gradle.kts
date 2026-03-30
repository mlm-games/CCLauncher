@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.apk.dist)
    alias(libs.plugins.ksp)
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi"
        )
    }
}

android {
    compileSdk = 36

    defaultConfig {
        applicationId = "app.cclauncher"
        minSdk = 24
        targetSdk = 36
        versionCode = 1270
        versionName = "v10.7.4"

        androidResources {
            localeFilters += setOf("en", "ar", "de", "es-rES", "es-rUS", "fr", "hr", "hu", "in", "it", "ja", "pl", "pt-rBR", "ru-rRU", "sv", "tr", "uk", "zh")
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val enableApkSplits = (providers.gradleProperty("enableApkSplits").orNull ?: "true").toBoolean()
    val includeUniversalApk = (providers.gradleProperty("includeUniversalApk").orNull ?: "true").toBoolean()
    val targetAbi = providers.gradleProperty("targetAbi").orNull

    splits {
        abi {
            isEnable = enableApkSplits
            reset()
            if (enableApkSplits) {
                if (targetAbi != null) {
                    include(targetAbi)
                } else {
                    include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
                }
            }
            isUniversalApk = includeUniversalApk && enableApkSplits
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "${rootProject.projectDir}/release.keystore")
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = false
            enableV4Signing = false
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("Long", "BUILD_TIME", "0L")
            isShrinkResources = true
        }
        getByName("debug") {
            isShrinkResources = false
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    namespace = "app.cclauncher"


    dependenciesInfo {
        includeInApk = false
    }
}


apkDist {
    artifactNamePrefix.set("cclauncher")
}


// Configure all tasks that are instances of AbstractArchiveTask
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.kotlin.stdlib)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.recyclerview)

    // Android lifecycle
    implementation(libs.lifecycle.viewmodel.ktx)

    // Work Manager
    implementation(libs.work.runtime.ktx)

    implementation(libs.kmp.settings.core)
    implementation(libs.androidx.compose.foundation)
    ksp(libs.kmp.settings.ksp)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.annotations)
    ksp(libs.koin.ksp.compiler)


    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)


    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.gson)
    implementation(libs.androidx.material.icons.extended)


    //Material dependencies
    implementation(libs.material)
    implementation(libs.material3.android)

    // Compose dependencies
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.constraintlayout.compose.android)

    implementation(libs.androidbrowserhelper)
    implementation(libs.androidx.datastore.preferences.core)

    // Testing
//    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
}
