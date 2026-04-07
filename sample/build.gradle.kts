plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    signingConfigs {
        named("debug") {
            storeFile = file("../common-60.keystore")
            storePassword = "000000"
            keyAlias = "000000"
            keyPassword = "000000"
        }
        create("release") {
            storeFile = file("../common-60.keystore")
            storePassword = "000000"
            keyAlias = "000000"
            keyPassword = "000000"
        }
    }

    namespace = "cc.shinichi.bigimageviewpager"
    compileSdk = 34

    defaultConfig {
        applicationId = "cc.shinichi.bigimageviewpager"
        minSdk = 24
        targetSdk = 34
        versionCode = 1000
        versionName = "1000"
        signingConfig = signingConfigs.getByName("debug")
        multiDexEnabled = true
    }

    splits {
        abi {
            isEnable = true
            reset()
            //noinspection ChromeOsAbiSupport
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        named("main") {
            jniLibs.srcDirs("libs")
        }
    }

    lint {
        abortOnError = false
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // AndroidX
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlin.stdlib)

    // PictureSelector
    implementation(libs.pictureselector)

    // Glide
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.glide.okhttp3)


    // Library module
    // implementation(libs.bigimageviewpager)
    // implementation(libs.bigimageviewpager-media3) // 可选：Media3 / ExoPlayer（sample 默认开启，用于演示视频能力）
    implementation(project(":library"))
    // implementation(project(":library-video-media3"))
}
