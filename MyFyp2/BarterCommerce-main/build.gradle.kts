// Top-level build file where you can add configuration options common to all sub-projects/modules.
//plugins {
//    alias(libs.plugins.android.application) apply false
//    alias(libs.plugins.jetbrains.kotlin.android)   apply false
//not used |
//    alias(libs.plugins.android.library) apply false

//}
//buildscript {
//    dependencies {
//        classpath ("com.google.gms:google-services:4.4.2")
//        classpath ("androidx.navigation:navigation-safe-args-gradle-plugin:2.8.8")
//
//    }
//}


buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.9.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.8.8") // latest from BarterCommerce
        classpath(libs.google.services) // latest from BarterCommerce

    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
}