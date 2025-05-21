plugins {
    alias(libs.plugins.android.application)

    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.budget"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.budget"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:deprecation")
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.annotation)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.firebase.database)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.firebase.analytics)
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation (libs.mpandroidchart)
    implementation("com.mikhaellopez:circularprogressbar:3.1.0")
    implementation("com.google.firebase:firebase-bom:33.12.0")
    implementation ("com.google.android.material:material:1.6.0")
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")
   implementation("com.google.gms:google-services:4.3.15")
    implementation ("com.google.cloud:google-cloud-dialogflow:2.1.0")
    implementation ("io.grpc:grpc-okhttp:1.30.0")
}