plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlin)
    alias(libs.plugins.navigationSafeArgs)
    id("kotlin-kapt")
}

android {
    namespace = "com.axelliant.hris"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.axelliant.hris"
        minSdk = 24
        targetSdk = 34
        versionCode = 6
        versionName = "1.5"
        multiDexEnabled = true

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
    buildFeatures {
        viewBinding = true
        dataBinding = true
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
    implementation ("com.microsoft.identity.client:msal:5.+")
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