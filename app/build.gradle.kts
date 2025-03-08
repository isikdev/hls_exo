plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.hls_exo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.hls_exo"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation ("androidx.media3:media3-exoplayer:1.5.1")
    implementation ("androidx.media3:media3-ui:1.5.1")
    implementation ("androidx.media3:media3-exoplayer-dash:1.5.1")
    implementation ("androidx.media3:media3-common:1.5.1")
    implementation ("androidx.media3:media3-session:1.5.1")
    implementation ("androidx.media3:media3-exoplayer-hls:1.5.1")
    implementation ("androidx.media3:media3-exoplayer-rtsp:1.5.1")
    implementation ("androidx.media3:media3-datasource-rtmp:1.5.1")
    implementation ("androidx.media3:media3-datasource:1.5.1")
    
    // Добавляем FFmpeg расширение для Media3/ExoPlayer
    implementation ("com.google.android.exoplayer:extension-ffmpeg:2.19.1")
    // или для новых версий Media3
    implementation ("androidx.media3:media3-decoder-ffmpeg:1.5.1")


}