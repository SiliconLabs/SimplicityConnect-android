// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    id("com.android.application") version "8.4.1" apply false
    id("org.jetbrains.kotlin.android") version "1.7.21" apply false
    id("org.jetbrains.kotlin.android.extensions") version "1.7.21" apply false
    id("org.jetbrains.kotlin.kapt") version "1.7.21" apply false
    id("com.google.dagger.hilt.android") version "2.45" apply false
}

buildscript {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven {
            url = uri("https://maven.google.com/")
            name = "Google"
        }
    }
}

allprojects {
    buildDir = File("Builds/${rootProject.name}/${project.name}")

    repositories {
        mavenCentral()
        google()
    }
}
