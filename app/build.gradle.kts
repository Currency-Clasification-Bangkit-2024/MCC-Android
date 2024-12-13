plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.hafidyahya.multiplecurrencyclassifier"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hafidyahya.multiplecurrencyclassifier"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures{
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.0")
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.6.0")
    implementation ("androidx.navigation:navigation-fragment-ktx:2.7.0")
    implementation ("androidx.navigation:navigation-ui-ktx:2.7.0")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("androidx.camera:camera-core:1.4.0")
    implementation ("androidx.camera:camera-view:1.4.0")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")  // Untuk mengikat Camera ke Lifecycle

    implementation ("com.google.android.material:material:1.9.0")

    implementation ("com.squareup.okhttp3:okhttp:4.11.0")

    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation ("androidx.viewpager2:viewpager2:1.0.0")
    implementation ("androidx.fragment:fragment-ktx:1.5.0")
    implementation ("com.google.android.material:material:1.9.0")






}