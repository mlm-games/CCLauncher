// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
//    kotlin("jvm") version "1.7.20" apply false // Example: kotlin("jvm") version "1.9.20" apply false
}

group = "app.cclauncher"
version = "5.2.7"

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
