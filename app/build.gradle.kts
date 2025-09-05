import java.io.FileInputStream
import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services) // applies the Google Services plugin
}

val localProps = Properties().apply {
    val propsFile = rootProject.file("local.properties")
    if (propsFile.exists()) {
        FileInputStream(propsFile).use { load(it) }
    }
}

android {
    namespace = "com.protectalk.protectalk"
    compileSdk = 36

    defaultConfig {
        //buildConfigField("String", "BASE_URL", "\"http://192.168.1.132:8080/\"")
        buildConfigField("String", "BASE_URL", "\"http://192.116.98.70:8080/\"")
        buildConfigField(
            "String",
            "GOOGLE_API_KEY",
            "\"${localProps.getProperty("google_api_key", "")}\""
        )
        buildConfigField(
            "String",
            "GOOGLE_PROJECT_ID",
            "\"${localProps.getProperty("google_project_id", "protectalk")}\""
        )
        buildConfigField(
            "String",
            "OPENAI_API_KEY",
            "\"${localProps.getProperty("openai_api_key", "")}\""
        )
        applicationId = "com.protectalk.protectalk"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
}

dependencies {
    // --- Firebase (BOM + Auth) ---
    implementation(platform(libs.firebase.bom))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.compose.ui:ui:1.5.3")
    implementation("androidx.compose.material3:material3:1.2.1") // This version includes pull-to-refresh

    // Lifecycle for foreground/background detection
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Retrofit + Moshi
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines + Tasks
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Material Icons
    implementation(libs.material.icons.core)
    implementation(libs.androidx.material.icons.extended)

    implementation("androidx.navigation:navigation-compose:2.7.3")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.foundation)
    implementation("com.google.accompanist:accompanist-swiperefresh:0.32.0") // Accompanist SwipeRefresh - stable pull-to-refresh solution
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // WorkManager for background token upload
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
