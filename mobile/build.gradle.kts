plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.android.extensions")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}

repositories {
    maven { url = uri("https://maven.fabric.io/public") }
    maven { url = uri("https://jitpack.io") }
    mavenCentral()
}

android {
    compileSdk = 33
    namespace = "com.siliconlabs.bledemo"

    defaultConfig {
        minSdk = 29
        targetSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isDebuggable = true
            //renderscriptDebuggable false
            //minifyEnabled true
            //pseudoLocalesEnabled false
        }
    }

    lint {
        checkReleaseBuilds = false
        // Or, if you prefer, you can continue to check for errors in release builds,
        // but continue the build even when errors are found:
        abortOnError = false
    }

    val versionDim = "version"
    flavorDimensions.add(versionDim)

    productFlavors {
        create("blueGecko") {
            dimension = versionDim
            applicationId = "com.siliconlabs.bledemo"
            versionCode = 46
            versionName = "2.8.0"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(fileTree(mapOf("include" to listOf("*.jar", "*.so"), "dir" to "libs")))

    // androidx
    implementation("androidx.core:core-ktx:1.10.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.fragment:fragment:1.5.6")
    implementation("androidx.core:core-splashscreen:1.0.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.activity:activity-ktx:1.6.0")
    implementation("com.google.android.material:material:1.9.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    // UI components
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.0")
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation("io.github.g00fy2.quickie:quickie-bundled:1.7.0")

    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.0.3")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")
    implementation("androidx.navigation:navigation-dynamic-features-fragment:2.5.3")

    // Dependency injection
    implementation("com.google.dagger:hilt-android:2.45")
    kapt("com.google.dagger:hilt-android-compiler:2.45")

    // View binding
    implementation("com.github.kirich1409:viewbindingpropertydelegate-noreflection:1.5.9")

    // Logging
    implementation("com.jakewharton.timber:timber:4.7.1")

    // Parsing
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.opencsv:opencsv:5.6")

    // Only used for Int.pow() method in a couple of places
    implementation("com.google.guava:guava:29.0-android")

    // Coil
    implementation("io.coil-kt:coil:2.4.0")
    implementation("io.coil-kt:coil-gif:2.4.0")
    implementation("io.coil-kt:coil-svg:2.4.0")

    // instrumented tests
    testImplementation("junit:junit:4.13.2")
    androidTestUtil("androidx.test:orchestrator:1.4.2")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")

    //Matter
    implementation("androidx.camera:camera-core:1.1.0")
    implementation("androidx.camera:camera-camera2:1.1.0")
    implementation("androidx.camera:camera-lifecycle:1.1.0")
    implementation("androidx.camera:camera-view:1.1.0")
    implementation("com.google.mlkit:barcode-scanning:17.0.2")
    implementation ("com.daimajia.swipelayout:library:1.2.0@aar")
}
