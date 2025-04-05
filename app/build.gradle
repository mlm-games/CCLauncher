plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    //alias(libs.plugins.compose.compiler) apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" // this version matches your Kotlin version
}

android {

    compileSdk 35

    splits {
        abi {
            enable true
            reset()
            if (project.hasProperty('targetAbi')) {
                include project.property('targetAbi')
            } else {
                include "arm64-v8a", "armeabi-v7a", "x86", "x86_64"
            }
            universalApk false
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    defaultConfig {
        applicationId 'app.clauncher'
        minSdkVersion 24
        targetSdkVersion 35
        versionCode 390
        versionName "v5.2.7"

        resourceConfigurations += ["en", "ar", "de", "es-rES", "es-rUS", "fr", "hr", "hu", "in", "it", "ja", "pl", "pt-rBR", "ru-rRU", "sv", "tr", "uk", "zh"]
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        release {
            storeFile file(System.getenv("KEYSTORE_PATH") ?: "${rootProject.projectDir}/release.keystore")
            storePassword System.getenv("STORE_PASSWORD")
            keyAlias System.getenv("KEY_ALIAS")
            keyPassword System.getenv("KEY_PASSWORD")
            v1SigningEnabled true
            v2SigningEnabled true
            enableV3Signing false
            enableV4Signing false
        }
    }

    buildTypes {
        release {
            signingConfig signingConfigs.release
            debuggable false
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            buildConfigField "Long", "BUILD_TIME", "0L"
            shrinkResources true
            vcsInfo.include false
        }
        debug {
            debuggable true
            minifyEnabled false
            applicationIdSuffix ".debug"
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose true
    }

    composeCompiler {
        reportsDestination = layout.buildDirectory.dir("compose_compiler")
        stabilityConfigurationFile = rootProject.layout.projectDirectory.file("stability_config.conf")
    }

    namespace 'app.clauncher'

    aaptOptions {
        cruncherEnabled = false
    }

    dependenciesInfo {
        includeInApk = false
    }
}

// Configure all tasks that are instances of AbstractArchiveTask
tasks.withType(AbstractArchiveTask).configureEach {
    preserveFileTimestamps = false
    reproducibleFileOrder = true
}

dependencies {
    implementation fileTree(dir: "libs", include: ["*.jar"])
    implementation libs.kotlin.stdlib
    implementation libs.core.ktx
    implementation libs.appcompat
    implementation libs.recyclerview

    // Android lifecycle
    implementation libs.lifecycle.extensions
    implementation libs.lifecycle.viewmodel.ktx

    // Navigation
    implementation libs.navigation.fragment.ktx

    // Work Manager
    implementation libs.work.runtime.ktx

    implementation("androidx.datastore:datastore-preferences:1.1.4")

    //Material dependency
    implementation libs.material
    implementation libs.material3.android

    // Compose dependencies
    implementation libs.ui
    implementation libs.material3
    implementation libs.ui.tooling.preview
    implementation libs.runtime
    implementation libs.activity.compose
    implementation libs.lifecycle.viewmodel.compose
    implementation libs.navigation.compose
    implementation libs.constraintlayout.compose.android

    // Testing
    androidTestImplementation libs.ui.test.junit4
    debugImplementation libs.ui.tooling
}

tasks.register('verifyReleaseApk') {
    dependsOn 'assembleRelease'
    doLast {
        def apkDir = new File("${layout.buildDirectory.get()}/outputs/apk/release/")
        def androidSdkPath = android.getSdkDirectory().getAbsolutePath()
        def buildToolsVersion = android.getBuildToolsVersion()
        def apksignerPath = "${androidSdkPath}/build-tools/${buildToolsVersion}/apksigner"

        apkDir.eachFileRecurse { file ->
            if (file.name.endsWith('.apk')) {
                println "Verifying APK: ${file.name}"
                exec {
                    commandLine apksignerPath, 'verify', '--verbose', file.absolutePath
                }
            }
        }
    }
}
