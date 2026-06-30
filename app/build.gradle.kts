plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.dss.absensiKoas"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dss.absensiKoas"
        minSdk = 29
        targetSdk = 36
        versionCode = 7
        versionName = "7.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BASE_URL_DEBUG", "\"http://10.0.2.2:8080/\"")
        buildConfigField("String", "BASE_URL_RELEASE", "\"https://presensicoasrsudhat.deliserdangkab.go.id/\"")

        // Google Maps API Key - diambil dari local.properties (jangan commit API key!)
        val mapsApiKey = project.findProperty("MAPS_API_KEY") as String? ?: "YOUR_MAPS_API_KEY_HERE"
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "BASE_URL",
                "\"http://172.100.20.4:8080/\""
            )
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }

        release {
            manifestPlaceholders += mapOf()
            buildConfigField(
                "String",
                "BASE_URL",
                "\"https://presensicoasrsudhat.deliserdangkab.go.id/api/\""
            )
            manifestPlaceholders["usesCleartextTraffic"] = "false"

            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    // ==========================
    // Core & Lifecycle
    // ==========================
    implementation(libs.androidx.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // ==========================
    // Compose
    // ==========================
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // ==========================
    // Hilt
    // ==========================
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // ==========================
    // Networking
    // ==========================
    implementation(libs.retrofit)
    implementation(libs.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    // ==========================
    // DataStore
    // ==========================
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    // ==========================
    // Location & Maps
    // ==========================
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)

    // ==========================
    // CameraX
    // ==========================
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // ==========================
    // Image Loading
    // ==========================
    implementation(libs.coil.compose)

    // ==========================
    // Animation
    // ==========================
    implementation(libs.lottie.compose)

    // ==========================
    // Permissions
    // ==========================
    implementation(libs.accompanist.permissions)

    // ==========================
    // Unit Test
    // ==========================
    testImplementation(libs.junit)

    // ==========================
    // Android Test
    // ==========================
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.ui.test.junit4)

    // ==========================
    // Debug
    // ==========================
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}