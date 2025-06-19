plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.wavesoffood"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.wavesoffood"
        minSdk = 28 // Updated from 24 to 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Firebase Auth
    implementation("com.google.firebase:firebase-auth:22.3.1")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Firebase Database and Storage
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)

    // Image Slider
    implementation("com.github.denzcoskun:ImageSlideshow:0.1.0")

    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation(libs.car.ui.lib) // Ensure that this is compatible with minSdk 28

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Kotlin test (optional)
    implementation(kotlin("test"))
}
