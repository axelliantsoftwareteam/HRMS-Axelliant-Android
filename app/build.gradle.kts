plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlin)
    alias(libs.plugins.navigationSafeArgs)
    alias(libs.plugins.hiltAndroid)
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.plugin.parcelize")

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
    ndkVersion = "28.2.13676358"
    defaultConfig {
        applicationId = "com.axelliant.hris"
        minSdk = 24
        targetSdk = 36
        // Must exceed Play Store production versionCode (currently 260911137).
        // CI uses date -u +%y%j%H%M; keep manual builds above the latest store value.
        versionCode = 262031249
        versionName = "1.17"
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "ENVIRONMENT", "\"release\"")
        buildConfigField("String", "API_BASE_URL", "\"https://internalsoftware.axelliant.dev/api/\"")
        buildConfigField("String", "API_BASE_TOKEN", "\"\"")
        buildConfigField("boolean", "TRUST_SELF_SIGNED_SSL", "false")

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
        debug {
//            buildConfigField("String", "API_BASE_URL", "\"https://internalsoftware.axelliant.dev/\"")
            buildConfigField("String", "API_BASE_URL", "\"https://10.10.202.4/api/\"")
            buildConfigField("boolean", "TRUST_SELF_SIGNED_SSL", "true")
        }
        release {
            isMinifyEnabled = false
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
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
        buildConfig = true
        viewBinding = true
        dataBinding = true
        compose = true

    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.ui:ui-viewbinding")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation("androidx.cardview:cardview:1.0.0")
    implementation(libs.material)
    implementation(libs.play.services.location)
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
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
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
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
    implementation(libs.androidx.core.ktx)

    implementation(libs.fluentui.core)
    implementation(libs.fluentui.drawer)
    implementation(libs.fluentui.icons)
    implementation(libs.fluentui.listitem)
    implementation(libs.fluentui.menus)
    implementation(libs.fluentui.progress)
    implementation(libs.fluentui.controls)
    implementation("com.intuit.sdp:sdp-android:1.1.1")
    implementation("com.microsoft.fluentui:fluentui_calendar:0.3.3")
    implementation("androidx.compose.runtime:runtime-livedata:1.7.0")
    implementation("com.microsoft.fluentui:fluentui_topappbars:0.3.9")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0")

}
