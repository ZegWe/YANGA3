import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

val ciSigningStore = System.getenv("YANGA_SIGNING_STORE")
val releaseSigningFile = rootProject.file("signing/release.properties")
val releaseSigning = Properties().apply {
    if (releaseSigningFile.isFile) releaseSigningFile.inputStream().use { load(it) }
}

val appVersion = Properties().apply { rootProject.file("version.properties").inputStream().use { load(it) } }

android {
    namespace = "com.yanga.client"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.zegwe.yanga"
        minSdk = 24
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = appVersion.getProperty("versionCode").toInt().also { require(it > 0) }
        versionName = appVersion.getProperty("versionName")
    }

    signingConfigs {
        if (releaseSigningFile.isFile || ciSigningStore != null) {
            create("release") {
                storeFile = rootProject.file(ciSigningStore ?: releaseSigning.getProperty("storeFile"))
                storePassword = System.getenv("YANGA_STORE_PASSWORD") ?: releaseSigning.getProperty("storePassword")
                keyAlias = System.getenv("YANGA_KEY_ALIAS") ?: releaseSigning.getProperty("keyAlias")
                keyPassword = System.getenv("YANGA_KEY_PASSWORD") ?: releaseSigning.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            if (releaseSigningFile.isFile || ciSigningStore != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  implementation("com.journeyapps:zxing-android-embedded:4.3.0")
  implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.material.kolor)
  implementation(libs.coil.compose)
  implementation(libs.coil.gif)
  implementation(libs.markwon.core)
  implementation(libs.markwon.linkify)
  implementation(libs.markwon.tables)
  implementation(libs.markwon.strike)
  implementation(libs.markwon.tasks)

  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.json)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)
}
