// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

// NOTE: KSP plugin per Kotlin 2.x non pubblicato su Maven Central / Google Maven / Gradle Plugin Portal.
// Workaround: usare versione snapshot da https://github.com/google/ksp/releases
// oppure scaricare manualmente il plugin e installarlo in maven local.
// Per ora il plugin è dichiarato nel version catalog ma non risolvibile automaticamente.