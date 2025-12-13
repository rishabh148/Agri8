plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.example.agri8"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.agri8"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.0" // Ensure this version matches the Compose Compiler version
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
}

dependencies {
    implementation("org.tensorflow:tensorflow-lite:2.11.0") // or the latest version
    implementation("org.tensorflow:tensorflow-lite-support:0.3.1") // optional, for additional support utilities

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Compose Dependencies
    implementation("androidx.compose.ui:ui:1.5.0") // Ensure compatibility with the Compose Compiler
    implementation("androidx.compose.material:material:1.5.0") // Ensure compatibility with the Compose Compiler
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.0")
    implementation("androidx.compose.compiler:compiler:1.5.0") // Ensure compatibility with the Compose Compiler
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation ("androidx.compose.material3:material3:1.2.0") // or the latest version

    // Networking (Retrofit and OkHttp)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.2")

    implementation ("androidx.compose.ui:ui:1.5.0")
    implementation ("androidx.compose.material3:material3:1.1.0")
    implementation ("androidx.compose.foundation:foundation:1.5.0")
    implementation ("androidx.activity:activity-compose:1.6.0")



    // Image Loading (Coil)
    implementation("io.coil-kt:coil-compose:2.3.0")

    // Permissions (Accompanist)
    implementation("com.google.accompanist:accompanist-permissions:0.28.0")

    // JSON Parsing (Optional)
    implementation("com.google.code.gson:gson:2.10")

    // ML Kit Translation
    implementation("com.google.mlkit:translate:17.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Testing Libraries
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
