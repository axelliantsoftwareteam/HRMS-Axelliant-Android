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

    lint {
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.compose.ui.viewbinding)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.cardview)
    implementation(libs.material)
    implementation(libs.play.services.location)
    implementation(libs.shimmer)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
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
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.security.crypto)
    implementation(libs.msal)
    {
        exclude(group = "io.opentelemetry")
        exclude (group="com.microsoft.device.display")
    }

    // OpenTelemetry API
    implementation (libs.opentelemetry.api) // Add this line
    implementation(libs.smoothbottombar)
    implementation(libs.imagepicker)

    implementation(libs.app.update)
    implementation (libs.app.update.ktx)

    implementation(libs.fluentui.core)
    implementation(libs.fluentui.drawer)
    implementation(libs.fluentui.icons)
    implementation(libs.fluentui.listitem)
    implementation(libs.fluentui.menus)
    implementation(libs.fluentui.progress)
    implementation(libs.fluentui.controls)
    implementation(libs.fluentui.calendar)
    implementation(libs.fluentui.topappbars)
    implementation(libs.camera.camera2)
    implementation(libs.camera.core)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.document.scanner)
    implementation(libs.mpandroidchart)

}
