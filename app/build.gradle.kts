plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.dagger.hilt)
    id("com.google.devtools.ksp")
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
//    kotlin("kapt")
}

android {
    namespace = "com.example.charityDept"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.charityDept"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "3.0"

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
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}



dependencies {
    implementation(libs.firebase.storage)
    implementation(libs.firebase.crashlytics)
    //    implementation("coreLibraryDesugaring com.android.tools:desugar_jdk_libs:2.1.2")
    //no need for adding  @RequiresApi(Build.VERSION_CODES.O)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    kspAndroidTest(libs.hilt.android.compiler)
    // the library provides the hiltViewModel() function used in Composables.
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")


    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
//    annotationProcessor(libs.androidx.room.compiler)

    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.retrofit.gson)
    implementation(libs.squareup.okhttp3.logging)

    //viewModel
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    //tracking data from remote server
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // images in json
    implementation ("io.coil-kt:coil-compose:2.2.2")

    //Navigation  2.7.6
    implementation ("androidx.navigation:navigation-compose:2.8.7")

    //extended Icons
    implementation ("androidx.compose.material:material-icons-extended:1.5.4")
    //system UI Controller
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.27.0")

    //croping images
//    implementation("com.github.CanHub:Android-Image-Cropper:4.0.0")

    /// ADDED (Room KTX for coroutines/transactions)
    implementation("androidx.room:room-ktx:2.6.1")

    /// ADDED (Room Paging bridge)
    implementation("androidx.room:room-paging:2.6.1")

    /// ADDED (Paging 3 runtime for DB→UI)
    implementation("androidx.paging:paging-runtime-ktx:3.3.2")

    /// ADDED (Paging 3 Compose adapter; placeholders off will be set in code)
    implementation("androidx.paging:paging-compose:3.3.2")

    /// ADDED (WorkManager for DeltaIn/DeltaOut workers)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    /// ADDED (Hilt integration for WorkManager; compiler via KSP)
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    /// ADDED (Coroutines — explicit to pin versions and avoid transitive drift)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    /// ADDED (Optional structured logging hooks for metrics: TTFI, queue length, retries)
    implementation("com.jakewharton.timber:timber:5.0.1")


    //image
    implementation("io.coil-kt:coil-compose:2.7.0")
//    implementation("com.canhub.cropper:canhub-android-image-cropper:4.6.0")
    implementation("com.vanniktech:android-image-cropper:4.7.0")
//    implementation("androidx.hilt:hilt-work:1.2.0")        // ← required for Hilt+WorkManager
//    kapt("androidx.hilt:hilt-compiler:1.2.0" )
// ← THIS is often miss
//    ksp("androidx.hilt:hilt-compiler:1.2.0")
//    kapt ("androidx.hilt:hilt-compiler:1.2.0")
    // (Optional) test variants:
//    kaptAndroidTest(libs.hilt.android.compiler) // instead of kspAndroidTest(...)

    // --- Tests (if you use Hilt in androidTest) ---
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.android.compiler)    // Google Hilt (tests)
    kspAndroidTest("androidx.hilt:hilt-compiler:1.2.0") // AndroidX Hilt (tests) if needed

}
