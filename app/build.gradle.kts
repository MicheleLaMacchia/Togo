import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "it.togo.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "it.togo.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0-dev"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        // Necessario per Compose UI test + Robolectric: risorse e manifest dell'app.
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Robolectric scarica gli jar android-all con un fetcher Maven interno che in
// Docker può fallire (SHA-512/rete) e non usa la cache Gradle. Facciamo
// risolvere gli stessi artifact a Gradle e li passiamo a Robolectric offline.
// Due configurazioni separate: nella stessa configurazione Gradle terrebbe una
// sola versione dello stesso artifact, mentre servono sia SDK 34 sia SDK 35.
val robolectricSdk34 = configurations.create("robolectricSdk34") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val robolectricSdk35 = configurations.create("robolectricSdk35") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val prepareRobolectricDependencies = tasks.register<Copy>("prepareRobolectricDependencies") {
    from(robolectricSdk34)
    from(robolectricSdk35)
    into(layout.buildDirectory.dir("robolectric-dependencies"))
}

tasks.withType<Test>().configureEach {
    dependsOn(prepareRobolectricDependencies)
    systemProperty(
        "robolectric.dependency.dir",
        layout.buildDirectory.dir("robolectric-dependencies").get().asFile.absolutePath,
    )
    systemProperty("robolectric.offline", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.room.runtime) {
        exclude(group = "com.intellij", module = "annotations")
    }
    implementation(libs.room.ktx) {
        exclude(group = "com.intellij", module = "annotations")
    }
    ksp(libs.room.compiler) {
        exclude(group = "com.intellij", module = "annotations")
    }
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.arch.core.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.sqlite.jdbc)
    testImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.junit)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // SDK 34 e 35 sono gli unici usati dai test Robolectric (@Config).
    add(
        robolectricSdk34.name,
        "org.robolectric:android-all-instrumented:14-robolectric-10818077-i7",
    )
    add(
        robolectricSdk35.name,
        "org.robolectric:android-all-instrumented:15-robolectric-12650502-i7",
    )
}
