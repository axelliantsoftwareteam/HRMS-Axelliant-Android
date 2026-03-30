plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlin)
    alias(libs.plugins.navigationSafeArgs)
    id("kotlin-kapt")
}

val androidKeystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
val androidKeystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val androidKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
val androidKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")
val hasReleaseSigning =
    !androidKeystorePath.isNullOrBlank() &&
        !androidKeystorePassword.isNullOrBlank() &&
        !androidKeyAlias.isNullOrBlank() &&
        !androidKeyPassword.isNullOrBlank()

android {
    namespace = "com.axelliant.hris"
    compileSdk = 36
    ndkVersion = "26.1.10909125"
    defaultConfig {
        applicationId = "com.axelliant.hris"
        minSdk = 24
        targetSdk = 36
        versionCode = 15
        versionName = "1.15"
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }


    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(androidKeystorePath!!)
                storePassword = androidKeystorePassword
                keyAlias = androidKeyAlias
                keyPassword = androidKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        viewBinding = true
        dataBinding = true
    }

    packagingOptions {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.play.services.location)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.koin.android)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
//    implementation(libs.navigation.fragment)
    implementation(libs.ssp.android) // multi screen text sizes support
    implementation(libs.sdp.android) // multi screen width height support
//gif image
    implementation(libs.android.gif.drawable)
    implementation(libs.glide)

    implementation(libs.converter.scalars)
    implementation(libs.gson)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.okhttp)

    implementation(libs.circleimageview) // circle image view
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    implementation(libs.material.v120alpha03)
    implementation("com.microsoft.identity.client:msal:5.6.0")
    {
        exclude(group = "io.opentelemetry")
        exclude (group="com.microsoft.device.display")
    }

    // OpenTelemetry API
    implementation (libs.opentelemetry.api) // Add this line
    implementation (libs.play.services.location.v2101)

    implementation(libs.smoothbottombar)
    implementation(libs.imagepicker)

    implementation(libs.app.update)
    implementation (libs.app.update.ktx)

    implementation (libs.commons.net)

}
