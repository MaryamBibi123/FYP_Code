import com.android.build.api.dsl.Packaging

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.parcelize")
    // gms is used for Google Play Services for Firebase
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs.kotlin")

//    id ("kotlin-kapt") // Required for Glide annotation processing

}
android {
    packaging {
        resources.excludes.add("META-INF/DEPENDENCIES")
    }
    compileSdk = 35
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }


    defaultConfig {
        applicationId = "com.example.signuplogina"
        minSdk = 28
        targetSdk = 35
        versionCode = 4
        versionName = "1.4"
        namespace = "com.example.signuplogina"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"  // Using JDK 17
    }
    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)  // Specify JDK 17
        }
    }
}

dependencies {
    // Core Android & Jetpack
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Glide & Image Loading
    implementation(libs.github.glide)
    implementation ("de.hdodenhof:circleimageview:2.1.0")
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.androidx.databinding.runtime)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.firebase.functions)
    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")

    // Firebase (BOM handles versions)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.messaging) // Keep only this if using BOM
    implementation ("com.google.firebase:firebase-installations:17.1.3")

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.analytics)

    // Extra UI
    implementation(libs.dotsindicator)
    implementation(libs.androidx.viewpager2)
    implementation(libs.support.annotations)
    implementation(libs.androidx.annotation)
    implementation(libs.picasso)
    implementation(libs.picasso.transformations)


    // Material EditText
//    implementation("com.rengwuxian.materialedittext:library:2.1.4")
//    {
//        exclude(group = "com.android.support", module = "support-v4")
//
//    }

// Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

// Android Architecture Components (ViewModel, Lifecycle)
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.2.0")

// Retrofit & OkHttp for networking
    implementation("com.squareup.retrofit2:retrofit:2.6.0")
    implementation("com.squareup.retrofit2:converter-gson:2.6.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.5.0")

    implementation(libs.okhttp)
    implementation(libs.volley)
    implementation(libs.google.auth.library.oauth2.http)

    // Play services & Auth
    implementation(libs.play.services.auth)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    implementation("com.intuit.sdp:sdp-android:1.0.6")
    implementation("com.intuit.ssp:ssp-android:1.0.6")
    implementation ("org.osmdroid:osmdroid-android:6.1.16")

}
